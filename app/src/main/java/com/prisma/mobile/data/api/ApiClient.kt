package com.prisma.mobile.data.api

import com.prisma.mobile.PrismaApplication
import com.prisma.mobile.util.Constants
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val token = PrismaApplication.instance.sessionManager.getToken()
        
        val requestBuilder = original.newBuilder()
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")

        if (!token.isNullOrEmpty()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        chain.proceed(requestBuilder.build())
    }

    private val failoverInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val originalUrl = originalRequest.url
        val rawPath = originalUrl.encodedPath
        val pathSegment = if (rawPath.contains("/api/v1/")) {
            rawPath.substringAfter("/api/v1/")
        } else {
            rawPath.trimStart('/')
        }
        val pathAndQuery = pathSegment + if (originalUrl.encodedQuery != null) "?${originalUrl.encodedQuery}" else ""

        val fallbackNodes = listOf(
            Constants.SERVER_RENDER_PUBLIC_URL,
            Constants.SERVER_DUCKDNS_PUBLIC_URL,
            Constants.SERVER_SABIO_PUBLIC_URL,
            Constants.WIFI_BASE_URL,
            Constants.USB_BASE_URL,
            Constants.EMULATOR_BASE_URL
        )

        var lastException: java.io.IOException? = null
        var response: okhttp3.Response? = null

        // 1. Intentar primero con el nodo configurado originalmente
        try {
            response = chain.proceed(originalRequest)
            if (response.isSuccessful || (response.code in 200..499 && response.code != 404)) {
                return@Interceptor response
            }
        } catch (e: java.io.IOException) {
            lastException = e
        }

        // 2. Conmutación automática (Failover) a los nodos de respaldo si el primero no responde
        val originalBase = originalUrl.toString()
        for (node in fallbackNodes) {
            val formattedNode = if (node.endsWith("/")) node else "$node/"
            if (originalBase.startsWith(formattedNode)) continue // Ya se intentó

            val targetUrlStr = "$formattedNode$pathAndQuery"
            val newHttpUrl = targetUrlStr.toHttpUrlOrNull() ?: continue
            val newRequest = originalRequest.newBuilder().url(newHttpUrl).build()

            try {
                response?.close()
                response = chain.proceed(newRequest)
                if (response.isSuccessful || (response.code in 200..499 && response.code != 404)) {
                    PrismaApplication.instance.sessionManager.setServerUrl(formattedNode)
                    return@Interceptor response
                }
            } catch (e: java.io.IOException) {
                lastException = e
            }
        }

        if (response != null) return@Interceptor response
        throw lastException ?: java.io.IOException("No se pudo conectar a ningún nodo del cluster PRISMA")
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = createOkHttpClient()

    private fun createOkHttpClient(): OkHttpClient {
        return try {
            val trustAllCerts = arrayOf<javax.net.ssl.TrustManager>(
                object : javax.net.ssl.X509TrustManager {
                    override fun checkClientTrusted(chain: Array<out java.security.cert.X509Certificate>?, authType: String?) {}
                    override fun checkServerTrusted(chain: Array<out java.security.cert.X509Certificate>?, authType: String?) {}
                    override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
                }
            )

            val sslContext = javax.net.ssl.SSLContext.getInstance("TLS")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())

            OkHttpClient.Builder()
                .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as javax.net.ssl.X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .addInterceptor(failoverInterceptor)
                .addInterceptor(authInterceptor)
                .addInterceptor(loggingInterceptor)
                .connectTimeout(6, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .build()
        } catch (e: Exception) {
            OkHttpClient.Builder()
                .addInterceptor(failoverInterceptor)
                .addInterceptor(authInterceptor)
                .addInterceptor(loggingInterceptor)
                .connectTimeout(6, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .build()
        }
    }

    private var retrofit: Retrofit = buildRetrofit(Constants.DEFAULT_BASE_URL)

    var authService: AuthApiService = retrofit.create(AuthApiService::class.java)
        private set
    var conductorService: ConductorApiService = retrofit.create(ConductorApiService::class.java)
        private set
    var tiendaService: TiendaApiService = retrofit.create(TiendaApiService::class.java)
        private set

    fun updateBaseUrl(newBaseUrl: String) {
        val url = if (newBaseUrl.endsWith("/")) newBaseUrl else "$newBaseUrl/"
        retrofit = buildRetrofit(url)
        authService = retrofit.create(AuthApiService::class.java)
        conductorService = retrofit.create(ConductorApiService::class.java)
        tiendaService = retrofit.create(TiendaApiService::class.java)
    }

    private fun buildRetrofit(baseUrl: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
