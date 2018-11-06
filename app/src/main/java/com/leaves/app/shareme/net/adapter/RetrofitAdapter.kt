package com.leaves.app.shareme.net.adapter

import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory


class RetrofitAdapter {

    companion object {
        private val BASE_URL: String = "http://172.18.4.162:3000/"

        private val retrofit = Retrofit.Builder()
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(ScalarsConverterFactory.create())
                .baseUrl(BASE_URL)
                .build()

        fun get(): Retrofit {
            return retrofit
        }
    }
}