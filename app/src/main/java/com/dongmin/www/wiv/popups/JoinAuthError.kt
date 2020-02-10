package com.dongmin.www.wiv.popups

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.Window
import android.widget.Button
import com.dongmin.www.wiv.R


class JoinAuthError(context : Context) {

    private var dlg = Dialog(context)
    private lateinit var btnOK : Button

    fun start() {
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dlg.setContentView(R.layout.popup_join_auth_error)
        dlg.setCancelable(false)

        btnOK = dlg.findViewById(R.id.btnOK)
        btnOK.setOnClickListener {
            dlg.dismiss()
        }

        dlg.show()
    }

}

