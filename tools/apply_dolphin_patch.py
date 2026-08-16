#!/usr/bin/env python3
"""Apply the SkyPortal Thor Android service patch to a Dolphin source checkout."""
from __future__ import annotations

import argparse
import shutil
import subprocess
from pathlib import Path

PERMISSION_BLOCK = '''    <permission\n        android:name="com.skyportalthor.permission.PORTAL_CONTROL"\n        android:protectionLevel="signature" />\n'''
SERVICE_BLOCK = '''        <service\n            android:name=".skyportal.SkyPortalService"\n            android:exported="true"\n            android:permission="com.skyportalthor.permission.PORTAL_CONTROL" />\n\n'''


def patch_manifest(path: Path) -> None:
    text = path.read_text(encoding="utf-8")
    if "com.skyportalthor.permission.PORTAL_CONTROL" not in text:
        marker = '    xmlns:tools="http://schemas.android.com/tools">\n'
        if marker not in text:
            raise RuntimeError("Manifest opening marker not found")
        text = text.replace(marker, marker + "\n" + PERMISSION_BLOCK, 1)

    if ".skyportal.SkyPortalService" not in text:
        marker = "    </application>"
        if marker not in text:
            raise RuntimeError("</application> not found")
        text = text.replace(marker, SERVICE_BLOCK + marker, 1)

    path.write_text(text, encoding="utf-8")


def patch_gradle(path: Path) -> None:
    text = path.read_text(encoding="utf-8")
    if "aidl = true" not in text:
        marker = "        resValues = true\n"
        if marker not in text:
            raise RuntimeError("buildFeatures/resValues marker not found")
        text = text.replace(marker, marker + "        aidl = true\n", 1)

    if "skyPortalVersionCode" not in text:
        marker = "        versionCode = getBuildVersionCode()"
        if marker not in text:
            raise RuntimeError("defaultConfig/versionCode marker not found")
        replacement = '''        // Optional override for an upgrade build made from a shallow checkout.
        versionCode = providers.gradleProperty("skyPortalVersionCode").orNull
            ?.toIntOrNull() ?: getBuildVersionCode()'''
        text = text.replace(marker, replacement, 1)
    path.write_text(text, encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("dolphin_repo", type=Path, help="Path to a dolphin-emu/dolphin checkout")
    args = parser.parse_args()

    repo = args.dolphin_repo.resolve()
    here = Path(__file__).resolve().parents[1]
    patch_root = here / "dolphin-patch" / "Source" / "Android" / "app" / "src" / "main"
    core_patch = here / "dolphin-patch" / "smart-portal-core.patch"

    manifest = repo / "Source/Android/app/src/main/AndroidManifest.xml"
    gradle = repo / "Source/Android/app/build.gradle.kts"
    if not manifest.exists() or not gradle.exists():
        raise SystemExit("This does not look like a Dolphin repository checkout")

    targets = [
        (
            patch_root / "aidl/com/skyportalthor/ipc/ISkylanderPortalService.aidl",
            repo / "Source/Android/app/src/main/aidl/com/skyportalthor/ipc/ISkylanderPortalService.aidl",
        ),
        (
            patch_root / "java/org/dolphinemu/dolphinemu/skyportal/SkyPortalService.kt",
            repo / "Source/Android/app/src/main/java/org/dolphinemu/dolphinemu/skyportal/SkyPortalService.kt",
        ),
    ]

    for src, dst in targets:
        dst.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(src, dst)
        print(f"copied: {dst.relative_to(repo)}")

    patch_manifest(manifest)
    print("patched: Source/Android/app/src/main/AndroidManifest.xml")
    patch_gradle(gradle)
    print("patched: Source/Android/app/build.gradle.kts")
    reverse_check = subprocess.run(
        [
            "git", "apply", "--reverse", "--check",
            "--ignore-space-change", "--ignore-whitespace", str(core_patch)
        ], cwd=repo,
        capture_output=True, text=True
    )
    if reverse_check.returncode == 0:
        print("already patched: native Smart Portal API")
    else:
        check = subprocess.run(
            [
                "git", "apply", "--check",
                "--ignore-space-change", "--ignore-whitespace", str(core_patch)
            ], cwd=repo,
            capture_output=True, text=True
        )
        if check.returncode != 0:
            raise RuntimeError(f"Native Smart Portal patch cannot be applied:\n{check.stderr}")
        subprocess.run(
            [
                "git", "apply", "--ignore-space-change", "--ignore-whitespace",
                str(core_patch)
            ],
            cwd=repo,
            check=True,
        )
        print("patched: native Smart Portal catalog and slot snapshot")
    print("SkyPortal patch applied successfully.")


if __name__ == "__main__":
    main()
