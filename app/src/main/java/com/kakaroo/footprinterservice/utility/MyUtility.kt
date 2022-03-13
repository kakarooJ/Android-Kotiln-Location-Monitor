package com.kakaroo.footprinterservice.utility

import android.util.Log
import com.kakaroo.footprinterservice.Common
import java.text.SimpleDateFormat
import java.util.*

class MyUtility {
    object MyUtility {}

    fun getCalendar(minuteValue: Int, addDate: Boolean): Calendar {
        //key 값이 "message"인 설정의 저장값 가져오기.
        val hourTime = minuteValue / 60
        val minuteTime = minuteValue - (hourTime*60)

        // 현재 지정된 시간으로 알람 시간 설정
        val calendar = Calendar.getInstance(TimeZone.getTimeZone(Common.ASIA_TIME_ZONE))
        //calendar.timeInMillis = System.currentTimeMillis()
        calendar[Calendar.HOUR_OF_DAY] = hourTime
        calendar[Calendar.MINUTE] = minuteTime
        calendar[Calendar.SECOND] = 0

        // 이미 지난 시간을 지정했다면 다음날 같은 시간으로 설정
        if(addDate) {
            var nowCalendar = Calendar.getInstance(TimeZone.getTimeZone(Common.ASIA_TIME_ZONE))
            if (calendar.before(nowCalendar)) {
                Log.i(Common.MY_TAG, "현재시간을 지났기 때문에 다음날짜로 설정됩니다.")
                calendar.add(Calendar.DATE, 1);
            }
        }

        return calendar
    }

    fun getMilliTime(minuteValue: Int): Long {
        //key 값이 "message"인 설정의 저장값 가져오기.
        return (minuteValue * 60 * 1000).toLong()
    }

    fun timeIsIncludedExceptionTime(calendar: Calendar, startTime: Int, intervalTime: Int) : Boolean {
        val startCalendar = getCalendar(startTime, false)
        val endCalendar = getCalendar(startTime+intervalTime, false)

        //현재 시간이 제외시간에 포함되는가?
        if ((startCalendar.before(calendar) || startCalendar == calendar) && endCalendar.after(calendar)) {
            return true
        }
        return false
    }

    fun getDateTimeFromCalendar(calendar: Calendar): String {
        return SimpleDateFormat("yyyy년 MM월 dd일 EE요일 a hh시 mm분 ", Locale.getDefault()).format(
            calendar.time
        )
    }
}