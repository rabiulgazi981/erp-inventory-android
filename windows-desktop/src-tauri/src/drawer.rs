//! Cash drawer: sends the ESC/POS "pulse" command to a receipt printer as a RAW print job.
//! The drawer is wired to the printer, so the printer opens it. Called by the dashboard
//! (window.erpDesktop.openDrawer / listPrinters) after a sale receipt prints.

use std::ffi::c_void;
use std::ptr::{null, null_mut};

/// ESC p <pin> <on 50 ms> <off 500 ms>. Pin 0 = drawer connector pin 2, 1 = pin 5.
fn pulse(pin: u8) -> Result<[u8; 5], String> {
    match pin {
        0 | 1 => Ok([0x1B, 0x70, pin, 0x19, 0xFA]),
        _ => Err("pin must be 0 or 1".into()),
    }
}

type Handle = *mut c_void;

#[repr(C)]
struct DocInfo1W {
    doc_name: *const u16,
    output_file: *const u16,
    datatype: *const u16,
}

#[repr(C)]
struct PrinterInfo4W {
    printer_name: *const u16,
    server_name: *const u16,
    attributes: u32,
}

#[link(name = "winspool")]
extern "system" {
    fn OpenPrinterW(name: *const u16, handle: *mut Handle, defaults: *const c_void) -> i32;
    fn ClosePrinter(handle: Handle) -> i32;
    fn StartDocPrinterW(handle: Handle, level: u32, doc: *const DocInfo1W) -> u32;
    fn EndDocPrinter(handle: Handle) -> i32;
    fn StartPagePrinter(handle: Handle) -> i32;
    fn EndPagePrinter(handle: Handle) -> i32;
    fn WritePrinter(handle: Handle, buf: *const c_void, len: u32, written: *mut u32) -> i32;
    fn GetDefaultPrinterW(buf: *mut u16, len: *mut u32) -> i32;
    fn EnumPrintersW(
        flags: u32,
        name: *const u16,
        level: u32,
        buf: *mut u8,
        len: u32,
        needed: *mut u32,
        returned: *mut u32,
    ) -> i32;
}

const PRINTER_ENUM_LOCAL: u32 = 0x2;
const PRINTER_ENUM_CONNECTIONS: u32 = 0x4;

fn wide(s: &str) -> Vec<u16> {
    s.encode_utf16().chain(std::iter::once(0)).collect()
}

unsafe fn from_wide(p: *const u16) -> String {
    if p.is_null() {
        return String::new();
    }
    let mut n = 0;
    while *p.add(n) != 0 {
        n += 1;
    }
    String::from_utf16_lossy(std::slice::from_raw_parts(p, n))
}

fn default_printer() -> Result<String, String> {
    unsafe {
        let mut len = 0u32;
        GetDefaultPrinterW(null_mut(), &mut len);
        if len == 0 {
            return Err("No default printer. Choose the receipt printer in Settings.".into());
        }
        let mut buf = vec![0u16; len as usize];
        if GetDefaultPrinterW(buf.as_mut_ptr(), &mut len) == 0 {
            return Err("No default printer. Choose the receipt printer in Settings.".into());
        }
        Ok(from_wide(buf.as_ptr()))
    }
}

pub fn list_printers() -> Result<Vec<String>, String> {
    unsafe {
        let flags = PRINTER_ENUM_LOCAL | PRINTER_ENUM_CONNECTIONS;
        let (mut needed, mut count) = (0u32, 0u32);
        EnumPrintersW(flags, null(), 4, null_mut(), 0, &mut needed, &mut count);
        if needed == 0 {
            return Ok(vec![]);
        }
        // u64 buffer keeps the PRINTER_INFO_4W array aligned.
        let mut buf = vec![0u64; (needed as usize).div_ceil(8)];
        if EnumPrintersW(flags, null(), 4, buf.as_mut_ptr().cast(), needed, &mut needed, &mut count) == 0 {
            return Err("Could not list printers".into());
        }
        let infos = std::slice::from_raw_parts(buf.as_ptr().cast::<PrinterInfo4W>(), count as usize);
        Ok(infos.iter().map(|i| from_wide(i.printer_name)).collect())
    }
}

pub fn open_drawer(pin: u8, printer: Option<String>) -> Result<(), String> {
    let bytes = pulse(pin)?;
    let name = match printer.filter(|p| !p.trim().is_empty()) {
        Some(p) => p,
        None => default_printer()?,
    };
    let wname = wide(&name);
    let doc_name = wide("ERP Inventory cash drawer");
    let datatype = wide("RAW");
    unsafe {
        let mut h: Handle = null_mut();
        if OpenPrinterW(wname.as_ptr(), &mut h, null()) == 0 {
            return Err(format!("Printer not found: {name}"));
        }
        let doc = DocInfo1W { doc_name: doc_name.as_ptr(), output_file: null(), datatype: datatype.as_ptr() };
        let mut ok = false;
        if StartDocPrinterW(h, 1, &doc) != 0 {
            if StartPagePrinter(h) != 0 {
                let mut written = 0u32;
                ok = WritePrinter(h, bytes.as_ptr().cast(), bytes.len() as u32, &mut written) != 0
                    && written == bytes.len() as u32;
                EndPagePrinter(h);
            }
            EndDocPrinter(h);
        }
        ClosePrinter(h);
        if ok { Ok(()) } else { Err(format!("Could not send to printer: {name}")) }
    }
}
