package com.github.mimo31.gravitysimulator;

import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AlertDialog;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

/**
 * Created by mimo31 on 3/20/2016.
 *
 * View for the main field where gravity is simulated.
 */
public class GravityView extends View implements DialogInterface.OnClickListener{

    private final MainActivity attachedTo;
    private final GestureDetectorCompat gestureDetector;
    private final ScaleGestureDetector scaleDetector;
    private boolean isAddingObjectValid;
    private boolean positionConfirmed;
    private float confirmHidingState;
    private boolean simulationPaused;
    private int objectInfoIndex = -1;
    private float objectInfoState;
    private GravitationalObject lastObjectInfoShown;
    private boolean changingVelocity;
    private GravitySpace space = new GravitySpace();

    public GravityView(MainActivity attachedTo) {
        super(attachedTo.getApplicationContext());
        this.attachedTo = attachedTo;
        this.gestureDetector = new GestureDetectorCompat(attachedTo.getApplicationContext(), new GestureListener(this));
        this.scaleDetector = new ScaleGestureDetector(attachedTo.getApplicationContext(), new ScaleListener(this));
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        Paint p = new Paint();
        p.setColor(Color.BLACK);
        canvas.drawRect(new Rect(0, 0, canvas.getWidth(), canvas.getHeight()), p);
        this.space.draw(canvas);
        if (this.changingVelocity) {
            this.space.drawObjectVelocity(canvas, this.lastObjectInfoShown);
            p.setColor(Color.WHITE);
            Rect confirmButton = new Rect(0, canvas.getHeight() * 15 / 16, canvas.getWidth(), canvas.getHeight());
            canvas.drawRect(confirmButton, p);
            p.setColor(Color.BLACK);
            StringDraw.drawMaxString("Confirm", confirmButton, canvas.getHeight() / 64, canvas, p);
        }
        if (this.attachedTo.addingObject != null) {
            this.space.drawSpecific(canvas, this.attachedTo.addingObject, this.isAddingObjectValid ? Color.GREEN : Color.RED, this.positionConfirmed);
        }
        if (this.confirmHidingState != 0 || this.attachedTo.addingObject != null) {
            int alpha = this.attachedTo.addingObject != null ? 127 : (int) (this.confirmHidingState * 127);
            p.setColor(Color.argb(alpha, 255, 255, 255));
            canvas.drawRect(new Rect(0, canvas.getHeight() * 15 / 16, canvas.getWidth(), canvas.getHeight()), p);
            p.setColor(Color.BLACK);
            StringDraw.drawMaxString("Cancel", new Rect(0, canvas.getHeight() * 15 / 16, canvas.getWidth() / 2, canvas.getHeight()), canvas.getHeight() / 64, canvas, p);
            StringDraw.drawMaxString(this.positionConfirmed ? "Confirm velocity" : "Confirm position", new Rect(canvas.getWidth() / 2, canvas.getHeight() * 15 / 16, canvas.getWidth(), canvas.getHeight()), canvas.getHeight() / 64, canvas, p);
        }
        if (this.simulationPaused) {
            p.setColor(Color.argb(63, 255, 255, 255));
            int squareHalfSideLength = canvas.getHeight() / 16;
            canvas.drawRect(new Rect(canvas.getWidth() / 2 - squareHalfSideLength, canvas.getHeight() / 2 - squareHalfSideLength, canvas.getWidth() / 2 - squareHalfSideLength / 2, canvas.getHeight() / 2 + squareHalfSideLength), p);
            canvas.drawRect(new Rect(canvas.getWidth() / 2 + squareHalfSideLength / 2, canvas.getHeight() / 2 - squareHalfSideLength, canvas.getWidth() / 2 + squareHalfSideLength, canvas.getHeight() / 2 + squareHalfSideLength), p);
        }
        if (this.objectInfoState != 0) {
            GravitationalObject objectToUse;
            if (this.objectInfoIndex == -1) {
                objectToUse = this.lastObjectInfoShown;
            }
            else {
                objectToUse = this.space.getObject(this.objectInfoIndex);
                this.space.drawObjectVelocity(canvas, this.objectInfoIndex);
            }
            objectToUse.drawInfo(canvas, MainActivity.getMovableViewPosition(this.objectInfoState, 0));
        }
        p.setColor(Color.WHITE);
        Rect menuButtonRect = new Rect(0, 0, canvas.getWidth(), canvas.getHeight() / 16);
        canvas.drawRect(menuButtonRect, p);
        if (this.attachedTo.addingObject == null && !this.changingVelocity) {
            p.setColor(Color.BLACK);
            StringDraw.drawMaxString("Menu", menuButtonRect, canvas.getHeight() / 128, canvas, p);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        if (!this.attachedTo.paused || this.attachedTo.addingObject != null) {
            this.gestureDetector.onTouchEvent(event);
            this.scaleDetector.onTouchEvent(event);
        }
        return true;
    }

    void validateAddingObject() {
        this.isAddingObjectValid = !this.space.doesCollide(this.attachedTo.addingObject);
    }

    void update() {
        boolean doInvalidate = false;
        if (this.confirmHidingState != 0) {
            this.confirmHidingState -= 0.08;
            if (this.confirmHidingState < 0) {
                this.confirmHidingState = 0;
            }
            doInvalidate = true;
        }
        if (!this.attachedTo.paused && !this.simulationPaused && !this.changingVelocity) {
            for (int i = 0; i < 64; i++) {
                this.space.updateObjects(1 / (double) 64);
            }
            doInvalidate = true;
        }
        if (this.objectInfoIndex != -1) {
            if (changingVelocity) {
                if (this.objectInfoState != 0) {
                    this.objectInfoState -= 0.08;
                    if (this.objectInfoState < 0) {
                        this.objectInfoState = 0;
                    }
                    doInvalidate = true;
                }
            }
            else {
                if (this.objectInfoState != 1) {
                    this.objectInfoState += 0.08;
                    if (this.objectInfoState > 1) {
                        this.objectInfoState = 1;
                    }
                    doInvalidate = true;
                }
            }
        }
        else if (this.objectInfoState != 0) {
            this.objectInfoState -= 0.08;
            if (this.objectInfoState < 0) {
                this.objectInfoState = 0;
            }
            doInvalidate = true;
        }
        if (doInvalidate) {
            this.postInvalidate();
        }
    }

    private void showObjectInfo(int objectIndex) {
        this.objectInfoIndex = objectIndex;
    }

    private void changeSelectedObjectVelocity() {
        this.changingVelocity = true;
        this.hideObjectInfo();
    }

    private void hideObjectInfo() {
        this.lastObjectInfoShown = this.space.getObject(this.objectInfoIndex);
        this.objectInfoIndex = -1;
    }

    private void removeSelectedObject() {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this.attachedTo, R.style.DialogTheme);
        alertBuilder.setTitle("Confirm");
        alertBuilder.setMessage("Are you sure you want to remove this the object?");
        alertBuilder.setPositiveButton("YES", this);
        alertBuilder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        alertBuilder.show();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        int indexToRemove = this.objectInfoIndex;
        this.hideObjectInfo();
        this.space.removeObject(indexToRemove);
        dialog.cancel();
    }

