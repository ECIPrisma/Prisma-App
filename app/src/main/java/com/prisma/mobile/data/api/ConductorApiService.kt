package com.prisma.mobile.data.api

import com.prisma.mobile.data.model.ApiResponse
import com.prisma.mobile.data.model.ConfirmarEntregaRequest
import com.prisma.mobile.data.model.PedidoDto
import com.prisma.mobile.data.model.VaradoReportRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ConductorApiService {

    @GET("repartidores/mis-rutas/hoy")
    suspend fun getRutasHoy(): Response<ApiResponse<List<Map<String, Any>>>>

    @POST("repartidores/rutas/{rutaId}/iniciar")
    suspend fun iniciarRuta(
        @Path("rutaId") rutaId: Long
    ): Response<ApiResponse<Map<String, Any>>>

    @GET("repartidores/rutas/{rutaId}/pedidos")
    suspend fun getPedidosRuta(
        @Path("rutaId") rutaId: Long
    ): Response<ApiResponse<List<PedidoDto>>>

    @GET("repartidores/rutas/{rutaId}/manifiesto")
    suspend fun getManifiestoCarga(
        @Path("rutaId") rutaId: Long
    ): Response<ApiResponse<Map<String, Any>>>

    @POST("repartidores/pedidos/{pedidoId}/confirmar-entrega")
    suspend fun confirmarEntrega(
        @Path("pedidoId") pedidoId: Long,
        @Body request: ConfirmarEntregaRequest
    ): Response<ApiResponse<PedidoDto>>

    @POST("repartidores/rutas/{rutaId}/finalizar")
    suspend fun finalizarRuta(
        @Path("rutaId") rutaId: Long
    ): Response<ApiResponse<Map<String, Any>>>

    @POST("proveedores/rutas/{rutaId}/reportar-varado")
    suspend fun reportarVarado(
        @Path("rutaId") rutaId: Long,
        @Body request: VaradoReportRequest
    ): Response<ApiResponse<Map<String, Any>>>
}
