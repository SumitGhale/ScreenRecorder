package com.example.screenrecorder

import android.annotation.SuppressLint
import android.content.Context

object ViewModelStore {
    @SuppressLint("StaticFieldLeak")
    private var viewModel: HomeScreenViewModel? = null

    fun initialize(context: Context) {
        if (viewModel == null) {
            viewModel = HomeScreenViewModel(context.applicationContext)
        }
    }

    fun getInstance(): HomeScreenViewModel {
        return viewModel ?: throw IllegalStateException("ViewModel not initialized")
    }
}