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
    @SerializedName("items") val items: List<ItemPedidoDto>? = emptyList(),
    // N-08: pago en línea (pasarela simulada). null = sin pago en línea (contra entrega)
    @SerializedName("estadoPago") val estadoPago: String? = null,
    @SerializedName("medioPagoEnLinea") val medioPagoEnLinea: String? = null,
    @SerializedName("detallePago") val detallePago: String? = null,
    @SerializedName("referenciaPago") val referenciaPago: String? = null
) {
    /** Pagado en línea: el dinero está en garantía o ya se liberó al proveedor. */
    val pagadoEnLinea: Boolean
        get() = estadoPago == EstadoPago.EN_GARANTIA || estadoPago == EstadoPago.LIBERADO

    /** Se puede pagar en línea mientras no esté pagado ni entregado/cancelado. */
    val admitePagoEnLinea: Boolean
        get() = !pagadoEnLinea && estado in setOf("CREADO", "ACEPTADO", "CARGADO_EN_BODEGA", "EN_RUTA")
}

object EstadoPago {
    const val PENDIENTE = "PENDIENTE"
    const val EN_GARANTIA = "EN_GARANTIA"
    const val RECHAZADO = "RECHAZADO"
    const val LIBERADO = "LIBERADO"
    /** El pedido pagado se canceló o rechazó antes de la entrega: el dinero volvió al tendero. */
    const val REEMBOLSADO = "REEMBOLSADO"
}

/** Cuerpo del checkout simulado. El CVV nunca se envía al servidor. */
data class PagarPedidoRequest(
    @SerializedName("medio") val medio: String,
    @SerializedName("celular") val celular: String? = null,
    @SerializedName("banco") val banco: String? = null,
    @SerializedName("tipoPersona") val tipoPersona: String? = null,
    @SerializedName("documento") val documento: String? = null,
    @SerializedName("numeroTarjeta") val numeroTarjeta: String? = null,
    @SerializedName("titular") val titular: String? = null,
    @SerializedName("vencimiento") val vencimiento: String? = null,
    @SerializedName("cuotas") val cuotas: Int? = null
)

data class PagoPedidoDto(
    @SerializedName("pedidoId") val pedidoId: Long,
    @SerializedName("numeroPedido") val numeroPedido: String?,
    @SerializedName("referencia") val referencia: String,
    @SerializedName("transaccionId") val transaccionId: String,
    @SerializedName("medio") val medio: String,
    @SerializedName("detalleMedio") val detalleMedio: String,
    @SerializedName("monto") val monto: Double,
    // HU-13: división del pago (3.5% PRISMA / 96.5% proveedor) y tarifa de la pasarela aparte
    @SerializedName("porcentajeComision") val porcentajeComision: Double? = null,
    @SerializedName("comisionPrisma") val comisionPrisma: Double? = null,
    @SerializedName("montoProveedor") val montoProveedor: Double? = null,
    @SerializedName("tarifaPasarela") val tarifaPasarela: Double? = null,
    @SerializedName("confirmadoPorWebhook") val confirmadoPorWebhook: Boolean = false,
    @SerializedName("estado") val estado: String,
    @SerializedName("aprobado") val aprobado: Boolean,
    @SerializedName("motivoRechazo") val motivoRechazo: String?,
    @SerializedName("creadoEn") val creadoEn: String?
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
    @SerializedName("items") val items: List<ItemPedidoRequest>,
    @SerializedName("idempotencyKey") val idempotencyKey: String? = java.util.UUID.randomUUID().toString()
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
