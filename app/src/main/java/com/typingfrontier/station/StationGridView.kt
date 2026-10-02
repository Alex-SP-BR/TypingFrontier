package com.typingfrontier.station

import android.content.Context
import android.graphics.*
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import com.typingfrontier.npc.SharedNpcState
import com.typingfrontier.social.SocialProfileRepository
import com.typingfrontier.utils.ViewUtils
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * Componente visual experimental para estação de trem.
 * Suporta câmera arrastável, zoom e movimentação do personagem.
 */
class StationGridView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    data class StationNPC(
        val name: String,
        var x: Int,
        var y: Int,
        var direction: String,
        val sprites: MutableMap<String, Bitmap> = mutableMapOf(),
        var movePath: MutableList<Point> = mutableListOf(),
        var isMoving: Boolean = false,
        var route: List<Point> = emptyList(),
        var currentWaypointIndex: Int = -1,
        var isCirculating: Boolean = false,
        var anchorX: Int = -1,
        var anchorY: Int = -1,
        var isInteracting: Boolean = false
    )

    /**
     * Modelo para jogadores remotos.
     */
    data class RemotePlayer(
        val id: String,
        val username: String,
        val role: String,
        val gender: String,
        var x: Int,
        var y: Int,
        var direction: String,
        val sprites: MutableMap<String, Bitmap> = mutableMapOf()
    )

    /**
     * Modelo para diálogos temporários no mundo.
     */
    data class WorldDialog(
        val senderId: String,
        val senderName: String,
        val message: String,
        val worldX: Int,
        val worldY: Int,
        val isPrivate: Boolean = false,
        val allowedUsers: Set<String> = emptySet(),
        val expirationTime: Long = System.currentTimeMillis() + 5000L
    )

    interface InteractionListener {
        fun onArmoireTapped()
        fun onNpcTapped(npcName: String)
        fun onTrainTapped()
        fun onPlayerPositionChanged(x: Int, y: Int, direction: String)
        fun onRemotePlayerTapped(userId: String, username: String) {}
        fun onNorthStairsTapped() {}
    }

    private var interactionListener: InteractionListener? = null
    private var targetImageView: ImageView? = null
    
    // Pincéis de Renderização dos Nomes
    private val paintPlayerNameStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#121212")
        textSize = 35f
        textAlign = Paint.Align.CENTER
        style = Paint.Style.STROKE
        strokeWidth = 8f
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        isFakeBoldText = true
    }

    private val paintPlayerName = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 35f
        textAlign = Paint.Align.CENTER
        style = Paint.Style.FILL
        isFakeBoldText = true
    }

    // Pincéis de Renderização para Diálogos Privados
    private val paintPrivateTextStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0A192F")
        textSize = 35f
        textAlign = Paint.Align.CENTER
        style = Paint.Style.STROKE
        strokeWidth = 8f
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        isFakeBoldText = true
    }

    private val paintPrivateText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#74C6E0")
        textSize = 35f
        textAlign = Paint.Align.CENTER
        style = Paint.Style.FILL
        isFakeBoldText = true
    }

    // Diálogos temporários no mundo
    private val activeDialogs = ConcurrentHashMap<String, WorldDialog>()

    // Grid e Mapa
    private val cols = 12
    private val rows = 24

    // Câmera
    private var scaleFactor = 1.0f
    private var offsetX = 0f
    private var offsetY = 0f
    private val matrixCamera = Matrix()
    private val inverseMatrix = Matrix()

    private var currentStationId: String = "sao_paulo"

    fun setStationId(stationId: String) {
        this.currentStationId = stationId
    }

    // Jogador
    private val playerSprites = mutableMapOf<String, Bitmap>()
    private var playerName: String = ""
    private var playerX = 5
    private var playerY = 20
    private var currentDirection = "frente"
    
    // NPCs
    private val npcList = mutableListOf<StationNPC>()
    
    // Jogadores Remotos
    private val remotePlayers = mutableMapOf<String, RemotePlayer>()
    private val remoteSpriteCache = mutableMapOf<String, Map<String, Bitmap>>()

    // Movimentação
    private var movePath = mutableListOf<Point>()
    private var isMoving = false
    private val moveInterval = 200L // Milissegundos entre células
    private var pendingArmoireAction = false
    private var pendingNpcName: String? = null

    // Detectores de Gestos
    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor *= detector.scaleFactor
            scaleFactor = scaleFactor.coerceIn(0.5f, 5.0f)
            invalidate()
            return true
        }
    })

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            handleTap(e.x, e.y)
            return true
        }

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            offsetX -= distanceX
            offsetY -= distanceY
            invalidate()
            return true
        }
    })

    fun setTarget(imageView: ImageView) {
        this.targetImageView = imageView
        imageView.visibility = View.INVISIBLE // Nós desenharemos a imagem
        invalidate()
    }

    fun setInteractionListener(listener: InteractionListener) {
        this.interactionListener = listener
    }

    fun getPlayerX(): Int = playerX
    fun getPlayerY(): Int = playerY

    fun setInitialPosition(x: Int, y: Int, direction: String) {
        this.playerX = x
        this.playerY = y
        this.currentDirection = direction
        invalidate()
    }

    fun getNpcPosition(npcName: String): Point? {
        val npc = npcList.find { it.name == npcName } ?: return null
        return Point(npc.x, npc.y)
    }

    /**
     * Adiciona ou substitui um diálogo temporário sobre o cenário.
     * A posição (worldX, worldY) é capturada no exato instante do envio/recebimento
     * e o balão PERMANECE nessa posição durante sua existência (5s).
     */
    fun addSpeechDialog(
        senderId: String,
        senderName: String,
        message: String,
        worldX: Int,
        worldY: Int,
        isPrivate: Boolean = false,
        allowedUsers: Set<String> = emptySet()
    ) {
        val dialog = WorldDialog(
            senderId = senderId,
            senderName = senderName,
            message = message,
            worldX = worldX,
            worldY = worldY,
            isPrivate = isPrivate,
            allowedUsers = allowedUsers,
            expirationTime = System.currentTimeMillis() + 5000L
        )
        val dialogKey = if (isPrivate) "private_$senderId" else "public_$senderId"
        activeDialogs[dialogKey] = dialog
        postInvalidate()
    }

    fun setNpcInteracting(npcName: String, interacting: Boolean) {
        npcList.find { it.name == npcName }?.let {
            it.isInteracting = interacting
        }
    }

    companion object {
        private val globalSpriteCache = ConcurrentHashMap<Int, Bitmap>()
        private val globalInFlight = ConcurrentHashMap.newKeySet<Int>()
        private val globalSpriteExecutor = Executors.newFixedThreadPool(2)
    }

    fun setPlayerData(sprites: Map<String, Int>, name: String) {
        this.playerName = name
        playerSprites.clear()
        
        // Redimensionamento preventivo para evitar ANR
        val targetProcessingHeight = 256
        
        sprites.forEach { (dir, resId) ->
            loadSprite(resId, targetProcessingHeight) { transparentBmp ->
                playerSprites[dir] = transparentBmp
            }?.let {
                playerSprites[dir] = it
            }
        }
        invalidate()
    }

    fun clearNpcs() {
        npcList.clear()
        invalidate()
    }

    /**
     * Adiciona ou atualiza um NPC na estação.
     */
    fun setNpcData(
        name: String, 
        x: Int, 
        y: Int, 
        direction: String, 
        sprites: Map<String, Int>, 
        route: List<Point> = emptyList(),
        isCirculating: Boolean = false
    ) {
        val npc = StationNPC(name, x, y, direction, route = route, isCirculating = isCirculating)
        if (isCirculating) {
            npc.anchorX = x
            npc.anchorY = y
        }
        
        val targetProcessingHeight = 256
        
        sprites.forEach { (dir, resId) ->
            loadSprite(resId, targetProcessingHeight) { transparentBmp ->
                npc.sprites[dir] = transparentBmp
            }?.let {
                npc.sprites[dir] = it
            }
        }
        
        npcList.removeAll { it.name == name }
        npcList.add(npc)
        
        invalidate()
    }

    /**
     * Atualiza a representação local do NPC a partir do estado compartilhado do mundo.
     * O servidor é a autoridade sobre a próxima célula (gridX/gridY).
     * A direção visual reflete o vetor do deslocamento real para evitar que a direção
     * do próximo movimento do servidor sobrescreva prematuramente o movimento atual.
     */
    fun updateNpcFromSharedState(sharedState: SharedNpcState) {
        val npc = npcList.find { it.name == sharedState.displayName || it.name == sharedState.npcId } ?: run {
            Log.w("StationGridView", "[SHARED_NPC_TRACE] updateNpcFromSharedState: NPC nao encontrado em npcList para displayName=${sharedState.displayName}, npcId=${sharedState.npcId}")
            return
        }
        
        Log.d("StationGridView", "[SHARED_NPC_TRACE] updateNpcFromSharedState recebido: npc=${npc.name}, posAtual=(${npc.x},${npc.y}), serverGrid=(${sharedState.gridX},${sharedState.gridY}), serverTarget=(${sharedState.targetX},${sharedState.targetY}), state=${sharedState.state}")

        npc.isInteracting = (sharedState.state == "interacting")
        if (npc.isInteracting) {
            npc.direction = sharedState.direction
            invalidate()
            return
        }

        val serverX = sharedState.gridX
        val serverY = sharedState.gridY
        val currentX = npc.x
        val currentY = npc.y

        val dx = Math.abs(serverX - currentX)
        val dy = Math.abs(serverY - currentY)

        if (currentX == serverX && currentY == serverY) {
            // NPC já está na posição autorizada do servidor.
            // Preserva a direção visual do deslocamento realizado e evita sobrescrever com a direção futura.
            npc.isMoving = false
            if (npc.direction.isEmpty()) {
                npc.direction = sharedState.direction
            }
            Log.d("StationGridView", "[SHARED_NPC_TRACE] NPC ${npc.name} ja esta em ($serverX,$serverY). DirecaoMantida=${npc.direction}")
        } else if (dx <= 1 && dy <= 1 && (dx + dy > 0)) {
            // Posição adjacente (1 célula): determina direção pelo deslocamento real e executa em ~200ms
            val moveDirection = when {
                serverX > currentX -> "direita"
                serverX < currentX -> "esquerda"
                serverY > currentY -> "baixo"
                else -> "cima"
            }
            npc.direction = moveDirection
            Log.d("StationGridView", "[SHARED_NPC_TRACE] NPC ${npc.name}: movendo 1 celula de ($currentX,$currentY) para ($serverX,$serverY) com direcao Real=$moveDirection")
            
            npc.movePath = mutableListOf(Point(serverX, serverY))
            if (!npc.isMoving) {
                npc.isMoving = true
                processNpcStep(npc)
            }
        } else {
            // Distância maior que 1 célula (sincronização inicial ou reconexão)
            Log.d("StationGridView", "[SHARED_NPC_TRACE] NPC ${npc.name}: gap de posicao detectado de ($currentX,$currentY) para ($serverX,$serverY). Sincronizando diretamente.")
            npc.x = serverX
            npc.y = serverY
            npc.direction = sharedState.direction
            npc.isMoving = false
        }
        invalidate()
    }

    /**
     * Atualiza a lista de jogadores remotos.
     */
    fun setRemotePlayers(newList: List<com.typingfrontier.social.PresenceManager.PresencePayload>, currentUserId: String) {
        val activeIds = newList.map { it.user_id }.toSet()
        
        // 1. Remove jogadores que saíram ou mudaram de estação
        val iterator = remotePlayers.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (!activeIds.contains(entry.key)) {
                iterator.remove()
            }
        }

        // 2. Adiciona ou atualiza jogadores
        newList.forEach { payload ->
            if (payload.user_id == currentUserId) return@forEach
            
            val effectiveRole = payload.profession.ifEmpty { payload.role }
            val player = remotePlayers.getOrPut(payload.user_id) {
                RemotePlayer(
                    id = payload.user_id,
                    username = payload.username,
                    role = effectiveRole,
                    gender = payload.gender,
                    x = payload.gridX,
                    y = payload.gridY,
                    direction = payload.direction
                )
            }
            
            // Atualiza posição e direção vinda da rede
            player.x = payload.gridX
            player.y = payload.gridY
            player.direction = payload.direction
            
            // Carrega sprites se necessário usando cache por combinação de profissão/gênero
            val cacheKey = "${effectiveRole}_${payload.gender}".lowercase()
            if (player.sprites.isEmpty()) {
                val cached = remoteSpriteCache[cacheKey]
                if (cached != null) {
                    player.sprites.putAll(cached)
                } else {
                    loadRemoteSprites(player, effectiveRole, payload.gender)
                    remoteSpriteCache[cacheKey] = player.sprites
                }
            }
        }
        invalidate()
    }

    private fun loadRemoteSprites(player: RemotePlayer, role: String, gender: String) {
        val targetProcessingHeight = 256
        val sexoChar = if (gender.equals("Feminino", ignoreCase = true)) "f" else "m"
        
        val baseNome = when (role) {
            "Médico" -> if (sexoChar == "f") "medica" else "medico"
            "Engenheiro" -> if (sexoChar == "f") "engenheira" else "engenheiro"
            "Professor" -> if (sexoChar == "f") "professora" else "professor"
            "Detetive" -> "detetive"
            "Policial" -> "policial"
            else -> "detetive"
        }

        val directions = listOf("frente", "costas", "esquerda", "direita")
        val dirMap = mapOf("frente" to "baixo", "costas" to "cima", "esquerda" to "esquerda", "direita" to "direita")

        for (dir in directions) {
            val resName = "${baseNome}_${sexoChar}_$dir"
            val resId = context.resources.getIdentifier(resName, "drawable", context.packageName)
            if (resId != 0) {
                val targetDir = dirMap[dir]!!
                val cachedBmp = globalSpriteCache[resId]
                if (cachedBmp != null) {
                    player.sprites[targetDir] = cachedBmp
                } else {
                    loadSprite(resId, targetProcessingHeight) { transparentBmp ->
                        player.sprites[targetDir] = transparentBmp
                        val cacheKey = "${role}_${gender}".lowercase()
                        remoteSpriteCache[cacheKey] = player.sprites
                        invalidate()
                    }
                }
            }
        }
    }



    fun startNpcMovingTo(npcName: String, tx: Int, ty: Int) {
        val npc = npcList.find { it.name == npcName } ?: return
        internalStartNpcMovingTo(npc, tx, ty)
    }

    private fun internalStartNpcMovingTo(npc: StationNPC, tx: Int, ty: Int) {
        Log.d("StationGridView", "[SHARED_NPC_TRACE] internalStartNpcMovingTo chamado: npc=${npc.name}, de=(${npc.x},${npc.y}) para=($tx,$ty)")
        val path = findPath(Point(npc.x, npc.y), Point(tx, ty))
        if (path != null && path.size > 1) {
            npc.movePath = path.toMutableList()
            npc.movePath.removeAt(0)
            Log.d("StationGridView", "[SHARED_NPC_TRACE] Caminho encontrado! movePath tamanho=${npc.movePath.size}")
            if (!npc.isMoving) {
                npc.isMoving = true
                processNpcStep(npc)
            } else {
                Log.d("StationGridView", "[SHARED_NPC_TRACE] NPC ja estava em movimento, novo movePath definido")
            }
        } else {
            Log.w("StationGridView", "[SHARED_NPC_TRACE] Nenhum caminho valido encontrado ate ($tx,$ty). pathSize=${path?.size ?: 0}")
            if (!npc.isMoving) {
                npc.x = tx
                npc.y = ty
                invalidate()
            }
        }
    }

    private fun processNpcStep(npc: StationNPC) {
        if (npc.movePath.isEmpty()) {
            Log.d("StationGridView", "[SHARED_NPC_TRACE] processNpcStep: movePath vazio. Finalizando movimento para npc=${npc.name} em (${npc.x},${npc.y})")
            npc.isMoving = false
            
            if (npc.route.isNotEmpty()) {
                // Lógica de Rota: Se houver rota definida, avança para o próximo waypoint
                npc.currentWaypointIndex = (npc.currentWaypointIndex + 1) % npc.route.size
                val next = npc.route[npc.currentWaypointIndex]
                
                // Pequena pausa de 1 segundo ao chegar em cada ponto da rota
                postDelayed({
                    internalStartNpcMovingTo(npc, next.x, next.y)
                }, 1000)
            }
            return
        }

        val next = npc.movePath.removeAt(0)

        // Determina direção baseada nos nomes de recursos do NPC
        npc.direction = when {
            next.x > npc.x -> "direita"
            next.x < npc.x -> "esquerda"
            next.y > npc.y -> "baixo"
            else -> "cima"
        }

        npc.x = next.x
        npc.y = next.y
        Log.d("StationGridView", "[SHARED_NPC_TRACE] processNpcStep executado: npc=${npc.name}, novaPos=(${npc.x},${npc.y}), direcao=${npc.direction}, passosRestantes=${npc.movePath.size}")
        invalidate()

        postDelayed({ processNpcStep(npc) }, moveInterval)
    }

    private fun loadSprite(resId: Int, targetHeight: Int, onProcessed: ((Bitmap) -> Unit)? = null): Bitmap? {
        globalSpriteCache[resId]?.let { return it }

        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeResource(context.resources, resId, options)
            
            options.inSampleSize = calculateInSampleSize(options, targetHeight, targetHeight)
            options.inJustDecodeBounds = false
            
            val scaledDown = BitmapFactory.decodeResource(context.resources, resId, options) ?: return null

            if (globalInFlight.add(resId)) {
                globalSpriteExecutor.execute {
                    try {
                        val transparent = ViewUtils.makeTransparent(scaledDown)
                        if (transparent != null) {
                            globalSpriteCache[resId] = transparent
                            post {
                                onProcessed?.invoke(transparent)
                                invalidate()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        globalInFlight.remove(resId)
                    }
                }
            }

            scaledDown
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    /**
     * Remove o fundo branco usando um algoritmo de Flood Fill a partir das bordas.
     * Além de transparência, remove pequenos resíduos de marca d'água desconectados.
     */
    private fun makeTransparent(src: Bitmap): Bitmap {
        return ViewUtils.makeTransparent(src)
    }

    private fun isWalkable(x: Int, y: Int): Boolean {
        // --- GEOMETRIA SIMPLIFICADA: ESTAÇÃO SÃO PAULO ---
        // Escadas completamente bloqueadas. O personagem caminha apenas no piso da plataforma.
        
        if (x < 0 || x >= cols || y < 0 || y >= rows) return false
        
        // 1. LIMITES LATERAIS ABSOLUTOS
        if (x == 0) return false      // Parede Extrema Esquerda
        if (x >= 9) return false      // Faixa Amarela / Trem / Trilhos
        
        // 2. LIMITES DE BORDAS (BLOQUEIA AS EXTREMIDADES Y=0 E Y=23, ONDE FICAM AS ESCADAS)
        if (y == 0 || y == 23) return false
        
        // 3. OBJETOS E MUROS INTERNOS (BLOCKED)
        
        // Muro e Escada Superior (x=1..8, y=1..2)
        // Bloqueia a coluna x=1 (antigo degrau) e os muros adjacentes.
        if ((y == 1 || y == 2) && x in 1..8) return false
        
        // BLOQUEIO DA ESCADA INFERIOR E MURO VERTICAL
        // 1. Muro Vertical (x=2) e Degraus da Escada (x=1)
        // Bloqueio determinístico. Liberamos y=18 e y=19 (2 quadrados bege em frente).
        if (x in 1..2 && y in 20..22) return false
        
        // 2. Parede Inferior da Plataforma (Restante da borda inferior)
        // O personagem agora consegue chegar até o nível y=21 (piso bege em frente à barreira).
        if (y == 22 && x in 3..8) return false
        
        // Armários Laterais Esquerdos (x=1)
        if (x == 1 && y in 5..17) return false
        
        // Balcão de Check-in e Totens (x=8)
        if (x == 8) {
            if (y in 11..13) return false
            if (y == 6 || y == 19) return false
        }
        
        // 4. TODO O RESTANTE É PISO VÁLIDO DE CIRCULAÇÃO (WALKABLE)
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        return true
    }

    private fun handleTap(screenX: Float, screenY: Float) {
        val mapCoords = floatArrayOf(screenX, screenY)
        inverseMatrix.mapPoints(mapCoords)
        
        val mapX = mapCoords[0]
        val mapY = mapCoords[1]
        
        val iv = targetImageView ?: return
        val drawable = iv.drawable ?: return
        val imageRect = getImageRect(iv, drawable)
        
        if (imageRect.contains(mapX, mapY)) {
            val cellWidth = imageRect.width() / cols
            val cellHeight = imageRect.height() / rows
            
            val tx = ((mapX - imageRect.left) / cellWidth).toInt()
            val ty = ((mapY - imageRect.top) / cellHeight).toInt()
            
            Log.d("StationGridView", "[STAIR_TRACE] screenX=$screenX, screenY=$screenY, tx=$tx, ty=$ty")

            // Verificar escadas fechadas para manutenção
            val isNorthStairs = (ty in 1..2 && tx in 1..2)
            val isRioSouthStairs = (currentStationId == "rio_de_janeiro" && ty >= 19 && tx in 1..2)

            if (isNorthStairs || isRioSouthStairs) {
                if (isNorthStairs && currentStationId == "sao_paulo") {
                    interactionListener?.onNorthStairsTapped()
                    return
                }
                Toast.makeText(context, "Escada em manutenção. Risco de acidente. Acesso proibido temporariamente.", Toast.LENGTH_SHORT).show()
                return
            }
            
            Log.d("StationGridView", "[STAIR_TRACE] getInteractionTarget ENTER tx=$tx, ty=$ty")
            val interactionTarget = getInteractionTarget(tx, ty)
            Log.d("StationGridView", "[STAIR_TRACE] interactionTarget = $interactionTarget")
            if (interactionTarget != null) {
                if (playerX == interactionTarget.x && playerY == interactionTarget.y) {
                    interactionListener?.onArmoireTapped()
                } else {
                    pendingArmoireAction = true
                    startMovingTo(interactionTarget.x, interactionTarget.y)
                }
                return
            }

            // Verificar se tocou em um NPC
            val tappedNpc = npcList.find { it.x == tx && it.y == ty }
            if (tappedNpc != null) {
                // Se estiver adjacente ao NPC, interage
                val dx = Math.abs(playerX - tappedNpc.x)
                val dy = Math.abs(playerY - tappedNpc.y)
                if (dx <= 1 && dy <= 1 && (dx + dy > 0)) {
                    interactionListener?.onNpcTapped(tappedNpc.name)
                } else {
                    // Senão, caminha até uma posição adjacente ao NPC
                    val targetX = if (playerX < tappedNpc.x) tappedNpc.x - 1 else if (playerX > tappedNpc.x) tappedNpc.x + 1 else tappedNpc.x
                    val targetY = if (playerY < tappedNpc.y) tappedNpc.y - 1 else if (playerY > tappedNpc.y) tappedNpc.y + 1 else tappedNpc.y
                    
                    var finalX = targetX
                    var finalY = targetY
                    if (finalX == tappedNpc.x && finalY == tappedNpc.y) {
                        val neighbors = listOf(Point(tappedNpc.x-1, tappedNpc.y), Point(tappedNpc.x+1, tappedNpc.y), Point(tappedNpc.x, tappedNpc.y-1), Point(tappedNpc.x, tappedNpc.y+1))
                        val walkableNeighbor = neighbors.find { isWalkable(it.x, it.y) }
                        if (walkableNeighbor != null) {
                            finalX = walkableNeighbor.x
                            finalY = walkableNeighbor.y
                        }
                    }

                    if (isWalkable(finalX, finalY)) {
                        pendingNpcName = tappedNpc.name
                        startMovingTo(finalX, finalY)
                    }
                }
                return
            }

            // Verificar se tocou em um jogador remoto
            val tappedRemote = remotePlayers.values.find { it.x == tx && it.y == ty }
            if (tappedRemote != null) {
                interactionListener?.onRemotePlayerTapped(tappedRemote.id, tappedRemote.username)
                return
            }

            // Verificar se tocou no Trem (Faixa Amarela / Trem / Trilhos -> x >= 9)
            if (tx >= 9) {
                interactionListener?.onTrainTapped()
                return
            }

            val targetX = tx
            val targetY = ty

            Log.d("StationGridView", "[STAIR_TRACE] targetX=$targetX, targetY=$targetY")

            Log.d("StationGridView", "[STAIR_TRACE] checking walkable target=($targetX,$targetY)")
            val walkable = isWalkable(targetX, targetY)
            Log.d("StationGridView", "[STAIR_TRACE] walkable=$walkable")

            if (walkable) {
                pendingArmoireAction = false
                pendingNpcName = null
                Log.d("StationGridView", "[STAIR_TRACE] START_MOVING_TO target=($targetX,$targetY)")
                startMovingTo(targetX, targetY)
            } else {
                Log.d("StationGridView", "[STAIR_TRACE] startMovingTo NOT CALLED (not walkable)")
            }
        }
    }

    private fun getInteractionTarget(tx: Int, ty: Int): Point? {
        // Armários Laterais (x=1, y=5..17) -> Interação em x=2
        if (tx == 1 && ty in 5..17) return Point(2, ty)
        if (tx == 2 && ty in 5..17) return Point(2, ty)
        
        // Armários Superiores (x=3..8, y=1..2) -> Interação em y=3
        if (tx in 3..8 && (ty == 1 || ty == 2)) return Point(tx, 3)
        if (tx in 3..8 && ty == 3) return Point(tx, 3)
        
        return null
    }

    private fun startMovingTo(tx: Int, ty: Int) {
        Log.d("StationGridView", "[STAIR_CELL_TRACE] PATH_START current=($playerX,$playerY) target=($tx,$ty)")
        Log.d("StationGridView", "[STAIR_TRACE] startMovingTo ENTER target=($tx,$ty)")
        val path = findPath(Point(playerX, playerY), Point(tx, ty))
        Log.d("StationGridView", "[STAIR_TRACE] PATH size=${path?.size ?: 0}, PATH=$path")
        if (path != null && path.size > 1) {
            movePath = path.toMutableList()
            movePath.removeAt(0) // Remove posição atual
            if (!isMoving) {
                isMoving = true
                processNextStep()
            }
        } else {
            Log.d("StationGridView", "[STAIR_TRACE] startMovingTo NOT CALLED (path null or empty)")
        }
    }

    private fun processNextStep() {
        Log.d("StationGridView", "[STAIR_TRACE] PROCESS_NEXT_STEP current=($playerX,$playerY)")
        if (movePath.isEmpty()) {
            isMoving = false
            Log.d("StationGridView", "[STAIR_CELL_TRACE] PATH_END final=($playerX,$playerY)")
            if (pendingArmoireAction) {
                pendingArmoireAction = false
                interactionListener?.onArmoireTapped()
            }
            if (pendingNpcName != null) {
                val name = pendingNpcName!!
                pendingNpcName = null
                interactionListener?.onNpcTapped(name)
            }
            return
        }
        
        val next = movePath.removeAt(0)
        
        // Determina direção
        currentDirection = when {
            next.x > playerX -> "direita"
            next.x < playerX -> "esquerda"
            next.y > playerY -> "frente" // DESCENDO
            else -> "costas"             // SUBINDO
        }
        
        playerX = next.x
        playerY = next.y
        if (playerX in 0..5 && playerY in 18..23) {
            Log.d("StationGridView", "[STAIR_CELL_TRACE] PLAYER_CELL x=$playerX y=$playerY direction=$currentDirection")
        }
        Log.d("StationGridView", "[STAIR_TRACE] PLAYER_POSITION_CHANGED x=$playerX y=$playerY")
        invalidate()
        
        // Notifica a Activity sobre a mudança de posição/direção para sincronização multiplayer
        interactionListener?.onPlayerPositionChanged(playerX, playerY, currentDirection)
        
        postDelayed({ processNextStep() }, moveInterval)
    }

    private fun findPath(start: Point, end: Point): List<Point>? {
        val queue = mutableListOf(listOf(start))
        val visited = mutableSetOf(start)
        while (queue.isNotEmpty()) {
            val path = queue.removeAt(0)
            val curr = path.last()
            if (curr == end) return path
            val neighbors = listOf(Point(curr.x+1, curr.y), Point(curr.x-1, curr.y), Point(curr.x, curr.y+1), Point(curr.x, curr.y-1))
            for (n in neighbors) {
                if (n.x in 0 until cols && n.y in 0 until rows && isWalkable(n.x, n.y) && n !in visited) {
                    visited.add(n)
                    queue.add(path + n)
                }
            }
        }
        return null
    }

    private fun getImageRect(iv: ImageView, drawable: android.graphics.drawable.Drawable): RectF {
        val rect = RectF()
        val matrix = iv.imageMatrix
        val tempRect = RectF(0f, 0f, drawable.intrinsicWidth.toFloat(), drawable.intrinsicHeight.toFloat())
        matrix.mapRect(rect, tempRect)
        return rect
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val iv = targetImageView ?: return
        val drawable = iv.drawable ?: return
        val imageRect = getImageRect(iv, drawable)

        if (imageRect.width() <= 0) {
            postInvalidateDelayed(100)
            return
        }

        // Configura Matrizes de Câmera
        matrixCamera.reset()
        matrixCamera.postTranslate(offsetX, offsetY)
        matrixCamera.postScale(scaleFactor, scaleFactor, width / 2f, height / 2f)
        matrixCamera.invert(inverseMatrix)

        canvas.save()
        canvas.concat(matrixCamera)

        // 1. Desenhar Fundo
        drawable.setBounds(imageRect.left.toInt(), imageRect.top.toInt(), imageRect.right.toInt(), imageRect.bottom.toInt())
        drawable.draw(canvas)

        val cellWidth = imageRect.width() / cols
        val cellHeight = imageRect.height() / rows

        // 3. Desenhar Jogador
        drawCharacter(canvas, playerName, playerX, playerY, currentDirection, playerSprites, imageRect, cellWidth, cellHeight)

        // 4. Desenhar NPCs
        npcList.forEach { npc ->
            drawCharacter(canvas, npc.name, npc.x, npc.y, npc.direction, npc.sprites, imageRect, cellWidth, cellHeight)
        }
        
        // 5. Desenhar Jogadores Remotos
        remotePlayers.values.forEach { rp ->
            drawCharacter(canvas, rp.username, rp.x, rp.y, rp.direction, rp.sprites, imageRect, cellWidth, cellHeight)
        }

        // 6. Desenhar Diálogos Temporários sobre o Cenário
        val currentTime = System.currentTimeMillis()
        var hasActiveDialogs = false

        val iterator = activeDialogs.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val dialog = entry.value
            if (currentTime >= dialog.expirationTime) {
                iterator.remove()
            } else {
                hasActiveDialogs = true
                val currentUserId = SocialProfileRepository.getCurrentUserId() ?: ""
                val isUserAllowed = !dialog.isPrivate ||
                        dialog.allowedUsers.contains(currentUserId) ||
                        dialog.allowedUsers.contains("local_user") ||
                        dialog.allowedUsers.contains("antonio") ||
                        dialog.senderId == "player_local"

                if (isUserAllowed) {
                    drawWorldDialogText(canvas, dialog, imageRect, cellWidth, cellHeight)
                }
            }
        }

        canvas.restore()

        if (hasActiveDialogs) {
            postInvalidateDelayed(100)
        }
    }

    private fun drawWorldDialogText(
        canvas: Canvas,
        dialog: WorldDialog,
        imageRect: RectF,
        cellWidth: Float,
        cellHeight: Float
    ) {
        val targetHeight = cellHeight * 2.1f
        val px = imageRect.left + dialog.worldX * cellWidth + (cellWidth / 2f)
        val py = imageRect.top + dialog.worldY * cellHeight + cellHeight
        val headY = py - targetHeight - 8f

        // Formata a mensagem na composição "Nome: mensagem"
        val fullText = if (dialog.senderName.isNotEmpty()) {
            if (dialog.message.startsWith("${dialog.senderName}:")) {
                dialog.message
            } else {
                "${dialog.senderName}: ${dialog.message}"
            }
        } else {
            dialog.message
        }

        val strokePaint = if (dialog.isPrivate) paintPrivateTextStroke else paintPlayerNameStroke
        val fillPaint = if (dialog.isPrivate) paintPrivateText else paintPlayerName

        val maxLineWidth = (cellWidth * 3.5f).coerceAtLeast(260f)
        val lines = wrapText(fullText, fillPaint, maxLineWidth)

        val lineHeight = 40f
        val numLines = lines.size

        for (i in 0 until numLines) {
            val line = lines[i]
            // Linha 0 (topo) em headY - (numLines-1)*40, última linha em headY
            val lineY = headY - ((numLines - 1 - i) * lineHeight)

            canvas.drawText(line, px, lineY, strokePaint)
            canvas.drawText(line, px, lineY, fillPaint)
        }
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (paint.measureText(text) <= maxWidth) {
            return listOf(text)
        }
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()

        for (word in words) {
            if (currentLine.isEmpty()) {
                currentLine.append(word)
            } else {
                val testLine = "$currentLine $word"
                if (paint.measureText(testLine) <= maxWidth) {
                    currentLine.append(" ").append(word)
                } else {
                    lines.add(currentLine.toString())
                    currentLine = StringBuilder(word)
                }
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }
        return lines
    }

    private fun drawCharacter(
        canvas: Canvas,
        name: String,
        gridX: Int,
        gridY: Int,
        direction: String,
        sprites: Map<String, Bitmap>,
        imageRect: RectF,
        cellWidth: Float,
        cellHeight: Float
    ) {
        val sprite = sprites[direction] ?: sprites["frente"] ?: sprites["baixo"]
        if (sprite != null) {
            val targetHeight = cellHeight * 2.1f
            val ratio = sprite.width.toFloat() / sprite.height.toFloat()
            val targetWidth = targetHeight * ratio
            
            val px = imageRect.left + gridX * cellWidth + (cellWidth / 2)
            val py = imageRect.top + gridY * cellHeight + cellHeight
            
            val dest = RectF(px - (targetWidth / 2), py - targetHeight, px + (targetWidth / 2), py)
            canvas.drawBitmap(sprite, null, dest, null)

            // Se houver um diálogo ativo exatamente nesta posição e para este personagem, oculta o nome simples
            val currentTime = System.currentTimeMillis()
            val hasActiveDialogAtPos = activeDialogs.values.any { d ->
                currentTime < d.expirationTime && d.worldX == gridX && d.worldY == gridY &&
                        (d.senderName == name || (d.senderId == "player_local" && name == playerName))
            }

            if (name.isNotEmpty() && !hasActiveDialogAtPos) {
                val textY = py - targetHeight - 8f
                canvas.drawText(name, px, textY, paintPlayerNameStroke)
                canvas.drawText(name, px, textY, paintPlayerName)
            }
        }
    }
}
