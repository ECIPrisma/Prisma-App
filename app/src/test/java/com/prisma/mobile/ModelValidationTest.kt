package com.prisma.mobile

import com.prisma.mobile.data.model.ItemPedidoDto
import com.prisma.mobile.data.model.PedidoDto
import com.prisma.mobile.data.model.ProductoDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ModelValidationTest {

    @Test
    fun testProductoModelCreation() {
        val prod = ProductoDto(
            id = 10L,
            sku = "SKU-001",
            nombre = "Coca-Cola 1.5L",
            presentacion = "Botella 1.5L",
            precioUnitario = 5000.0,
            precioSugeridoPublico = 6000.0,
            stockDisponible = 24,
            imagenUrl = "/logo.png"
        )
        assertEquals("Coca-Cola 1.5L", prod.nombre)
        assertEquals(5000.0, prod.precioUnitario, 0.001)
        assertEquals(24, prod.stockDisponible)
    }

    @Test
    fun testPedidoModelCalculation() {
        val item1 = ItemPedidoDto(
            id = 1L,
            nombreProductoHistorico = "Gaseosa",
            presentacionHistorica = "Unidad",
            cantidad = 2,
            precioUnitarioHistorico = 4000.0,
            subtotalLinea = 8000.0
        )
        val pedido = PedidoDto(
            id = 100L,
            numeroPedido = "PED-2026-0001",
            ordenParada = 1,
            subtotal = 8000.0,
            totalAPagar = 8000.0,
            estado = "CREADO",
            pinEntregaHash = "123456",
            fechaEstimadaEntrega = "2026-10-26",
            tienda = null,
            items = listOf(item1)
        )
        assertNotNull(pedido.items)
        assertEquals(1, pedido.items?.size)
        assertEquals(8000.0, pedido.totalAPagar, 0.001)
    }
}
