package com.prisma.mobile.data.model

import com.google.gson.annotations.SerializedName

data class PedidoDto(
    @SerializedName("id") val id: Long,
    @SerializedName("numeroPedido") val numeroPedido: String,
    @SerializedName("ordenParada") val ordenParada: Int,
    @SerializedName("subtotal") val subtotal: Double,
    @SerializedName("totalAPagar") val totalAPagar: Double,
    @SerializedName("estado") val estado: String,
    @SerializedName("pinEntregaHash") val pinEntregaHash: String?,
    @SerializedName("fechaEstimadaEntrega") val fechaEstimadaEntrega: String?,
    @SerializedName("tienda") val tienda: TiendaDto?,
    @SerializedName("items") val items: List<ItemPedidoDto>? = emptyList()
)

data class ItemPedidoDto(
    @SerializedName("id") val id: Long?,
    @SerializedName("nombreProductoHistorico") val nombreProductoHistorico: String?,
    @SerializedName("presentacionHistorica") val presentacionHistorica: String?,
    @SerializedName("cantidad") val cantidad: Int,
    @SerializedName("precioUnitarioHistorico") val precioUnitarioHistorico: Double?,
    @SerializedName("subtotalLinea") val subtotalLinea: Double?
)

data class TiendaDto(
    @SerializedName("id") val id: Long,
    @SerializedName("nombreTienda") val nombreTienda: String,
    @SerializedName("direccion") val direccion: String,
    @SerializedName("barrio") val barrio: String?,
    @SerializedName("telefonoTienda") val telefonoTienda: String?
)

data class ItemPedidoRequest(
    @SerializedName("productoId") val productoId: Long,
    @SerializedName("cantidad") val cantidad: Int
)

data class CrearPedidoRequest(
    @SerializedName("proveedorId") val proveedorId: Long = 1L,
    @SerializedName("items") val items: List<ItemPedidoRequest>
)

data class ConfirmarEntregaRequest(
    @SerializedName("pin") val pin: String,
    @SerializedName("medioPago") val medioPago: String = "EFECTIVO",
    @SerializedName("notasEntrega") val notasEntrega: String = "Entrega registrada desde App Móvil"
)

data class VaradoReportRequest(
    @SerializedName("accion") val accion: String = "VARADO",
    @SerializedName("motivo") val motivo: String = "Falla mecánica reportada desde terminal de camión"
)

data class ReportarAveriaRequest(
    @SerializedName("descripcion") val descripcion: String
)
