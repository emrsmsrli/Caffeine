package tr.edu.iyte.caffeine

enum class CaffeineMode(val label: String, val min: Int) {
    INACTIVE("Caffeine", 0),
    ONE_MIN("1:00", 1),
    FIVE_MINS("5:00", 5),
    TEN_MINS("10:00", 10),
    THIRTY_MINS("30:00", 30),
    ONE_HOUR("60:00", 60),
    INFINITE_MINS("\u221E", Int.MAX_VALUE);

    fun next() = when(this) {
        INACTIVE      -> ONE_MIN
        ONE_MIN       -> FIVE_MINS
        FIVE_MINS     -> TEN_MINS
        TEN_MINS      -> THIRTY_MINS
        THIRTY_MINS   -> ONE_HOUR
        ONE_HOUR      -> INFINITE_MINS
        INFINITE_MINS -> INACTIVE
    }
}