package com.pasitos.app.data.account

import kotlinx.coroutines.flow.MutableStateFlow

/** Repositorio de cuenta en memoria: registra cada llamada y devuelve [result]. */
class FakeAccountRepository : AccountRepository {

    val calls = mutableListOf<String>()
    var result: Result<Unit> = Result.success(Unit)

    override val pendingTermsVersion = MutableStateFlow<String?>(null)

    override suspend fun refresh(): Result<Unit> = record("refresh")

    override suspend fun updateName(firstName: String, lastName: String) = record("updateName $firstName $lastName")

    override suspend fun requestEmailChange(newEmail: String, password: String) = record("requestEmailChange $newEmail")

    override suspend fun changePassword(currentPassword: String, newPassword: String) = record("changePassword")

    override suspend fun requestPasswordCreation() = record("requestPasswordCreation")

    override suspend fun acceptTerms(): Result<Unit> = record("acceptTerms").onSuccess { pendingTermsVersion.value = null }

    private fun record(call: String): Result<Unit> {
        calls += call
        return result
    }
}
