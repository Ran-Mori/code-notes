package com.retrofit

import com.google.gson.annotations.SerializedName

data class DataResponse(
    @SerializedName("userId") val uid: Int = 0,
    @SerializedName("id") val id: Int = 0,
    @SerializedName("title") val title: String = "",
    @SerializedName("body") val body: String = "",
)
