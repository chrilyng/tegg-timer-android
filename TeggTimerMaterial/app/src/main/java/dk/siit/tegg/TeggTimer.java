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
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import dk.siit.tegg.view.EggView;

public class TeggTimer extends Activity implements AlarmCallback {
	private List<CountDownTimer> mTimers;
	private EggView mRingNum;
    private long mRemainingTime;
    private Intent mTimerService;

    private boolean mFinished = false;

    protected Ringtone mRingtone;

    public static final int MINUTE = 60000;
    public static final int SECOND = 1000;

    public static final String ALARM_BROADCAST = "TIMES_UP";
    public static final String ALARM_TIME = "FIRST_TIME";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_clock);

        Uri myUri =  RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        mRingtone = RingtoneManager.getRingtone(this, myUri);


        if(getIntent().getBooleanExtra(ALARM_BROADCAST, false)) {
            doBindService();

            mFinished = true;

            Window window = getWindow();

            window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
            window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

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
            builder.setTitle(getString(R.string.alarm_service_finished));
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

        mRingNum = (EggView) findViewById(R.id.ring_num);
        mRingNum.registerAlarmCallback(this);
        
        mTimers = new ArrayList<CountDownTimer>();

        startButton();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onUserInteraction() {
       	super.onUserInteraction();
    }

    public void startButton() {
    	Button start = (Button) findViewById(R.id.ring_start);
        start.setText(getString(R.string.alarm_start));
        start.setOnClickListener(new Button.OnClickListener() {
        	public void onClick(View arg0) {
        		stopButton();


                mFinished = false;

        		int rot = (int)-mRingNum.getClock().getRotation();
                // Wheel updates every ten seconds
                mRemainingTime = rot*10*SECOND;

                mTimerService = new Intent(TeggTimer.this, TimerService.class);
                mTimerService.putExtra(ALARM_TIME, mRemainingTime);
                startService(mTimerService);
                doBindService();
        		mTimers.add(new CountDownTimer(mRemainingTime, SECOND) {

                    @Override
                    public void onTick(long millisUntilFinished) {
                        TextView clock = (TextView) findViewById(R.id.clock);
                        mRingNum.updateTime(millisUntilFinished);
                        int min = (int) (millisUntilFinished / MINUTE);
                        int sec = (int) ((millisUntilFinished % MINUTE) / SECOND);
                        clock.setText(updateText(min, sec));
                    }

                    @Override
                    public void onFinish() {
                        mFinished = true;
                    }
                });
				for (CountDownTimer timer : mTimers) {
					timer.start();
				}
				mRingNum.setLocked(true);
        	}
        });
    }
    
    public void stopButton() {
    	Button startButton = (Button) findViewById(R.id.ring_start);
        startButton.setText(getString(R.string.alarm_cancel));
        startButton.setOnClickListener(new Button.OnClickListener() {

            @Override
            public void onClick(View v) {
                resetAlarm();
            }
        });

    }
    
    public String updateText(int min, int sec) {

    	String minText;
    	String secText;
    	if(min<10) {
    		minText = "0"+min;
    	} else
    		minText= ""+min;
    	if(sec<10) {
    		secText = "0"+sec;
    	} else
    		secText= ""+sec;
    	return minText+":"+secText;
    }

    private void resetAlarm() {
        mFinished = true;

        TextView clock = (TextView) findViewById(R.id.clock);
        mRingNum.updateRotation(0);
        for (CountDownTimer timer : mTimers) {
            timer.cancel();
        }
        mTimers.clear();

        if(mIsTimerServiceBound) {
            mBoundTimerService.cancelAlarm();
        }
        clock.setText(updateText(0,0));
        mRingNum.setLocked(false);
        startButton();
    }


    private TimerService mBoundTimerService;

    private ServiceConnection mTimerServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mBoundTimerService = ((TimerService.LocalBinder)service).getService();
        }

        public void onServiceDisconnected(ComponentName className) {
            mBoundTimerService = null;
        }
    };

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

    private boolean mIsTimerServiceBound = false;

    @Override
    public void onBackPressed() {
        if(!mFinished)
            resetAlarm();
        else
            finish();
        //super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        doUnbindService();
    }

    @Override
    public void updateAlarm(int time) {
        TextView clock = (TextView) findViewById(R.id.clock);
        clock.setText(updateText(time, 0));
    }
}