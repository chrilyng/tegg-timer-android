package dk.siit.tegg.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import dk.siit.tegg.AlarmCallback;
import dk.siit.tegg.R;
import dk.siit.tegg.TeggTimer;

public class EggView extends View {
    private static final int CLOCK_RATIO = 6; // 360 degrees divided by 60 minutes

    private List<AlarmCallback> alarmCallbacks;

    private boolean locked;

    private Drawable clockIcon;

    private int clockDiameter;

    private Clock clock;

    public EggView(Context context, AttributeSet attrs) {
        super(context, attrs);
        alarmCallbacks = new ArrayList<>();
        clock = new Clock();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();

        // Rotate the numbers around the center of the "egg"
        canvas.rotate(clock.getRotation(), clockDiameter / 2f, clockDiameter / 2f);

        // Draw the image on the canvas
        clockIcon.draw(canvas);
        canvas.restore();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
        int parentHeight = MeasureSpec.getSize(heightMeasureSpec);

        clockDiameter = parentWidth;
        clockIcon = getResources().getDrawable(R.drawable.ring_num);
        clockIcon.setBounds(0, 0, clockDiameter, clockDiameter);

        float centerX = clockDiameter / 2f;
        float centerY = clockDiameter / 2f;
        clock.setCenterX(centerX);
        clock.setCenterY(centerY);

        this.setMeasuredDimension(parentWidth, parentHeight);

        for (AlarmCallback callback : alarmCallbacks) {
            callback.viewMeasured();
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int action = event.getAction();
        if (!isLocked()) {
            switch (action) {
                case MotionEvent.ACTION_DOWN: {

                    float x = event.getX();
                    float y = event.getY();

                    clock.setLastTouchX(x);
                    clock.setLastTouchY(y);
                    // Remember where we started
                    break;
                }
                case MotionEvent.ACTION_MOVE: {
                    float x = event.getX();
                    float y = event.getY();

                    double degrees = clock.calculateRotation(x, y);

                    // Remember this touch position for the next move event

                    clock.setLastTouchX(x);
                    clock.setLastTouchY(y);
                    clock.setRotation(clock.getRotation() + (float) degrees);
                    int time = -(int) clock.getRotation() / CLOCK_RATIO;
                    clock.setTime(time);

                    if (Math.abs(degrees) > 1) {
                        notifyObservers(time);
                    }
                    // Invalidate to request a redraw
                    invalidate();
                    break;

                }
                //screen released
                case MotionEvent.ACTION_UP: {

                    int rotat = Math.round(clock.getRotation());

                    // round the 360 degrees to the 60 minutes in an hour
                    rotat = rotat / CLOCK_RATIO;
                    rotat = rotat * CLOCK_RATIO;
                    clock.setRotation(rotat);
                    int time = -rotat / CLOCK_RATIO;
                    clock.setTime(time);

                    invalidate();
                    notifyObservers(time);
                    break;
                }
                default:
                    break;
            }
        }
        return true;
    }

    public void updateTime(long timeLeft) {
        int fullRotation = (int) -(timeLeft / (10 * TeggTimer.SECOND));
        clock.setRotation(fullRotation);
        invalidate();
    }

    public void updateRotation(int rotation) {
        clock.setRotation(rotation);
        invalidate();
    }

    public void notifyObservers(int time) {
        for (AlarmCallback alarmCallback : alarmCallbacks) {
            alarmCallback.updateAlarm(time);
        }
    }

    public void registerAlarmCallback(AlarmCallback alarmCallback) {
        alarmCallbacks.add(alarmCallback);
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public boolean isLocked() {
        return locked;
    }

    public Clock getClock() {
        return clock;
    }
}
