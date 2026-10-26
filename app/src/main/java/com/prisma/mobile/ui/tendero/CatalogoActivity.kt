package com.prisma.mobile.ui.tendero

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.prisma.mobile.data.api.ApiClient
import com.prisma.mobile.data.model.CrearPedidoRequest
import com.prisma.mobile.data.model.ItemPedidoRequest
import com.prisma.mobile.databinding.ActivityCatalogoBinding
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class CatalogoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCatalogoBinding
    private lateinit var adapter: ProductoAdapter
    private val sessionManager by lazy { com.prisma.mobile.PrismaApplication.instance.sessionManager }
    private var listaOriginalProductos = listOf<com.prisma.mobile.data.model.ProductoDto>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCatalogoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        cargarCatalogo()
    }

    private fun setupUI() {
        binding.toolbar.title = "PRISMA - Mi Tienda"
        binding.tvTenderoInfo.text = "${sessionManager.getUserName()} • Suscrito a FEMSA"

        // 3 rayitas (Menú hamburguesa): abre el menú lateral con todas las operaciones
        binding.toolbar.setNavigationOnClickListener {
            binding.drawerLayout.openDrawer(androidx.core.view.GravityCompat.START)
        }

        // Menú de opciones de la toolbar: Cerrar Sesión directo
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                com.prisma.mobile.R.id.action_logout -> {
                    confirmarCerrarSesion()
                    true
                }
                else -> false
            }
        }

        // Operaciones del Menú Lateral (Drawer)
        binding.drawerOptPedidos.setOnClickListener {
            binding.drawerLayout.closeDrawer(androidx.core.view.GravityCompat.START)
            startActivity(Intent(this, MisPedidosActivity::class.java))
        }

        binding.drawerOptRastreo.setOnClickListener {
            binding.drawerLayout.closeDrawer(androidx.core.view.GravityCompat.START)
            startActivity(Intent(this, RastreoCamionActivity::class.java))
        }

        binding.drawerOptLoDeSiempre.setOnClickListener {
            binding.drawerLayout.closeDrawer(androidx.core.view.GravityCompat.START)
            repetirUltimoPedido()
        }

        binding.drawerOptLogout.setOnClickListener {
            binding.drawerLayout.closeDrawer(androidx.core.view.GravityCompat.START)
            confirmarCerrarSesion()
        }

        adapter = ProductoAdapter { prod, cant ->
            actualizarResumenCarrito()
        }

        binding.rvProductos.layoutManager = LinearLayoutManager(this)
        binding.rvProductos.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener {
            cargarCatalogo()
        }

        // Filtros Rápidos de Categoría
        binding.chipTodos.setOnClickListener { aplicarFiltro(FiltroTipo.TODOS) }
        binding.chipSuscritos.setOnClickListener { aplicarFiltro(FiltroTipo.SUSCRITOS) }
        binding.chipPromociones.setOnClickListener { aplicarFiltro(FiltroTipo.PROMOCIONES) }

        // CU-T03 y CU-T04: Confirmar Carrito
        binding.fabCart.setOnClickListener {
            val itemsSeleccionados = adapter.getCantidadesSeleccionadas()
            if (itemsSeleccionados.isEmpty()) {
                Toast.makeText(this, "Selecciona al menos 1 producto para comprar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            mostrarDialogoCheckout(itemsSeleccionados)
        }
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(androidx.core.view.GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(androidx.core.view.GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    private fun actualizarResumenCarrito() {
        val items = adapter.getCantidadesSeleccionadas()
        val totalUnidades = items.values.sum()
        binding.fabCart.text = if (totalUnidades > 0) "Ver Carrito ($totalUnidades)" else "Carrito Vacío"
    }

    private enum class FiltroTipo { TODOS, SUSCRITOS, PROMOCIONES }

    private fun aplicarFiltro(tipo: FiltroTipo) {
        val filtrados = when (tipo) {
            FiltroTipo.TODOS -> listaOriginalProductos
            FiltroTipo.SUSCRITOS -> listaOriginalProductos.filter { it.esHabitual || it.sku.contains("KO-") }
            FiltroTipo.PROMOCIONES -> listaOriginalProductos.filter { it.esPromocion || it.precioSugeridoPublico != null }
        }
        adapter.setProductos(filtrados)
        if (filtrados.isEmpty()) {
            Toast.makeText(this, "No hay productos en esta sección", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmarCerrarSesion() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar Sesión")
            .setMessage("¿Deseas salir de tu cuenta de tendero?")
            .setPositiveButton("Cerrar Sesión") { _, _ ->
                sessionManager.clearSession()
                val intent = Intent(this, com.prisma.mobile.ui.auth.LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun cargarCatalogo() {
        binding.swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            try {
                // Proveedor FEMSA (ID 1) por defecto
                val response = ApiClient.tiendaService.getCatalogo(1L)
                if (response.isSuccessful) {
                    val rawList = response.body()?.datos ?: emptyList()
                    // Marcar como habituales los productos suscritos frecuentes (ej: Coca-Cola) y promociones
                    listaOriginalProductos = rawList.map { prod ->
                        val habitual = prod.sku.contains("KO-") || prod.nombre.contains("Coca-Cola")
                        val promo = prod.precioSugeridoPublico != null && prod.precioSugeridoPublico > prod.precioUnitario
                        prod.copy(esHabitual = habitual, esPromocion = promo)
                    }
                    adapter.setProductos(listaOriginalProductos)
                } else {
                    Toast.makeText(this@CatalogoActivity, "Error al cargar catálogo", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@CatalogoActivity, "Error de red: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun mostrarDialogoCheckout(items: Map<Long, Int>) {
        val totalItems = items.values.sum()
        AlertDialog.Builder(this)
            .setTitle("Confirmar Pedido")
            .setMessage("Vas a solicitar $totalItems unidades para despacho en la próxima ruta programada.\n\nPago: Efectivo contra entrega en mostrador.\n\n¿Deseas radicar el pedido?")
            .setPositiveButton("Confirmar Pedido") { _, _ ->
                enviarPedido(items)
            }
            .setNegativeButton("Seguir comprando", null)
            .show()
    }

    private fun enviarPedido(items: Map<Long, Int>) {
        val requestList = items.map { ItemPedidoRequest(it.key, it.value) }
        lifecycleScope.launch {
            try {
                val response = ApiClient.tiendaService.crearPedido(CrearPedidoRequest(1L, requestList))
                if (response.isSuccessful && response.body()?.exitoso == true) {
                    val ped = response.body()?.datos
                    Toast.makeText(this@CatalogoActivity, "Pedido #${ped?.numeroPedido} generado con éxito", Toast.LENGTH_LONG).show()
                    // Redirigir a Mis Pedidos para ver su PIN
                    startActivity(Intent(this@CatalogoActivity, MisPedidosActivity::class.java))
                } else {
                    Toast.makeText(this@CatalogoActivity, "No se pudo procesar el pedido", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@CatalogoActivity, "Error de conexión: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun repetirUltimoPedido() {
        lifecycleScope.launch {
            try {
                val response = ApiClient.tiendaService.repetirUltimoPedido()
                if (response.isSuccessful && response.body()?.exitoso == true) {
                    val ped = response.body()?.datos
                    Toast.makeText(this@CatalogoActivity, "Pedido repetido: #${ped?.numeroPedido}", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this@CatalogoActivity, MisPedidosActivity::class.java))
                } else {
                    Toast.makeText(this@CatalogoActivity, "No hay pedidos previos para repetir", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@CatalogoActivity, "Error de red: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarHubOpcionesTendero() {
        val bottomSheet = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val sheetView = layoutInflater.inflate(com.prisma.mobile.R.layout.bottom_sheet_tendero_hub, null)
        bottomSheet.setContentView(sheetView)

        sheetView.findViewById<android.view.View>(com.prisma.mobile.R.id.hubOptPedidos)?.setOnClickListener {
            bottomSheet.dismiss()
            startActivity(Intent(this, MisPedidosActivity::class.java))
        }

        sheetView.findViewById<android.view.View>(com.prisma.mobile.R.id.hubOptPin)?.setOnClickListener {
            bottomSheet.dismiss()
            startActivity(Intent(this, MisPedidosActivity::class.java))
        }

        sheetView.findViewById<android.view.View>(com.prisma.mobile.R.id.hubOptRastreo)?.setOnClickListener {
            bottomSheet.dismiss()
            startActivity(Intent(this, RastreoCamionActivity::class.java))
        }

        sheetView.findViewById<android.view.View>(com.prisma.mobile.R.id.hubOptLoDeSiempre)?.setOnClickListener {
            bottomSheet.dismiss()
            repetirUltimoPedido()
        }

        sheetView.findViewById<android.view.View>(com.prisma.mobile.R.id.hubOptLogout)?.setOnClickListener {
            bottomSheet.dismiss()
            confirmarCerrarSesion()
        }

        bottomSheet.show()
    }
}
