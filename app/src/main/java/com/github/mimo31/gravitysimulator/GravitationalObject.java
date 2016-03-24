package com.github.mimo31.gravitysimulator;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

/**
 * Created by Viktor on 3/11/2016.
 *
 * Class for representing one Object in the simulation of gravity.
 */
public class GravitationalObject {

    final int radius;
    final private int density;
    Vector2d position;
    Vector2d velocity;

    public GravitationalObject(Vector2d position, int radius, int density) {
        this.position = position;
        this.radius = radius;
        this.velocity = new Vector2d(0, 0);
        this.density = density;
    }

    public boolean doesCollide(GravitationalObject object) {
        return Math.sqrt(Math.pow(this.position.x - object.position.x, 2) + Math.pow(this.position.y - object.position.y, 2)) <= this.radius + object.radius;
    }

    public void draw(Canvas canvas, int centerColor) {
        Paint p = new Paint();
        int colorValue = (int) (255 - this.density / (float) 1000 * 255);
        p.setColor(Color.rgb(colorValue, colorValue, 255));
        canvas.drawCircle((float) this.position.x, (float) this.position.y, this.radius, p);
        if (centerColor != 0) {
            p.setColor(centerColor);
            canvas.drawCircle((float) this.position.x, (float) this.position.y, this.radius / 2, p);
        }
    }

    public Vector2d getGravitationalForce(GravitationalObject object) {
        Vector2d distanceVector = new Vector2d(object.position.x - this.position.x, object.position.y - this.position.y);
        return distanceVector.multiply(this.getMass() * object.getMass() / Math.pow(distanceVector.getLength(), 3));
    }

    public void drawInfo(Canvas canvas, float state) {
        Paint p = new Paint();
        int startX = (int) ((1 - MainActivity.getMovableViewPosition(state, 0)) * canvas.getWidth());
        p.setColor(Color.argb(127, 255, 255, 255));
        int changeVelocityY = canvas.getHeight() * 5 / 6;
        canvas.drawRect(new Rect(startX, canvas.getHeight() / 2, canvas.getWidth(), changeVelocityY), p);

        int backRemoveY = canvas.getHeight() * 11 / 12;
        p.setColor(Color.argb(127, 255, 0, 0));
        Rect changeVelocityRect = new Rect(startX, changeVelocityY, canvas.getWidth(), backRemoveY);
        canvas.drawRect(changeVelocityRect, p);

        int removeX = startX + canvas.getWidth() / 2;
        p.setColor(Color.argb(127, 255, 127, 127));
        Rect backRect = new Rect(startX, backRemoveY, removeX, canvas.getHeight());
        canvas.drawRect(backRect, p);

        Rect removeRect = new Rect(removeX, backRemoveY, canvas.getWidth(), canvas.getHeight());
        p.setColor(Color.argb(127, 0, 255, 255));
        canvas.drawRect(removeRect, p);

        int borderSize = canvas.getHeight() / 96;
        p.setColor(Color.BLACK);
        String[] quantityNames = new String[] { "Mass", "Density", "Radius", "Total speed" };
        Rect[] quantityBounds = new Rect[] { getInfoTableRect(startX, canvas, 0, 0), getInfoTableRect(startX, canvas, 0, 1), getInfoTableRect(startX, canvas, 0, 2), getInfoTableRect(startX, canvas, 0, 3) };
        StringDraw.drawMaxStrings(quantityNames, quantityBounds, borderSize, StringDraw.TextAlign.LEFT, canvas, p);

        String[] valueStrings = new String[] { String.valueOf(this.getMass()), String.valueOf(this.density), String.valueOf(this.radius), String.valueOf((int) Math.round(this.velocity.getLength())) };
        Rect[] valueBounds = new Rect[] { getInfoTableRect(startX, canvas, 1, 0), getInfoTableRect(startX, canvas, 1, 1), getInfoTableRect(startX, canvas, 1, 2), getInfoTableRect(startX, canvas, 1, 3) };
        StringDraw.drawMaxStrings(valueStrings, valueBounds, borderSize, StringDraw.TextAlign.RIGHT, canvas, p);

        String[] controlStrings = { "Change the velocity", "Back", "Remove this object" };
        Rect[] controlBounds = {changeVelocityRect, backRect, removeRect};
        StringDraw.drawMaxStrings(controlStrings, controlBounds, borderSize, StringDraw.TextAlign.MIDDLE, canvas, p);
    }

    private static Rect getInfoTableRect(int startX, Canvas canvas, int tableX, int tableY) {
        return new Rect(startX + tableX * canvas.getWidth() / 2, canvas.getHeight() / 2 + tableY * canvas.getHeight() / 12, startX + + (1 + tableX) * canvas.getWidth() / 2, canvas.getHeight() / 2 + (tableY + 1) * canvas.getHeight() / 12);
    }

    public double getMass() {
        return this.density * Math.pow(this.radius, 2) / 64;
    }
}
