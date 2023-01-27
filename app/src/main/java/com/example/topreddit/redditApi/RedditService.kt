package com.example.topreddit.redditApi

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface RedditService {

    @GET("/{sorting}/.json")
    suspend fun getPosts(
        @Header("User-Agent") UserAgent: String?,
        @Path("sorting") sorting: String?,
        @Query("after") after: String?,
        @Query("limit") limit: Int?
    ): ListPostResponse


    companion object {
        private const val BASE_URL = "https://www.reddit.com"
        private const val TAG = "DataSource"

        fun create(): RedditService {
            val logger = HttpLoggingInterceptor()
            logger.level = Level.BASIC

            val client = OkHttpClient.Builder()
                .addInterceptor(logger)
                .build()
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(RedditService::class.java)
        }
    }
}