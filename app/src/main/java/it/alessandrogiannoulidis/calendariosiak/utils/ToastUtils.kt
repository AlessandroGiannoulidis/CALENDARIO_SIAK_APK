package it.alessandrogiannoulidis.calendariosiak.utils

import android.content.Context
import android.widget.Toast

object ToastUtils {
    private var currentToast: Toast? = null

    fun showToast(context: Context, message: String, isLong: Boolean = false) {
        currentToast?.cancel()
        val duration = if (isLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
        currentToast = Toast.makeText(context, message, duration)
        currentToast?.show()
    }

    fun showToast(context: Context, messageResId: Int, isLong: Boolean = false) {
        showToast(context, context.getString(messageResId), isLong)
    }
}
