package com.bookmind.account

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Premium-entitlement surface. = plan's `SubscriptionRepository` (RevenueCat).
 *
 * The shipped [LocalSubscriptionRepository] persists a single local flag so the
 * premium-gated UI is exercisable without a billing backend. Wiring RevenueCat
 * means a `RevenueCatSubscriptionRepository` implementing this interface, plus the
 * `purchases` dependency and `Purchases.configure(...)` in `App`.
 */
interface SubscriptionRepository {
    val isPremium: StateFlow<Boolean>
    /** Launches purchase flow; returns true if the user is now premium. */
    suspend fun purchase(): Boolean
    fun restore()
}

/** Local stub: a developer/QA toggle standing in for a real paywall. */
@Singleton
class LocalSubscriptionRepository @Inject constructor(
    @ApplicationContext context: Context
) : SubscriptionRepository {

    private val prefs = context.getSharedPreferences("bookmind_subscription", Context.MODE_PRIVATE)
    private val _isPremium = MutableStateFlow(prefs.getBoolean(KEY_PREMIUM, false))
    override val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    override suspend fun purchase(): Boolean {
        // Real impl: RevenueCatUI.presentPaywallIfNeeded() → entitlement check.
        prefs.edit().putBoolean(KEY_PREMIUM, true).apply()
        _isPremium.value = true
        return true
    }

    override fun restore() {
        _isPremium.value = prefs.getBoolean(KEY_PREMIUM, false)
    }

    private companion object {
        const val KEY_PREMIUM = "is_premium"
    }
}
