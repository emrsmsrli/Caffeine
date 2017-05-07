package tr.edu.iyte.caffeine;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Icon;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.PowerManager;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.support.annotation.Nullable;
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
@SuppressWarnings("deprecation")
public class CaffeineService extends TileService {

    /**
     *
     */
    public class PowerBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                Log.v(TAG, "PowerBroadcastReceiver: Received " + Intent.ACTION_SCREEN_OFF);
                mode = Mode.INACTIVE;
                releaseWakeLock();
                resetTimer();
            }
        }
    }

    public static class ReceiverService extends Service {
        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            registerBroadcastReceiver();
            return super.onStartCommand(intent, flags, startId);
        }

        @Override
        public void onDestroy() {
            unregisterBroadcastReceiver();
            super.onDestroy();
        }

        private void registerBroadcastReceiver() {
            if(!isBroadcastRegistered) {
                registerReceiver(RECEIVER, new IntentFilter(Intent.ACTION_SCREEN_OFF));
                isBroadcastRegistered = true;
                Log.i(TAG, "Receiver registered");
            }
        }

        private void unregisterBroadcastReceiver() {
            if(isBroadcastRegistered) {
                unregisterReceiver(RECEIVER);
                isBroadcastRegistered = false;
                Log.i(TAG, "Receiver unregistered");
            }
        }
    }

    /**
     *
     */
    private static class Clock {
        private static final int THIRTY_THREE = 33;
        private static final int SIXTY_SIX = 66;

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

        private int getPercentage() {
            int totalsec = min * 60 + sec;
            float perc = (float) totalsec / (mode.getMin() * 60) * 100;
            if(perc > THIRTY_THREE - 1 && perc < THIRTY_THREE + 1)
                return THIRTY_THREE;
            else if(perc > SIXTY_SIX - 1 && perc < SIXTY_SIX + 1)
                return SIXTY_SIX;
            else return 0;
        }
        
        @Override
        public String toString() {
            if(sec < 10)
                return String.format(Locale.getDefault(), "%d:0%d", min, sec);
            return String.format(Locale.getDefault(), "%d:%d", min, sec);
        }
    }

    /**
     *
     */
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

    private static final String TAG = "CaffeineService";
    private static final Clock CLOCK = new Clock();

    private static boolean isListening = false;
    private static boolean isBroadcastRegistered = false;
    private static Mode mode = Mode.INACTIVE;
    private static AsyncTask<Clock, Clock, Void> timer = null;
    private static PowerManager.WakeLock wakeLock = null;

    private static PowerBroadcastReceiver RECEIVER = null;

    @Override
    public void onTileAdded() {
        Log.d(TAG, "Tile added");
        if(RECEIVER == null)
            RECEIVER = new PowerBroadcastReceiver();
    }

    /**
     *
     */
    @Override
    public void onTileRemoved() {
        Log.d(TAG, "Tile removed");
        releaseWakeLock();
        resetTimer();
        mode = Mode.INACTIVE;
    }

    /**
     *
     */
    @Override
    public void onStartListening() {
        Log.d(TAG, "Started listening");
        if(mode == Mode.INACTIVE)
            defaultTile();
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
        switch(mode) {
            case INACTIVE:
            case ONE_MIN:
            case FIVE_MINS:
            case TEN_MINS:
                mode = mode.next();
                createTimer();
                break;
            case INFINITE_MINS:
                reset();
                break;
            default:
                break;
        }
    }

    /**
     *
     */
    private void createTimer() {
        resetTimer();
        if(!acquireWakeLock()) {
            reset();
            return;
        }

        timer = new AsyncTask<Clock, Clock, Void>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                CLOCK.set(mode.getMin());
                updateTile(true, mode.getLabel());
            }

            @Override
            protected Void doInBackground(Clock... c) {
                Clock clock = c[0];

                try {
                    if(mode == Mode.INFINITE_MINS)
                        Thread.sleep(Long.MAX_VALUE);
                    else
                        while(!clock.isFinished())
                            waitAndUpdate(clock);
                } catch(InterruptedException e) {
                    Log.v(TAG, "Thread interrupted, Caffeine mode changed");
                }

                return null;
            }

            @Override
            protected void onProgressUpdate(Clock... c) {
                super.onProgressUpdate(c);
                updateTile(false, c[0].toString());
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                reset();
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

    /**
     *
     */
    private void reset() {
        defaultTile();
        releaseWakeLock();
        resetTimer();
    }

    /**
     *
     */
    private void defaultTile() {
        mode = Mode.INACTIVE;
        updateTile(true, mode.getLabel());
    }

    /**
     *
     * @param isModeChanged
     * @param label
     */
    private void updateTile(boolean isModeChanged, String label) {
        Log.i(TAG, "Updating tile: " + label);
        Tile tile = getQsTile();
        tile.setLabel(label);

        if(isModeChanged) {
            tile.setState(mode == Mode.INACTIVE ? Tile.STATE_INACTIVE : Tile.STATE_ACTIVE);
            tile.setIcon(Icon.createWithResource(this, R.drawable.ic_caffeine_full));
        } else if(CLOCK.getPercentage() == Clock.SIXTY_SIX) {
            tile.setIcon(Icon.createWithResource(this, R.drawable.ic_caffeine_66percent));
        } else if(CLOCK.getPercentage() == Clock.THIRTY_THREE) {
            tile.setIcon(Icon.createWithResource(this, R.drawable.ic_caffeine_33percent));
        }

        tile.updateTile();
    }

    /**
     *
     */
    private void resetTimer() {
        Log.v(TAG, "Resetting timer");
        if(timer != null && timer.getStatus() != AsyncTask.Status.FINISHED)
            timer.cancel(true);
        timer = null;
    }

    /**
     *
     */
    private boolean acquireWakeLock() {
        if(wakeLock == null) {
            Log.i(TAG, "Acquiring wakelock..");
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "CaffeineWL");
            wakeLock.acquire();

            if(startService(new Intent(CaffeineService.this, ReceiverService.class)) == null) {
                Log.e(TAG, "Cannot start ReceiverService, Caffeine won't continue");
                return false;
            }
        }
        return true;
    }

    /**
     *
     */
    private void releaseWakeLock() {
        if(wakeLock != null && wakeLock.isHeld()) {
            Log.i(TAG, "Releasing wakelock..");
            wakeLock.release();
            wakeLock = null;
        }

        stopService(new Intent(this, ReceiverService.class));
    }
}
