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
            binding.tvPresentation.text = prod.presentacion

            val formatoPesos = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
            binding.tvWholesalePrice.text = "${formatoPesos.format(prod.precioUnitario)} COP"

            // Asignación de imagen fotográfica real de los productos de supermercado
            val resId = when {
                prod.sku.contains("RET") || prod.nombre.contains("Retornable", ignoreCase = true) || prod.nombre.contains("Vidrio", ignoreCase = true) ->
                    com.prisma.mobile.R.drawable.real_coca_vidrio
                prod.sku.contains("BRISA") || prod.nombre.contains("Agua", ignoreCase = true) || prod.nombre.contains("Brisa", ignoreCase = true) ->
                    com.prisma.mobile.R.drawable.real_agua_brisa
                prod.sku.contains("DELVALLE") || prod.nombre.contains("Jugo", ignoreCase = true) || prod.nombre.contains("Valle", ignoreCase = true) ->
                    com.prisma.mobile.R.drawable.real_delvalle
                prod.sku.contains("1.5L") || prod.nombre.contains("1.5L", ignoreCase = true) ->
                    com.prisma.mobile.R.drawable.real_coca_15l
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

            binding.btnPlus.setOnClickListener {
                val nueva = (cantidades[prod.id] ?: 0) + 1
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
