//! Cash drawer: sends the ESC/POS "pulse" command to a receipt printer as a RAW print job
//! (CUPS `lp -o raw`). The drawer is wired to the printer, so the printer opens it. Called by
//! the dashboard (window.erpDesktop.openDrawer / listPrinters) after a sale receipt prints.

use std::io::Write;
use std::process::{Command, Stdio};

/// ESC p <pin> <on 50 ms> <off 500 ms>. Pin 0 = drawer connector pin 2, 1 = pin 5.
fn pulse(pin: u8) -> Result<[u8; 5], String> {
    match pin {
        0 | 1 => Ok([0x1B, 0x70, pin, 0x19, 0xFA]),
        _ => Err("pin must be 0 or 1".into()),
    }
}

pub fn list_printers() -> Result<Vec<String>, String> {
    let out = Command::new("/usr/bin/lpstat").arg("-e").output().map_err(|e| e.to_string())?;
    // `lpstat -e` exits non-zero when there are no printers.
    Ok(String::from_utf8_lossy(&out.stdout)
        .lines()
        .map(str::trim)
        .filter(|l| !l.is_empty())
        .map(String::from)
        .collect())
}

pub fn open_drawer(pin: u8, printer: Option<String>) -> Result<(), String> {
    let bytes = pulse(pin)?;
    let mut cmd = Command::new("/usr/bin/lp");
    // Without -d, lp uses the default printer.
    if let Some(p) = printer.filter(|p| !p.trim().is_empty()) {
        cmd.arg("-d").arg(p);
    }
    let mut child = cmd
        .args(["-o", "raw", "-t", "ERP Inventory cash drawer", "-s"])
        .stdin(Stdio::piped())
        .stdout(Stdio::null())
        .stderr(Stdio::piped())
        .spawn()
        .map_err(|e| e.to_string())?;
    child.stdin.take().unwrap().write_all(&bytes).map_err(|e| e.to_string())?;
    let out = child.wait_with_output().map_err(|e| e.to_string())?;
    if out.status.success() {
        Ok(())
    } else {
        let msg = String::from_utf8_lossy(&out.stderr).trim().to_string();
        Err(if msg.is_empty() { "Could not send to printer".into() } else { msg })
    }
}
