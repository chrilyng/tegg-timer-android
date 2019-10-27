package dk.siit.tegg;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import java.util.Locale;

import dk.siit.tegg.view.EggView;

public class TeggTimer extends Activity implements AlarmCallback, CountdownCallback {
    private EggView eggView;
    private TextView clockTextView;

    private Intent timerService;
    private TimerService boundTimerService;
    private boolean isTimerServiceBound = false;

    protected Ringtone alarmRingtone;
    private int timeSet = 0;
    private long remainingTime;
    private boolean countdownFinished = true;

    public static final int MINUTE = 60000;
    public static final int SECOND = 1000;

    public static final String ALARM_BROADCAST = "TIMES_UP";
    public static final String ALARM_TIME = "FIRST_TIME";
    private static final String EXTRA_FINISHED = "FINISH";
    private static final String EXTRA_TIME_SET = "TIME_SET";


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_clock);
        doBindService();

        clockTextView = findViewById(R.id.clock);
        eggView = findViewById(R.id.ring_num);
        eggView.registerAlarmCallback(this);

        Uri myUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        alarmRingtone = RingtoneManager.getRingtone(this, myUri);

        if (getIntent().getBooleanExtra(ALARM_BROADCAST, false) && savedInstanceState == null) {
            countdownFinished = true;

            Window window = getWindow();

            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                    | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);

            // Play alarm
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setCancelable(true);
            builder.setNeutralButton(getString(R.string.alarm_cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    alarmRingtone.stop();
                    resetAlarm();
                }
            });
            builder.setMessage(getString(R.string.alarm_service_finished));
            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    alarmRingtone.stop();
                    resetAlarm();
                }
            });
            Dialog dialog = builder.create();
            dialog.show();

            alarmRingtone.play();
        }
        if (countdownFinished)
            startButton();
    }

    public void startButton() {
        Button start = findViewById(R.id.ring_start);
        start.setText(getString(R.string.alarm_start));
        start.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View arg0) {
                stopButton();

                countdownFinished = false;

                int rot = (int) -eggView.getClock().getRotation();
                // Wheel updates every ten seconds
                remainingTime = rot * 10 * SECOND;

                timerService = new Intent(TeggTimer.this, TimerService.class);
                timerService.putExtra(ALARM_TIME, remainingTime);
                startService(timerService);
                doBindService();
                eggView.setLocked(true);
            }
        });
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        countdownFinished = savedInstanceState.getBoolean(EXTRA_FINISHED, true);
        if (!countdownFinished) {
            doBindService();
            stopButton();
        } else {
            timeSet = savedInstanceState.getInt(EXTRA_TIME_SET);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(EXTRA_FINISHED, countdownFinished);
        if (countdownFinished) {
            outState.putInt(EXTRA_TIME_SET, timeSet);
        }
        super.onSaveInstanceState(outState);
    }

    public void stopButton() {
        Button startButton = findViewById(R.id.ring_start);
        startButton.setText(getString(R.string.alarm_cancel));
        startButton.setOnClickListener(new Button.OnClickListener() {

            @Override
            public void onClick(View v) {
                resetAlarm();
            }
        });
    }

    private void resetAlarm() {
        countdownFinished = true;

        eggView.updateRotation(0);

        if (isTimerServiceBound) {
            boundTimerService.cancelAlarm();
        }
        clockTextView.setText(generateText(0, 0));
        eggView.setLocked(false);
        startButton();
    }

    void doBindService() {
        bindService(new Intent(TeggTimer.this,
                TimerService.class), mTimerServiceConnection, Context.BIND_AUTO_CREATE);
        isTimerServiceBound = true;
    }

    void doUnbindService() {
        if (isTimerServiceBound) {
            unbindService(mTimerServiceConnection);
            isTimerServiceBound = false;
        }
    }

    @Override
    public void onBackPressed() {
        if (!countdownFinished)
            resetAlarm();
        else
            super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        doUnbindService();
        super.onDestroy();
    }

    @Override
    public void updateAlarm(int time) {
        timeSet = time;
        clockTextView.setText(generateText(timeSet, 0));
    }

    @Override
    public void viewMeasured() {
        if (countdownFinished) {
            updateCountdown(timeSet * MINUTE);
        }
    }

    @Override
    public void updateCountdown(long time) {
        eggView.updateTime(time);
        int min = (int) (time / MINUTE);
        int sec = (int) ((time % MINUTE) / SECOND);
        clockTextView.setText(generateText(min, sec));
    }

    private static String generateText(int min, int sec) {
        // TODO: probably an easier way using SimpleDateFormat or similar?
        String minText = "%s%d";
        String secText = "%s%d";
        if (min < 10) {
            minText = String.format(Locale.getDefault(), minText, "0", min);
        } else
            minText = String.format(Locale.getDefault(), minText, "", min);
        if (sec < 10) {
            secText = String.format(Locale.getDefault(), secText, "0", sec);
        } else
            secText = String.format(Locale.getDefault(), secText, "", sec);
        return String.format(Locale.getDefault(), "%s:%s", minText, secText);
    }

    private ServiceConnection mTimerServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            boundTimerService = ((TimerService.LocalBinder) service).getService();
            boundTimerService.registerCallback(TeggTimer.this);
        }

        public void onServiceDisconnected(ComponentName className) {
            boundTimerService.unregisterCallback(TeggTimer.this);
            boundTimerService = null;
        }
    };
}