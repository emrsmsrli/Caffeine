package tr.edu.iyte.caffeine.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import tr.edu.iyte.caffeine.util.*

class PowerBroadcastReceiver : BroadcastReceiver(), Loggable {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_SCREEN_OFF) {
            verbose("PowerBroadcastReceiver: Received ${Intent.ACTION_SCREEN_OFF}")
            Clock.reset()
            CaffeineManager.reset(context)
            context.stopService<ScreenOffReceiverService>()
        }
    }
}