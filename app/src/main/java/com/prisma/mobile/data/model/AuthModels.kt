package com.prisma.mobile.data.model

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(
    @SerializedName("exitoso") val exitoso: Boolean,
    @SerializedName("mensaje") val mensaje: String,
    @SerializedName("datos") val datos: T?,
    @SerializedName("timestamp") val timestamp: String?
)

data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class AuthResponse(
    @SerializedName("token") val token: String,
    @SerializedName("tipoToken") val tipoToken: String,
    @SerializedName("id") val id: Long,
    @SerializedName("email") val email: String,
    @SerializedName("nombreCompleto") val nombreCompleto: String,
    @SerializedName("rol") val rol: String,
    @SerializedName("proveedorId") val proveedorId: Long?
)
