package tr.edu.iyte.caffeine

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.Build
import android.os.CountDownTimer
import android.os.PowerManager
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import tr.edu.iyte.caffeine.util.*

class TimerService : Service(), Loggable {
    inner class TimerServiceProxy : Binder() { fun get() = this@TimerService }
    override fun onBind(intent: Intent?) = TimerServiceProxy()

    interface TimerListener {
        fun onTick(label: String, percentage: Float)
        fun onFinish()
    }

    private inner class Timer(private val secs: Long) :
            CountDownTimer(secs.toMillis() + 300, 500) {
        override fun onTick(millisUntilFinished: Long) {
            val sec = (millisUntilFinished / 1000).toInt()
            val min = sec / 60
            val percentage = sec / secs.toFloat()

            if(secs >= Int.MAX_VALUE)
                return
            listener?.onTick(String.format("%d:%02d", min, sec % 60), percentage)
        }

        override fun onFinish() {
            onReset()
            listener?.onFinish()
        }
    }

    private inner class ScreenOffReceiver : BroadcastReceiver(), Loggable {
        override fun onReceive(context: Context, intent: Intent) {
            if(intent.action != Intent.ACTION_SCREEN_OFF)
                return
            info("Received ${Intent.ACTION_SCREEN_OFF}, intent: $intent")
            onReset()
        }
    }

    var listener: TimerListener? = null

    private var wakelock: PowerManager.WakeLock? = null
    private var mode = CaffeineMode.INACTIVE
    private var currentTimer: Timer? = null

    private val screenOffReceiver = ScreenOffReceiver()
    private val callListener = object : PhoneStateListener() {
        override fun onCallStateChanged(state: Int, incomingNumber: String?) {
            super.onCallStateChanged(state, incomingNumber)
            if(state == TelephonyManager.CALL_STATE_OFFHOOK)
                onReset()
        }
    }

    fun onModeChange() {
        when(mode) {
            CaffeineMode.INFINITE_MINS -> {
                onReset()
                listener?.onFinish()
            }
            else                       -> {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !isCaffeineRunning) {
                    notificationManager.createNotificationChannel(
                            NotificationChannel("caffeine_channel",
                                    "Caffeine Notification Channel",
                                    NotificationManager.IMPORTANCE_HIGH))
                    val notif = NotificationCompat.Builder(this, "caffeine_channel")
                            .setOnlyAlertOnce(true)
                            .setSmallIcon(R.drawable.ic_caffeine_full)
                            .setContentText("Caffeine is running")
                            .setOngoing(true)
                            .setCategory(Notification.CATEGORY_SERVICE)
                            .build()
                    startForeground(85, notif)
                }

                mode = mode.next()
                currentTimer?.cancel()

                acquireWakelock(mode.min.toSeconds())
                registerInterruptionListeners()

                currentTimer?.cancel()
                listener?.onTick(mode.label, 1f)
                currentTimer = Timer(mode.min.toSeconds())
                currentTimer?.start()
                isCaffeineRunning = true
            }
        }
    }

    fun onReset() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            stopForeground(true)

        mode = CaffeineMode.INACTIVE
        unregisterInterruptionListeners()
        releaseWakelock()
        currentTimer?.cancel()
        currentTimer = null
        isCaffeineRunning = false
    }

    private fun registerInterruptionListeners() {
        if(!isCaffeineRunning) {
            registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
            telephonyManager.listen(callListener, PhoneStateListener.LISTEN_CALL_STATE)
            info("Screen off receiver and call listener registered")
        }
    }

    private fun unregisterInterruptionListeners() {
        if(isCaffeineRunning) {
            unregisterReceiver(screenOffReceiver)
            telephonyManager.listen(callListener, PhoneStateListener.LISTEN_NONE)
            info("Screen off receiver and call listener unregistered")
        }
    }

    @Suppress("deprecation")
    private fun acquireWakelock(secs: Long) {
        releaseWakelock()

        info("Acquiring wakelock..")
        wakelock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "CaffeineWL")
        wakelock?.acquire(secs.toMillis())
    }

    private fun releaseWakelock() {
        if(wakelock == null || !wakelock!!.isHeld)
            return
        info("Releasing wakelock..")
        wakelock?.release()
        wakelock = null
    }
}