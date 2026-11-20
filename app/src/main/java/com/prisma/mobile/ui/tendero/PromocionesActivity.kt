package com.prisma.mobile.ui.tendero

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.prisma.mobile.PrismaApplication
import com.prisma.mobile.data.api.ApiClient
import com.prisma.mobile.data.model.CrearPedidoRequest
import com.prisma.mobile.data.model.ItemPedidoRequest
import com.prisma.mobile.data.model.ProductoDto
import com.prisma.mobile.databinding.ActivityPromocionesBinding
import kotlinx.coroutines.launch

class PromocionesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPromocionesBinding
    private lateinit var adapter: ProductoAdapter
    private val sessionManager by lazy { PrismaApplication.instance.sessionManager }
    private var listaPromos = listOf<ProductoDto>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPromocionesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        cargarPromociones()
    }

    private fun setupUI() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        adapter = ProductoAdapter { prod, cant ->
            actualizarResumenCarrito()
        }

        binding.rvPromociones.layoutManager = LinearLayoutManager(this)
        binding.rvPromociones.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener {
            cargarPromociones()
        }

        binding.fabCart.setOnClickListener {
            val itemsSeleccionados = adapter.getCantidadesSeleccionadas()
            if (itemsSeleccionados.isEmpty()) {
                Toast.makeText(this, "Selecciona al menos 1 producto en oferta", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            mostrarDialogoCheckout(itemsSeleccionados)
        }
    }

    private fun actualizarResumenCarrito() {
        val items = adapter.getCantidadesSeleccionadas()
        val totalUnidades = items.values.sum()
        binding.fabCart.text = if (totalUnidades > 0) "Comprar Ofertas ($totalUnidades)" else "Carrito Vacío"
    }

    private fun cargarPromociones() {
        binding.swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            try {
                val fullList = sessionManager.getOfflineCatalog().ifEmpty {
                    sessionManager.getDefaultCatalog()
                }

                // Filtrar exclusivamente productos marcados en promoción
                listaPromos = fullList.filter { it.esPromocion || (it.precioSugeridoPublico != null && it.precioSugeridoPublico > it.precioUnitario) }
                    .map { it.copy(esPromocion = true) }

                adapter.setProductos(listaPromos)
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun mostrarDialogoCheckout(items: Map<Long, Int>) {
        val totalItems = items.values.sum()
        AlertDialog.Builder(this)
            .setTitle("Confirmar Pedido de Ofertas")
            .setMessage("Vas a solicitar $totalItems unidades en promoción para entrega en tu próxima ruta.\n\nPago: Efectivo contra entrega en mostrador.\n\n¿Deseas radicar el pedido?")
            .setPositiveButton("Confirmar Pedido") { _, _ -> enviarPedido(items) }
            .setNegativeButton("Seguir viendo", null)
            .show()
    }

    private fun enviarPedido(items: Map<Long, Int>) {
        binding.fabCart.isEnabled = false
        val lineas = items.map { (prodId, cant) -> ItemPedidoRequest(prodId, cant) }
        val req = CrearPedidoRequest(proveedorId = 1L, items = lineas)

        lifecycleScope.launch {
            try {
                val resp = ApiClient.tiendaService.crearPedido(req)
                if (resp.isSuccessful && resp.body()?.exitoso == true) {
                    val ped = resp.body()?.datos
                    AlertDialog.Builder(this@PromocionesActivity)
                        .setTitle("¡Pedido de Promociones Radicado!")
                        .setMessage("Tu orden #${ped?.numeroPedido ?: ""} ha sido recibida con éxito por el proveedor.\n\nPuedes consultar el estado y tu PIN de entrega en 'Mis Pedidos'.")
                        .setPositiveButton("Ver Mis Pedidos") { _, _ ->
                            startActivity(android.content.Intent(this@PromocionesActivity, MisPedidosActivity::class.java))
                            finish()
                        }
                        .setNegativeButton("Aceptar") { _, _ -> finish() }
                        .setCancelable(false)
                        .show()
                } else {
                    sessionManager.enqueueOfflineOrder(req)
                    Toast.makeText(this@PromocionesActivity, "Sin conexión: Pedido de promociones guardado localmente", Toast.LENGTH_LONG).show()
                    finish()
                }
            } catch (e: Exception) {
                sessionManager.enqueueOfflineOrder(req)
                Toast.makeText(this@PromocionesActivity, "Modo sin conexión: Pedido de promociones encolado", Toast.LENGTH_LONG).show()
                finish()
            } finally {
                binding.fabCart.isEnabled = true
            }
        }
    }
}
