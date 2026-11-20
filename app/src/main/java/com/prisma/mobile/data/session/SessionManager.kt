package com.prisma.mobile.data.session

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.prisma.mobile.data.model.AuthResponse

class SessionManager(context: Context) {

    private val prefs: SharedPreferences = try {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            "prisma_secure_session",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // Fallback estándar si el dispositivo no soporta crypto hardware
        context.getSharedPreferences("prisma_session_prefs", Context.MODE_PRIVATE)
    }

    fun saveAuthSession(auth: AuthResponse) {
        prefs.edit().apply {
            putString(KEY_TOKEN, auth.token)
            putLong(KEY_USER_ID, auth.id)
            putString(KEY_EMAIL, auth.email)
            putString(KEY_NAME, auth.nombreCompleto)
            putString(KEY_ROLE, auth.rol)
            if (auth.proveedorId != null) {
                putLong(KEY_PROVEEDOR_ID, auth.proveedorId)
            } else {
                remove(KEY_PROVEEDOR_ID)
            }
            apply()
        }
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun getUserRole(): String? = prefs.getString(KEY_ROLE, null)

    fun getUserEmail(): String? = prefs.getString(KEY_EMAIL, null)

    fun getUserName(): String = prefs.getString(KEY_NAME, "Usuario") ?: "Usuario"

    fun isLoggedIn(): Boolean = !getToken().isNullOrEmpty()

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    fun getServerUrl(): String {
        val saved = prefs.getString(KEY_CUSTOM_SERVER_URL, null)
        if (saved.isNullOrEmpty() || saved.contains("server-sabio") || saved == "https://eciprisma.duckdns.org/api/v1/" || saved.contains("192.168.100.") || saved.contains("10.0.2.2")) {
            return com.prisma.mobile.util.Constants.DEFAULT_BASE_URL
        }
        return saved
    }

    fun setServerUrl(url: String) {
        prefs.edit().putString(KEY_CUSTOM_SERVER_URL, url).apply()
    }

    fun saveOfflineCatalog(productos: List<com.prisma.mobile.data.model.ProductoDto>) {
        val json = com.google.gson.Gson().toJson(productos)
        prefs.edit().putString(KEY_OFFLINE_CATALOG, json).apply()
    }

    fun getOfflineCatalog(): List<com.prisma.mobile.data.model.ProductoDto> {
        val json = prefs.getString(KEY_OFFLINE_CATALOG, null)
        if (!json.isNullOrEmpty()) {
            try {
                val type = object : com.google.gson.reflect.TypeToken<List<com.prisma.mobile.data.model.ProductoDto>>() {}.type
                val parsed = com.google.gson.Gson().fromJson<List<com.prisma.mobile.data.model.ProductoDto>>(json, type)
                if (!parsed.isNullOrEmpty()) return parsed
            } catch (_: Exception) {}
        }
        return getDefaultCatalog()
    }

    fun getDefaultCatalog(): List<com.prisma.mobile.data.model.ProductoDto> {
        return listOf(
            com.prisma.mobile.data.model.ProductoDto(1L, "KO-PET-400", "Coca-Cola Original 400ml", "Paquete x 12 Unidades", 28500.0, 36000.0, 999, 1, true, true, "real_coca_400ml"),
            com.prisma.mobile.data.model.ProductoDto(2L, "KO-PET-3000", "Coca-Cola Sabor Original 3 Litros PET", "Paca x 6 Botellas", 54000.0, 66000.0, 999, 1, true, true, "real_coca_3l"),
            com.prisma.mobile.data.model.ProductoDto(3L, "KO-PET-1500", "Coca-Cola 1.5L Retornable Vidrio/PET", "Caja x 8 Botellas", 32000.0, 42000.0, 999, 1, false, true, "real_coca_15l"),
            com.prisma.mobile.data.model.ProductoDto(4L, "KO-350ML-RET", "Coca-Cola Vidrio Retornable 350ml", "Canasta x 30 Botellas", 36000.0, 54000.0, 999, 1, false, true, "real_coca_vidrio"),
            com.prisma.mobile.data.model.ProductoDto(5L, "KO-LATA-330", "Coca-Cola Sin Azúcar Lata 330ml", "Pack x 24 Latas", 54000.0, 72000.0, 999, 1, false, false, "real_coca_400ml"),
            com.prisma.mobile.data.model.ProductoDto(6L, "KO-SPRITE-400", "Sprite Lima-Limón 400ml PET", "Paca x 12 Unidades", 27000.0, 34800.0, 999, 1, false, false, "real_sprite"),
            com.prisma.mobile.data.model.ProductoDto(7L, "KO-QUATRO-400", "Quatro Toronja 400ml PET", "Paca x 12 Unidades", 28000.0, 35400.0, 999, 1, false, false, "real_quatro"),
            com.prisma.mobile.data.model.ProductoDto(8L, "KO-MONSTER-473", "Monster Energy Drink 473ml", "Bandeja x 12 Latas", 86000.0, 108000.0, 999, 1, true, false, "real_monster"),
            com.prisma.mobile.data.model.ProductoDto(9L, "KO-POWER-500", "Powerade Ion4 Mountain Berry 500ml", "Paca x 12 Botellas", 32400.0, 42000.0, 999, 1, false, false, "real_powerade"),
            com.prisma.mobile.data.model.ProductoDto(10L, "VALLE-FRUT-15", "Jugo Del Valle Frut Citrus 1.5L", "Pack x 6 Unidades", 21000.0, 28000.0, 999, 1, true, false, "real_delvalle"),
            com.prisma.mobile.data.model.ProductoDto(11L, "AGUA-BRISA-600", "Agua Brisa Manantial Sin Gas 600ml", "Paquete x 24 Botellas", 26000.0, 36000.0, 999, 1, false, false, "real_agua_brisa"),
            com.prisma.mobile.data.model.ProductoDto(12L, "PST-MANZ-400", "Manzana Postobón 400ml PET", "Paca x 15 Unidades", 36000.0, 42000.0, 999, 1, true, false, "real_postobon_manzana"),
            com.prisma.mobile.data.model.ProductoDto(13L, "PST-COLOMB-400", "Gaseosa Colombiana La Nuestra 400ml", "Paca x 15 Unidades", 36000.0, 43500.0, 999, 1, false, false, "real_colombiana"),
            com.prisma.mobile.data.model.ProductoDto(14L, "HIT-MORA-500ML", "Jugo Hit Mora 500ml PET", "Paca x 12 Unidades", 26400.0, 33600.0, 999, 1, false, false, "real_hit_mora"),
            com.prisma.mobile.data.model.ProductoDto(15L, "PST-BRETANA-300", "Bretaña Soda Mezclador 300ml Vidrio", "Canasta x 24 Botellas", 38000.0, 48000.0, 999, 1, false, false, "real_bretana")
        )
    }

    fun enqueueOfflineOrder(order: com.prisma.mobile.data.model.CrearPedidoRequest) {
        val actuales = getPendingOfflineOrders().toMutableList()
        actuales.add(order)
        val json = com.google.gson.Gson().toJson(actuales)
        prefs.edit().putString(KEY_OFFLINE_ORDERS, json).apply()
    }

    fun getPendingOfflineOrders(): List<com.prisma.mobile.data.model.CrearPedidoRequest> {
        val json = prefs.getString(KEY_OFFLINE_ORDERS, null) ?: return emptyList()
        return try {
            val type = object : com.google.gson.reflect.TypeToken<List<com.prisma.mobile.data.model.CrearPedidoRequest>>() {}.type
            com.google.gson.Gson().fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun isTenderoPro(): Boolean = prefs.getBoolean(KEY_TENDERO_PRO, false)

    fun setTenderoPro(activo: Boolean) {
        prefs.edit().putBoolean(KEY_TENDERO_PRO, activo).apply()
    }

    fun clearPendingOfflineOrders() {
        prefs.edit().remove(KEY_OFFLINE_ORDERS).apply()
    }

    companion object {
        private const val KEY_TOKEN = "jwt_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_EMAIL = "email"
        private const val KEY_NAME = "nombre_completo"
        private const val KEY_ROLE = "user_role"
        private const val KEY_PROVEEDOR_ID = "proveedor_id"
        private const val KEY_CUSTOM_SERVER_URL = "custom_server_url"
        private const val KEY_OFFLINE_CATALOG = "offline_catalog_cache"
        private const val KEY_OFFLINE_ORDERS = "offline_orders_queue"
        private const val KEY_TENDERO_PRO = "tendero_pro_activo"
    }
}
