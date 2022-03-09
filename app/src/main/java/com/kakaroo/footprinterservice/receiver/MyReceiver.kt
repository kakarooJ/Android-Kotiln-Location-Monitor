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
import com.google.gson.Gson
import com.kakaroo.footprinterservice.Common
import com.kakaroo.footprinterservice.entity.FootPrinter
import com.kakaroo.footprinterservice.service.IRetrofitNetworkService
import com.kakaroo.footprinterservice.service.MyAlarmService
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.converter.gson.GsonConverterFactory

import retrofit2.Retrofit
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.lang.Exception
import java.lang.StringBuilder
import java.net.HttpURLConnection
import java.net.URL
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.min


class MyReceiver : BroadcastReceiver() {    //manifest에 등록

    var mCurrentLocation : Location = Location("")
    lateinit var mContext: Context
    lateinit var mFusedLocationProviderClient: FusedLocationProviderClient
    //var mRestartFlag: Boolean = false

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onReceive(context: Context?, intent: Intent?) {
        var recvMode: Int = Common.ALARM_REPEAT_MODE
        Log.d(Common.MY_TAG, "onReceive : ${intent?.action}")
        when (intent?.action) {
            Common.MY_ACTION_FOOT_PRINTER -> {
                recvMode = Common.ALARM_REPEAT_MODE
            }
            Common.MY_ACTION_FOOT_PRINTER_RESTART -> {
                recvMode = Common.ALARM_RESTART_MODE
            }
            Intent.ACTION_SCREEN_ON -> {
                recvMode = Common.BOOT_COMPLETE_MODE
            }
        }

        if (context != null) {
            mContext = context

            val mPref = PreferenceManager.getDefaultSharedPreferences(mContext)
            val myAlarmService = MyAlarmService(this.mContext, mPref)

            if (recvMode == Common.ALARM_REPEAT_MODE) {
                //시나리오 : 나중에
                //1. 제외 시간 또는 제외날짜 설정되어 있는지 확인
                //  1.1 제외시간이 설정된 경우 현재 시간 구해서 비교
                //    1.1.1 제외시간이 포함되어 있으면 리턴
                //현재시간이 제외시간에 포함되면 기존 알람 스톱, 알람을 다음 시작시간에 맞게끔 재설정
                //재시작 플래그를 설정

                //var selections: HashSet<String>// = HashSet<String>()
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
                                    myAlarmService.stopService()
                                    myAlarmService.startService(Common.ALARM_RESTART_MODE)
                                    Log.i(Common.MY_TAG, "OnReceive - Exception day!!")
                                    return
                                }
                            }
                        }
                    }
                } catch (e: NullPointerException) {
                    Log.w(Common.MY_TAG, "$e :: intentional NullPointerException when getting timeExDayPref_key")
                }


                val strInterval : String? = mPref.getString("timeExGapPref_key", "0")
                val minuteInterval : Int = strInterval?.toInt() ?: 0

                if(minuteInterval != 0) {   //제외 시간이 설정된 경우
                    val minuteValue = mPref.getInt("timeExPref_key", 0)
                    val endMinuteVlaue = minuteValue + minuteInterval

                    val hourTime = minuteValue / 60
                    val minuteTime = minuteValue - (hourTime*60)

                    val endHourTime = endMinuteVlaue / 60
                    val endMinuteTime = endMinuteVlaue - (hourTime*60)

                    // 현재 지정된 시간으로 알람 시간 설정
                    val startCalendar = Calendar.getInstance(TimeZone.getTimeZone(Common.ASIA_TIME_ZONE))
                    startCalendar.timeInMillis = System.currentTimeMillis()
                    startCalendar[Calendar.HOUR_OF_DAY] = hourTime
                    startCalendar[Calendar.MINUTE] = minuteTime
                    startCalendar[Calendar.SECOND] = 0

                    val endCalendar = Calendar.getInstance(TimeZone.getTimeZone(Common.ASIA_TIME_ZONE))
                    endCalendar.timeInMillis = System.currentTimeMillis()
                    endCalendar[Calendar.HOUR_OF_DAY] = endHourTime
                    endCalendar[Calendar.MINUTE] = endMinuteTime
                    endCalendar[Calendar.SECOND] = 0

                    // 이미 지난 시간을 지정했다면 다음날 같은 시간으로 설정
                    val nowCalendar = Calendar.getInstance(TimeZone.getTimeZone(Common.ASIA_TIME_ZONE))
                    //현재 시간이 제외시간에 포함되는가?
                    if (startCalendar.before(nowCalendar) && endCalendar.after(nowCalendar)) {
                        //TODO: 알람 해제, 알람을 다음 시작시간에 맞게끔 재설정하고 리턴
                        Log.i(Common.MY_TAG, "OnReceive - ${startCalendar.get(Calendar.HOUR_OF_DAY)}시 ${startCalendar.get(Calendar.MINUTE)}분부터 "+
                                "${endCalendar.get(Calendar.HOUR_OF_DAY)}시 ${endCalendar.get(Calendar.MINUTE)}분까지 제외시간입니다.")
                        //mRestartFlag = true
                        myAlarmService.stopService()
                        myAlarmService.startService(Common.ALARM_RESTART_MODE)
                        return
                    } /*else {
                        if(mRestartFlag) {
                            //TODO: 알람간격 재설정
                            Log.i(Common.MY_TAG, "OnReceive - 알람을 재설정합니다.!!")
                            mRestartFlag = false
                            myAlarmService.startService(Common.ALARM_REPEAT_MODE)
                        }
                    }*/
                }

                //2. 위치 퍼미션 확인
                val locationManager = mContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    && !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    Log.i(Common.MY_TAG, "OnReceive - LocationServices is off, return")
                    return
                }

                //3. 현재 위치 정보 얻기
                startLocationUpdates(mContext)

            }
            else if (recvMode == Common.ALARM_RESTART_MODE) {
                Log.i(Common.MY_TAG, "OnReceive - 해제시간 후, 알람을 재설정합니다.!!")
                myAlarmService.startService(Common.ALARM_REPEAT_MODE)
            }
            else if (recvMode == Common.BOOT_COMPLETE_MODE) {
                val bStartedService = mPref.getBoolean("service_start_key", false)
                if(bStartedService) {
                    Log.i(Common.MY_TAG, "OnReceive - 부팅후, 서비스 알람을 재설정합니다.!!")
                    myAlarmService.startService(Common.ALARM_REPEAT_MODE)
                } else {
                    Log.i(Common.MY_TAG, "OnReceive - 서비스가 켜져 있지 않았습니다.")
                }
            }
        }
    }

    private fun startLocationUpdates(context: Context?) {
        Log.i(Common.MY_TAG, "startLocationUpdates called")
        if(context != null) {
            if (ActivityCompat.checkSelfPermission(context.applicationContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(context.applicationContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(context.applicationContext, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED){
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
                    Log.d(Common.MY_TAG, "Last Location:: ${location.latitude} , ${location.longitude}")
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
        mCurrentLocation = location
        Log.i(Common.MY_TAG, "onLocationChanged::현재 위치는 위도:${mCurrentLocation.latitude}, 경도:${mCurrentLocation.longitude} 입니다.")

        if(isSameMinuteCompareToPreviousPostTime()) {
            Log.i(Common.MY_TAG, "onLocationChanged:: 동일시간에 location change 정보가 추가로 들어왔기 때문에 리턴합니다.")
            removeLocationUpdate()
            return
        }

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
                return
            }
        }
        executeHttpPost(mPref)
        removeLocationUpdate()
    }

    fun removeLocationUpdate(){
        mFusedLocationProviderClient.removeLocationUpdates(mLocationCallback)
    }

    private fun isSameMinuteCompareToPreviousPostTime(): Boolean {
        val mPref = PreferenceManager.getDefaultSharedPreferences(mContext)

        val calendar = Calendar.getInstance(TimeZone.getTimeZone(Common.ASIA_TIME_ZONE))
        val thisYear = calendar.get(Calendar.YEAR)
        val thisDayOfYear = calendar.get(Calendar.DAY_OF_YEAR)

        val prevYear = mPref.getInt(Common.POST_CALENDAR_YEAR_KEY, 2000)
        val prevDayOfYear = mPref.getInt(Common.POST_CALENDAR_DAY_OF_YEAR_KEY, 1)

        Log.e(Common.MY_TAG, "thisYear: $thisYear, thisDayOfYear: $thisDayOfYear, prevYear: $prevYear, prevDayOfYear: $prevDayOfYear")

        if(thisYear == prevYear && thisDayOfYear == prevDayOfYear)  {   //같은 날이면
            val minute = calendar.get(Calendar.MINUTE)
            val prevMinute = mPref.getInt(Common.POST_CALENDAR_MINUTE_KEY, 10)
            Log.e(Common.MY_TAG, "minute: $minute, prevMinute: $prevMinute")

            if(prevMinute == minute) {  //분까지 같으면
                Log.i(Common.MY_TAG, "이전에 post 한 시간과 같습니다.")
                return true
            }
        }
        return false
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun executeHttpPost(mPref: SharedPreferences) {
        //3.1.3. HTTP_POST
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
        savePostCalendarPreference()

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

    //jingyu_test
    /*
    public fun executeHttpPost(mPref: SharedPreferences, id: Long) {
        //Result Text View에 출력할 Output
        var result : String = ""

        // HttpUrlConnection
        val th = Thread {
            try {
                //3.1.3. HTTP_POST
                var page = mPref.getString("url_key", Common.DEFAULT_URL)
                if(page == "") {
                    page = Common.DEFAULT_URL
                }
                page += Common.URL_SLASH + "post"

                var bNeedRequestParam: Boolean = true
                // URL 객체 생성
                val url = URL(page)
                // 연결 객체 생성
                val httpConn: HttpURLConnection = url.openConnection() as HttpURLConnection

                // 결과값 저장 문자열
                val sb = StringBuilder()

                // 연결되면
                if (httpConn != null) {
                    Log.i(Common.MY_TAG, page + " connection succesfully")
                    result += "Connection successfully!" + "\n"
                    // 응답 타임아웃 설정
                    httpConn.setRequestProperty("Accept", "application/json")
                    httpConn.setRequestProperty("Content-type", "application/json; utf-8")
                    httpConn.setConnectTimeout(100000)
                    // POST 요청방식
                    httpConn.setRequestMethod("POST")

                    val data = FootPrinter(id = 1, time = "2022-03-01 07:01:52", latitude = 37.290000, longitude = 129.3500000)
                    val gson = Gson()
                    val jsonStr: String = gson.toJson(data)

                    val wr = OutputStreamWriter(httpConn.getOutputStream())
                    wr.write(jsonStr)
                    wr.flush()

                    // url에 접속 성공하면 (200)
                    var status : Int = try {
                        httpConn.responseCode
                    } catch (e: IOException) {
                        // HttpUrlConnection will throw an IOException if any 4XX
                        // response is sent. If we request the status again, this
                        // time the internal status will be properly set, and we'll be
                        // able to retrieve it.
                        Log.e(Common.MY_TAG, "URL responseCode error :$e")
                        httpConn.responseCode
                    }
                    result += "Respose code:" + status + "\n"
                    Log.e(Common.MY_TAG, "URL responseCode code: $status")

                    if(status == HttpURLConnection.HTTP_OK) {
                        val br = BufferedReader(
                            InputStreamReader(
                                httpConn.inputStream, "utf-8"
                            )
                        )
                        var line: String?

                        while (br.readLine().also { line = it } != null) {
                            sb.append(line)
                        }
                        result += sb.toString()
                        // 버퍼리더 종료
                        br.close()
                    }
                    // 연결 끊기
                    httpConn.disconnect()
                }
            } catch (e: Exception) {
                Log.e(Common.MY_TAG, "HttpURLConnection error :$e")
                result += "HttpURLConnection error! $e"
            } finally {
                Log.i(Common.MY_TAG, "result: "+result)
                //binding.tvResult.setText(result)
            }
        }
        th.start()
    }*/

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
    private fun savePostCalendarPreference() {
        val mPref = PreferenceManager.getDefaultSharedPreferences(mContext)
        val calendar = Calendar.getInstance(TimeZone.getTimeZone(Common.ASIA_TIME_ZONE))
        val year = calendar.get(Calendar.YEAR)
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        val minutes = calendar.get(Calendar.MINUTE)

        Log.i(Common.MY_TAG, "savePostCalendarPreference:: year:$year, dayOfYear:$dayOfYear, min:$minutes")

        with (mPref.edit()) {
            putInt(Common.POST_CALENDAR_YEAR_KEY, year)
            putInt(Common.POST_CALENDAR_DAY_OF_YEAR_KEY, dayOfYear)
            putInt(Common.POST_CALENDAR_MINUTE_KEY, minutes)
            apply()
        }
    }
}
