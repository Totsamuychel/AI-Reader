package com.bookmind.account

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/** A signed-in user. = Firebase `FirebaseUser` (subset we use). */
data class Account(val email: String, val displayName: String? = null)

/**
 * Authentication surface. = plan's `AuthRepository`.
 *
 * The shipped implementation is [LocalAccountRepository], which authenticates
 * against an on-device credential store so the feature works without a backend.
 * Swapping in Firebase Auth means providing a `FirebaseAccountRepository` that
 * implements this same interface and binding it in the DI module (and adding the
 * `firebase-auth-ktx` dependency + `google-services.json`).
 */
interface AccountRepository {
    val currentUser: StateFlow<Account?>
    suspend fun signIn(email: String, password: String): Result<Account>
    suspend fun signUp(email: String, password: String): Result<Account>
    fun signOut()
}

/**
 * Local, backend-free auth: credentials are stored AES-encrypted on device. Good
 * enough to gate premium UI and exercise the account flows before a cloud backend
 * is wired up.
 */
@Singleton
class LocalAccountRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : AccountRepository {

    private val prefs: SharedPreferences by lazy { open() }

    private val _currentUser = MutableStateFlow(restoreSession())
    override val currentUser: StateFlow<Account?> = _currentUser.asStateFlow()

    override suspend fun signIn(email: String, password: String): Result<Account> {
        val normalized = email.trim().lowercase()
        if (normalized.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("Введите email и пароль"))
        }
        val stored = prefs.getString(passwordKey(normalized), null)
            ?: return Result.failure(IllegalStateException("Аккаунт не найден — зарегистрируйтесь"))
        if (stored != hash(password)) {
            return Result.failure(IllegalStateException("Неверный пароль"))
        }
        return Result.success(activate(normalized))
    }

    override suspend fun signUp(email: String, password: String): Result<Account> {
        val normalized = email.trim().lowercase()
        if (!normalized.contains("@") || password.length < 6) {
            return Result.failure(IllegalArgumentException("Нужен корректный email и пароль от 6 символов"))
        }
        if (prefs.contains(passwordKey(normalized))) {
            return Result.failure(IllegalStateException("Такой аккаунт уже существует"))
        }
        prefs.edit().putString(passwordKey(normalized), hash(password)).apply()
        return Result.success(activate(normalized))
    }

    override fun signOut() {
        prefs.edit().remove(KEY_SESSION).apply()
        _currentUser.value = null
    }

    private fun activate(email: String): Account {
        prefs.edit().putString(KEY_SESSION, email).apply()
        return Account(email).also { _currentUser.value = it }
    }

    private fun restoreSession(): Account? =
        prefs.getString(KEY_SESSION, null)?.let { Account(it) }

    private fun passwordKey(email: String) = "pw_$email"

    private fun hash(password: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(password.toByteArray())
            .joinToString("") { "%02x".format(it) }

    private fun open(): SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "bookmind_accounts",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (t: Throwable) {
        context.getSharedPreferences("bookmind_accounts_fallback", Context.MODE_PRIVATE)
    }

    private companion object {
        const val KEY_SESSION = "session_email"
    }
}
