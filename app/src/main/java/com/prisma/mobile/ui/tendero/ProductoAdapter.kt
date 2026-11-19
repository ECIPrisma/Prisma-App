package com.prisma.mobile.ui.tendero

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.prisma.mobile.data.model.ProductoDto
import com.prisma.mobile.databinding.ItemProductoBinding
import java.text.NumberFormat
import java.util.Locale

class ProductoAdapter(
    private val onQuantityChanged: (ProductoDto, Int) -> Unit
) : RecyclerView.Adapter<ProductoAdapter.ProductoViewHolder>() {

    private val productos = mutableListOf<ProductoDto>()
    private val cantidades = mutableMapOf<Long, Int>()

    fun setProductos(nuevos: List<ProductoDto>) {
        productos.clear()
        productos.addAll(nuevos)
        notifyDataSetChanged()
    }

    fun getCantidadesSeleccionadas(): Map<Long, Int> = cantidades.filter { it.value > 0 }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductoViewHolder {
        val binding = ItemProductoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProductoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductoViewHolder, position: Int) {
        holder.bind(productos[position])
    }

    override fun getItemCount(): Int = productos.size

    inner class ProductoViewHolder(private val binding: ItemProductoBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(prod: ProductoDto) {
            binding.tvProductName.text = prod.nombre
            val agotado = prod.stockDisponible <= 0
            binding.tvPresentation.text = if (agotado) "${prod.presentacion} • Agotado" else prod.presentacion

            val formatoPesos = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
            binding.tvWholesalePrice.text = if (agotado) "Agotado" else "${formatoPesos.format(prod.precioUnitario)} COP"

            // Asignación de imagen fotográfica real de los productos de la empresa
            val resId = when {
                // Postobón Manzana
                prod.sku.contains("MANZ") || prod.nombre.contains("Manzana", ignoreCase = true) ->
                    com.prisma.mobile.R.drawable.real_postobon_manzana
                // Colombiana La Nuestra
                prod.sku.contains("COLOMB") || prod.nombre.contains("Colombiana", ignoreCase = true) ->
                    com.prisma.mobile.R.drawable.real_colombiana
                // Sprite
                prod.sku.contains("SPRITE") || prod.nombre.contains("Sprite", ignoreCase = true) ->
                    com.prisma.mobile.R.drawable.real_sprite
                // Quatro Toronja
                prod.sku.contains("QUATRO") || prod.nombre.contains("Quatro", ignoreCase = true) ->
                    com.prisma.mobile.R.drawable.real_quatro
                // Monster Energy
                prod.sku.contains("MONSTER") || prod.nombre.contains("Monster", ignoreCase = true) ->
                    com.prisma.mobile.R.drawable.real_monster
                // Powerade
                prod.sku.contains("POWER") || prod.nombre.contains("Powerade", ignoreCase = true) ->
                    com.prisma.mobile.R.drawable.real_powerade
                // Jugo Hit
                prod.sku.contains("HIT") || prod.nombre.contains("Hit", ignoreCase = true) ->
                    com.prisma.mobile.R.drawable.real_hit_mora
                // Bretaña Soda
                prod.sku.contains("BRETANA") || prod.nombre.contains("Bretaña", ignoreCase = true) ->
                    com.prisma.mobile.R.drawable.real_bretana
                // Coca-Cola 3 Litros
                prod.sku.contains("3000") || prod.sku.contains("3L") || prod.nombre.contains("3L", ignoreCase = true) || prod.nombre.contains("3 Litros", ignoreCase = true) ->
                    com.prisma.mobile.R.drawable.real_coca_3l
                // Coca-Cola Vidrio Retornable
                prod.sku.contains("RET") || prod.nombre.contains("Retornable", ignoreCase = true) || prod.nombre.contains("Vidrio", ignoreCase = true) ->
                    com.prisma.mobile.R.drawable.real_coca_vidrio
                // Agua Brisa
                prod.sku.contains("BRISA") || prod.nombre.contains("Agua", ignoreCase = true) || prod.nombre.contains("Brisa", ignoreCase = true) || prod.nombre.contains("Manantial", ignoreCase = true) ->
                    com.prisma.mobile.R.drawable.real_agua_brisa
                // Del Valle
                prod.sku.contains("VALLE") || prod.nombre.contains("Valle", ignoreCase = true) ->
                    com.prisma.mobile.R.drawable.real_delvalle
                // Coca-Cola 1.5L
                prod.sku.contains("1.5L") || prod.sku.contains("1500") || prod.nombre.contains("1.5L", ignoreCase = true) ->
                    com.prisma.mobile.R.drawable.real_coca_15l
                // Coca-Cola 400ml / Predeterminado
                else ->
                    com.prisma.mobile.R.drawable.real_coca_400ml
            }
            binding.ivProduct.setImageResource(resId)

            if (prod.esPromocion) {
                binding.tvPromoBadge.visibility = View.VISIBLE
            } else {
                binding.tvPromoBadge.visibility = View.GONE
            }

            if (prod.esHabitual) {
                binding.tvHabitualBadge.visibility = View.VISIBLE
            } else {
                binding.tvHabitualBadge.visibility = View.GONE
            }

            val cantActual = cantidades[prod.id] ?: 0
            binding.tvQuantity.text = cantActual.toString()

            binding.btnPlus.isEnabled = !agotado
            binding.btnPlus.alpha = if (agotado) 0.4f else 1.0f

            binding.btnPlus.setOnClickListener {
                if (agotado) return@setOnClickListener
                val actual = cantidades[prod.id] ?: 0
                val minReq = if (prod.cantidadMinimaPedido > 0) prod.cantidadMinimaPedido else 1
                val nueva = if (actual == 0) minReq else actual + 1
                cantidades[prod.id] = nueva
                binding.tvQuantity.text = nueva.toString()
                onQuantityChanged(prod, nueva)
            }

            binding.btnMinus.setOnClickListener {
                val actual = cantidades[prod.id] ?: 0
                if (actual > 0) {
                    val nueva = actual - 1
                    cantidades[prod.id] = nueva
                    binding.tvQuantity.text = nueva.toString()
                    onQuantityChanged(prod, nueva)
                }
            }
        }
    }
}
