package com.dongmin.www.wiv.activities

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import com.dongmin.www.wiv.R
import com.dongmin.www.wiv.activities.Init.StaticData.departments
import com.dongmin.www.wiv.activities.Init.StaticData.sf
import com.dongmin.www.wiv.activities.Init.StaticData.subDepartments
import com.dongmin.www.wiv.adapters.SubjectListAdapter
import com.dongmin.www.wiv.elements.SubjectElement
import com.dongmin.www.wiv.libraries.HttpConnector
import com.dongmin.www.wiv.libraries.HttpConnectorOthers
import com.dongmin.www.wiv.libraries.UIModifyAvailableListener
import kotlinx.android.synthetic.main.activity_enroll.*
import org.json.JSONObject

class Enroll : AppCompatActivity(), View.OnClickListener, SubjectListAdapter.CallbackToEnroll {

    private var selectedDeptStartNo : Int = 0
    private lateinit var subjectListAdapter : RecyclerView.Adapter<SubjectListAdapter.ViewHolder>
    private lateinit var subjectListLayoutManager : RecyclerView.LayoutManager

    //checkbox 가 조회 중간에 변해도 검색 조회 버튼을 누를 당시의 값을 유지하도록 하기 위한 변수들 (chk ~~ State)
    private var chkOnlyVacantSubjectState = false

    private var chkIncludeGsState = "o"
    private var chkIncludeGpState = "o"
    private var chkIncludeRoState = "o"
    private var chkIncludeGzState = "o"

    private var chkExcludeExperimentSubjectState = false
    private var chkExcludeEngineeringSubjectState = false

    override fun onCreate(savedInstanceState : Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enroll)

        sf = getSharedPreferences("app_info", MODE_PRIVATE)

        //대학 스피너
        departments = departments.distinct() as ArrayList<String>
        spnJeongongDept.adapter = ArrayAdapter(this@Enroll, R.layout.support_simple_spinner_dropdown_item, departments)
        spnJeongongDept.setSelection(0)

        //학부/과 스피너
        spnJeongongSubDept.adapter = ArrayAdapter(this, R.layout.support_simple_spinner_dropdown_item, fillSpnJeongongSubDept())
        spnJeongongSubDept.setSelection(0)

