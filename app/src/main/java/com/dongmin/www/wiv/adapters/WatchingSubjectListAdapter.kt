package com.dongmin.www.wiv.adapters

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
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

                //위치 고정
                val pos = holder.adapterPosition

                //데이터베이스 변경
                HttpConnector("delete_watching_subject.php",
                    "secCode=onlythiswivappcancallthisdeletewatchingsubjectfile!&email=${sf.getString("email", "")}&deletingSubject=${watchingSubjectsList[pos].subjectNoDiv}",
                    object : UIModifyAvailableListener(context!!) {
                        override fun taskCompleted(result: String?) {
                            super.taskCompleted(result)
                            if(result == "NETWORK_CONNECTION_FAILED") return

                            //알림 구독 삭제
                            FirebaseMessaging.getInstance().unsubscribeFromTopic(watchingSubjectsList[pos].subjectNoDiv)

                            //메시지 띄우기
                            Toast.makeText(context!!, "과목 알림이 해제되었습니다.", Toast.LENGTH_SHORT).show()

                            //sharedPreferences 갱신
                            val watchingSubjects = sf.getString("watching_subject", "0000-00")!!.split(";")
                            val watchingSubjectsName = sf.getString("watching_subject_name", "")!!.split(";")
                            var remaining = 0
                            var modifiedWatchingSubjects = ""
                            var modifiedWatchingSubjectsName = ""
                            for(i : Int in 0..(watchingSubjects.size - 1)) {
                                if(watchingSubjectsList[pos].subjectNoDiv == watchingSubjects[i]) {
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



                            val sfEditor = sf.edit()
                            sfEditor.putString("watching_subject", modifiedWatchingSubjects)
                            sfEditor.putString("watching_subject_name", modifiedWatchingSubjectsName)
                            //남은 과목이 없는 경우, 00000-00 으로 채우기
                            if(remaining == 0) {
                                sfEditor.putString("watching_subject", "00000-00")

                                //알림 과목 없음 메인화면 메시지 띄우기
                                callbackToMain!!.onNoWatchingSubject()
                            }
                            sfEditor.apply()

                            //리스트에서 삭제
                            watchingSubjectsList.removeAt(pos)

                            //리스트 새로고침
                            notifyDataSetChanged()
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
    }
    fun setCallbackToMain(callback : Callback) {
        callbackToMain = callback
    }



    override fun getItemCount(): Int {
        return watchingSubjectsList.size
    }
}
