package com.prisma.mobile.data.model

import com.google.gson.annotations.SerializedName

data class ProductoDto(
    @SerializedName("id") val id: Long,
    @SerializedName("sku") val sku: String,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("presentacion") val presentacion: String,
    @SerializedName("precioUnitario") val precioUnitario: Double,
    @SerializedName("precioSugeridoPublico") val precioSugeridoPublico: Double?,
    @SerializedName("stockDisponible") val stockDisponible: Int,
    @SerializedName("esPromocion") val esPromocion: Boolean = false,
    @SerializedName("esHabitual") val esHabitual: Boolean = false,
    @SerializedName("imagenUrl") val imagenUrl: String?
)
