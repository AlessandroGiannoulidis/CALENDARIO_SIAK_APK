package it.alessandrogiannoulidis.calendariosiak.utils

import android.content.Context
import android.widget.Toast

object ToastUtils {
    private var currentToast: Toast? = null

    fun showToast(context: Context, message: String) {
        currentToast?.cancel()
        currentToast = ToastUtils.showToast(context, context.getString(R.string.nome_stringa))
    }
}
