package com.kakaroo.footprinterservice

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TimePicker
import androidx.annotation.RequiresApi
import androidx.preference.PreferenceDialogFragmentCompat

class TimePickerPreferenceDialog : PreferenceDialogFragmentCompat() {

    lateinit var mTimePicker: TimePicker

    override fun onCreateDialogView(context: Context?): View {
        mTimePicker = TimePicker(context)
        return mTimePicker
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onBindDialogView(view: View?) {
        super.onBindDialogView(view)

        val minutesAfterMidnight = (preference as TimepickerPreference)
            .getPersistedMinutesFromMidnight()
        mTimePicker.setIs24HourView(false)
        mTimePicker.hour = minutesAfterMidnight / 60
        mTimePicker.minute = minutesAfterMidnight % 60
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onDialogClosed(positiveResult: Boolean) {
        // Save settings
        if (positiveResult) {
            val hours: Int
            val minutes: Int
            // Get the current values from the TimePicker
            if (Build.VERSION.SDK_INT >= 23) {
                hours = mTimePicker.hour
                minutes = mTimePicker.minute
            } else {
                @Suppress("DEPRECATION")
                hours = mTimePicker.currentHour
                @Suppress("DEPRECATION")
                minutes = mTimePicker.currentMinute
            }

            // Generate value to save
            val minutesAfterMidnight = hours * 60 + minutes

            // Save the value
            preference.apply {
                // This allows the client to ignore the user value.
                if (callChangeListener(minutesAfterMidnight)) {
                    // Save the value
                    (preference as TimepickerPreference).persistMinutesFromMidnight(minutesAfterMidnight)
                    preference.summary = (preference as TimepickerPreference).minutesFromMidnightToHourlyTime(minutesAfterMidnight)
                }
            }
        }
        /*if(positiveResult) {
            val minutesAfterMidnight = (timepicker.hour * 60) + timepicker.minute
            (preference as TimepickerPreference).persistMinutesFromMidnight(minutesAfterMidnight)
            preference.summary = "123"//minutesFromMidnightToHourlyTime(minutesAfterMidnight)
        }*/
    }

    companion object {
        fun newInstance(key: String): TimePickerPreferenceDialog {
            val fragment = TimePickerPreferenceDialog()
            val bundle = Bundle(1)
            bundle.putString(ARG_KEY, key)
            fragment.arguments = bundle

            return fragment
        }
    }
}