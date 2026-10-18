"""Verify the optimized combined APK against the last published SyncMe build."""

import os
from pathlib import Path
import re
import shutil
import subprocess
import tempfile
import zipfile


output = Path("build-checkpoint")
output.mkdir(exist_ok=True)
candidates = list(Path("app/build/outputs/apk/gplay/release").glob("*arm64-v8a*release.apk"))
assert len(candidates) == 1, candidates
apk = output / "SyncMe-257-ARM64.apk"
shutil.copyfile(candidates[0], apk)
reference = Path("/tmp/syncme-reference/SyncMe-256-ARM64.apk")
assert reference.is_file()
build_tools = sorted(
    (Path(os.environ["ANDROID_HOME"]) / "build-tools").glob("*/aapt"),
    key=lambda path: tuple(int(part) for part in re.findall(r"\d+", path.parent.name)),
)[-1].parent


def run(tool, *args):
    return subprocess.check_output(
        [str(build_tools / tool), *map(str, args)], text=True, encoding="utf-8", errors="replace"
    )


def metadata(path):
    text = run("aapt", "dump", "badging", path)
    package = re.search(r"^package: name='([^']+)' versionCode='(\d+)' versionName='([^']+)'", text, re.M)
    assert package, text
    return text, package.groups()


with zipfile.ZipFile(apk) as archive:
    assert archive.testzip() is None
    assert any(name.startswith("lib/arm64-v8a/") for name in archive.namelist())
    assert not any(name.startswith(("lib/x86/", "lib/x86_64/", "lib/armeabi-v7a/")) for name in archive.namelist())
    verifier_found = False
    with tempfile.TemporaryDirectory() as directory:
        for name in archive.namelist():
            if re.fullmatch(r"classes\d*\.dex", name):
                dex = Path(directory) / name
                dex.write_bytes(archive.read(name))
                if "Lorg/rustls/platformverifier/CertificateVerifier;" in run("dexdump", "-f", dex):
                    verifier_found = True
                    break
    assert verifier_found, "TLS JNI certificate verifier missing from optimized APK"

manifest, (application_id, version_code, version_name) = metadata(apk)
_, (previous_id, previous_code, _) = metadata(reference)
assert application_id == previous_id == "ir.syncme.app.syncmerelease"
assert version_code == "202610242" and int(version_code) > int(previous_code)
assert version_name == "257"
assert "native-code: 'arm64-v8a'" in manifest
(output / "manifest.txt").write_text(manifest)
resources = run("aapt", "dump", "--values", "resources", apk)
assert '"com.github.dresmaily.syncme"' in resources, "OAuth redirect scheme changed"

signature = run("apksigner", "verify", "--verbose", "--print-certs", apk)
previous_signature = run("apksigner", "verify", "--print-certs", reference)
pattern = r"certificate SHA-256 digest: ([0-9a-fA-F]+)"
current_digest = re.search(pattern, signature)
previous_digest = re.search(pattern, previous_signature)
assert current_digest and previous_digest
assert current_digest[1] == previous_digest[1], "Signing certificate differs from build 256"
(output / "signature.txt").write_text(signature)
(output / "update-compatibility.txt").write_text(
    f"Application ID: {application_id}\nVersion code: {previous_code} -> {version_code}\n"
    f"Signing certificate SHA-256: {current_digest[1]}\n"
    "Same application ID and signing certificate; version code increased.\n"
)
print(f"Verified {apk}: version {version_name}, ARM64, compatible signing certificate")
