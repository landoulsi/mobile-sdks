package com.landoulsi.update.shared.model

sealed class UpdateState {
    object NoUpdate : UpdateState()
    data class UpdateRecommended(val config: UpdateConfig) : UpdateState()
    data class UpdateRequired(val config: UpdateConfig) : UpdateState()
    data class Error(val message: String) : UpdateState()
}
