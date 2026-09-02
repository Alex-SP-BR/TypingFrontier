package com.typingfrontier.social

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.typingfrontier.databinding.ActivityRankingBinding
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RankingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRankingBinding
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var adapter: RankingAdapter? = null
    
    private var currentCategory = RankingCategory.LEVEL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRankingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarAbas()
        configurarRecycler()
        configurarBotoes()

        carregarRanking()
    }

    private fun configurarAbas() {
        RankingCategory.values().forEach { cat ->
            binding.tabLayout.addTab(binding.tabLayout.newTab().setText(cat.displayName))
        }

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val index = tab?.position ?: 0
                currentCategory = RankingCategory.values()[index]
                carregarRanking()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun configurarRecycler() {
        binding.recyclerRanking.layoutManager = LinearLayoutManager(this)
        binding.swipeRefresh.setOnRefreshListener {
            carregarRanking()
        }
    }

    private fun configurarBotoes() {
        binding.btnVoltar.setOnClickListener {
            finish()
        }
        binding.btnRefresh.setOnClickListener {
            carregarRanking()
        }
        binding.btnForumCategoria.setOnClickListener {
            val intent = Intent(this, DiscussionActivity::class.java)
            intent.putExtra("EXTRA_CATEGORY", currentCategory.columnName)
            startActivity(intent)
        }
    }

    private fun carregarRanking() {
        binding.progressBar.visibility = View.VISIBLE
        binding.swipeRefresh.isRefreshing = true

        scope.launch {
            // Garante que a identidade social foi inicializada antes de destacar o usuário
            SocialProfileRepository.initializeSocialIdentity()
            SocialProfileRepository.awaitInitialization()

            val currentUserId = SocialProfileRepository.currentProfile?.id

            try {
                // Consulta ao Supabase
                val result = withContext(Dispatchers.IO) {
                    SupabaseManager.client.postgrest["profiles"]
                        .select() {
                            order(currentCategory.columnName, order = Order.DESCENDING)
                            // Segundo critério para empates (Determinístico)
                            order("created_at", order = Order.ASCENDING)
                            limit(100)
                        }.decodeList<SocialProfile>()
                }

                exibirRanking(result, currentUserId)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@RankingActivity, "Ranking temporariamente indisponível. Verifique sua conexão.", Toast.LENGTH_LONG).show()
                }
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun exibirRanking(entries: List<SocialProfile>, currentUserId: String?) {
        if (entries.isEmpty()) {
            Toast.makeText(this, "Nenhum herói encontrado nesta categoria.", Toast.LENGTH_SHORT).show()
        }
        
        adapter = RankingAdapter(entries, currentUserId, currentCategory) { profileId ->
            val intent = Intent(this, SocialProfileActivity::class.java)
            intent.putExtra("EXTRA_USER_ID", profileId)
            startActivity(intent)
        }
        binding.recyclerRanking.adapter = adapter
    }
}
