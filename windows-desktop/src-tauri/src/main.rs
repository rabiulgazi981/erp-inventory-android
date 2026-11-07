// Hide the console window on Windows release builds.
#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

use tauri::{Manager, WebviewUrl, WebviewWindowBuilder};
use tauri_plugin_opener::OpenerExt;

/// The live dashboard this app shows. Everything (features, data, login) comes from here,
/// so dashboard changes reach the app without a new installer.
const DASHBOARD_HOST: &str = "erpinventory.bd";

/// Links to these hosts stay inside the app; any other link opens in the default browser.
fn is_dashboard(url: &tauri::Url) -> bool {
    match url.scheme() {
        "tauri" | "about" | "data" | "blob" => true,
        "http" | "https" => url.host_str().is_some_and(|h| {
            h == DASHBOARD_HOST || h.ends_with(&format!(".{DASHBOARD_HOST}")) || h == "tauri.localhost"
        }),
        _ => false,
    }
}

fn main() {
    tauri::Builder::default()
        // A second click on the icon focuses the open window instead of opening another one.
        .plugin(tauri_plugin_single_instance::init(|app, _args, _cwd| {
            if let Some(w) = app.get_webview_window("main") {
                let _ = w.unminimize();
                let _ = w.set_focus();
            }
        }))
        .plugin(tauri_plugin_opener::init())
        .setup(|app| {
            let handle = app.handle().clone();
            let handle2 = app.handle().clone();
            WebviewWindowBuilder::new(app, "main", WebviewUrl::App("index.html".into()))
                .title("ERP Inventory")
                .inner_size(1280.0, 800.0)
                .min_inner_size(900.0, 600.0)
                .maximized(true)
                .center()
                // Lets the dashboard know it runs inside the Windows app (e.g. to hide the
                // "Download for Windows" button). Runs on every page before its own scripts.
                .initialization_script(concat!(
                    "window.erpDesktop = Object.freeze({ platform: 'windows', version: '",
                    env!("CARGO_PKG_VERSION"),
                    "' });"
                ))
                .on_navigation(move |url| {
                    if is_dashboard(url) {
                        true
                    } else {
                        let _ = handle.opener().open_url(url.as_str(), None::<&str>);
                        false
                    }
                })
                .on_new_window(move |url, _features| {
                    // Pop-ups (WhatsApp, Facebook, other sites) open in the default browser.
                    // Dashboard pop-ups (invoice print/PDF view) open as a normal app window.
                    if is_dashboard(&url) {
                        tauri::webview::NewWindowResponse::Allow
                    } else {
                        let _ = handle2.opener().open_url(url.as_str(), None::<&str>);
                        tauri::webview::NewWindowResponse::Deny
                    }
                })
                .build()?;
            Ok(())
        })
        .run(tauri::generate_context!())
        .expect("error while running ERP Inventory");
}
