package tr.edu.iyte.caffeine

import android.content.ComponentName
import android.content.ServiceConnection
import android.graphics.drawable.Icon
import android.os.IBinder
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import tr.edu.iyte.caffeine.util.*

class CaffeineTileService : TileService(), Loggable, TimerService.TimerListener {
    private val icCaffeineEmpty by lazy { Icon.createWithResource(this, R.drawable.ic_caffeine_empty) }
    private val icCaffeineFull by lazy { Icon.createWithResource(this, R.drawable.ic_caffeine_full) }
    private val icCaffeine66percent by lazy { Icon.createWithResource(this, R.drawable.ic_caffeine_66percent) }
    private val icCaffeine33percent by lazy { Icon.createWithResource(this, R.drawable.ic_caffeine_33percent) }

    private var isRemovingTile = false

    private var timerService: TimerService? = null
    private val timerServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(cn: ComponentName?, binder: IBinder?) {
            timerService = (binder as TimerService.TimerServiceProxy).get()
            timerService?.listener = this@CaffeineTileService

            if(isRemovingTile) {
                timerService?.onReset()
                stopService<TimerService>()
            }
        }

        override fun onServiceDisconnected(cn: ComponentName?) {}
    }

    override fun onTileAdded() {
        super.onTileAdded()
        isCaffeineRunning = false
        isRemovingTile = false
        info("tile added")
    }

    override fun onStartListening() {
        super.onStartListening()
        info("started listening")

        if(isLocked) {
            info("phone is locked, caffeine won't operate")
            updateTile(state = Tile.STATE_UNAVAILABLE)
            return
        }

        if(!startService<TimerService>()
            || !applicationContext.bindService(intent<TimerService>(), timerServiceConnection, 0))
            updateTile(state = Tile.STATE_UNAVAILABLE)
        else if(!isCaffeineRunning)
            updateTile()
    }

    override fun onClick() {
        super.onClick()
        timerService?.onModeChange()
    }

    override fun onStopListening() {
        if(timerService != null) {
            timerService?.listener = null
            applicationContext.unbindService(timerServiceConnection)
            timerService = null
        }
        info("stopped listening")
        super.onStopListening()
    }

    override fun onTileRemoved() {
        info("tile removed")
        if(isCaffeineRunning) {
            isRemovingTile = true
            applicationContext.bindService(intent<TimerService>(), timerServiceConnection, 0)
        }
        super.onTileRemoved()
    }

    override fun onTick(label: String, percentage: Float) {
        updateTile(state = Tile.STATE_ACTIVE,
                label = label,
                icon = when {
                    percentage > .66 -> icCaffeineFull
                    percentage > .33 -> icCaffeine66percent
                    else             -> icCaffeine33percent
                })
    }

    override fun onFinish() {
        updateTile()
    }

    private fun updateTile(
            state: Int = Tile.STATE_INACTIVE,
            label: String = getString(R.string.tile_name),
            icon: Icon = icCaffeineEmpty) {
        qsTile?.state = state
        qsTile?.label = label
        qsTile?.icon = icon
        info("updating label: $label")
        qsTile?.updateTile()
    }
}