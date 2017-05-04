package tr.edu.iyte.caffeine;

import android.service.quicksettings.TileService;

public class CaffeineService extends TileService {

    private static final Clock CLOCK = new Clock();

    /**
     * TODO
     * - implement finite state machine 0 - 5 - 10 - infinite minutes
     * - implement countdown timer with async task, publishProgress(Clock c)
     */

    @Override
    public void onTileAdded() {
        super.onTileAdded();
    }

    @Override
    public void onTileRemoved() {
        super.onTileRemoved();
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
    }

    @Override
    public void onClick() {
        super.onClick();
    }
}
