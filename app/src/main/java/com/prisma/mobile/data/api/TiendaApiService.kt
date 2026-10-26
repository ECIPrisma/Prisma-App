package com.prisma.mobile.data.api

import com.prisma.mobile.data.model.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface TiendaApiService {

    @GET("tiendas/mi-tienda")
    suspend fun getMiTienda(): Response<ApiResponse<TiendaDto>>

    @GET("tiendas/catalogo/{proveedorId}")
    suspend fun getCatalogo(
        @Path("proveedorId") proveedorId: Long
    ): Response<ApiResponse<List<ProductoDto>>>

    @GET("tiendas/mis-pedidos")
    suspend fun getMisPedidos(): Response<ApiResponse<List<PedidoDto>>>

    @POST("tiendas/pedidos")
    suspend fun crearPedido(
        @Body request: CrearPedidoRequest
    ): Response<ApiResponse<PedidoDto>>

    @POST("tiendas/pedidos/repetir-ultimo")
    suspend fun repetirUltimoPedido(): Response<ApiResponse<PedidoDto>>

    @POST("tiendas/pedidos/{pedidoId}/reportar-averia")
    suspend fun reportarAveria(
        @Path("pedidoId") pedidoId: Long,
        @Body request: ReportarAveriaRequest
    ): Response<ApiResponse<Map<String, Any>>>
}
