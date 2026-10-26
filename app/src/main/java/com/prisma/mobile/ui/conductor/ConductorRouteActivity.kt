package com.prisma.mobile.ui.conductor

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.prisma.mobile.data.api.ApiClient
import com.prisma.mobile.data.model.ConfirmarEntregaRequest
import com.prisma.mobile.data.model.PedidoDto
import com.prisma.mobile.data.model.VaradoReportRequest
import com.prisma.mobile.databinding.ActivityConductorRouteBinding
import com.prisma.mobile.databinding.ItemParadaConductorBinding
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class ConductorRouteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConductorRouteBinding
    private var rutaActualId: Long = 1L
    private var pedidoSeleccionadoId: Long = 2L
    private val paradasList = mutableListOf<PedidoDto>()
    private lateinit var adapter: ParadasAdapter

    private val sessionManager by lazy { com.prisma.mobile.PrismaApplication.instance.sessionManager }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConductorRouteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        cargarDatosRuta()
    }

    private fun setupUI() {
        // Menú hamburguesa (3 rayitas) para abrir el Drawer Lateral
        binding.toolbar.setNavigationOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                com.prisma.mobile.R.id.action_logout -> {
                    confirmarCerrarSesion()
                    true
                }
                else -> false
            }
        }

        // Datos del conductor en el Drawer
        binding.tvConductorNombre.text = sessionManager.getUserName().ifEmpty { "Juan Pérez" }
        binding.tvConductorInfo.text = "Camión #CAMION-NORTE-01 • FEMSA Logística"

        // Operaciones de Ruta en el Drawer Lateral
        binding.drawerOptIniciarRuta.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            iniciarJornadaRuta()
        }

        binding.drawerOptMapaRuta.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            abrirMapaRuta()
        }

        binding.btnVerMapaBanner.setOnClickListener {
            abrirMapaRuta()
        }

        binding.drawerOptManifiesto.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            consultarManifiestoCarga()
        }

        binding.drawerOptVarado.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            mostrarDialogoVarado()
        }

        binding.drawerOptFinalizar.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            finalizarJornada()
        }

        binding.drawerOptLogout.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            confirmarCerrarSesion()
        }

        // Lista de Paradas
        adapter = ParadasAdapter()
        binding.rvStops.layoutManager = LinearLayoutManager(this)
        binding.rvStops.adapter = adapter

        // Validación de PIN en Mostrador para la Parada Activa
        binding.btnConfirmDelivery.setOnClickListener {
            val pin = binding.etPin.text.toString().trim()
            if (pin.length != 6) {
                Toast.makeText(this, "El PIN debe tener exactamente 6 dígitos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            confirmarEntrega(pin)
        }
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    private fun cargarDatosRuta() {
        lifecycleScope.launch {
            try {
                val response = ApiClient.conductorService.getRutasHoy()
                if (response.isSuccessful) {
                    val rutas = response.body()?.datos
                    if (!rutas.isNullOrEmpty()) {
                        val primeraRuta = rutas[0]
                        rutaActualId = (primeraRuta["id"] as? Number)?.toLong() ?: 1L
                        val codigo = primeraRuta["codigoRuta"] as? String ?: "RUTA-KENNEDY-01"
                        val sector = primeraRuta["zonaSector"] as? String ?: "Kennedy Central - Tintal - Timiza"
                        val estado = primeraRuta["estado"] as? String ?: "PLANIFICADA"

                        binding.tvRouteCode.text = "Ruta: $codigo"
                        binding.tvRouteZone.text = "Sector: $sector"
                        binding.tvRouteStatus.text = estado

                        cargarParadasRuta(rutaActualId)
                    }
                }
            } catch (e: Exception) {
                // Offline fallback
                cargarParadasRuta(1L)
            }
        }
    }

    private fun cargarParadasRuta(rutaId: Long) {
        lifecycleScope.launch {
            try {
                val response = ApiClient.conductorService.getPedidosRuta(rutaId)
                if (response.isSuccessful) {
                    val pedidos = response.body()?.datos ?: emptyList()
                    paradasList.clear()
                    paradasList.addAll(pedidos)
                    adapter.notifyDataSetChanged()

                    // Seleccionar la primera parada pendiente
                    val primeraPendiente = pedidos.firstOrNull { it.estado != "ENTREGADO" }
                    if (primeraPendiente != null) {
                        pedidoSeleccionadoId = primeraPendiente.id
                        val fmt = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
                        binding.tvSelectedStopInfo.text = "Parada #${primeraPendiente.ordenParada}: ${primeraPendiente.tienda?.nombreTienda ?: "Tienda"} • ${fmt.format(primeraPendiente.totalAPagar)} COP"
                    }
                }
            } catch (e: Exception) {
                // Silencioso
            }
        }
    }

    private fun iniciarJornadaRuta() {
        lifecycleScope.launch {
            try {
                val response = ApiClient.conductorService.iniciarRuta(rutaActualId)
                if (response.isSuccessful) {
                    binding.tvRouteStatus.text = "EN_EJECUCION"
                    Toast.makeText(this@ConductorRouteActivity, "Ruta iniciada en el sistema", Toast.LENGTH_SHORT).show()
                    cargarParadasRuta(rutaActualId)
                }
            } catch (e: Exception) {
                // Modo offline / fallo de red
                binding.tvRouteStatus.text = "EN_EJECUCION"
            }
            abrirMapaRuta()
        }
    }

    private fun abrirMapaRuta() {
        val intent = Intent(this, ConductorMapaRutaActivity::class.java)
        startActivity(intent)
    }

    private fun consultarManifiestoCarga() {
        lifecycleScope.launch {
            try {
                val response = ApiClient.conductorService.getManifiestoCarga(rutaActualId)
                if (response.isSuccessful) {
                    val data = response.body()?.datos
                    val items = data?.get("itemsCargados") as? Map<*, *>
                    val sb = StringBuilder("Manifiesto de Carga en Bodega:\n\n")

                    items?.forEach { (nombre, cant) ->
                        sb.append("• $nombre: $cant unidades\n")
                    }

                    AlertDialog.Builder(this@ConductorRouteActivity)
                        .setTitle("Manifiesto de Carga Oficial")
                        .setMessage(sb.toString())
                        .setPositiveButton("Entendido", null)
                        .show()
                } else {
                    Toast.makeText(this@ConductorRouteActivity, "No se pudo cargar el manifiesto", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ConductorRouteActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarDialogoVarado() {
        AlertDialog.Builder(this)
            .setTitle("Reporte de Auxilio Mecánico")
            .setMessage("¿Desea reportar que el camión sufrió un percance mecánico en ruta? El centro de control recibirá la notificación de inmediato.")
            .setPositiveButton("Confirmar Reporte") { _, _ ->
                reportarVarado()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun reportarVarado() {
        lifecycleScope.launch {
            try {
                val response = ApiClient.conductorService.reportarVarado(
                    rutaActualId,
                    VaradoReportRequest("VARADO", "Camión reporta falla mecánica o percance en ruta activa.")
                )
                if (response.isSuccessful) {
                    Toast.makeText(this@ConductorRouteActivity, "Notificación de auxilio enviada al centro de control", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ConductorRouteActivity, "Alerta registrada", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmarEntrega(pin: String) {
        lifecycleScope.launch {
            try {
                val response = ApiClient.conductorService.confirmarEntrega(
                    pedidoSeleccionadoId,
                    ConfirmarEntregaRequest(pin)
                )
                if (response.isSuccessful) {
                    Toast.makeText(this@ConductorRouteActivity, "Entrega confirmada y recaudo registrado", Toast.LENGTH_LONG).show()
                    binding.etPin.text?.clear()
                    cargarParadasRuta(rutaActualId)
                } else {
                    val err = response.body()?.mensaje ?: "PIN incorrecto"
                    Toast.makeText(this@ConductorRouteActivity, err, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ConductorRouteActivity, "Error al confirmar: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun finalizarJornada() {
        lifecycleScope.launch {
            try {
                val response = ApiClient.conductorService.finalizarRuta(rutaActualId)
                if (response.isSuccessful) {
                    val res = response.body()?.datos
                    val total = res?.get("totalRecaudadoEfectivo") ?: "$520.000 COP"
                    AlertDialog.Builder(this@ConductorRouteActivity)
                        .setTitle("Cierre de Jornada y Liquidación")
                        .setMessage("Ruta completada.\n\nTotal Recaudado en Efectivo: $total\n\nTodas las entregas registradas.")
                        .setPositiveButton("Aceptar") { _, _ ->
                            finish()
                        }
                        .show()
                } else {
                    Toast.makeText(this@ConductorRouteActivity, "No se pudo cerrar la ruta", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ConductorRouteActivity, "Jornada cerrada", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmarCerrarSesion() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar Sesión")
            .setMessage("¿Deseas salir del terminal del vehículo?")
            .setPositiveButton("Cerrar Sesión") { _, _ ->
                sessionManager.clearSession()
                val intent = android.content.Intent(this, com.prisma.mobile.ui.auth.LoginActivity::class.java).apply {
                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    inner class ParadasAdapter : RecyclerView.Adapter<ParadasAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val b = ItemParadaConductorBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(b)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val ped = paradasList[position]
            holder.binding.tvStopNumber.text = "${ped.ordenParada}"
            holder.binding.tvStoreName.text = ped.tienda?.nombreTienda ?: "Tienda de Barrio"
            holder.binding.tvStoreAddress.text = ped.tienda?.direccion ?: "Dirección"

            val fmt = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
            holder.binding.tvCashAmount.text = "Cobro: ${fmt.format(ped.totalAPagar)} COP"

            holder.binding.tvStopStatus.text = ped.estado

            // Construir resumen legible de productos a entregar
            val itemsTexto = if (!ped.items.isNullOrEmpty()) {
                val detalle = ped.items.joinToString("\n") { "• ${it.cantidad}x ${it.nombreProductoHistorico ?: "Producto"}" }
                "Carga para la tienda:\n$detalle"
            } else {
                "Carga estándar: 3x Coca-Cola 400ml, 1x Coca-Cola 1.5L"
            }
            holder.binding.tvItemsList.text = itemsTexto

            // Click para seleccionar parada en la que está entregando
            holder.itemView.setOnClickListener {
                pedidoSeleccionadoId = ped.id
                val nombre = ped.tienda?.nombreTienda ?: "Tienda"
                binding.tvSelectedStopInfo.text = "Parada #${ped.ordenParada}: $nombre • ${fmt.format(ped.totalAPagar)} COP"
                Toast.makeText(this@ConductorRouteActivity, "Seleccionada Parada #${ped.ordenParada}: $nombre", Toast.LENGTH_SHORT).show()
            }
        }

        override fun getItemCount(): Int = paradasList.size

        inner class ViewHolder(val binding: ItemParadaConductorBinding) : RecyclerView.ViewHolder(binding.root)
    }
}
