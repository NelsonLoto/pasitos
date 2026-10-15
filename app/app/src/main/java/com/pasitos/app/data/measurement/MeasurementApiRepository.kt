package com.pasitos.app.data.measurement

import com.pasitos.app.data.auth.AuthRepository
import com.pasitos.app.data.child.ChildRepository
import com.pasitos.app.data.measurement.model.Measurement
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
class MeasurementApiRepository @Inject constructor(
    private val httpClient: HttpClient,
    private val authRepository: AuthRepository,
    private val childRepository: ChildRepository,
) : MeasurementRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _measurements = MutableStateFlow<List<Measurement>>(emptyList())
    override val measurements: StateFlow<List<Measurement>> = _measurements.asStateFlow()

    init {
        // No hay un endpoint "traeme todas las mediciones de todos mis hijos" -las rutas
        // son por hijo (GET /children/{id}/measurements)-, así que se refresca una por una
        // cada vez que cambia la lista de hijos (alta de hijo nuevo, login, logout).
        childRepository.children
            .onEach { children ->
                val token = authRepository.currentSession.value?.token
                if (token == null || children.isEmpty()) {
                    _measurements.value = emptyList()
                    return@onEach
                }
                _measurements.value = children.flatMap { child -> fetchMeasurements(child.id, token) }
            }
            .launchIn(repositoryScope)
    }

    private suspend fun fetchMeasurements(childId: String, token: String): List<Measurement> {
        val response = httpClient.get("/children/$childId/measurements") {
            bearerAuth(token)
        }
        return if (response.status == HttpStatusCode.OK) {
            response.body<List<MeasurementResponseDto>>().map { it.toDomain(childId) }
        } else {
            emptyList()
        }
    }

    override suspend fun addMeasurement(
        childId: String,
        date: LocalDate,
        weightKg: Double?,
        heightCm: Double?,
        headCircumferenceCm: Double?,
    ): Result<Measurement> {
        val token = authRepository.currentSession.value?.token
            ?: return Result.failure(Exception("No hay sesión activa"))

        return runCatching {
            val response = httpClient.post("/children/$childId/measurements") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(
                    CreateMeasurementRequestDto(
                        fechaMedicion = date.toString(),
                        pesoKg = weightKg,
                        tallaCm = heightCm,
                        perimetroCefalicoCm = headCircumferenceCm,
                    )
                )
            }
            when (response.status) {
                HttpStatusCode.Created -> response.body<MeasurementResponseDto>().toDomain(childId)
                else -> throw Exception("No se pudo guardar la medición")
            }
        }.onSuccess { measurement ->
            _measurements.value = _measurements.value + measurement
        }
    }

    private fun MeasurementResponseDto.toDomain(childId: String) = Measurement(
        id = id.toString(),
        childId = childId,
        date = LocalDate.parse(fechaMedicion),
        weightKg = pesoKg,
        heightCm = tallaCm,
        headCircumferenceCm = perimetroCefalicoCm,
    )
}
