package com.dongmin.www.wiv.libraries

import android.content.Context
import android.util.Log
import android.widget.Toast


abstract class UIModifyAvailableListener(private val context : Context) {
    open fun taskCompleted(result : String?) {
        //Log.d("여기 온다", "오나요?")
        if(result == "NETWORK_CONNECTION_FAILED") {
            //Log.d("여기 온다", "여기 와")
            Toast.makeText(context, "인터넷 연결 상태를 확인하세요.", Toast.LENGTH_SHORT).show()
            return
        }
    }
}