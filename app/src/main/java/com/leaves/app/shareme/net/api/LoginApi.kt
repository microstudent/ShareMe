package com.leaves.app.shareme.net.api

import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface LoginApi {

    @GET("login/cellphone")
    fun login(@Query("phone") phone: String, @Query("password") password: String): Observable<String>
}