package com.github.mimo31.gravitysimulator;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by mimo31 on 5/6/2016.
 * <p/>
 * An class to handle object object gravity physics and displaying.
 */
public class GravitySpace {

    private List<GravitationalObject> objects = new ArrayList<>();
    private Vector2d viewPosition = new Vector2d(0, 0);
    /*
     * Represents the level of the zoom of the map. That is the natural logarithm of the actual enlargement.
     */
    double zoomLevel = 0;

    /*
    * Moves the objects according to the laws of gravity.
     */
    public void updateObjects(double deltaTime) {
        for (int i = 0; i < this.objects.size(); i++) {
            GravitationalObject currentObject = this.objects.get(i);
            if (currentObject.getMass() != 0) {
                Vector2d totalForce = new Vector2d(0, 0);
                for (int j = 0; j < this.objects.size(); j++) {
                    if (j != i) {
                        totalForce = totalForce.add(currentObject.getGravitationalForce(this.objects.get(j)));
                    }
                }
                currentObject.velocity = currentObject.velocity.add(totalForce.multiply(deltaTime / currentObject.getMass()));
            }
            currentObject.position = currentObject.position.add(currentObject.velocity.multiply(deltaTime));
        }
        for (int i = 0; i < this.objects.size(); i++) {
            for (int j = i + 1; j < this.objects.size(); j++) {
                GravitationalObject o1 = this.objects.get(i);
                GravitationalObject o2 = this.objects.get(j);
                if (o1.doesCollide(o2)) {
                    Vector2d distanceVector = o1.position.subtract(o2.position);
                    double collisionFactor = o1.velocity.subtract(o2.velocity).dot(distanceVector) / distanceVector.dot(distanceVector);
                    Vector2d addVector = distanceVector.multiply(2 / (o1.getMass() + o2.getMass()) * collisionFactor);
                    o1.velocity = o1.velocity.subtract(addVector.multiply(o2.getMass()));
                    o2.velocity = o2.velocity.add(addVector.multiply(o1.getMass()));
                    Vector2d totalShift = distanceVector.multiply((o1.radius + o2.radius) / distanceVector.getLength() * 2 - 2);
                    double o1VelocityFraction = o1.velocity.getLength() / (o1.velocity.getLength() + o2.velocity.getLength());
                    double o2VelocityFraction = 1 - o1VelocityFraction;
                    Vector2d o1Shift = totalShift.multiply(o1VelocityFraction);
                    Vector2d o2Shift = totalShift.multiply(-o2VelocityFraction);
                    o1.position = o1.position.add(o1Shift);
                    o2.position = o2.position.add(o2Shift);
                }
            }
        }
    }

    public Bundle putToBundle() {
        Bundle bundle = new Bundle();
        bundle.putDouble("viewX", this.viewPosition.x);
        bundle.putDouble("viewY", this.viewPosition.y);
        bundle.putParcelableArray("objects", this.objects.toArray(new GravitationalObject[this.objects.size()]));
        bundle.putDouble("zoom", this.zoomLevel);
        return bundle;
    }

    public GravitySpace(Bundle bundle) {
        this.viewPosition = new Vector2d(bundle.getDouble("viewX"), bundle.getDouble("viewY"));
        this.objects = new ArrayList<>(Arrays.asList((GravitationalObject[]) bundle.getParcelableArray("objects")));
        this.zoomLevel = bundle.getDouble("zoom");
    }

    public GravitySpace() {

    }

    public void draw(Canvas canvas) {
        double enlargement = this.getEnlargement();
        Paint p = new Paint();
        for (int i = 0; i < this.objects.size(); i++) {
            GravitationalObject currentObject = this.objects.get(i);
            Vector2d realCenterVector = this.getViewLocation(currentObject.position, canvas.getWidth(), canvas.getHeight());
            double objectRealSize = currentObject.radius * enlargement;
            Rect enclosingRect = new Rect((int) (realCenterVector.x - objectRealSize), (int) (realCenterVector.y - objectRealSize), (int) (realCenterVector.x + objectRealSize), (int) (realCenterVector.y + objectRealSize));
            if (enclosingRect.intersect(0, 0, canvas.getWidth(), canvas.getHeight())) {
                p.setColor(currentObject.getColor());
                canvas.drawCircle((float) realCenterVector.x, (float) realCenterVector.y, (float) objectRealSize, p);
            }
        }
    }

