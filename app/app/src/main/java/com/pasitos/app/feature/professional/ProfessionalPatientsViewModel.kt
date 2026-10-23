package com.pasitos.app.feature.professional

import androidx.lifecycle.ViewModel
import com.pasitos.app.data.child.ChildRepository
import com.pasitos.app.data.child.model.Child
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class ProfessionalPatientsViewModel @Inject constructor(repository: ChildRepository) : ViewModel() {
    val patients: StateFlow<List<Child>> = repository.children
}
