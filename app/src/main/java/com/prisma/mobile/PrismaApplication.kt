package com.prisma.mobile

import android.app.Application
import com.prisma.mobile.data.session.SessionManager
import org.osmdroid.config.Configuration

class PrismaApplication : Application() {

    lateinit var sessionManager: SessionManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        sessionManager = SessionManager(this)

        // Inicializar URL del servidor guardada o por defecto
        com.prisma.mobile.data.api.ApiClient.updateBaseUrl(sessionManager.getServerUrl())

        // Configuración de OSMDroid (OpenStreetMap Low-Cost $0)
        Configuration.getInstance().load(this, getSharedPreferences("osm_prefs", MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName
    }

    companion object {
        lateinit var instance: PrismaApplication
            private set
    }
}
