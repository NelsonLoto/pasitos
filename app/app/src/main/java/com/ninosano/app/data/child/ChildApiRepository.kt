package com.ninosano.app.data.child

import com.ninosano.app.data.auth.AuthRepository
import com.ninosano.app.data.auth.ErrorResponseDto
import com.ninosano.app.data.child.model.Child
import com.ninosano.app.data.child.model.ChildSex
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@Singleton
class ChildApiRepository @Inject constructor(
    private val httpClient: HttpClient,
    private val authRepository: AuthRepository,
) : ChildRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _children = MutableStateFlow<List<Child>>(emptyList())
    override val children: StateFlow<List<Child>> = _children.asStateFlow()

    private val _activeChildId = MutableStateFlow<String?>(null)
    override val activeChildId: StateFlow<String?> = _activeChildId.asStateFlow()

    init {
        // No hay forma de "cargar" en la interfaz (el fake nunca lo necesitó, era todo en
        // memoria) — al reaccionar a la sesión, un hijo cargado en otra instalación/sesión
        // aparece solo. Al cerrar sesión se limpia todo para que un tutor distinto logueándose
        // después en el mismo dispositivo no vea hijos ajenos.
        authRepository.currentSession
            .onEach { session ->
                if (session == null) {
                    _children.value = emptyList()
                    _activeChildId.value = null
                } else {
                    refreshChildren(session.token)
                }
            }
            .launchIn(repositoryScope)
    }

    private suspend fun refreshChildren(token: String) {
        val response = httpClient.get("/children") {
            bearerAuth(token)
        }
        if (response.status == HttpStatusCode.OK) {
            val fetched = response.body<List<ChildResponseDto>>().map { it.toDomain() }
            _children.value = fetched
            if (_activeChildId.value == null) {
                _activeChildId.value = fetched.firstOrNull()?.id
            }
        }
    }

    override suspend fun addChild(name: String, birthDate: LocalDate, sex: ChildSex): Result<Child> {
        val token = authRepository.currentSession.value?.token
            ?: return Result.failure(Exception("No hay sesión activa"))

        return runCatching {
            val response = httpClient.post("/children") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(
                    CreateChildRequestDto(
                        nombre = name.trim(),
                        fechaNacimiento = birthDate.toString(),
                        sexo = sex.toWireSexo(),
                    )
                )
            }
            when (response.status) {
                HttpStatusCode.Created -> response.body<ChildResponseDto>().toDomain()
                HttpStatusCode.BadRequest -> throw Exception(response.body<ErrorResponseDto>().message)
                else -> throw Exception("No se pudo conectar con el servidor")
            }
        }.onSuccess { child ->
            _children.value = _children.value + child
            if (_activeChildId.value == null) {
                _activeChildId.value = child.id
            }
        }
    }

    override fun selectChild(childId: String) {
        _activeChildId.value = childId
    }

    private fun ChildResponseDto.toDomain() = Child(
        id = id,
        name = nombre,
        birthDate = LocalDate.parse(fechaNacimiento),
        sex = sexo.toChildSex(),
    )

    private fun ChildSex.toWireSexo() = when (this) {
        ChildSex.Female -> "F"
        ChildSex.Male -> "M"
    }

    private fun String.toChildSex() = when (this) {
        "F" -> ChildSex.Female
        else -> ChildSex.Male
    }
}
