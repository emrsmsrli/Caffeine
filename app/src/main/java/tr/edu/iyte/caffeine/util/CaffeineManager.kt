package tr.edu.iyte.caffeine.util

object CaffeineManager {

    var mode = CaffeineMode.INACTIVE
        private set

    fun changeMode() {
        when(mode) {
            CaffeineMode.INACTIVE,
            CaffeineMode.ONE_MIN,
            CaffeineMode.FIVE_MINS,
            CaffeineMode.TEN_MINS -> {
                mode = mode.next()
                Clock.set(mode.min)
            }
            CaffeineMode.INFINITE_MINS -> {
                mode = mode.next()
                Clock.reset()
            }
        }
    }

    fun reset() {
        mode = CaffeineMode.INACTIVE
    }

}