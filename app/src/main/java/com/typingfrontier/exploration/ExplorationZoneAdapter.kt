package com.typingfrontier.exploration

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.typingfrontier.R

class ExplorationZoneAdapter(
    private val zonas: List<ExplorationZone>,
    private val onItemClick: (ExplorationZone) -> Unit
) : RecyclerView.Adapter<ExplorationZoneAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtNome = view.findViewById<TextView>(R.id.txtItemNome)
        val txtDesc = view.findViewById<TextView>(R.id.txtItemDescricao)
        val txtNivel = view.findViewById<TextView>(R.id.txtItemPreco)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_loja, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val zona = zonas[position]
        holder.txtNome.text = zona.nome
        
        val imgItem = holder.itemView.findViewById<android.widget.ImageView>(R.id.imgItem)

        if (zona.id == "rio_construcao") {
            holder.txtDesc.text = "${zona.descricao}\nTrem de alta velocidade"
            holder.txtNivel.text = "Conteúdo Futuro"
            holder.txtNivel.setTextColor(android.graphics.Color.GRAY)
            holder.itemView.alpha = 0.6f
            imgItem.setImageResource(android.R.drawable.ic_menu_manage) 
            // Destaque sutil: Ícone em tom dourado/obra
            imgItem.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FFD600"))
        } else {
            holder.txtDesc.text = "${zona.descricao}\nFoco: ${zona.atributoFoco} | Risco: ${zona.riscoBase}%"
            holder.txtNivel.text = "Nível Requerido: ${zona.nivelMinimo}"
            holder.txtNivel.setTextColor(android.graphics.Color.parseColor("#FFD600"))
            holder.itemView.alpha = 1.0f
            imgItem.setImageResource(android.R.drawable.ic_menu_agenda)
            imgItem.imageTintList = null
        }

        holder.itemView.setOnClickListener { onItemClick(zona) }
    }

    override fun getItemCount(): Int = zonas.size
}
