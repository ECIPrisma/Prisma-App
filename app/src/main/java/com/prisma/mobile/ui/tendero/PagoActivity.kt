package com.prisma.mobile.ui.tendero

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.prisma.mobile.R
import com.prisma.mobile.data.api.ApiClient
import com.prisma.mobile.data.model.EstadoPago
import com.prisma.mobile.data.model.PagarPedidoRequest
import com.prisma.mobile.data.model.PagoPedidoDto
import com.prisma.mobile.databinding.ActivityPagoBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale

/**
 * N-08 – Checkout en línea del tendero (Nequi / PSE / tarjeta) contra la pasarela SIMULADA del backend.
 * No mueve dinero real: el backend responde como el sandbox de Wompi con datos de prueba.
 */
class PagoActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PEDIDO_ID = "extra_pedido_id"
        const val EXTRA_NUMERO_PEDIDO = "extra_numero_pedido"
        const val EXTRA_TOTAL = "extra_total"

        private val BANCOS = listOf(
            "Bancolombia", "Banco de Bogotá", "Davivienda", "BBVA Colombia", "Banco de Occidente",
            "Banco Popular", "Banco Caja Social", "Nu Colombia", "Banco que aprueba", "Banco que rechaza"
        )
        private val CUOTAS = (1..12).map { if (it == 1) "1 cuota" else "$it cuotas" }

        fun intent(context: Context, pedidoId: Long, numeroPedido: String?, total: Double): Intent =
            Intent(context, PagoActivity::class.java)
                .putExtra(EXTRA_PEDIDO_ID, pedidoId)
                .putExtra(EXTRA_NUMERO_PEDIDO, numeroPedido)
                .putExtra(EXTRA_TOTAL, total)
    }

    private enum class Medio { NEQUI, PSE, TARJETA }

    private lateinit var binding: ActivityPagoBinding
    private val fmt = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply { maximumFractionDigits = 0 }

    private var pedidoId: Long = -1
    private var numeroPedido: String = ""
    private var total: Double = 0.0
    private var medio = Medio.NEQUI
    private var procesando = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPagoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        pedidoId = intent.getLongExtra(EXTRA_PEDIDO_ID, -1)
        numeroPedido = intent.getStringExtra(EXTRA_NUMERO_PEDIDO) ?: ""
        total = intent.getDoubleExtra(EXTRA_TOTAL, 0.0)
        if (pedidoId <= 0) {
            finish()
            return
        }

        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // No se permite salir mientras la pasarela procesa la transacción
                if (!procesando) finish()
            }
        })

        binding.tvResumenPedido.text = "PEDIDO #$numeroPedido"
        binding.tvTotalPagar.text = "${fmt.format(total)} COP"
        binding.btnPagar.text = "Pagar ${fmt.format(total)}"

        binding.actBanco.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, BANCOS))
        binding.actCuotas.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, CUOTAS))
        binding.actCuotas.setText(CUOTAS[0], false)

        binding.optNequi.setOnClickListener { seleccionarMedio(Medio.NEQUI) }
        binding.optPse.setOnClickListener { seleccionarMedio(Medio.PSE) }
        binding.optTarjeta.setOnClickListener { seleccionarMedio(Medio.TARJETA) }

        binding.etNumeroTarjeta.addTextChangedListener(FormatoGrupos(binding.etNumeroTarjeta))
        binding.etVencimiento.addTextChangedListener(FormatoVencimiento(binding.etVencimiento))

        binding.btnAutocompletar.setOnClickListener { autocompletarDatosPrueba() }
        binding.btnPagar.setOnClickListener { pagar() }

        seleccionarMedio(Medio.NEQUI)
    }

    // ------------------------------------------------------------------ Selección de medio

    private fun seleccionarMedio(nuevo: Medio) {
        medio = nuevo
        val opciones = mapOf(Medio.NEQUI to binding.optNequi, Medio.PSE to binding.optPse, Medio.TARJETA to binding.optTarjeta)
        opciones.forEach { (m, vista) ->
            vista.background = ContextCompat.getDrawable(
                this, if (m == nuevo) R.drawable.bg_option_item_selected else R.drawable.bg_option_item
            )
        }
        binding.formNequi.visibility = if (nuevo == Medio.NEQUI) View.VISIBLE else View.GONE
        binding.formPse.visibility = if (nuevo == Medio.PSE) View.VISIBLE else View.GONE
        binding.formTarjeta.visibility = if (nuevo == Medio.TARJETA) View.VISIBLE else View.GONE

        binding.tvDatosPrueba.text = when (nuevo) {
            Medio.NEQUI -> "Aprobado: 399 111 1111\nRechazado: 399 222 2222"
            Medio.PSE -> "Aprobado: cualquier banco o \"Banco que aprueba\"\nRechazado: \"Banco que rechaza\""
            Medio.TARJETA -> "Aprobada: 4242 4242 4242 4242\nRechazada: 4111 1111 1111 1111\nCualquier fecha futura y cualquier CVV"
        }
        limpiarErrores()
    }

    private fun autocompletarDatosPrueba() {
        when (medio) {
            Medio.NEQUI -> binding.etCelular.setText("3991111111")
            Medio.PSE -> {
                binding.actBanco.setText("Banco que aprueba", false)
                binding.etDocumento.setText("1020304050")
            }
            Medio.TARJETA -> {
                binding.etNumeroTarjeta.setText("4242424242424242")
                binding.etTitular.setText("MARTA RODRIGUEZ")
                binding.etVencimiento.setText("1230")
                binding.etCvv.setText("123")
            }
        }
        limpiarErrores()
    }

    // ------------------------------------------------------------------ Validación y pago

    private fun limpiarErrores() {
        listOf(binding.tilCelular, binding.tilBanco, binding.tilDocumento, binding.tilNumeroTarjeta,
            binding.tilTitular, binding.tilVencimiento, binding.tilCvv).forEach { it.error = null }
    }

    private fun texto(et: EditText) = et.text?.toString()?.trim().orEmpty()
    private fun digitos(et: EditText) = texto(et).filter { it.isDigit() }

    /** Valida en el cliente y arma la solicitud; devuelve null si hay errores (se muestran en los campos). */
    private fun construirSolicitud(): PagarPedidoRequest? {
        limpiarErrores()
        return when (medio) {
            Medio.NEQUI -> {
                val celular = digitos(binding.etCelular)
                if (celular.length != 10 || !celular.startsWith("3")) {
                    binding.tilCelular.error = "Ingresa un celular de 10 dígitos que empiece por 3"
                    null
                } else PagarPedidoRequest(medio = "NEQUI", celular = celular)
            }
            Medio.PSE -> {
                val banco = binding.actBanco.text?.toString()?.trim().orEmpty()
                val documento = digitos(binding.etDocumento)
                var ok = true
                if (banco.isEmpty()) { binding.tilBanco.error = "Selecciona tu banco"; ok = false }
                if (documento.length !in 5..15) { binding.tilDocumento.error = "Documento inválido"; ok = false }
                val persona = if (binding.chipPersonaJuridica.isChecked) "JURIDICA" else "NATURAL"
                if (ok) PagarPedidoRequest(medio = "PSE", banco = banco, tipoPersona = persona, documento = documento) else null
            }
            Medio.TARJETA -> {
                val numero = digitos(binding.etNumeroTarjeta)
                val titular = texto(binding.etTitular)
                val vencimiento = texto(binding.etVencimiento)
                val cvv = digitos(binding.etCvv)
                var ok = true
                if (numero.length !in 13..19 || !luhnValido(numero)) {
                    binding.tilNumeroTarjeta.error = "Número de tarjeta inválido"; ok = false
                }
                if (titular.length < 3) { binding.tilTitular.error = "Ingresa el nombre del titular"; ok = false }
                if (!vencimientoValido(vencimiento)) { binding.tilVencimiento.error = "Fecha inválida"; ok = false }
                if (cvv.length !in 3..4) { binding.tilCvv.error = "CVV inválido"; ok = false }
                val cuotas = binding.actCuotas.text?.toString()?.filter { it.isDigit() }?.toIntOrNull() ?: 1
                // El CVV solo se valida en el dispositivo; nunca se envía al servidor
                if (ok) PagarPedidoRequest(medio = "TARJETA", numeroTarjeta = numero, titular = titular,
                    vencimiento = vencimiento, cuotas = cuotas) else null
            }
        }
    }

    private fun pagar() {
        val solicitud = construirSolicitud() ?: return
        mostrarProcesando(true)

        lifecycleScope.launch {
            // Pasos visuales que imitan el recorrido real de una pasarela
            binding.tvPasoProcesando.text = "Conectando con la pasarela de pagos..."
            delay(700)
            binding.tvPasoProcesando.text = when (medio) {
                Medio.NEQUI -> "Esperando aprobación en tu app Nequi..."
                Medio.PSE -> "Autorizando el débito con tu banco..."
                Medio.TARJETA -> "Validando la tarjeta con el banco emisor..."
            }
            try {
                val respuesta = ApiClient.tiendaService.pagarPedido(pedidoId, solicitud)
                delay(900)
                val pago = respuesta.body()?.datos
                if (respuesta.isSuccessful && pago != null) {
                    mostrarResultado(pago)
                } else {
                    mostrarProcesando(false)
                    mostrarErrorServidor(extraerMensaje(respuesta.errorBody()?.string())
                        ?: "No se pudo procesar el pago, intenta nuevamente")
                }
            } catch (e: Exception) {
                mostrarProcesando(false)
                mostrarErrorServidor("Sin conexión con la pasarela. Revisa tu señal e intenta de nuevo.")
            }
        }
    }

    private fun mostrarProcesando(activo: Boolean) {
        procesando = activo
        binding.overlayProcesando.visibility = if (activo) View.VISIBLE else View.GONE
        binding.btnPagar.isEnabled = !activo
    }

    private fun mostrarErrorServidor(mensaje: String) {
        com.google.android.material.snackbar.Snackbar.make(binding.root, mensaje, com.google.android.material.snackbar.Snackbar.LENGTH_LONG).show()
    }

    private fun extraerMensaje(json: String?): String? = try {
        json?.let { JSONObject(it).optString("mensaje").takeIf { m -> m.isNotBlank() } }
    } catch (_: Exception) {
        null
    }

    // ------------------------------------------------------------------ Resultado

    private fun mostrarResultado(pago: PagoPedidoDto) {
        mostrarProcesando(false)
        binding.scrollCheckout.visibility = View.GONE
        binding.scrollResultado.visibility = View.VISIBLE
        binding.toolbar.title = if (pago.aprobado) "Pago Aprobado" else "Pago Rechazado"

        val detalle = buildString {
            appendLine("Pedido      #${pago.numeroPedido ?: numeroPedido}")
            appendLine("Monto       ${fmt.format(pago.monto)} COP")
            appendLine("Medio       ${pago.detalleMedio}")
            appendLine("Referencia  ${pago.referencia}")
            append("Transacción ${pago.transaccionId}")
        }
        binding.tvDetalleResultado.text = detalle

        if (pago.aprobado) {
            binding.tvIconoResultado.text = "✓"
            binding.tvIconoResultado.setTextColor(ContextCompat.getColor(this, R.color.status_green))
            binding.tvIconoResultado.background = ContextCompat.getDrawable(this, R.drawable.bg_circle_success)
            binding.tvTituloResultado.text = "¡Pago aprobado!"
            binding.tvMensajeResultado.text =
                "Tu dinero queda en garantía. El proveedor lo recibe cuando confirmes la entrega con tu PIN."
            binding.tvEstadoPagoBadge.text = if (pago.estado == EstadoPago.LIBERADO) "PAGADO • LIBERADO" else "PAGADO • EN GARANTÍA"
            binding.tvEstadoPagoBadge.background = ContextCompat.getDrawable(this, R.drawable.badge_pill_green)
            binding.tvEstadoPagoBadge.setTextColor(ContextCompat.getColor(this, R.color.status_green_text))

            binding.btnAccionPrincipal.text = "Ver mis pedidos y PIN"
            binding.btnAccionPrincipal.setOnClickListener { irAMisPedidos() }
            binding.btnAccionSecundaria.visibility = View.GONE
        } else {
            binding.tvIconoResultado.text = "✕"
            binding.tvIconoResultado.setTextColor(ContextCompat.getColor(this, R.color.status_red))
            binding.tvIconoResultado.background = ContextCompat.getDrawable(this, R.drawable.bg_circle_error)
            binding.tvTituloResultado.text = "Pago rechazado"
            binding.tvMensajeResultado.text =
                "${pago.motivoRechazo ?: "La transacción no fue aprobada"}.\nTu pedido sigue activo: puedes intentar con otro medio o pagar contra entrega."
            binding.tvEstadoPagoBadge.text = "RECHAZADO"
            binding.tvEstadoPagoBadge.background = ContextCompat.getDrawable(this, R.drawable.badge_pill_red)
            binding.tvEstadoPagoBadge.setTextColor(ContextCompat.getColor(this, R.color.status_red_text))

            binding.btnAccionPrincipal.text = "Intentar de nuevo"
            binding.btnAccionPrincipal.setOnClickListener { volverAlCheckout() }
            binding.btnAccionSecundaria.visibility = View.VISIBLE
            binding.btnAccionSecundaria.text = "Pagar contra entrega"
            binding.btnAccionSecundaria.setOnClickListener { irAMisPedidos() }
        }
    }

    private fun volverAlCheckout() {
        binding.scrollResultado.visibility = View.GONE
        binding.scrollCheckout.visibility = View.VISIBLE
        binding.toolbar.title = "Pago en Línea"
    }

    private fun irAMisPedidos() {
        startActivity(Intent(this, MisPedidosActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
        finish()
    }

    // ------------------------------------------------------------------ Utilidades

    private fun luhnValido(numero: String): Boolean {
        var suma = 0
        var duplicar = false
        for (i in numero.length - 1 downTo 0) {
            var d = numero[i] - '0'
            if (duplicar) {
                d *= 2
                if (d > 9) d -= 9
            }
            suma += d
            duplicar = !duplicar
        }
        return suma % 10 == 0
    }

    private fun vencimientoValido(valor: String): Boolean {
        val partes = valor.split("/")
        if (partes.size != 2 || partes[0].length != 2 || partes[1].length != 2) return false
        val mes = partes[0].toIntOrNull() ?: return false
        val anio = partes[1].toIntOrNull() ?: return false
        if (mes !in 1..12) return false
        // Se usa Calendar (no java.time) para soportar minSdk 24
        val fecha = java.util.Calendar.getInstance()
        val anioActual = fecha.get(java.util.Calendar.YEAR) % 100
        val mesActual = fecha.get(java.util.Calendar.MONTH) + 1
        return anio > anioActual || (anio == anioActual && mes >= mesActual)
    }

    /** Agrupa el número de tarjeta de a 4 dígitos: 4242 4242 4242 4242 */
    private class FormatoGrupos(private val campo: EditText) : TextWatcher {
        private var editando = false
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            if (editando || s == null) return
            editando = true
            val formateado = s.filter { it.isDigit() }.take(19).chunked(4).joinToString(" ")
            if (formateado != s.toString()) {
                campo.setText(formateado)
                campo.setSelection(formateado.length)
            }
            editando = false
        }
    }

    /** Inserta la barra del vencimiento: 1230 → 12/30 */
    private class FormatoVencimiento(private val campo: EditText) : TextWatcher {
        private var editando = false
        private var borrando = false
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            borrando = count > after
        }
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            if (editando || s == null || borrando) return
            editando = true
            val d = s.filter { it.isDigit() }.take(4)
            val formateado = if (d.length >= 2) "${d.take(2)}/${d.drop(2)}" else d.toString()
            if (formateado != s.toString()) {
                campo.setText(formateado)
                campo.setSelection(formateado.length)
            }
            editando = false
        }
    }
}
