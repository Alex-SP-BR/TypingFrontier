package com.typingfrontier.utils

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.typingfrontier.SoundManager
import java.util.concurrent.atomic.AtomicBoolean

object AdManager {
    private const val TAG = "AdManager"
    
    // ID real do bloco de anúncios recompensados
    private const val REWARDED_AD_UNIT_ID = "ca-app-pub-4553906388461124/2680340478"

    private var rewardedAd: RewardedAd? = null
    private var isLoading = false
    private var isRewardHandled = false
    
    private lateinit var consentInformation: ConsentInformation
    private var isMobileAdsInitializeCalled = AtomicBoolean(false)

    /**
     * Inicializa as configurações globais de anúncios.
     * Chamado no Application.onCreate.
     */
    fun init(context: Context) {
        val requestConfiguration = MobileAds.getRequestConfiguration()
            .toBuilder()
            .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G)
            .build()
        
        MobileAds.setRequestConfiguration(requestConfiguration)
    }

    /**
     * Gerencia o fluxo de consentimento UMP e inicializa o SDK de anúncios se permitido.
     * Deve ser chamado na Activity inicial (ex: MainActivity).
     */
    fun iniciarFluxoConsentimento(activity: Activity) {
        Log.d(TAG, "UMP: iniciarFluxoConsentimento executado")

        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false) // App 13+
            .build()

        consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        Log.w(TAG, "Erro UMP Form: ${formError.errorCode}: ${formError.message}")
                    }
                    if (consentInformation.canRequestAds()) {
                        initializeMobileAdsSdk(activity)
                    }
                }
            },
            { requestConsentError ->
                Log.w(TAG, "Erro UMP Info: ${requestConsentError.errorCode}: ${requestConsentError.message}")
                if (consentInformation.canRequestAds()) {
                    initializeMobileAdsSdk(activity)
                }
            }
        )

        // Se o consentimento já foi obtido anteriormente, inicializa imediatamente
        if (consentInformation.canRequestAds()) {
            initializeMobileAdsSdk(activity)
        }
    }

    /**
     * Inicializa o Mobile Ads SDK efetivamente.
     */
    private fun initializeMobileAdsSdk(context: Context) {
        if (isMobileAdsInitializeCalled.getAndSet(true)) {
            return
        }

        MobileAds.initialize(context) {
            loadRewardedAd(context)
        }
    }

    /**
     * Carrega um anúncio recompensado se não houver um carregado ou em processo de carga.
     */
    fun loadRewardedAd(context: Context) {
        if (isLoading || rewardedAd != null) return

        isLoading = true
        val adRequest = AdRequest.Builder().build()
        
        RewardedAd.load(context, REWARDED_AD_UNIT_ID, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d(TAG, "Falha ao carregar anúncio: ${adError.message}")
                rewardedAd = null
                isLoading = false
            }

            override fun onAdLoaded(ad: RewardedAd) {
                Log.d(TAG, "Anúncio carregado com sucesso.")
                rewardedAd = ad
                isLoading = false
            }
        })
    }

    /**
     * Verifica se o anúncio está pronto para exibição.
     */
    fun isAdLoaded(): Boolean = rewardedAd != null

    /**
     * Exibe o anúncio recompensado e gerencia os callbacks.
     */
    fun showRewardedAd(
        activity: Activity, 
        onRewardEarned: () -> Unit, 
        onAdClosed: () -> Unit, 
        onAdFailed: (String) -> Unit
    ) {
        val ad = rewardedAd
        if (ad != null) {
            // Pausa a música se estiver tocando
            val musicWasPlaying = SoundManager.isMusicPlaying()
            if (musicWasPlaying) {
                SoundManager.pause()
            }

            isRewardHandled = false // Reseta a flag de recompensa para esta exibição
            
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Anúncio fechado pelo usuário.")
                    rewardedAd = null
                    
                    // Restaura a música se ela estava tocando
                    if (musicWasPlaying) {
                        SoundManager.resume()
                    }

                    // Prepara o próximo anúncio
                    loadRewardedAd(activity)
                    onAdClosed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                    Log.d(TAG, "Falha ao exibir anúncio: ${adError.message}")
                    rewardedAd = null

                    // Restaura a música se ela estava tocando
                    if (musicWasPlaying) {
                        SoundManager.resume()
                    }

                    loadRewardedAd(activity)
                    onAdFailed(adError.message)
                }
            }

            ad.show(activity, OnUserEarnedRewardListener { rewardItem ->
                // Proteção contra múltiplos callbacks de recompensa para o mesmo anúncio
                if (!isRewardHandled) {
                    isRewardHandled = true
                    Log.d(TAG, "Recompensa recebida: ${rewardItem.amount} ${rewardItem.type}")
                    onRewardEarned()
                }
            })
        } else {
            Log.d(TAG, "Anúncio solicitado mas não carregado.")
            onAdFailed("O anúncio ainda não está pronto. Tente novamente em alguns instantes.")
            loadRewardedAd(activity)
        }
    }
}
