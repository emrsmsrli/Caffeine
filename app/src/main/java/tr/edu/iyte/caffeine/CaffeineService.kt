package tr.edu.iyte.caffeine

import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import tr.edu.iyte.caffeine.util.CaffeineManager
import tr.edu.iyte.caffeine.util.Clock
import tr.edu.iyte.caffeine.util.ClockListener

class CaffeineService : TileService(), ClockListener, AnkoLogger {

    override fun onClick() {
        super.onClick()
        CaffeineManager.changeMode()
    }

    override fun onTileAdded() {
        super.onTileAdded()
        info("Tile added")
    }

    override fun onTileRemoved() {
        info("Tile removed")
        super.onTileRemoved()
    }

    override fun onStartListening() {
        super.onStartListening()
        info("Started listening")
        Clock.listener = this

        if(Clock.isFinished())
            updateTile()
    }

    override fun onStopListening() {
        info("Stopped listening")
        Clock.listener = null
        super.onStopListening()
    }

    private fun updateTile(
            state: Int = Tile.STATE_INACTIVE,
            label: String = getString(R.string.tile_name),
            icon: Int = R.drawable.ic_caffeine_empty) {
        qsTile?.state = state
        qsTile.label = label
        qsTile.icon = Icon.createWithResource(this, icon)
        info("Updating label: $label")
        qsTile.updateTile()
    }

    override fun onTick() {
        updateTile(state = Tile.STATE_ACTIVE,
                label = Clock.toString(),
                icon = when(Clock.getPercentage()) {
                    Clock.SIXTY_SIX -> R.drawable.ic_caffeine_66percent
                    Clock.THIRTY_THREE -> R.drawable.ic_caffeine_33percent
                    else -> R.drawable.ic_caffeine_full
                })
    }

    override fun onFinish() {
        updateTile()
    }
}