package com.dongmin.www.wiv.services

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import com.dongmin.www.wiv.R
import com.dongmin.www.wiv.libraries.HttpConnector
import com.dongmin.www.wiv.libraries.UIModifyAvailableListener
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import android.os.PowerManager
import android.content.Context.POWER_SERVICE
import android.support.v4.content.ContextCompat.getSystemService



class WivMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage?) {
        super.onMessageReceived(remoteMessage)

        val sf = getSharedPreferences("app_info", MODE_PRIVATE)


       // Log.d("메시지 도착", "메시지 도착!!!")

        when(remoteMessage!!.data["topic"]) {

            "wiv_notice" -> {
                sendNotification(remoteMessage)
            }

            else -> {
                //로그인 상태가 아니면 알림을 받지 못한다
                if(!sf.getBoolean("is_login", false)) return
                //왓칭 중인 과목인지 최종 확인
                val userWatchingSubjects = sf.getString("watching_subject", "00000-00")!!.split(";")
                for(i : Int in 0..(userWatchingSubjects.size - 1)) {
                    if(remoteMessage.data["topic"] == userWatchingSubjects[i]) {

                        //인증 최종 확인
                        HttpConnector("user_auth_check.php",
                            "secCode=onlythiswivappcancallthisuserauthcheckphpfile!&email=${sf.getString("email", "")}&token=${sf.getString("token","")}",
                            object : UIModifyAvailableListener(applicationContext) {
                                override fun taskCompleted(result: String?) {
                                    if(result == "NETWORK_CONNECTION_FAILED") return
                                    when(result!!) {
                                        "AUTHORIZED" -> {
                                            //최종 인증을 통과한 기기에 대해서만 알림을 보낸다
                                            sendNotification(remoteMessage)
                                        }
                                        "UNAUTHORIZED" -> {
                                            Log.e("WIV", "미인증 기기")
                                        }
                                    }
                                }
                            }).execute()
                        break
                    }
                }
            }
        }


    }

    override fun onNewToken(token : String?) {
        //Log.d("onNewToken 와", token)
    }

    private fun sendNotification(remoteMessage : RemoteMessage) {

        val builder = when(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            true -> {
                NotificationCompat.Builder(this@WivMessagingService, "wiv_channel")
                    .setSmallIcon(R.drawable.wivlogo)
                    .setContentTitle(remoteMessage.data["title"])
                    .setContentText(remoteMessage.data["body"])
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setLargeIcon((ContextCompat.getDrawable(applicationContext, R.drawable.wivlogo2) as BitmapDrawable).bitmap)
                    //.setLargeIcon((resources.getDrawable(R.drawable.wivlogo) as BitmapDrawable).bitmap)
                    .setPriority(NotificationManager.IMPORTANCE_HIGH)
                    .setAutoCancel(true)
            }

            false -> {
                NotificationCompat.Builder(this@WivMessagingService, "wiv_channel")
                    .setSmallIcon(R.drawable.wivlogo)
                    .setContentTitle(remoteMessage.data["title"])
                    .setContentText(remoteMessage.data["body"])
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setLargeIcon((ContextCompat.getDrawable(applicationContext, R.drawable.wivlogo2) as BitmapDrawable).bitmap)
                    //.setLargeIcon((applicationContext.getDrawable(R.drawable.wivlogo) as BitmapDrawable).bitmap)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
            }
        }

        with(NotificationManagerCompat.from(this@WivMessagingService)) {
            // notificationId is a unique int for each notification that you must define
            notify("${remoteMessage.data["topic"]!!.split("-")[0]}${remoteMessage.data["topic"]!!.split("-")[1]}".toInt(), builder.build())
        }

        val pm = applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        val isScreenOn = if (Build.VERSION.SDK_INT >= 20) pm.isInteractive else pm.isScreenOn // check if screen is on
        if (!isScreenOn) {
            val wl = pm.newWakeLock(
                PowerManager.SCREEN_DIM_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "WIV:notificationLock"
            )
            wl.acquire(5000) //set your time in milliseconds
        }
    }
}