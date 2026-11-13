# ERP Inventory for macOS

A small macOS app (Tauri + the Mac's own WebKit) that opens the live dashboard at
https://erpinventory.bd. All features and data come from the server, so dashboard changes
reach the app without a new installer. It is the Mac twin of `windows-desktop/`.

- Installer: `.dmg` with a universal app (Apple Silicon M1–M4 and Intel), macOS 11.3 or newer.
  Drag **ERP Inventory** into **Applications**.
- Login is kept in the app's own web profile, so the user stays signed in between launches
  for as long as the server session allows.
- Links to other sites (WhatsApp, Facebook, ...) open in the default browser.
- `window.print()` opens the Mac Print dialog (WebKit ignores it in apps on its own).
  Receipt pop-ups stay open after printing; close them with ⌘W.
- Downloads (PDF, Excel) are saved to **Downloads** and opened.
- ⌘+ / ⌘- / ⌘0 zoom. Sets `window.erpDesktop = { platform: 'macos', version }` on every page.
- Not notarized (no Apple Developer account yet), only ad-hoc signed. The first open shows
  "Apple could not verify…". Click **Done**, then System Settings → Privacy & Security →
  **Open Anyway** (on macOS 14 and older, right-click the app → Open also works).

## Build

GitHub Actions (`.github/workflows/mac.yml`) builds on every push that touches
`mac-desktop/` and uploads the `ERP-Inventory-Mac` artifact (the `.dmg`).

Locally on a Mac (needs Rust with both Apple targets and Node 20+):

```
rustup target add aarch64-apple-darwin x86_64-apple-darwin
cd mac-desktop
npm ci
npm run build -- --target universal-apple-darwin --bundles dmg
```

Output: `src-tauri/target/universal-apple-darwin/release/bundle/dmg/ERP Inventory_<version>_universal.dmg`.
