# ERP Inventory for Windows

A small Windows 10/11 app (Tauri + Microsoft Edge WebView2) that opens the live
dashboard at https://erpinventory.bd. All features and data come from the server, so
dashboard changes reach the app without a new installer.

- Installer: NSIS `.exe`, installs per user (no admin needed). If WebView2 is missing
  (rare on Windows 10, built into 11) the installer downloads it.
- Login is kept in the app's own WebView2 profile, so the user stays signed in between
  launches for as long as the server session allows.
- Links to other sites (WhatsApp, Facebook, ...) open in the default browser.
- Second launch focuses the open window.
- Not code-signed yet: the first install shows SmartScreen "Windows protected your PC";
  click **More info → Run anyway**.

## Build

GitHub Actions (`.github/workflows/windows.yml`) builds the installer on every push that
touches `windows-desktop/` and uploads it as the `ERP-Inventory-Windows` artifact.

Locally on Windows (needs Rust and Node 20+):

```
cd windows-desktop
npm ci
npm run build
```

Output: `src-tauri/target/release/bundle/nsis/ERP Inventory_<version>_x64-setup.exe`.
