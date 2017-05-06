package tr.edu.iyte.caffeine;

import android.os.AsyncTask;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

import java.util.Locale;

/*
    Long clicking on your quick settings tile will,
    by default, go to your app’s ‘App Info’ screen.
    You can override that behavior by adding an <intent-filter>
    to one of your activities with ACTION_QS_TILE_PREFERENCES.

    -----------------------------------------------------------------

    In active mode, your TileService will still be bound for onTileAdded()
    and onTileRemoved() (and for click events). However, the only time
    you’ll get a callback to onStartListening() is after you call the static
    TileService.requestListeningState() method. You’ll then be able to update
    your tile exactly once before receiving a callback to onStopListening().
    This gives you an easy one-shot ability to update your tile right when
    your data changes whether the tile is visible or not.
 */

/**
 * todo javadoc
 */
public class CaffeineService extends TileService {

    private static class Clock {
        private int min;
        private int sec;

        private void set(int min) {
            this.min = min;
            this.sec = 0;
        }

        private void decrement() {
            if(sec == 0) {
                sec = 60;
                --min;
            }
            --sec;
        }

        private boolean isFinished() {
            return min == 0 && sec == 0;
        }
        
        @Override
        public String toString() {
            if(sec < 10)
                return String.format(Locale.getDefault(), "%d:0%d", min, sec);
            return String.format(Locale.getDefault(), "%d:%d", min, sec);
        }
    }

    private enum Mode {
        INACTIVE("Caffeine", 0),
        ONE_MIN("1:00", 1),
        FIVE_MINS("5:00", 5),
        TEN_MINS("10:00", 10),
        INFINITE_MINS("\u221E", 0);

        private String label;
        private int min;

        Mode(String label, int min) {
            this.label = label;
            this.min = min;
        }

        public String getLabel() {
            return label;
        }

        public int getMin() {
            return min;
        }

        public Mode next() {
            switch(this) {
                case INACTIVE:
                    return ONE_MIN;
                case ONE_MIN:
                    return FIVE_MINS;
                case FIVE_MINS:
                    return TEN_MINS;
                case TEN_MINS:
                    return INFINITE_MINS;
                case INFINITE_MINS:
                default:
                    return INACTIVE;
            }
        }
    }

    /**
     *
     */
    private static final String TAG = "CaffeineService";

    /**
     *
     */
    private static final Clock CLOCK = new Clock();

    /**
     *
     */
    private static boolean isListening = false;

    /**
     *
     */
    private static boolean isWakeLockAcquired = false;

    /**
     *
     */
    private static Mode mode = Mode.INACTIVE;

    /**
     *
     */
    private static AsyncTask<Clock, Clock, Void> timerTask = null;

    //todo remove this method
    @Override
    public void onCreate() {
        super.onCreate();
        android.os.Debug.waitForDebugger();
    }

    @Override
    public void onTileAdded() {
        Log.d(TAG, "Tile added");
    }

    @Override
    public void onTileRemoved() {
        Log.d(TAG, "Tile removed");
        resetClock(false);
    }

    /**
     *
     */
    @Override
    public void onStartListening() {
        Log.d(TAG, "Started listening");
        isListening = true;
    }

    /**
     *
     */
    @Override
    public void onStopListening() {
        Log.d(TAG, "Stopped listening");
        isListening = false;
    }

    /**
     *
     */
    @Override
    public void onClick() {
        Log.d(TAG, "Tile clicked, last mode: " + mode.toString());

        switch(mode) {
            case INACTIVE:
            case ONE_MIN:
            case FIVE_MINS:
            case TEN_MINS:
                mode = mode.next();
                createTask();
                break;
            case INFINITE_MINS:
                resetClock(true);
                break;
            default:
                break;
        }

        Log.d(TAG, "Mode changed: " + mode.toString());
    }

    /**
     *
     */
    private void createTask() {
        timerTask = new AsyncTask<Clock, Clock, Void>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                resetClock(false);
                setTile();
            }

            @Override
            protected Void doInBackground(Clock... c) {
                Clock clock = c[0];

                try {
                    if(mode == Mode.INFINITE_MINS) {
                        while(!Thread.interrupted()) // FIXME: 05/05/2017 is this true?
                            Thread.sleep(30000);
                    } else {
                        while(!clock.isFinished())
                            waitAndUpdate(clock);
                    }
                } catch(InterruptedException e) {
                    Log.i(TAG, "Thread interrupted, Caffeine mode changed");
                }

                return null;
            }

            @Override
            protected void onProgressUpdate(Clock... c) {
                super.onProgressUpdate(c);
                Tile tile = getQsTile();
                CaffeineService.updateTileLabel(tile, c[0].toString());
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                resetClock(true);
            }

            /**
             *
             * @param clock
             * @throws InterruptedException
             */
            private void waitAndUpdate(Clock clock) throws InterruptedException {
                Thread.sleep(1000);
                clock.decrement();
                if(isListening)
                    publishProgress(clock);
            }
        }.execute(CLOCK);
    }

    private void resetClock(boolean finished) {
        Log.i(TAG, "Resetting clock...");
        if(timerTask != null && timerTask.getStatus() != AsyncTask.Status.FINISHED)
            timerTask.cancel(true);
        timerTask = null;

        releaseWakeLock();

        if(finished)
            resetTile();
    }

    private void setTile() {
        if(!isWakeLockAcquired)
            acquireWakeLock();
        Tile tile = getQsTile();
        CLOCK.set(mode.getMin());
        tile.setState(Tile.STATE_ACTIVE);
        updateTileLabel(tile, mode.getLabel());
    }
    
    private void resetTile() {
        mode = Mode.INACTIVE;
        Tile tile = getQsTile();
        tile.setState(Tile.STATE_INACTIVE);
        updateTileLabel(tile, mode.getLabel());
    }

    private void acquireWakeLock() {
        //todo acquire wakelock
        isWakeLockAcquired = true;
    }

    private void releaseWakeLock() {
        //todo release wakelock
        isWakeLockAcquired = false;
    }

    private static void updateTileLabel(Tile t, String label) {
        Log.d(TAG, "Updating tile label: " + label);
        t.setLabel(label);
        t.updateTile();
    }
}
