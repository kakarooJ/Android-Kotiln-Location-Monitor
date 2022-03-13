package com.kakaroo.footprinterservice

import android.app.Activity
import android.content.*
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import java.util.*
import android.os.Build

import androidx.core.app.ActivityCompat

import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.annotation.RequiresApi
import android.provider.Settings
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.preference.*
import com.kakaroo.footprinterservice.service.MyAlarmService
import com.google.android.material.snackbar.Snackbar
import com.kakaroo.footprinterservice.receiver.MyReceiver

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
            Log.e(Common.MY_TAG, "위치 설정이 필요합니다.")
            showDialogForLocationServiceSetting();
        }else {
            checkRunTimePermission();
        }


    }

    private fun startRestartService() {
        Log.i(Common.MY_TAG, "MainActivity startRestartService called")
        val intent = Intent(this, RestartService::class.java)
        intent.action = Common.ACTION_RESTART_SERVICE_FOREGROUND
        //val intentFilter = IntentFilter("com.kakaroo.footprinter.RestartService");
        //val myReceiver = MyReceiver()
        //registerReceiver(myReceiver, intentFilter);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
    private fun stopRestartService() {
        Log.i(Common.MY_TAG, "MainActivity stopRestartService called")
        val intent = Intent(this, RestartService::class.java)
        //val intentFilter = IntentFilter("com.kakaroo.footprinter.RestartService");
        stopService(intent)
    }

    override fun onResume() {
        super.onResume()
        /*
        if (!checkLocationServicesStatus()) {
            Log.e(Common.MY_TAG, "위치 설정이 필요합니다.")
            showDialogForLocationServiceSetting();
        }else {
            checkRunTimePermission();
        }*/
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(Common.MY_TAG, "MainActivity onDestroy called")
        //val myReceiver = MyReceiver()
        //unregisterReceiver(myReceiver)
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
            /*|| (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    != PackageManager.PERMISSION_GRANTED)*/ ){
            Log.e(Common.MY_TAG, "checkRunTimePermission:: 위치 권한이 필요합니다.")
            //Toast.makeText(this, "위치 권한 설정(항상허용)이 필요합니다!!", Toast.LENGTH_SHORT).show()
            // 3-1. 사용자가 퍼미션 거부를 한 적이 있는 경우에는

            // 3-1. 사용자가 퍼미션 거부를 한 적이 있는 경우에는
            /*if (ActivityCompat.shouldShowRequestPermissionRationale(this,Common.MAP_PERMISSIONS[0])
                || ActivityCompat.shouldShowRequestPermissionRationale(this,Common.MAP_PERMISSIONS[1]) ) {

                // 3-2. 요청을 진행하기 전에 사용자가에게 퍼미션이 필요한 이유를 설명해줄 필요가 있습니다.
                Snackbar.make(
                    findViewById(R.id.main_layout), "이 앱을 실행하려면 위치 항상 허용 권한이 필요합니다.",
                    Snackbar.LENGTH_INDEFINITE
                ).setAction("확인", object : View.OnClickListener {
                    override fun onClick(view: View?) {
                        // 3-3. 사용자게에 퍼미션 요청을 합니다. 요청 결과는 onRequestPermissionResult에서 수신됩니다.
                        ActivityCompat.requestPermissions(this as Activity, Common.MAP_PERMISSIONS, Common.REQ_CODE_MY_PERMISSION_LOCATION_ACCESS_ALL)
                    }
                }).show()
            } else {
                // 4-1. 사용자가 퍼미션 거부를 한 적이 없는 경우에는 퍼미션 요청을 바로 합니다.
                // 요청 결과는 onRequestPermissionResult에서 수신됩니다.
                ActivityCompat.requestPermissions(this, Common.MAP_PERMISSIONS, Common.REQ_CODE_MY_PERMISSION_LOCATION_ACCESS_ALL)
            }*/
            ActivityCompat.requestPermissions(this as Activity, Common.MAP_PERMISSIONS, Common.REQ_CODE_MY_PERMISSION_LOCATION_ACCESS_ALL)
        } else {
            Log.i(Common.MY_TAG, "checkRunTimePermission:: 위치 권한이 있습니다.")
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

            if(timeExDayPref?.values?.size == 7) {
                val serviceControlPref: SwitchPreferenceCompat? = findPreference("service_start_key")
                if (serviceControlPref != null) {
                    serviceControlPref.isEnabled = false
                }
            }

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
                val serviceControlPref: SwitchPreferenceCompat? = findPreference("service_start_key")
                if(newValueSet.size == 7) {
                    if (serviceControlPref != null) {
                        serviceControlPref.isEnabled = false
                    }
                } else {
                    if (serviceControlPref != null) {
                        serviceControlPref.isEnabled = true
                    }
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
                    myAlarmService.startAlarmService(Common.ALARM_REPEAT_MODE)
                    setPreferenceEnableDisable(false)
                    (activity as MainActivity).startRestartService()
                } else {
                    myAlarmService.stopAlarmService()
                    setPreferenceEnableDisable(true)
                    (activity as MainActivity).stopRestartService()
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


