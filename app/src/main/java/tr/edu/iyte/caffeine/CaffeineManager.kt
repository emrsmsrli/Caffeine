package tr.edu.iyte.caffeine

import android.annotation.SuppressLint
import android.content.Context
import android.os.PowerManager
import tr.edu.iyte.caffeine.services.ScreenOffReceiverService
import tr.edu.iyte.caffeine.extensions.*

object CaffeineManager : Loggable {
    private var wakeLock: PowerManager.WakeLock? = null
    var mode = CaffeineMode.INACTIVE
        private set

    fun changeMode(context: Context) = when(mode) {
        CaffeineMode.INACTIVE,
        CaffeineMode.ONE_MIN,
        CaffeineMode.FIVE_MINS,
        CaffeineMode.TEN_MINS      -> {
            mode = mode.next()
            Clock.set(context, mode.min)
            acquireWakeLock(context, mode.min)
            context.startService<ScreenOffReceiverService>()
        }
        CaffeineMode.INFINITE_MINS -> {
            reset(context)
            Clock.reset()
        }
    }

    fun reset(context: Context) {
        mode = CaffeineMode.INACTIVE
        context.stopService<ScreenOffReceiverService>()
        releaseWakeLock()
    }

    @SuppressLint("WakelockTimeout")
    @Suppress("deprecation")
    private fun acquireWakeLock(context: Context, min: Int) {
        if(wakeLock != null && wakeLock!!.isHeld)
            wakeLock?.release()

        info("Acquiring wakelock..")
        wakeLock = context.powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "CaffeineWL")
        if(min == Int.MAX_VALUE)
            wakeLock?.acquire()
        else
            wakeLock?.acquire(min * 60 * 1000L)
    }

    private fun releaseWakeLock() {
        if(wakeLock != null && wakeLock!!.isHeld) {
            info("Releasing wakelock..")
            wakeLock?.release()
            wakeLock = null
        }
    }
}