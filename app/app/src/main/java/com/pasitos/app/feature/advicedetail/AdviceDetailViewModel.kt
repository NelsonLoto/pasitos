package com.pasitos.app.feature.advicedetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.navigation.toRoute
import com.pasitos.app.core.navigation.NsRoute
import com.pasitos.app.data.advice.AdviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class AdviceDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    adviceRepository: AdviceRepository,
) : ViewModel() {

    private val adviceId: String = savedStateHandle.toRoute<NsRoute.AdviceDetail>().adviceId
    private val tip = adviceRepository.tips.value.first { it.id == adviceId }

    private val _uiState = MutableStateFlow(
        AdviceDetailUiState(
            badgeText = "${tip.category} · ${tip.ageRangeLabel.uppercase()}",
            title = tip.title,
            emoji = tip.emoji,
            body = tip.body,
        ),
    )
    val uiState: StateFlow<AdviceDetailUiState> = _uiState.asStateFlow()
}
