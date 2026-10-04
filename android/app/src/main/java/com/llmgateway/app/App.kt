package com.llmgateway.app

import android.app.Application
import com.llmgateway.app.data.api.ApiClient

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        ApiClient.init(this)
    }
}
