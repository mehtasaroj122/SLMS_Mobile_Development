package com.saroj.lmsmobile.utils

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.saroj.lmsmobile.R

object LmsToast {
    @Suppress("DEPRECATION")
    fun show(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
        val view = LayoutInflater.from(context).inflate(R.layout.layout_lms_toast, null)
        view.findViewById<TextView>(R.id.textLmsToastMessage).apply {
            text = message
            setTextColor(ContextCompat.getColor(context, R.color.lms_toast_text))
        }
        view.findViewById<ImageView>(R.id.imageLmsToastLogo)?.alpha = 0.95f
        val yOffset = (56 * context.resources.displayMetrics.density).toInt()
        Toast(context.applicationContext).apply {
            this.duration = duration
            setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, yOffset)
            this.view = view
        }.show()
    }
}
