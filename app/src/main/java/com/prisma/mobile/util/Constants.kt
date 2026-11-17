package com.prisma.mobile.util

object Constants {
    // IP local detectada de la máquina de desarrollo en la red Wi-Fi
    const val SERVER_WIFI_IP = "192.168.100.9"
    const val SERVER_PORT = "8080"

    // Servidor Cloud Producción Oficial (DuckDNS + AWS EC2 + Neon PostgreSQL)
    const val SERVER_DUCKDNS_PUBLIC_URL = "https://eciprisma.duckdns.org/api/v1/"

    // Servidores Cloud de Contingencia / Failover
    const val SERVER_RENDER_PUBLIC_URL = "https://prisma-backend-web.onrender.com/api/v1/"
    const val SERVER_SABIO_PUBLIC_URL = "https://server-sabio.tail2474b3.ts.net:8443/api/v1/"
    const val SERVER_SABIO_VPN_URL = SERVER_DUCKDNS_PUBLIC_URL

    const val DEFAULT_BASE_URL = SERVER_DUCKDNS_PUBLIC_URL
    const val CLOUD_BASE_URL = SERVER_DUCKDNS_PUBLIC_URL
    const val SERVER_SABIO_URL = SERVER_SABIO_PUBLIC_URL

    // URLs locales opcionales
    const val EMULATOR_BASE_URL = "http://10.0.2.2:8080/api/v1/"
    const val WIFI_BASE_URL = "http://192.168.100.9:8080/api/v1/"
    const val USB_BASE_URL = "http://localhost:8080/api/v1/"

    fun isEmulator(): Boolean {
        val fp = android.os.Build.FINGERPRINT.lowercase()
        val model = android.os.Build.MODEL.lowercase()
        val brand = android.os.Build.BRAND.lowercase()
        val device = android.os.Build.DEVICE.lowercase()
        val hardware = android.os.Build.HARDWARE.lowercase()
        val product = android.os.Build.PRODUCT.lowercase()

        return fp.startsWith("generic")
                || fp.startsWith("unknown")
                || model.contains("google_sdk")
                || model.contains("emulator")
                || model.contains("android sdk built for x86")
                || brand.startsWith("generic")
                || device.startsWith("generic")
                || hardware.contains("goldfish")
                || hardware.contains("ranchu")
                || product.contains("sdk")
                || product.contains("emulator")
                || product.contains("simulator")
    }
    
    // Roles del sistema
    const val ROLE_ADMIN = "ROLE_ADMIN"
    const val ROLE_PROVEEDOR = "ROLE_PROVEEDOR"
    const val ROLE_REPARTIDOR = "ROLE_REPARTIDOR"
    const val ROLE_TENDERO = "ROLE_TENDERO"

    /**
     * MVP: los tenderos siguen pagando sus pedidos como siempre (efectivo contra entrega); quien paga en línea
     * a PRISMA es el distribuidor (suscripción Prisma Pro, desde la web). El checkout del tendero (PagoActivity)
     * se conserva pero queda oculto. Cambiar a true para volver a mostrarlo.
     */
    const val PAGO_EN_LINEA_TENDERO_HABILITADO = false
}
