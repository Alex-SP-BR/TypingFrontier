package com.typingfrontier.station

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.typingfrontier.*
import com.typingfrontier.economy.ProfessionManager

class StationActivity : AppCompatActivity(), StationGridView.InteractionListener {
    
    private var armoireDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_station)
        
        supportActionBar?.hide()

        val imgBg = findViewById<ImageView>(R.id.imgBackgroundStation)
        val gridView = findViewById<StationGridView>(R.id.stationGridView)

        // Vincula o grid à imagem para que ele saiba onde desenhar
        gridView.setTarget(imgBg)
        gridView.setInteractionListener(this)

        // --- TESTE 3: CONFIGURAR DADOS DO JOGADOR ---
        configurarJogador(gridView)
        
        // --- ADICIONAR NPC DE TESTE ---
        configurarNpcTeste(gridView)
    }

    private fun configurarNpcTeste(gridView: StationGridView) {
        val npcSprites = mapOf(
            "cima" to R.drawable.npc_estacao_homem_01_cima,
            "baixo" to R.drawable.npc_estacao_homem_01_baixo,
            "esquerda" to R.drawable.npc_estacao_homem_01_esquerda,
            "direita" to R.drawable.npc_estacao_homem_01_direita
        )
        
        // Waypoints da rota contínua (X, Y) - Todos os pontos são Walkable na grade 12x24
        val route = listOf(
            android.graphics.Point(7, 12),
            android.graphics.Point(7, 18),
            android.graphics.Point(3, 18),
            android.graphics.Point(3, 5),
            android.graphics.Point(5, 5),
            android.graphics.Point(5, 12)
        )
        
        gridView.setNpcData(
            name = "NPC Estação 01",
            x = 5,
            y = 12,
            direction = "baixo",
            sprites = npcSprites,
            route = route
        )

        // Inicia a rota do NPC após 2 segundos
        findViewById<android.view.View>(R.id.stationGridView).postDelayed({
            gridView.startNpcMovingTo("NPC Estação 01", 7, 12)
        }, 2000)
    }

    override fun onArmoireTapped() {
        abrirInterfaceArmario()
    }

    private fun abrirInterfaceArmario() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_armario, null)
        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)
        
        armoireDialog = builder.create()
        armoireDialog?.show()

        dialogView.findViewById<Button>(R.id.btnFecharArmario).setOnClickListener {
            armoireDialog?.dismiss()
        }

        atualizarListasArmario(dialogView)
    }

    private fun atualizarListasArmario(view: View) {
        val player = PlayerManager.player
        val layoutMochila = view.findViewById<LinearLayout>(R.id.layoutMochilaTransfer)
        val layoutArmario = view.findViewById<LinearLayout>(R.id.layoutArmarioTransfer)
        val txtCapacidade = view.findViewById<TextView>(R.id.txtCapacidadeArmario)

        layoutMochila.removeAllViews()
        layoutArmario.removeAllViews()

        val totalArmario = player.armario.values.sum()
        txtCapacidade.text = "Capacidade: $totalArmario/${player.capacidadeArmario}"

        // Preencher Mochila
        player.mochila.forEach { (id, qtd) ->
            if (qtd > 0) {
                val itemView = criarItemTransfer(id, qtd, true, view)
                layoutMochila.addView(itemView)
            }
        }

        // Preencher Armário
        player.armario.forEach { (id, qtd) ->
            if (qtd > 0) {
                val itemView = criarItemTransfer(id, qtd, false, view)
                layoutArmario.addView(itemView)
            }
        }
    }

    private fun criarItemTransfer(id: String, qtd: Int, isMochila: Boolean, dialogView: View): View {
        val itemView = LayoutInflater.from(this).inflate(R.layout.item_transfer, null)
        val equip = ProfessionManager.getEquipment(id)
        
        itemView.findViewById<TextView>(R.id.txtNomeTransfer).text = equip?.nome ?: id
        itemView.findViewById<TextView>(R.id.txtQtdTransfer).text = "Qtd: $qtd"
        
        val img = itemView.findViewById<ImageView>(R.id.imgItemTransfer)
        if (equip?.imagemRes != null) {
            img.setImageResource(equip.imagemRes)
        }

        val btn = itemView.findViewById<Button>(R.id.btnActionTransfer)
        btn.text = if (isMochila) "GUARDAR" else "RETIRAR"
        
        btn.setOnClickListener {
            val action = if (isMochila) GameAction.DepositItem(id) else GameAction.WithdrawItem(id)
            val result = GameEngine.dispatch(action)
            
            if (result is EngineResult.Success) {
                atualizarListasArmario(dialogView)
                Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
            } else if (result is EngineResult.Failure) {
                Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
            }
        }

        return itemView
    }

    private fun configurarJogador(gridView: StationGridView) {
        val player = com.typingfrontier.PlayerManager.player
        val sexoChar = if (player.sexo == "Feminino") "f" else "m"
        
        val baseNome = when (player.profissao) {
            "Médico" -> if (sexoChar == "f") "medica" else "medico"
            "Engenheiro" -> if (sexoChar == "f") "engenheira" else "engenheiro"
            "Professor" -> if (sexoChar == "f") "professora" else "professor"
            "Detetive" -> "detetive"
            "Policial" -> "policial"
            else -> "homem"
        }
        
        val sprites = mutableMapOf<String, Int>()
        val directions = listOf("frente", "costas", "esquerda", "direita")
        
        for (dir in directions) {
            val resName = "${baseNome}_${sexoChar}_$dir"
            val resId = resources.getIdentifier(resName, "drawable", packageName)
            if (resId != 0) sprites[dir] = resId
        }
        
        val nomeExibicao = if (player.nome.isNotEmpty()) player.nome else "Viajante"
        gridView.setPlayerData(sprites, nomeExibicao)
    }
}
