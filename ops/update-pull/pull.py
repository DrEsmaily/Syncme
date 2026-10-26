#!/usr/bin/python3
"""Mirror verified public SyncMe releases without SSH or GitHub credentials."""
import fcntl
import hashlib
import json
import os
from pathlib import Path
import re
import signal
import sys
import tempfile
import urllib.parse
import urllib.request

DESTINATION = Path('/opt/syncme/updates/android')
RELEASE_API = 'https://api.github.com/repos/DrEsmaily/SyncMe/releases/latest'
APK_URL = 'https://updates.syncme.ir/android/SyncMe.apk'
MAX_APK = 536870912


class HTTPSOnlyRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, request, fp, code, message, headers, new_url):
        if urllib.parse.urlsplit(new_url).scheme != 'https':
            raise ValueError('Insecure redirect rejected')
        return super().redirect_request(request, fp, code, message, headers, new_url)


def open_https(url):
    parsed = urllib.parse.urlsplit(url)
    if parsed.scheme != 'https' or parsed.username or parsed.password:
        raise ValueError('HTTPS without embedded credentials is required')
    response = urllib.request.build_opener(HTTPSOnlyRedirect()).open(urllib.request.Request(url, headers={
        'User-Agent': 'SyncMe-Update-Mirror/1', 'Cache-Control': 'no-cache',
    }), timeout=20)
    if urllib.parse.urlsplit(response.url).scheme != 'https':
        response.close()
        raise ValueError('Insecure redirect rejected')
    return response


def read_json(url, limit):
    with open_https(url) as response:
        data = response.read(limit + 1)
    if len(data) > limit:
        raise ValueError('JSON exceeds size limit')
    result = json.loads(data)
    if not isinstance(result, dict):
        raise ValueError('JSON object required')
    return result


def asset_url(release, name):
    matches = [item for item in release['assets'] if item['name'] == name]
    if len(matches) != 1:
        raise ValueError(f'Release asset not ready: {name}')
    url = matches[0]['browser_download_url']
    parsed = urllib.parse.urlsplit(url)
    parts = parsed.path.split('/')
    if (parsed.scheme != 'https' or parsed.netloc != 'github.com' or parsed.query or parsed.fragment
            or len(parts) != 7 or parts[1].lower() != 'dresmaily' or parts[2].lower() != 'syncme'
            or parts[3:] != ['releases', 'download', release['tag_name'], name]):
        raise ValueError('Unexpected release asset URL')
    return url


def validate(metadata, release):
    version, code, build, size, checksum = (metadata[key] for key in ('version', 'versionCode', 'build', 'size', 'sha256'))
    if not isinstance(version, str) or len(version) > 32 or not re.fullmatch(r'[0-9]+(\.[0-9]+){1,3}', version):
        raise ValueError('Invalid version')
    if type(code) is not int or not 0 < code <= 2100000000 or type(build) is not int or build <= 0:
        raise ValueError('Invalid version or build code')
    if type(size) is not int or not 0 < size <= MAX_APK:
        raise ValueError('Invalid APK size')
    if not isinstance(checksum, str) or not re.fullmatch(r'[0-9a-f]{64}', checksum):
        raise ValueError('Invalid checksum')
    if metadata['url'] != APK_URL or release['tag_name'] != f'v{version}-build{build}':
        raise ValueError('Metadata does not match this release')


def mirror(destination=DESTINATION):
    with (destination / '.mirror.lock').open('a') as lock:
        try:
            fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError:
            return 'Another update check is already running.'
        release = read_json(RELEASE_API, 1048576)
        if release.get('draft') is not False or release.get('prerelease') is not False:
            raise ValueError('Only published stable releases are accepted')
        metadata = read_json(asset_url(release, 'latest.json'), 65536)
        validate(metadata, release)
        apk_url = asset_url(release, f"SyncMe-{metadata['build']}-ARM64.apk")
        current_path = destination / 'latest.json'
        if current_path.exists():
            current = json.loads(current_path.read_text())
            if metadata['versionCode'] < current['versionCode']:
                return 'A newer version is already hosted; downgrade skipped.'
            if metadata['versionCode'] == current['versionCode']:
                if metadata['sha256'] != current['sha256']:
                    raise ValueError('Same version code has a different APK')
                apk = destination / 'SyncMe.apk'
                if apk.is_file() and apk.stat().st_size == metadata['size']:
                    return f"Build {metadata['build']} is already hosted."
        with tempfile.TemporaryDirectory(prefix='.syncme-pull-', dir=destination) as folder:
            staging = Path(folder)
            digest, size = hashlib.sha256(), 0
            with open_https(apk_url) as source, (staging / 'SyncMe.apk').open('wb') as target:
                while True:
                    block = source.read(min(65536, metadata['size'] - size + 1))
                    if not block:
                        break
                    size += len(block)
                    if size > metadata['size']:
                        raise ValueError('APK exceeds declared size')
                    digest.update(block)
                    target.write(block)
                target.flush()
                os.fsync(target.fileno())
            if size != metadata['size'] or digest.hexdigest() != metadata['sha256']:
                raise ValueError('APK checksum or size mismatch')
            with (staging / 'latest.json').open('w') as target:
                json.dump(metadata, target, indent=2)
                target.write('\n')
                target.flush()
                os.fsync(target.fileno())
            for name in ('SyncMe.apk', 'latest.json'):
                os.chmod(staging / name, 0o644)
            os.replace(staging / 'SyncMe.apk', destination / 'SyncMe.apk')
            os.replace(staging / 'latest.json', current_path)
            directory = os.open(destination, os.O_RDONLY | os.O_DIRECTORY)
            try:
                os.fsync(directory)
            finally:
                os.close(directory)
            return f"Published SyncMe {metadata['version']} (Build {metadata['build']})."


def interrupted(signum, frame):
    raise SystemExit('Update check interrupted; staging cleanup requested.')


if __name__ == '__main__':
    signal.signal(signal.SIGTERM, interrupted)
    try:
        print(mirror())
    except Exception as error:
        sys.exit(f'Update mirror failed: {error}')
