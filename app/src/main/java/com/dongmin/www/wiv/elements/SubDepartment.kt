package com.dongmin.www.wiv.elements

import com.dongmin.www.wiv.libraries.IFillFromJSON
import org.json.JSONObject

class SubDepartment : IFillFromJSON {

    var no : Int = 0
    var dept : String = ""
    var subDept : String = ""
    var deptName : String = ""
    var subDeptName : String = ""


    override fun fillFromJSON(jsonString: String) : SubDepartment {
        this.no  = JSONObject(jsonString).getString("no").toInt()
        this.dept = JSONObject(jsonString).getString("dept")
        this.subDept = JSONObject(jsonString).getString("sub_dept")
        this.deptName = JSONObject(jsonString).getString("dept_name")
        this.subDeptName = JSONObject(jsonString).getString("sub_dept_name")
        return this
    }

}