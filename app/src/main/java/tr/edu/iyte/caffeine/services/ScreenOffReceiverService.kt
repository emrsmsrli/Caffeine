package tr.edu.iyte.caffeine.services

import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

private var isBroadcastRegistered = false
private val RECEIVER = PowerBroadcastReceiver()

class ScreenOffReceiverService : Service(), AnkoLogger {
    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if(!isBroadcastRegistered) {
            registerReceiver(RECEIVER, IntentFilter(Intent.ACTION_SCREEN_OFF))
            isBroadcastRegistered = true
            info("Screen off receiver registered")
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        if(isBroadcastRegistered) {
            unregisterReceiver(RECEIVER)
            isBroadcastRegistered = false
            info("Screen off receiver unregistered")
        }
    }
}