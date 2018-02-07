package org.trv.alex.flashlight;

import android.content.Context;
import android.graphics.Color;
import android.preference.PreferenceManager;

public class AppPreferences {

    public static final String PREF_TURN_ON_FLASHLIGHT_ON_START = "prefTurnOnFlashlightOnStart";
    public static final String PREF_TURN_OFF_FLASHLIGHT_ON_MINIMIZE = "prefTurnOffFlashlightOnMinimize";
    public static final String PREF_PREVENT_SCREEN_FROM_SLEEPING = "prefPreventScreenFromSleeping";
    public static final String PREF_ENABLE_FLASHLIGHT_BLINKING = "prefEnableFlashlightBlinking";
    public static final String PREF_ENABLE_SCREEN_BLINKING = "prefEnableScreenBlinking";
    public static final String PREF_ENABLED_DURATION_FLASHLIGHT = "prefEnabledDurationFlashlight";
    public static final String PREF_DISABLED_DURATION_FLASHLIGHT = "prefDisabledDurationFlashlight";
    public static final String PREF_ENABLED_DURATION_SCREEN = "prefEnabledDurationScreen";
    public static final String PREF_DISABLED_DURATION_SCREEN = "prefDisabledDurationScreen";
    public static final String PREF_FIRST_COLOR = "prefFirstColor";
    public static final String PREF_SECOND_COLOR = "prefSecondColor";

    public static void setPrefTurnOnFlashlightOnStart(Context context, boolean turnOn) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(PREF_TURN_ON_FLASHLIGHT_ON_START, turnOn)
                .apply();
    }

    public static boolean getPrefTurnOnFlashlightOnStart(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PREF_TURN_ON_FLASHLIGHT_ON_START, false);
    }

    public static void setPrefTurnOffFlashlightOnMinimize(Context context, boolean turnOff) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(PREF_TURN_OFF_FLASHLIGHT_ON_MINIMIZE, turnOff)
                .apply();
    }

    public static boolean getPrefTurnOffFlashlightOnMinimize(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PREF_TURN_OFF_FLASHLIGHT_ON_MINIMIZE, false);
    }

    public static boolean getPrefPreventScreenFromSleeping(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PREF_PREVENT_SCREEN_FROM_SLEEPING, false);
    }

    public static boolean getPrefEnableFlashlightBlinking(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PREF_ENABLE_FLASHLIGHT_BLINKING, false);
    }

    public static boolean getPrefEnableScreenBlinking(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PREF_ENABLE_SCREEN_BLINKING, false);
    }

    public static void setPrefEnableFlashlightBlinking(Context context, boolean value) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(PREF_ENABLE_FLASHLIGHT_BLINKING, value)
                .apply();
    }

    public static void setPrefEnableScreenBlinking(Context context, boolean value) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(PREF_ENABLE_SCREEN_BLINKING, value)
                .apply();
    }

















    /*--------------------------------------------------------------------------------------------*/


    public static void setPrefEnabledDurationFlashlight(Context context, int duration) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putInt(PREF_ENABLED_DURATION_FLASHLIGHT, duration)
                .apply();
    }

    public static int getPrefEnabledDurationFlashlight(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(PREF_ENABLED_DURATION_FLASHLIGHT, 1);
    }

    public static void setPrefDisabledDurationFlashlight(Context context, int duration) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putInt(PREF_DISABLED_DURATION_FLASHLIGHT, duration)
                .apply();
    }

    public static int getPrefDisabledDurationFlashlight(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(PREF_DISABLED_DURATION_FLASHLIGHT, 1);
    }



    /*--------------------------------------------------------------------------------------------*/


    public static void setPrefEnabledDurationScreen(Context context, int duration) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putInt(PREF_ENABLED_DURATION_SCREEN, duration)
                .apply();
    }

    public static int getPrefEnabledDurationScreen(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(PREF_ENABLED_DURATION_SCREEN, 1);
    }

    public static void setPrefDisabledDurationScreen(Context context, int duration) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putInt(PREF_DISABLED_DURATION_SCREEN, duration)
                .apply();
    }

    public static int getPrefDisabledDurationScreen(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(PREF_DISABLED_DURATION_SCREEN, 1);
    }



    /*--------------------------------------------------------------------------------------------*/



    public static int getPrefFirstColor(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(PREF_FIRST_COLOR, Color.WHITE);
    }

    public static int getPrefSecondColor(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(PREF_SECOND_COLOR, Color.BLACK);
    }











}
