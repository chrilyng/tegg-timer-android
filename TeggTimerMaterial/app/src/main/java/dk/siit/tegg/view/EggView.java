package dk.siit.tegg.view;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import dk.siit.tegg.R;
import dk.siit.tegg.TeggTimer;

public class EggView extends View {

    private List<Handler> handlers;

    public List<Handler> getHandlers() {
        return handlers;
    }

    private boolean locked;

    //View
    private Drawable clockIcon;

    private int clockDiameter;

    private Clock clock;

    public EggView(Context context, AttributeSet attrs) {
        super(context, attrs);
        clockDiameter = getResources().getDimensionPixelSize(R.dimen.clock_diameter);
        clockIcon = getResources().getDrawable(R.drawable.ring_num);
        clockIcon.setBounds(0, 0, clockDiameter, clockDiameter);

        float centerx = clockDiameter/2+this.getLeft();
        float centery = clockDiameter/2+this.getTop();

        setClock(new Clock(centerx, centery));
        
        handlers = new ArrayList<Handler>();

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        //Rotate the numbers around the center of the "egg"

        canvas.rotate((float) getClock().getRotation(), clockDiameter/2, clockDiameter/2);
        //Draw the image on the canvas
        clockIcon.draw(canvas);
        canvas.restore();
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int action = event.getAction();
        if(!isLocked()){
        switch(action) {
            case MotionEvent.ACTION_DOWN: {

                float x = event.getX();
                float y = event.getY();

                getClock().setLastTouchX(x);
                getClock().setLastTouchY(y);
                // Remember where we started
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                float x = event.getX();
                float y = event.getY();

                double degrees = getClock().calculateRotation(x, y);

                // Remember this touch position for the next move event

                getClock().setLastTouchX(x);
                getClock().setLastTouchY(y);
                getClock().setRotation(getClock().getRotation() + (float)degrees);

                if(Math.abs(degrees)>1) {
                    notifyObservers();
                }
                // Invalidate to request a redraw
                invalidate();
                break;

            }
            //screen released
            case MotionEvent.ACTION_UP: {

                int rotat = (int) Math.round(getClock().getRotation());

                // round the 360 degrees to the 60 minutes in an hour
                rotat = rotat/6;
                rotat = rotat*6;
                getClock().setRotation(rotat);

                invalidate();
                notifyObservers();
                break;
            }
            default:
                break;
        }
        }
        return true;
    }

    public void updateTime(long timeLeft) {
        int fullRotation = (int) -(timeLeft/10* TeggTimer.SECOND);
        getClock().setRotation(fullRotation);
        invalidate();
    }

    public void updateRotation(int rotation) {
        getClock().setRotation(rotation);
        invalidate();
    }

    public void notifyObservers() {
        for (Handler handler: handlers) {

            Message message = handler.obtainMessage();
            getClock().setTime(-(int) getClock().getRotation() / 6);
            message.arg1= getClock().getTime();
            message.sendToTarget();
        }
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

    public void setClock(Clock clock) {
        this.clock = clock;
    }
}
