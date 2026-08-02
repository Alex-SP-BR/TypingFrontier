package com.typingfrontier

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Este serviço serve apenas para detectar quando o usuário remove o app da lista de recentes.
 */
class MusicService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Quando o app é "limpo" (swiped away)
        SoundManager.stop()
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }
}
