package com.kakaroo.footprinterservice.service

import com.kakaroo.footprinterservice.entity.FootPrinter
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

//API를 관리해 주는 Interface
interface IRetrofitNetworkService {
    @POST("/post")
    @Headers("accept: application/json",
        "content-type: application/json")
    fun postMethod(
        @Body footPrinter: FootPrinter//,
        //@Query("id") id: Long
    ): Call<Int>
}