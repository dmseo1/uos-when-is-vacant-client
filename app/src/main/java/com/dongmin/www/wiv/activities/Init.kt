package com.dongmin.www.wiv.activities

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.dongmin.www.wiv.R
import com.dongmin.www.wiv.elements.SubDepartment
import com.dongmin.www.wiv.libraries.HttpConnector
import com.dongmin.www.wiv.libraries.UIModifyAvailableListener
import org.json.JSONObject


class Init : AppCompatActivity() {


    //전역 변수 선언
    companion object StaticData {
        var NUM_MAX_WATCHING_SUBJECTS = 2
        const val basicURL = "http://dak2183242.cafe24.com/wiv/"
        var subDepartments = ArrayList<SubDepartment>()
        var departments = ArrayList<String>()
        lateinit var sf : SharedPreferences
    }


    override fun onCreate(savedInstanceState : Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_init)
       // progressBar.indeterminateDrawable.setColorFilter(Color.parseColor("#6583A7"), PorterDuff.Mode.SRC_IN)

        //인터넷 연결 확인
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if(connectivityManager.activeNetworkInfo == null || !connectivityManager.activeNetworkInfo.isConnected) {
            Toast.makeText(applicationContext, "인터넷에 연결 후 다시 시도해주십시오.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }


        //TODO: 백그라운드 제한 확인

        //유저 데이터 받아오기
        sf = getSharedPreferences("app_info", MODE_PRIVATE)

        //채널 등록(알림을 받기 위한 채널)
        createNotificationChannel()

        //버전 체크
        HttpConnector("fetch_app_variables.php", "secCode=onlythiswivappcancallthisfetchappvariablesphpfile!", object : UIModifyAvailableListener(applicationContext) {
            override fun taskCompleted(result: String?) {
                super.taskCompleted(result)
                if(result == "NETWORK_CONNECTION_FAILED") return
                try {
                    val jsonObject = JSONObject(result).getJSONObject("app_variables")
                    val minVersionCode = jsonObject.getString("min_version_code").toLong()
                    NUM_MAX_WATCHING_SUBJECTS = jsonObject.getString("num_max_watching_subjects").toInt()

                    when(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        true -> {
                            if(minVersionCode > packageManager.getPackageInfo(packageName, 0).longVersionCode) {
                                Toast.makeText(applicationContext, "어플리케이션을 업데이트해주세요.", Toast.LENGTH_SHORT).show()
                                finish()
                            return
                        }
                    }
                    false -> {
                        if(minVersionCode > packageManager.getPackageInfo(packageName, 0).versionCode) {
                            Toast.makeText(applicationContext, "어플리케이션을 업데이트해주세요.", Toast.LENGTH_SHORT).show()
                            finish()
                            return
                        }
                        }
                    }
                } catch(e : Exception) {
                    e.printStackTrace()
                    Toast.makeText(applicationContext, "예기치 않은 오류가 발생하였습니다.", Toast.LENGTH_SHORT).show()
                    finish()
                    return
                }


                //학과 정보 가져온 후 로그인 상태 체크, 이후 로그인 페이지 또는 메인 페이지를 띄운다
                HttpConnector("fetch_dept_info.php", "secCode=onlythiswivappcancallthisfetchdeptinfophpfile!", object : UIModifyAvailableListener(applicationContext) {
                    override fun taskCompleted(result: String?) {
                        super.taskCompleted(result)
                        if(result == "NETWORK_CONNECTION_FAILED") return
                        subDepartments.clear()
                        departments.clear()
                        for(i : Int in 0..100) {
                            try {
                                val jsonString = JSONObject(result).getJSONArray("dept_info").getJSONObject(i).toString()
                                val subDepartment = SubDepartment().fillFromJSON(jsonString)
                            subDepartments.add(subDepartment)
                            departments.add(subDepartment.deptName)
                        } catch(e : Exception) {
                            break
                        }
                    }

                    subDepartments.sortBy {
                        fun selector(sd : SubDepartment) : Int = sd.no
                            selector(it)}

                        departments = departments.distinct() as ArrayList<String>

                        if(sf.getBoolean("is_login", false)) {

                            //Log.d("FetchedInfo", "${userInfo.userNo}/${userInfo.userEmail}/${userInfo.userToken}")
                            startActivity(Intent(this@Init, Main::class.java))
                        } else {
                            startActivity(Intent(this@Init, Login::class.java))
                        }
                        finish()
                    }
                }).execute()    //확인 후 로그인
            }
        }).execute()    //버전체크
    }



    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_name)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(getString(R.string.channel_name), name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}