    public void drawSpecific(Canvas canvas, GravitationalObject object, int centerColor, boolean withVelocity) {
        double enlargement = this.getEnlargement();
        Paint p = new Paint();
        Vector2d realCenterVector = this.getViewLocation(object.position, canvas.getWidth(), canvas.getHeight());
        double objectRealSize = object.radius * enlargement;
        Rect enclosingRect = new Rect((int) (realCenterVector.x - objectRealSize), (int) (realCenterVector.y - objectRealSize), (int) (realCenterVector.x + objectRealSize), (int) (realCenterVector.y + objectRealSize));
        if (enclosingRect.intersect(0, 0, canvas.getWidth(), canvas.getHeight())) {
            p.setColor(object.getColor());
            canvas.drawCircle((float) realCenterVector.x, (float) realCenterVector.y, (float) objectRealSize, p);
            if (centerColor != 0) {
                p.setColor(centerColor);
                canvas.drawCircle((float) realCenterVector.x, (float) realCenterVector.y, (float) objectRealSize / 2, p);
            }
        }
        if (withVelocity) {
            p.setColor(Color.RED);
            this.drawVelocity(canvas, p, realCenterVector, object.velocity.multiply(enlargement));
        }
    }

    /*
     * Draws an arrow from the center of the passed GravitationalObject pointing in the direction of the velocity of the object.
     */
    private void drawVelocity(Canvas canvas, Paint p, Vector2d position, Vector2d velocity) {
        if (velocity.x != 0 || velocity.y != 0) {
            canvas.save();
            float totalSpeed = (float) velocity.getLength() * 32;
            canvas.translate((float) position.x, (float) position.y);
            canvas.rotate((float) (Math.atan2(velocity.y, velocity.x) / Math.PI * 180));
            canvas.scale(totalSpeed / 1000, totalSpeed / 1000);
            canvas.drawRect(new Rect(0, -50, 800, 50), p);
            Path trianglePath = new Path();
            trianglePath.moveTo(800, -100);
            trianglePath.lineTo(1000, 0);
            trianglePath.lineTo(800, 100);
            trianglePath.lineTo(800, -100);
            trianglePath.close();
            canvas.drawPath(trianglePath, p);
            canvas.restore();
        }
    }

    public void addObject(GravitationalObject object) {
        this.objects.add(object);
    }

    public GravitationalObject getObject(int index) {
        return this.objects.get(index);
    }

    public int getNumberOfObjects() {
        return this.objects.size();
    }

    public void removeObject(int index) {
        this.objects.remove(index);
    }

    /*
     * @return The vector in the gravitational space that corresponds to the passed onViewLocation vector in the view.
     */
    public Vector2d getSpaceLocation(Vector2d onViewLocation, int viewWidth, int viewHeight) {
        return this.viewPosition.add(onViewLocation.subtract(new Vector2d(viewWidth, viewHeight).multiply(1 / (double) 2)).multiply(1 / this.getEnlargement()));
    }

    private Vector2d getViewLocation(Vector2d onSpaceLocation, int viewWidth, int viewHeight) {
        return onSpaceLocation.subtract(this.viewPosition).multiply(this.getEnlargement()).add(new Vector2d(viewWidth / 2, viewHeight / 2));
    }

    /*
     * @param spaceLocation The vector to find.
     * @return The index of the GravitationalObject that contains the passed spaceLocation. If no object contains this location -1 is returned.
     */
    public int isContainedIn(Vector2d spaceLocation) {
        for (int i = 0; i < this.objects.size(); i++) {
            GravitationalObject currentObject = this.objects.get(i);
            if (currentObject.position.subtract(spaceLocation).selfDot() <= Math.pow(currentObject.radius, 2)) {
                return i;
            }
        }
        return -1;
    }

    public double getEnlargement() {
        return Math.exp(this.zoomLevel);
    }

    public void drawObjectVelocity(Canvas canvas, int index) {
        this.drawObjectVelocity(canvas, this.objects.get(index));
    }

    public void drawObjectVelocity(Canvas canvas, GravitationalObject object) {
        Paint p = new Paint();
        p.setColor(Color.RED);
        this.drawVelocity(canvas, p, this.getViewLocation(object.position, canvas.getWidth(), canvas.getHeight()), object.velocity.multiply(this.getEnlargement()));
    }

    public boolean doesCollide(GravitationalObject object) {
        for (int i = 0; i < this.objects.size(); i++) {
            if (this.objects.get(i).doesCollide(object)) {
                return true;
            }
        }
        return false;
    }

    public void scale(double scaleFactor, Vector2d focusPoint) {
        this.viewPosition = focusPoint.multiply(1 - 1 / scaleFactor).add(this.viewPosition.multiply(1 / scaleFactor));
        this.zoomLevel += Math.log(scaleFactor);
    }

    public void moveView(Vector2d positionChange) {
        this.viewPosition = this.viewPosition.add(positionChange);
    }

    public Vector2d getViewPosition() {
        return this.viewPosition;
    }

    public void goToTheNearestObject() {
        if (objects.size() == 0) {
            return;
        }
        double leastDistance = (this.viewPosition.subtract(objects.get(0).position)).selfDot();
        int leastDistanceIndex = 0;
        for (int i = 1; i < objects.size(); i++) {
            double distance = (this.viewPosition.subtract(objects.get(i).position)).selfDot();
            if (distance < leastDistance) {
                leastDistance = distance;
                leastDistanceIndex = i;
            }
        }
        this.viewPosition = this.objects.get(leastDistanceIndex).position;
    }
}
