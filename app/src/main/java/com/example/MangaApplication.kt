package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.util.DebugLogger
import okhttp3.Cache
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Custom Application class that optimizes Coil image loading, memory caching,
 * and disk caching to eliminate UI lag and frame drops during scrolling and image rendering.
 */
class MangaApplication : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        val okHttpClient = OkHttpClient.Builder()
            .connectionPool(ConnectionPool(10, 5, TimeUnit.MINUTES))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .cache(Cache(File(cacheDir, "http_cache"), 100L * 1024 * 1024)) // 100 MB HTTP cache
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25) // Use up to 25% of available RAM for bitmap memory cache
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(250L * 1024 * 1024) // 250 MB disk cache for manga pages and covers
                    .build()
            }
            .respectCacheHeaders(false) // Cache manga images even if CDN headers have short TTL
            .allowHardware(true) // Direct GPU rendering for hardware bitmaps
            .crossfade(150) // Fast and smooth crossfade
            .build()
    }
}
