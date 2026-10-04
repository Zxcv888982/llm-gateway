package com.llmgateway.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private val Context.dataStore by preferencesDataStore(name = "app_prefs")

class TokenStore(private val context: Context) {
    private val tokenKey = stringPreferencesKey("auth_token")

    val tokenValue: String
        get() = runBlocking { context.dataStore.data.first()[tokenKey] ?: "" }

    suspend fun saveToken(token: String) {
        context.dataStore.edit { it[tokenKey] = token }
    }

    suspend fun clearToken() {
        context.dataStore.edit { it.remove(tokenKey) }
    }
}
