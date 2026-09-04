package pl.stapik.planner.data.model

enum class Difficulty(val wireValue: String, val loadPercent: Int) {
    LIGHT("light", 10),
    NORMAL("normal", 20),
    MEDIUM("medium", 30),
    HARD("hard", 50);

    companion object {
        fun fromWireValue(value: String): Difficulty = entries.find { it.wireValue == value } ?: NORMAL
    }
}
