package com.llmgateway.app.data.api

import android.content.Context
import com.llmgateway.app.BuildConfig
import com.llmgateway.app.data.TokenStore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private lateinit var tokenStore: TokenStore
    lateinit var service: ApiService
        private set

    // 全局 401 事件：UI 层监听后自动跳转登录页
    private val _unauthorizedEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val unauthorizedEvents = _unauthorizedEvents.asSharedFlow()

    fun init(context: Context) {
        tokenStore = TokenStore(context)

        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }

        // 附加 token
        val authInterceptor = Interceptor { chain ->
            val token = tokenStore.tokenValue
            val request = if (token.isNotEmpty()) {
                chain.request().newBuilder().addHeader("Authorization", "Bearer $token").build()
            } else chain.request()
            chain.proceed(request)
        }

        // 401 自动处理：清除 token + 通知 UI
        val unauthorizedInterceptor = Interceptor { chain ->
            val response = chain.proceed(chain.request())
            if (response.code == 401) {
                val path = chain.request().url.encodedPath
                // 登录/注册接口的 401 是正常业务错误，不触发全局登出
                if (!path.contains("/auth/login") && !path.contains("/auth/register")) {
                    tokenStore.let {
                        kotlinx.coroutines.runBlocking { it.clearToken() }
                    }
                    _unauthorizedEvents.tryEmit(Unit)
                }
            }
            response
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(unauthorizedInterceptor)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL + "/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        service = retrofit.create(ApiService::class.java)
    }

    fun getTokenStore() = tokenStore
}
