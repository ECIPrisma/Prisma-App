package com.prisma.mobile.data.repository

import com.prisma.mobile.data.api.ApiClient
import com.prisma.mobile.data.model.*
import retrofit2.Response

class ConductorRepository {
    suspend fun getRutasHoy(): Response<ApiResponse<List<Map<String, Any>>>> {
        return ApiClient.conductorService.getRutasHoy()
    }

    suspend fun iniciarRuta(rutaId: Long): Response<ApiResponse<Map<String, Any>>> {
        return ApiClient.conductorService.iniciarRuta(rutaId)
    }

    suspend fun getPedidosRuta(rutaId: Long): Response<ApiResponse<List<PedidoDto>>> {
        return ApiClient.conductorService.getPedidosRuta(rutaId)
    }

    suspend fun confirmarEntrega(pedidoId: Long, request: ConfirmarEntregaRequest): Response<ApiResponse<PedidoDto>> {
        return ApiClient.conductorService.confirmarEntrega(pedidoId, request)
    }

    suspend fun finalizarRuta(rutaId: Long): Response<ApiResponse<Map<String, Any>>> {
        return ApiClient.conductorService.finalizarRuta(rutaId)
    }

    suspend fun reportarVarado(rutaId: Long, request: VaradoReportRequest): Response<ApiResponse<Map<String, Any>>> {
        return ApiClient.conductorService.reportarVarado(rutaId, request)
    }
}
