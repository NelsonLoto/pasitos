package com.pasitos.app.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pasitos.app.data.auth.AuthRepository
import com.pasitos.app.core.database.PendingMutationDao
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import javax.inject.Provider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

@HiltViewModel
class SessionProfileViewModel @Inject constructor(private val auth: AuthRepository, private val pending: Provider<PendingMutationDao>) : ViewModel() {
    val confirm = MutableStateFlow(false)
    val warning = MutableStateFlow(false)
    fun requestLogout() { viewModelScope.launch {
        warning.value = withContext(Dispatchers.IO) { pending.get().observeCount().first() > 0 }
        confirm.value = true
    } }
    fun logout() { confirm.value = false; auth.logout() }
}

@Composable
fun SessionProfileScreen(viewModel: SessionProfileViewModel = hiltViewModel()) {
    val confirm by viewModel.confirm.collectAsState()
    val warning by viewModel.warning.collectAsState()
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Perfil", style = MaterialTheme.typography.headlineMedium)
        Button(onClick = viewModel::requestLogout) { Text("Cerrar sesión") }
    }
    if (confirm) AlertDialog(onDismissRequest = { viewModel.confirm.value = false },
        title = { Text("¿Cerrar sesión?") },
        text = { Text(if (warning) "Hay cambios sin sincronizar. Si cerrás sesión se borrarán del teléfono. ¿Querés continuar?" else "Se borrarán los datos de esta cuenta guardados en el teléfono.") },
        confirmButton = { TextButton(onClick = viewModel::logout) { Text("Cerrar sesión") } },
        dismissButton = { TextButton(onClick = { viewModel.confirm.value = false }) { Text("Cancelar") } })
}
