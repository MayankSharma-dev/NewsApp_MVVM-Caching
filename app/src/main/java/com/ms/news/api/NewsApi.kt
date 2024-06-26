package com.ms.news.api

import com.ms.news.BuildConfig
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

// Network Request
interface NewsApi {
    companion object{
        const val BASE_URL = "https://newsapi.org/v2/"
        const val API_KEY =  BuildConfig.news_api_access_key
    }

    @Headers("X-Api-Key: $API_KEY")
    @GET("top-headlines?country=in&pageSize=100")
    suspend fun getBreakingNews(): NewsResponse

    @Headers("X-Api-Key: $API_KEY")
    @GET("everything")
    suspend fun searchNews(
        @Query("q") query: String,
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int
    ): NewsResponse
}