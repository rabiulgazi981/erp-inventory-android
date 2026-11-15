fn main() {
    // App commands the dashboard may call; each gets an `allow-<name>` permission that
    // capabilities/dashboard.json grants to erpinventory.bd/dashboard pages only.
    tauri_build::try_build(
        tauri_build::Attributes::new()
            .app_manifest(tauri_build::AppManifest::new().commands(&["open_drawer", "list_printers"])),
    )
    .expect("failed to run tauri-build");
}
