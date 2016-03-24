package com.github.mimo31.gravitysimulator;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

/**
 * Created by Viktor on 3/11/2016.
 *
 * Class for drawing strings into rectangular bounds.
 */
public class StringDraw {


    public enum TextAlign {
        MIDDLE, UP, UP_RIGHT, RIGHT, DOWN_RIGHT, DOWN, DOWN_LEFT, LEFT, UP_LEFT
    }

    public static void drawMaxString(String s, Rect bounds, int borderSize, Canvas canvas, Paint p) {
        drawMaxString(s, bounds, borderSize, TextAlign.MIDDLE, canvas, p);
    }

    public static void drawMaxString(String s, Rect bounds, int borderSize, TextAlign align, Canvas canvas, Paint p) {
        bounds = applyBorders(bounds, borderSize);
        p.setTextSize(getMaxTextSize(s, bounds, p));
        fitString(s, bounds, align, canvas, p);
    }

    private static float getMaxTextSize(String s, Rect bounds, Paint p) {
        p.setTextSize(100);
        Rect bounds100 = new Rect();
        p.getTextBounds(s, 0, s.length(), bounds100);
        if ((bounds100.bottom - bounds100.top) / (float) (bounds100.right - bounds100.left) < (bounds.bottom - bounds.top) / (float) (bounds.right - bounds.left)) {
            return 100 * (bounds.right - bounds.left) / (float) (bounds100.right - bounds100.left);
        }
        else {
            return 100 * (bounds.bottom - bounds.top) / (float) (bounds100.bottom - bounds100.top);
        }
    }

    private static void fitString(String s, Rect bounds, TextAlign align, Canvas canvas, Paint p) {
        Rect textSize = new Rect();
        p.getTextBounds(s, 0, s.length(), textSize);
        int leftX = bounds.left - textSize.left;
        int middleX = (bounds.left + bounds.right - textSize.left - textSize.right) / 2;
        int rightX = bounds.right - textSize.right;
        int upY = bounds.top - textSize.top;
        int middleY = (bounds.top + bounds.bottom - textSize.top - textSize.bottom) / 2;
        int downY = bounds.bottom - textSize.bottom;
        int x;
        int y;
        switch (align) {
            case DOWN_LEFT:
                x = leftX;
                y = downY;
                break;
            case DOWN:
                x = middleX;
                y = downY;
                break;
            case DOWN_RIGHT:
                x = rightX;
                y = downY;
                break;
            case LEFT:
                x = leftX;
                y = middleY;
                break;
            case RIGHT:
                x = rightX;
                y = middleY;
                break;
            case UP:
                x = middleX;
                y = upY;
                break;
            case UP_LEFT:
                x = leftX;
                y = upY;
                break;
            case UP_RIGHT:
                x = rightX;
                y = upY;
                break;
            default:
                x = middleX;
                y = middleY;

        }
        canvas.drawText(s, x, y, p);
    }

    public static void drawMaxStrings(String[] strings, Rect[] bounds, int borderSize, TextAlign align, Canvas canvas, Paint p) {
        for (int i = 0; i < bounds.length; i++) {
            bounds[i] = applyBorders(bounds[i], borderSize);
        }
        float smallestTextSize = getMaxTextSize(strings[0], bounds[0], p);
        for (int i = 1; i < strings.length; i++) {
            float currentTextSize = getMaxTextSize(strings[i], bounds[i], p);
            if (currentTextSize < smallestTextSize) {
                smallestTextSize = currentTextSize;
            }
        }
        p.setTextSize(smallestTextSize);
        for (int i = 0; i < strings.length; i++) {
            fitString(strings[i], bounds[i], align, canvas, p);
        }
    }

    private static Rect applyBorders(Rect original, int borderSize) {
        return new Rect(original.left + borderSize, original.top + borderSize, original.right - borderSize, original.bottom - borderSize);
    }
}
