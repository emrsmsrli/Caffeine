package tr.edu.iyte.caffeine

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.os.Build
import android.os.CountDownTimer
import android.os.PowerManager
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import tr.edu.iyte.caffeine.util.*

class CaffeineTileService : TileService(), Loggable {
    inner class Timer(private val secs: Long) :
            CountDownTimer(secs.toMillis() + 300, 500) {
        override fun onTick(millisUntilFinished: Long) {
            val sec = millisUntilFinished / 1000
            val min = sec / 60
            val percentage = sec / secs.toFloat()

            if(secs < Int.MAX_VALUE)
                updateTile(state = Tile.STATE_ACTIVE,
                        label = String.format("%d:%02d", min, sec % 60),
                        icon = when {
                            percentage > .66 -> icCaffeineFull
                            percentage > .33 -> icCaffeine66percent
                            else             -> icCaffeine33percent
                        })
        }

        override fun onFinish() {
            reset()
            updateTile()
        }
    }

    inner class ScreenOffReceiver : BroadcastReceiver(), Loggable {
        override fun onReceive(context: Context, intent: Intent) {
            if(intent.action == Intent.ACTION_SCREEN_OFF) {
                info("Received ${Intent.ACTION_SCREEN_OFF}, intent: $intent")
                reset()
            }
        }
    }

    private val icCaffeineEmpty by lazy { Icon.createWithResource(this, R.drawable.ic_caffeine_empty) }
    private val icCaffeineFull by lazy { Icon.createWithResource(this, R.drawable.ic_caffeine_full) }
    private val icCaffeine66percent by lazy { Icon.createWithResource(this, R.drawable.ic_caffeine_66percent) }
    private val icCaffeine33percent by lazy { Icon.createWithResource(this, R.drawable.ic_caffeine_33percent) }

    private var wakelock: PowerManager.WakeLock? = null
    private var mode = CaffeineMode.INACTIVE
    private var currentTimer: Timer? = null

    private val screenOffReceiver = ScreenOffReceiver()
    private val callListener = object : PhoneStateListener() {
        override fun onCallStateChanged(state: Int, incomingNumber: String?) {
            super.onCallStateChanged(state, incomingNumber)
            if(state == TelephonyManager.CALL_STATE_OFFHOOK)
                reset()
        }
    }

    override fun onTileAdded() {
        super.onTileAdded()
        info("tile added")
    }

    override fun onTileRemoved() {
        info("tile removed")
        reset()
        super.onTileRemoved()
    }

    override fun onClick() {
        super.onClick()

        if(isLocked) {
            info("phone is locked, caffeine won't operate")
            return
        }

        when(mode) {
            CaffeineMode.INFINITE_MINS -> {
                reset()
                updateTile()
                currentTimer?.cancel()
            }
            else                       -> {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && currentTimer == null) {
                    notificationManager.createNotificationChannel(
                            NotificationChannel("caffeine_channel",
                                    "Caffeine Notification Channel",
                                    NotificationManager.IMPORTANCE_HIGH))
                    val notif = NotificationCompat.Builder(this, "caffeine_channel")
                            .setSmallIcon(R.drawable.ic_caffeine_full)
                            .setContentText("Caffeine is running")
                            .setCategory(Notification.CATEGORY_SERVICE)
                            .build()
                    startForeground(1, notif)
                }

                mode = mode.next()
                currentTimer?.cancel()

                acquireWakelock(mode.min.toSeconds())
                registerInterruptionListeners()

                updateTile(state = Tile.STATE_ACTIVE, label = mode.label, icon = icCaffeineFull)
                currentTimer = Timer(mode.min.toSeconds())
                currentTimer?.start()
            }
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        if(currentTimer == null)
            updateTile()
    }

    private fun reset() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            stopForeground(true)

        mode = CaffeineMode.INACTIVE
        unregisterInterruptionListeners()
        releaseWakelock()
        currentTimer?.cancel()
        currentTimer = null
    }

    private fun registerInterruptionListeners() {
        registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
        telephonyManager.listen(callListener, PhoneStateListener.LISTEN_CALL_STATE)
        info("Screen off receiver and call listener registered")
    }

    private fun unregisterInterruptionListeners() {
        unregisterReceiver(screenOffReceiver)
        telephonyManager.listen(callListener, PhoneStateListener.LISTEN_NONE)
        info("Screen off receiver and call listener unregistered")
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

    private fun updateTile(
            state: Int = Tile.STATE_INACTIVE,
            label: String = getString(R.string.tile_name),
            icon: Icon = icCaffeineEmpty) {
        qsTile?.state = state
        qsTile?.label = label
        qsTile?.icon = icon
        info("updating label: $label")
        qsTile?.updateTile()
    }
}