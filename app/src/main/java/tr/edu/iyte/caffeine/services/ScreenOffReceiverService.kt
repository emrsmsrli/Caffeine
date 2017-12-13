package tr.edu.iyte.caffeine.services

import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import tr.edu.iyte.caffeine.Clock
import tr.edu.iyte.caffeine.extensions.*

class ScreenOffReceiverService : Service(), Loggable {
    private val callListener = object : PhoneStateListener() {
        private var isCallActive = false

        override fun onCallStateChanged(state: Int, incomingNumber: String?) {
            super.onCallStateChanged(state, incomingNumber)
            if(state == TelephonyManager.CALL_STATE_OFFHOOK && !isCallActive)
                isCallActive = true
            else if(state == TelephonyManager.CALL_STATE_IDLE && isCallActive)
                isCallActive = false

            val t = this@ScreenOffReceiverService
            if(isCallActive) {
                t.toast("Call detected, pausing Caffeine")
                info("Call started, pausing clock")
                Clock.pause()
            } else {
                info("Call ended, resuming clock")
                Clock.resume(t)
            }
        }
    }

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if(!isBroadcastRegistered) {
            registerReceiver(RECEIVER, IntentFilter(Intent.ACTION_SCREEN_OFF))
            isBroadcastRegistered = true
            telephonyManager.listen(callListener, PhoneStateListener.LISTEN_CALL_STATE)
            info("Screen off receiver and call listener registered")
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        if(isBroadcastRegistered) {
            unregisterReceiver(RECEIVER)
            isBroadcastRegistered = false
            telephonyManager.listen(callListener, PhoneStateListener.LISTEN_NONE)
            info("Screen off receiver and call listener unregistered")
        }
    }

    companion object {
        private var isBroadcastRegistered = false
        private val RECEIVER = PowerBroadcastReceiver()
    }
}