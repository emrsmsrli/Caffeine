package tr.edu.iyte.caffeine.util

import android.app.Service
import android.content.Context
import org.jetbrains.anko.intentFor

inline fun <reified T: Service> Context.stopService() = stopService(intentFor<T>())