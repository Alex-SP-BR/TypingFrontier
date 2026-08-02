package com.typingfrontier.shop

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.typingfrontier.EconomyManager
import com.typingfrontier.R
import com.typingfrontier.economy.Equipment

class LojaAdapter(
    private val context: Context,
    private val itens: List<Equipment>
) : BaseAdapter() {

    override fun getCount(): Int = itens.size

    override fun getItem(position: Int): Any = itens[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_loja, parent, false)

        val item = itens[position]
        val currentPlayer = com.typingfrontier.PlayerManager.player
        val itemEquipado = com.typingfrontier.economy.ProfessionManager.getEquipment(currentPlayer.equipamentoId)

        val txtNome = view.findViewById<TextView>(R.id.txtItemNome)
        val txtDescricao = view.findViewById<TextView>(R.id.txtItemDescricao)
        val txtPreco = view.findViewById<TextView>(R.id.txtItemPreco)
        val img = view.findViewById<ImageView>(R.id.imgItem)

        txtNome.text = item.nome
        
        if (currentPlayer.equipamentoId == item.id) {
            txtDescricao.text = "${item.descricao}\nBônus: +${item.bonus} em ${item.atributoAlvo}"
            txtPreco.text = "✅ EQUIPADO"
            txtPreco.setTextColor(android.graphics.Color.parseColor("#2E7D32"))
        } else if (item.id == "blessing" && currentPlayer.temBlessing) {
            txtDescricao.text = "${item.descricao}\nBônus: +${item.bonus} em ${item.atributoAlvo}"
            txtPreco.text = "🛡️ ATIVA"
            txtPreco.setTextColor(android.graphics.Color.parseColor("#1565C0"))
        } else {
            val precoOriginal = if (item.id == "blessing") item.preco else com.typingfrontier.EconomyManager.precoInflacionado(item.preco)
            
            if (item.id != "blessing" && itemEquipado != null) {
                val credito = (itemEquipado.preco * 0.4).toInt()
                val precoFinal = (precoOriginal - credito).coerceAtLeast(0)
                
                txtDescricao.text = "${item.descricao}\nBônus: +${item.bonus} em ${item.atributoAlvo}\n(Crédito de R$ $credito pelo item atual)"
                txtPreco.text = "💰 R$ $precoFinal"
            } else {
                txtDescricao.text = "${item.descricao}\nBônus: +${item.bonus} em ${item.atributoAlvo}"
                txtPreco.text = "💰 R$ $precoOriginal"
            }

            txtPreco.setTextColor(android.graphics.Color.parseColor("#2E7D32"))
        }

        // Ícone genérico de item
        img.setImageResource(android.R.drawable.ic_menu_agenda)

        return view
    }
}
