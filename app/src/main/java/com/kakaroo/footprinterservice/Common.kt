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

    val     POST_CALENDAR_YEAR_KEY          =   "post_year_key"
    val     POST_CALENDAR_DAY_OF_YEAR_KEY   =   "post_day_of_year_key"
    val     POST_CALENDAR_MINUTE_KEY        =   "post_minute_key"

    val     LOCATION_DECIMAL_POINT  =   10000000
    val     DEFAULT_LATITUDE        =   373863871L
    val     DEFAULT_LONGITUDE       =   1269648526L

    val     MAP_PERMISSIONS = arrayOf(
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_BACKGROUND_LOCATION)

    //intent
    val     REQ_CODE_MY_PERMISSION_LOCATION_ACCESS_ALL      =   100
    val     REQ_CODE_GPS_ENABLE                             =   101

    val     MY_ACTION_FOOT_PRINTER              =   "com.kakaroo.footprinterservice.FOOT_PRINTER"
    val     MY_ACTION_FOOT_PRINTER_RESTART      =   "com.kakaroo.footprinterservice.FOOT_PRINTER_RESTART"

    val     ALARM_REPEAT_MODE           = 0
    val     ALARM_RESTART_MODE          = 1
    val     BOOT_COMPLETE_MODE          = 2

}