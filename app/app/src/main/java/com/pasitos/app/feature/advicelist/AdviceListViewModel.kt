package com.pasitos.app.feature.advicelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pasitos.app.data.advice.AdviceRepository
import com.pasitos.app.data.advice.model.AdviceTip
import com.pasitos.app.data.child.ChildRepository
import com.pasitos.app.data.child.model.ageLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class AdviceListViewModel @Inject constructor(
    private val childRepository: ChildRepository,
    private val adviceRepository: AdviceRepository,
) : ViewModel() {

    val uiState = combine(
        childRepository.children,
        childRepository.activeChildId,
        adviceRepository.tips,
    ) { children, activeChildId, tips ->
        val activeChild = children.firstOrNull { it.id == activeChildId }
        AdviceListUiState(
            childName = activeChild?.name.orEmpty(),
            stageLabel = activeChild?.let { "¿Qué se viene? · ${it.ageLabel()}" } ?: "Consejos por edad",
            tips = tips.map { it.toUiModel() },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AdviceListUiState())

    private fun AdviceTip.toUiModel() = AdviceTipUiModel(
        id = id,
        category = category,
        title = title,
        summary = summary,
        emoji = emoji,
    )
}
