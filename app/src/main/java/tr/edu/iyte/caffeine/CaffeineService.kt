package tr.edu.iyte.caffeine

import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import tr.edu.iyte.caffeine.extensions.*

class CaffeineService : TileService(), Clock.ClockListener, Loggable {

    private val icCaffeineEmpty by lazy { Icon.createWithResource(this, R.drawable.ic_caffeine_empty) }
    private val icCaffeineFull by lazy { Icon.createWithResource(this, R.drawable.ic_caffeine_full) }
    private val icCaffeine66percent by lazy { Icon.createWithResource(this, R.drawable.ic_caffeine_66percent) }
    private val icCaffeine33percent by lazy { Icon.createWithResource(this, R.drawable.ic_caffeine_33percent) }

    override fun onClick() {
        super.onClick()

        if(isLocked) {
            info("Device locked, Caffeine won't operate")
            return
        }

        CaffeineManager.changeMode(applicationContext)
    }

    override fun onTileAdded() {
        super.onTileAdded()
        info("Tile added")
    }

    override fun onTileRemoved() {
        info("Removing Caffeine tile")
        CaffeineManager.reset(applicationContext)
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
            icon: Icon = icCaffeineEmpty) {
        qsTile?.state = state
        qsTile?.label = label
        qsTile?.icon = icon
        info("Updating label: $label")
        qsTile?.updateTile()
    }

    override fun onTick() {
        updateTile(state = Tile.STATE_ACTIVE,
                label = Clock.toString(),
                icon = when(Clock.getPercentage()) {
                    Clock.Percentage.FULL         -> icCaffeineFull
                    Clock.Percentage.SIXTY_SIX    -> icCaffeine66percent
                    Clock.Percentage.THIRTY_THREE -> icCaffeine33percent
                })
    }

    override fun onFinish() {
        updateTile()
    }
}