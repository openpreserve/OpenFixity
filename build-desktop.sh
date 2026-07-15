#!/usr/bin/env bash
#
# OpenFixity is an application for monitoring and reporting on the fixity of files.
# Copyright (C) 2026 Open Preservation Foundation
# Licensed under the Apache License, Version 2.0. See LICENSE.
#
# Builds a native desktop installer for the OS this runs on, using jpackage:
#   Linux   -> .deb (or .rpm)      macOS -> .dmg      Windows -> .msi
# The installer bundles its own Java runtime, so end users need nothing installed.
#
# jpackage CANNOT cross-compile: run this on the OS you want to package for. The tag-driven
# GitHub workflow (.github/workflows/desktop-installers.yml) runs it once per target OS.
#
# Prereqs: Node + npm, Maven, a JDK 17+ with jpackage on PATH. Plus WiX 3 on Windows, and
# dpkg (or rpmbuild) on Linux.
set -euo pipefail

APP_NAME="OpenFixity"
VENDOR="Open Preservation Foundation"
MAIN_CLASS="org.openpreservation.fixity.apps.server.desktop.OpenFixityDesktop"

# The runtime modules jlink includes in the bundled JRE. java.se pulls in the standard library;
# the rest cover TLS, the H2/JDBC stack, zip filesystem, locale data and DNS.
ADD_MODULES="java.se,jdk.crypto.ec,jdk.crypto.cryptoki,jdk.unsupported,jdk.zipfs,jdk.localedata,jdk.management,jdk.naming.dns,jdk.charsets"

# --- versioning ----------------------------------------------------------------------------
# Single source of truth is the POM. FULL keeps the -ALPHA label for the distributed filename
# and the in-app About page; NUMERIC is the bare x.y.z jpackage needs for its version field.
FULL_VERSION="$(grep -m1 -oE '<version>[^<]+' pom.xml | sed -E 's/<version>//')"
NUMERIC="$(echo "${FULL_VERSION}" | sed -E 's/-.*$//')"

OS="$(uname -s)"

# macOS rejects a CFBundleVersion whose first component is 0 ("The first number in the version
# must be greater than zero"). Our alpha is 0.1.1, so on macOS ONLY we pass a compliant version
# for that invisible bundle metadata. The filename and the app's own /api/info still report the
# true FULL_VERSION, so nothing a user sees is affected.
JP_VERSION="${NUMERIC}"
if [ "${OS}" = "Darwin" ] && [[ "${NUMERIC}" == 0.* ]]; then
    JP_VERSION="1.0.0"
fi

# --- platform label + icon -----------------------------------------------------------------
# Normalise the arch name so installer filenames read x64 / arm64 consistently. TARGET_ARCH
# overrides the host detection for a cross-build (an x64 installer produced on an Apple Silicon
# runner via an x64 JDK under Rosetta 2), where uname would otherwise report the host's arm64.
if [ -n "${TARGET_ARCH:-}" ]; then
    ARCH="${TARGET_ARCH}"
else
    case "$(uname -m)" in
        x86_64|amd64) ARCH="x64" ;;
        aarch64|arm64) ARCH="arm64" ;;
        *) ARCH="$(uname -m)" ;;
    esac
fi

ICON_DIR="branding/icons"
ICON=""
case "${OS}" in
    Linux)               PLATFORM="linux-${ARCH}";  ICON="${ICON_DIR}/OpenFixity.png" ;;
    Darwin)              PLATFORM="mac-${ARCH}";     ICON="${ICON_DIR}/OpenFixity.icns"
        # iconutil (macOS only) compiles the .iconset into the .icns jpackage wants. Non-fatal:
        # if it fails, jpackage falls back to its default icon rather than aborting the build.
        if [ ! -f "${ICON}" ] && [ -d "${ICON_DIR}/OpenFixity.iconset" ] && command -v iconutil >/dev/null; then
            iconutil -c icns "${ICON_DIR}/OpenFixity.iconset" -o "${ICON}" || echo "  (iconutil failed; using default icon)"
        fi ;;
    MINGW*|MSYS*|CYGWIN*) PLATFORM="windows-x64";        ICON="${ICON_DIR}/OpenFixity.ico" ;;
    *)                   PLATFORM="${OS}" ;;
esac
ICON_ARG=()
if [ -n "${ICON}" ] && [ -f "${ICON}" ]; then
    ICON_ARG=(--icon "${ICON}")
    echo "  app icon: ${ICON}"
