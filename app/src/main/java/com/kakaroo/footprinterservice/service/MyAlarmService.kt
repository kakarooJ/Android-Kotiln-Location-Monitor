package com.kakaroo.footprinterservice.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.kakaroo.footprinterservice.Common
import com.kakaroo.footprinterservice.receiver.MyReceiver
import com.kakaroo.footprinterservice.utility.MyUtility
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class MyAlarmService(var mContext: Context, var mPref: SharedPreferences) {

    @RequiresApi(Build.VERSION_CODES.M)
    fun startAlarmService(action: Int) {

        Log.i(Common.MY_TAG, "startAlarmService:: action:$action" )

        val bStartedService = mPref.getBoolean("service_start_key", false)
        Log.i(Common.MY_TAG, "startAlarmService:: bStartedService:${bStartedService}")

        //기존 알람은 삭제
        cancelAlarm()

        val alarmManager = this.mContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent = Intent(this.mContext, MyReceiver::class.java)

        alarmIntent.action = Common.MY_ACTION_FOOT_PRINTER
        /*
        if(action == Common.ALARM_REPEAT_MODE || action == Common.ALARM_RESTART_REPEAT_MODE || action == Common.ALARM_DAY_CHANGE_MODE) {
            alarmIntent.action = Common.MY_ACTION_FOOT_PRINTER
        } else if(action == Common.ALARM_RESTART_MODE) {
            alarmIntent.action = Common.MY_ACTION_FOOT_PRINTER_RESTART
        }*/

        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT//FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getBroadcast(this.mContext, 0, alarmIntent, flags)//PendingIntent.FLAG_CANCEL_CURRENT)
        var triggerTimeMillis: Long = 0

        if(action == Common.ALARM_REPEAT_MODE) {    //start service
            val calendar = MyUtility().getCalendar(mPref.getInt("timePref_Key", 0), true)

            val strInterval : String? = mPref.getString("timeIntervalPref_key", "0")
            val minuteInterval : Int = strInterval?.toInt() ?: 0

            try {
                val selections = mPref.getStringSet("timeExDayPref_key", null) as HashSet<String>
                if(selections != null) {
                    val selected: List<String> = selections.toList()

                    //위에서 설정된 시간이 제외 요일에 포함되어 있는 경우 제외되지 않은 다음 날짜로 설정한다.
                    if(selected.isNotEmpty()) {
                        var dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)   //일:1, 월:2 .. 토:7
                        selected.forEach { item ->
                            if(item.toInt() == dayOfWeek) {
                                Log.w(Common.MY_TAG, "제외 요일이므로 다음날짜에 시작됩니다.")
                                calendar.add(Calendar.DATE, 1)
                                dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                            }
                        }
                        dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                        if(dayOfWeek == 1) { //위에서 계산된 요일이 일요일이면 한 번 더 체크한다.
                            selected.forEach { item ->
                                Log.w(Common.MY_TAG, "2nd item: ${item.toInt()}, dayOfWeek: ${dayOfWeek}")
                                if(item.toInt() == dayOfWeek) {
                                    Log.w(Common.MY_TAG, "제외 요일이므로 다음날짜에 시작됩니다.")
                                    calendar.add(Calendar.DATE, 1)
                                    dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                                }
                            }
                        }
                    }
                }
            } catch (e: NullPointerException) {
                Log.w(Common.MY_TAG, "$e :: intentional NullPointerException when getting timeExDayPref_key")
            }

            val dateText = MyUtility().getDateTimeFromCalendar(calendar)
            Toast.makeText(this.mContext, dateText + "부터, "+minuteInterval+"분 간격으로 " +"알림이 실행됩니다!", Toast.LENGTH_LONG).show()
            Log.d(Common.MY_TAG, dateText + "부터, "+minuteInterval+"분 간격으로 " +"알림이 실행됩니다!")

            //ELAPSED_REALTIME_WAKEUP
            triggerTimeMillis = if(Common.ALARM_WAKEUP_TYPE_ELAPSED == 1) {
                val nowCalendar = Calendar.getInstance(TimeZone.getTimeZone(Common.ASIA_TIME_ZONE))
                val diffTimeMillis = calendar.timeInMillis - nowCalendar.timeInMillis
                diffTimeMillis
            } else {
                calendar.timeInMillis
            }
        } else if(action == Common.ALARM_RESTART_REPEAT_MODE) {
            val calendar = Calendar.getInstance(TimeZone.getTimeZone(Common.ASIA_TIME_ZONE))
            calendar[Calendar.SECOND] = 0
            val strInterval : String? = mPref.getString("timeIntervalPref_key", "0")
            val minuteInterval : Int = strInterval?.toInt() ?: 0
            val millisInterval = MyUtility().getMilliTime(minuteInterval)

            val dateText = MyUtility().getDateTimeFromCalendar(calendar)
            //Toast.makeText(this.mContext, dateText + "부터, "+minuteInterval+"분 간격으로 " +"알림이 실행됩니다!", Toast.LENGTH_LONG).show()
            Log.d(Common.MY_TAG, dateText + "부터, "+minuteInterval+"분 간격으로 알람이 울립니다!")

            //ELAPSED_REALTIME_WAKEUP
            if(Common.ALARM_WAKEUP_TYPE_ELAPSED == 1) {
                val nowCalendar = Calendar.getInstance(TimeZone.getTimeZone(Common.ASIA_TIME_ZONE))
                nowCalendar[Calendar.SECOND] = 0
                val diffMillis = abs(System.currentTimeMillis() - nowCalendar.timeInMillis)
                Log.d(Common.MY_TAG, "diffMillis: $diffMillis")
                //0초 부터 알람을 울리기 위해
                triggerTimeMillis = millisInterval - diffMillis
            } else {
                triggerTimeMillis = calendar.timeInMillis + millisInterval
            }

        } else if(action == Common.RESTART_SERVICE_MODE) {
            Log.w(Common.MY_TAG, "RESTART_SERVICE_MODE!!!")

            var bTimeIsSet = false
            var calendar = Calendar.getInstance(TimeZone.getTimeZone(Common.ASIA_TIME_ZONE))
            calendar[Calendar.SECOND] = 0

            val strInterval : String? = mPref.getString("timeIntervalPref_key", "0")
            val minuteInterval : Int = strInterval?.toInt() ?: 0

            val prevTriggerCalendar = getTriggerPrefTime()

            Log.d(Common.MY_TAG, "현재 시간은 ${MyUtility().getDateTimeFromCalendar(calendar)} 입니다")
            Log.d(Common.MY_TAG, "트리거시간은 ${MyUtility().getDateTimeFromCalendar(prevTriggerCalendar)} 입니다")

            if(prevTriggerCalendar.before(calendar) || prevTriggerCalendar == calendar) {   //prevTriggerCalendar < calendar :: Trigger할 시간이 지났으면 바로 알람을 띄운다.
                bTimeIsSet = true
                Log.w(Common.MY_TAG, "RESTART_SERVICE_MODE:Case-1")
            } else if(mPref.getBoolean(Common.HAS_BEEN_POST_THIS_SERVICE_KEY, false)) { // 이번 서비스에 post 한 적이 있다면
                val postYear = mPref.getInt(Common.POST_CALENDAR_YEAR_KEY, 2000)
                val postDayOfYear = mPref.getInt(Common.POST_CALENDAR_DAY_OF_YEAR_KEY, 1)
                val postHour = mPref.getInt(Common.POST_CALENDAR_HOUR_KEY, 9)
                val postMinute = mPref.getInt(Common.POST_CALENDAR_MINUTE_KEY, 10)

                val postCalendar = Calendar.getInstance(TimeZone.getTimeZone(Common.ASIA_TIME_ZONE))

                postCalendar[Calendar.YEAR] = postYear
                postCalendar[Calendar.DAY_OF_YEAR] = postDayOfYear
                postCalendar[Calendar.HOUR_OF_DAY] = postHour
                postCalendar[Calendar.MINUTE] = postMinute + minuteInterval
                postCalendar[Calendar.SECOND] = 0

                Log.d(Common.MY_TAG, "포스트예상시간은 ${MyUtility().getDateTimeFromCalendar(postCalendar)} 입니다")

                if(postCalendar.before(calendar) || postCalendar == calendar) {   //Post할 시간이 현재 시간을 지났으면 바로 알람을 띄운다.
                    bTimeIsSet = true
                    Log.w(Common.MY_TAG, "RESTART_SERVICE_MODE:Case-2")
                }
            }

            if(bTimeIsSet) {
                //전송할 시간이 exception시간에 속하면 exception 종료시간으로 알람을 설정한다.
                if(MyReceiver().timeIsIncludedExceptionTime(calendar, mPref)) {
                    val minute = mPref.getInt("timeExPref_key", 0)
                    val strInterval : String? = mPref.getString("timeExGapPref_key", "0")
                    val minuteInterval : Int = strInterval?.toInt() ?: 0
                    calendar = MyUtility().getCalendar(minute + minuteInterval, true)
                    Log.w(Common.MY_TAG, "RESTART_SERVICE_MODE: Exception 시간으로 전송시간을 재설정합니다.")
                }
            }
            
            if(bTimeIsSet) {
                //최종적으로 설정된 시간이 맞는지 체크
                val nowCalendar = Calendar.getInstance(TimeZone.getTimeZone(Common.ASIA_TIME_ZONE))
                nowCalendar[Calendar.SECOND] = 0

                if(calendar.before(nowCalendar) || calendar == nowCalendar) {
                    Log.e(Common.MY_TAG, "Error:: Time is already passed, will be changed to current time")
                    calendar = nowCalendar
                    //calendar.add(Calendar.DATE, 1);
                }

                val dateText = MyUtility().getDateTimeFromCalendar(calendar)
                //Toast.makeText(this.mContext, dateText + "부터, "+minuteInterval+"분 간격으로 " +"알림이 실행됩니다!", Toast.LENGTH_LONG).show()
                Log.d(Common.MY_TAG, dateText + "부터, "+minuteInterval+"분 간격으로 알람이 울립니다!")

                //ELAPSED_REALTIME_WAKEUP
                if(Common.ALARM_WAKEUP_TYPE_ELAPSED == 1) {
                    val millisInterval = MyUtility().getMilliTime(minuteInterval)
                    val nowCalendar = Calendar.getInstance(TimeZone.getTimeZone(Common.ASIA_TIME_ZONE))
                    nowCalendar[Calendar.SECOND] = 0
                    val diffMillis = abs(System.currentTimeMillis()-nowCalendar.timeInMillis)
                    Log.d(Common.MY_TAG, "diffMillis: $diffMillis")
                    //0초 부터 알람을 울리기 위해
                    triggerTimeMillis = millisInterval - diffMillis
                } else {
                    triggerTimeMillis = calendar.timeInMillis
                }
            }

        } else if(action == Common.ALARM_RESTART_MODE) {
            val minute = mPref.getInt("timeExPref_key", 0)
            val strInterval : String? = mPref.getString("timeExGapPref_key", "0")
            val minuteInterval : Int = strInterval?.toInt() ?: 0
            val calendar = MyUtility().getCalendar(minute + minuteInterval, true)

            val dateText = MyUtility().getDateTimeFromCalendar(calendar)
            //Toast.makeText(this.mContext, dateText + "에 알림이 다시 실행될 예정입니다!", Toast.LENGTH_LONG).show()
            Log.w(Common.MY_TAG, "현재시간이 제외시간이기 때문에 ${dateText}에 알림이 다시 실행될 예정입니다!")

            //ELAPSED_REALTIME_WAKEUP
            triggerTimeMillis = if(Common.ALARM_WAKEUP_TYPE_ELAPSED == 1) {
                val nowCalendar = Calendar.getInstance(TimeZone.getTimeZone(Common.ASIA_TIME_ZONE))
                val diffTimeMillis = calendar.timeInMillis - nowCalendar.timeInMillis
                diffTimeMillis//-30000 //30000 : 10초전에 알람을 울려 restart를 한다.*/
            } else {
                calendar.timeInMillis
            }
        } else if(action == Common.ALARM_DAY_CHANGE_MODE) {
            val calendar = MyUtility().getCalendar(0, true)   //00시 00분
            val dateText = MyUtility().getDateTimeFromCalendar(calendar)
            //Toast.makeText(this.mContext, dateText + "에 알림이 다시 실행될 예정입니다!", Toast.LENGTH_LONG).show()
            Log.w(Common.MY_TAG, dateText + "에 알림이 다시 실행될 예정입니다!")

            //ELAPSED_REALTIME_WAKEUP
            triggerTimeMillis = if(Common.ALARM_WAKEUP_TYPE_ELAPSED == 1) {
                val nowCalendar = Calendar.getInstance(TimeZone.getTimeZone(Common.ASIA_TIME_ZONE))
                val diffTimeMillis = calendar.timeInMillis - nowCalendar.timeInMillis
                diffTimeMillis
            } else {
                calendar.timeInMillis
            }
        }

        //***알람 설정 ; 파라미터에 알람타입, trigger 시간정보 calendar, 시간 간격(단위, millis이므로 하루: 24 * 60 * 60 * 1000), sender
        /*alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP, calendar.timeInMillis,
            millisInterval, pendingIntent
        )*/

        if(triggerTimeMillis > 0) {
            //ELAPSED_REALTIME_WAKEUP
            if(Common.ALARM_WAKEUP_TYPE_ELAPSED == 1) {
                Log.d(Common.MY_TAG, "triggerTimeMillis: ${triggerTimeMillis}, ${triggerTimeMillis/60000.0}분 뒤에 알람 설정")
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerTimeMillis, pendingIntent)
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ->
                        alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerTimeMillis, pendingIntent)
                    else ->
                        alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerTimeMillis, pendingIntent)
                }
            } else {
                //휴대폰 설정한 시간 기준
                val tDate = Date(triggerTimeMillis)
                val tDateFormat = SimpleDateFormat("yyyy-MM-dd kk:mm:ss E", Locale("ko", "KR"))

                // 현재 시간을 dateFormat 에 선언한 형태의 String 으로 변환
                val strDate = tDateFormat.format(tDate)

                Log.i(Common.MY_TAG, "${strDate}요일로 알람이 설정되었습니다.")
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent)
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ->
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent)
                    else ->
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent)
                }
            }

            setTriggerPrefTime(triggerTimeMillis)
        }
    }

    private fun cancelAlarm() {
        val alarmManager = this.mContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent = Intent(this.mContext, MyReceiver::class.java)

        alarmIntent.action = Common.MY_ACTION_FOOT_PRINTER

        var flags = PendingIntent.FLAG_UPDATE_CURRENT//FLAG_NO_CREATE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT//FLAG_NO_CREATE
        }
        val pendingIntent = PendingIntent.getBroadcast(this.mContext, 0, alarmIntent, flags)
        if(pendingIntent != null) {
            Log.i(Common.MY_TAG, "알람 ${alarmIntent.action}를 해제합니다.")
            alarmManager.cancel(pendingIntent)
        } else {
            Log.i(Common.MY_TAG, "등록된 알람 ${alarmIntent.action}가 없습니다.")
        }
    }

    fun stopAlarmService(){
        Log.i(Common.MY_TAG, "stopAlarmService called")
        cancelAlarm()

        resetTriggerPrefTime()
        resetPostTrialPref()

        //Toast.makeText(this.mContext, "service가 중지됩니다.!!", Toast.LENGTH_SHORT).show()
    }

    private fun setTriggerPrefTime(timeMillis: Long) {
        if(mPref != null) {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = timeMillis

            val mYear = calendar[Calendar.YEAR]
            val mDay = calendar[Calendar.DAY_OF_YEAR]
            val mHour = calendar[Calendar.HOUR_OF_DAY]
            val mMinute = calendar[Calendar.MINUTE]

            Log.d(Common.MY_TAG,
                "setTriggerPrefTime:: ${mYear}년 ${mDay}일 ${mHour}시 ${mMinute}분")
            with (mPref.edit()) {
                putInt(Common.TRIGGER_CALENDAR_YEAR_KEY, mYear)
                putInt(Common.TRIGGER_CALENDAR_DAY_OF_YEAR_KEY, mDay)
                putInt(Common.TRIGGER_CALENDAR_HOUR_KEY, mHour)
                putInt(Common.TRIGGER_CALENDAR_MINUTE_KEY, mMinute)
                apply()
            }
        }
    }

    private fun resetTriggerPrefTime() {
        if(mPref != null) {
            Log.d(Common.MY_TAG, "resetTriggerPrefTime:: trigger time will be reset!!")
            with (mPref.edit()) {
                putInt(Common.TRIGGER_CALENDAR_YEAR_KEY, Common.TRIGGER_CALENDAR_YEAR_DEFAULT)
                putInt(Common.TRIGGER_CALENDAR_DAY_OF_YEAR_KEY, Common.TRIGGER_CALENDAR_DAY_OF_YEAR_DEFAULT)
                putInt(Common.TRIGGER_CALENDAR_HOUR_KEY, Common.TRIGGER_CALENDAR_HOUR_DEFAULT)
                putInt(Common.TRIGGER_CALENDAR_MINUTE_KEY, Common.TRIGGER_CALENDAR_MINUTE_DEFAULT)
                apply()
            }
        }
    }

    private fun resetPostTrialPref() {
        if(mPref != null) {
            Log.d(Common.MY_TAG, "resetPostTrialPref:: post trial will be reset!!")
            with (mPref.edit()) {
                putBoolean(Common.HAS_BEEN_POST_THIS_SERVICE_KEY, false)
                apply()
            }
        }
    }

    private fun getTriggerPrefTime(): Calendar {
        var calendar: Calendar = Calendar.getInstance()
        if(mPref != null) {
            calendar[Calendar.YEAR] = mPref.getInt(Common.TRIGGER_CALENDAR_YEAR_KEY, Common.TRIGGER_CALENDAR_YEAR_DEFAULT)
            calendar[Calendar.DAY_OF_YEAR] = mPref.getInt(Common.TRIGGER_CALENDAR_DAY_OF_YEAR_KEY, Common.TRIGGER_CALENDAR_DAY_OF_YEAR_DEFAULT)
            calendar[Calendar.HOUR_OF_DAY] = mPref.getInt(Common.TRIGGER_CALENDAR_HOUR_KEY, Common.TRIGGER_CALENDAR_HOUR_DEFAULT)
            calendar[Calendar.MINUTE] = mPref.getInt(Common.TRIGGER_CALENDAR_MINUTE_KEY, Common.TRIGGER_CALENDAR_MINUTE_DEFAULT)
        } else {
            calendar[Calendar.YEAR] = Common.TRIGGER_CALENDAR_YEAR_DEFAULT
        }
        calendar[Calendar.SECOND] = 0
        return calendar
    }
}