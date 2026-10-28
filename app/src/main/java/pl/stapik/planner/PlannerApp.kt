package pl.stapik.planner

import android.app.Application
import android.content.Context
import pl.stapik.planner.di.AppContainer

class PlannerApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as PlannerApp).container
