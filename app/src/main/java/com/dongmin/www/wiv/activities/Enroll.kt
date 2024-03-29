package com.dongmin.www.wiv.activities

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import com.dongmin.www.wiv.elements.SubDepartment
import com.dongmin.www.wiv.elements.SubjectElement
import com.dongmin.www.wiv.libraries.HttpConnector
import com.dongmin.www.wiv.libraries.HttpConnectorOthers
import com.dongmin.www.wiv.libraries.UIModifyAvailableListener
import com.dongmin.www.wiv.popups.FilterHelp
import kotlinx.android.synthetic.main.activity_enroll.*
import org.json.JSONException
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
    private var chkExcludeVolunteerSubjectState = false
    private var chkExcludeFLanguageSubjectState = false
    private var chkExcludeConsultSubjectState = false

    private var isInvalidateBackPressed = false

    //현재 출력된 조회 결과를 호출한 시간보다 이전에 호출한 조회 결과를 막기 위한 변수. 성공한 조회 시간을 기록한다.
    private var searchedTime = 0L

    override fun onCreate(savedInstanceState : Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enroll)
        sf = getSharedPreferences("app_info", MODE_PRIVATE)

        onLoadingState(View.VISIBLE)
        //학과 정보 가져온 후 로그인 상태 체크, 이후 로그인 페이지 또는 메인 페이지를 띄운다
        HttpConnector("fetch_dept_info.php", "secCode=onlythiswivappcancallthisfetchdeptinfophpfile!", object : UIModifyAvailableListener(applicationContext) {
            override fun taskCompleted(result: String?) {
                super.taskCompleted(result)

                if (result!!.contains("NETWORK_CONNECTION")) {
                    onLoadingState(View.GONE)
                    finish()
                    return
                }
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
                onLoadingState(View.GONE)


                //대학 스피너
                spnJeongongDept.adapter = ArrayAdapter(this@Enroll, R.layout.support_simple_spinner_dropdown_item, departments)
                spnJeongongDept.setSelection(0)

                //학부/과 스피너
                spnJeongongSubDept.adapter = ArrayAdapter(this@Enroll, R.layout.support_simple_spinner_dropdown_item, fillSpnJeongongSubDept())
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
            }
        }).execute()

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

        if(sf.getBoolean("chk_exclude_volunteer_subject", false)) {
            chkExcludeVolunteerSubject.isChecked = true
        }

        if(sf.getBoolean("chk_exclude_f_language_subject", false)) {
            chkExcludeFLanguageSubject.isChecked = true
        }

        //전공 분류
        if(sf.getBoolean("chk_exclude_consult_subject", false)) {
            chkExcludeConsultSubject.isChecked = true
        }



        //조회 버튼 리스너 연결
        btnSearch.setOnClickListener(this@Enroll)

        //필터 보이기/숨기기 버튼 리스너 연결
        btnFilterHideShow.setOnClickListener(this@Enroll)

        //교양 라디오버튼 리스너 연결
        rdGyoyang.setOnCheckedChangeListener {_, isChecked ->
            if(isChecked)  {
                lGyoyangFilter.visibility = View.VISIBLE
                lJeongongFilter.visibility = View.GONE
                lJeongongPartition.visibility = View.GONE
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
                if(chkOnlyVacantSubject.isChecked) {
                    lJeongongPartition.visibility = View.VISIBLE
                }
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
                } else {
                    lJeongongPartition.visibility = View.VISIBLE
                }
            } else {
                sfEditor.putBoolean("chk_only_vacant_subject_state", false)
                lGyoyangPartition.visibility = View.GONE
                lGsPartition.visibility = View.GONE
                lJeongongPartition.visibility = View.GONE
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

        //사회봉사 조회 제외 체크박스 리스너 설정
        chkExcludeVolunteerSubject.setOnCheckedChangeListener { _, isChecked ->
            val sfEditor = sf.edit()
            if(isChecked) {
                sfEditor.putBoolean("chk_exclude_volunteer_subject", true)
            } else {
                sfEditor.putBoolean("chk_exclude_volunteer_subject", false)
            }
            sfEditor.apply()
        }

        //제2외국어 조회 제외 체크박스 리스너 설정
        chkExcludeFLanguageSubject.setOnCheckedChangeListener { _, isChecked ->
            val sfEditor = sf.edit()
            if(isChecked) {
                sfEditor.putBoolean("chk_exclude_f_language_subject", true)
            } else {
                sfEditor.putBoolean("chk_exclude_f_language_subject", false)
            }
            sfEditor.apply()
        }

        //학업설계상담 조회 제외 체크박스 리스너 설정
        chkExcludeConsultSubject.setOnCheckedChangeListener { _, isChecked ->
            val sfEditor = sf.edit()
            if(isChecked) {
                sfEditor.putBoolean("chk_exclude_consult_subject", true)
            } else {
                sfEditor.putBoolean("chk_exclude_consult_subject", false)
            }
            sfEditor.apply()
        }

        //결과 조회 어댑터 생성 및 연결
        subjectListAdapter = SubjectListAdapter(this@Enroll)
        subjectListLayoutManager = LinearLayoutManager(this@Enroll)
        listResult.adapter = subjectListAdapter
        listResult.layoutManager = subjectListLayoutManager

        //조회 결과 recycler view adapter 의 콜백 연결
        (subjectListAdapter as SubjectListAdapter).setCallbackToEnroll(this@Enroll)

        //프로그레스 숨김 설정
        progressBar.visibility = View.GONE

        //과목 등록, 대학/학부/과 re-fetch 시 나타나는 opaWindow 에 터치 무효화 설정
        opaWindow.setOnTouchListener{ _, _ ->
            return@setOnTouchListener true
        }

        //도움말 버튼들 리스너 연결
        btnHelpExperiment.setOnClickListener(this)
        btnHelpEngineering.setOnClickListener(this)
        btnHelpFLanguage.setOnClickListener(this)
        btnHelpVolunteer.setOnClickListener(this)
        btnHelpConsult.setOnClickListener(this)
    }

    override fun onBackPressed() {
        if(isInvalidateBackPressed) {
            return
        } else {
            super.onBackPressed()
        }
    }



    //인증 정보가 없는 경우 액티비티 종료(어댑터 요청)
    override fun finishActivity() {
        setResult(9999, null)
        finish()
    }

    override fun onLoadingState(visibility: Int) {
        pgBar.visibility = visibility
        opaWindow.visibility = visibility
    }

    override fun handleBackPress(invalidate: Boolean) {
        isInvalidateBackPressed = invalidate
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
                        lJeongongPartition.visibility = View.GONE
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
                            if(chkOnlyVacantSubject.isChecked) {
                                lJeongongPartition.visibility = View.VISIBLE
                            }
                        }
                        btnFilterHideShow.text = resources.getString(R.string.activity_enroll_btn_filter_hide)
                    }
                }
            }
            btnSearch.id -> {

                //조회 시작 타임 기록
                val searchStartTime = System.currentTimeMillis()

                //조회 도중, 조회시간이 3초 이상이 넘어가면 조회 버튼을 재클릭할 수 있도록 함
                val threeSeconds = Thread {
                    try {
                        Thread.sleep(3000)
                        btnSearch.setOnClickListener(this@Enroll)
                    } catch(e : InterruptedException) {

                    }
                }
                threeSeconds.start()

                //버튼 클릭 무효화
                btnSearch.setOnClickListener(null)

                //checkbox 가 조회 중간에 변해도 검색 조회 버튼을 누를 당시의 값을 유지하도록 하기 위하여, 변수들을 기록한다.
                //


                chkOnlyVacantSubjectState = chkOnlyVacantSubject.isChecked

                //공석 조회시에만 제외 필터를 적용한다
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

                    //전공 조회 여부를 체크하는 이유: 전공을 조회할 때에는 이러한 것을 필터링할 이유가 없기 때문에
                    //전공 조회시에는 모두 false 로 만들어주기 위함이다.
                    chkExcludeExperimentSubjectState = when(rdJeongong.isChecked) {
                        true -> false
                        false -> chkExcludeExperimentSubject.isChecked
                    }

                    chkExcludeEngineeringSubjectState = when(rdJeongong.isChecked) {
                        true -> false
                        false -> chkExcludeEngineeringSubject.isChecked
                    }

                    chkExcludeVolunteerSubjectState = when(rdJeongong.isChecked) {
                        true -> false
                        false -> chkExcludeVolunteerSubject.isChecked
                    }

                    chkExcludeFLanguageSubjectState = when(rdJeongong.isChecked) {
                        true -> false
                        false -> chkExcludeFLanguageSubject.isChecked
                    }

                    //교양 조회 여부를 체크하는 이유: 교양을 조회할 때에는 이러한 것을 필터링할 이유가 없기 때문에
                    //교양 조회시에는 모두 false 로 만들어주기 위함이다.
                    chkExcludeConsultSubjectState = when(rdGyoyang.isChecked) {
                        true -> false
                        false -> chkExcludeConsultSubject.isChecked
                    }
                } else {
                    chkIncludeGsState = "o"
                    chkIncludeGpState = "o"
                    chkIncludeRoState = "o"
                    chkIncludeGzState = "o"
                    chkExcludeExperimentSubjectState = false
                    chkExcludeEngineeringSubjectState = false
                    chkExcludeVolunteerSubjectState = false
                    chkExcludeFLanguageSubjectState = false
                    chkExcludeConsultSubjectState = false
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
                HttpConnector("fetch_search_pos.php", "secCode=onlythiswivappcancallthisfetchsearchposphpfile!&userNo=${sf.getString("no", "0")}&token=${sf.getString("token", "FIRST")}&os=android", object : UIModifyAvailableListener(applicationContext){
                    override fun taskCompleted(result: String?) {
                        super.taskCompleted(result)
                        if(result!!.contains("NETWORK_CONNECTION")) {
                            progressBar.visibility = View.GONE
                            btnSearch.setOnClickListener(this@Enroll)
                            threeSeconds.interrupt()
                            return
                        } else if(result.contains("ERROR_CODE")) {
                            Toast.makeText(applicationContext, "예기치 않은 오류가 발생하였습니다. 관리자에게 문의하세요.", Toast.LENGTH_LONG).show()
                            btnSearch.setOnClickListener(this@Enroll)
                            threeSeconds.interrupt()
                            return
                        }

                        //기존 조회 내역 다시 비우기(실질적인 역할)
                        (subjectListAdapter as SubjectListAdapter).subjectList.clear()

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
                                        threeSeconds.interrupt()
                                        return
                                    }
                                    val successfullySearched = searchResultRepresentation(result!!, searchStartTime)
                                    btnSearch.setOnClickListener(this@Enroll)
                                    threeSeconds.interrupt()
                                    if(successfullySearched) searchedTime = searchStartTime
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
                                        threeSeconds.interrupt()
                                        return
                                    }
                                    val successfullySearched = searchResultRepresentation(result!!, searchStartTime)
                                    btnSearch.setOnClickListener(this@Enroll)
                                    threeSeconds.interrupt()
                                    if(successfullySearched) searchedTime = searchStartTime
                                }
                            }).execute()
                        }
                    }
                }).execute()


            }

            btnHelpExperiment.id -> {
                FilterHelp(this@Enroll, resources.getString(R.string.popup_filter_help_experiment)).start()
            }
            btnHelpEngineering.id -> {
                FilterHelp(this@Enroll, resources.getString(R.string.popup_filter_help_engineer)).start()
            }
            btnHelpVolunteer.id -> {
                FilterHelp(this@Enroll, resources.getString(R.string.popup_filter_help_volunteer)).start()
            }
            btnHelpFLanguage.id -> {
                FilterHelp(this@Enroll, resources.getString(R.string.popup_filter_help_f_language)).start()
            }
            btnHelpConsult.id -> {
                FilterHelp(this@Enroll, resources.getString(R.string.popup_filter_help_consult)).start()
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

        if(subDeptAdapterList.size > 1) {
            subDeptAdapterList = subDeptAdapterList.distinct() as ArrayList<String>
        }

        return subDeptAdapterList
    }

    private fun searchResultRepresentation(result : String, searchStartTime : Long) : Boolean {
        progressBar.visibility = View.GONE
        if(searchStartTime < searchedTime) return false   //이전 시간에 요청된 조회 결과에 대해 무효화시킴
        try {
            val data = JSONObject(result).getJSONArray("subject_search_result")
            // Log.d("DATA", data.toString())

            var realCnt = 0
            for(i : Int in 0..(data.length() - 1)) {
                val subject = SubjectElement().fillFromJSON(data.get(i).toString())
                if(subject.dayNightNm == "계약" || subject.subjectNm == "") continue
                if(chkOnlyVacantSubjectState && (subject.tlsnCount.toInt() >= subject.tlsnLimitCount.toInt())) continue
                if(chkExcludeExperimentSubjectState && subject.subjectDiv2 == "학문기초" &&
                    (subject.subjectNm.contains("및실험") || subject.subjectNm.contains("학및실습") || subject.subjectNm.contains("창의주제탐구세미나"))) continue
                if(chkExcludeEngineeringSubjectState && subject.subjectDiv2 == "공학소양") continue
                if(chkExcludeVolunteerSubjectState && subject.subjectDiv2 == "사회봉사") continue
                if(chkExcludeFLanguageSubjectState && subject.subjectDiv2 == "외국어" &&
                    (subject.subjectNm.contains("중국어") || subject.subjectNm.contains("일본어") || subject.subjectNm.contains("스페인어") ||
                            subject.subjectNm.contains("베트남어") || subject.subjectNm.contains("러시아어") || subject.subjectNm.contains("독일어") ||
                            subject.subjectNm.contains("불어") || subject.subjectNm.contains("라틴어"))) continue
                if(chkExcludeConsultSubjectState && subject.subjectDiv.contains("전공") && subject.subjectNm.contains("학업설계상담")) continue

                (subjectListAdapter as SubjectListAdapter).subjectList.add(subject)
                realCnt++
            }

            if(realCnt == 0) {
                lblDescription.text = resources.getString(R.string.activity_enroll_lbl_description_2)
                lblDescription.visibility = View.VISIBLE
                subjectListAdapter.notifyDataSetChanged()
                return true
            }

            subjectListAdapter.notifyDataSetChanged()
            listResult.scrollToPosition(0)

            return true
        } catch(e : JSONException) {
            Toast.makeText(this@Enroll, "조회를 다시 시도해주십시오.", Toast.LENGTH_SHORT).show()
            return false
        }
    }
}