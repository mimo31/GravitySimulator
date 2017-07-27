package com.github.mimo31.gravitysimulator;

import android.app.Activity;
import android.content.DialogInterface;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements Runnable
{
    // attached views
    private GravityView gravityView;
    private PauseMenuView pauseView;
    private View addObjectView;
    private View helpView;
    private View settingsView;

    private final Handler updateHandler = new Handler();
    private boolean updating = true;
    private final int updateDelay = 17;
    public GravitationalObject addingObject;

    // state when an animation between view is performed
    // starts at zero and is then animated to one
    // when reaches one, the ViewState is switched from the animation to the actual View
    // when no animation is being performed, should be equal set to 0
    private float animationState = 0;

    // specifies what is the app currently doing
    public ViewState state = ViewState.SIMULATION;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null)
        {
            this.gravityView = new GravityView(this, savedInstanceState.getBundle("GravityView"));
        }
        else
        {
            this.gravityView = new GravityView(this);
        }
        this.addContentView(this.gravityView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        this.updateHandler.postDelayed(this, updateDelay);
    }

    public static float getMovableViewPosition(float state, float initialSpeed)
    {
        return (float) ((2 * initialSpeed - 2) * Math.pow(state, 3) + (3 - 3 * initialSpeed) * Math.pow(state, 2) + initialSpeed * state);
    }

    @Override
    public void run()
    {
        if (this.updating)
        {
            this.update();
            updateHandler.postDelayed(this, updateDelay);
        }
    }

    /**
     * Updates everything in the Activity.
     * Should be called every updateDelay ms (when the Activity is shown to the user, else it doesn't need to be updated).
     */
    private void update()
    {
        // if the state is an animation
        if (this.state == ViewState.ANIM_ADD_OBJECT_CANCEL || this.state == ViewState.ANIM_ADD_OBJECT_TO_GRAVITY ||
                this.state == ViewState.ANIM_ADD_OBJECT_TO_PAUSE || this.state == ViewState.ANIM_PAUSE_TO_ADD_OBJECT ||
                this.state == ViewState.ANIM_PAUSING || this.state == ViewState.ANIM_RESUMING)
        {
            // animate animationState
            this.animationState += 0.08;
            if (this.animationState > 1)
            {
                this.animationState = 1;
            }

            // get the fraction of the distance the views in the animation have already travelled
            float positionPart = getMovableViewPosition(this.animationState, 0.3f);

            // dimensions of the Activity
            int width = this.getWidth();
            int height = this.getHeight();

            // depending on which animation is being performed, update the positions of the views
            switch (this.state)
            {
                case ANIM_ADD_OBJECT_CANCEL:
                    this.gravityView.setX(positionPart * width);
                    this.addObjectView.setX((positionPart - 1) * width);
                    break;
                case ANIM_ADD_OBJECT_TO_GRAVITY:
                    this.addObjectView.setX(-positionPart * width);
                    this.gravityView.setX((1 - positionPart) * width);
                    break;
                case ANIM_ADD_OBJECT_TO_PAUSE:
                    this.addObjectView.setY(-positionPart * height);
                    this.pauseView.setY((1 - positionPart) * height);
                    break;
                case ANIM_PAUSE_TO_ADD_OBJECT:
                    this.pauseView.setY(positionPart * height);
                    this.addObjectView.setY((positionPart - 1) * height);
                    break;
                case ANIM_PAUSING:
                    this.gravityView.setX(positionPart * width);
                    this.pauseView.setX((positionPart - 1) * width);
                    break;
                case ANIM_RESUMING:
                    this.pauseView.setX(-positionPart * width);
                    this.gravityView.setX((1 - positionPart) * width);
            }

            // if the state == 1, the animation has ended, change the view state and hide the old view
            if (this.animationState == 1)
            {
                switch (this.state)
                {
                    case ANIM_ADD_OBJECT_CANCEL:
                        this.state = ViewState.ADD_OBJECT_VIEW;
                        this.gravityView.setVisibility(View.GONE);
                        break;
                    case ANIM_ADD_OBJECT_TO_GRAVITY:
                        this.state = ViewState.ADDING_OBJECT;
                        this.addObjectView.setVisibility(View.GONE);
                        break;
                    case ANIM_ADD_OBJECT_TO_PAUSE:
                        this.state = ViewState.PAUSE_VIEW;
                        this.addObjectView.setVisibility(View.GONE);
                        break;
                    case ANIM_PAUSE_TO_ADD_OBJECT:
                        this.state = ViewState.ADD_OBJECT_VIEW;
                        this.pauseView.setVisibility(View.GONE);

                        // focus on the radius EditText and pop up the keyboard
                        this.addObjectView.findViewById(R.id.radiusText).requestFocus();
                        this.showSoftKeyboard();
                        break;
                    case ANIM_PAUSING:
                        this.state = ViewState.PAUSE_VIEW;
                        this.gravityView.setVisibility(View.GONE);
                        break;
                    case ANIM_RESUMING:
                        this.state = ViewState.SIMULATION;
                        this.pauseView.setVisibility(View.GONE);
                        break;
                }
                this.animationState = 0;
            }
        }

        // update the GravityView if needed
        if (this.state == ViewState.SIMULATION)
        {
            this.gravityView.update();
        }
    }

    /**
     * Starts the animation of showing the AddObjectView and hiding the PauseView.
     */
    private void addNewObject()
    {
        this.state = ViewState.ANIM_PAUSE_TO_ADD_OBJECT;

        // create the AddObjectView if it hasn't been yet created
        if (this.addObjectView == null)
        {
            this.addObjectView = this.getLayoutInflater().inflate(R.layout.add_object_layout, null);
            this.addContentView(this.addObjectView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }

        int height = this.getHeight();

        // set the correct initial position of the AddObjectView
        this.addObjectView.setX(0);
        this.addObjectView.setY(-height);

        // show the AddObjectView
        this.addObjectView.setVisibility(View.VISIBLE);

        // clear its EditTexts
        this.clearAddObjectView();
    }

    /**
     * Pops up the soft keyboard.
     */
    private void showSoftKeyboard()
    {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(this.getApplicationContext().INPUT_METHOD_SERVICE);
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    /**
     * Clears the text in the AddObjectView.
     */
    private void clearAddObjectView()
    {
        ((TextView) this.addObjectView.findViewById(R.id.radiusText)).setText("");
        ((TextView) this.addObjectView.findViewById(R.id.densityText)).setText("");
    }

    /**
     * Starts the animation of showing help.
     */
    private void showHelp()
    {
        // create the HelpView if it hasn't been yet created
        if (this.helpView == null)
        {
            this.helpView = this.getLayoutInflater().inflate(R.layout.help_layout, null);
            this.addContentView(this.helpView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }

        // construct the animation of popping up from the middle of the screen
        ScaleAnimation scaleAnimation = new ScaleAnimation(0, 1, 0, 1, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        scaleAnimation.setDuration(200);
        scaleAnimation.setAnimationListener(new ShowHelpListener(this));

        // start the animation
        this.helpView.startAnimation(scaleAnimation);
    }

    private static class ShowHelpListener implements Animation.AnimationListener
    {

        final MainActivity attachedTo;

        public ShowHelpListener(MainActivity attachedTo)
        {
            this.attachedTo = attachedTo;
        }

        @Override
        public void onAnimationStart(Animation animation)
        {
            this.attachedTo.state = ViewState.GENERIC_ANIM;
            this.attachedTo.helpView.setVisibility(View.VISIBLE);
        }

        @Override
        public void onAnimationRepeat(Animation animation)
        {

        }

        @Override
        public void onAnimationEnd(Animation animation)
        {
            this.attachedTo.state = ViewState.HELP_VIEW;
            this.attachedTo.pauseView.setVisibility(View.GONE);
        }
    }

    /**
     * Starts the animation of disappearing in the middle of the screen.
     * @param v the View that has caused the called (the button)
     */
    public void hideHelp(View v)
    {
        // construct the animation
        ScaleAnimation scaleAnimation = new ScaleAnimation(1, 0, 1, 0, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        scaleAnimation.setDuration(200);
        scaleAnimation.setAnimationListener(new HideHelpListener(this));

        // start the animation
        this.helpView.startAnimation(scaleAnimation);
    }

    private static class HideHelpListener implements Animation.AnimationListener
    {

        final MainActivity attachedTo;

        public HideHelpListener(MainActivity attachedTo)
        {
            this.attachedTo = attachedTo;
        }

        @Override
        public void onAnimationStart(Animation animation)
        {
            this.attachedTo.state = ViewState.GENERIC_ANIM;
            this.attachedTo.pauseView.setVisibility(View.VISIBLE);
        }

        @Override
        public void onAnimationRepeat(Animation animation)
        {

        }

        @Override
        public void onAnimationEnd(Animation animation)
        {
            this.attachedTo.state = ViewState.PAUSE_VIEW;
            this.attachedTo.helpView.setVisibility(View.GONE);
        }
    }

    /**
     * Check that the parameters entered for the new object are valid.
     * If they are, starts the animation to the GravityView where the position and velocity are set.
     * @param v the View that has caused the call (the button)
     */
    public void confirmObjectAddition(View v)
    {
        // make sure the call is from the proper button
        if (v.getId() != R.id.addConfirmButton)
        {
            return;
        }

        // make sure the user entered the radius
        String enteredRadiusText = ((TextView) this.findViewById(R.id.radiusText)).getText().toString();
        if (enteredRadiusText.equals(""))
        {
            Toast.makeText(this.getApplicationContext(), "You must enter an object radius.", Toast.LENGTH_SHORT).show();
            return;
        }

        // make sure the radius is in the valid range
        int enteredRadius = Integer.parseInt(enteredRadiusText);
        if (enteredRadius == 0)
        {
            Toast.makeText(this.getApplicationContext(), "The radius you enter may not be 0.", Toast.LENGTH_SHORT).show();
            return;
        }
        else if (enteredRadius > 1000000)
        {
            Toast.makeText(this.getApplicationContext(), "The radius you enter may not be bigger than 1000000.", Toast.LENGTH_SHORT).show();
            return;
        }

        // make sure the user entered the density
        String enteredDensityText = ((TextView) this.findViewById(R.id.densityText)).getText().toString();
        if (enteredDensityText.equals(""))
        {
            Toast.makeText(this.getApplicationContext(), "You must enter an object density.", Toast.LENGTH_SHORT).show();
            return;
        }

        // make sure the density is in the valid range
        int enteredDensity = Integer.parseInt(enteredDensityText);
        if (enteredDensity > 1000)
        {
            Toast.makeText(this.getApplicationContext(), "The density you enter may not be bigger than 1000.", Toast.LENGTH_SHORT).show();
            return;
        }

        // create the object to be added
        this.addingObject = new GravitationalObject(this.gravityView.getSpaceViewPosition(), enteredRadius, enteredDensity);

        this.state = ViewState.ANIM_ADD_OBJECT_TO_GRAVITY;

        int width = this.getWidth();

        // set the correct position for the GravityView
        this.gravityView.setX(width);
        this.gravityView.setY(0);

        // show the GravityView
        this.gravityView.setVisibility(View.VISIBLE);

        // prepare the GravityView
        this.gravityView.startAddingObject();
        this.gravityView.postInvalidate();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putBundle("GravityView", this.gravityView.putToBundle());
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        this.updating = false;
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        this.updating = true;
        this.updateHandler.postDelayed(this, updateDelay);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);
        boolean isNull = this.gravityView == null;
        if (isNull)
        {
            if (savedInstanceState != null)
            {
                this.gravityView = new GravityView(this, savedInstanceState.getBundle("GravityView"));
            }
            else
            {
                this.gravityView = new GravityView(this);
            }
            this.addContentView(this.gravityView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            this.updateHandler.postDelayed(this, updateDelay);
        }
    }

    public void cancelObjectAddition(View v)
    {
        if (v.getId() == R.id.addCancelButton)
        {
            this.state = ViewState.ANIM_ADD_OBJECT_TO_PAUSE;

            int height = this.getHeight();

            // set correct initial position for the PauseView
            this.pauseView.setX(0);
            this.pauseView.setY(height);

            // show the PauseView
            this.pauseView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Starts the animation of hiding the GravityView and showing the PauseView.
     */
    void pause()
    {
        this.state = ViewState.ANIM_PAUSING;

        int width = this.getWidth();
        int height = this.getHeight();

        // create the PauseView if it hasn't been yet created.
        if (this.pauseView == null)
        {
            this.pauseView = new PauseMenuView(this);
            this.addContentView(this.pauseView, new ViewGroup.LayoutParams(width, height));
        }

        // set the correct initial position for PauseView
        this.pauseView.setX(-width);
        this.pauseView.setY(0);

        // show the PauseView
        this.pauseView.setVisibility(View.VISIBLE);
    }

    /**
     * Starts the animation of hiding the PauseView and showing the GravityView.
     */
    private void resume()
    {
        this.state = ViewState.ANIM_RESUMING;

        // set the correct initial position for GravityView
        int width = this.getWidth();
        this.gravityView.setX(width);
        this.gravityView.setY(0);

        // show the GravityView
        this.gravityView.setVisibility(View.VISIBLE);
    }

    /**
     * @return the width of the Activity in pixels
     */
    private int getWidth()
    {
        return this.getWindow().findViewById(Window.ID_ANDROID_CONTENT).getWidth();
    }

    /**
     * @return the height of the Activity in pixels
     */
    private int getHeight()
    {
        return this.getWindow().findViewById(Window.ID_ANDROID_CONTENT).getHeight();
    }

    /**
     * Starts the animation of showing back the AddObjectView and hiding the GravityView.
     */
    void cancelObjectAddFromGravity()
    {
        this.state = ViewState.ANIM_ADD_OBJECT_CANCEL;

        // set the correct initial position for AddObjectView
        int width = this.getWidth();
        this.addObjectView.setX(-width);
        this.addObjectView.setY(0);

        // show the AddObjectView
        this.addObjectView.setVisibility(View.VISIBLE);
    }

    private static class PauseMenuView extends View
    {

        private final MainActivity attachedTo;
        private final GestureDetectorCompat gestureDetector;

        public PauseMenuView(MainActivity attachedTo)
        {
            super(attachedTo.getApplicationContext());
            this.attachedTo = attachedTo;
            this.gestureDetector = new GestureDetectorCompat(attachedTo.getApplicationContext(), new GestureListener(this));
        }

        @Override
        public boolean onTouchEvent(MotionEvent event)
        {
            super.onTouchEvent(event);
            if (this.attachedTo.state == ViewState.PAUSE_VIEW)
            {
                this.gestureDetector.onTouchEvent(event);
            }
            return true;
        }

        @Override
        public void draw(Canvas canvas)
        {
            super.draw(canvas);

            Paint p = new Paint();

            int width = canvas.getWidth();
            int height = canvas.getHeight();

            // create rectangles for the buttons
            Rect addObjectRect = new Rect(0, 0, width, height * 8 / 15);
            Rect clearRect = new Rect(0, addObjectRect.bottom, width, height * 12 / 15);
            Rect helpRect = new Rect(0, clearRect.bottom, width / 2, height * 14 / 15);
            Rect settingsRect = new Rect(helpRect.right, clearRect.bottom, width, helpRect.bottom);
            Rect backRect = new Rect(0, helpRect.bottom, width, height);

            // draw the backgrounds for the buttons
            p.setColor(Color.RED);
            canvas.drawRect(addObjectRect, p);
            p.setColor(Color.GREEN);
            canvas.drawRect(clearRect, p);
            p.setColor(Color.BLUE);
            canvas.drawRect(helpRect, p);
            p.setColor(Color.MAGENTA);
            canvas.drawRect(settingsRect, p);
            p.setColor(Color.BLACK);
            canvas.drawRect(backRect, p);

            // draw the text in the buttons
            p.setColor(Color.WHITE);
            StringDraw.drawMaxString("ADD NEW OBJECT", addObjectRect, canvas.getHeight() / 15, canvas, p);
            StringDraw.drawMaxString("CLEAR ALL OBJECTS", clearRect, canvas.getHeight() / 30, canvas, p);
            StringDraw.drawMaxString("HELP", helpRect, canvas.getHeight() / 60, canvas, p);
            StringDraw.drawMaxString("SETTINGS", settingsRect, canvas.getHeight() / 60, canvas, p);
            StringDraw.drawMaxString("BACK", backRect, canvas.getHeight() / 120, canvas, p);
        }

        private static class GestureListener extends GestureDetector.SimpleOnGestureListener implements DialogInterface.OnClickListener
        {

            private final PauseMenuView attachedTo;

            public GestureListener(PauseMenuView attachedTo)
            {
                this.attachedTo = attachedTo;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e)
            {
                int width = this.attachedTo.getWidth();
                int height = this.attachedTo.getHeight();

                float tapX = e.getX();
                float tapY = e.getY();

                if (tapY > height * 14 / 15)
                {
                    this.attachedTo.attachedTo.resume();
                }
                else if (tapY > height * 12 / 15)
                {
                    if (tapX < width / 2)
                    {
                        this.attachedTo.attachedTo.showHelp();
                    }
                    else
                    {
                        this.attachedTo.attachedTo.showSettings();
                    }
                }
                else if (tapY > height * 8 / 15)
                {
                    AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this.attachedTo.attachedTo, R.style.DialogTheme);
                    alertBuilder.setTitle("Confirm");
                    alertBuilder.setMessage("Are you sure you want to remove all the objects?");
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
                else
                {
                    this.attachedTo.attachedTo.addNewObject();
                }
                return true;
            }

            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                this.attachedTo.attachedTo.gravityView.clearAllObjects();
                this.attachedTo.attachedTo.gravityView.postInvalidate();
                dialog.cancel();
            }
        }
    }

    private static class SettingsChangeListener implements CompoundButton.OnCheckedChangeListener
    {
        private final MainActivity attachedTo;

        private SettingsChangeListener(MainActivity attachedTo)
        {
            this.attachedTo = attachedTo;
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
        {
            // set update the variables according to the state of the CheckBoxes in the settings

            // find the CheckBoxes
            CheckBox gridCheckBox = (CheckBox) this.attachedTo.findViewById(R.id.settingsGridCheckBox);
            CheckBox followCheckBox = (CheckBox) this.attachedTo.findViewById(R.id.settingsFollowCheckBox);

            // set the variables in the GravityView
            GravityView gravityView = this.attachedTo.gravityView;
            gravityView.showLineGrid = gridCheckBox.isChecked();
            gravityView.followObjects = followCheckBox.isChecked();
        }
    }

    /**
     * Starts the animation of showing settings.
     */
    private void showSettings()
    {
        // create the HelpView if it hasn't been yet created
        if (this.settingsView == null)
        {
            this.settingsView = this.getLayoutInflater().inflate(R.layout.settings_layout, null);
            this.addContentView(this.settingsView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            // set listeners to checked changes on the CheckBoxes
            SettingsChangeListener listener = new SettingsChangeListener(this);
            ((CheckBox) this.findViewById(R.id.settingsGridCheckBox)).setOnCheckedChangeListener(listener);
            ((CheckBox) this.findViewById(R.id.settingsFollowCheckBox)).setOnCheckedChangeListener(listener);
        }

        // construct the animation of popping up from the middle of the screen
        ScaleAnimation scaleAnimation = new ScaleAnimation(0, 1, 0, 1, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        scaleAnimation.setDuration(200);
        scaleAnimation.setAnimationListener(new ShowSettingsListener(this));

        // start the animation
        this.settingsView.startAnimation(scaleAnimation);
    }

    private static class ShowSettingsListener implements Animation.AnimationListener
    {

        final MainActivity attachedTo;

        public ShowSettingsListener(MainActivity attachedTo)
        {
            this.attachedTo = attachedTo;
        }

        @Override
        public void onAnimationStart(Animation animation)
        {
            this.attachedTo.state = ViewState.GENERIC_ANIM;
            this.attachedTo.settingsView.setVisibility(View.VISIBLE);
        }

        @Override
        public void onAnimationRepeat(Animation animation)
        {

        }

        @Override
        public void onAnimationEnd(Animation animation)
        {
            this.attachedTo.state = ViewState.SETTINGS_VIEW;
            this.attachedTo.pauseView.setVisibility(View.GONE);
        }
    }

    /**
     * Starts the animation of disappearing in the middle of the screen.
     * @param v the View that has caused the called (the button)
     */
    public void hideSettings(View v)
    {
        // construct the animation
        ScaleAnimation scaleAnimation = new ScaleAnimation(1, 0, 1, 0, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        scaleAnimation.setDuration(200);
        scaleAnimation.setAnimationListener(new HideSettingsListener(this));

        // start the animation
        this.settingsView.startAnimation(scaleAnimation);
    }

    private static class HideSettingsListener implements Animation.AnimationListener
    {

        final MainActivity attachedTo;

        public HideSettingsListener(MainActivity attachedTo)
        {
            this.attachedTo = attachedTo;
        }

        @Override
        public void onAnimationStart(Animation animation)
        {
            this.attachedTo.state = ViewState.GENERIC_ANIM;
            this.attachedTo.pauseView.setVisibility(View.VISIBLE);
        }

        @Override
        public void onAnimationRepeat(Animation animation)
        {

        }

        @Override
        public void onAnimationEnd(Animation animation)
        {
            this.attachedTo.state = ViewState.PAUSE_VIEW;
            this.attachedTo.settingsView.setVisibility(View.GONE);
        }
    }
}
