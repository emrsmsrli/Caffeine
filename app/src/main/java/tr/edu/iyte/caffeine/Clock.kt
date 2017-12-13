package tr.edu.iyte.caffeine

import android.content.Context
import android.os.CountDownTimer
import java.lang.ref.WeakReference

object Clock {
    interface ClockListener {
        fun onTick()
        fun onFinish()
    }

    enum class Percentage(val value: Int) {
        FULL(100),
        SIXTY_SIX(66),
        THIRTY_THREE(33)
    }

    private var min: Int = 0
    private var sec: Int = 0
    private var isCancelled = false
    private var isPaused = false

    private var timer: TimerObject.Timer? = null
    var listener: ClockListener? = null

    /**
     * Sets [Clock.min] to the [min], [Clock.sec] to 0.
     * @param min Minutes to be set
    */
    fun set(context: Context, min: Int) {
        Clock.min = min
        sec = 0
        timer?.cancel()
        timer = TimerObject.Timer(min, context)
        timer?.start()
        isCancelled = false
    }

    /**
     * Resets the clock
     */
    fun reset() {
        min = 0
        sec = 0
        timer?.cancel()
        listener?.onFinish()
        isCancelled = true
    }

    /**
     * Pauses the clock
     */
    fun pause() {
        if(!isCancelled && !isPaused) {
            timer?.cancel()
            isPaused = true
        }
    }

    /**
     * Resumes the clock
     */
    fun resume(context: Context) {
        if(!isCancelled && isPaused) {
            timer = TimerObject.Timer(min, context)
            timer?.start()
            isPaused = false
        }
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
     * @return [Percentage.THIRTY_THREE] for %33 percentage,
     * [Percentage.SIXTY_SIX] for %66 percentage, [Percentage.FULL] otherwise.
     */
    fun getPercentage(): Percentage {
        val totalsec = min * 60 + sec
        val perc = totalsec.toFloat() / (CaffeineManager.mode.min * 60) * 100
        return when {
            perc <= Percentage.THIRTY_THREE.value -> Percentage.THIRTY_THREE
            perc <= Percentage.SIXTY_SIX.value    -> Percentage.SIXTY_SIX
            else                                                             -> Percentage.FULL
        }
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

        class Timer(min: Int, context: Context) :
                CountDownTimer(min * MIN_IN_MILLIS + 2 * SEC_IN_MILLIS, SEC_IN_MILLIS) {
            private val ctx = WeakReference<Context>(context)

            override fun onTick(millisUntilFinished: Long) {
                listener?.onTick()
                decrement()
            }

            override fun onFinish() {
                CaffeineManager.reset(ctx.get()!!)
                listener?.onFinish()
            }
        }
    }
}