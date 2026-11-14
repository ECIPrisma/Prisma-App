package com.prisma.mobile.data.repository

import com.prisma.mobile.data.api.ApiClient
import com.prisma.mobile.data.model.*
import retrofit2.Response

class TiendaRepository {
    suspend fun getMiTienda(): Response<ApiResponse<TiendaDto>> {
        return ApiClient.tiendaService.getMiTienda()
    }

    suspend fun getCatalogo(proveedorId: Long): Response<ApiResponse<List<ProductoDto>>> {
        return ApiClient.tiendaService.getCatalogo(proveedorId)
    }

    suspend fun getMisPedidos(): Response<ApiResponse<List<PedidoDto>>> {
        return ApiClient.tiendaService.getMisPedidos()
    }

    suspend fun crearPedido(request: CrearPedidoRequest): Response<ApiResponse<PedidoDto>> {
        return ApiClient.tiendaService.crearPedido(request)
    }

    suspend fun repetirUltimoPedido(): Response<ApiResponse<PedidoDto>> {
        return ApiClient.tiendaService.repetirUltimoPedido()
    }

    suspend fun reportarAveria(pedidoId: Long, request: ReportarAveriaRequest): Response<ApiResponse<Map<String, Any>>> {
        return ApiClient.tiendaService.reportarAveria(pedidoId, request)
    }
}
