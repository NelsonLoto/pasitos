package com.pasitos.app.feature.professional

import androidx.lifecycle.ViewModel
import com.pasitos.app.core.time.ArgentinaClock
import com.pasitos.app.data.child.ChildRepository
import com.pasitos.app.data.child.model.Child
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class ProfessionalPatientsViewModel @Inject constructor(
    repository: ChildRepository,
    private val clock: ArgentinaClock,
) : ViewModel() {
    val patients: StateFlow<List<Child>> = repository.children
    fun today() = clock.today()
}
