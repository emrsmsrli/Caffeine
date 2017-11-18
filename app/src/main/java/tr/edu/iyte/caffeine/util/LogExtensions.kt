package tr.edu.iyte.caffeine.util

import android.util.Log

interface Loggable {
    val tag: String
        get() = getTag(javaClass)
}

private fun getTag(clazz: Class<*>): String {
    val tag = clazz.simpleName
    return if (tag.length <= 23) {
        tag
    } else {
        tag.substring(0, 23)
    }
}

fun Loggable.info(message: Any) {
    if (Log.isLoggable(tag, Log.INFO)) {
        Log.i(tag, message.toString())
    }
}

fun Loggable.verbose(message: Any) {
    if (Log.isLoggable(tag, Log.VERBOSE)) {
        Log.v(tag, message.toString())
    }
}