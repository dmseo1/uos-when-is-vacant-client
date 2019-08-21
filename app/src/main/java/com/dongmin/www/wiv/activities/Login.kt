package com.dongmin.www.wiv.activities

import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import com.dongmin.www.wiv.R
import com.dongmin.www.wiv.libraries.HttpConnector
import com.dongmin.www.wiv.libraries.UIModifyAvailableListener
import kotlinx.android.synthetic.main.activity_login.*
import android.content.Context
import android.view.inputmethod.InputMethodManager


class Login : AppCompatActivity(), View.OnClickListener{

    private val emailRegex = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$".toRegex()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        //프로그레스 바 색 변경
        pgBar.indeterminateDrawable.setColorFilter(Color.parseColor("#05264D"), PorterDuff.Mode.SRC_IN)
        pgBar.visibility = View.GONE
        opaWindow.visibility = View.GONE

        //버튼 리스너 연결
        btnNext.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when(v!!.id) {
            btnNext.id -> {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(txtEmail.windowToken, 0)

                if(txtEmail.text.toString().isEmpty()) {
                    Toast.makeText(applicationContext, "이메일을 입력하세요.", Toast.LENGTH_SHORT).show()
                } else if(!emailRegex.matches(txtEmail.text.toString())) {
                    Toast.makeText(applicationContext, "올바른 이메일 형식이 아닙니다.", Toast.LENGTH_SHORT).show()
                } else if(!txtEmail.text.toString().contains("@uos.ac.kr")) {
                    Toast.makeText(applicationContext, "다계정 방지를 위해 서울시립대학교 웹메일만 허용합니다.", Toast.LENGTH_SHORT).show()
                } else if(txtEmail.text.toString().length > 35) {
                    Toast.makeText(applicationContext, "이메일 길이가 지나치게 깁니다. 실제로 본인 이메일이 맞다면 관리자에게 문의하세요.", Toast.LENGTH_SHORT).show()
                } else {
                    //메일 전송하기
                    pgBar.visibility = View.VISIBLE
                    opaWindow.visibility = View.VISIBLE
                    btnNext.setOnClickListener(null)
                    HttpConnector("send_auth_code.php", "secCode=onlythiswivappcancallthissendauthcodephpfile!&email=${txtEmail.text}", object : UIModifyAvailableListener(applicationContext) {
                        override fun taskCompleted(result: String?) {
                            super.taskCompleted(result)
                            pgBar.visibility = View.GONE
                            opaWindow.visibility = View.GONE
                            if(result!!.contains("NETWORK_CONNECTION")) {
                                btnNext.setOnClickListener(this@Login)
                                return
                            }
                            //단, 어뷰징 적발된 이메일로 가입 시도하는 경우 막는다
                            if(result == "DENIED_BY_ABUSING") {
                                Toast.makeText(applicationContext, "어뷰징 행위로 인해 인증이 제한된 이메일입니다.", Toast.LENGTH_SHORT).show()
                                btnNext.setOnClickListener(this@Login)
                                return
                            }
                            startActivity(Intent(this@Login, Join::class.java).putExtra("email", txtEmail.text.toString()))
                            finish()
                        }
                    }).execute()
                }
            }
        }
    }
}