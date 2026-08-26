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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shop)

        val player = PlayerManager.player
        val listView = findViewById<ListView>(R.id.listViewLoja)
        val btnVoltar = findViewById<Button>(R.id.btnVoltar)
        val txtSaldo = findViewById<TextView>(R.id.txtSaldoLoja)

        txtSaldo.text = "Saldo: ${CurrencyUtils.formatar(player.dinheiro)}"
        txtSaldo.setOnClickListener {
            CurrencyUtils.mostrarSaldoExato(this, PlayerManager.player.dinheiro)
        }

        val itensBase = ProfessionManager.getItemsForShop(player.profissao)
        val itensComBlessing = itensBase.toMutableList()
        
        val precoBlessing = 100 + (player.nivel * 100)
        val blessingItem = Equipment(
            id = "blessing",
            nome = "🕊️ Benção de Proteção",
            preco = precoBlessing,
            atributoAlvo = "PROGRESSO",
            bonus = 100,
            descricao = "Protege contra perda de LVL e Atributos. Imprescindível para zonas perigosas."
        )
        itensComBlessing.add(blessingItem)

        val adapter = LojaAdapter(this, itensComBlessing)
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val item = itensComBlessing[position]
            val result = GameEngine.dispatch(GameAction.BuyItem(item))
            
            when (result) {
                is EngineResult.Success -> {
                    Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
                    // Atualiza a lista para mostrar novos preços/créditos após a compra
                    adapter.notifyDataSetChanged()
                }
                is EngineResult.Failure -> {
                    Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
                }
            }
            
            txtSaldo.text = "Saldo: ${CurrencyUtils.formatar(player.dinheiro)}"
            PlayerManager.save(this)
        }

        btnVoltar.setOnClickListener { finish() }
    }
}
