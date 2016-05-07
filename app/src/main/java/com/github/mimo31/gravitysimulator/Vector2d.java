package com.github.mimo31.gravitysimulator;

import android.graphics.PointF;

/**
 * Created by Viktor on 3/11/2016.
 *
 * Class for representing a 2D Vector.
 */
public class Vector2d {

    public final double x;
    public final double y;

    public Vector2d(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Vector2d(PointF p) {
        this.x = p.x;
        this.y = p.y;
    }

    public double dot(Vector2d v) {
        return this.x * v.x + this.y * v.y;
    }

    public Vector2d subtract(Vector2d v) {
        return new Vector2d(this.x - v.x, this.y - v.y);
    }

    public double getLength() {
        return Math.sqrt(Math.pow(this.x, 2) + Math.pow(this.y, 2));
    }

    public Vector2d add(Vector2d v) {
        return new Vector2d(this.x + v.x, this.y + v.y);
    }

    public Vector2d multiply(double factor) {
        return new Vector2d(this.x * factor, this.y * factor);
    }

    public double selfDot() {
        return this.x * this.x + this.y * this.y;
    }
}
