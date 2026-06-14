package com.bookmind.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookmind.account.Account
import com.bookmind.account.AccountRepository
import com.bookmind.account.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val isSignUp: Boolean = false,
    val isWorking: Boolean = false,
    val error: String? = null,
    val account: Account? = null,
    val isPremium: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val accounts: AccountRepository,
    private val subscriptions: SubscriptionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            accounts.currentUser.collect { account ->
                _uiState.update { it.copy(account = account) }
            }
        }
        viewModelScope.launch {
            subscriptions.isPremium.collect { premium ->
                _uiState.update { it.copy(isPremium = premium) }
            }
        }
    }

    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value, error = null) }
    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, error = null) }
    fun toggleMode() = _uiState.update { it.copy(isSignUp = !it.isSignUp, error = null) }

    fun submit(onSuccess: () -> Unit) {
        val state = _uiState.value
        _uiState.update { it.copy(isWorking = true, error = null) }
        viewModelScope.launch {
            val result = if (state.isSignUp) {
                accounts.signUp(state.email, state.password)
            } else {
                accounts.signIn(state.email, state.password)
            }
            result
                .onSuccess {
                    _uiState.update { s -> s.copy(isWorking = false, password = "") }
                    onSuccess()
                }
                .onFailure { t ->
                    _uiState.update { s -> s.copy(isWorking = false, error = t.message) }
                }
        }
    }

    fun signOut() = accounts.signOut()

    fun upgrade() {
        viewModelScope.launch { subscriptions.purchase() }
    }
}
