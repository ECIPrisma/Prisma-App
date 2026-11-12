package com.prisma.mobile

import com.prisma.mobile.data.model.EstadoPago
import com.prisma.mobile.data.model.PedidoDto
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** N-08: reglas de la app para mostrar el pago en línea (pasarela simulada). */
class PagoEnLineaModelTest {

    private fun pedido(estado: String, estadoPago: String?) = PedidoDto(
        id = 1L, numeroPedido = "PED-2026-0001", ordenParada = 1, subtotal = 125000.0, totalAPagar = 125000.0,
        estado = estado, pinEntregaHash = "123456", fechaEstimadaEntrega = null, tienda = null,
        estadoPago = estadoPago
    )

    @Test
    fun pedidoSinPagoAdmitePagoEnLinea() {
        val p = pedido("CREADO", null)
        assertFalse(p.pagadoEnLinea)
        assertTrue(p.admitePagoEnLinea)
    }

    @Test
    fun pedidoEnGarantiaNoSePagaDeNuevo() {
        val p = pedido("EN_RUTA", EstadoPago.EN_GARANTIA)
        assertTrue(p.pagadoEnLinea)
        assertFalse(p.admitePagoEnLinea)
    }

    @Test
    fun pagoRechazadoPermiteReintentar() {
        val p = pedido("CREADO", EstadoPago.RECHAZADO)
        assertFalse(p.pagadoEnLinea)
        assertTrue(p.admitePagoEnLinea)
    }

    @Test
    fun pedidoEntregadoNoAdmitePago() {
        assertFalse(pedido("ENTREGADO", null).admitePagoEnLinea)
        assertTrue(pedido("ENTREGADO", EstadoPago.LIBERADO).pagadoEnLinea)
    }
}
