package com.saroj.lmsmobile.utils

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import com.saroj.lmsmobile.R

object LmsToast {
    @Suppress("DEPRECATION")
    fun show(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
        val view = LayoutInflater.from(context).inflate(R.layout.layout_lms_toast, null)
        view.findViewById<TextView>(R.id.textLmsToastMessage).text = message
        Toast(context.applicationContext).apply {
            this.duration = duration
            setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 120)
            this.view = view
        }.show()
    }
}
