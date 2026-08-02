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
        holder.txtDesc.text = "${zona.descricao}\nFoco: ${zona.atributoFoco} | Risco: ${zona.riscoBase}%"
        holder.txtNivel.text = "Nível Requerido: ${zona.nivelMinimo}"
        holder.txtNivel.setTextColor(android.graphics.Color.parseColor("#FFD600"))

        holder.itemView.setOnClickListener { onItemClick(zona) }
    }

    override fun getItemCount(): Int = zonas.size
}
