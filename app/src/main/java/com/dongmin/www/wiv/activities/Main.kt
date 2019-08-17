package com.dongmin.www.wiv.activities

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.View
import android.widget.Toast
import com.dongmin.www.wiv.R
import com.dongmin.www.wiv.activities.Init.StaticData.sf
import com.dongmin.www.wiv.adapters.SubjectListAdapter
import com.dongmin.www.wiv.adapters.WatchingSubjectListAdapter
import com.dongmin.www.wiv.elements.SubjectElement
import com.dongmin.www.wiv.elements.WatchingSubjectElement
import com.dongmin.www.wiv.libraries.HttpConnector
import com.dongmin.www.wiv.libraries.UIModifyAvailableListener
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONException
import org.json.JSONObject

class Main : AppCompatActivity(), View.OnClickListener, WatchingSubjectListAdapter.Callback {

    private val REQUEST_CODE_MAIN = 1
    private val RESULT_CODE_UI = 11
    private val REQUEST_CODE_ENROLL = 2
    private val RESULT_CODE_FINISH = 9999

    private lateinit var noticeIntent : Intent

    private lateinit var watchingSubjectsListAdapter : RecyclerView.Adapter<WatchingSubjectListAdapter.ViewHolder>
    private lateinit var watchingSubjectsListLayoutManager : RecyclerView.LayoutManager

    override fun onCreate(savedInstanceState : Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sf = getSharedPreferences("app_info", MODE_PRIVATE)

        //Log.d("유저번호", sf.getString("no", "0"))

        //왓칭 과목 어댑터, 매니저 설정
        watchingSubjectsListAdapter = WatchingSubjectListAdapter(this)
        watchingSubjectsListLayoutManager = LinearLayoutManager(this)
        watchingSubjectsList.adapter = watchingSubjectsListAdapter
        watchingSubjectsList.layoutManager = watchingSubjectsListLayoutManager

        //어댑터 콜백 설정
        (watchingSubjectsListAdapter as WatchingSubjectListAdapter).setCallbackToMain(this)

        //알림받는 과목이 있는지 확인한 후 표시
        if(sf.getString("watching_subject", "00000-00") == "00000-00") {
            lblNoWatchingSubject.visibility = View.VISIBLE
            btnEnrollChange.text = resources.getString(R.string.activity_main_btn_enroll)
        } else {
            lblNoWatchingSubject.visibility = View.GONE
            updateWatchingSubjectsList()
            btnEnrollChange.text = resources.getString(R.string.activity_main_btn_change)
        }

        //설정 버튼 동작
        btnSettings.setOnClickListener(this)

        //터치 무효화창 초기 설정
        touchInvalidator.visibility = View.GONE
        touchInvalidator.setOnTouchListener { _, _ ->
            return@setOnTouchListener true
        }

        //광고 로드
        val adRequest = AdRequest.Builder().build()
        adBottom.loadAd(adRequest)
        adBottom.adListener = object : AdListener() {
            override fun onAdLoaded() {
                Log.d("Ad", "광고 뜸")
            }

            override fun onAdFailedToLoad(errorCode : Int) {
                Log.d("Ad", "광고 띄우기 실패, ErrorCode:$errorCode")
            }

            override fun onAdOpened() {
                Log.d("Ad", "광고 열림")
            }

            override fun onAdClicked() {
                Log.d("Ad", "광고 클릭됨")
            }

            override fun onAdLeftApplication() {
                Log.d("Ad", "광고 나감")
            }

            override fun onAdClosed() {
                Log.d("Ad", "광고 닫음")
            }

        }

    }

