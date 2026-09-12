package com.typingfrontier.shop

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.typingfrontier.EconomyManager
import com.typingfrontier.R
import com.typingfrontier.economy.Equipment
import com.typingfrontier.utils.CurrencyUtils

sealed class LojaItem {
    data class HeaderProfissao(val nome: String, val isExpanded: Boolean = false) : LojaItem()
    data class HeaderTipo(val nome: String, val isExpanded: Boolean = false) : LojaItem()
    data class Equipamento(val equipment: Equipment) : LojaItem()
}

class LojaAdapter(
    private val context: Context,
    private val itens: List<LojaItem>,
    private val onBuyClick: (Equipment) -> Unit = {},
    private val onSellClick: (Equipment) -> Unit = {}
) : BaseAdapter() {

    companion object {
        private const val TYPE_HEADER_PROFISSAO = 0
        private const val TYPE_HEADER_TIPO = 1
        private const val TYPE_EQUIPAMENTO = 2
    }

    override fun getCount(): Int = itens.size

    override fun getItem(position: Int): Any = itens[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getItemViewType(position: Int): Int {
        return when (itens[position]) {
            is LojaItem.HeaderProfissao -> TYPE_HEADER_PROFISSAO
            is LojaItem.HeaderTipo -> TYPE_HEADER_TIPO
            is LojaItem.Equipamento -> TYPE_EQUIPAMENTO
        }
    }

    override fun getViewTypeCount(): Int = 3

    override fun isEnabled(position: Int): Boolean = true

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return when (val itemWrapper = itens[position]) {
            is LojaItem.HeaderProfissao -> getHeaderProfissaoView(itemWrapper, convertView, parent)
            is LojaItem.HeaderTipo -> getHeaderTipoView(itemWrapper, convertView, parent)
            is LojaItem.Equipamento -> getEquipamentoView(itemWrapper.equipment, convertView, parent)
        }
    }

    private fun getHeaderProfissaoView(item: LojaItem.HeaderProfissao, convertView: View?, parent: ViewGroup): View {
        val view = (convertView ?: LayoutInflater.from(context).inflate(R.layout.item_loja_header_profissao, parent, false)) as TextView
        val prefix = if (item.isExpanded) "▾ " else "▸ "
        view.text = prefix + item.nome

        // Diferenciação Visual por Profissão (Indicador Lateral)
        val color = getProfessionColor(item.nome)
        val density = context.resources.displayMetrics.density
        val indicator = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setSize((6 * density).toInt(), (24 * density).toInt())
            setColor(color)
            cornerRadius = 2 * density
        }
        view.setCompoundDrawablesWithIntrinsicBounds(indicator, null, null, null)
        view.compoundDrawablePadding = (12 * density).toInt()

        return view
    }

    private fun getHeaderTipoView(item: LojaItem.HeaderTipo, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_loja_header_tipo, parent, false)
        val prefix = if (item.isExpanded) "▾ " else "▸ "
        (view as TextView).text = prefix + item.nome
        return view
    }

    private fun getProfessionColor(nome: String?): Int {
        val n = nome?.uppercase() ?: ""
        return when {
            n.contains("POLICIAL") -> Color.parseColor("#00BCD4") // Azul/Ciano
            n.contains("MÉDICO") -> Color.parseColor("#4CAF50")   // Verde
            n.contains("PROFESSOR") -> Color.parseColor("#9C27B0") // Roxo
            n.contains("ENGENHEIRO") -> Color.parseColor("#FF9800") // Laranja
            n.contains("DETETIVE") -> Color.parseColor("#673AB7")  // Azul-Violeta
            else -> Color.parseColor("#74C6E0") // Padrão (Itens Especiais)
        }
    }

    private fun getEquipamentoView(item: Equipment, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_loja, parent, false)

        val currentPlayer = com.typingfrontier.PlayerManager.player

        val indicator = view.findViewById<View>(R.id.viewIndicatorItem)
        val txtNome = view.findViewById<TextView>(R.id.txtItemNome)
        val txtPosse = view.findViewById<TextView>(R.id.txtItemPosse)
        val txtDescricao = view.findViewById<TextView>(R.id.txtItemDescricao)
        val txtPreco = view.findViewById<TextView>(R.id.txtItemPreco)
        val img = view.findViewById<ImageView>(R.id.imgItem)
        val btnComprar = view.findViewById<android.widget.Button>(R.id.btnComprarItem)
        val btnVender = view.findViewById<android.widget.Button>(R.id.btnVenderItem)

        // Aplica cor da profissão ao indicador lateral do card
        indicator.setBackgroundColor(getProfessionColor(item.profissao))

        txtNome.text = item.nome
        txtDescricao.text = "${item.descricao}\nBônus: +${item.bonus} em ${item.atributoAlvo}"
        
        // 1. Verificar Posse na Mochila e Equipado (Slots + Ponte)
        val qtdNaMochila = currentPlayer.mochila[item.id] ?: 0
        val isEquipado = currentPlayer.slotsEquipados.values.contains(item.id) || currentPlayer.equipamentoId == item.id

        btnComprar.setOnClickListener { onBuyClick(item) }

        if (item.id != "blessing") {
            val statusList = mutableListOf<String>()
            if (qtdNaMochila > 0) statusList.add("Possui: ×$qtdNaMochila")
            if (isEquipado) statusList.add("✅ EQUIPADO")

            if (statusList.isNotEmpty()) {
                txtPosse.text = statusList.joinToString(" | ")
                txtPosse.visibility = View.VISIBLE
            } else {
                txtPosse.visibility = View.GONE
            }

            // Botão Vender (Só para quem tem na mochila)
            if (qtdNaMochila > 0) {
                btnVender.visibility = View.VISIBLE
                btnVender.setOnClickListener { onSellClick(item) }
            } else {
                btnVender.visibility = View.GONE
            }
            btnComprar.visibility = View.VISIBLE
        } else {
            txtPosse.visibility = View.GONE
            btnVender.visibility = View.GONE
            
            // Lógica especial para Blessing
            btnComprar.visibility = if (currentPlayer.temBlessing) View.GONE else View.VISIBLE
        }

        // 2. Lógica de Preço e Estado Especial (Blessing)
        if (item.id == "blessing" && currentPlayer.temBlessing) {
            txtPreco.text = "🛡️ ATIVA"
            txtPreco.setTextColor(android.graphics.Color.parseColor("#1565C0"))
            txtPreco.setCompoundDrawables(null, null, null, null)
        } else {
            val precoExibido = if (item.id == "blessing") item.preco else com.typingfrontier.EconomyManager.precoInflacionado(item.preco)
            
            // Configuração do ícone da moeda com tamanho controlado (20dp)
            val coinIcon = androidx.core.content.ContextCompat.getDrawable(context, com.typingfrontier.R.drawable.fron_coin)
            val size = (20 * context.resources.displayMetrics.density).toInt()
            coinIcon?.setBounds(0, 0, size, size)
            txtPreco.setCompoundDrawables(coinIcon, null, null, null)

            txtPreco.text = CurrencyUtils.formatar(precoExibido)
            txtPreco.setTextColor(android.graphics.Color.parseColor("#2E7D32"))
        }

        // Preparado para imagens individuais com fallback para ícone genérico
        val placeholder = android.R.drawable.ic_menu_agenda
        img.setImageResource(item.imagemRes ?: placeholder)

        return view
    }
}
