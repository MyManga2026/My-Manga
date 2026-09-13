package com.example.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import okhttp3.ConnectionPool
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/**
 * Centralized Network Utility that provides:
 * 1. Resilient OkHttpClient with automatic retry, exponential backoff, and robust connection pooling.
 * 2. Real-time Android ConnectivityManager verification to prevent false "offline" errors.
 */
object NetworkUtils {

    private const val TAG = "NetworkUtils"
    private const val MAX_RETRIES = 2

    private val connectionPool = ConnectionPool(32, 5, TimeUnit.MINUTES)

    /**
     * Interceptor that retries failed network calls on transient connection drops or timeouts.
     */
    private val retryInterceptor = Interceptor { chain ->
        val request = chain.request()
        var response: Response? = null
        var exception: IOException? = null
        var tryCount = 0

        while (tryCount <= MAX_RETRIES) {
            try {
                response?.close()
                response = chain.proceed(request)
                if (response.isSuccessful || response.code < 500) {
                    return@Interceptor response
                }
                // Server 5xx error: close and retry once if applicable
                if (tryCount < MAX_RETRIES && response.code in listOf(502, 503, 504)) {
                    response.close()
                    Thread.sleep((tryCount + 1) * 600L)
                } else {
                    return@Interceptor response
                }
            } catch (e: SocketTimeoutException) {
                exception = e
                Log.w(TAG, "Socket timeout on ${request.url}, attempt ${tryCount + 1}/$MAX_RETRIES")
            } catch (e: UnknownHostException) {
                exception = e
                Log.w(TAG, "Unknown host on ${request.url}, attempt ${tryCount + 1}/$MAX_RETRIES")
            } catch (e: IOException) {
                exception = e
                Log.w(TAG, "IO exception on ${request.url}, attempt ${tryCount + 1}/$MAX_RETRIES: ${e.message}")
            }

            tryCount++
            if (tryCount <= MAX_RETRIES) {
                try {
                    Thread.sleep(tryCount * 500L)
                } catch (_: InterruptedException) {
                    break
                }
            }
        }

        if (response != null) {
            return@Interceptor response
        }

        throw exception ?: IOException("Network request failed after $MAX_RETRIES attempts: ${request.url}")
    }

    /**
     * Standard User-Agent and headers interceptor to avoid getting blocked by scrapers/firewalls.
     */
    private val headersInterceptor = Interceptor { chain ->
        val original = chain.request()
        val builder = original.newBuilder()

        if (original.header("User-Agent").isNullOrBlank()) {
            builder.header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile; rv:128.0) Gecko/128.0 Firefox/128.0")
        }
        if (original.header("Accept-Language").isNullOrBlank()) {
            builder.header("Accept-Language", "en-US,en;q=0.9")
        }

        chain.proceed(builder.build())
    }

    /**
     * Shared resilient OkHttpClient instance for all API and scraper clients.
     */
    val sharedHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectionPool(connectionPool)
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .followRedirects(true)
            .followSslRedirects(true)
            .addInterceptor(headersInterceptor)
            .addInterceptor(retryInterceptor)
            .build()
    }

    /**
     * Checks if the device is genuinely connected to an active network with validated internet capability.
     */
    fun isNetworkAvailable(context: Context?): Boolean {
        if (context == null) return true // Default to true if context unavailable to avoid false offline blocks
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return true

        return try {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
        } catch (e: Exception) {
            Log.e(TAG, "Error checking connectivity", e)
            true // Fail open so users aren't erroneously blocked
        }
    }
}
