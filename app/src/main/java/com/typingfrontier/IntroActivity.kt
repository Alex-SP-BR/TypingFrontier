package com.typingfrontier

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class IntroActivity : AppCompatActivity() {

    private lateinit var txtLogo: TextView
    private lateinit var containerCinematic: FrameLayout
    private lateinit var imgCity: ImageView
    private lateinit var imgProfessions: ImageView
    private lateinit var imgFinalLogo: ImageView
    private lateinit var layoutNarrativa: LinearLayout
    private lateinit var txtNarrativa: TextView
    private lateinit var btnSkip: Button
    private lateinit var viewBlackOverlay: View

    private val narrativa = listOf(
        "Bem-vindo ao TypingFrontier.",
        "Você está começando sua jornada em São Paulo.",
        "Uma cidade cheia de oportunidades, mas também marcada por grandes desafios.",
        "A violência preocupa seus moradores.",
        "Muitos hospitais estão sobrecarregados.",
        "Parte da infraestrutura precisa de melhorias.",
        "Diversos serviços públicos enfrentam dificuldades.",
        "Mesmo assim...",
        "Milhares de pessoas trabalham todos os dias para construir um futuro melhor.",
        "Neste mundo, você não é um herói destinado a salvar a cidade.",
        "Você é uma pessoa comum.",
        "Suas escolhas definirão quem você será.",
        "Trabalhe com dedicação.",
        "Estude para evoluir.",
        "Treine seu corpo e sua mente.",
        "Explore lugares desconhecidos.",
        "Ajude quando puder.",
        "Seja um exemplo para as pessoas ao seu redor.",
        "Grandes mudanças começam com pequenas atitudes.",
        "Talvez você não consiga mudar o mundo inteiro...",
        "Mas sempre contribua para melhorar o lugar onde você vive.",
        "Sua jornada começa agora."
    )

    private var currentTextIndex = 0
    private var typingJob: Job? = null
    private var isIntroFinished = false
    private var isReviewMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro)

        isReviewMode = intent.getBooleanExtra("isReview", false)

        txtLogo = findViewById(R.id.txtLogo)
        containerCinematic = findViewById(R.id.containerCinematic)
        imgCity = findViewById(R.id.imgCity)
        imgProfessions = findViewById(R.id.imgProfessions)
        imgFinalLogo = findViewById(R.id.imgFinalLogo)
        layoutNarrativa = findViewById(R.id.layoutNarrativa)
        txtNarrativa = findViewById(R.id.txtNarrativa)
        btnSkip = findViewById(R.id.btnSkip)
        viewBlackOverlay = findViewById(R.id.viewBlackOverlay)
        
        imgCity.setImageResource(R.drawable.intro_paisagem)
        imgProfessions.setImageResource(R.drawable.intro_profissoes)
        imgFinalLogo.setImageResource(R.drawable.app_icon_base)

        btnSkip.setOnClickListener { finishIntro() }
        containerCinematic.setOnClickListener { advanceNarrative() }

        startSequence()
    }

    private fun startSequence() {
        lifecycleScope.launch {
            SoundManager.playWithFadeIn(this@IntroActivity, "aventura", 2000)

            txtLogo.animate().alpha(1f).setDuration(1500).start()
            delay(3000)
            txtLogo.animate().alpha(0f).setDuration(1500).start()
            delay(1500)

            showCinematic()
        }
    }

    private fun showCinematic() {
        containerCinematic.visibility = View.VISIBLE
        containerCinematic.animate().alpha(1f).setDuration(2000).start()

        applyZoom(imgCity)
        applyZoom(imgProfessions)

        startNarrative()
    }

    private fun applyZoom(view: ImageView) {
        val zoomAnimator = ValueAnimator.ofFloat(1f, 1.10f)
        zoomAnimator.duration = 180000 
        zoomAnimator.interpolator = LinearInterpolator()
        zoomAnimator.addUpdateListener { animation ->
            val scale = animation.animatedValue as Float
            view.scaleX = scale
            view.scaleY = scale
        }
        zoomAnimator.start()
    }

    private fun startNarrative() {
        if (currentTextIndex < narrativa.size) {
            showText(narrativa[currentTextIndex])
        }
    }

    private fun advanceNarrative() {
        if (typingJob?.isActive == true) {
            typingJob?.cancel()
            txtNarrativa.text = narrativa[currentTextIndex]
            return
        }

        currentTextIndex++
        
        if (currentTextIndex == 9) {
            imgProfessions.animate().alpha(1f).setDuration(3000).start()
        }

        if (currentTextIndex < narrativa.size) {
            showText(narrativa[currentTextIndex])
        } else {
            showFinalLogo()
        }
    }

    private fun showText(text: String) {
        typingJob?.cancel()
        txtNarrativa.alpha = 0f
        txtNarrativa.text = ""
        
        lifecycleScope.launch {
            txtNarrativa.animate().alpha(1f).setDuration(500).start()
            typingJob = lifecycleScope.launch {
                text.forEach { char ->
                    txtNarrativa.append(char.toString())
                    delay(40) 
                }
            }
        }
    }

    private fun showFinalLogo() {
        layoutNarrativa.animate().alpha(0f).setDuration(1000).start()
        btnSkip.animate().alpha(0f).setDuration(1000).start()
        
        imgFinalLogo.visibility = View.VISIBLE
        imgFinalLogo.animate().alpha(1f).setDuration(3000).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                // Aguarda 7 segundos (total de 10 contando o fade) se o usuário não fizer nada
                lifecycleScope.launch {
                    delay(7000)
                    if (!isIntroFinished) finishIntro()
                }
            }
        }).start()
    }

    private fun finishIntro() {
        if (isIntroFinished) return
        isIntroFinished = true

        viewBlackOverlay.visibility = View.VISIBLE
        viewBlackOverlay.animate().alpha(1f).setDuration(2000)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    SoundManager.fadeOutAndStop(2000) {
                        runOnUiThread {
                            if (!isReviewMode) {
                                PlayerManager.player.introConcluida = true
                                PlayerManager.save(this@IntroActivity)
                                startActivity(Intent(this@IntroActivity, CreateCharacterActivity::class.java))
                            }
                            finish()
                        }
                    }
                }
            }).start()
    }
}
