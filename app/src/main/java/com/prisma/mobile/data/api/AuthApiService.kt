package com.prisma.mobile.data.api

import com.prisma.mobile.data.model.ApiResponse
import com.prisma.mobile.data.model.AuthResponse
import com.prisma.mobile.data.model.LoginRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<ApiResponse<AuthResponse>>
}
