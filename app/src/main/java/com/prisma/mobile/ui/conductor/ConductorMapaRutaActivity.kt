package com.prisma.mobile.ui.conductor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.prisma.mobile.R
import com.prisma.mobile.data.api.ApiClient
import com.prisma.mobile.data.model.ConfirmarEntregaRequest
import com.prisma.mobile.databinding.ActivityConductorMapaRutaBinding
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.text.NumberFormat
import java.util.Locale

class ConductorMapaRutaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConductorMapaRutaBinding

    // Coordenadas de los puntos en Kennedy Central, Bogotá
    private val posCamion = GeoPoint(4.6184200, -74.1568200)

    private data class LocalParada(
        val id: Long,
        val numero: Int,
        val nombre: String,
        val direccion: String,
        val punto: GeoPoint,
        val productosTexto: String,
        val totalCobro: Double,
        var entregado: Boolean = false
    )

    private val locales = mutableListOf(
        LocalParada(
            id = 2L,
            numero = 1,
            nombre = "Tienda La Bendición",
            direccion = "Calle 45 Sur # 78H-12, Kennedy Central",
            punto = GeoPoint(4.6210000, -74.1590000),
            productosTexto = "Carga a entregar:\n• 3x Coca-Cola Original 400ml\n• 1x Coca-Cola 1.5L",
            totalCobro = 126000.0
        ),
        LocalParada(
            id = 3L,
            numero = 2,
            nombre = "Minimercado San José",
            direccion = "Transversal 78K # 41D Sur, Timiza",
            punto = GeoPoint(4.6238000, -74.1565000),
            productosTexto = "Carga a entregar:\n• 2x Jugo Del Valle Manzana 300ml\n• 4x Agua Brisa 600ml",
            totalCobro = 84000.0
        ),
        LocalParada(
            id = 4L,
            numero = 3,
            nombre = "Cigarrería El Diamante",
            direccion = "Carrera 79 # 40C Sur, Tintal",
            punto = GeoPoint(4.6265000, -74.1542000),
            productosTexto = "Carga a entregar:\n• 5x Postobón Manzana 400ml\n• 2x Coca-Cola Vidrio 250ml",
            totalCobro = 310000.0
        )
    )

    private var localSeleccionadoIndex = 0
    private val storeMarkers = mutableListOf<Marker>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConductorMapaRutaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.btnVerHojaRuta.setOnClickListener {
            finish()
        }

        binding.fabCenterTruck.setOnClickListener {
            binding.mapView.controller.animateTo(posCamion)
            binding.mapView.controller.setZoom(15.5)
            Toast.makeText(this, "Centrado en el camión", Toast.LENGTH_SHORT).show()
        }

        binding.btnValidarPinLocal.setOnClickListener {
            mostrarDialogoValidarPin()
        }

        configurarChips()
        configurarMapaOSM()
        actualizarDetalleLocal(0)
    }

    private fun configurarChips() {
        binding.chipLocal1.setOnClickListener { seleccionarLocal(0) }
        binding.chipLocal2.setOnClickListener { seleccionarLocal(1) }
        binding.chipLocal3.setOnClickListener { seleccionarLocal(2) }
    }

    private fun seleccionarLocal(index: Int) {
        if (index !in locales.indices) return
        localSeleccionadoIndex = index
        actualizarDetalleLocal(index)

        // Centrar mapa suavemente en el local seleccionado
        binding.mapView.controller.animateTo(locales[index].punto)
    }

    private fun actualizarDetalleLocal(index: Int) {
        val loc = locales[index]
        val fmt = NumberFormat.getCurrencyInstance(Locale("es", "CO"))

        binding.tvLocalTitle.text = "Parada #${loc.numero}: ${loc.nombre}" + if (loc.entregado) " (ENTREGADO)" else ""
        binding.tvLocalAddress.text = loc.direccion
        binding.tvLocalItems.text = loc.productosTexto
        binding.tvLocalCash.text = "Cobro en mostrador: ${fmt.format(loc.totalCobro)} COP"

        if (loc.entregado) {
            binding.btnValidarPinLocal.isEnabled = false
            binding.btnValidarPinLocal.text = "Entrega Completada"
            binding.btnValidarPinLocal.setBackgroundColor(Color.parseColor("#059669"))
        } else {
            binding.btnValidarPinLocal.isEnabled = true
            binding.btnValidarPinLocal.text = "Validar PIN"
            binding.btnValidarPinLocal.setBackgroundColor(ContextCompat.getColor(this, R.color.prisma_teal_dark))
        }

        // Actualizar estado del chip seleccionado
        when (index) {
            0 -> binding.chipLocal1.isChecked = true
            1 -> binding.chipLocal2.isChecked = true
            2 -> binding.chipLocal3.isChecked = true
        }
    }

    private fun configurarMapaOSM() {
        val map = binding.mapView
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

        val controller = map.controller
        controller.setZoom(14.8)
        controller.setCenter(posCamion)

        // 1. Trazado de ruta vehicular completa que une el Camión con los 3 Locales secuencialmente
        val rutaVial = Polyline(map).apply {
            // Desde Camión hasta Local 1
            addPoint(posCamion)
            addPoint(GeoPoint(4.6189500, -74.1572500))
            addPoint(GeoPoint(4.6196000, -74.1578000))
            addPoint(GeoPoint(4.6203500, -74.1584500))
            addPoint(locales[0].punto) // Local 1: La Bendición

            // Desde Local 1 hasta Local 2
            addPoint(GeoPoint(4.6218000, -74.1581000))
            addPoint(GeoPoint(4.6226000, -74.1574000))
            addPoint(locales[1].punto) // Local 2: San José

            // Desde Local 2 hasta Local 3
            addPoint(GeoPoint(4.6247000, -74.1557000))
            addPoint(GeoPoint(4.6256000, -74.1550000))
            addPoint(locales[2].punto) // Local 3: El Diamante

            outlinePaint.apply {
                color = Color.parseColor("#0284C7") // Azul de navegación vehicular
                strokeWidth = 12f
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                isAntiAlias = true
            }
        }
        map.overlays.add(rutaVial)

        // 2. Marcador del Camión Repartidor FEMSA
        val truckMarker = Marker(map).apply {
            position = posCamion
            title = "Camión #CAMION-NORTE-01"
            snippet = "En recorrido de reparto activo"
            icon = vectorToBitmapDrawable(R.drawable.ic_truck_marker, 48)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        }
        map.overlays.add(truckMarker)

        // 3. Marcadores de cada uno de los 3 locales comerciales a visitar
        storeMarkers.clear()
        locales.forEachIndexed { i, local ->
            val marker = Marker(map).apply {
                position = local.punto
                title = "Parada #${local.numero}: ${local.nombre}"
                snippet = local.direccion
                icon = crearIconoMarcadorLocal(local.numero, local.entregado)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                setOnMarkerClickListener { _, _ ->
                    seleccionarLocal(i)
                    true
                }
            }
            storeMarkers.add(marker)
            map.overlays.add(marker)
        }

        map.invalidate()
    }

    private fun crearIconoMarcadorLocal(numeroParada: Int, entregado: Boolean): BitmapDrawable {
        val sizePx = (46 * resources.displayMetrics.density).toInt()
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Fondo del pin: Verde si ya fue entregado, Teal si está pendiente
        paint.color = if (entregado) Color.parseColor("#059669") else Color.parseColor("#0F766E")
        paint.style = Paint.Style.FILL
        canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f - 2, paint)

        // Borde blanco de alto contraste
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f * resources.displayMetrics.density
        canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f - 4, paint)

        // Texto o Check del número de parada
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        paint.textSize = 17f * resources.displayMetrics.density
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.textAlign = Paint.Align.CENTER
        val yPos = (sizePx / 2f - ((paint.descent() + paint.ascent()) / 2))
        val texto = if (entregado) "OK" else "$numeroParada"
        canvas.drawText(texto, sizePx / 2f, yPos, paint)

        return BitmapDrawable(resources, bitmap)
    }

    private fun vectorToBitmapDrawable(resId: Int, sizeDp: Int = 48): BitmapDrawable {
        val drawable = ContextCompat.getDrawable(this, resId) ?: return BitmapDrawable(resources)
        val sizePx = (sizeDp * resources.displayMetrics.density).toInt()
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return BitmapDrawable(resources, bitmap)
    }

    private fun mostrarDialogoValidarPin() {
        val local = locales[localSeleccionadoIndex]
        val input = EditText(this).apply {
            hint = "PIN de 6 dígitos"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            filters = arrayOf(android.text.InputFilter.LengthFilter(6))
            setPadding(40, 30, 40, 30)
        }

        AlertDialog.Builder(this)
            .setTitle("Confirmar Entrega en ${local.nombre}")
            .setMessage("Solicita al tendero el PIN de validación para registrar la descarga y el cobro:")
            .setView(input)
            .setPositiveButton("Confirmar") { _, _ ->
                val pin = input.text.toString().trim()
                if (pin.length != 6) {
                    Toast.makeText(this, "El PIN debe tener 6 dígitos", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                procesarEntrega(local, pin)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun procesarEntrega(local: LocalParada, pin: String) {
        lifecycleScope.launch {
            try {
                val response = ApiClient.conductorService.confirmarEntrega(
                    local.id,
                    ConfirmarEntregaRequest(pin)
                )
                local.entregado = true
                Toast.makeText(this@ConductorMapaRutaActivity, "Entrega registrada en ${local.nombre}", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                // Modo offline / demostración
                local.entregado = true
                Toast.makeText(this@ConductorMapaRutaActivity, "Entrega confirmada localmente", Toast.LENGTH_SHORT).show()
            }

            // Actualizar icono del marcador en el mapa
            if (localSeleccionadoIndex in storeMarkers.indices) {
                storeMarkers[localSeleccionadoIndex].icon = crearIconoMarcadorLocal(local.numero, true)
                binding.mapView.invalidate()
            }

            actualizarDetalleLocal(localSeleccionadoIndex)

            // Si hay un siguiente local pendiente, avanzar automáticamente
            val siguiente = locales.indexOfFirst { !it.entregado }
            if (siguiente != -1) {
                seleccionarLocal(siguiente)
            } else {
                Toast.makeText(this@ConductorMapaRutaActivity, "¡Todas las paradas de la ruta han sido completadas!", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }
}
