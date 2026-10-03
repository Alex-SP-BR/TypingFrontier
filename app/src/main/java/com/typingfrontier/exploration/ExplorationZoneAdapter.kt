package com.typingfrontier.exploration

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_exploration_zone, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val zona = zonas[position]
        holder.txtNome.text = zona.nome
        
        val imgItem = holder.itemView.findViewById<android.widget.ImageView>(R.id.imgItem)

        if (zona.id == "rio_construcao") {
            holder.txtDesc.text = zona.descricao
            holder.txtNivel.text = "Nível Requerido: ${zona.nivelMinimo}"
            holder.txtNivel.setTextColor(Color.parseColor("#74C6E0"))
            holder.itemView.alpha = 1.0f
            imgItem.setImageBitmap(createEmojiBitmap(holder.itemView.context, "🚄", 40))
            imgItem.imageTintList = null
        } else {
            val sb = StringBuilder(zona.descricao)
            sb.append("\nFoco: ").append(formatAttr(zona.atributoPrincipal))
            sb.append(" | Risco: ").append(zona.riscoBase).append("%")

            val apoios = mutableListOf<String>()
            zona.atributoSecundario?.let { apoios.add(formatAttr(it)) }
            zona.atributoTerciario?.let { apoios.add(formatAttr(it)) }

            if (apoios.isNotEmpty()) {
                sb.append("\nApoio: ").append(apoios.joinToString(" • "))
            }

            holder.txtDesc.text = sb.toString()
            holder.txtNivel.text = "Nível Requerido: ${zona.nivelMinimo}"
            holder.txtNivel.setTextColor(Color.parseColor("#74C6E0"))
            holder.itemView.alpha = 1.0f
            imgItem.setImageResource(android.R.drawable.ic_menu_agenda)
            imgItem.imageTintList = null
        }

        holder.itemView.setOnClickListener { onItemClick(zona) }
    }

    private fun formatAttr(attr: String?): String {
        return when (attr) {
            "FORCA" -> "💪 Força"
            "VELOCIDADE" -> "⚡ Velocidade"
            "RESISTENCIA" -> "🛡️ Resistência"
            "CARISMA" -> "🗣️ Carisma"
            "INTELIGENCIA" -> "🧠 Inteligência"
            else -> attr ?: ""
        }
    }

    private fun createEmojiBitmap(context: Context, emoji: String, sizeDp: Int): Bitmap {
        val sizePx = (sizeDp * context.resources.displayMetrics.density).toInt()
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            textSize = sizePx * 0.7f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val x = sizePx / 2f
        val y = (sizePx / 2f) - ((paint.descent() + paint.ascent()) / 2f)
        canvas.drawText(emoji, x, y, paint)
        return bitmap
    }

    override fun getItemCount(): Int = zonas.size
}
