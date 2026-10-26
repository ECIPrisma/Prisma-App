package com.prisma.mobile.ui.tendero

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.prisma.mobile.data.api.ApiClient
import com.prisma.mobile.data.model.PedidoDto
import com.prisma.mobile.data.model.ReportarAveriaRequest
import com.prisma.mobile.databinding.ActivityMisPedidosBinding
import com.prisma.mobile.databinding.ItemPedidoTenderoBinding
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class MisPedidosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMisPedidosBinding
    private val pedidosList = mutableListOf<PedidoDto>()
    private lateinit var adapter: PedidosAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMisPedidosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        adapter = PedidosAdapter()
        binding.rvOrders.layoutManager = LinearLayoutManager(this)
        binding.rvOrders.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener {
            cargarPedidos()
        }

        cargarPedidos()
    }

    override fun onResume() {
        super.onResume()
        cargarPedidos()
    }

    private fun cargarPedidos() {
        binding.swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            try {
                val response = ApiClient.tiendaService.getMisPedidos()
                if (response.isSuccessful) {
                    val pedidos = response.body()?.datos ?: emptyList()
                    pedidosList.clear()
                    pedidosList.addAll(pedidos)
                    adapter.notifyDataSetChanged()

                    // Buscar pedido activo con PIN para mostrar en la tarjeta destacada
                    val pedidoConPin = pedidos.firstOrNull { it.pinEntregaHash != null && it.estado != "ENTREGADO" } 
                        ?: pedidos.firstOrNull()

                    if (pedidoConPin != null && pedidoConPin.pinEntregaHash != null) {
                        val pin = pedidoConPin.pinEntregaHash
                        // Formatear con espacios entre dígitos (ej: 1 9 3 8 4 2)
                        binding.tvPinDisplay.text = pin.map { "$it " }.joinToString("").trim()
                        binding.tvPinOrderInfo.text = "Asociado al Pedido #${pedidoConPin.numeroPedido}"
                    }
                } else {
                    Toast.makeText(this@MisPedidosActivity, "No se pudo sincronizar pedidos", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MisPedidosActivity, "Error de red: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun mostrarDialogoReportarAveria(pedido: PedidoDto) {
        val input = EditText(this).apply {
            hint = "Describa la avería o producto faltante..."
            setPadding(40, 30, 40, 30)
        }

        AlertDialog.Builder(this)
            .setTitle("Reportar Novedad o Avería")
            .setMessage("Pedido #${pedido.numeroPedido}\nIndique los productos con daño para que el proveedor procese la reposición.")
            .setView(input)
            .setPositiveButton("Enviar Reporte") { _, _ ->
                val desc = input.text.toString().trim()
                if (desc.isNotEmpty()) {
                    enviarReporteAveria(pedido.id, desc)
                } else {
                    Toast.makeText(this, "Debe ingresar una descripción", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun enviarReporteAveria(pedidoId: Long, descripcion: String) {
        lifecycleScope.launch {
            try {
                val response = ApiClient.tiendaService.reportarAveria(pedidoId, ReportarAveriaRequest(descripcion))
                if (response.isSuccessful) {
                    Toast.makeText(this@MisPedidosActivity, "Novedad radicada exitosamente ante el proveedor.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@MisPedidosActivity, "Error al radicar el reporte", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MisPedidosActivity, "Error de conexión: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    inner class PedidosAdapter : RecyclerView.Adapter<PedidosAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val b = ItemPedidoTenderoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(b)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val ped = pedidosList[position]
            holder.binding.tvOrderNumber.text = "Pedido #${ped.numeroPedido}"
            holder.binding.tvStatusBadge.text = ped.estado

            val fmt = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
            holder.binding.tvTotalAmount.text = "Total: ${fmt.format(ped.totalAPagar)} COP"

            // Mostrar el PIN de entrega del pedido si está en ruta o creado
            val pin = ped.pinEntregaHash ?: "Generando..."
            holder.binding.tvPinInline.text = pin

            // Mostrar la lista de productos que trae el camión
            val itemsTexto = if (!ped.items.isNullOrEmpty()) {
                val detalle = ped.items.joinToString("\n") { "• ${it.cantidad}x ${it.nombreProductoHistorico ?: "Producto"}" }
                "Productos solicitados:\n$detalle"
            } else {
                "Productos solicitados: Coca-Cola y Manantial"
            }
            holder.binding.tvOrderProducts.text = itemsTexto

            holder.binding.btnReportAveria.setOnClickListener {
                mostrarDialogoReportarAveria(ped)
            }
        }

        override fun getItemCount(): Int = pedidosList.size

        inner class ViewHolder(val binding: ItemPedidoTenderoBinding) : RecyclerView.ViewHolder(binding.root)
    }
}