    override fun onResume() {
        super.onResume()
        //Log.d("onResumeCalled-Main", "onResumeCalled")

        //공지에 대한 인텐트 인스턴스를 생성한다
        noticeIntent = Intent(this@Main, Notice::class.java)

        //최근 공지 하나를 서버에서 받아온다
        HttpConnector("fetch_notice.php",
            "secCode=onlythiswivappcancallthisfetchnoticephpfile!", object : UIModifyAvailableListener(applicationContext) {
                override fun taskCompleted(result: String?) {
                    super.taskCompleted(result)
                    if(result!!.contains("NETWORK_CONNECTION")) return
                    try {
                        val jsonObject = JSONObject(result).getJSONObject("notice_info")
                        lblNotice.text = jsonObject.getString("title")

                        //공지 제목, 내용을 인텐트에 저장
                        noticeIntent.putExtra("notice_title", jsonObject.getString("title"))
                        noticeIntent.putExtra("notice_content", jsonObject.getString("content"))

                        //공지 내용이 있고 나서야 공지가 클릭이 된다
                        lNotice.setOnClickListener(this@Main)
                    } catch(e : JSONException) {
                        Log.e("WIV", "공지사항 불러오기 실패")
                    }
                }
        }).execute()

        //메인 화면에 나올 때마다 인증 체크를 한다
        HttpConnector("user_auth_check.php",
            "secCode=onlythiswivappcancallthisuserauthcheckphpfile!&email=${sf.getString("email", "")}&token=${sf.getString("token", "")}",
            object : UIModifyAvailableListener(applicationContext) {
                override fun taskCompleted(result: String?) {
                    super.taskCompleted(result)
                    if(result!!.contains("NETWORK_CONNECTION")) return
                    when(result) {

                        //인증시 통과
                        "AUTHORIZED" -> {
                            //버튼 활성화
                            btnEnrollChange.setOnClickListener(this@Main)
                            lblUnauth.visibility = View.GONE
                            touchInvalidator.visibility = View.GONE
                        }

                        //인증받지 못할 경우
                        "UNAUTHORIZED" -> {
                            //인증 만료 메시지 표시
                            lblUnauth.visibility = View.VISIBLE

                            //터치 무효화 가동
                            touchInvalidator.visibility = View.VISIBLE

                            //버튼 비활성화
                            btnEnrollChange.setOnClickListener(null)

                            //자동 접속 해제
                            val sfEditor = sf.edit()
                            sfEditor.putBoolean("is_login", false)
                            sfEditor.putString("watching_subject", "00000-00")
                            sfEditor.putString("watching_subject_name", "")
                            sfEditor.apply()
                        }
                    }
                }
            }).execute()
    }

    //왓칭과목의 리스트를 업데이트하는 메서드. 초기접속 또는 Enroll 에서 과목을 등록할 때 호출.
    private fun updateWatchingSubjectsList() {
        (watchingSubjectsListAdapter as WatchingSubjectListAdapter).watchingSubjectsList.clear()

        //sharedPreferences 에서 불러온다
        val watchingSubjects = sf.getString("watching_subject", "00000-00")!!.split(";")
        val watchingSubjectsName = sf.getString("watching_subject_name", "")!!.split(";")

        //TODO(출시시 해당 로그 제거)
        //Log.d("watchingSubjects", sf.getString("watching_subject", "00000-00"))
        //Log.d("watchingSubjectsName", sf.getString("watching_subject_name", ""))

        for(i : Int in 0..(watchingSubjects.size - 1)) {
            (watchingSubjectsListAdapter as WatchingSubjectListAdapter).watchingSubjectsList.add(WatchingSubjectElement(watchingSubjectsName[i], watchingSubjects[i]))
        }

        Log.d("LENGTH", (watchingSubjectsListAdapter as WatchingSubjectListAdapter).watchingSubjectsList.size.toString())

        watchingSubjectsListAdapter.notifyDataSetChanged()
    }

    override fun onNoWatchingSubject() {
        lblNoWatchingSubject.visibility = View.VISIBLE
    }


    override fun onClick(v: View?) {
        when(v!!.id) {
            lNotice.id -> {
                startActivity(noticeIntent)
            }

            btnEnrollChange.id -> {
                startActivityForResult(Intent(this@Main, Enroll::class.java), REQUEST_CODE_ENROLL)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when(requestCode) {
            REQUEST_CODE_ENROLL -> {
                when(resultCode) {
                    RESULT_CODE_UI -> {
                        lblNoWatchingSubject.visibility = View.GONE
                        updateWatchingSubjectsList()
                        btnEnrollChange.text = resources.getString(R.string.activity_main_btn_change)
                    }
                    RESULT_CODE_FINISH -> {
                        finish()
                    }
                }
            }
        }
    }

}