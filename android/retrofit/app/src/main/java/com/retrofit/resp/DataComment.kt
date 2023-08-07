package com.retrofit.resp

import com.google.gson.annotations.SerializedName

data class DataComment(
    @SerializedName("postId") val postId: Int = 0,
    @SerializedName("id") val id: Int = 0,
    @SerializedName("name") val name: String = "",
    @SerializedName("email") val email: String = "",
    @SerializedName("body") val body: String = "",
)
