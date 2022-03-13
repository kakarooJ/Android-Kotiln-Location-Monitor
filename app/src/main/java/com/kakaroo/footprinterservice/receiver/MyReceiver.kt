package com.kakaroo.footprinterservice.receiver

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context

import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.preference.PreferenceManager
import com.google.android.gms.location.*
import com.kakaroo.footprinterservice.Common
import com.kakaroo.footprinterservice.RestartService
import com.kakaroo.footprinterservice.entity.FootPrinter
import com.kakaroo.footprinterservice.service.IRetrofitNetworkService
import com.kakaroo.footprinterservice.service.MyAlarmService
import com.kakaroo.footprinterservice.utility.MyUtility
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.converter.gson.GsonConverterFactory

import retrofit2.Retrofit
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.*


class MyReceiver : BroadcastReceiver() {    //manifest에 등록

    var mCurrentLocation : Location = Location("")
    var mContext: Context? = null
    lateinit var mFusedLocationProviderClient: FusedLocationProviderClient
    //var mRestartFlag: Boolean = false

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context?, intent: Intent?) {
        var recvMode: Int = Common.ALARM_REPEAT_MODE
        Log.e(Common.MY_TAG, "onReceive : ${intent?.action}")
        when (intent?.action) {
            Common.MY_ACTION_FOOT_PRINTER -> {
                recvMode = Common.ALARM_REPEAT_MODE
            }
            Intent.ACTION_SCREEN_ON -> {
                recvMode = Common.BOOT_COMPLETE_MODE
            }
            Common.MY_ACTION_RESTART_SERVICE -> {
                recvMode = Common.RESTART_SERVICE_MODE
            }
        }

        if (context != null) {
            mContext = context

            val mPref = PreferenceManager.getDefaultSharedPreferences(mContext)
            val myAlarmService = MyAlarmService(mContext!!, mPref)

            if (recvMode == Common.ALARM_REPEAT_MODE) {
                if(executeLocationService(mPref, myAlarmService)) {
                    //알람 반복을 위해 다시 알람을 등록한다.
                    myAlarmService.startAlarmService(Common.ALARM_RESTART_REPEAT_MODE)
                }
            }
            else if (recvMode == Common.BOOT_COMPLETE_MODE) {
                context.startService(Intent(context, RestartService::class.java))
                /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(Intent(context, RestartService::class.java))
                } else {
                    context.startService(Intent(context, RestartService::class.java))
                }*/

                val bStartedService = mPref.getBoolean("service_start_key", false)
                if(bStartedService) {
                    val nowCalendar = Calendar.getInstance(TimeZone.getTimeZone(Common.ASIA_TIME_ZONE))
                    if(timeIsIncludedExceptionTime(nowCalendar, mPref)) {  //현재시간이 해제시간에 포함되면
                        Log.i(Common.MY_TAG, "OnReceive - 부팅후, 현재 해제시간이므로 알람을 재설정합니다.!!")
                        myAlarmService.startAlarmService(Common.ALARM_RESTART_MODE)
                    } else {
                        Log.i(Common.MY_TAG, "OnReceive - 부팅후, 서비스 알람을 재설정합니다.!!")
                        executeLocationService(mPref, myAlarmService)   //바로 HTTP 시작

                        myAlarmService.startAlarmService(Common.ALARM_RESTART_REPEAT_MODE)
                    }
                } else {
                    Log.i(Common.MY_TAG, "OnReceive - 서비스가 켜져 있지 않았습니다.")
                }
            } else if(recvMode == Common.RESTART_SERVICE_MODE) {
                Log.i(Common.MY_TAG, "OnReceive - RestartService will be started")
                val bStartedService = mPref.getBoolean("service_start_key", false)
                //Log.i(Common.MY_TAG, "RESTART_SERVICE_MODE:: bStartedService:${bStartedService}")
                //알람 반복을 위해 다시 알람을 등록한다.
                if(bStartedService) {
                    myAlarmService.startAlarmService(Common.RESTART_SERVICE_MODE)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(Intent(context, RestartService::class.java))
                } else {
                    context.startService(Intent(context, RestartService::class.java))
                }
            }
        } else {
            Log.e(Common.MY_TAG, "OnReceive - context is not initialized yet!!")
        }
    }

    //1. 제외 시간 또는 제외날짜 설정되어 있는지 확인
    //  1.1 제외시간이 설정된 경우 현재 시간 구해서 비교
    //    1.1.1 제외시간이 포함되어 있으면 리턴
    //현재시간이 제외시간에 포함되면 기존 알람 스톱, 알람을 다음 시작시간에 맞게끔 재설정
    @RequiresApi(Build.VERSION_CODES.O)
    private fun executeLocationService(mPref: SharedPreferences, myAlarmService: MyAlarmService): Boolean {
        try {
            val selections = mPref.getStringSet("timeExDayPref_key", null) as HashSet<String>
            if(selections != null) {
                val selected: List<String> = selections.toList()

                if(selected.isNotEmpty()) { //설정된 제외 요일이 있는 경우
                    val nowCalendar = Calendar.getInstance(TimeZone.getTimeZone(Common.ASIA_TIME_ZONE))
                    val dayOfWeek = nowCalendar.get(Calendar.DAY_OF_WEEK)   //일:1, 월:2 .. 토:7
                    selected.forEach { item ->
                        if(item.toInt() == dayOfWeek) {
                            //TODO: 알람 해제, 알람을 다음 시작시간에 맞게끔 재설정하고 리턴
                            myAlarmService.startAlarmService(Common.ALARM_DAY_CHANGE_MODE)
                            Log.i(Common.MY_TAG, "OnReceive - Today[${nowCalendar[Calendar.DAY_OF_WEEK]} is Exception day!!")
                            return false
                        }
                    }
                }
            }
        } catch (e: NullPointerException) {
            Log.w(Common.MY_TAG, "$e :: intentional NullPointerException when getting timeExDayPref_key")
        }

        //현재 시간이 제외시간에 포함되는가?
        val nowCalendar = Calendar.getInstance(TimeZone.getTimeZone(Common.ASIA_TIME_ZONE))
        if(timeIsIncludedExceptionTime(nowCalendar, mPref)){
            myAlarmService.startAlarmService(Common.ALARM_RESTART_MODE)
            return false
        }

        //2. 위치 퍼미션 확인
        val locationManager = mContext!!.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            && !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            Log.i(Common.MY_TAG, "OnReceive - LocationServices is off, return")

            myAlarmService.startAlarmService(Common.ALARM_RESTART_REPEAT_MODE)
            return false
        }

        //3. 현재 위치 정보 얻기
        startLocationUpdates(mContext)

        return true
    }

    fun timeIsIncludedExceptionTime(calendar: Calendar, mPref: SharedPreferences) : Boolean {
        if(mPref != null) {
            //val mPref = PreferenceManager.getDefaultSharedPreferences(mContext)

            val strInterval : String? = mPref.getString("timeExGapPref_key", "0")
            val minuteInterval : Int = strInterval?.toInt() ?: 0

            if(minuteInterval != 0) {   //제외 시간이 설정된 경우
                val minuteValue = mPref.getInt("timeExPref_key", 0)
                return MyUtility().timeIsIncludedExceptionTime(calendar, minuteValue, minuteInterval)
            }
        } else {
            Log.d(Common.MY_TAG, "timeIsIncludedExceptionTime:: mPref is null!!!")
        }
        return false
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startLocationUpdates(context: Context?) {
        Log.i(Common.MY_TAG, "startLocationUpdates called")
        if(context != null) {
            if (ActivityCompat.checkSelfPermission(context.applicationContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(context.applicationContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(context.applicationContext, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED){
                Toast.makeText(context, "위치 권한 설정(항상허용)이 필요합니다!!", Toast.LENGTH_SHORT).show()
                Log.e(Common.MY_TAG, "startLocationUpdates:: permission is needed")
                return
            }
        } else {
            Log.e(Common.MY_TAG, "startLocationUpdates:: context is null")
            return
        }

        //FusedLocationProviderClient의 인스턴스를 생성.
        val mLocationRequest =  LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        mFusedLocationProviderClient =
            context?.let { LocationServices.getFusedLocationProviderClient(it) }!!

        //TODO: Last Location
        /*mFusedLocationProviderClient.lastLocation
            .addOnSuccessListener { location ->
                if(location == null) {
                    Log.e(Common.MY_TAG, "location get fail")
                } else {
                    Log.w(Common.MY_TAG, ">>Last Location:: ${location.latitude} , ${location.longitude}")
                    if(!checkTimeIsGoodCompareToPreviousPostTime()) {
                        Log.i(Common.MY_TAG, "Last Location:: 동일시간에 location change 정보가 추가로 들어왔기 때문에 리턴합니다.")
                        removeLocationUpdate()
                    } else {
                        mCurrentLocation = location
                        if(checkToNeedHttpPostWithLocation()) {
                            executeHttpPost()
                            removeLocationUpdate()
                        }
                    }
                }
            }
            .addOnFailureListener {
                Log.e(Common.MY_TAG, "location error is ${it.message}")
                it.printStackTrace()
            }*/

        // 기기의 위치에 관한 정기 업데이트를 요청하는 메서드 실행
        // 지정한 루퍼 스레드(Looper.myLooper())에서 콜백(mLocationCallback)으로 위치 업데이트를 요청
        mFusedLocationProviderClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper())
    }

    // 시스템으로 부터 위치 정보를 콜백으로 받음
    private val mLocationCallback = object : LocationCallback() {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onLocationResult(locationResult: LocationResult) {
            // 시스템에서 받은 location 정보를 onLocationChanged()에 전달
            locationResult.lastLocation
            onLocationChanged(locationResult.lastLocation)
        }
    }

    //3.1. 위치 정보 callback 호출
    @RequiresApi(Build.VERSION_CODES.O)
    fun onLocationChanged(location: Location) {
        Log.i(Common.MY_TAG, ">>onLocationChanged::현재 위치는 위도:${location.latitude}, 경도:${location.longitude} 입니다.")

        if(!checkTimeIsGoodCompareToPreviousPostTime()) {
            Log.i(Common.MY_TAG, "onLocationChanged:: 동일시간에 location change 정보가 추가로 들어왔기 때문에 리턴합니다.")
            removeLocationUpdate()
            return
        }

        mCurrentLocation = location
        if(checkToNeedHttpPostWithLocation()) {
            executeHttpPost()
            removeLocationUpdate()
        }
    }

    private fun checkToNeedHttpPostWithLocation(): Boolean {
        val mPref = PreferenceManager.getDefaultSharedPreferences(mContext)
        val bNearCheck : Boolean = mPref.getBoolean("near_key", false)

        //3.1.1 반경 정보 체크 유무 확인
        if(!bNearCheck) {   //false 이면 이전 값과 비교함
            val prevLatitude = mPref.getLong(Common.LOCATION_LATITUDE_KEY, Common.DEFAULT_LATITUDE+1)
            val prevLongitude = mPref.getLong(Common.LOCATION_LONGITUDE_KEY, Common.DEFAULT_LONGITUDE+10)

            val dLatitude : Double =  (prevLatitude.toDouble() / Common.LOCATION_DECIMAL_POINT)
            val dLongitude : Double =  (prevLongitude.toDouble() / Common.LOCATION_DECIMAL_POINT)

            val prevLocation = Location("")
            prevLocation.latitude = dLatitude
            prevLocation.longitude = dLongitude

            Log.i(Common.MY_TAG, "이전에 저장된 위치는 위도:$dLatitude, 경도:$dLongitude 입니다.")

            //3.1.1.1 If Yes, 현재 위치가 이전 위치의 반경 이내이면 HTTP_POST 하지 않고 리턴
            val distance : Float = mCurrentLocation.distanceTo(prevLocation)    //미터 단위
            if(distance < Common.RADIUS_DISTANCE) {
                Log.i(Common.MY_TAG, "반경 ${Common.RADIUS_DISTANCE} 미터 이내여서 저장하지 않습니다.")
                return false
            }
        }
        return true
    }

    fun removeLocationUpdate(){
        mFusedLocationProviderClient.removeLocationUpdates(mLocationCallback)
    }

    private fun checkTimeIsGoodCompareToPreviousPostTime(): Boolean {
        val mPref = PreferenceManager.getDefaultSharedPreferences(mContext)

        val calendar = Calendar.getInstance(TimeZone.getTimeZone(Common.ASIA_TIME_ZONE))
        val thisYear = calendar.get(Calendar.YEAR)
        val thisDayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        val thisHour = calendar.get(Calendar.HOUR_OF_DAY)
        val thisMinute = calendar.get(Calendar.MINUTE)

        val prevYear = mPref.getInt(Common.POST_CALENDAR_YEAR_KEY, 2000)
        val prevDayOfYear = mPref.getInt(Common.POST_CALENDAR_DAY_OF_YEAR_KEY, 1)
        val prevHour = mPref.getInt(Common.POST_CALENDAR_HOUR_KEY, 9)
        val prevMinute = mPref.getInt(Common.POST_CALENDAR_MINUTE_KEY, 10)

        Log.i(Common.MY_TAG, "현재 전송 시각은 ${thisYear}년 ${thisDayOfYear}일째 ${thisHour}시 ${thisMinute}분 입니다.")
        Log.i(Common.MY_TAG, "이전 전송 시각은 ${prevYear}년 ${prevDayOfYear}일째 ${prevHour}시 ${prevMinute}분 입니다.")

        if(thisYear == prevYear && thisDayOfYear == prevDayOfYear
            && thisHour == prevHour && thisMinute == prevMinute)  {   //같은 시간에 전송이 됐다면
            Log.i(Common.MY_TAG, "이전에 post 한 시간과 같습니다.")
            return false
        }
        return true
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun executeHttpPost() {
        //3.1.3. HTTP_POST
        val mPref = PreferenceManager.getDefaultSharedPreferences(mContext)
        var urlAddress = mPref.getString("url_key", Common.DEFAULT_URL)
        if(urlAddress == "") {
            urlAddress = Common.DEFAULT_URL
        }
        val retrofit = Retrofit.Builder()
            .baseUrl(urlAddress + Common.URL_SLASH)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val networkService: IRetrofitNetworkService = retrofit.create(IRetrofitNetworkService::class.java)

        //ex) 최종 output 형태는 2022-03-01 07:01:52 이어야 한다.
        /*val current = LocalDateTime.now()
        val formatted = current.format(DateTimeFormatter.ISO_DATE_TIME) //출력형식: 2022-03-01T07:01:52.285
        val pTime = formatted.replace("T", " ")
        val lastIndex = pTime.lastIndexOf(".")
        val time = if(lastIndex == -1) pTime else pTime.substring(0, lastIndex)*/

        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val calendar = Calendar.getInstance(TimeZone.getTimeZone(Common.ASIA_TIME_ZONE))
        val date = calendar.time
        formatter.timeZone = TimeZone.getTimeZone("Asia/Seoul")
        val today = formatter.format(date)
        val ts: String = Timestamp.valueOf(today).toString()
        val time = ts.substring(0, ts.length - 2)

        Log.i(Common.MY_TAG, "postMethod:: $time, 위도:${mCurrentLocation.latitude}, 경도:${mCurrentLocation.longitude}")

        val data = FootPrinter(id = 1, time = time, latitude = mCurrentLocation.latitude, longitude = mCurrentLocation.longitude)

        //3.1.2 현재 위치/시간을 preference에 저장
        saveLocationToPreference()
        saveHttpPostTimeToPreference()

        networkService.postMethod(data).enqueue(object : Callback<Int> {
            override fun onResponse(call: Call<Int>, response: Response<Int>) {
                Log.d(Common.MY_TAG, "onResponse:: call:$call, response:${response.toString()}")
                Log.d(Common.MY_TAG, "onResponse:: post body:${response.body().toString()}")
            }

            override fun onFailure(call: Call<Int>, t: Throwable) {
                Log.d(Common.MY_TAG, "onFailure:: call:$call, msg:${t.message.toString()}")
            }
        })
    }

    private fun saveLocationToPreference() {
        val mPref = PreferenceManager.getDefaultSharedPreferences(mContext)
        if(mCurrentLocation.latitude == 0.0 || mCurrentLocation.longitude == 0.0) {
            return
        }
        val lLatitude = (mCurrentLocation.latitude * Common.LOCATION_DECIMAL_POINT).toLong()
        val lLongitude = (mCurrentLocation.longitude * Common.LOCATION_DECIMAL_POINT).toLong()

        Log.i(Common.MY_TAG, "saveLocationToPreference::저장되는 위치는 위도:${lLatitude}, 경도:${lLongitude} 입니다.")

        with (mPref.edit()) {
            putLong(Common.LOCATION_LATITUDE_KEY, lLatitude)
            putLong(Common.LOCATION_LONGITUDE_KEY, lLongitude)
            apply()
        }
    }
    private fun saveHttpPostTimeToPreference() {
        val mPref = PreferenceManager.getDefaultSharedPreferences(mContext)
        val calendar = Calendar.getInstance(TimeZone.getTimeZone(Common.ASIA_TIME_ZONE))
        val year = calendar.get(Calendar.YEAR)
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minutes = calendar.get(Calendar.MINUTE)

        Log.i(Common.MY_TAG, "saveHttpPostTimeToPreference::저장되는 시간은 year:$year, dayOfYear:$dayOfYear, hour:$hour, min:$minutes 입니다.")

        with (mPref.edit()) {
            putInt(Common.POST_CALENDAR_YEAR_KEY, year)
            putInt(Common.POST_CALENDAR_DAY_OF_YEAR_KEY, dayOfYear)
            putInt(Common.POST_CALENDAR_HOUR_KEY, hour)
            putInt(Common.POST_CALENDAR_MINUTE_KEY, minutes)
            putBoolean(Common.HAS_BEEN_POST_THIS_SERVICE_KEY, true)
            apply()
        }
    }
}
