package com.aistudio.tdbassistant.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private const val TAG = "SecurityManager"
private const val KEY_ALIAS = "tdb_gemini_api_key_v3"
private const val PREFS_NAME = "tdb_secure_vault"
private const val KEY_CIPHER = "gemini_key_cipher"
private const val KEY_IV = "gemini_key_iv"
private const val KEY_VERIFIED_AT = "gemini_key_verified_at"
private const val KEY_VERIFIED_MODEL = "gemini_key_verified_model"

class SecurityManager(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun hasSavedApiKey(): Boolean = prefs.contains(KEY_CIPHER) && prefs.contains(KEY_IV)

    fun getLastVerifiedModel(): String = prefs.getString(KEY_VERIFIED_MODEL, "") ?: ""

    fun getLastVerifiedTimestamp(): Long = prefs.getLong(KEY_VERIFIED_AT, 0L)

    fun saveApiKey(rawKey: String): Boolean {
        val trimmed = rawKey.trim()
        if (trimmed.isEmpty()) return false
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
            val encrypted = cipher.doFinal(trimmed.toByteArray(StandardCharsets.UTF_8))
            prefs.edit()
                .putString(KEY_CIPHER, Base64.encodeToString(encrypted, Base64.NO_WRAP))
                .putString(KEY_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
                .commit()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to encrypt API key", e)
            false
        }
    }

    fun readApiKey(): String {
        val cipherB64 = prefs.getString(KEY_CIPHER, null) ?: return ""
        val ivB64 = prefs.getString(KEY_IV, null) ?: return ""
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val iv = Base64.decode(ivB64, Base64.NO_WRAP)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(128, iv))
            val plain = cipher.doFinal(Base64.decode(cipherB64, Base64.NO_WRAP))
            String(plain, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decrypt API key", e)
            ""
        }
    }

    fun markVerified(modelId: String) {
        prefs.edit()
            .putLong(KEY_VERIFIED_AT, System.currentTimeMillis())
            .putString(KEY_VERIFIED_MODEL, modelId)
            .commit()
    }

    fun clearVerification() {
        prefs.edit()
            .remove(KEY_VERIFIED_AT)
            .remove(KEY_VERIFIED_MODEL)
            .commit()
    }

    fun removeApiKey() {
        prefs.edit()
            .remove(KEY_CIPHER)
            .remove(KEY_IV)
            .remove(KEY_VERIFIED_AT)
            .remove(KEY_VERIFIED_MODEL)
            .commit()
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (keyStore.containsAlias(KEY_ALIAS)) {
            return keyStore.getKey(KEY_ALIAS, null) as SecretKey
        }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }
}
