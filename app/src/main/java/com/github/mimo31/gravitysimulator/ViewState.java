package com.github.mimo31.gravitysimulator;

/**
 * Created by mimo31 on 7/27/17.
 * Specifies the current view / action the Activity is currently in / is performing.
 */

public enum ViewState
{
    SIMULATION, PAUSE_VIEW, HELP_VIEW, SETTINGS_VIEW, ADD_OBJECT_VIEW, ADDING_OBJECT,

    // animations

    ANIM_PAUSING, ANIM_RESUMING, GENERIC_ANIM, ANIM_ADD_OBJECT_TO_GRAVITY, ANIM_ADD_OBJECT_CANCEL, ANIM_PAUSE_TO_ADD_OBJECT, ANIM_ADD_OBJECT_TO_PAUSE
}