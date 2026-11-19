package com.prisma.mobile.data.api

import com.prisma.mobile.data.model.ApiResponse
import com.prisma.mobile.data.model.AuthResponse
import com.prisma.mobile.data.model.LoginRequest
import com.prisma.mobile.data.model.TotpCodeRequest
import com.prisma.mobile.data.model.TotpDisableRequest
import com.prisma.mobile.data.model.TotpSetupResponse
import com.prisma.mobile.data.model.TotpStatusResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApiService {

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<ApiResponse<AuthResponse>>

    @GET("auth/totp/status")
    suspend fun getTotpStatus(): Response<ApiResponse<TotpStatusResponse>>

    @POST("auth/totp/setup")
    suspend fun setupTotp(): Response<ApiResponse<TotpSetupResponse>>

    @POST("auth/totp/enable")
    suspend fun enableTotp(
        @Body request: TotpCodeRequest
    ): Response<ApiResponse<TotpStatusResponse>>

    @POST("auth/totp/disable")
    suspend fun disableTotp(
        @Body request: TotpDisableRequest
    ): Response<ApiResponse<TotpStatusResponse>>
}
