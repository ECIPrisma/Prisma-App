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
        val json = prefs.getString(KEY_OFFLINE_CATALOG, null) ?: return emptyList()
        return try {
            val type = object : com.google.gson.reflect.TypeToken<List<com.prisma.mobile.data.model.ProductoDto>>() {}.type
            com.google.gson.Gson().fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
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
    }
}
