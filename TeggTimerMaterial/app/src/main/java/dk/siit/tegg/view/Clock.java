package dk.siit.tegg.view;

public class Clock {
    private float lastTouchX;
    private float lastTouchY;
    private float rotation = 0f;
    private float centerX;
    private float centerY;

    private int time;

    Clock() {
    }

    double calculateRotation(float nex, float ney) {
        boolean clockwiseRotation = true;

        float latestX = nex - centerX;
        float latestY = ney - centerY;

        // Calculate the distance moved
        float dx = latestX - lastTouchX;
        float dy = latestY - lastTouchY;

        // Only accept the counterclockwise rotations
        if (latestX > 0 && lastTouchX > 0 && latestY > 0 && lastTouchY > 0 && dx > 0 && dy < 0) {
            clockwiseRotation = false;
        }
        if (latestX > 0 && lastTouchX > 0 && latestY < 0 && lastTouchY < 0 && dx < 0 && dy < 0) {
            clockwiseRotation = false;
        }
        if (latestX < 0 && lastTouchX < 0 && latestY < 0 && lastTouchY < 0 && dx < 0 && dy > 0) {
            clockwiseRotation = false;
        }
        if (latestX < 0 && lastTouchX < 0 && latestY > 0 && lastTouchY > 0 && dx > 0 && dy > 0) {
            clockwiseRotation = false;
        }

        if (clockwiseRotation)
            return 0;

        // Find the lengths of the triangles sides using trig

        //Distance to center from last touch using trig
        double lastSide = lastTouchX / Math.cos(Math.atan2(lastTouchY, lastTouchX));
        double oppoSide = dx / Math.cos(Math.atan2(dy, dx));
        double firstSide = latestX / Math.cos(Math.atan2(latestY, latestX));

        // Do the pythagoras for the triangles sides
        double degrees = Math.toDegrees(Math.acos((Math.pow(lastSide, 2) + Math.pow(firstSide, 2) - Math.pow(oppoSide, 2)) / (2 * lastSide * firstSide)));
        // counter clockwise is negative
        degrees = -degrees;

        return degrees;
    }

    void setLastTouchX(float lastTouchX) {
        this.lastTouchX = lastTouchX - centerX;
    }

    void setLastTouchY(float lastTouchY) {
        this.lastTouchY = lastTouchY - centerY;
    }

    public float getRotation() {
        return rotation;
    }

    void setRotation(float rotation) {
        this.rotation = rotation;
    }

    void setCenterX(float centerX) {
        this.centerX = centerX;
    }

    void setCenterY(float centerY) {
        this.centerY = centerY;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }
}
