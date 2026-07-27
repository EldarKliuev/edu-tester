package com.example.edu_tester

import android.app.Application
import com.example.edu_tester.di.AppContainer

class EduTesterApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
