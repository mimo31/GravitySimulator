package com.github.mimo31.gravitysimulator;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AlertDialog;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements Runnable {

    private GravityView attachedGravityView;
    private PauseMenuView attachedPauseView;
    private View addObjectView;
    private View helpView;
    private boolean pausing;
    private boolean resuming;
    private float pausingState;
    public boolean paused;
    private final Handler updateHandler = new Handler();
    private final int updateDelay = 17;
    public GravitationalObject addingObject;
    private boolean animatingHelp = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.attachedGravityView = new GravityView(this);
        this.addContentView(this.attachedGravityView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        this.updateHandler.postDelayed(this, updateDelay);
    }

    public static float getMovableViewPosition(float state, float initialSpeed) {
        return (float) ((2 * initialSpeed - 2) * Math.pow(state, 3) + (3 - 3 * initialSpeed) * Math.pow(state, 2) + initialSpeed * state);
    }

    public void lockOrientation() {
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
    }

    public void unlockOrientation() {
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_USER);
    }

    @Override
    public void run() {
        this.update();
        updateHandler.postDelayed(this, updateDelay);
    }

    private void update() {
        if (this.pausing) {
            this.pausingState += 0.08;
            if (this.pausingState >= 1) {
                this.pausingState = 1;
                this.pausing = false;
            }
            this.updatePauseResumePosition();
        }
        else if (this.resuming) {
            this.pausingState -= 0.08;
            if (this.pausingState <= 0) {
                this.pausingState = 0;
                this.resuming = false;
                if (this.addingObject == null) {
                    this.paused = false;
                }
            }
            this.updatePauseResumePosition();
        }
        this.attachedGravityView.update();
    }

    private void updatePauseResumePosition() {
        float position = getMovableViewPosition(this.pausingState, 0.3f);
        this.attachedGravityView.setX((int) (position * this.attachedGravityView.getWidth()));
        if (this.addingObject != null) {
            this.addObjectView.setX((int) ((position - 1) * this.getWidth()));
        }
        else {
            this.attachedPauseView.setX((int) ((position - 1) * this.getWidth()));
        }
    }

    private void addNewObject() {
        if (this.addObjectView == null) {
            this.addObjectView = this.getLayoutInflater().inflate(R.layout.add_object_layout, null);
            this.addContentView(this.addObjectView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            ((TextView) this.findViewById(R.id.radiusText)).setHint("max " + this.getMaxRadius());
        }
        else {
            this.addObjectView.setX(0);
            this.addObjectView.setVisibility(View.VISIBLE);
            this.clearAddObjectView();
        }
        this.attachedPauseView.setVisibility(View.GONE);
    }

    private void clearAddObjectView() {
        ((TextView) this.addObjectView.findViewById(R.id.radiusText)).setText("");
        ((TextView) this.addObjectView.findViewById(R.id.densityText)).setText("");
    }

    private void showHelp() {
        if (this.helpView == null) {
            this.helpView = this.getLayoutInflater().inflate(R.layout.help_layout, null);
            this.addContentView(this.helpView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
        ScaleAnimation scaleAnimation = new ScaleAnimation(0, 1, 0, 1, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        scaleAnimation.setDuration(200);
        scaleAnimation.setAnimationListener(new ShowHelpListener(this));
        this.helpView.startAnimation(scaleAnimation);
    }

    private static class ShowHelpListener implements Animation.AnimationListener {

        final MainActivity attachedTo;

        public ShowHelpListener(MainActivity attachedTo) {
            this.attachedTo = attachedTo;
        }

        @Override
        public void onAnimationStart(Animation animation) {
            this.attachedTo.animatingHelp = true;
            this.attachedTo.helpView.setVisibility(View.VISIBLE);
        }

        @Override
        public void onAnimationRepeat(Animation animation) {

        }

        @Override
        public void onAnimationEnd(Animation animation) {
            this.attachedTo.animatingHelp = false;
            this.attachedTo.attachedPauseView.setVisibility(View.GONE);
        }
    }

    public void hideHelp(View v) {
        if (!this.animatingHelp) {
            ScaleAnimation scaleAnimation = new ScaleAnimation(1, 0, 1, 0, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            scaleAnimation.setDuration(200);
            scaleAnimation.setAnimationListener(new HideHelpListener(this));
            this.helpView.startAnimation(scaleAnimation);
        }
    }

    private static class HideHelpListener implements Animation.AnimationListener {

        final MainActivity attachedTo;

        public HideHelpListener(MainActivity attachedTo) {
            this.attachedTo = attachedTo;
        }

        @Override
        public void onAnimationStart(Animation animation) {
            this.attachedTo.animatingHelp = true;
            this.attachedTo.attachedPauseView.setVisibility(View.VISIBLE);
        }

        @Override
        public void onAnimationRepeat(Animation animation) {

        }

        @Override
        public void onAnimationEnd(Animation animation) {
            this.attachedTo.animatingHelp = false;
            this.attachedTo.helpView.setVisibility(View.GONE);
        }
    }

    public void confirmObjectAddition(View v) {
        if (v.getId() == R.id.addConfirmButton) {
            String enteredRadiusText = ((TextView) this.findViewById(R.id.radiusText)).getText().toString();
            if (enteredRadiusText.equals("")) {
                Toast.makeText(this.getApplicationContext(), "You must enter an object radius.", Toast.LENGTH_SHORT).show();
            }
            else {
                int maxRadius = this.getMaxRadius();
                int enteredRadius = Integer.parseInt(enteredRadiusText);
                if (enteredRadius == 0) {
                    Toast.makeText(this.getApplicationContext(), "The radius you enter may not be 0.", Toast.LENGTH_SHORT).show();
                }
                else if (enteredRadius > maxRadius) {
                    Toast.makeText(this.getApplicationContext(), "The radius you enter may not be bigger than " + String.valueOf(maxRadius) + ".", Toast.LENGTH_SHORT).show();
                }
                else {
                    String enteredDensityText = ((TextView) this.findViewById(R.id.densityText)).getText().toString();
                    if (enteredDensityText.equals("")) {
                        Toast.makeText(this.getApplicationContext(), "You must enter an object density.", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        int enteredDensity = Integer.parseInt(enteredDensityText);
                        if (enteredDensity > 1000) {
                            Toast.makeText(this.getApplicationContext(), "The density you enter may not be bigger than 1000.", Toast.LENGTH_SHORT).show();
                        }
                        else {
                            this.addingObject = new GravitationalObject(new Vector2d(this.getWidth() / (double)2, this.getHeight() / (double)2), enteredRadius, enteredDensity);
                            this.resume();
                            this.attachedGravityView.validateAddingObject();
                            this.attachedGravityView.postInvalidate();
                        }
                    }
                }
            }
        }
    }

    public void cancelObjectAddition(View v) {
        if (v.getId() == R.id.addCancelButton) {
            this.addObjectView.setVisibility(View.GONE);
            this.attachedPauseView.setVisibility(View.VISIBLE);
        }
    }

    private int getMaxRadius() {
        return Math.min(this.getWidth(), this.getHeight()) / 2;
    }

    void pause() {
        this.paused = true;
        this.pausing = true;
        if (this.attachedPauseView == null) {
            this.attachedPauseView = new PauseMenuView(this);
            this.addContentView(this.attachedPauseView, new ViewGroup.LayoutParams(this.getWidth(), this.getHeight()));
            this.attachedPauseView.setX(-this.getWidth());
        }
        else {
            if (this.addObjectView != null) {
                this.addObjectView.setVisibility(View.GONE);
            }
            this.attachedPauseView.setVisibility(View.VISIBLE);
        }
    }

    private void resume() {
        this.resuming = true;
    }

    private int getWidth() {
        return this.getWindow().findViewById(Window.ID_ANDROID_CONTENT).getWidth();
    }

    private int getHeight() {
        return this.getWindow().findViewById(Window.ID_ANDROID_CONTENT).getHeight();
    }

    void cancelAdditionFromGravityView() {
        this.pausing = true;
    }

    private static class PauseMenuView extends View {

        private final MainActivity attachedTo;
        private final GestureDetectorCompat gestureDetector;

        public PauseMenuView(MainActivity attachedTo) {
            super(attachedTo.getApplicationContext());
            this.attachedTo = attachedTo;
            this.gestureDetector = new GestureDetectorCompat(attachedTo.getApplicationContext(), new GestureListener(this));
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            super.onTouchEvent(event);
            if (!this.attachedTo.pausing && !this.attachedTo.resuming && !this.attachedTo.animatingHelp) {
                this.gestureDetector.onTouchEvent(event);
            }
            return true;
        }

        @Override
        public void draw(Canvas canvas) {
            super.draw(canvas);
            Paint p = new Paint();
            Rect addObjectRect = new Rect(0, 0, canvas.getWidth(), canvas.getHeight() * 8 / 15);
            Rect clearRect = new Rect(0, addObjectRect.bottom, canvas.getWidth(), canvas.getHeight() * 12 / 15);
            Rect helpRect = new Rect(0, clearRect.bottom, canvas.getWidth(), canvas.getHeight() * 14 / 15);
            Rect backRect = new Rect(0, helpRect.bottom, canvas.getWidth(), canvas.getHeight());
            p.setColor(Color.RED);
            canvas.drawRect(addObjectRect, p);
            p.setColor(Color.GREEN);
            canvas.drawRect(clearRect, p);
            p.setColor(Color.BLUE);
            canvas.drawRect(helpRect, p);
            p.setColor(Color.BLACK);
            canvas.drawRect(backRect, p);
            p.setColor(Color.WHITE);
            StringDraw.drawMaxString("ADD NEW OBJECT", addObjectRect, canvas.getHeight() / 15, canvas, p);
            StringDraw.drawMaxString("CLEAR ALL OBJECTS", clearRect, canvas.getHeight() / 30, canvas, p);
            StringDraw.drawMaxString("HELP", helpRect, canvas.getHeight() / 60, canvas, p);
            StringDraw.drawMaxString("BACK", backRect, canvas.getHeight() / 120, canvas, p);
        }

        private static class GestureListener extends GestureDetector.SimpleOnGestureListener implements DialogInterface.OnClickListener {

            private final PauseMenuView attachedTo;

            public GestureListener(PauseMenuView attachedTo) {
                this.attachedTo = attachedTo;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                float yPosition = e.getY();
                if (yPosition > this.attachedTo.getHeight() * 14 / 15) {
                    this.attachedTo.attachedTo.resume();
                }
                else if (yPosition > this.attachedTo.getHeight() * 12 / 15) {
                    this.attachedTo.attachedTo.showHelp();
                }
                else if (yPosition > this.attachedTo.getHeight() * 8 / 15) {
                    AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this.attachedTo.attachedTo, R.style.DialogTheme);
                    alertBuilder.setTitle("Confirm");
                    alertBuilder.setMessage("Are you sure you want to remove all the objects?");
                    alertBuilder.setPositiveButton("YES", this);
                    alertBuilder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                    alertBuilder.show();
                }
                else {
                    this.attachedTo.attachedTo.addNewObject();
                }
                return true;
            }

            @Override
            public void onClick(DialogInterface dialog, int which) {
                this.attachedTo.attachedTo.attachedGravityView.clearAllObjects();
                this.attachedTo.attachedTo.attachedGravityView.postInvalidate();
                dialog.cancel();
            }
        }
    }
}
