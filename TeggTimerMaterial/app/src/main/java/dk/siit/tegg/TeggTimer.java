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

import dk.siit.tegg.view.EggView;

public class TeggTimer extends Activity implements AlarmCallback, CountdownCallback {
    private EggView mRingNum;
    private TextView mClock;
    private long mRemainingTime;
    private Intent mTimerService;
    private TimerService mBoundTimerService;
    private boolean mIsTimerServiceBound = false;
    private boolean mFinished = true;
    protected Ringtone mRingtone;
    private int mTimeSet = 0;

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

        mClock = findViewById(R.id.clock);
        mRingNum = findViewById(R.id.ring_num);
        mRingNum.registerAlarmCallback(this);

        Uri myUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        mRingtone = RingtoneManager.getRingtone(this, myUri);

        if (getIntent().getBooleanExtra(ALARM_BROADCAST, false) && savedInstanceState == null) {
            mFinished = true;

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
                    mRingtone.stop();
                    resetAlarm();
                }
            });
            builder.setMessage(getString(R.string.alarm_service_finished));
            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    mRingtone.stop();
                    resetAlarm();
                }
            });
            Dialog dialog = builder.create();
            dialog.show();

            mRingtone.play();
        }
        if (mFinished)
            startButton();
    }

    public void startButton() {
        Button start = findViewById(R.id.ring_start);
        start.setText(getString(R.string.alarm_start));
        start.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View arg0) {
                stopButton();

                mFinished = false;

                int rot = (int) -mRingNum.getClock().getRotation();
                // Wheel updates every ten seconds
                mRemainingTime = rot * 10 * SECOND;

                mTimerService = new Intent(TeggTimer.this, TimerService.class);
                mTimerService.putExtra(ALARM_TIME, mRemainingTime);
                startService(mTimerService);
                doBindService();
                mRingNum.setLocked(true);
            }
        });
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mFinished = savedInstanceState.getBoolean(EXTRA_FINISHED, true);
        if (!mFinished) {
            doBindService();
            stopButton();
        } else {
            mTimeSet = savedInstanceState.getInt(EXTRA_TIME_SET);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(EXTRA_FINISHED, mFinished);
        if (mFinished) {
            outState.putInt(EXTRA_TIME_SET, mTimeSet);
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
        mFinished = true;

        mRingNum.updateRotation(0);

        if (mIsTimerServiceBound) {
            mBoundTimerService.cancelAlarm();
        }
        mClock.setText(generateText(0, 0));
        mRingNum.setLocked(false);
        startButton();
    }

    void doBindService() {
        bindService(new Intent(TeggTimer.this,
                TimerService.class), mTimerServiceConnection, Context.BIND_AUTO_CREATE);
        mIsTimerServiceBound = true;
    }

    void doUnbindService() {
        if (mIsTimerServiceBound) {
            unbindService(mTimerServiceConnection);
            mIsTimerServiceBound = false;
        }
    }

    @Override
    public void onBackPressed() {
        if (!mFinished)
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
        mTimeSet = time;
        mClock.setText(generateText(mTimeSet, 0));
    }

    @Override
    public void viewMeasured() {
        if (mFinished) {
            updateCountdown(mTimeSet * MINUTE);
        }
    }

    @Override
    public void updateCountdown(long time) {
        mRingNum.updateTime(time);
        int min = (int) (time / MINUTE);
        int sec = (int) ((time % MINUTE) / SECOND);
        mClock.setText(generateText(min, sec));
    }

    public static String generateText(int min, int sec) {
        String minText;
        String secText;
        if (min < 10) {
            minText = "0" + min;
        } else
            minText = "" + min;
        if (sec < 10) {
            secText = "0" + sec;
        } else
            secText = "" + sec;
        return minText + ":" + secText;
    }

    private ServiceConnection mTimerServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mBoundTimerService = ((TimerService.LocalBinder) service).getService();
            mBoundTimerService.registerCallback(TeggTimer.this);
        }

        public void onServiceDisconnected(ComponentName className) {
            mBoundTimerService.unregisterCallback(TeggTimer.this);
            mBoundTimerService = null;
        }
    };
}