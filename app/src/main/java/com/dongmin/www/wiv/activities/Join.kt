package com.dongmin.www.wiv.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.dongmin.www.wiv.R
import com.dongmin.www.wiv.activities.Init.StaticData.sf
import com.dongmin.www.wiv.libraries.HttpConnector
import com.dongmin.www.wiv.libraries.UIModifyAvailableListener
import com.dongmin.www.wiv.popups.JoinAuthError
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.android.synthetic.main.activity_join.*

class Join : AppCompatActivity(), View.OnClickListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_join)
        onLoadingState(View.GONE)

        lblEmail.text = intent.getStringExtra("email")
    }

    private fun onLoadingState(visibility : Int) {
        opaWindow.visibility = visibility
        pgBar.visibility = visibility
        when(visibility) {
            View.VISIBLE -> {
                btnResend.setOnClickListener(null)
                btnCancel.setOnClickListener(null)
                btnConfirm.setOnClickListener(null)
            }
            View.GONE -> {
                btnResend.setOnClickListener(this@Join)
                btnCancel.setOnClickListener(this@Join)
                btnConfirm.setOnClickListener(this@Join)
            }
        }
    }

    override fun onClick(v: View?) {
        when(v!!.id) {
            btnResend.id -> {
                onLoadingState(View.VISIBLE)
                HttpConnector("send_auth_code.php", "secCode=onlythiswivappcancallthissendauthcodephpfile!&email=${intent.getStringExtra("email")}", object : UIModifyAvailableListener(applicationContext) {
                    override fun taskCompleted(result: String?) {
                        super.taskCompleted(result)
                        onLoadingState(View.GONE)
                        if(result == "NETWORK_CONNECTION_FAILED") return
                        Toast.makeText(applicationContext, "인증메일을 재전송하였습니다. 반드시 가장 최근 메일의 인증코드를 입력해주세요.", Toast.LENGTH_SHORT).show()

                    }
                }).execute()
            }

            btnCancel.id -> {
                finish()
            }

            btnConfirm.id -> {

                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(txtAuthCode.windowToken, 0)


                onLoadingState(View.VISIBLE)
                HttpConnector("verify_auth_code.php",
                    "secCode=onlythiswivappcancallthisverifyauthcodephpfile!&email=${intent.getStringExtra("email")}&key=${txtAuthCode.text}",
                    object : UIModifyAvailableListener(applicationContext) {
                        override fun taskCompleted(result: String?) {
                            super.taskCompleted(result)
                            if(result == "NETWORK_CONNECTION_FAILED") {
                                onLoadingState(View.GONE)
                                return
                            }
                            when(result) {
                                "VERIFIED" -> {
                                    //토큰 부여
                                    FirebaseMessaging.getInstance().isAutoInitEnabled = true
                                    FirebaseInstanceId.getInstance().instanceId
                                        .addOnCompleteListener(OnCompleteListener { task ->
                                            if (!task.isSuccessful) {   //부여 실패
                                                onLoadingState(View.GONE)
                                                Toast.makeText(applicationContext, "최종 인증 처리에 실패하였습니다. 인터넷 연결 상태를 확인해주세요.", Toast.LENGTH_SHORT).show()
                                                return@OnCompleteListener
                                            }

                                            // Get new Instance ID token
                                            val token = task.result?.token
                                            // Log and toast
                                            //Log.d("생성된 토큰", token)
                                            //Toast.makeText(baseContext, token, Toast.LENGTH_SHORT).show()

                                            //sharedPreference 갱신
                                            val sfEditor = sf.edit()
                                            sfEditor.putString("email", intent.getStringExtra("email"))
                                            sfEditor.putBoolean("is_login", true)
                                            sfEditor.putString("token", token)
                                            sfEditor.apply()


                                            HttpConnector("register_user.php",
                                                "secCode=onlythiswivappcancallthisregisteruserphpfile!&email=${intent.getStringExtra("email")}" +
                                                        "&token=$token",
                                                        object : UIModifyAvailableListener(applicationContext) {
                                                            override fun taskCompleted(result: String?) {
                                                                super.taskCompleted(result)
                                                                onLoadingState(View.GONE)
                                                                if(result == "NETWORK_CONNECTION_FAILED") return
                                                                when(result!!.contains("DUP")) {
                                                                    true -> {
                                                                        Toast.makeText(applicationContext, "인증이 완료되었습니다. 이전 기기 또는 어플리케이션에서의 인증은 해제됩니다.", Toast.LENGTH_SHORT).show()
                                                                    }
                                                                    false -> {
                                                                        Toast.makeText(applicationContext, "인증이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                                                                    }
                                                                }

                                                                sfEditor.putString("no", result.split(" ")[1])
                                                                sfEditor.apply()

                                                                //메시지 표시 후 화면 넘김
                                                                startActivity(Intent(this@Join, Main::class.java))

                                                                finish()

                                                            }
                                            }).execute()
                                        })
                                }
                                "UNVERIFIED" -> {
                                    onLoadingState(View.GONE)
                                    JoinAuthError(this@Join).start()
                                }
                            }
                        }
                }).execute()
            }
        }
    }

}