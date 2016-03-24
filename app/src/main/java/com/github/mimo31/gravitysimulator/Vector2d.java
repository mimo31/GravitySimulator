package com.github.mimo31.gravitysimulator;

/**
 * Created by Viktor on 3/11/2016.
 */
public class Vector2d {

    public double x;
    public double y;

    public Vector2d(double x, double y) {
        this.x = x;
        this.y = y;
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
}
