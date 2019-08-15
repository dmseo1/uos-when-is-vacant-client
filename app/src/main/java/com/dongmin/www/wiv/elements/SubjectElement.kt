package com.dongmin.www.wiv.elements

import com.dongmin.www.wiv.libraries.IFillFromJSON
import org.json.JSONObject

class SubjectElement : IFillFromJSON {
    var category = 1   //전공과 전공이 아닌 것을 구분. 1: 교양, 2: 전공
    var subjectDiv = "" //대분류
    var subjectDiv2 = ""    //소분류
    var subjectNo = ""
    var classDiv = ""   //분반
    var subjectNm = ""
    var dayNightNm = ""
    var shyr = ""
    var credit = ""
    var profNm = ""
    var classNm = ""    //수업일시,장소
    var tlsnCount = ""
    var tlsnLimitCount = ""

    override fun fillFromJSON(jsonString: String): SubjectElement {
        subjectDiv = JSONObject(jsonString).getString("subject_div")
        subjectDiv2 = JSONObject(jsonString).getString("subject_div2")
        category = when(subjectDiv.contains("전공")) {
            true -> 2
            false -> 1
        }
        subjectNo = JSONObject(jsonString).getString("subject_no")
        classDiv = JSONObject(jsonString).getString("class_div")
        subjectNm = JSONObject(jsonString).getString("subject_nm")
        dayNightNm = JSONObject(jsonString).getString("day_night_nm")
        shyr = JSONObject(jsonString).getString("shyr")
        credit = JSONObject(jsonString).getString("credit")
        profNm = JSONObject(jsonString).getString("prof_nm")
        classNm = JSONObject(jsonString).getString("class_nm")
        tlsnCount = when(JSONObject(jsonString).getString("tlsn_count")) {
            "" -> "0"
            else -> JSONObject(jsonString).getString("tlsn_count")
        }
        tlsnLimitCount = when(JSONObject(jsonString).getString("tlsn_limit_count")) {
            "" -> "0"
            else -> JSONObject(jsonString).getString("tlsn_limit_count")
        }

        return this
    }
}
