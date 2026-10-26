import hashlib
import importlib.util
import io
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

source = Path(__file__).resolve().parents[2] / 'ops/update-pull/pull.py'
spec = importlib.util.spec_from_file_location('syncme_pull', source)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class MirrorTest(unittest.TestCase):
    def setUp(self):
        self.folder = tempfile.TemporaryDirectory()
        self.destination = Path(self.folder.name)
        self.addCleanup(self.folder.cleanup)
        self.apk = b'verified-release-fixture'
        self.metadata = {'version': '1.1.13', 'versionCode': 202611052, 'build': 269,
                         'url': module.APK_URL, 'size': len(self.apk), 'sha256': hashlib.sha256(self.apk).hexdigest()}
        base = 'https://github.com/DrEsmaily/Syncme/releases/download/v1.1.13-build269/'
        self.release = {'tag_name': 'v1.1.13-build269', 'draft': False, 'prerelease': False,
                        'assets': [{'name': name, 'browser_download_url': base + name} for name in ['latest.json', 'SyncMe-269-ARM64.apk']]}

    def run_mirror(self, apk=None):
        def response(url):
            if url == module.RELEASE_API:
                data = json.dumps(self.release).encode()
            elif url.endswith('/latest.json'):
                data = json.dumps(self.metadata).encode()
            else:
                data = self.apk if apk is None else apk
            return io.BytesIO(data)
        with patch.object(module, 'open_https', side_effect=response):
            return module.mirror(self.destination)

    def seed_previous(self):
        previous = self.metadata | {'versionCode': 202611042, 'build': 268, 'version': '1.1.12', 'sha256': 'b' * 64}
        (self.destination / 'latest.json').write_text(json.dumps(previous))
        (self.destination / 'SyncMe.apk').write_bytes(b'old-apk')
        return (self.destination / 'latest.json').read_bytes()

    def test_success_publishes_exact_bytes_and_readable_files(self):
        self.assertIn('Published SyncMe', self.run_mirror())
        self.assertEqual((self.destination / 'SyncMe.apk').read_bytes(), self.apk)
        self.assertEqual(json.loads((self.destination / 'latest.json').read_text()), self.metadata)
        self.assertEqual((self.destination / 'SyncMe.apk').stat().st_mode & 0o777, 0o644)

    def test_hash_or_size_failure_preserves_previous_pair(self):
        old_json = self.seed_previous()
        for bad in [b'x' * len(self.apk), self.apk[:-1], self.apk + b'extra']:
            with self.assertRaises(ValueError):
                self.run_mirror(bad)
            self.assertEqual((self.destination / 'latest.json').read_bytes(), old_json)
            self.assertEqual((self.destination / 'SyncMe.apk').read_bytes(), b'old-apk')
        self.assertEqual(list(self.destination.glob('.syncme-pull-*')), [])

    def test_downgrade_is_skipped(self):
        self.seed_previous()
        current = self.metadata | {'versionCode': 202611062}
        (self.destination / 'latest.json').write_text(json.dumps(current))
        self.assertIn('downgrade skipped', self.run_mirror())
        self.assertEqual((self.destination / 'SyncMe.apk').read_bytes(), b'old-apk')

    def test_unchanged_release_does_not_redownload_apk(self):
        self.run_mirror()
        self.assertIn('already hosted', self.run_mirror(apk=b'wrong download must not be read'))

    def test_same_version_cannot_replace_apk_identity(self):
        self.run_mirror()
        self.metadata['sha256'] = 'f' * 64
        with self.assertRaisesRegex(ValueError, 'different APK'):
            self.run_mirror()

    def test_incomplete_or_prerelease_has_no_public_changes(self):
        previous = self.seed_previous()
        self.release['assets'] = []
        with self.assertRaises(ValueError):
            self.run_mirror()
        self.assertEqual((self.destination / 'latest.json').read_bytes(), previous)
        self.release['prerelease'] = True
        with self.assertRaises(ValueError):
            self.run_mirror()

    def test_untrusted_asset_and_mismatched_metadata_are_rejected(self):
        self.release['assets'][0]['browser_download_url'] = 'https://example.org/latest.json'
        with self.assertRaises(ValueError):
            self.run_mirror()
        self.release['assets'][0]['browser_download_url'] = 'https://github.com/DrEsmaily/Syncme/releases/download/v1.1.13-build269/latest.json'
        self.metadata['url'] = 'https://example.org/SyncMe.apk'
        with self.assertRaises(ValueError):
            self.run_mirror()

    def test_apk_is_replaced_before_metadata(self):
        real_replace = module.os.replace
        calls = []
        def replace(source, target):
            calls.append(Path(target).name)
            return real_replace(source, target)
        with patch.object(module.os, 'replace', side_effect=replace):
            self.run_mirror()
        self.assertEqual(calls, ['SyncMe.apk', 'latest.json'])

    def test_http_redirect_is_rejected_before_following(self):
        handler = module.HTTPSOnlyRedirect()
        with self.assertRaises(ValueError):
            handler.redirect_request(None, None, 302, '', {}, 'http://example.org/apk')


if __name__ == '__main__':
    unittest.main()
