package com.bookmind.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AES-encrypted storage for AI provider API keys, kept out of the plaintext
 * DataStore. Keys are namespaced per [AiModel] so switching providers preserves
 * each one's credential. Falls back to in-memory storage if the platform keystore
 * is unavailable (e.g. some emulators), so the app never crashes on settings.
 */
@Singleton
class SecureKeyStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences by lazy { open() }
    private val fallback = HashMap<String, String>()
    private var usingFallback = false

    private fun open(): SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "bookmind_secure_keys",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (t: Throwable) {
        usingFallback = true
        context.getSharedPreferences("bookmind_secure_keys_fallback", Context.MODE_PRIVATE)
    }

    fun apiKey(model: AiModel): String =
        if (usingFallback) fallback[model.name].orEmpty()
        else prefs.getString(model.name, "").orEmpty()

    fun setApiKey(model: AiModel, key: String) {
        if (usingFallback) {
            fallback[model.name] = key
            return
        }
        prefs.edit().putString(model.name, key).apply()
    }

    fun hasApiKey(model: AiModel): Boolean = apiKey(model).isNotBlank()
}
