package com.bongshaltech.erpinventory.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Small secrets (sign-in tokens) kept encrypted by an Android Keystore key that never leaves the phone. */
object SecretStore {
    private const val ALIAS = "erp_secret_store"
    private const val PREFS = "secrets"

    fun put(ctx: Context, name: String, value: String?) {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        if (value == null) { prefs.remove(name).commit(); return }
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, key())
        prefs.putString(name, Base64.encodeToString(c.iv, Base64.NO_WRAP) + ":" +
            Base64.encodeToString(c.doFinal(value.toByteArray()), Base64.NO_WRAP)).commit()
    }

    fun get(ctx: Context, name: String): String? {
        val stored = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(name, null) ?: return null
        return runCatching {
            val (iv, data) = stored.split(":").map { Base64.decode(it, Base64.NO_WRAP) }
            val c = Cipher.getInstance("AES/GCM/NoPadding")
            c.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
            String(c.doFinal(data))
        }.getOrNull()
    }

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).build()
        )
        return gen.generateKey()
    }
}
