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

/**
 * CaffeineService is a subclass of a {@link TileService}.
 * Implements a tile to keep the screen awake for an amount
 * of time which the user chooses.
 */
@SuppressWarnings("deprecation")
public class CaffeineService extends TileService {

    /**
     * A subclass of {@link BroadcastReceiver} to capture {@link Intent#ACTION_SCREEN_OFF}.
     * This broadcast effectively causes Caffeine to stop.
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

    /**
     * A subclass of {@link Service} to register a persistent {@link PowerBroadcastReceiver}
     * to the system. This way even if {@link CaffeineService} is unbound, broadcasts will be received.
     */
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

        /**
         * Registers the {@link PowerBroadcastReceiver} to the system.
         */
        private void registerBroadcastReceiver() {
            if(!isBroadcastRegistered) {
                registerReceiver(RECEIVER, new IntentFilter(Intent.ACTION_SCREEN_OFF));
                isBroadcastRegistered = true;
                Log.i(TAG, "Receiver registered");
            }
        }

        /**
         * Unregisters the {@link PowerBroadcastReceiver} from the system.
         */
        private void unregisterBroadcastReceiver() {
            if(isBroadcastRegistered) {
                unregisterReceiver(RECEIVER);
                isBroadcastRegistered = false;
                Log.i(TAG, "Receiver unregistered");
            }
        }
    }

    /**
     * An helper class to keep track of the time easily.
     */
    private static class Clock {
        private static final int THIRTY_THREE = 33;
        private static final int SIXTY_SIX = 66;

        private int min;
        private int sec;

        /**
         * Sets the {@link #min minutes} to the {@code min}, {@link #sec seconds} to {@code 0}.
         * @param min Minutes to be set
         */
        private void set(int min) {
            this.min = min;
            this.sec = 0;
        }

        /**
         * Decrements the clock by one second.
         */
        private void decrement() {
            if(sec == 0) {
                sec = 60;
                --min;
            }
            --sec;
        }

        /**
         * Checks if the clock is 0:00.
         * @return {@code true} if clock is finished, {@code false} otherwise.
         */
        private boolean isFinished() {
            return min == 0 && sec == 0;
        }

        /**
         * Returns the remaining percentage of the clock for the current {@link Mode}.
         * @return {@link #THIRTY_THREE} for %33 percentage, {@link #SIXTY_SIX} for %66 percentage, {@code 0} otherwise.
         */
        private int getPercentage() {
            int totalsec = min * 60 + sec;
            float perc = (float) totalsec / (mode.getMin() * 60) * 100;
            if(perc > THIRTY_THREE - 1 && perc < THIRTY_THREE + 1)
                return THIRTY_THREE;
            /* todo */
            else if(perc > SIXTY_SIX - 1 && perc < SIXTY_SIX + 1)
                return SIXTY_SIX;
            else return 0;
        }

        /**
         * Returns a human readable form of a {@link Clock} object. Format is always {@code (m)m:ss}.
         * @return
         */
        @Override
        public String toString() {
            if(sec < 10)
                return String.format(Locale.getDefault(), "%d:0%d", min, sec);
            return String.format(Locale.getDefault(), "%d:%d", min, sec);
        }
    }

    /**
     * Mode enumerator for {@link CaffeineService}.
     * Enumerator is based on a straightforward state machine between the modes.
     * <p>
     * The method {@link #next()} returns the next {@link Mode} in the state machine.
     * <br>
     * The method {@link #getMin()} returns the operation minutes of the mode.
     * <br>
     * The method {@link #getLabel()} returns the initial mode label for the {@link Tile}.
     * <p>
     * It consists of 5 modes:
     * <ul>
     *     <li>{@link #INACTIVE} is for doing nothing,</li>
     *     <li>{@link #ONE_MIN} is for keeping screen on for 1 minutes,</li>
     *     <li>{@link #FIVE_MINS} is for keeping screen on for 5 minutes,</li>
     *     <li>{@link #TEN_MINS} is for keeping screen on for 10 minutes,</li>
     *     <li>{@link #INFINITE_MINS} is for keeping screen on for indefinitely.</li>
     * </ul>
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
        if(RECEIVER == null)
            RECEIVER = new PowerBroadcastReceiver();
        Log.d(TAG, "Tile added");
    }

    /**
     * When removing the tile, makes sure the service doesn't hold a
     * {@link android.os.PowerManager.WakeLock} for keeping the screen on.
     * Also clears all the background threads.
     */
    @Override
    public void onTileRemoved() {
        releaseWakeLock();
        resetTimer();
        mode = Mode.INACTIVE;
        Log.d(TAG, "Tile removed");
    }

    /**
     * When tile is visible on the screen,
     * resets the tile if the state is changed.
     */
    @Override
    public void onStartListening() {
        if(mode == Mode.INACTIVE)
            defaultTile();
        isListening = true;
        Log.d(TAG, "Started listening");
    }

    @Override
    public void onStopListening() {
        isListening = false;
        Log.d(TAG, "Stopped listening");
    }

    /**
     * Implementation of the {@link Mode} state machine.
     * When tile is clicked, proceeds to the next mode
     * and resets the timer.
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
     * First resets the {@link #timer} if there is one exists.
     * Then tries to acquire a wakelock for keeping the screen on, if it fails, caffeine will continue.
     * Then creates an {@link AsyncTask} for counting down from the current {@link Mode#min}.
     * <p>
     * {@link AsyncTask#onPreExecute()} prepares the {@link #CLOCK} with
     * setting it for the current {@link Mode} and updating the tile.
     * <br>
     * {@link AsyncTask#doInBackground(Object[])} sleeps for some time and sends request to update the tile.
     * <br>
     * {@link AsyncTask#onProgressUpdate(Object[])} updates the tile with {@link Clock#toString()}
     * <br>
     * {@link AsyncTask#onPostExecute(Object)} resets everything because the {@link Clock#isFinished()} returned {@code true}.
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
             * Waits for a second and calls {@link Clock#decrement()}.
             * If tile is listening the updates, updates the tile.
             * @param clock Current clock
             * @throws InterruptedException If {@link Mode} is changed.
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
     * Resets the tile to the default values,
     * releases the wakelock if there is one held,
     * releases the background the tasks.
     */
    private void reset() {
        defaultTile();
        releaseWakeLock();
        resetTimer();
    }

    /**
     * Sets the {@link #mode} to {@link Mode#INACTIVE}
     * and updates tile to default values.
     */
    private void defaultTile() {
        mode = Mode.INACTIVE;
        updateTile(true, mode.getLabel());
    }

    /**
     * Updates tile according to given label and mode change flag.
     * @param isModeChanged If the mode changed.
     * @param label Label to be updated.
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
     * Cancels the background task and releases it.
     */
    private void resetTimer() {
        Log.v(TAG, "Resetting timer");
        if(timer != null && timer.getStatus() != AsyncTask.Status.FINISHED)
            timer.cancel(true);
        timer = null;
    }

    /**
     * Tries to acquire a wakelock and registers the {@link ReceiverService}.
     * @return {@code true} if wakelock is acquired and service is started, {@code false} otherwise.
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
     * Tries to release the wakelock and unregisters the {@link ReceiverService}.
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