else
    echo "  (no icon for ${OS}; jpackage will use its default)"
fi

echo "======================================"
echo " OpenFixity desktop build"
echo "   version : ${FULL_VERSION}  (jpackage: ${JP_VERSION})"
echo "   platform: ${PLATFORM}"
echo "======================================"

# --- [1/4] build the shaded jar with the frontend embedded ---------------------------------
echo "[1/4] mvn clean package (builds React + shaded jar)…"
mvn -q clean package -DskipTests

# --- [2/4] collect the app jar + the JavaFX jars into one input dir ------------------------
echo "[2/4] collecting jars…"
rm -rf target/desktop-libs target/jpackage-input target/desktop
mkdir -p target/jpackage-input
# The shaded jar (server + frontend + all non-JavaFX deps). Not the original-* jar.
cp target/open-fixity-*.jar target/jpackage-input/ 2>/dev/null
# Drop the unshaded original jar if the glob caught it.
rm -f target/jpackage-input/original-*.jar
MAIN_JAR="$(cd target/jpackage-input && ls open-fixity-*.jar | grep -v '^original-' | head -n1)"
# JavaFX ships native code that cannot be shaded into the fat jar, so bundle its jars alongside.
mvn -q dependency:copy-dependencies \
    -DincludeGroupIds=org.openjfx \
    -DoutputDirectory=target/desktop-libs
cp target/desktop-libs/*.jar target/jpackage-input/
echo "  main jar: ${MAIN_JAR}; $(ls target/desktop-libs/*.jar | wc -l) JavaFX jars bundled"

# --- [3/4] portable app-image --------------------------------------------------------------
echo "[3/4] jpackage: portable app-image…"
jpackage \
    --type app-image \
    --name "${APP_NAME}" \
    --app-version "${JP_VERSION}" \
    --vendor "${VENDOR}" \
    --input target/jpackage-input \
    --main-jar "${MAIN_JAR}" \
    --main-class "${MAIN_CLASS}" \
    --add-modules "${ADD_MODULES}" \
    --java-options "-Dfile.encoding=UTF-8" \
    ${ICON_ARG[@]+"${ICON_ARG[@]}"} \
    --dest target/desktop

# --- [4/4] native installer for this OS ----------------------------------------------------
echo "[4/4] jpackage: native installer…"
INSTALLER_TYPE=""
EXTRA=()
case "${OS}" in
    Linux)   command -v dpkg-deb >/dev/null && INSTALLER_TYPE="deb" || { command -v rpmbuild >/dev/null && INSTALLER_TYPE="rpm"; }
             EXTRA=(--linux-shortcut --linux-menu-group "Utility" --linux-package-name openfixity) ;;
    Darwin)  INSTALLER_TYPE="dmg" ;;
    MINGW*|MSYS*|CYGWIN*) INSTALLER_TYPE="msi"
             EXTRA=(--win-shortcut --win-menu --win-menu-group "OpenFixity" --win-dir-chooser) ;;
esac

if [ -n "${INSTALLER_TYPE}" ]; then
    # ${EXTRA[@]+...} expands to nothing when EXTRA is empty, avoiding an unbound-variable error
    # under `set -u` on macOS's ancient bash 3.2.
    jpackage \
        --type "${INSTALLER_TYPE}" \
        --name "${APP_NAME}" \
        --app-version "${JP_VERSION}" \
        --vendor "${VENDOR}" \
        --input target/jpackage-input \
        --main-jar "${MAIN_JAR}" \
        --main-class "${MAIN_CLASS}" \
        --add-modules "${ADD_MODULES}" \
        --java-options "-Dfile.encoding=UTF-8" \
        ${ICON_ARG[@]+"${ICON_ARG[@]}"} \
        --dest target/desktop \
        ${EXTRA[@]+"${EXTRA[@]}"}

    # Rename to a consistent, version-honest name for distribution:
    #   OpenFixity-0.1.1-ALPHA-mac-arm64.dmg
    for f in target/desktop/*."${INSTALLER_TYPE}"; do
        [ -f "$f" ] || continue
        mv "$f" "target/desktop/${APP_NAME}-${FULL_VERSION}-${PLATFORM}.${INSTALLER_TYPE}"
    done
else
    echo "  (no installer type for ${OS} here)"
fi

echo
echo "Done. Artifacts in target/desktop:"
ls -1 target/desktop/*.deb target/desktop/*.rpm target/desktop/*.dmg target/desktop/*.msi 2>/dev/null | sed 's/^/  /' || true
