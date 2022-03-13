package com.kakaroo.footprinterservice

import android.Manifest

object Common {
    val     MY_TAG                  =   "FootPrinterService"
    //preference
    val     PREF_NAME               =   "my_pref"
    val     PREF_KEY_START_TIME     =   "start_time"

    val     URL_SLASH       :   String  = "/"
    val     DEFAULT_URL     :   String  = "http://3.35.40.166:8080"//"http://192.168.219.111:8080"//"http://127.0.0.1:8080"//"http://10.0.2.2:8080"//

    //time
    val     STR_AM                  =   "오전"
    val     STR_PM                  =   "오후"

    val     ASIA_TIME_ZONE          =   "Asia/Seoul"
    
    val     RADIUS_DISTANCE         =   10     //m단위
    val     LOCATION_LATITUDE_KEY   =   "prev_latitude_key"
    val     LOCATION_LONGITUDE_KEY  =   "prev_longitude_key"

    val     HAS_BEEN_POST_THIS_SERVICE_KEY  =   "has_been_post_this_service_key"
    val     POST_CALENDAR_YEAR_KEY          =   "post_year_key"
    val     POST_CALENDAR_DAY_OF_YEAR_KEY   =   "post_day_of_year_key"
    val     POST_CALENDAR_HOUR_KEY          =   "post_hour_key"
    val     POST_CALENDAR_MINUTE_KEY        =   "post_minute_key"

    val     TRIGGER_CALENDAR_YEAR_KEY          =   "trigger_year_key"
    val     TRIGGER_CALENDAR_DAY_OF_YEAR_KEY   =   "trigger_day_of_year_key"
    val     TRIGGER_CALENDAR_HOUR_KEY          =   "trigger_hour_key"
    val     TRIGGER_CALENDAR_MINUTE_KEY        =   "trigger_minute_key"

    val     TRIGGER_CALENDAR_YEAR_DEFAULT           =   1900
    val     TRIGGER_CALENDAR_DAY_OF_YEAR_DEFAULT    =   1
    val     TRIGGER_CALENDAR_HOUR_DEFAULT           =   9
    val     TRIGGER_CALENDAR_MINUTE_DEFAULT         =   0

    val     RESTART_SERVICE_ALARM_PERIOD        = 10*1000L //10초

    val     LOCATION_DECIMAL_POINT  =   10000000
    val     DEFAULT_LATITUDE        =   373863871L
    val     DEFAULT_LONGITUDE       =   1269648526L

    val     MAP_PERMISSIONS = arrayOf(
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION)//,
                                //Manifest.permission.ACCESS_BACKGROUND_LOCATION)

    //intent
    val     REQ_CODE_MY_PERMISSION_LOCATION_ACCESS_ALL      =   100
    val     REQ_CODE_GPS_ENABLE                             =   101

    val     MY_ACTION_FOOT_PRINTER              =   "com.kakaroo.footprinterservice.FOOT_PRINTER"
    //val     MY_ACTION_FOOT_PRINTER_RESTART      =   "com.kakaroo.footprinterservice.FOOT_PRINTER_RESTART"
    val     MY_ACTION_RESTART_SERVICE           =   "com.kakaroo.footprinterservice.RESTART.PERSISTENTSERVICE"

    val     ALARM_REPEAT_MODE           = 0     //알람 서비스 시작, 설정된 start time을 알람이 등록된다.
    val     ALARM_RESTART_MODE          = 1     //서비스 해제를 위한 알람, 알람이 울리면 다시 ALARM_RESTART_REPEAT_MODE를 이용해 알람을 등록한다.
    val     ALARM_DAY_CHANGE_MODE       = 2     //요일에 해당되는 경우 다음 날짜에 알람을 등록한다.
    val     ALARM_RESTART_REPEAT_MODE   = 3     //알람이 울리거나 해제시간이 종료되어 다시 알람을 재설정할때의 모드.
    val     BOOT_COMPLETE_MODE          = 4
    val     RESTART_SERVICE_MODE        = 5


    val     ALARM_WAKEUP_TYPE_ELAPSED           =   0

    val     ACTION_RESTART_SERVICE_FOREGROUND  =   "footprinter.restartservice.foreground"
    val     ACTION_RESTART_SERVICE_BACKGROUND  =   "footprinter.restartservice.background"
}