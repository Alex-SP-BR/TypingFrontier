package com.typingfrontier.shop

import android.os.Bundle
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.typingfrontier.PlayerManager
import com.typingfrontier.R
import com.typingfrontier.GameEngine
import com.typingfrontier.GameAction
import com.typingfrontier.EngineResult
import com.typingfrontier.utils.CurrencyUtils
import com.typingfrontier.economy.ProfessionManager
import com.typingfrontier.economy.Equipment

class ShopActivity : AppCompatActivity() {

    private var professionExpanded: String? = null
    private var typeExpanded: String? = null
    private var specialExpanded: Boolean = false
    private val itensVisuais = mutableListOf<LojaItem>()
    private lateinit var adapter: LojaAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shop)

        val player = PlayerManager.player
        val listView = findViewById<ListView>(R.id.listViewLoja)
        val btnVoltar = findViewById<Button>(R.id.btnVoltar)
        val txtSaldo = findViewById<TextView>(R.id.txtSaldoLoja)
        val txtCapacidade = findViewById<TextView>(R.id.txtCapacidadeLoja)

        txtSaldo.text = "Saldo: ${CurrencyUtils.formatar(player.dinheiro)}"
        txtSaldo.setOnClickListener {
            CurrencyUtils.mostrarSaldoExato(this, PlayerManager.player.dinheiro)
        }

        val ocupacao = player.mochila.values.sum()
        txtCapacidade.text = "Mochila: $ocupacao / ${player.capacidadeMochila}"
        if (ocupacao >= player.capacidadeMochila) {
            txtCapacidade.setTextColor(android.graphics.Color.RED)
        } else {
            txtCapacidade.setTextColor(android.graphics.Color.parseColor("#666666"))
        }

        adapter = LojaAdapter(this, itensVisuais)
        listView.adapter = adapter

        updateShopList()

        listView.setOnItemClickListener { _, _, position, _ ->
            val itemWrapper = itensVisuais[position]
            when (itemWrapper) {
                is LojaItem.HeaderProfissao -> {
                    if (itemWrapper.nome == "ITENS ESPECIAIS") {
                        specialExpanded = !specialExpanded
                        if (specialExpanded) {
                            professionExpanded = null
                            typeExpanded = null
                        }
                    } else {
                        val profNome = itemWrapper.nome
                        if (professionExpanded == profNome) {
                            professionExpanded = null
                            typeExpanded = null
                        } else {
                            professionExpanded = profNome
                            typeExpanded = null
                            specialExpanded = false
                        }
                    }
                    updateShopList()
                }
                is LojaItem.HeaderTipo -> {
                    val tipoNome = itemWrapper.nome
                    if (typeExpanded == tipoNome) {
                        typeExpanded = null
                    } else {
                        typeExpanded = tipoNome
                    }
                    updateShopList()
                }
                is LojaItem.Equipamento -> {
                    val item = itemWrapper.equipment
                    val result = GameEngine.dispatch(GameAction.BuyItem(item))
                    
                    when (result) {
                        is EngineResult.Success -> {
                            Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
                            adapter.notifyDataSetChanged()

                            // Atualizar Saldo e Capacidade após compra
                            txtSaldo.text = "Saldo: ${CurrencyUtils.formatar(player.dinheiro)}"
                            val novaOcupacao = player.mochila.values.sum()
                            txtCapacidade.text = "Mochila: $novaOcupacao / ${player.capacidadeMochila}"
                            if (novaOcupacao >= player.capacidadeMochila) {
                                txtCapacidade.setTextColor(android.graphics.Color.RED)
                            } else {
                                txtCapacidade.setTextColor(android.graphics.Color.parseColor("#666666"))
                            }
                        }
                        is EngineResult.Failure -> {
                            Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                    
                    PlayerManager.save(this)
                }
            }
        }

        btnVoltar.setOnClickListener { finish() }
    }

    private fun updateShopList() {
        itensVisuais.clear()
        val player = PlayerManager.player

        // 1. ITENS ESPECIAIS
        itensVisuais.add(LojaItem.HeaderProfissao("ITENS ESPECIAIS", specialExpanded))
        if (specialExpanded) {
            val precoBlessing = 100 + (player.nivel * 100)
            val blessingItem = Equipment(
                id = "blessing",
                nome = "🕊️ Benção de Proteção",
                preco = precoBlessing,
                atributoAlvo = "PROGRESSO",
                bonus = 100,
                descricao = "Protege contra perda de LVL e Atributos. Imprescindível para zonas perigosas."
            )
            itensVisuais.add(LojaItem.Equipamento(blessingItem))
        }

        // 2. EQUIPAMENTOS PROFISSIONAIS
        val todos = ProfessionManager.getAllEquipments()
        val agrupadosPorProfissao = todos.groupBy { it.profissao ?: "GERAL" }
        val ordemProfissoes = listOf("Policial", "Médico", "Professor", "Engenheiro", "Detetive")

        for (prof in ordemProfissoes) {
            val equipamentosDaProfissao = agrupadosPorProfissao[prof] ?: continue
            val profUpper = prof.uppercase()
            val isProfExpanded = professionExpanded == profUpper
            
            itensVisuais.add(LojaItem.HeaderProfissao(profUpper, isProfExpanded))
            
            if (isProfExpanded) {
                val agrupadosPorTipo = equipamentosDaProfissao.groupBy { it.tipo }
                for ((tipo, equipamentos) in agrupadosPorTipo) {
                    val nomeTipo = when(tipo) {
                        "COLETE" -> "COLETES"
                        "ESTETOSCOPIO" -> "ESTETOSCÓPIOS"
                        "LIVRO" -> "LIVROS"
                        "MALETA" -> "MALETAS"
                        "LUPA" -> "LUPAS"
                        "DRONE" -> "DRONES"
                        else -> tipo.uppercase() + "S"
                    }
                    val isTipoExpanded = typeExpanded == nomeTipo
                    itensVisuais.add(LojaItem.HeaderTipo(nomeTipo, isTipoExpanded))
                    
                    if (isTipoExpanded) {
                        for (equip in equipamentos) {
                            itensVisuais.add(LojaItem.Equipamento(equip))
                        }
                    }
                }
            }
        }
        adapter.notifyDataSetChanged()
    }
}