        //대학 스피너 선택 변화에 따른 학부/과 스피너 채우기
        spnJeongongDept.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                spnJeongongSubDept.adapter = ArrayAdapter(this@Enroll, R.layout.support_simple_spinner_dropdown_item, fillSpnJeongongSubDept())
                spnJeongongSubDept.setSelection(0)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }


        //sf 를 이용한 체크박스들 초기 설정
        if(sf.getBoolean("chk_only_vacant_subject_state", false)) {
            chkOnlyVacantSubject.isChecked = true
            lGyoyangPartition.visibility = View.VISIBLE
            if(sf.getBoolean("chk_include_gs", false)) {
                lGsPartition.visibility = View.VISIBLE
            } else {
                lGsPartition.visibility = View.GONE
            }
        } else {
            lGyoyangPartition.visibility = View.GONE
            lGsPartition.visibility = View.GONE
        }

        //교양 분류
        when(sf.getBoolean("chk_include_gs", true)) {
            true -> {
                chkIncludeGs.isChecked = true
            }
            false -> {
                chkIncludeGs.isChecked = false
            }
        }

        when(sf.getBoolean("chk_include_gp", true)) {
            true -> chkIncludeGp.isChecked = true
            false -> chkIncludeGp.isChecked = false
        }

        when(sf.getBoolean("chk_include_ro", true)) {
            true -> chkIncludeRo.isChecked = true
            false -> chkIncludeRo.isChecked = false
        }

        when(sf.getBoolean("chk_include_gz", true)) {
            true -> chkIncludeGz.isChecked = true
            false -> chkIncludeGz.isChecked = false
        }

        //교선 분류
        if(sf.getBoolean("chk_exclude_experiment_subject", false)) {
            chkExcludeExperimentSubject.isChecked = true
        }

        if(sf.getBoolean("chk_exclude_engineering_subject", false)) {
            chkExcludeEngineeringSubject.isChecked = true
        }



        //조회 버튼 리스너 연결
        btnSearch.setOnClickListener(this)

        //필터 보이기/숨기기 버튼 리스너 연결
        btnFilterHideShow.setOnClickListener(this)

        //교양 라디오버튼 리스너 연결
        rdGyoyang.setOnCheckedChangeListener {_, isChecked ->
            if(isChecked)  {
                lGyoyangFilter.visibility = View.VISIBLE
                lJeongongFilter.visibility = View.GONE
                if(chkOnlyVacantSubject.isChecked) {
                    lGyoyangPartition.visibility = View.VISIBLE
                    if(chkIncludeGs.isChecked) {
                        lGsPartition.visibility = View.VISIBLE
                    }
                }
            }
        }

        //전공 라디오버튼 리스너 연결
        rdJeongong.setOnCheckedChangeListener {_, isChecked ->
            if(isChecked) {
                lGyoyangFilter.visibility = View.GONE
                lJeongongFilter.visibility = View.VISIBLE
                lGyoyangPartition.visibility = View.GONE
                lGsPartition.visibility = View.GONE
            }
        }




        //비어있는 과목만 조회 체크박스 리스너 연결
        chkOnlyVacantSubject.setOnCheckedChangeListener { _, isChecked ->
            val sfEditor = sf.edit()
            if(isChecked) {
                sfEditor.putBoolean("chk_only_vacant_subject_state", true)
                if(rdGyoyang.isChecked) {
                    lGyoyangPartition.visibility = View.VISIBLE
                    if(chkIncludeGs.isChecked) {
                        lGsPartition.visibility = View.VISIBLE
                    }
                }
            } else {
                sfEditor.putBoolean("chk_only_vacant_subject_state", false)
                lGyoyangPartition.visibility = View.GONE
                lGsPartition.visibility = View.GONE
            }
            sfEditor.apply()
        }

        //교양선택 체크박스 리스너 연결
        chkIncludeGs.setOnCheckedChangeListener { _, isChecked ->
            val sfEditor = sf.edit()
            if(isChecked) {
                sfEditor.putBoolean("chk_include_gs", true)
                lGsPartition.visibility = View.VISIBLE
            } else {
                sfEditor.putBoolean("chk_include_gs", false)
                lGsPartition.visibility = View.GONE
            }
            sfEditor.apply()
        }

        //교양필수 체크박스 리스너 연결
        chkIncludeGp.setOnCheckedChangeListener { _, isChecked ->
            val sfEditor = sf.edit()
            if(isChecked) {
                sfEditor.putBoolean("chk_include_gp", true)
            } else {
                sfEditor.putBoolean("chk_include_gp", false)
            }
            sfEditor.apply()
        }

        //ROTC 체크박스 리스너 연결
        chkIncludeRo.setOnCheckedChangeListener { _, isChecked ->
            val sfEditor = sf.edit()
            if(isChecked) {
                sfEditor.putBoolean("chk_include_ro", true)
            } else {
                sfEditor.putBoolean("chk_include_ro", false)
            }
            sfEditor.apply()
        }

        //교직 체크박스 리스너 연결
        chkIncludeGz.setOnCheckedChangeListener { _, isChecked ->
            val sfEditor = sf.edit()
            if(isChecked) {
                sfEditor.putBoolean("chk_include_gz", true)
            } else {
                sfEditor.putBoolean("chk_include_gz", false)
            }
            sfEditor.apply()
        }



        //실험과목 조회 제외 체크박스 리스너 연결
        chkExcludeExperimentSubject.setOnCheckedChangeListener { _, isChecked ->
            val sfEditor = sf.edit()
            if(isChecked) {
                sfEditor.putBoolean("chk_exclude_experiment_subject", true)
            } else {
                sfEditor.putBoolean("chk_exclude_experiment_subject", false)
            }
            sfEditor.apply()
        }

        //공학소양 조회 제외 체크박스 리스너 설정
        chkExcludeEngineeringSubject.setOnCheckedChangeListener { _, isChecked ->
            val sfEditor = sf.edit()
            if(isChecked) {
                sfEditor.putBoolean("chk_exclude_engineering_subject", true)
            } else {
                sfEditor.putBoolean("chk_exclude_engineering_subject", false)
            }
            sfEditor.apply()
        }

        //결과 조회 어댑터 생성 및 연결
        subjectListAdapter = SubjectListAdapter(this)
        subjectListLayoutManager = LinearLayoutManager(this)
        listResult.adapter = subjectListAdapter
        listResult.layoutManager = subjectListLayoutManager

        //조회 결과 recycler view adapter 의 콜백 연결
        (subjectListAdapter as SubjectListAdapter).setCallbackToEnroll(this)

        //프로그레스 숨김 설정
        progressBar.visibility = View.GONE

        //과목 등록시 나타나는 opaWindow 에 터치 무효화 설정
        opaWindow.setOnTouchListener{ _, _ ->
            return@setOnTouchListener true
        }
    }

    //인증 정보가 없는 경우 액티비티 종료(어댑터 요청)
    override fun finishActivity() {
        finish()
    }

    override fun onLoadingState(visibility: Int) {
        pgBar.visibility = visibility
        opaWindow.visibility = visibility
    }

    override fun onClick(v: View?) {
        when(v!!.id) {

            btnFilterHideShow.id -> {

                when(rdGpGJ.visibility) {
                    View.VISIBLE -> {
                        rdGpGJ.visibility = View.GONE
                        lGyoyangFilter.visibility = View.GONE
                        lJeongongFilter.visibility = View.GONE
                        chkOnlyVacantSubject.visibility = View.GONE
                        lGyoyangPartition.visibility = View.GONE
                        lGsPartition.visibility = View.GONE
                        btnFilterHideShow.text = resources.getString(R.string.activity_enroll_btn_filter_show)
                    }

                    View.GONE -> {
                        rdGpGJ.visibility = View.VISIBLE
                        chkOnlyVacantSubject.visibility = View.VISIBLE
                        if(rdGyoyang.isChecked) {
                            lGyoyangFilter.visibility = View.VISIBLE
                            if(chkOnlyVacantSubject.isChecked) {
                                lGyoyangPartition.visibility = View.VISIBLE
                                if(chkIncludeGs.isChecked) {
                                    lGsPartition.visibility = View.VISIBLE
                                }
                            }
                        } else {
                            lJeongongFilter.visibility = View.VISIBLE
                        }
                        btnFilterHideShow.text = resources.getString(R.string.activity_enroll_btn_filter_hide)
                    }
                }
            }
            btnSearch.id -> {

                btnSearch.setOnClickListener(null)

                //checkbox 가 조회 중간에 변해도 검색 조회 버튼을 누를 당시의 값을 유지하도록 하기 위함
                chkOnlyVacantSubjectState = chkOnlyVacantSubject.isChecked

                if(chkOnlyVacantSubjectState) {
                    chkIncludeGsState = when(chkIncludeGs.isChecked) {
                        true -> "o"
                        false -> "x"
                    }

                    chkIncludeGpState = when(chkIncludeGp.isChecked) {
                        true -> "o"
                        false -> "x"
                    }

                    chkIncludeRoState = when(chkIncludeRo.isChecked) {
                        true -> "o"
                        false -> "x"
                    }

                    chkIncludeGzState = when(chkIncludeGz.isChecked) {
                        true -> "o"
                        false -> "x"
                    }

                    chkExcludeExperimentSubjectState = when(rdJeongong.isChecked) {
                        true -> false
                        false -> chkExcludeExperimentSubject.isChecked
                    }

                    chkExcludeEngineeringSubjectState = when(rdJeongong.isChecked) {
                        true -> false
                        false -> chkExcludeEngineeringSubject.isChecked
                    }
                }

                //교과목명을 한 글자 이상 입력 후 조회(트래픽 과다 방지)
                //if((rdGyoyang.isChecked && txtGyoyangSubjectName.text.toString().isEmpty()) || (rdJeongong.isChecked && txtJeongongSubjectName.text.toString().isEmpty())) {
                //    Toast.makeText(this@Enroll, "교과목명을 한 글자 이상 입력한 후 조회해주세요.", Toast.LENGTH_SHORT).show()
                //    return
                //}

                //키보드 내림
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(txtGyoyangSubjectName.windowToken, 0)
                imm.hideSoftInputFromWindow(txtJeongongSubjectName.windowToken, 0)

                //기존 조회 내역 비우기
                (subjectListAdapter as SubjectListAdapter).subjectList.clear()
                subjectListAdapter.notifyDataSetChanged()

                //알림말 지우고 프로그레스바 보이기
                lblDescription.visibility = View.GONE
                progressBar.visibility = View.VISIBLE

                //라디오버튼 체크에 따라 변수값 설정
                val targetSubjectType = when(rdGyoyang.isChecked) {
                    true -> "gyoyang"
                    false -> "jeongong"
                }
                val targetTxtSubjectName = when(rdGyoyang.isChecked) {
                    true -> txtGyoyangSubjectName
                    false -> txtJeongongSubjectName
                }

                //과목 정보를 가지고 올 주소를 결정한다(DB의 정보)
                HttpConnector("fetch_search_pos.php", "secCode=onlythiswivappcancallthisfetchsearchposphpfile!&userNo=${sf.getString("no", "0")}", object : UIModifyAvailableListener(applicationContext){
                    override fun taskCompleted(result: String?) {
                        super.taskCompleted(result)
                        if(result == "NETWORK_CONNECTION_FAILED") {
                            progressBar.visibility = View.GONE
                            btnSearch.setOnClickListener(this@Enroll)
                            return
                        } else if(result!!.contains("ERROR_CODE")) {
                            Toast.makeText(applicationContext, "예기치 않은 오류가 발생하였습니다. 관리자에게 문의하세요.", Toast.LENGTH_LONG).show()
                            return
                        }

                        Log.d("여기를", "통과하는고니고니")

                        //교양, 전공에 따라 다르게 처리
                        if(targetSubjectType == "jeongong") {
                            //타겟 학부, 학과 코드가 담긴 element 를 선택
                            val targetSubDepartmentElement = subDepartments[selectedDeptStartNo + spnJeongongSubDept.selectedItemPosition - 1]

                            //위 element 를 adapter 에 전달
                            (subjectListAdapter as SubjectListAdapter).updateSubDepartment(targetSubDepartmentElement)

                            //검색 시작
                            HttpConnectorOthers(result, "secCode=onlythiswivappcancallthisdeptsearchphpfile!&userNo=${sf.getString("no", "0")}&gj=$targetSubjectType&dept=${targetSubDepartmentElement.dept}&subDept=${targetSubDepartmentElement.subDept}&subjectNm=${targetTxtSubjectName.text}", object : UIModifyAvailableListener(applicationContext) {
                                override fun taskCompleted(result: String?) {
                                    super.taskCompleted(result)
                                    if(result == "NETWORK_CONNECTION_FAILED") {
                                        progressBar.visibility = View.GONE
                                        btnSearch.setOnClickListener(this@Enroll)
                                        return
                                    }
                                    searchResultRepresentation(result!!)
                                    btnSearch.setOnClickListener(this@Enroll)

                                }
                            }).execute()
                        } else {

                            //교선, 교필, ROTC, 교직 필터에 의한 파라미터 설정
                            val gyoyangFilter = "gs=$chkIncludeGsState&gp=$chkIncludeGpState&ro=$chkIncludeRoState&gz=$chkIncludeGzState"

                            //검색 시작
                            HttpConnectorOthers(result, "secCode=onlythiswivappcancallthisdeptsearchphpfile!&userNo=${sf.getString("no", "0")}&gj=$targetSubjectType&subjectNm=${targetTxtSubjectName.text}&$gyoyangFilter", object : UIModifyAvailableListener(applicationContext) {
                                override fun taskCompleted(result: String?) {
                                    super.taskCompleted(result)
                                    if(result == "NETWORK_CONNECTION_FAILED") {
                                        progressBar.visibility = View.GONE
                                        btnSearch.setOnClickListener(this@Enroll)
                                        return
                                    }
                                    searchResultRepresentation(result!!)
                                    btnSearch.setOnClickListener(this@Enroll)
                                }
                            }).execute()
                        }
                    }

                }).execute()


            }
        }
    }

    private fun fillSpnJeongongSubDept() : ArrayList<String> {
        var subDeptAdapterList = ArrayList<String>()
        var isFirstDetected = false
        for(i : Int in 0..(subDepartments.size - 1)) {
            if(subDepartments[i].deptName == spnJeongongDept.selectedItem.toString()) {
                if(!isFirstDetected) { selectedDeptStartNo = subDepartments[i].no; isFirstDetected = true }
                subDeptAdapterList.add(subDepartments[i].subDeptName)
            }
        }

        subDeptAdapterList = subDeptAdapterList.distinct() as ArrayList<String>

        return subDeptAdapterList
    }

    private fun searchResultRepresentation(result : String) {

        progressBar.visibility = View.GONE

        val data = JSONObject(result).getJSONArray("subject_search_result")
        Log.d("DATA", data.toString())

        var realCnt = 0
        for(i : Int in 0..(data.length() - 1)) {
            val subject = SubjectElement().fillFromJSON(data.get(i).toString())
            if(subject.dayNightNm == "계약" || subject.subjectNm == "") continue
            if(chkOnlyVacantSubjectState && (subject.tlsnCount.toInt() >= subject.tlsnLimitCount.toInt())) continue
            if(chkExcludeExperimentSubjectState && (subject.subjectNm.contains("및실험") || subject.subjectNm.contains("학및실습"))) continue
            if(chkExcludeEngineeringSubjectState && subject.subjectDiv2 == "공학소양") continue

            (subjectListAdapter as SubjectListAdapter).subjectList.add(subject)
            realCnt++
        }

        if(realCnt == 0) {
            lblDescription.text = resources.getString(R.string.activity_enroll_lbl_description_2)
            lblDescription.visibility = View.VISIBLE
            subjectListAdapter.notifyDataSetChanged()
            return
        }

        subjectListAdapter.notifyDataSetChanged()
        listResult.scrollToPosition(0)
    }
}