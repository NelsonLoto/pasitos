package com.ninosano.app.data.child

import com.ninosano.app.data.child.model.Child
import com.ninosano.app.data.child.model.ChildSex
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@Singleton
class FakeChildRepository @Inject constructor() : ChildRepository {

    private val _children = MutableStateFlow<List<Child>>(emptyList())
    override val children: StateFlow<List<Child>> = _children.asStateFlow()

    private val _activeChildId = MutableStateFlow<String?>(null)
    override val activeChildId: StateFlow<String?> = _activeChildId.asStateFlow()

    override suspend fun addChild(name: String, birthDate: LocalDate, sex: ChildSex): Result<Child> {
        delay(800)
        val child = Child(
            id = "child-${_children.value.size + 1}",
            name = name.trim(),
            birthDate = birthDate,
            sex = sex,
        )
        _children.update { it + child }
        // El primer hijo cargado queda seleccionado automáticamente.
        if (_activeChildId.value == null) {
            _activeChildId.value = child.id
        }
        return Result.success(child)
    }

    override fun selectChild(childId: String) {
        _activeChildId.value = childId
    }
}
