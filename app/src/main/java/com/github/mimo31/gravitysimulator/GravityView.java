package com.github.mimo31.gravitysimulator;

import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AlertDialog;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

/**
 * Created by mimo31 on 3/20/2016.
 * <p>
 * View for the main field where gravity is simulated.
 */
public class GravityView extends View implements DialogInterface.OnClickListener
{
    // pointer to the Activity
    private final MainActivity attachedTo;

    // touch detectors
    private final GestureDetectorCompat gestureDetector;
    private final ScaleGestureDetector scaleDetector;

    // whether the simulation is paused by the user and the two pause-resembling rects should be drawn
    private boolean paused;

    // when adding a new object, indicates whether the new object doesn't collide with another object already in GravitySpace
    private boolean isAddingObjectValid;

    // when adding a new object, indicates whether the position of the new object is already confirmed
    // basically indicates whether the position or the velocity is being set
    private boolean positionConfirmed;

    // animation state of the confirm button after a new object has been added
    // when the object is added, it is set to 1, and it is animated down to zero when the button completely disappears
    private float confirmHidingState;

    // index of the object we should be showing info of
    // when no info should be shown, value is -1
    private int objectInfoIndex = -1;

    // animation state of the object info panel
    // 0 - hidden
    // 1 - completely shown
    // if not already hidden or completely shown, the value is animated depending on objectInfoIndex == -1
    private float objectInfoState;

    // the gravitational object which has been previously selected
    // needed for changing velocity (where we need to know which object we are manipulating but the panel must be hidden)
    // needed to still display data about an object when the info panel is hiding
    private GravitationalObject lastObjectInfoShown;

    // indicates that the velocity (of lastObjectInfoShown) is being changed
    private boolean changingVelocity;

    // GravitySpace - where all objects are stored (except the one that is being added and the deleted ones)
    private GravitySpace space = new GravitySpace();

    // indicates whether the line grid in the background should be drawn
    // this is can be directly specified by the user in the settings
    public boolean showLineGrid = true;

    // indicates whether the view should move along with the center of mass of the objects
    public boolean followObjects = true;

    public GravityView(MainActivity attachedTo)
    {
        super(attachedTo.getApplicationContext());
        this.attachedTo = attachedTo;
        this.gestureDetector = new GestureDetectorCompat(attachedTo.getApplicationContext(), new GestureListener(this));
        this.scaleDetector = new ScaleGestureDetector(attachedTo.getApplicationContext(), new ScaleListener(this));
    }

    public GravityView(MainActivity attachedTo, Bundle bundle)
    {
        this(attachedTo);
        this.space = new GravitySpace(bundle.getBundle("space"));
        this.paused = bundle.getBoolean("paused");
    }

