package pl.stapik.planner.data.model

val ActivityDef?.checkSignature: String
    get() = when {
        this == null -> ""
        !id.isNullOrBlank() -> id
        else -> "$name|$difficulty"
    }
