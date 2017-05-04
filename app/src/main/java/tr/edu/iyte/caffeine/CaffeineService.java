package tr.edu.iyte.caffeine;

import android.os.AsyncTask;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

public class CaffeineService extends TileService {

    private static class Clock {
        private int min;
        private int sec;

        private void set(int min, int sec) {
            this.min = min;
            this.sec = sec;
        }

        private int getMin() {
            return min;
        }

        private int getSec() {
            return sec;
        }
    }

    private static final String TAG = "CaffeineService";
    private static final Clock CLOCK = new Clock();

    private enum Mode {
        INACTIVE("Caffeine"),
        FIVE_MINS("5:00"),
        TEN_MINS("10:00"),
        INFINITE_MINS("\u221E");

        private String label;

        Mode(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private boolean isListening = false;
    private Mode mode = Mode.INACTIVE;

    /**
     * TODO
     * - implement finite state machine 0 - 5 - 10 - infinite minutes
     * - implement countdown timer with async task, publishProgress(Clock c)
     */

    private AsyncTask<Clock, Clock, Void> timerTask = null;

    @Override
    public void onTileAdded() {
        super.onTileAdded();
        Tile t = getQsTile();
        t.setState(Tile.STATE_INACTIVE);
        t.setLabel(mode.getLabel());
        t.updateTile();
    }

    @Override
    public void onTileRemoved() {
        super.onTileRemoved();
        resetClock();
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        isListening = true;
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
        isListening = false;
    }

    @Override
    public void onClick() {
        super.onClick();
        Tile t = getQsTile();

        switch(mode) {
            case INACTIVE:
                mode = Mode.FIVE_MINS;
                t.setState(Tile.STATE_ACTIVE);
                t.setLabel(mode.getLabel());
                break;
            case FIVE_MINS:
                mode = Mode.TEN_MINS;
                t.setLabel(mode.getLabel());
                break;
            case TEN_MINS:
                mode = Mode.INFINITE_MINS;
                t.setLabel(mode.getLabel());
                break;
            case INFINITE_MINS:
                mode = Mode.INACTIVE;
                t.setState(Tile.STATE_INACTIVE);
                t.setLabel(mode.getLabel());
                resetClock();
                break;
            default:
                break;
        }

        t.updateTile();
    }

    private void createTask() {
        timerTask = new AsyncTask<Clock, Clock, Void>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                //todo init tile
            }

            @Override
            protected Void doInBackground(Clock... c) {
                Clock clock = c[0];

                try {
                    Thread.sleep(1000);
                } catch(InterruptedException e) {
                    Log.e(TAG, "doInBackground: Caffeine mode changed");
                }

                return null;
            }

            @Override
            protected void onProgressUpdate(Clock... values) {
                super.onProgressUpdate(values);
                Clock clock = values[0];

                //todo update tile
            }
        }.execute(CLOCK);
    }

    private void setClock(int min, int sec) {
        CLOCK.set(min, sec);
    }

    private void resetClock() {
        if(timerTask != null && timerTask.getStatus() != AsyncTask.Status.FINISHED)
            timerTask.cancel(true);
    }
}