    @Override
    public void draw(Canvas canvas)
    {
        super.draw(canvas);
        Paint p = new Paint();

        int width = canvas.getWidth();
        int height = canvas.getHeight();

        // black background
        p.setColor(Color.BLACK);
        canvas.drawRect(new Rect(0, 0, width, height), p);

        // draw the GravitySpace
        this.space.draw(canvas, this.showLineGrid);

        if (this.changingVelocity)
        {
            // draw the red arrow
            this.space.drawObjectVelocity(canvas, this.lastObjectInfoShown);

            // draw the confirm button
            p.setColor(Color.WHITE);
            Rect confirmButton = new Rect(0, height * 15 / 16, width, height);
            canvas.drawRect(confirmButton, p);

            p.setColor(Color.BLACK);
            StringDraw.drawMaxString("Confirm", confirmButton, height / 64, canvas, p);
        }

        if (this.attachedTo.state == ViewState.ADDING_OBJECT || this.attachedTo.state == ViewState.ANIM_ADD_OBJECT_CANCEL ||
                this.attachedTo.state == ViewState.ANIM_ADD_OBJECT_TO_GRAVITY)
        {
            // draw the adding object with the specific color depending on whether the object is valid
            this.space.drawSpecific(canvas, this.attachedTo.addingObject, this.isAddingObjectValid ? Color.GREEN : Color.RED, this.positionConfirmed);
        }

        if (this.confirmHidingState != 0 || (this.attachedTo.state == ViewState.ADDING_OBJECT|| this.attachedTo.state == ViewState.ANIM_ADD_OBJECT_CANCEL ||
                this.attachedTo.state == ViewState.ANIM_ADD_OBJECT_TO_GRAVITY))
        {
            // draw the cancel and confirm buttons

            // alpha value of their background
            int alpha = this.attachedTo.addingObject != null ? 127 : (int) (this.confirmHidingState * 127);

            p.setColor(Color.argb(alpha, 255, 255, 255));
            canvas.drawRect(new Rect(0, height * 15 / 16, width, height), p);

            p.setColor(Color.BLACK);
            StringDraw.drawMaxString("Cancel", new Rect(0, height * 15 / 16, width / 2, height), height / 64, canvas, p);
            StringDraw.drawMaxString(this.positionConfirmed ? "Confirm velocity" : "Confirm position", new Rect(canvas.getWidth() / 2, canvas.getHeight() * 15 / 16, canvas.getWidth(), canvas.getHeight()), canvas.getHeight() / 64, canvas, p);
        }

        if (this.paused)
        {
            // draw the pause-resembling rectangles
            p.setColor(Color.argb(63, 255, 255, 255));
            int squareHalfSideLength = height / 16;
            canvas.drawRect(new Rect(width / 2 - squareHalfSideLength, height / 2 - squareHalfSideLength, width / 2 - squareHalfSideLength / 2, height / 2 + squareHalfSideLength), p);
            canvas.drawRect(new Rect(width / 2 + squareHalfSideLength / 2, height / 2 - squareHalfSideLength, width / 2 + squareHalfSideLength, height / 2 + squareHalfSideLength), p);
        }

        // draw the info panel
        if (this.objectInfoState != 0)
        {
            GravitationalObject objectToUse;
            if (this.objectInfoIndex == -1)
            {
                objectToUse = this.lastObjectInfoShown;
            }
            else
            {
                objectToUse = this.space.getObject(this.objectInfoIndex);
                this.space.drawObjectVelocity(canvas, this.objectInfoIndex);
            }
            objectToUse.drawInfo(canvas, MainActivity.getMovableViewPosition(this.objectInfoState, 0));
        }

        // draw the top white rectangle
        p.setColor(Color.WHITE);
        Rect menuButtonRect = new Rect(0, 0, canvas.getWidth(), canvas.getHeight() / 16);
        canvas.drawRect(menuButtonRect, p);

        // draw text to the rectangle
        String text = null;
        p.setColor(Color.BLACK);
        if ((this.attachedTo.state == ViewState.SIMULATION || this.attachedTo.state == ViewState.ANIM_PAUSING ||
                this.attachedTo.state == ViewState.ANIM_RESUMING) && !this.changingVelocity)
        {
            text = "Menu";
        }
        else if (this.changingVelocity || ((this.attachedTo.state == ViewState.ADDING_OBJECT || this.attachedTo.state == ViewState.ANIM_ADD_OBJECT_CANCEL ||
                this.attachedTo.state == ViewState.ANIM_ADD_OBJECT_TO_GRAVITY) && this.positionConfirmed))
        {
            text = "Tap anywhere to change the velocity";
        }
        else if ((this.attachedTo.state == ViewState.ADDING_OBJECT || this.attachedTo.state == ViewState.ANIM_ADD_OBJECT_CANCEL ||
                this.attachedTo.state == ViewState.ANIM_ADD_OBJECT_TO_GRAVITY) && !this.positionConfirmed)
        {
            text = "Tap anywhere to change the position";
        }
        if (text != null)
        {
            StringDraw.drawMaxString(text, menuButtonRect, canvas.getHeight() / 128, canvas, p);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        super.onTouchEvent(event);

        // listen to touch events only if the current state is simulation or object adding
        ViewState activityState = this.attachedTo.state;
        if (activityState == ViewState.SIMULATION || activityState == ViewState.ADDING_OBJECT)
        {
            this.gestureDetector.onTouchEvent(event);
            this.scaleDetector.onTouchEvent(event);
        }
        return true;
    }

    /**
     * Updates the variable isAddingObjectValid to whether the adding object collides with some another object in GravitySpace.
     */
    void validateAddingObject()
    {
        this.isAddingObjectValid = !this.space.doesCollide(this.attachedTo.addingObject);
    }

    void startAddingObject()
    {
        this.objectInfoIndex = -1;
        this.objectInfoState = 0;
        this.changingVelocity = false;
        this.positionConfirmed = false;
        this.validateAddingObject();
    }

    /**
     * Updates the GravityView.
     * Only to be called when the Activity is in the SIMULATION ViewState.
     * If the above rule is followed, should be called approx. every 17 ms.
     */
    void update()
    {
        boolean doInvalidate = false;

        // if the confirm button after adding an object isn't yet completely hidden
        if (this.confirmHidingState != 0)
        {
            this.confirmHidingState -= 0.08;
            if (this.confirmHidingState < 0)
            {
                this.confirmHidingState = 0;
            }
            doInvalidate = true;
        }

        // update the objects, if not paused or not changing velocity
        if (!this.paused && !this.changingVelocity)
        {
            for (int i = 0; i < 64; i++)
            {
                this.space.update(1 / (double) 64, this.followObjects);
            }
            this.space.updateViewVelocity();
            doInvalidate = true;
        }

        // object is selected - info panel should be completely shown && info panel isn't completely shown
        // - animate the showing of the info panel
        if (this.objectInfoIndex != -1 && this.objectInfoState != 1)
        {
            this.objectInfoState += 0.08;
            if (this.objectInfoState > 1)
            {
                this.objectInfoState = 1;
            }
            doInvalidate = true;
        }
        // no object is selected - info panel should be completely hidden && info panel isn't completely hidden
        // - animate the hiding of the info panel
        else if (this.objectInfoIndex == -1 && this.objectInfoState != 0)
        {
            this.objectInfoState -= 0.08;
            if (this.objectInfoState < 0)
            {
                this.objectInfoState = 0;
            }
            doInvalidate = true;
        }

        if (doInvalidate)
        {
            this.postInvalidate();
        }
    }

    /**
     * Selects an object. That also starts the animation of showing the object info panel.
     * @param objectIndex index of the object to select
     */
    private void showObjectInfo(int objectIndex)
    {
        this.objectInfoIndex = objectIndex;
    }

    /**
     * Starts hiding the object info panel and sets changingVelocity to true.
     * Should be called only if an object info panel is shown.
     */
    private void changeSelectedObjectVelocity()
    {
        this.changingVelocity = true;
        this.hideObjectInfo();
    }

    /**
     * Unselects the object which's info is shown. That also starts the animation of hiding the object info panel.
     * The information which object was selected is still kept in the 'lastObjectInfoShown' variable.
     * Should be called whenever the object info panel needs to be hidden.
     */
    private void hideObjectInfo()
    {
        this.lastObjectInfoShown = this.space.getObject(this.objectInfoIndex);
        this.objectInfoIndex = -1;
    }

    /**
     * Shows a prompt asking whether the user really wants to remove the object.
     * If the user then selects yes, the object which's object info is currently shown gets removed and the object info gets animated out.
     * If the user selects no, nothing happens.
     * Should be called only if some object info is currently shown.
     */
    private void removeSelectedObject()
    {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this.attachedTo, R.style.DialogTheme);
        alertBuilder.setTitle("Confirm");
        alertBuilder.setMessage("Are you sure you want to remove this object?");
        alertBuilder.setPositiveButton("YES", this);
        alertBuilder.setNegativeButton("NO", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.cancel();
            }
        });
        alertBuilder.show();
    }

    // handles the removal of the selected object
    @Override
    public void onClick(DialogInterface dialog, int which)
    {
        int indexToRemove = this.objectInfoIndex;
        this.hideObjectInfo();
        this.space.removeObject(indexToRemove);
        this.space.updateViewVelocity();
        dialog.cancel();
    }

    /**
     * Removes all objects form the GravitySpace.
     */
    void clearAllObjects()
    {
        this.space = new GravitySpace();
    }

    /**
     * @return Vector of where in the GravitySpace is the current viewpoint located.
     */
    public Vector2d getSpaceViewPosition()
    {
        return this.space.getViewPosition();
    }

    /**
     * @return Bundle with all relevant state data about the View including the GravitySpace.
     */
    public Bundle putToBundle()
    {
        Bundle bundle = new Bundle();
        bundle.putBundle("space", this.space.putToBundle());
        bundle.putBoolean("paused", this.paused);
        return bundle;
    }

    private static class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener
    {

        private final GravityView attachedTo;

        public ScaleListener(GravityView attachedTo)
        {
            this.attachedTo = attachedTo;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector)
        {
            super.onScale(detector);
            Vector2d spaceLocation = this.attachedTo.space.getSpaceLocation(new Vector2d(detector.getFocusX(), detector.getFocusY()), this.attachedTo.getWidth(), this.attachedTo.getHeight());
            this.attachedTo.space.scale(detector.getScaleFactor(), spaceLocation);
            this.attachedTo.postInvalidate();
            return true;
        }
    }

    private static class GestureListener extends GestureDetector.SimpleOnGestureListener
    {

        private final GravityView attachedTo;

        public GestureListener(GravityView attachedTo)
        {
            this.attachedTo = attachedTo;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e)
        {
            float tapX = e.getX();
            float tapY = e.getY();
            int viewWidth = this.attachedTo.getWidth();
            int viewHeight = this.attachedTo.getHeight();

            MainActivity activity = this.attachedTo.attachedTo;

            // we're listening to the touch event only if the state is SIMULATING or ADDING_OBJECT, so one of those must the case now
            if (activity.state == ViewState.SIMULATION)
            {
                if (this.attachedTo.changingVelocity)
                {
                    this.handleChangingVelocityClicks(tapX, tapY);
                    return true;
                }

                // check if the Menu button was pressed
                if (tapY < viewHeight / 16)
                {
                    activity.pause();
                    return true;
                }

                // check if the object info panel was tapped
                if (this.attachedTo.objectInfoIndex != -1 && this.attachedTo.objectInfoState == 1 && tapY >= viewHeight * 5 / 6)
                {
                    this.handleObjectInfoClicks(tapX, tapY);
                    return true;
                }

                // check if an object was tapped
                GravitySpace space = this.attachedTo.space;
                Vector2d spaceLocation = space.getSpaceLocation(new Vector2d(tapX, tapY), viewWidth, viewHeight);
                int clickedIndex = space.isContainedIn(spaceLocation);
                if (clickedIndex != -1)
                {
                    this.attachedTo.showObjectInfo(clickedIndex);
                    this.attachedTo.postInvalidate();
                }
            }
            else
            {
                // check if the bottom buttons were pressed
                if (tapY >= viewHeight * 15 / 16)
                {
                    // the confirm button was pressed
                    if (tapX >= viewWidth / 2)
                    {
                        // confirming the velocity - the object is added to GravitySpace
                        if (this.attachedTo.positionConfirmed)
                        {
                            this.attachedTo.space.addObject(activity.addingObject);
                            this.attachedTo.space.updateViewVelocity();
                            activity.addingObject = null;
                            activity.state = ViewState.SIMULATION;
                            this.attachedTo.confirmHidingState = 1;
                            this.attachedTo.positionConfirmed = false;
                            this.attachedTo.postInvalidate();
                        }
                        // confirming the position
                        else
                        {
                            // check the position is valid
                            if (this.attachedTo.isAddingObjectValid)
                            {
                                this.attachedTo.positionConfirmed = true;
                                this.attachedTo.postInvalidate();
                            }
                        }

                    }
                    // the cancel button was pressed
                    else
                    {
                        activity.cancelObjectAddFromGravity();
                    }

                    return true;
                }

                // the top advice was tapped - ignore
                if (tapY < viewHeight / 16)
                {
                    return true;
                }

                // the GravitySpace was tapped, set the velocity or position

                // location of the tap in the space coordinates
                Vector2d spaceClickLocation = this.attachedTo.space.getSpaceLocation(new Vector2d(tapX, tapY), viewWidth, viewHeight);

                // now velocity will be set
                if (this.attachedTo.positionConfirmed)
                {
                    // vector from the adding object to the tap location
                    Vector2d distanceVector = spaceClickLocation.subtract(activity.addingObject.position);

                    // shrink the vector by 1 / 32 and set it as the veloctity
                    activity.addingObject.velocity = distanceVector.multiply(1 / (double) 32);
                }
                // now position will be set
                else
                {
                    // set the position
                    activity.addingObject.position = spaceClickLocation;

                    // validate that the new object does not collide with other objects
                    this.attachedTo.validateAddingObject();
                }
                this.attachedTo.postInvalidate();
            }
            return true;
        }

        /**
         * Handles taps on the object info panel. Should be called only if the panel was tapped and if it was already expanded at that time.
         * @param tapX x screen coordinate of the tap
         * @param tapY y screen coordinate of the tap
         */
        private void handleObjectInfoClicks(float tapX, float tapY)
        {
            int viewWidth = this.attachedTo.getWidth();
            int viewHeight = this.attachedTo.getHeight();

            // check whether the buttons were pressed
            if (tapY >= viewHeight * 5 / 6)
            {
                // back or remove object were tapped
                if (tapY >= viewHeight * 11 / 12)
                {
                    if (tapX >= viewWidth / 2)
                    {
                        this.attachedTo.removeSelectedObject();
                    }
                    else
                    {
                        this.attachedTo.hideObjectInfo();
                    }
                }
                // change velocity was tapped
                else
                {
                    this.attachedTo.changeSelectedObjectVelocity();
                }
            }
        }

        /**
         * Handles taps while changing velocity. Should be called for all taps anywhere while changing velocity.
         * @param tapX x screen coordinate of the tap
         * @param tapY y screen coordinate of the tap
         */
        private void handleChangingVelocityClicks(float tapX, float tapY)
        {
            int viewWidth = this.attachedTo.getWidth();
            int viewHeight = this.attachedTo.getHeight();

            // confirm button tapped
            if (tapY >= viewHeight * 15 / 16)
            {
                this.attachedTo.space.updateViewVelocity();
                this.attachedTo.changingVelocity = false;
                this.attachedTo.postInvalidate();
            }
            else
            {
                // GravitySpace tapped
                if (tapY >= viewHeight / 16)
                {
                    // the object which's velocity we are changing
                    GravitationalObject changingObject = this.attachedTo.lastObjectInfoShown;

                    // the location of the tap in GravitySpace coordinates
                    Vector2d spaceClickLocation = this.attachedTo.space.getSpaceLocation(new Vector2d(tapX, tapY), viewWidth, viewHeight);

                    changingObject.velocity = spaceClickLocation.subtract(changingObject.position).multiply(1 / (double) 32);

                    this.attachedTo.postInvalidate();
                }
            }
        }

        @Override
        public boolean onDoubleTap(MotionEvent e)
        {
            super.onDoubleTap(e);
            MainActivity activity = this.attachedTo.attachedTo;
            if (activity.state == ViewState.SIMULATION)
            {
                // toggle pause
                this.attachedTo.paused = !this.attachedTo.paused;
                this.attachedTo.postInvalidate();
            }
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
        {
            super.onScroll(e1, e2, distanceX, distanceY);
            this.attachedTo.space.moveView(new Vector2d(distanceX, distanceY).multiply(1 / this.attachedTo.space.getEnlargement()));
            this.attachedTo.postInvalidate();
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e)
        {
            super.onLongPress(e);
            this.attachedTo.space.goToTheNearestObject();
            this.attachedTo.postInvalidate();
        }
    }
}