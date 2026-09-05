#!/usr/bin/env python3
# Copyright 2026 LeVraiPedro and SkyPortal Thor contributors
# SPDX-License-Identifier: GPL-2.0-or-later
"""Lightweight repository licensing checks used locally and in CI."""

from __future__ import annotations

import hashlib
import subprocess
import sys
import zipfile
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
PROJECT_COPYRIGHT = "Copyright 2026 LeVraiPedro and SkyPortal Thor contributors"
PROJECT_SPDX = "SPDX-License-Identifier: GPL-2.0-or-later"
GPL_V2_TEXT_SHA256 = "aaf135472f81c5b4a0dca9367e5bb5e9750032b5bebe5442b36e4c0a47430df3"


def tracked_files() -> list[str]:
    completed = subprocess.run(
        ["git", "ls-files", "--cached", "--others", "--exclude-standard"],
        cwd=ROOT,
        check=True,
        capture_output=True,
        text=True,
    )
    return [line.strip().replace("\\", "/") for line in completed.stdout.splitlines() if line.strip()]


def is_project_source(path: str) -> bool:
    if path.startswith(".github/workflows/") and path.endswith(".yml"):
        return True
    if path in {"build.gradle.kts", "settings.gradle.kts", "gradle.properties", "app/build.gradle.kts"}:
        return True
    if path.startswith("app/src/") and Path(path).suffix in {".kt", ".aidl", ".xml"}:
        return True
    if path.startswith("tools/") and Path(path).suffix in {".kt", ".py"}:
        return True
    if path.startswith("dolphin-patch/Source/") and Path(path).suffix in {".kt", ".aidl"}:
        return True
    return path in {
        "dolphin-patch/AndroidManifest.additions.xml",
        "dolphin-patch/build.gradle.addition.txt",
    }


def check_required_documents(errors: list[str]) -> None:
    license_path = ROOT / "LICENSE"
    reuse_path = ROOT / "LICENSES/GPL-2.0-or-later.txt"
    notice_path = ROOT / "NOTICE.md"
    for path in (license_path, reuse_path, notice_path):
        if not path.is_file():
            errors.append(f"missing required file: {path.relative_to(ROOT)}")
    if not license_path.is_file() or not reuse_path.is_file():
        return
    # Git may materialize text files with CRLF on Windows. Compare and hash the
    # canonical LF form so the same official text passes on every build host.
    license_bytes = license_path.read_bytes().replace(b"\r\n", b"\n").replace(b"\r", b"\n")
    reuse_bytes = reuse_path.read_bytes().replace(b"\r\n", b"\n").replace(b"\r", b"\n")
    if license_bytes != reuse_bytes:
        errors.append("LICENSE and LICENSES/GPL-2.0-or-later.txt differ")
    license_digest = hashlib.sha256(license_bytes).hexdigest()
    if license_digest != GPL_V2_TEXT_SHA256:
        errors.append(
            "LICENSE does not match the pinned official Dolphin GPL-2.0-or-later text "
            f"(expected SHA-256 {GPL_V2_TEXT_SHA256}, got {license_digest})"
        )
    license_text = license_bytes.decode("utf-8")
    if not license_text.startswith("GNU GENERAL PUBLIC LICENSE\nVersion 2, June 1991"):
        errors.append("LICENSE is not the complete GPL version 2 text")
    if "END OF TERMS AND CONDITIONS" not in license_text or len(license_text) < 17_000:
        errors.append("LICENSE appears truncated")


def check_project_headers(files: list[str], errors: list[str]) -> None:
    for relative in files:
        if not is_project_source(relative):
            continue
        text = (ROOT / relative).read_text(encoding="utf-8")
        header = "\n".join(text.splitlines()[:10])
        if PROJECT_COPYRIGHT not in header:
            errors.append(f"missing SkyPortal copyright header: {relative}")
        if PROJECT_SPDX not in header:
            errors.append(f"missing GPL-2.0-or-later SPDX header: {relative}")


def check_third_party_notices(errors: list[str]) -> None:
    expected = {
        "gradlew": "SPDX-License-Identifier: Apache-2.0",
        "gradlew.bat": "SPDX-License-Identifier: Apache-2.0",
    }
    for relative, marker in expected.items():
        text = (ROOT / relative).read_text(encoding="utf-8")
        if marker not in "\n".join(text.splitlines()[:25]):
            errors.append(f"third-party notice missing or changed: {relative}")

    wrapper_jar = ROOT / "gradle/wrapper/gradle-wrapper.jar"
    try:
        with zipfile.ZipFile(wrapper_jar) as archive:
            apache_license = archive.read("META-INF/LICENSE").decode("utf-8")
    except (FileNotFoundError, KeyError, zipfile.BadZipFile, UnicodeDecodeError) as exc:
        errors.append(f"Gradle wrapper Apache notice is unreadable: {exc}")
    else:
        if "Apache License" not in apache_license or "Version 2.0" not in apache_license:
            errors.append("Gradle wrapper JAR no longer contains the Apache License 2.0 notice")

    for relative in (
        "dolphin-patch/smart-portal-core.patch",
        "dolphin-patch/portal-led-api4.patch",
        "dolphin-patch/android-menu-lifecycle.patch",
        "dolphin-patch/skyportal-dolphin.patch",
    ):
        patch_path = ROOT / relative
        if not patch_path.is_file():
            errors.append(f"missing required Dolphin patch: {relative}")
            continue
        for number, line in enumerate(patch_path.read_text(encoding="utf-8").splitlines(), start=1):
            if line.startswith("---"):
                continue
            if line.startswith("-") and ("Copyright" in line or "SPDX-License-Identifier" in line):
                errors.append(f"Dolphin notice removed by {relative}:{number}")


def check_forbidden_tracked_content(files: list[str], errors: list[str]) -> None:
    forbidden_suffixes = {
        ".jks", ".keystore", ".p12", ".pfx", ".pem", ".key",
        ".iso", ".gcm", ".wbfs", ".wad", ".rvz", ".gcz", ".wia", ".ciso",
    }
    forbidden_roots = {
        "artifacts", "captures", "device-backups", "collections", "dumps",
        "test-fixtures", "user-collections",
    }
    for relative in files:
        path = Path(relative)
        lowered_parts = {part.lower() for part in path.parts}
        is_private_env = path.name == ".env" or (
            path.name.startswith(".env.") and path.name != ".env.example"
        )
        if path.name in {"local.properties", "secrets.properties", "keystore.properties"} or is_private_env:
            errors.append(f"sensitive local configuration is tracked: {relative}")
        if path.suffix.lower() in forbidden_suffixes:
            errors.append(f"forbidden binary/content type is tracked: {relative}")
        if lowered_parts & forbidden_roots:
            errors.append(f"private artifact directory is tracked: {relative}")
        if path.suffix.lower() == ".sky" and not relative.startswith("app/src/test/resources/fixtures/"):
            errors.append(f"undocumented Skylander dump is tracked: {relative}")


def main() -> int:
    errors: list[str] = []
    files = tracked_files()
    check_required_documents(errors)
    check_project_headers(files, errors)
    check_third_party_notices(errors)
    check_forbidden_tracked_content(files, errors)
    if errors:
        for error in errors:
            print(f"licensing-check: ERROR: {error}", file=sys.stderr)
        return 1
    checked = sum(1 for path in files if is_project_source(path))
    print(f"licensing-check: OK ({checked} SkyPortal source files checked)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
