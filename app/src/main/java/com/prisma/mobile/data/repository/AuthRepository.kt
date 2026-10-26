package com.prisma.mobile.data.repository

import com.prisma.mobile.data.api.ApiClient
import com.prisma.mobile.data.model.ApiResponse
import com.prisma.mobile.data.model.AuthResponse
import com.prisma.mobile.data.model.LoginRequest
import com.prisma.mobile.data.session.SessionManager
import retrofit2.Response

class AuthRepository(
    private val sessionManager: SessionManager
) {
    suspend fun login(request: LoginRequest): Response<ApiResponse<AuthResponse>> {
        val response = ApiClient.authService.login(request)
        if (response.isSuccessful && response.body()?.exitoso == true) {
            response.body()?.datos?.let { auth ->
                sessionManager.saveAuthSession(auth)
            }
        }
        return response
    }

    fun isUserLoggedIn(): Boolean = sessionManager.isLoggedIn()
    fun getUserRole(): String? = sessionManager.getUserRole()
    fun logout() = sessionManager.clearSession()
}
