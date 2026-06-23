package com.lenz.tennisapp.data.api.interceptor

import com.lenz.tennisapp.data.datastore.ApiKeyStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TennisApiKeyInterceptor @Inject constructor(
    private val keyStore: ApiKeyStore
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())

        if (response.code == 401 || response.code == 403) {
            runBlocking { keyStore.setTennisKeyExpired(true) }
        } else if (response.isSuccessful) {
            // api-tennis.com returns HTTP 200 with success:0 on key issues
            // We check this in the repository layer after parsing
        }
        return response
    }
}

