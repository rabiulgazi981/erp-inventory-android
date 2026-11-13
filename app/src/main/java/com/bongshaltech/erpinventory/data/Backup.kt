package com.bongshaltech.erpinventory.data

import android.content.Context
import android.net.Uri
import com.bongshaltech.erpinventory.ErpApp
import com.bongshaltech.erpinventory.security.DbKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.File
import java.security.SecureRandom
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * One backup file holds the shop database, its passphrase and the settings, all locked with a
 * password the owner chooses. The file can be saved anywhere the phone offers, Google Drive included,
 * and restored on any phone with the same password. Without the password the file is useless.
 */
object Backup {
    private val MAGIC = "ERPBK1".toByteArray()
    private const val ITERATIONS = 150_000
    private const val DB = "shop.db"

    sealed interface RestoreResult {
        data object Done : RestoreResult
        data object WrongPassword : RestoreResult
        data object NotABackup : RestoreResult
    }

    private fun key(password: String, salt: ByteArray): SecretKeySpec {
        // PBKDF2 with SHA-1 exists on every Android version the app supports (7.0+).
        val f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
        val bytes = f.generateSecret(PBEKeySpec(password.toCharArray(), salt, ITERATIONS, 256)).encoded
        return SecretKeySpec(bytes, "AES")
    }

    private fun settingsFile(ctx: Context) = File(ctx.filesDir, "datastore/settings.preferences_pb")

    suspend fun write(ctx: Context, uri: Uri, password: String) = withContext(Dispatchers.IO) {
        val app = ctx.applicationContext as ErpApp
        // Fold the write-ahead log into the main file so one file has everything.
        app.db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").use { it.moveToFirst() }
        val zip = ByteArrayOutputStream()
        ZipOutputStream(zip).use { z ->
            fun put(name: String, bytes: ByteArray) { z.putNextEntry(ZipEntry(name)); z.write(bytes); z.closeEntry() }
            put(DB, ctx.getDatabasePath(DB).readBytes())
            put("db.key", DbKey.get(ctx))
            settingsFile(ctx).takeIf { it.exists() }?.let { put("settings.preferences_pb", it.readBytes()) }
        }
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key(password, salt))
        val body = cipher.doFinal(zip.toByteArray())
        ctx.contentResolver.openOutputStream(uri)!!.use { out ->
            out.write(MAGIC); out.write(salt); out.write(cipher.iv); out.write(body)
        }
    }

    /** Replaces this phone's shop with the backup. The app must restart afterwards. */
    suspend fun restore(ctx: Context, uri: Uri, password: String): RestoreResult = withContext(Dispatchers.IO) {
        val all = ctx.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
        if (all.size < MAGIC.size + 28 || !all.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) return@withContext RestoreResult.NotABackup
        val input = DataInputStream(ByteArrayInputStream(all, MAGIC.size, all.size - MAGIC.size))
        val salt = ByteArray(16).also { input.readFully(it) }
        val iv = ByteArray(12).also { input.readFully(it) }
        val body = input.readBytes()
        val plain = try {
            Cipher.getInstance("AES/GCM/NoPadding").run {
                init(Cipher.DECRYPT_MODE, key(password, salt), GCMParameterSpec(128, iv)); doFinal(body)
            }
        } catch (e: javax.crypto.AEADBadTagException) {
            return@withContext RestoreResult.WrongPassword
        }
        val files = mutableMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(plain)).use { z ->
            while (true) { val e = z.nextEntry ?: break; files[e.name] = z.readBytes() }
        }
        val db = files[DB]; val dbKey = files["db.key"]
        if (db == null || dbKey == null) return@withContext RestoreResult.NotABackup

        (ctx.applicationContext as ErpApp).db.close()
        val path = ctx.getDatabasePath(DB)
        File(path.path + "-wal").delete(); File(path.path + "-shm").delete()
        path.writeBytes(db)
        DbKey.set(ctx, dbKey)
        files["settings.preferences_pb"]?.let { s -> settingsFile(ctx).apply { parentFile?.mkdirs() }.writeBytes(s) }
        RestoreResult.Done
    }
}
