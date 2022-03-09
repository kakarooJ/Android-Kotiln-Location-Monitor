package com.kakaroo.footprinterservice

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.content.SharedPreferences
import java.util.*
import android.os.Build

import android.widget.Toast
import androidx.core.app.ActivityCompat

import android.content.pm.PackageManager
import android.location.LocationManager
import android.content.Context
import androidx.annotation.RequiresApi
import android.content.DialogInterface
import android.provider.Settings
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog

import androidx.preference.*
import com.google.gson.Gson
import com.kakaroo.footprinterservice.entity.FootPrinter
import com.kakaroo.footprinterservice.service.IRetrofitNetworkService
import com.kakaroo.footprinterservice.service.MyAlarmService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.lang.Exception
import java.lang.StringBuilder
import java.net.HttpURLConnection
import java.net.URL
import java.io.OutputStreamWriter


class MainActivity : AppCompatActivity() {

    lateinit var mPref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        //UI 실행화면이므로
        if (!checkLocationServicesStatus()) {
            showDialogForLocationServiceSetting();
        }else {
            checkRunTimePermission();
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.getItemId()) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun checkLocationServicesStatus(): Boolean {    //false 이면 리턴
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
    }

    fun checkRunTimePermission() {
        if( (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED)
            || (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED)
            || (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) ){
            ActivityCompat.requestPermissions(this as Activity, Common.MAP_PERMISSIONS, Common.REQ_CODE_MY_PERMISSION_LOCATION_ACCESS_ALL)
        }
    }

    //여기부터는 GPS 활성화를 위한 메소드들
    private fun showDialogForLocationServiceSetting() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this@MainActivity)
        builder.setTitle("위치 서비스 비활성화")
        builder.setMessage(
            """
            앱을 사용하기 위해서는 위치 서비스가 필요합니다.
            위치 설정을 수정하시겠습니까?
            """.trimIndent()
        )
        builder.setCancelable(true)
        builder.setPositiveButton("설정", DialogInterface.OnClickListener { dialog, id ->
            val callGPSSettingIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivityForResult(callGPSSettingIntent, Common.REQ_CODE_GPS_ENABLE)
        })
        builder.setNegativeButton("취소",
            DialogInterface.OnClickListener { dialog, id -> dialog.cancel() })
        builder.create().show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            Common.REQ_CODE_GPS_ENABLE ->
                //사용자가 GPS 활성 시켰는지 검사
                if (checkLocationServicesStatus()) {
                    if (checkLocationServicesStatus()) {
                        Log.d(Common.MY_TAG, "onActivityResult : GPS 활성화 되있음")
                        checkRunTimePermission()
                        return
                    }
                }

            Common.REQ_CODE_MY_PERMISSION_LOCATION_ACCESS_ALL ->
                {}
        }
    }


    class SettingsFragment : PreferenceFragmentCompat() {
        @RequiresApi(Build.VERSION_CODES.M)
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            //SharedPreference객체를 참조하여 설정상태에 대한 제어 가능.
            (activity as MainActivity).mPref = PreferenceManager.getDefaultSharedPreferences(activity as MainActivity);

            //MultiSelectListPreference에 Summary 출력위한 함수 호출
            val timeExDayPref: MultiSelectListPreference? = findPreference("timeExDayPref_key")

            timeExDayPref?.useSelectionAsSummary()

            timeExDayPref?.setOnPreferenceChangeListener { _, newValue ->
                val newValueSet = newValue as? HashSet<*>
                    ?: return@setOnPreferenceChangeListener true

                if(newValueSet.size == 0) {
                    timeExDayPref?.summary = getString(R.string.service_except_no_day_gap)
                } else {
                    var str: String = ""
                    for (value in newValueSet) {
                        if (str.length != 0) {
                            str += ", "
                        }
                        val i = timeExDayPref?.findIndexOfValue(value.toString())
                        str += timeExDayPref?.entries[i]
                    }
                    timeExDayPref?.summary = str
                }

                /*if (timeExDayPref?.summary.isEmpty()) {
                    timeExDayPref?.summary = getString(R.string.service_except_no_day_gap)
                }*/
                //timeExDayPref?.summary = newValueSet.joinToString(", ")
                true
            }

            /*val locationPref: PreferenceScreen? = findPreference("location_key")
            locationPref?.setOnPreferenceClickListener({
                //condition: Map Permission 체크
                val intent = Intent(activity, MapsActivity::class.java)
                startActivityForResult(intent, Common.CALL_MAPS_ACTIVITY)
                true
            })*/

            val serviceStartPref: SwitchPreferenceCompat? = findPreference("service_start_key")

            //카테고리 초기값(enable/disable)설정
            serviceStartPref?.isChecked()?.let { setPreferenceEnableDisable(!it) }
            
            serviceStartPref?.setOnPreferenceClickListener({
                val myAlarmService = MyAlarmService((activity as MainActivity).applicationContext, (activity as MainActivity).mPref)
                if(serviceStartPref?.isChecked) {
                    //(activity as MainActivity).startService(Common.ALARM_REPEAT_MODE)
                    myAlarmService.startService(Common.ALARM_REPEAT_MODE)
                    setPreferenceEnableDisable(false)
                } else {
                    //(activity as MainActivity).stopService()
                    myAlarmService.stopService()
                    setPreferenceEnableDisable(true)
                }

                true
            })

            val urlKeyPref: EditTextPreference? = findPreference("url_key")
            urlKeyPref?.setOnPreferenceChangeListener{ preference, newValue ->
                if(newValue == "") {
                    val pref = PreferenceManager.getDefaultSharedPreferences(this.context)
                    pref.edit().putString("url_key", Common.DEFAULT_URL).apply()
                    Log.i(Common.MY_TAG, "Default URL로 채워집니다")
                }
                true
            }
        }

        fun setPreferenceEnableDisable(enabled: Boolean) {
            val serviceStartCategoryPref: PreferenceCategory? = findPreference("serviceStartCategory_Key")
            if (serviceStartCategoryPref != null) {
                serviceStartCategoryPref.isEnabled = enabled
            }
            val serviceStopCategoryPref: PreferenceCategory? = findPreference("serviceStopCategory_Key")
            if (serviceStopCategoryPref != null) {
                serviceStopCategoryPref.isEnabled = enabled
            }
            val locationCategoryPref: PreferenceCategory? = findPreference("locationCategory_Key")
            if (locationCategoryPref != null) {
                locationCategoryPref.isEnabled = enabled
            }
            val urlKeyPref: EditTextPreference? = findPreference("url_key")
            if (urlKeyPref != null) {
                urlKeyPref.isEnabled = enabled
            }
        }

        override fun onDisplayPreferenceDialog(preference: Preference?) {
            if(preference is TimepickerPreference) {
                val timepickerdialog = TimePickerPreferenceDialog.newInstance(preference.key)
                timepickerdialog.setTargetFragment(this, 0)
                timepickerdialog.show(parentFragmentManager, "TimePickerDialog")
            }
            else {
                super.onDisplayPreferenceDialog(preference)
            }
        }

        //MultiSelectListPreference에 Summary 출력
        fun MultiSelectListPreference.useSelectionAsSummary() {
            var summary = ""
            for (value in this.values) {
                if (summary.isNotEmpty()) {
                    summary += ", "
                }
                val i = this.findIndexOfValue(value.toString())
                summary += this.entries[i]
            }
            if (summary.isEmpty()) {
                summary = getString(R.string.service_except_no_day_gap)
            }
            this.summary = summary
        }
    }
}


