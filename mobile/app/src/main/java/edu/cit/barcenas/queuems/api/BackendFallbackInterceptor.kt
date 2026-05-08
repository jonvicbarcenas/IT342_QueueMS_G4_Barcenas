package edu.cit.barcenas.queuems.api

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException

class BackendFallbackInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        var lastError: IOException? = null

        BackendUrlProvider.allBaseUrls.forEachIndexed { index, baseUrl ->
            val request = if (index == 0) {
                originalRequest
            } else {
                originalRequest.newBuilder()
                    .url(BackendUrlProvider.replaceBaseUrl(originalRequest.url, baseUrl))
                    .build()
            }

            try {
                return chain.proceed(request)
            } catch (error: IOException) {
                lastError = error
                if (!error.isConnectionFailure()) {
                    throw error
                }
            }
        }

        throw lastError ?: ConnectException("Unable to reach QueueMS backend")
    }

    private fun IOException.isConnectionFailure(): Boolean {
        return this is ConnectException || this is SocketTimeoutException
    }
}
