package dev.deathlegion.dazki.manager

import android.app.Application

/**
 * Process singleton for the manager app. Owns the repository and keeps
 * it alive across configuration changes.
 */
class DazkiManagerApp : Application() {

    lateinit var repository: DazkiManagerRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        repository = DazkiManagerRepository(this)
    }

    companion object {
        @Volatile private var instance: DazkiManagerApp? = null
        fun get(): DazkiManagerApp = instance ?: error("DazkiManagerApp not initialized")
    }
}
