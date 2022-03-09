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
import java.text.SimpleDateFormat
import java.util.*

class MyAlarmService(var mContext: Context, var mPref: SharedPreferences) {

    @RequiresApi(Build.VERSION_CODES.M)
    fun startService(action: Int) {

        Log.i(Common.MY_TAG, "startService:: action:$action" +
                "")
        //기존 알람은 삭제
        cancelAlarm(action)

        val alarmManager = this.mContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent = Intent(this.mContext, MyReceiver::class.java)

        if(action == Common.ALARM_REPEAT_MODE) {
            alarmIntent.action = Common.MY_ACTION_FOOT_PRINTER
        } else if(action == Common.ALARM_RESTART_MODE) {
            alarmIntent.action = Common.MY_ACTION_FOOT_PRINTER_RESTART
        }

        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT//FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getBroadcast(this.mContext, 0, alarmIntent, flags)//PendingIntent.FLAG_CANCEL_CURRENT)

        if(action == Common.ALARM_REPEAT_MODE) {
            val calendar = getCalendar(mPref.getInt("timePref_Key", 0))
            val strInterval : String? = mPref.getString("timeIntervalPref_key", "0")
            val minuteInterval : Int = strInterval?.toInt() ?: 0
            val millisInterval = getMilliStartIntervalTime(minuteInterval)

            val currentDateTime = calendar.time
            val dateText: String =
                SimpleDateFormat("yyyy년 MM월 dd일 EE요일 a hh시 mm분 ", Locale.getDefault()).format(
                    currentDateTime
                )
            Toast.makeText(this.mContext, dateText + "기준, "+minuteInterval+"분 간격으로 " +"서비스가 실행됩니다!", Toast.LENGTH_LONG).show()
            Log.d(Common.MY_TAG, dateText + "기준, "+minuteInterval+"분 간격으로 " +"서비스가 실행됩니다!")

            //***알람 설정 ; 파라미터에 알람타입, trigger 시간정보 calendar, 시간 간격(단위, millis이므로 하루: 24 * 60 * 60 * 1000), sender
            //repeat 로 호출시 trigger 시간정보에 간격을 더해서 알림이 울리는 것 같다. 시작시점을 당겨준다.
            calendar.timeInMillis -= millisInterval
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP, calendar.timeInMillis,
                millisInterval, pendingIntent
            )
            //alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)

        } else if(action == Common.ALARM_RESTART_MODE) {
            val minute = mPref.getInt("timeExPref_key", 0)
            val strInterval : String? = mPref.getString("timeExGapPref_key", "0")
            val minuteInterval : Int = strInterval?.toInt() ?: 0
            val calendar = getCalendar(minute + minuteInterval)

            //***알람 설정 ; 파라미터에 알람타입, trigger 시간정보 calendar, 시간 간격(단위, millis이므로 하루: 24 * 60 * 60 * 1000), sender
            alarmManager.set(
                AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent
            )
            //alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)

            val currentDateTime = calendar.time
            val dateText: String =
                SimpleDateFormat("yyyy년 MM월 dd일 EE요일 a hh시 mm분 ", Locale.getDefault()).format(
                    currentDateTime
                )
            Toast.makeText(this.mContext, dateText + "에 서비스가 다시 실행됩니다!", Toast.LENGTH_LONG).show()
            Log.i(Common.MY_TAG, dateText + "에 서비스가 다시 실행됩니다!")
        }
    }

    private fun cancelAlarm(action: Int) {
        val alarmManager = this.mContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent = Intent(this.mContext, MyReceiver::class.java)

        if(action == Common.ALARM_REPEAT_MODE) {
            alarmIntent.action = Common.MY_ACTION_FOOT_PRINTER
        } else if(action == Common.ALARM_RESTART_MODE) {
            alarmIntent.action = Common.MY_ACTION_FOOT_PRINTER_RESTART
        }
        var flags = PendingIntent.FLAG_NO_CREATE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
        }
        val pendingIntent = PendingIntent.getBroadcast(this.mContext, 0, alarmIntent, flags)
        if(pendingIntent != null) {
            Log.i(Common.MY_TAG, "등록된 알람 $action 을 해제합니다.")
            alarmManager.cancel(pendingIntent)
        } else {
            Log.i(Common.MY_TAG, "등록된 알람 $action 이 없습니다.")
        }
    }

    fun stopService(){
        Log.i(Common.MY_TAG, "stopService called")
        cancelAlarm(Common.ALARM_REPEAT_MODE)
        cancelAlarm(Common.ALARM_RESTART_MODE)

        Toast.makeText(this.mContext, "service가 중지됩니다.!!", Toast.LENGTH_SHORT).show()
    }

    private fun getCalendar(minuteValue : Int): Calendar {
        //key 값이 "message"인 설정의 저장값 가져오기.
        val hourTime = minuteValue / 60
        val minuteTime = minuteValue - (hourTime*60)

        // 현재 지정된 시간으로 알람 시간 설정
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis()
        calendar[Calendar.HOUR_OF_DAY] = hourTime
        calendar[Calendar.MINUTE] = minuteTime
        calendar[Calendar.SECOND] = 0

        // 이미 지난 시간을 지정했다면 다음날 같은 시간으로 설정
        var nowCalendar = Calendar.getInstance()
        if (calendar.before(nowCalendar)) {
            calendar.add(Calendar.DATE, 1);
        }
        return calendar
    }

    private fun getMilliStartIntervalTime(minuteValue: Int): Long {
        //key 값이 "message"인 설정의 저장값 가져오기.
        return (minuteValue * 60 * 1000).toLong()
    }
}