    void clearAllObjects() {
        this.space = new GravitySpace();
    }

    public Vector2d getSpaceViewPosition() {
        return this.space.getViewPosition();
    }

    private static class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        private final GravityView attachedTo;

        public ScaleListener(GravityView attachedTo) {
            this.attachedTo = attachedTo;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            super.onScale(detector);
            Vector2d spaceLocation = this.attachedTo.space.getSpaceLocation(new Vector2d(detector.getFocusX(), detector.getFocusY()), this.attachedTo.getWidth(), this.attachedTo.getHeight());
            this.attachedTo.space.scale(detector.getScaleFactor(), spaceLocation);
            this.attachedTo.postInvalidate();
            return true;
        }
    }

    private static class GestureListener extends GestureDetector.SimpleOnGestureListener {

        private final GravityView attachedTo;

        public GestureListener(GravityView attachedTo) {
            this.attachedTo = attachedTo;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            if (this.attachedTo.objectInfoIndex == -1) {
                if (this.attachedTo.changingVelocity) {
                    if (this.attachedTo.objectInfoState == 0) {
                        this.handleChangingVelocityClicks(e);
                    }
                }
                else {
                    if (e.getY() < this.attachedTo.getHeight() / 16 && !this.attachedTo.attachedTo.paused) {
                        this.attachedTo.attachedTo.pause();
                    }
                    if (this.attachedTo.attachedTo.addingObject != null) {
                        if (e.getY() >= this.attachedTo.getHeight() * 15 / 16) {
                            if (e.getX() >= this.attachedTo.getWidth() / 2) {
                                if (this.attachedTo.positionConfirmed) {
                                    this.attachedTo.space.addObject(this.attachedTo.attachedTo.addingObject);
                                    this.attachedTo.attachedTo.addingObject = null;
                                    this.attachedTo.confirmHidingState = 1;
                                    this.attachedTo.positionConfirmed = false;
                                    this.attachedTo.attachedTo.paused = false;
                                } else {
                                    if (this.attachedTo.isAddingObjectValid) {
                                        this.attachedTo.positionConfirmed = true;
                                    }
                                }
                                this.attachedTo.postInvalidate();
                            }
                            else {
                                this.attachedTo.attachedTo.cancelAdditionFromGravityView();
                                this.attachedTo.positionConfirmed = false;
                            }
                        }
                        else {
                            if (e.getY() > this.attachedTo.getHeight() / 16) {
                                GravitationalObject addingObject = this.attachedTo.attachedTo.addingObject;
                                Vector2d spaceClickLocation = this.attachedTo.space.getSpaceLocation(new Vector2d(e.getX(), e.getY()), this.attachedTo.getWidth(), this.attachedTo.getHeight());
                                if (this.attachedTo.positionConfirmed) {
                                    addingObject.velocity = spaceClickLocation.subtract(addingObject.position).multiply(1 / (double) 32);
                                }
                                else {
                                    addingObject.position = spaceClickLocation;
                                    this.attachedTo.validateAddingObject();
                                }
                                this.attachedTo.postInvalidate();
                            }
                        }
                    }
                    else {
                        Vector2d spaceLocation = this.attachedTo.space.getSpaceLocation(new Vector2d(e.getX(), e.getY()), this.attachedTo.getWidth(), this.attachedTo.getHeight());
                        int clickedIndex = this.attachedTo.space.isContainedIn(spaceLocation);
                        if (clickedIndex != -1) {
                            this.attachedTo.showObjectInfo(clickedIndex);
                        }
                    }
                }
            }
            else {
                if (this.attachedTo.objectInfoState == 1) {
                    this.handleObjectInfoClicks(e);
                }
            }
            return true;
        }

        private void handleObjectInfoClicks(MotionEvent e) {
            if (e.getY() >= this.attachedTo.getHeight() * 5 / 6) {
                if (e.getY() >= this.attachedTo.getHeight() * 11 / 12) {
                    if (e.getX() >= this.attachedTo.getWidth() / 2) {
                        this.attachedTo.removeSelectedObject();
                    }
                    else {
                        this.attachedTo.hideObjectInfo();
                    }
                }
                else {
                    this.attachedTo.changeSelectedObjectVelocity();
                }
            }
        }

        private void handleChangingVelocityClicks(MotionEvent e) {
            if (e.getY() >= this.attachedTo.getHeight() * 15 / 16) {
                this.attachedTo.changingVelocity = false;
                this.attachedTo.postInvalidate();
            }
            else {
                if (e.getY() >= this.attachedTo.getHeight() / 16) {
                    GravitationalObject changingObject = this.attachedTo.lastObjectInfoShown;
                    Vector2d spaceClickLocation = this.attachedTo.space.getSpaceLocation(new Vector2d(e.getX(), e.getY()), this.attachedTo.getWidth(), this.attachedTo.getHeight());
                    changingObject.velocity = spaceClickLocation.subtract(changingObject.position).multiply(1 / (double) 32);
                    this.attachedTo.postInvalidate();
                }
            }
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            super.onDoubleTap(e);
            if (!this.attachedTo.attachedTo.paused) {
                this.attachedTo.simulationPaused = !this.attachedTo.simulationPaused;
                this.attachedTo.postInvalidate();
            }
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            super.onScroll(e1, e2, distanceX, distanceY);
            this.attachedTo.space.moveView(new Vector2d(distanceX, distanceY).multiply(1 / this.attachedTo.space.getEnlargement()));
            this.attachedTo.postInvalidate();
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            super.onLongPress(e);
            this.attachedTo.space.goToTheNearestObject();
            this.attachedTo.postInvalidate();
        }
    }
}