#!/usr/bin/env python3
# Copyright 2026 LeVraiPedro and SkyPortal Thor contributors
# SPDX-License-Identifier: GPL-2.0-or-later
"""Apply the SkyPortal Thor Android service patch to a Dolphin source checkout."""
from __future__ import annotations

import argparse
import shutil
import subprocess
from pathlib import Path

SUPPORTED_DOLPHIN_COMMIT = "54070da5851e12f2d1a4389daa528e4fb81327ce"

PERMISSION_BLOCK = '''    <permission\n        android:name="com.skyportalthor.permission.PORTAL_CONTROL"\n        android:protectionLevel="signature" />\n'''
SERVICE_BLOCK = '''        <service\n            android:name=".skyportal.SkyPortalService"\n            android:exported="true"\n            android:permission="com.skyportalthor.permission.PORTAL_CONTROL" />\n\n'''


def verify_dolphin_revision(repo: Path, allow_unsupported: bool) -> None:
    result = subprocess.run(
        ["git", "rev-parse", "--verify", "HEAD"],
        cwd=repo,
        capture_output=True,
        text=True,
    )
    if result.returncode != 0:
        detail = result.stderr.strip() or "git rev-parse failed"
        raise SystemExit(f"Cannot determine the Dolphin source revision: {detail}")

    current_commit = result.stdout.strip().lower()
    if current_commit == SUPPORTED_DOLPHIN_COMMIT:
        print(f"verified: Dolphin commit {current_commit}")
        return

    message = (
        f"Unsupported Dolphin revision {current_commit}; "
        f"this patch is verified for {SUPPORTED_DOLPHIN_COMMIT}."
    )
    if not allow_unsupported:
        raise SystemExit(
            message
            + " Re-run with --allow-unsupported only after reviewing and testing the resulting diff."
        )
    print(f"WARNING: {message} Continuing because --allow-unsupported was specified.")


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


def apply_git_patch(repo: Path, patch: Path, label: str) -> None:
    reverse_check = subprocess.run(
        [
            "git", "apply", "--reverse", "--check",
            "--ignore-space-change", "--ignore-whitespace", str(patch)
        ],
        cwd=repo,
        capture_output=True,
        text=True,
    )
    if reverse_check.returncode == 0:
        print(f"already patched: {label}")
        return

    check = subprocess.run(
        [
            "git", "apply", "--check",
            "--ignore-space-change", "--ignore-whitespace", str(patch)
        ],
        cwd=repo,
        capture_output=True,
        text=True,
    )
    if check.returncode != 0:
        raise RuntimeError(f"{label} patch cannot be applied:\n{check.stderr}")
    subprocess.run(
        [
            "git", "apply", "--ignore-space-change", "--ignore-whitespace", str(patch)
        ],
        cwd=repo,
        check=True,
    )
    reverse_check = subprocess.run(
        [
            "git", "apply", "--reverse", "--check",
            "--ignore-space-change", "--ignore-whitespace", str(patch)
        ],
        cwd=repo,
        capture_output=True,
        text=True,
    )
    if reverse_check.returncode != 0:
        raise RuntimeError(f"{label} patch failed its reverse check:\n{reverse_check.stderr}")
    print(f"patched: {label}")


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Apply the SkyPortal API 4 integration to a Dolphin source checkout."
    )
    parser.add_argument("dolphin_repo", type=Path, help="Path to a dolphin-emu/dolphin checkout")
    parser.add_argument(
        "--allow-unsupported",
        action="store_true",
        help=(
            "allow a Dolphin HEAD other than the pinned supported commit; "
            "the resulting source must be reviewed and tested"
        ),
    )
    args = parser.parse_args()

    repo = args.dolphin_repo.resolve()
    here = Path(__file__).resolve().parents[1]
    patch_root = here / "dolphin-patch" / "Source" / "Android" / "app" / "src" / "main"
    core_patches = [
        (
            here / "dolphin-patch" / "smart-portal-core.patch",
            "native Smart Portal catalog, slots and USB state",
        ),
        (
            here / "dolphin-patch" / "portal-led-api4.patch",
            "native Portal of Power LED state API 4",
        ),
        (
            here / "dolphin-patch" / "android-menu-lifecycle.patch",
            "Android emulation menu lifecycle",
        ),
    ]

    manifest = repo / "Source/Android/app/src/main/AndroidManifest.xml"
    gradle = repo / "Source/Android/app/build.gradle.kts"
    if not manifest.exists() or not gradle.exists():
        raise SystemExit("This does not look like a Dolphin repository checkout")
    verify_dolphin_revision(repo, args.allow_unsupported)

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
    for patch, label in core_patches:
        apply_git_patch(repo, patch, label)
    print("SkyPortal API 4 patch applied successfully.")


if __name__ == "__main__":
    main()
