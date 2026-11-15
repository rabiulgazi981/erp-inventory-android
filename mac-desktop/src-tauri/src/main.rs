// ERP Inventory for macOS: a window that shows the live dashboard at erpinventory.bd.

use std::sync::atomic::{AtomicUsize, Ordering};

use tauri::webview::{DownloadEvent, NewWindowFeatures, NewWindowResponse};
use tauri::{AppHandle, Manager, Url, WebviewUrl, WebviewWindow, WebviewWindowBuilder};
use tauri_plugin_opener::OpenerExt;

/// The live dashboard this app shows. Everything (features, data, login) comes from here,
/// so dashboard changes reach the app without a new installer.
const DASHBOARD_HOST: &str = "erpinventory.bd";

/// The page asks for printing by navigating to this scheme (see `PAGE_SCRIPT`). The macOS web
/// view ignores `window.print()` on its own, so the app catches this and opens the Print dialog.
const PRINT_SCHEME: &str = "erpprint";

/// Runs on every page before its own scripts.
/// - `window.erpDesktop` lets the dashboard know it runs inside the app (it hides its
///   "Download" buttons there).
/// - `window.print()` is routed to the native Print dialog. Receipt pop-ups that print and then
///   close themselves stay open until printing is done; the user closes them with ⌘W.
const PAGE_SCRIPT: &str = concat!(
    "window.erpDesktop = Object.freeze({ platform: 'macos', version: '",
    env!("CARGO_PKG_VERSION"),
    "' });",
    r#"
(function () {
  var printed = false;
  window.print = function () { printed = true; window.location.href = 'erpprint://print'; };
  var close = window.close.bind(window);
  window.close = function () { if (!printed) close(); };
})();
"#
);

static NEXT_WINDOW: AtomicUsize = AtomicUsize::new(1);

/// Links to these hosts stay inside the app; any other link opens in the default browser.
fn is_dashboard(url: &Url) -> bool {
    match url.scheme() {
        "tauri" | "about" | "data" | "blob" => true,
        "http" | "https" => url.host_str().is_some_and(|h| {
            h == DASHBOARD_HOST || h.ends_with(&format!(".{DASHBOARD_HOST}")) || h == "tauri.localhost"
        }),
        _ => false,
    }
}

/// Builds the main window or a pop-up (invoice print, POS customer display, ...). Every window
/// gets the same rules for links, printing and downloads.
fn open_window(
    app: &AppHandle,
    label: String,
    url: WebviewUrl,
    features: Option<NewWindowFeatures>,
) -> tauri::Result<WebviewWindow> {
    let nav_app = app.clone();
    let nav_label = label.clone();
    let popup_app = app.clone();
    let download_app = app.clone();

    let mut builder = WebviewWindowBuilder::new(app, label.clone(), url)
        .title("ERP Inventory")
        .initialization_script(PAGE_SCRIPT)
        // ⌘+ / ⌘- / ⌘0 zoom like a browser.
        .zoom_hotkeys_enabled(true)
        .on_navigation(move |url| {
            if url.scheme() == PRINT_SCHEME {
                if let Some(w) = nav_app.get_webview_window(&nav_label) {
                    let _ = w.print();
                }
                return false;
            }
            if is_dashboard(url) {
                true
            } else {
                let _ = nav_app.opener().open_url(url.as_str(), None::<&str>);
                false
            }
        })
        .on_new_window(move |url, features| {
            // Dashboard pop-ups open as an app window; other sites (WhatsApp, Facebook, ...)
            // open in the default browser.
            if !is_dashboard(&url) {
                let _ = popup_app.opener().open_url(url.as_str(), None::<&str>);
                return NewWindowResponse::Deny;
            }
            let label = format!("popup-{}", NEXT_WINDOW.fetch_add(1, Ordering::Relaxed));
            match open_window(
                &popup_app,
                label,
                WebviewUrl::External("about:blank".parse().unwrap()),
                Some(features),
            ) {
                Ok(window) => NewWindowResponse::Create { window },
                Err(_) => NewWindowResponse::Deny,
            }
        })
        .on_download(move |_webview, event| {
            // Files (PDF, Excel) are saved to the Downloads folder and then opened.
            if let DownloadEvent::Finished { path: Some(path), success: true, .. } = event {
                let _ = download_app
                    .opener()
                    .open_path(path.to_string_lossy(), None::<&str>);
            }
            true
        });

    builder = match features {
        Some(features) => builder.window_features(features).on_document_title_changed(|w, title| {
            let _ = w.set_title(&title);
        }),
        None => builder
            .inner_size(1280.0, 800.0)
            .min_inner_size(900.0, 600.0)
            .maximized(true)
            .center(),
    };
    builder.build()
}

fn main() {
    tauri::Builder::default()
        .plugin(tauri_plugin_opener::init())
        .setup(|app| {
            open_window(app.handle(), "main".into(), WebviewUrl::App("index.html".into()), None)?;
            Ok(())
        })
        .run(tauri::generate_context!())
        .expect("error while running ERP Inventory");
}
