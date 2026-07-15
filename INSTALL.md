# Installing OpenFixity (desktop)

Download the installer for your platform from the
[Releases page](https://github.com/openpreserve/OpenFixity/releases), or from the
OPF artifact server. Each installer bundles its own Java runtime, so you do not need
Java installed.

| Platform | File |
| --- | --- |
| Windows | `OpenFixity-<version>-windows-x64.msi` |
| macOS (Apple Silicon) | `OpenFixity-<version>-mac-arm64.dmg` |
| macOS (Intel) | `OpenFixity-<version>-mac-x64.dmg` |
| Linux (Debian/Ubuntu) | `OpenFixity-<version>-linux-x64.deb` |

If you are unsure which Mac you have: Apple menu, *About This Mac*. A "Chip"
beginning with **Apple** is Apple Silicon; an **Intel** processor is Intel.

## A note on security warnings

These pre-alpha installers are **not yet code-signed**, so Windows and macOS will
warn that the publisher is unverified. This is expected for an alpha. The steps
below are the standard way to run an unsigned app you trust.

### Windows

1. Run the `.msi`.
2. If SmartScreen says "Windows protected your PC", click **More info**, then
   **Run anyway**.

### macOS

1. Open the `.dmg` and drag OpenFixity to Applications.
2. The first time you launch it, **right-click (or Control-click) the app and choose
   Open**, then confirm. Double-clicking alone will be blocked by Gatekeeper; the
   right-click-Open route is what tells macOS you trust it.
3. If macOS still refuses, go to *System Settings, Privacy & Security*, scroll down,
   and click **Open Anyway** next to the OpenFixity message.

### Linux

```bash
sudo apt install ./OpenFixity-<version>-linux-x64.deb
```

## Verifying a download

Each installer on the artifact server is published with its MD5 and SHA-256. To
check a download:

```bash
# Linux / macOS
shasum -a 256 OpenFixity-<version>-<platform>.<ext>
```

Compare the result against the SHA-256 shown for that file.

## Running it

Launch OpenFixity from your applications menu. It opens its own window, runs
entirely on your machine, and stores its data under your home directory in
`.openfixity`.
