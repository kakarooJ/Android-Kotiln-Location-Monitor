package com.kakaroo.footprinterservice

import android.content.Context
import android.util.AttributeSet
import androidx.preference.DialogPreference

// This class is used in our preference where user can pick a time for notifications to appear.
// Specifically, this class is responsible for saving/retrieving preference data.
class TimepickerPreference(context: Context?, attrs: AttributeSet?) : DialogPreference(context, attrs) {

    // Get saved preference value (in minutes from midnight, so 1 AM is represented as 1*60 here
    fun getPersistedMinutesFromMidnight(): Int {
        return super.getPersistedInt(DEFAULT_MINUTES_FROM_MIDNIGHT)
    }

    // Save preference
    fun persistMinutesFromMidnight(minutesFromMidnight: Int) {
        super.persistInt(minutesFromMidnight)
        notifyChanged()
    }

    fun minutesFromMidnightToHourlyTime(minutes: Int) : String{
        val hourTime = minutes / 60
        val minuteTime = minutes - (hourTime*60)

        val strAmPm = if(hourTime >= 12) Common.STR_PM else Common.STR_AM
        val timeHour = if(hourTime > 12) hourTime - 12 else hourTime
        return strAmPm + " " + timeHour.toString() + "시 " + minuteTime.toString() + "분"
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        super.onSetInitialValue(defaultValue)
        summary = minutesFromMidnightToHourlyTime(getPersistedMinutesFromMidnight())
        //time = getPersistedInt((defaultValue as? Int) ?: DEFAULT_VALUE)
    }

    // Mostly for default values
    companion object {
        // By default we want notification to appear at 9 AM each time.
        private const val DEFAULT_HOUR = 9
        const val DEFAULT_MINUTES_FROM_MIDNIGHT = DEFAULT_HOUR * 60
    }

}