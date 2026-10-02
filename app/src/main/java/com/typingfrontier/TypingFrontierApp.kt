package com.typingfrontier

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.typingfrontier.social.DiscussionActivity
import com.typingfrontier.social.PresenceManager
import com.typingfrontier.social.RoleManagementActivity
import com.typingfrontier.station.StationActivity
import com.typingfrontier.utils.AdManager
import com.typingfrontier.utils.ViewUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TypingFrontierApp : Application() {

    companion object {
        private lateinit var instance: TypingFrontierApp
        fun getAppContext(): android.content.Context = instance.applicationContext
    }

    private var activityCount = 0
    private val handler = Handler(Looper.getMainLooper())
    private var pauseRunnable: Runnable? = null
    private var currentActivity: Activity? = null

    private fun isOnlineActivity(activity: Activity?): Boolean {
        return activity is GameActivity ||
               activity is DiscussionActivity ||
               activity is StationActivity ||
               activity is RoleManagementActivity
    }

    private fun updateGlobalPresence(activity: Activity?) {
        if (isOnlineActivity(activity)) {
            PresenceManager.startPresence()
        } else {
            PresenceManager.stopPresence()
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Inicializa AdMob
        AdManager.init(this)

        // Carrega configurações de som, HUD, save local e moeda em background (I/O de disco e processamento fora da Main Thread)
        CoroutineScope(Dispatchers.IO).launch {
            SoundManager.init(this@TypingFrontierApp)
            HudSettingsManager.init(this@TypingFrontierApp)
            PlayerManager.load(this@TypingFrontierApp)
            ViewUtils.getCoinDrawable(this@TypingFrontierApp)
        }

        // Inicializa Supabase (Camada Social)
        com.typingfrontier.social.SupabaseManager.init()
        
        // Inicializa Identidade Social e Sincroniza Ranking (Offline-first)
        com.typingfrontier.social.SocialProfileRepository.initializeSocialIdentity {
            com.typingfrontier.social.SocialProfileRepository.syncStatistics()
        }
        
        // Inicia o serviço para monitorar se o usuário "limpa" o app dos recentes
        try {
            startService(Intent(this, MusicService::class.java))
        } catch (e: Exception) {}

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            
            override fun onActivityStarted(activity: Activity) {
                // Cancela qualquer pausa pendente pois uma nova tela abriu
                pauseRunnable?.let { handler.removeCallbacks(it) }
                pauseRunnable = null
                currentActivity = activity

                if (activityCount == 0) {
                    // App voltando do background para o primeiro plano
                    SoundManager.resume()
                }
                activityCount++
            }

            override fun onActivityResumed(activity: Activity) {
                currentActivity = activity
                updateGlobalPresence(activity)
            }

            override fun onActivityPaused(activity: Activity) {}

            override fun onActivityStopped(activity: Activity) {
                // Sincroniza ranking social em background quando o jogador sai de uma tela
                com.typingfrontier.social.SocialProfileRepository.syncStatistics()

                activityCount--
                if (activityCount == 0) {
                    // Se não abrir nenhuma outra tela em 500ms, o app foi para o background
                    pauseRunnable = Runnable {
                        if (activityCount == 0) {
                            SoundManager.pause()
                            PresenceManager.stopPresence()

                            if (currentActivity is StationActivity) {
                                currentActivity?.finish()
                                currentActivity = null
                            }
                        }
                    }
                    handler.postDelayed(pauseRunnable!!, 500)
                }
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {
                if (currentActivity === activity) {
                    currentActivity = null
                }
                if (isOnlineActivity(currentActivity)) {
                    PresenceManager.startPresence()
                }
            }
        })
    }
}
