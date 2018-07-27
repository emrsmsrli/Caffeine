package tr.edu.iyte.caffeine.util

import android.app.NotificationManager
import android.content.Context
import android.os.PowerManager
import android.telephony.TelephonyManager

fun Int.toSeconds() = this * 60L
fun Long.toMillis() = this * 1000L

val Context.powerManager: PowerManager
    get() = getSystemService(Context.POWER_SERVICE) as PowerManager

val Context.telephonyManager: TelephonyManager
    get() = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

val Context.notificationManager: NotificationManager
    get() = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager