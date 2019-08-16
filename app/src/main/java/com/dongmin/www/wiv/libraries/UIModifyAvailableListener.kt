package com.dongmin.www.wiv.libraries

import android.content.Context
import android.util.Log
import android.widget.Toast


abstract class UIModifyAvailableListener(private val context : Context) {
    open fun taskCompleted(result : String?) {
        //Log.d("여기 온다", "오나요?")
        when(result!!) {
            "NETWORK_CONNECTION_FAILED" -> {
                Toast.makeText(context, "인터넷 연결 상태를 확인하세요.", Toast.LENGTH_SHORT).show()
                return
            }
            "NETWORK_CONNECTION_UNSTABLE" -> {
                Toast.makeText(context, "인터넷 또는 서버 상태가 불안정합니다. 잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                return
            }
        }
    }
}