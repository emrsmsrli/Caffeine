package tr.edu.iyte.caffeine.util

object Clock {
    private val THIRTY_THREE = 33
    private val SIXTY_SIX = 66

    private var min: Int = 0
    private var sec: Int = 0

    /**
     * Sets [Clock.min] to the [min], [Clock.sec] to 0.
     * @param min Minutes to be set
    */
    fun set(min: Int) {
        Clock.min = min
        sec = 0
    }

    /**
     * Decrements the clock by one second.
     */
    fun decrement() {
        if (sec == 0) {
            sec = 60
            --min
        }
        --sec
    }

    /**
     * Checks if the clock is 0:00.
     * @return *true* if clock is finished, *false* otherwise.
     */
    fun isFinished() = min == 0 && sec == 0

    /**
     * Returns the remaining percentage of the clock for the current [CaffeineMode].
     * @return [THIRTY_THREE] for %33 percentage, [SIXTY_SIX] for %66 percentage, *0* otherwise.
     */
    fun getPercentage(): Int {
        val totalsec = min * 60 + sec
        val perc = totalsec.toFloat() / /*(CaffeineManager.mode.min * 60) * */ 100
        if (perc <= THIRTY_THREE)
            return THIRTY_THREE
        else if (perc <= SIXTY_SIX)
            return SIXTY_SIX
        else
            return 0
    }

    /**
     * @return A human readable form of a [Clock] object. Format is always *(m)m:ss*.
     */
    override fun toString(): String {
        if (sec < 10)
            return "$min:0$sec"
        return "$min:$sec"
    }
}