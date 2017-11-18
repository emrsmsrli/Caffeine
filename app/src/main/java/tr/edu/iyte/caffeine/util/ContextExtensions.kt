package tr.edu.iyte.caffeine.util

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.PowerManager

inline fun <reified T: Service> Context.startService() {
    startService(Intent(this, T::class.java))
}
inline fun <reified T: Service> Context.stopService() =
        stopService(Intent(this, T::class.java))

val Context.powerManager: PowerManager
    get() = getSystemService(Context.POWER_SERVICE) as PowerManager