package com.dongmin.www.wiv.adapters

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.dongmin.www.wiv.R
import com.dongmin.www.wiv.activities.Init.StaticData.NUM_MAX_WATCHING_SUBJECTS
import com.dongmin.www.wiv.activities.Init.StaticData.sf
import com.dongmin.www.wiv.elements.SubDepartment
import com.dongmin.www.wiv.elements.SubjectElement
import com.dongmin.www.wiv.libraries.HttpConnector
import com.dongmin.www.wiv.libraries.UIModifyAvailableListener
import com.google.firebase.messaging.FirebaseMessaging

class SubjectListAdapter(context : Context) : RecyclerView.Adapter<SubjectListAdapter.ViewHolder>() {

    var subjectList = ArrayList<SubjectElement>()
    private var context : Context? = context
    private lateinit var selectedSubDepartment : SubDepartment


    //하나의 뷰에는 어떤 정보들이 들어갈까요?
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // each data item is just a string in this case

        var subjectName : TextView = itemView.findViewById(R.id.subjectName)
        var subjectInfo1 : TextView = itemView.findViewById(R.id.subjectInfo1)
        var subjectInfo2 : TextView = itemView.findViewById(R.id.subjectInfo2)
        var subjectInfo3 : TextView = itemView.findViewById(R.id.subjectInfo3)
        var btnEnroll : TextView = itemView.findViewById(R.id.btnEnroll)
    }

    override fun getItemViewType(position: Int): Int {
        return subjectList[position].category
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubjectListAdapter.ViewHolder {
        // create a new view
        return when(viewType) {
            1 -> SubjectListAdapter.ViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.element_subject, parent, false))
            2 -> SubjectListAdapter.ViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.element_subject, parent, false))
            else -> SubjectListAdapter.ViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.element_subject, parent, false))
        }
    }


    override fun onBindViewHolder(holder: SubjectListAdapter.ViewHolder, position: Int) {
        // - get element from your data set at this position
        // - replace the contents of the view with that element

        try {

            holder.subjectName.text = context!!.resources.getString(R.string.element_subject_first_row,
                subjectList[position].subjectNm, subjectList[position].subjectNo, subjectList[position].classDiv)

            when(subjectList[position].category) {
                1 -> {
                    holder.subjectInfo1.text = context!!.resources.getString(R.string.element_subject_second_row_g,
                        subjectList[position].shyr, subjectList[position].subjectDiv, subjectList[position].subjectDiv2, subjectList[position].credit)
                }
                2 -> {
                    holder.subjectInfo1.text = context!!.resources.getString(R.string.element_subject_second_row_j,
                        subjectList[position].shyr, subjectList[position].subjectDiv, subjectList[position].credit)
                }
            }

            holder.subjectInfo2.text = context!!.resources.getString(R.string.element_subject_third_row,
                subjectList[position].profNm, subjectList[position].classNm)
            holder.subjectInfo3.text = context!!.resources.getString(R.string.element_subject_fourth_row,
                subjectList[position].tlsnCount, subjectList[position].tlsnLimitCount)

            holder.btnEnroll.setOnClickListener {

                //최대 등록 가능 개수 초과 확인
                if(sf.getString("watching_subject", "00000-00") != "00000-00" &&
                    sf.getString("watching_subject", "00000-00")!!.split(";").size >= NUM_MAX_WATCHING_SUBJECTS) {
                    Toast.makeText(context, "최대 알림 등록 개수를 초과하였습니다. 기존 등록 과목 삭제 후 이용하세요.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                //로딩 화면 띄우기
                callbackToEnroll!!.onLoadingState(View.VISIBLE)

                //어댑터 내부 변수 직접 참조로 인한 오류를 막기 위해 새 변수 지정
                val pos = holder.adapterPosition
                val watchingSubject = "${subjectList[pos].subjectNo}-${subjectList[pos].classDiv}"
                val watchingSubjectNo = subjectList[pos].subjectNo
                val watchingSubjectClassDiv = subjectList[pos].classDiv
                val watchingSubjectDiv = subjectList[pos].subjectDiv
                val watchingSubjectName = subjectList[pos].subjectNm

                //인증 상태 체크
                HttpConnector("user_auth_check.php",
                    "secCode=onlythiswivappcancallthisuserauthcheckphpfile!&email=${sf.getString("email", "")}&token=${sf.getString("token", "")}",
                    object: UIModifyAvailableListener(context!!) {
                        override fun taskCompleted(result: String?) {
                            super.taskCompleted(result)
                            if(result!!.contains("NETWORK_CONNECTION")) {
                                callbackToEnroll!!.onLoadingState(View.GONE)
                                return
                            }
                            when(result) {
                                "AUTHORIZED" -> {
                                    //이미 해당 유저가 알림을 받고 있는 과목인지 체크
                                    //if(sf.getString("watching_subject", "00000-00") == "${subjectList[pos].subjectNo}-${subjectList[pos].classDiv}") {
                                    //    Toast.makeText(context, "이미 알림을 받고 있는 과목입니다.", Toast.LENGTH_SHORT).show()
                                    //    return
                                    //}

                                    //기존 채널 삭제
                                    //FirebaseMessaging.getInstance().unsubscribeFromTopic(sf.getString("watching_subject", "00000-00"))

                                    //알림을 받을 채널 설정
                                    FirebaseMessaging.getInstance().subscribeToTopic(watchingSubject)
                                        .addOnCompleteListener { task ->

                                            callbackToEnroll!!.onLoadingState(View.GONE)
                                            if(task.isSuccessful) {
                                                //데이터베이스에 해당 정보 등록

                                                //전공의 경우 학부, 과 코드 번호를 전달하기 위해 조건변수 추가
                                                val addCond = when(watchingSubjectDiv.contains("전공")) {
                                                    true -> "&dept=${selectedSubDepartment.dept}&subDept=${selectedSubDepartment.subDept}"
                                                    false -> ""
                                                }

                                                //등록 정보 전송
                                                HttpConnector("enroll_watching_subject.php",
                                                    "secCode=onlythiswivappcancallthisenrollwatchingsubjectfile!&email=${sf.getString("email", "")}&subjectNo=$watchingSubjectNo&classDiv=$watchingSubjectClassDiv&subjectDiv=$watchingSubjectDiv&subjectName=$watchingSubjectName$addCond",
                                                    object : UIModifyAvailableListener(context!!) {
                                                        override fun taskCompleted(result: String?) {
                                                            super.taskCompleted(result)
                                                            if(result!!.contains("NETWORK_CONNECTION")) return

                                                            //sf 에 저장
                                                            val sfEditor = sf.edit()

                                                            val watchingSubjects = sf.getString("watching_subject", "00000-00")!!.split(";")
                                                            if(watchingSubjects[0] == "00000-00") { //기존 등록된 과목이 없는 경우
                                                                sfEditor.putString("watching_subject", watchingSubject)
                                                                sfEditor.putString("watching_subject_name", watchingSubjectName)
                                                            } else {    //기존 등록된 과목이 있는 경우
                                                                sfEditor.putString("watching_subject", "${sf.getString("watching_subject", "00000-00")};$watchingSubject")
                                                                sfEditor.putString("watching_subject_name", "${sf.getString("watching_subject_name", "")};$watchingSubjectName")
                                                            }

                                                            sfEditor.apply()


                                                            //메인 액티비티 변경을 위한 인텐트 result 전달
                                                            (context as AppCompatActivity).setResult(11, Intent().putExtra("subject_name", watchingSubjectName).putExtra("subject_no", watchingSubjectNo).putExtra("class_div", watchingSubjectClassDiv))

                                                            //토스트 메시지 표시
                                                            Toast.makeText(context, "과목 알림 설정이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                ).execute()
                                            } else {
                                                Toast.makeText(context, "등록에 실패하였습니다. 인터넷 연결 상태 확인 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                }

                                "UNAUTHORIZED" -> {

                                    callbackToEnroll!!.onLoadingState(View.GONE)

                                    //자동 로그인 해제
                                    val sfEditor = sf.edit()
                                    sfEditor.putBoolean("is_login", false)
                                    sfEditor.putString("watching_subject", "00000-00")
                                    sfEditor.putString("watching_subject_name", "")
                                    sfEditor.apply()

                                    //토스트 메시지 표시 후 액티비티 종료
                                    Toast.makeText(context, "인증정보가 없습니다", Toast.LENGTH_SHORT).show()
                                    (context as AppCompatActivity).setResult(9999, null)
                                    callbackToEnroll!!.finishActivity()
                                }
                            }
                        }
                    }).execute()
            }
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }
    }

    override fun getItemCount(): Int {
        return subjectList.size
    }

    //학부, 학과 코드를 전해주기 위해
    fun updateSubDepartment(subDepartment : SubDepartment) {
        this.selectedSubDepartment = subDepartment
    }

    private var callbackToEnroll : CallbackToEnroll? = null
    fun setCallbackToEnroll(callback : CallbackToEnroll) {
        callbackToEnroll = callback
    }
    interface CallbackToMain {
        fun modifyUIInfo(subjectElement : SubjectElement)
        fun finishActivity()
    }

    interface CallbackToEnroll {
        fun onLoadingState(visibility : Int)
        fun finishActivity()
    }

    //fun addItem(item : SubjectElement) {
    //    subjectList.add(item)
    //}
}
