package com.typingfrontier.shop

import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.typingfrontier.EconomyManager
import com.typingfrontier.PlayerManager
import com.typingfrontier.R
import com.typingfrontier.economy.Equipment
import com.typingfrontier.economy.ProfessionManager
import com.typingfrontier.utils.CurrencyUtils
import com.typingfrontier.utils.ViewUtils

class ShopActivity : AppCompatActivity() {

    private var professionExpanded: String? = null
    private var typeExpanded: String? = null
    private var specialExpanded: Boolean = true
    private val itensVisuais = mutableListOf<LojaItem>()
    private lateinit var adapter: LojaAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shop)

        val player = PlayerManager.player

        val txtDinheiro = findViewById<TextView>(R.id.txtSaldoLoja)
        val listLoja = findViewById<ListView>(R.id.listViewLoja)
        val txtCapacidade = findViewById<TextView>(R.id.txtCapacidadeLoja)

        fun updateUIStatus() {
            val coinIcon = ViewUtils.getCoinDrawable(this)
            val size = (20 * resources.displayMetrics.density).toInt()
            coinIcon.setBounds(0, 0, size, size)
            
            val saldoValor = CurrencyUtils.formatar(player.dinheiro)
            val baseText = "Saldo:  $saldoValor"
            val spannable = SpannableStringBuilder(baseText)
            
            // Posiciona a moeda no espaço entre "Saldo:" e o valor
            val imageSpan = ImageSpan(coinIcon, ImageSpan.ALIGN_BOTTOM)
            spannable.setSpan(imageSpan, 7, 8, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            
            txtDinheiro.text = spannable
            
            val ocupacaoAtual = player.mochila.values.sum()
            txtCapacidade.text = "Mochila: $ocupacaoAtual/${player.capacidadeMochila}"
        }

        updateUIStatus()
        updateShopList()

        adapter = LojaAdapter(
            this,
            itensVisuais,
            onBuyClick = { item -> mostrarConfirmacaoCompra(item) { updateUIStatus(); updateShopList() } },
            onSellClick = { item -> mostrarConfirmacaoVenda(item) { updateUIStatus(); updateShopList() } }
        )
        listLoja.adapter = adapter

        listLoja.setOnItemClickListener { _, _, position, _ ->
            val item = itensVisuais[position]
            when (item) {
                is LojaItem.HeaderProfissao -> {
                    if (item.nome == "ITENS ESPECIAIS") {
                        specialExpanded = !specialExpanded
                    } else {
                        professionExpanded = if (professionExpanded == item.nome) null else item.nome
                    }
                    updateShopList()
                }
                is LojaItem.HeaderTipo -> {
                    typeExpanded = if (typeExpanded == item.nome) null else item.nome
                    updateShopList()
                }
                is LojaItem.Equipamento -> {
                    // Ação no clique do item se necessário
                }
            }
        }

        findViewById<android.widget.Button>(R.id.btnVoltar).setOnClickListener { finish() }
    }

    private fun executarCompra(item: Equipment, onComplete: () -> Unit) {
        val result = com.typingfrontier.GameEngine.dispatch(com.typingfrontier.GameAction.BuyItem(item))
        if (result is com.typingfrontier.EngineResult.Success) {
            android.widget.Toast.makeText(this, result.message, android.widget.Toast.LENGTH_SHORT).show()
            PlayerManager.save(this)
            onComplete()
        } else if (result is com.typingfrontier.EngineResult.Failure) {
            android.widget.Toast.makeText(this, result.message, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun mostrarConfirmacaoCompra(item: Equipment, onComplete: () -> Unit) {
        val preco = if (item.id == "blessing" || item.id == "medicine") item.preco else EconomyManager.precoInflacionado(item.preco)
        AlertDialog.Builder(this)
            .setTitle("Confirmar Compra")
            .setMessage("Deseja comprar ${item.nome} por ${CurrencyUtils.formatar(preco)}?")
            .setPositiveButton("Comprar") { _, _ -> executarCompra(item, onComplete) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarConfirmacaoVenda(item: Equipment, onComplete: () -> Unit) {
        val precoAtual = EconomyManager.precoInflacionado(item.preco)
        val valorVenda = Math.round(precoAtual * 0.40).toInt()
        
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Confirmar Venda")
        
        val p = PlayerManager.player
        val isEquipado = p.slotsEquipados.values.contains(item.id) || p.equipamentoId == item.id
        
        if (isEquipado) {
            builder.setMessage("Este item (${item.nome}) está equipado! Você deve desequipar primeiro na tela de Status para poder vender.")
            builder.setPositiveButton("Entendido", null)
        } else {
            builder.setMessage("Deseja vender ${item.nome} por ${CurrencyUtils.formatar(valorVenda)}?\n(40% do valor de mercado atual)")
            builder.setPositiveButton("Vender") { _, _ ->
                val result = com.typingfrontier.GameEngine.dispatch(com.typingfrontier.GameAction.SellItem(item))
                if (result is com.typingfrontier.EngineResult.Success) {
                    android.widget.Toast.makeText(this, result.message, android.widget.Toast.LENGTH_SHORT).show()
                    PlayerManager.save(this)
                    onComplete()
                } else if (result is com.typingfrontier.EngineResult.Failure) {
                    android.widget.Toast.makeText(this, result.message, android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            builder.setNegativeButton("Cancelar", null)
        }
        builder.show()
    }

    private fun updateShopList() {
        itensVisuais.clear()
        val player = PlayerManager.player

        // 1. ITENS ESPECIAIS
        itensVisuais.add(LojaItem.HeaderProfissao("ITENS ESPECIAIS", specialExpanded))
        if (specialExpanded) {
            val precoMed = EconomyManager.getMedicinePrice(player.nivel)
            val medicineItem = Equipment(
                id = "medicine",
                nome = "💊 Medicamento",
                preco = precoMed,
                atributoAlvo = "CLÍNICO",
                bonus = 1,
                descricao = "Trata Traumas, recupera HP e restaura 50% de XP e Atributos perdidos. (Estoque: ${player.estoqueMedicamento}/${player.limiteTraumas})"
            )
            itensVisuais.add(LojaItem.Equipamento(medicineItem))

            val precoBen = EconomyManager.getBlessingPrice(player.nivel)
            val blessingItem = Equipment(
                id = "blessing",
                nome = "🛡️ Seguro de Equipamentos",
                preco = precoBen,
                atributoAlvo = "SEGURO",
                bonus = 100,
                descricao = "Protege seus itens equipados durante um Colapso. (Estoque: ${player.estoqueBencao}/${player.limiteTraumas})"
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
                        "CAPACETE" -> "CAPACETES"
                        "MICROFONE" -> "MICROFONES"
                        "TONFA" -> "TONFAS"
                        "ALGEMAS" -> "ALGEMAS"
                        "BOTA" -> "BOTAS"
                        "CRACHA" -> "CRACHÁS"
                        "JALECO" -> "JALECOS"
                        "BOLSA" -> "BOLSAS"
                        "SAPATO" -> "SAPATOS"
                        "RADIO" -> "RÁDIOS"
                        "NOTEBOOK" -> "NOTEBOOKS"
                        "MEDIDOR" -> "MEDIDORES"
                        "OCULOS" -> "ÓCULOS"
                        "TERNO" -> "TERNOS"
                        "REGUA" -> "RÉGUAS"
                        "CELULAR" -> "CELULARES"
                        "CHAPEU" -> "CHAPÉUS"
                        "GRAVATA" -> "GRAVATAS"
                        "SOBRETUDO" -> "SOBRETUDOS"
                        "CADERNO" -> "CADERNOS"
                        "LANTERNA" -> "LANTERNAS"
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
        if (::adapter.isInitialized) {
            adapter.notifyDataSetChanged()
        }
    }
}
