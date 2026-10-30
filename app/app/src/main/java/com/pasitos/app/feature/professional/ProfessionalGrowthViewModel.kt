package com.pasitos.app.feature.professional

import androidx.lifecycle.ViewModel
import com.pasitos.app.core.time.ArgentinaClock
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ProfessionalGrowthViewModel @Inject constructor(
    private val clock: ArgentinaClock,
) : ViewModel() {
    fun today() = clock.today()
}
