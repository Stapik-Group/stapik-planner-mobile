package pl.stapik.planner.ui.plan

enum class EditNotice { LOAD_LIMIT }

data class EditUiState(
    val isEditing: Boolean = false,
    val notice: EditNotice? = null
)
