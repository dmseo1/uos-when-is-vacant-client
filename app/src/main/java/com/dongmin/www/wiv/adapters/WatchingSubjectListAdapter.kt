package com.dongmin.www.wiv.adapters

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.dongmin.www.wiv.R
import com.dongmin.www.wiv.activities.Init.StaticData.sf
import com.dongmin.www.wiv.elements.WatchingSubjectElement
import com.dongmin.www.wiv.libraries.HttpConnector
import com.dongmin.www.wiv.libraries.UIModifyAvailableListener
import com.google.firebase.messaging.FirebaseMessaging

class WatchingSubjectListAdapter(context : Context) : RecyclerView.Adapter<WatchingSubjectListAdapter.ViewHolder>() {

    var watchingSubjectsList = ArrayList<WatchingSubjectElement>()
    private var context : Context? = context

    //하나의 뷰에는 어떤 정보들이 들어갈까요?
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // each data item is just a string in this case

        var subjectName : TextView = itemView.findViewById(R.id.subjectName)
        var subjectNoDiv : TextView = itemView.findViewById(R.id.subjectNoDiv)
        var btnDelete : Button = itemView.findViewById(R.id.btnDelete)
    }

    override fun getItemViewType(position: Int): Int {
        return 1
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WatchingSubjectListAdapter.ViewHolder {
        // create a new view
        return when(viewType) {
            1 -> WatchingSubjectListAdapter.ViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.element_watching_subject, parent, false))
            else -> WatchingSubjectListAdapter.ViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.element_watching_subject, parent, false))
        }
    }


    override fun onBindViewHolder(holder: WatchingSubjectListAdapter.ViewHolder, position: Int) {
        // - get element from your data set at this position
        // - replace the contents of the view with that element

        try {
            holder.subjectName.text = context!!.resources.getString(R.string.element_watching_subject_lbl_subject_name,
            watchingSubjectsList[position].subjectName)

            holder.subjectNoDiv.text = context!!.resources.getString(R.string.element_watching_subject_lbl_subject_no_div,
                watchingSubjectsList[position].subjectNoDiv)

            holder.btnDelete.setOnClickListener {

                callbackToMain!!.invalidateTouch(View.VISIBLE)

                //위치 고정
                val pos = holder.adapterPosition
                val subjectNoDiv = watchingSubjectsList[pos].subjectNoDiv

                //인증 상태 체크
                HttpConnector("user_auth_check.php",
                    "secCode=onlythiswivappcancallthisuserauthcheckphpfile!&email=${sf.getString("email", "")}&token=${sf.getString("token", "")}",
                    object: UIModifyAvailableListener(context!!) {
                        override fun taskCompleted(result: String?) {
                            super.taskCompleted(result)
                            if (result!!.contains("NETWORK_CONNECTION")) {
                                callbackToMain!!.invalidateTouch(View.GONE)
                                return
                            }
                            when (result) {
                                "AUTHORIZED" -> {
                                    //알림 구독 삭제
                                    FirebaseMessaging.getInstance().unsubscribeFromTopic(subjectNoDiv).addOnCompleteListener { task ->
                                        if(task.isSuccessful) {
                                            //sharedPreferences 갱신 자료 제작
                                            val watchingSubjects = sf.getString("watching_subject", "0000-00")!!.split(";")
                                            val watchingSubjectsName = sf.getString("watching_subject_name", "")!!.split(";")
                                            var remaining = 0
                                            var modifiedWatchingSubjects = ""
                                            var modifiedWatchingSubjectsName = ""
                                            for(i : Int in 0..(watchingSubjects.size - 1)) {
                                                if(subjectNoDiv == watchingSubjects[i]) {
                                                    continue
                                                }
                                                if(remaining == 0) {
                                                    modifiedWatchingSubjects += watchingSubjects[i]
                                                    modifiedWatchingSubjectsName += watchingSubjectsName[i]
                                                    remaining ++
                                                } else {
                                                    modifiedWatchingSubjects += ";${watchingSubjects[i]}"
                                                    modifiedWatchingSubjectsName += ";${watchingSubjectsName[i]}"
                                                }
                                            }
                                            if(remaining == 0) modifiedWatchingSubjects = "00000-00"

                                            //데이터베이스 변경
                                            HttpConnector("delete_watching_subject.php",
                                                "secCode=onlythiswivappcancallthisdeletewatchingsubjectfile!&email=${sf.getString("email", "")}&watchingSubjects=$modifiedWatchingSubjects",
                                                object : UIModifyAvailableListener(context!!) {
                                                    override fun taskCompleted(result: String?) {
                                                        super.taskCompleted(result)
                                                        if(result!!.contains("NETWORK_CONNECTION")) {
                                                            Toast.makeText(context, "인터넷 연결 불안정으로 인한 동기화 오류입니다. 정상적인 사용을 위해 재인증이 필요합니다.", Toast.LENGTH_LONG).show()
                                                            val sfEditor = sf.edit()
                                                            sfEditor.putBoolean("is_login", false)
                                                            sfEditor.putString("watching_subject", "00000-00")
                                                            sfEditor.putString("watching_subject_name", "")
                                                            sfEditor.apply()
                                                            callbackToMain!!.finishActivity()
                                                            return
                                                        }

                                                        //sf 수정
                                                        val sfEditor = sf.edit()
                                                        sfEditor.putString("watching_subject", modifiedWatchingSubjects)
                                                        sfEditor.putString("watching_subject_name", modifiedWatchingSubjectsName)
                                                        //남은 과목이 없는 경우, 00000-00 으로 채우기
                                                        //알림 과목 없음 메인화면 메시지 띄우기
                                                        if(remaining == 0) callbackToMain!!.onNoWatchingSubject()
                                                        sfEditor.apply()

                                                        //리스트에서 삭제
                                                        watchingSubjectsList.removeAt(pos)

                                                        //리스트 새로고침
                                                        notifyDataSetChanged()

                                                        //메시지 띄우기
                                                        Toast.makeText(context!!, "과목 알림이 해제되었습니다.", Toast.LENGTH_SHORT).show()
                                                      //  Log.e("왓칭 과목", sf.getString("watching_subject", "00000-00"))

                                                        callbackToMain!!.updateWatchingSubjectCnt()
                                                        callbackToMain!!.invalidateTouch(View.GONE)
                                                    }
                                                }).execute()
                                        } else {
                                            callbackToMain!!.invalidateTouch(View.GONE)

                                        }
                                    }
                                }

                                "UNAUTHORIZED" -> {
                                    callbackToMain!!.invalidateTouch(View.GONE)

                                    //자동 로그인 해제
                                    val sfEditor = sf.edit()
                                    sfEditor.putBoolean("is_login", false)
                                    sfEditor.putString("watching_subject", "00000-00")
                                    sfEditor.putString("watching_subject_name", "")
                                    sfEditor.apply()

                                    //토스트 메시지 표시 후 액티비티 종료
                                    Toast.makeText(context, "인증정보가 없습니다", Toast.LENGTH_SHORT).show()
                                    (context as AppCompatActivity).setResult(9999, null)
                                    callbackToMain!!.finishActivity()
                                }
                            }
                        }
                    }).execute()


            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private var callbackToMain : Callback? = null
    interface Callback {
        fun onNoWatchingSubject()
        fun invalidateTouch(visibility : Int)
        fun updateWatchingSubjectCnt()
        fun finishActivity()
    }

    fun setCallbackToMain(callback : Callback) {
        callbackToMain = callback
    }


    override fun getItemCount(): Int {
        return watchingSubjectsList.size
    }
}
