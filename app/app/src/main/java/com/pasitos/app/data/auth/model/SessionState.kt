package com.pasitos.app.data.auth.model

import com.pasitos.shared.api.auth.AccountResponse

sealed interface SessionState {
    data object Restoring : SessionState
    data class SignedIn(val account: AccountResponse) : SessionState
    data class SignedOut(val expired: Boolean = false) : SessionState
}
