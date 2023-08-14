package com.gson

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class Staff(
    @SerializedName("name") val name: String,
    @SerializedName("age") val age: Int,
    @SerializedName("position") val position: Array<String>,
    @SerializedName("skills") val skills: List<String>,
    @SerializedName("salary") val salary: Map<String, BigDecimal>
)
