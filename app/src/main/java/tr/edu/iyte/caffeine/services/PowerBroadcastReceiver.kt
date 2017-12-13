package tr.edu.iyte.caffeine.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import tr.edu.iyte.caffeine.CaffeineManager
import tr.edu.iyte.caffeine.Clock
import tr.edu.iyte.caffeine.extensions.*

class PowerBroadcastReceiver : BroadcastReceiver(), Loggable {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_SCREEN_OFF) {
            info("Received ${Intent.ACTION_SCREEN_OFF}, intent: $intent")
            Clock.reset()
            CaffeineManager.reset(context)
            context.stopService<ScreenOffReceiverService>()
        }
    }
}