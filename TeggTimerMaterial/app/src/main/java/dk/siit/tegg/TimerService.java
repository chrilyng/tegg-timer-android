package dk.siit.tegg;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TimerService extends Service {

    NotificationManager mNM;

    @Override
    public void onCreate() {
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        Intent intent = new Intent(this, TeggTimer.class);
        intent.putExtra(TeggTimer.ALARM_BROADCAST, true);
        mAlarmSender = PendingIntent.getActivity(this, 0, intent, 0);
    }

    @Override
    public void onDestroy() {
        mNM.cancel(R.string.alarm_service_started);

        AlarmManager am = (AlarmManager)getSystemService(ALARM_SERVICE);
        am.cancel(mAlarmSender);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        long firstTime = intent.getLongExtra(TeggTimer.ALARM_TIME,10000);

        setAlarm(firstTime);


        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {

        return mBinder;
    }

    /**
     * Show a notification while this service is running.
     */
    private void showNotification(long timeTarget) {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = getText(R.string.alarm_service_started);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        builder.setTicker("Countdown");
        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(android.R.drawable.stat_sys_warning, text,
                System.currentTimeMillis());

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, TeggTimer.class), 0);


        String timeNotification = text + ": " +SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT).format(new Date(timeTarget));

        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(this, getText(R.string.alarm_service_label),
                       timeNotification, contentIntent);

        // Send the notification.
        // We use a layout id because it is a unique number.  We use it later to cancel.
        mNM.notify(R.string.alarm_service_started, notification);
    }


    private PendingIntent mAlarmSender;


    public void setAlarm(long time) {
        long firstTime = SystemClock.elapsedRealtime()+time;

        AlarmManager am = (AlarmManager)getSystemService(ALARM_SERVICE);

        am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                firstTime, mAlarmSender);

        // show the icon in the status bar
        showNotification(System.currentTimeMillis()+time);

        Toast.makeText(this, R.string.repeating_scheduled,
                Toast.LENGTH_LONG).show();
    }


    public void cancelAlarm() {
        // And cancel the alarm.
        AlarmManager am = (AlarmManager)getSystemService(ALARM_SERVICE);
        am.cancel(mAlarmSender);

        mNM.cancelAll();

        // Tell the user about what we did.
        Toast.makeText(this, R.string.repeating_unscheduled,
                Toast.LENGTH_LONG).show();
    }

    public class LocalBinder extends Binder {
        TimerService getService() {
            return TimerService.this;
        }
    }

    private final IBinder mBinder = new LocalBinder();
}
