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

        fun updateUIStatus() {
            txtSaldo.text = "Saldo: ${CurrencyUtils.formatar(player.dinheiro)}"
            val ocupacao = player.mochila.values.sum()
            txtCapacidade.text = "Mochila: $ocupacao / ${player.capacidadeMochila}"
            if (ocupacao >= player.capacidadeMochila) {
                txtCapacidade.setTextColor(android.graphics.Color.RED)
            } else {
                txtCapacidade.setTextColor(android.graphics.Color.parseColor("#666666"))
            }
        }

        txtSaldo.setOnClickListener {
            CurrencyUtils.mostrarSaldoExato(this, PlayerManager.player.dinheiro)
        }

        updateUIStatus()

        adapter = LojaAdapter(this, itensVisuais,
            onBuyClick = { item ->
                mostrarConfirmacaoCompra(item) {
                    updateUIStatus()
                }
            },
            onSellClick = { item ->
                mostrarConfirmacaoVenda(item) {
                    updateUIStatus()
                }
            }
        )
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
                    // Clique no card não faz mais compra automática para evitar bug de venda/compra dupla
                }
            }
        }

        btnVoltar.setOnClickListener { finish() }
    }

    private fun executarCompra(item: Equipment, onSucesso: () -> Unit) {
        val result = GameEngine.dispatch(GameAction.BuyItem(item))
        when (result) {
            is EngineResult.Success -> {
                Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
                adapter.notifyDataSetChanged()
                onSucesso()
                PlayerManager.save(this)
            }
            is EngineResult.Failure -> {
                Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarConfirmacaoCompra(item: Equipment, onSucesso: () -> Unit) {
        val precoAtual = com.typingfrontier.EconomyManager.precoInflacionado(item.preco)
        
        androidx.appcompat.app.AlertDialog.Builder(this, R.style.Theme_TypingFrontier_ShopDialog)
            .setTitle("Confirmar Compra")
            .setMessage("Deseja comprar ${item.nome}?\n\nPreço: ${CurrencyUtils.formatar(precoAtual)}\n\nVocê tem certeza que deseja adquirir este item?")
            .setPositiveButton("COMPRAR") { _, _ ->
                executarCompra(item, onSucesso)
            }
            .setNegativeButton("CANCELAR", null)
            .show()
            .also { dialog ->
                val color = android.graphics.Color.parseColor("#FB121212")
                dialog.window?.findViewById<android.view.View>(androidx.appcompat.R.id.parentPanel)?.let { panel ->
                    panel.backgroundTintList = android.content.res.ColorStateList.valueOf(color)
                }
            }
    }

    private fun mostrarConfirmacaoVenda(item: Equipment, onSucesso: () -> Unit) {
        val precoAtual = com.typingfrontier.EconomyManager.precoInflacionado(item.preco)
        val valorVenda = Math.round(precoAtual * 0.40).toInt()

        androidx.appcompat.app.AlertDialog.Builder(this, R.style.Theme_TypingFrontier_ShopDialog)
            .setTitle("Confirmar Venda")
            .setMessage("Deseja vender ${item.nome} por ${CurrencyUtils.formatar(valorVenda)}?\n(40% do valor de mercado)")
            .setPositiveButton("VENDER") { _, _ ->
                val result = GameEngine.dispatch(GameAction.SellItem(item))
                when (result) {
                    is EngineResult.Success -> {
                        Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
                        adapter.notifyDataSetChanged()
                        onSucesso()
                        PlayerManager.save(this)
                    }
                    is EngineResult.Failure -> {
                        Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("CANCELAR", null)
            .show()
            .also { dialog ->
                val color = android.graphics.Color.parseColor("#FB121212")
                dialog.window?.findViewById<android.view.View>(androidx.appcompat.R.id.parentPanel)?.let { panel ->
                    panel.backgroundTintList = android.content.res.ColorStateList.valueOf(color)
                }
            }
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
