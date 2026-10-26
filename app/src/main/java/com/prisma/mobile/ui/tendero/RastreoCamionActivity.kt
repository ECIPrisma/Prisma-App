package com.prisma.mobile.ui.tendero

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.prisma.mobile.databinding.ActivityRastreoCamionBinding
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.prisma.mobile.data.api.ApiClient
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.text.NumberFormat
import java.util.Locale

class RastreoCamionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRastreoCamionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRastreoCamionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        configurarMapaOSM()
        cargarDetallePedidoEnCamino()
    }

    private fun cargarDetallePedidoEnCamino() {
        lifecycleScope.launch {
            try {
                val response = ApiClient.tiendaService.getMisPedidos()
                if (response.isSuccessful) {
                    val pedidos = response.body()?.datos.orEmpty()
                    val pedActivo = pedidos.firstOrNull { it.estado == "EN_RUTA" || it.estado == "CREADO" }
                        ?: pedidos.firstOrNull()

                    if (pedActivo != null) {
                        binding.tvOrderNumberTrack.text = "Pedido en camino: #${pedActivo.numeroPedido}"
                        binding.tvPinPill.text = "PIN: ${pedActivo.pinEntregaHash ?: "839201"}"

                        val formatoPesos = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
                        binding.tvTotalDueTrack.text = "${formatoPesos.format(pedActivo.totalAPagar)} COP"

                        val itemsTexto = if (!pedActivo.items.isNullOrEmpty()) {
                            "Carga para entregar:\n" + pedActivo.items.joinToString("\n") {
                                "• ${it.cantidad}x ${it.nombreProductoHistorico ?: "Producto"}"
                            }
                        } else {
                            "Carga para entregar:\n• 3x Coca-Cola Original 400ml\n• 1x Coca-Cola 1.5L"
                        }
                        binding.tvProductsInTruck.text = itemsTexto
                    }
                }
            } catch (_: Exception) {
                // Conserva los datos por defecto si está fuera de línea
            }
        }
    }

    private fun configurarMapaOSM() {
        val map = binding.mapView
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        // Ocultar botones/manos de control táctil en pantalla
        map.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

        val mapController = map.controller
        mapController.setZoom(15.5)

        // Coordenadas de Bogotá (Kennedy Central - Semilla)
        val posCamion = GeoPoint(4.6184200, -74.1568200)
        val posTienda = GeoPoint(4.6210000, -74.1590000)

        mapController.setCenter(posCamion)

        // 1. Trazado de ruta por la malla vial urbana (siguiendo las calles reales, no a través de las casas)
        val rutaVial = Polyline(map).apply {
            addPoint(posCamion)                                    // Salida: Calle 41D Sur / Transversal 78K
            addPoint(GeoPoint(4.6189500, -74.1572500))              // Giro por Transversal 78K
            addPoint(GeoPoint(4.6196000, -74.1578000))              // Intersección Calle 41C Sur con Carrera 79
            addPoint(GeoPoint(4.6203500, -74.1584500))              // Subida por Carrera 79 hacia Calle 40C Sur
            addPoint(GeoPoint(4.6207500, -74.1588000))              // Giro hacia la tienda por Calle 40C Sur
            addPoint(posTienda)                                     // Llegada: Tienda La Bendición

            outlinePaint.apply {
                color = android.graphics.Color.parseColor("#0284C7") // Azul de navegación vehicular moderno
                strokeWidth = 12f
                strokeCap = android.graphics.Paint.Cap.ROUND
                strokeJoin = android.graphics.Paint.Join.ROUND
                isAntiAlias = true
            }
        }
        map.overlays.add(rutaVial)

        // 2. Marcador del Camión (se agrega después para quedar por ENCIMA de la ruta)
        val truckMarker = Marker(map).apply {
            position = posCamion
            title = "Vehículo de Reparto FEMSA"
            snippet = "En ruta hacia tu tienda"
            icon = vectorToBitmapDrawable(com.prisma.mobile.R.drawable.ic_truck_marker, 48)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        }
        map.overlays.add(truckMarker)

        // 3. Marcador de la Tienda de destino
        val storeMarker = Marker(map).apply {
            position = posTienda
            title = "Tienda La Bendición"
            snippet = "Punto de entrega (Parada #2)"
            icon = vectorToBitmapDrawable(com.prisma.mobile.R.drawable.ic_store_marker, 48)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        }
        map.overlays.add(storeMarker)

        map.invalidate()
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

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }
}
