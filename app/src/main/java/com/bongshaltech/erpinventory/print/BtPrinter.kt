package com.bongshaltech.erpinventory.print

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.UUID

data class PairedPrinter(val name: String, val address: String, val looksLikePrinter: Boolean)

sealed interface PrintResult {
    data object Done : PrintResult
    data object BluetoothOff : PrintResult
    data object NoBluetooth : PrintResult
    data class Failed(val reason: String) : PrintResult
}

/** Prints over classic Bluetooth (serial port profile), which almost all receipt printers use. */
object BtPrinter {
    private val SPP: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    /** Android 12+ asks for "Nearby devices" before the app can talk to a paired printer. */
    val permission: String? = if (Build.VERSION.SDK_INT >= 31) Manifest.permission.BLUETOOTH_CONNECT else null

    fun hasPermission(ctx: Context) =
        permission == null || ContextCompat.checkSelfPermission(ctx, permission) == PackageManager.PERMISSION_GRANTED

    private fun adapter(ctx: Context) = ctx.getSystemService(BluetoothManager::class.java)?.adapter

    fun isOn(ctx: Context) = adapter(ctx)?.isEnabled == true

    @SuppressLint("MissingPermission")
    fun paired(ctx: Context): List<PairedPrinter> {
        if (!hasPermission(ctx)) return emptyList()
        val a = adapter(ctx) ?: return emptyList()
        return runCatching {
            a.bondedDevices.map {
                PairedPrinter(
                    name = it.name ?: it.address,
                    address = it.address,
                    looksLikePrinter = it.bluetoothClass?.majorDeviceClass == BluetoothClass.Device.Major.IMAGING,
                )
            }.sortedWith(compareByDescending<PairedPrinter> { it.looksLikePrinter }.thenBy { it.name.lowercase() })
        }.getOrDefault(emptyList())
    }

    @SuppressLint("MissingPermission")
    suspend fun print(ctx: Context, address: String, bytes: ByteArray): PrintResult = withContext(Dispatchers.IO) {
        val a = adapter(ctx) ?: return@withContext PrintResult.NoBluetooth
        if (!a.isEnabled) return@withContext PrintResult.BluetoothOff
        try {
            val socket = a.getRemoteDevice(address).createRfcommSocketToServiceRecord(SPP)
            socket.use { s ->
                s.connect()
                val out = s.outputStream
                // Small writes so slow printers keep up.
                var i = 0
                while (i < bytes.size) {
                    val n = minOf(1024, bytes.size - i)
                    out.write(bytes, i, n)
                    out.flush()
                    i += n
                    delay(15)
                }
                // Let the printer take the last bytes before the link closes.
                delay(400)
            }
            PrintResult.Done
        } catch (e: Exception) {
            PrintResult.Failed(e.message ?: e.javaClass.simpleName)
        }
    }
}
