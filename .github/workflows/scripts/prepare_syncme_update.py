"""Create the two manually hosted updater files from a verified build."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import zipfile

parser = argparse.ArgumentParser()
parser.add_argument('--apk', type=Path, required=True)
parser.add_argument('--version', required=True)
parser.add_argument('--build', type=int, required=True)
parser.add_argument('--output', type=Path, required=True)
args = parser.parse_args()
manifest = (args.output / 'manifest.txt').read_text()
match = re.search(r"versionCode='(\d+)' versionName='([^']+)'", manifest)
assert match and match[2] == args.version, "Android versionName must equal the semantic release version"
metadata = {
    'version': args.version,
    'versionCode': int(match[1]),
    'build': args.build,
    'url': 'https://updates.syncme.ir/android/SyncMe.apk',
    'size': args.apk.stat().st_size,
    'sha256': hashlib.sha256(args.apk.read_bytes()).hexdigest(),
}
path = args.output / 'latest.json'
path.write_text(json.dumps(metadata, indent=2) + '\n')
with zipfile.ZipFile(args.output / f'SyncMe-{args.build}-Server-Upload.zip', 'w', compression=zipfile.ZIP_STORED) as archive:
    archive.write(args.apk, 'SyncMe.apk')
    archive.write(path, 'latest.json')
print('Prepared latest.json and server-upload ZIP from verified APK metadata.')
