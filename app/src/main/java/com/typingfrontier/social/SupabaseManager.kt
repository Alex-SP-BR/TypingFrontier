package com.typingfrontier.social

import com.typingfrontier.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.SettingsSessionManager
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

/**
 * Gerenciador da infraestrutura Supabase para a camada social do Typing Frontier.
 * Responsável pela inicialização e disponibilização do cliente Supabase.
 */
object SupabaseManager {
    
    lateinit var client: SupabaseClient
        private set

    fun init() {
        // Inicializa o cliente usando as credenciais do BuildConfig (vindas do local.properties)
        client = createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_KEY
        ) {
            install(Postgrest)
            install(Auth) {
                // Garante a persistência da sessão no Android usando SharedPreferences (via multiplatform-settings)
                sessionManager = SettingsSessionManager()
            }
            install(Realtime)
        }
    }
}
