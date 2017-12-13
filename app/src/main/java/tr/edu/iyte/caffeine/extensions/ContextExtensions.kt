package tr.edu.iyte.caffeine.extensions

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.telephony.TelephonyManager
import android.widget.Toast

inline fun <reified T: Service> Context.startService() {
    startService(Intent(this, T::class.java))
}
inline fun <reified T: Service> Context.stopService() =
        stopService(Intent(this, T::class.java))

val Context.powerManager: PowerManager
    get() = getSystemService(Context.POWER_SERVICE) as PowerManager

val Context.telephonyManager: TelephonyManager
    get() = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

fun Context.toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()