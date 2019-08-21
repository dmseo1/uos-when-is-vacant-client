package com.dongmin.www.wiv.libraries

import android.content.Context
import android.util.Log
import android.widget.Toast


abstract class UIModifyAvailableListener(private val context : Context) {
    open fun taskCompleted(result : String?) {
        //Log.d("여기 온다", "오나요?")

        //일치성
        when(result!!) {
            "NETWORK_CONNECTION_FAILED" -> {
                Toast.makeText(context, "인터넷 연결 상태를 확인하세요.", Toast.LENGTH_SHORT).show()
                return
            }
            "NETWORK_CONNECTION_UNSTABLE" -> {
                Toast.makeText(context, "인터넷 또는 서버 상태가 불안정합니다. 잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                return
            }

            "NETWORK_CONNECTION_DB_FAILED" -> {
                Toast.makeText(context, "서버 문제입니다. 관리자에게 문의하세요.", Toast.LENGTH_SHORT).show()
                return
            }
        }

        //포함성
        when(result.contains("NETWORK_CONNECTION_ERROR_CODE")) {
            true -> {
                Toast.makeText(context, "서버 측 처리 실패입니다. 관리자에게 문의하세요($result)", Toast.LENGTH_LONG).show()
                return
            }
            false -> {

            }
        }

        //기타 처리
        when(result.contains("NETWORK_CONNECTION")) {
            true -> {
                Toast.makeText(context, "예기치 않은 오류입니다. 관리자에게 문의하세요($result)", Toast.LENGTH_LONG).show()
                return
            }
            false -> {

            }
        }
    }
}