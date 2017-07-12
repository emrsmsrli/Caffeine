package tr.edu.iyte.caffeine.util

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.error
import org.jetbrains.anko.info
import org.jetbrains.anko.startService
import tr.edu.iyte.caffeine.services.ScreenOffReceiverService

object CaffeineManager : AnkoLogger {
    private var wakeLock: PowerManager.WakeLock? = null
    var context: Context? = null
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
                acquireWakeLock()
            }
            CaffeineMode.INFINITE_MINS -> {
                reset()
                Clock.reset()
            }
        }
    }

    fun reset() {
        mode = CaffeineMode.INACTIVE
        context?.stopService(Intent(context, ScreenOffReceiverService::class.java))
        releaseWakeLock()
    }

    @Suppress("deprecation")
    fun acquireWakeLock() {
        if(wakeLock == null) {
            info("Acquiring wakelock..")
            val powerManager = context?.getSystemService(Context.POWER_SERVICE) as? PowerManager
            if(powerManager == null) {
                Clock.reset()
                reset()
                return
            }

            wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "CaffeineWL")
            wakeLock?.acquire()

            if(context?.startService<ScreenOffReceiverService>() == null) {
                error("Cannot start ScreenOffReceiverService, Caffeine won't continue")
                Clock.reset()
                reset()
            }
        }
    }

    fun releaseWakeLock() {
        if(wakeLock != null && wakeLock!!.isHeld) {
            info("Releasing wakelock..")
            wakeLock?.release()
            wakeLock = null
        }
    }
}