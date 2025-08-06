package com.colorblind.spectra.UI.quiz

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import com.colorblind.spectra.R

class LoadingDialog(context: Context) : Dialog(context) {
    init {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_loading, null)
        setContentView(view)
        setCancelable(false)
    }
}
