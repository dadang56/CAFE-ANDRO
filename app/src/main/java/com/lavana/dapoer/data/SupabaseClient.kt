package com.lavana.dapoer.data

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage

object SupabaseClient {
    const val SUPABASE_URL = "https://mtjyggxyjojcvcjxiblo.supabase.co"
    const val SUPABASE_KEY = "sb_publishable_nmPTNpDZPNHnuVaGUcBmKQ_3KgP_twU"

    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_KEY
    ) {
        install(Auth)      // Autentikasi User (Login/Register)
        install(Postgrest) // CRUD Query Database (Insert, Select, Update, Delete)
        install(Realtime)  // Sinkronisasi Pesanan Real-time
        install(Storage)   // Unggah Bukti Bayar dan Foto Menu
    }

    val auth get() = client.auth
    val db get() = client.postgrest
    val realtime get() = client.realtime
    val storage get() = client.storage

    fun syncDynamicCategories(items: List<MenuItem>) {
        val unique = items.map { it.category }.filter { it.isNotBlank() }.distinct()
        val all = (unique + CUSTOM_CATEGORIES).distinct()
        DYNAMIC_CATEGORIES.clear()
        DYNAMIC_CATEGORIES.addAll(all)
    }

    suspend fun initializeMenuIfEmpty(): List<MenuItem> {
        return try {
            val fetched = db["menu_items"].select().decodeList<MenuItem>()
            if (fetched.isEmpty()) {
                val fallback = getFallbackMenu()
                db["menu_items"].insert(fallback)
                syncDynamicCategories(fallback)
                fallback
            } else {
                syncDynamicCategories(fetched)
                fetched
            }
        } catch (e: Exception) {
            e.printStackTrace()
            val fallback = getFallbackMenu()
            syncDynamicCategories(fallback)
            fallback
        }
    }

    var mockUserId: String? = null
    var mockUserEmail: String? = null

    fun setMockUser(id: String?, email: String?) {
        mockUserId = id
        mockUserEmail = email
    }

    val currentUserId: String?
        get() = try {
            auth.currentUserOrNull()?.id ?: mockUserId
        } catch (e: Exception) {
            mockUserId
        }

    val currentUserEmail: String?
        get() = try {
            auth.currentUserOrNull()?.email ?: mockUserEmail
        } catch (e: Exception) {
            mockUserEmail
        }
}
