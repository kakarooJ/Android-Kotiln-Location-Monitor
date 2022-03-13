package com.kakaroo.footprinterservice

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import com.kakaroo.footprinterservice.receiver.MyReceiver
import com.kakaroo.footprinterservice.service.MyAlarmService

class RestartService : Service() {
    companion object {
        const val NOTIFICATION_ID = 10
        const val CHANNEL_ID = "primary_notification_channel"
    }

    override fun onBind(intent: Intent): IBinder? {
        TODO("Return the communication channel to the service.")
        //Log.i(Common.MY_TAG, "RestartService::onBind()")
        //return null
    }

    override fun onCreate() {
        Log.v(Common.MY_TAG, "RestartService::onCreate()")
        unregisterRestartAlarm()
        super.onCreate()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val notificationChannel = NotificationChannel(
            CHANNEL_ID,
            "FootPrinter Service",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationChannel.enableLights(true)
        notificationChannel.lightColor = Color.RED
        notificationChannel.enableVibration(true)
        notificationChannel.description = "FootPrinter Notification"

        val notificationManager = applicationContext.getSystemService(
            Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(
            notificationChannel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("MyService is running")
                .setContentText("MyService is running")
                .build()
            Log.d(Common.MY_TAG, "start foreground")
            startForeground(NOTIFICATION_ID, notification)

            /**
             * startForeground 를 사용하면 notification 을 보여주어야 하는데 없애기 위한 코드
             */
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val emptyNoti: Notification.Builder =
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    Notification.Builder(this, CHANNEL_ID)
                } else {
                    Notification.Builder(this)
                }
            emptyNoti.setSmallIcon(R.drawable.notification_bg)

            nm.notify(NOTIFICATION_ID, emptyNoti.build())
            nm.cancel(NOTIFICATION_ID)
        }

        return START_STICKY


        //return super.onStartCommand(intent, flags, startId)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.v(Common.MY_TAG, "RestartService::onTaskRemoved()")
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        Log.v(Common.MY_TAG, "RestartService::onDestroy()")
        registerRestartAlarm()
        super.onDestroy()
    }

    private fun unregisterRestartAlarm() {
        val alarmManager = this.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent = Intent(this, MyReceiver::class.java)
        alarmIntent.action = Common.MY_ACTION_RESTART_SERVICE

        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT//FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getBroadcast(this, 0, alarmIntent, flags)
        if(pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }

    private fun registerRestartAlarm() {
        val alarmIntent = Intent(this, MyReceiver::class.java)
        alarmIntent.action = Common.MY_ACTION_RESTART_SERVICE
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT//FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getBroadcast(this, 0, alarmIntent, flags)

        val alarmManager = this.getSystemService(Context.ALARM_SERVICE) as AlarmManager
/*
        val firstTime = SystemClock.elapsedRealtime() + Common.RESTART_SERVICE_ALARM_PERIOD
        alarmManager.setRepeating(
            AlarmManager.ELAPSED_REALTIME_WAKEUP, firstTime, Common.RESTART_SERVICE_ALARM_PERIOD, pendingIntent)
*/
        val triggerTimeMillis: Long = Common.RESTART_SERVICE_ALARM_PERIOD
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerTimeMillis, pendingIntent)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ->
                alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerTimeMillis, pendingIntent)
            else ->
                alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerTimeMillis, pendingIntent)
        }

    }
}