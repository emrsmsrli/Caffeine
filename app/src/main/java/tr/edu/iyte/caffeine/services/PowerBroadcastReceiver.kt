package tr.edu.iyte.caffeine.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.verbose
import tr.edu.iyte.caffeine.util.CaffeineManager
import tr.edu.iyte.caffeine.util.Clock

class PowerBroadcastReceiver : BroadcastReceiver(), AnkoLogger {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_SCREEN_OFF) {
            verbose("PowerBroadcastReceiver: Received ${Intent.ACTION_SCREEN_OFF}")
            Clock.reset()
            CaffeineManager.reset()
            context?.stopService(Intent(context, ScreenOffReceiverService::class.java))
        }
    }
}