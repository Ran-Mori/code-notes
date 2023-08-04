package com.retrofit


import io.reactivex.rxjava3.core.Observable
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @GET("/posts/{id}")
    fun getRespById(@Path("id") id: Int): Observable<DataResponse>

    @GET("/posts")
    fun getAllResp(): Observable<List<DataResponse>>

    @GET("/comments")
    fun getComment(@Query("postId") postId: Int): Observable<List<DataComment>>
}