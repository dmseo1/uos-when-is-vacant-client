package com.dongmin.www.wiv.activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.dongmin.www.wiv.R
import kotlinx.android.synthetic.main.activity_notice.*

class Notice : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notice)

        lblTitle.text = intent.getStringExtra("notice_title")
        lblContent.text = intent.getStringExtra("notice_content")
            //.replace(" ", "\u00A0")

        btnClose.setOnClickListener {
            finish()
        }
    }
}