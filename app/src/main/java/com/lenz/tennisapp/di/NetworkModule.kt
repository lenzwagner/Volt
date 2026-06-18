package com.lenz.tennisapp.di

import com.lenz.tennisapp.data.api.OddsApiService
import com.lenz.tennisapp.data.api.RankingProxyService
import com.lenz.tennisapp.data.api.TennisApiService
import com.lenz.tennisapp.data.api.interceptor.OddsApiKeyInterceptor
import com.lenz.tennisapp.data.api.interceptor.TennisApiKeyInterceptor
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private fun baseOkHttp() = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })

    @Provides
    @Singleton
    fun provideDefaultOkHttp(): OkHttpClient =
        baseOkHttp().build()

    @Provides
    @Singleton
    @Named("tennis")
    fun provideTennisOkHttp(interceptor: TennisApiKeyInterceptor): OkHttpClient =
        baseOkHttp().addInterceptor(interceptor).build()

    @Provides
    @Singleton
    @Named("odds")
    fun provideOddsOkHttp(interceptor: OddsApiKeyInterceptor): OkHttpClient =
        baseOkHttp().addInterceptor(interceptor).build()

    @Provides
    @Singleton
    fun provideTennisApiService(
        @Named("tennis") client: OkHttpClient,
        moshi: Moshi
    ): TennisApiService = Retrofit.Builder()
        .baseUrl("https://api.api-tennis.com/tennis/")
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(TennisApiService::class.java)

    @Provides
    @Singleton
    fun provideRankingProxyService(
        client: OkHttpClient,
        moshi: Moshi
    ): RankingProxyService = Retrofit.Builder()
        .baseUrl("https://tennis-ranking-proxy.onrender.com/")
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(RankingProxyService::class.java)

    @Provides
    @Singleton
    fun provideOddsApiService(
        @Named("odds") client: OkHttpClient,
        moshi: Moshi
    ): OddsApiService = Retrofit.Builder()
        .baseUrl("https://api.the-odds-api.com/v4/")
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(OddsApiService::class.java)
}
