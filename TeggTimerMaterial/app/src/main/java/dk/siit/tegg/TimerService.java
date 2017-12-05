package dk.siit.tegg;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static dk.siit.tegg.TeggTimer.SECOND;

public class TimerService extends Service {
    private NotificationManager mNM;
    private PendingIntent mAlarmSender;
    private final IBinder mBinder = new LocalBinder();
    private CountDownTimer mCountDownTimer;
    private List<CountdownCallback> mCountdownCallbackList;
    private AlarmManager mAm;

    @Override
    public void onCreate() {
        mCountdownCallbackList = new ArrayList<>();

        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        mAm = (AlarmManager)getSystemService(ALARM_SERVICE);

        Intent intent = new Intent(this, TeggTimer.class);
        intent.putExtra(TeggTimer.ALARM_BROADCAST, true);
        mAlarmSender = PendingIntent.getActivity(this, 0, intent, 0);
    }

    @Override
    public void onDestroy() {
        mNM.cancel(R.string.alarm_service_started);

        if(mAm!=null)
            mAm.cancel(mAlarmSender);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent!=null) {
            long firstTime = intent.getLongExtra(TeggTimer.ALARM_TIME, TeggTimer.MINUTE);

            setAlarm(firstTime);
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private void showNotification(long timeTarget) {
        CharSequence text = getText(R.string.alarm_service_started);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        builder.setTicker(text);

        Notification alarmStatusNotification = new Notification(android.R.drawable.stat_sys_warning, text,
                System.currentTimeMillis());

        PendingIntent alarmIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, TeggTimer.class), 0);


        String timeNotification = text + ": " +SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT).format(new Date(timeTarget));

        alarmStatusNotification.setLatestEventInfo(this, getText(R.string.alarm_service_label),
                timeNotification, alarmIntent);

        mNM.notify(R.string.alarm_service_started, alarmStatusNotification);
    }

    public void setAlarm(long time) {
        long firstTime = SystemClock.elapsedRealtime()+time;

        if(mAm!=null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                mAm.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        firstTime, mAlarmSender);
            } else {
                mAm.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        firstTime, mAlarmSender);
            }
        }

        mCountDownTimer = new CountDownTimer(time, SECOND) {

            @Override
            public void onTick(long millisUntilFinished) {
                for(CountdownCallback countdownCallback : mCountdownCallbackList) {
                    countdownCallback.updateCountdown(millisUntilFinished);
                }
            }

            @Override
            public void onFinish() {
                for(CountdownCallback countdownCallback : mCountdownCallbackList) {
                    countdownCallback.updateCountdown(0);
                }
            }

        };
        mCountDownTimer.start();

        showNotification(System.currentTimeMillis()+time);
    }


    public void cancelAlarm() {
        mCountDownTimer.cancel();

        if(mAm!=null)
            mAm.cancel(mAlarmSender);

        mNM.cancelAll();
    }

    public class LocalBinder extends Binder {
        TimerService getService() {
            return TimerService.this;
        }
    }

    public void registerCallback(CountdownCallback callback) {
        mCountdownCallbackList.add(callback);
    }

    public void unregisterCallback(CountdownCallback callback) {
        mCountdownCallbackList.remove(callback);
    }
}
