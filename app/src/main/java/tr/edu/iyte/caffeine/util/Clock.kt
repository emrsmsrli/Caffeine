package tr.edu.iyte.caffeine.util

import android.os.CountDownTimer
import org.jetbrains.anko.AnkoLogger

object Clock {
    val FULL = 100
    val THIRTY_THREE = 33
    val SIXTY_SIX = 66

    private var min: Int = 0
    private var sec: Int = 0

    private var timer: TimerObject.Timer? = null
    var listener: ClockListener? = null

    /**
     * Sets [Clock.min] to the [min], [Clock.sec] to 0.
     * @param min Minutes to be set
    */
    fun set(min: Int) {
        Clock.min = min
        sec = 0
        timer?.cancel()
        timer = TimerObject.Timer(min)
        timer?.start()
    }

    /**
     * Resets the clock
     */
    fun reset() {
        min = 0
        sec = 0
        timer?.cancel()
        listener?.onFinish()
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
        val perc = totalsec.toFloat() / (CaffeineManager.mode.min * 60) * 100
        if (perc <= THIRTY_THREE)
            return THIRTY_THREE
        else if (perc <= SIXTY_SIX)
            return SIXTY_SIX
        else
            return FULL
    }

    /**
     * @return A human readable form of a [Clock] object. Format is always *(m)m:ss*.
     */
    override fun toString(): String {
        if(CaffeineManager.mode == CaffeineMode.INFINITE_MINS)
            return CaffeineManager.mode.label
        if (sec < 10)
            return "$min:0$sec"
        return "$min:$sec"
    }

    internal object TimerObject {
        const val SEC_IN_MILLIS = 1000L
        const val MIN_IN_MILLIS = 60 * SEC_IN_MILLIS

        class Timer(min: Int) :
                CountDownTimer(min * MIN_IN_MILLIS, SEC_IN_MILLIS), AnkoLogger {
            override fun onTick(millisUntilFinished: Long) {
                decrement()
                listener?.onTick()
            }

            override fun onFinish() {
                CaffeineManager.reset()
                listener?.onFinish()
            }
        }
    }
}