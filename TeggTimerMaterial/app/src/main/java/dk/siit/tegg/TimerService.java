package dk.siit.tegg;

import android.app.AlarmManager;
import android.app.NotificationChannel;
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
import android.support.v4.app.NotificationManagerCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static dk.siit.tegg.TeggTimer.SECOND;

public class TimerService extends Service {
    private static final String CHANNEL_ID = "7777";

    private NotificationManagerCompat notificationManagerCompat;
    private PendingIntent mAlarmSender;
    private final IBinder mBinder = new LocalBinder();
    private CountDownTimer mCountDownTimer;
    private List<CountdownCallback> mCountdownCallbackList;
    private AlarmManager mAm;
    private int notificationId = 0;

    @Override
    public void onCreate() {
        notificationId = 0;
        mCountdownCallbackList = new ArrayList<>();

        notificationManagerCompat = NotificationManagerCompat.from(this);

        mAm = (AlarmManager) getSystemService(ALARM_SERVICE);

        Intent intent = new Intent(this, TeggTimer.class);
        intent.putExtra(TeggTimer.ALARM_BROADCAST, true);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        mAlarmSender = PendingIntent.getActivity(this, 0, intent, 0);
    }

    @Override
    public void onDestroy() {
        notificationManagerCompat.cancel(notificationId);

        if (mAm != null)
            mAm.cancel(mAlarmSender);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
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
        if (notificationId == 0) {
            ++notificationId;
            createNotificationChannel();
        }

        PendingIntent alarmIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, TeggTimer.class), 0);

        String timeNotification = text + ": " + SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT).format(new Date(timeTarget));

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setContentTitle(text)
                .setContentText(timeNotification)
                .setContentIntent(alarmIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        // notificationId is a unique int for each notification that you must define
        notificationManagerCompat.notify(notificationId, builder.build());
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.alarm_service_label);
            String description = getString(R.string.alarm_service_label);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null)
                notificationManager.createNotificationChannel(channel);
        }
    }


    public void setAlarm(long time) {
        long firstTime = SystemClock.elapsedRealtime() + time;

        if (mAm != null) {
            mAm.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    firstTime, mAlarmSender);
        }

        mCountDownTimer = new CountDownTimer(time, SECOND) {

            @Override
            public void onTick(long millisUntilFinished) {
                for (CountdownCallback countdownCallback : mCountdownCallbackList) {
                    countdownCallback.updateCountdown(millisUntilFinished);
                }
            }

            @Override
            public void onFinish() {
                for (CountdownCallback countdownCallback : mCountdownCallbackList) {
                    countdownCallback.updateCountdown(0);
                }
            }

        };
        mCountDownTimer.start();

        showNotification(System.currentTimeMillis() + time);
    }

    public void cancelAlarm() {
        notificationId = 0;
        mCountDownTimer.cancel();

        if (mAm != null)
            mAm.cancel(mAlarmSender);

        notificationManagerCompat.cancelAll();
    }

    class LocalBinder extends Binder {
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
