package com.bongshaltech.erpinventory.ui

import android.content.Context
import android.widget.Toast
import com.bongshaltech.erpinventory.R
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

/**
 * Opens Google's barcode scanner. It runs inside Google Play services, so the app needs no
 * camera permission and the picture never reaches the app; only the scanned code does.
 */
fun scanBarcode(ctx: Context, onCode: (String) -> Unit) {
    val options = GmsBarcodeScannerOptions.Builder().enableAutoZoom().build()
    GmsBarcodeScanning.getClient(ctx, options).startScan()
        .addOnSuccessListener { b -> b.rawValue?.trim()?.takeIf { it.isNotEmpty() }?.let(onCode) }
        .addOnFailureListener { Toast.makeText(ctx, ctx.getString(R.string.scan_failed), Toast.LENGTH_LONG).show() }
}
