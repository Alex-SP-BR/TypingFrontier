package com.typingfrontier.station

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.ImageView
import com.typingfrontier.utils.ViewUtils

/**
 * Componente visual experimental para estação de trem.
 * Suporta câmera arrastável, zoom e movimentação do personagem.
 */
class StationGridView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    /**
     * Modelo simples para NPCs na estação.
     */
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

    interface InteractionListener {
        fun onArmoireTapped()
        fun onNpcTapped(npcName: String)
    }

    private var interactionListener: InteractionListener? = null
    private var targetImageView: ImageView? = null
    
    // Pincéis
    private val paintGrid = Paint().apply {
        color = Color.parseColor("#40FFFFFF")
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }
    
    private val paintText = Paint().apply {
        color = Color.CYAN
        textSize = 20f
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }

    private val paintPlayerName = Paint().apply {
        color = Color.WHITE
        textSize = 18f
        textAlign = Paint.Align.CENTER
        setShadowLayer(3f, 0f, 0f, Color.BLACK)
    }

    // Grid e Mapa
    private val cols = 12
    private val rows = 24

    // Câmera
    private var scaleFactor = 1.0f
    private var offsetX = 0f
    private var offsetY = 0f
    private val matrixCamera = Matrix()
    private val inverseMatrix = Matrix()

    // Jogador
    private val playerSprites = mutableMapOf<String, Bitmap>()
    private var playerName: String = ""
    private var playerX = 5
    private var playerY = 20
    private var currentDirection = "frente"
    
    // NPCs
    private val npcList = mutableListOf<StationNPC>()

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

    fun setNpcInteracting(npcName: String, interacting: Boolean) {
        npcList.find { it.name == npcName }?.let {
            it.isInteracting = interacting
            if (!interacting && it.isCirculating) {
                decidirProximoPassoCirculacao(it)
            }
        }
    }

    fun setPlayerData(sprites: Map<String, Int>, name: String) {
        this.playerName = name
        playerSprites.clear()
        
        // Redimensionamento preventivo para evitar ANR
        val targetProcessingHeight = 256
        
        sprites.forEach { (dir, resId) ->
            loadSprite(resId, targetProcessingHeight)?.let {
                playerSprites[dir] = it
            }
        }
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
            loadSprite(resId, targetProcessingHeight)?.let {
                npc.sprites[dir] = it
            }
        }
        
        npcList.removeAll { it.name == name }
        npcList.add(npc)
        
        // Se for circulante, inicia o ciclo de decisão após o primeiro delay
        if (isCirculating) {
            postDelayed({ decidirProximoPassoCirculacao(npc) }, 60000L)
        }
        
        invalidate()
    }

    private fun decidirProximoPassoCirculacao(npc: StationNPC) {
        if (!npc.isCirculating || npc.anchorX == -1 || npc.isInteracting) return
        if (npc.isMoving) return

        val neighbors = listOf(
            Point(npc.x - 1, npc.y), // esquerda
            Point(npc.x + 1, npc.y), // direita
            Point(npc.x, npc.y - 1), // cima
            Point(npc.x, npc.y + 1)  // baixo
        )

        val validCells = neighbors.filter { n ->
            // Deve estar dentro da área 3x3 em torno da âncora
            val withinArea = n.x in (npc.anchorX - 1)..(npc.anchorX + 1) &&
                             n.y in (npc.anchorY - 1)..(npc.anchorY + 1)
            
            withinArea && isWalkable(n.x, n.y)
        }

        if (validCells.isNotEmpty()) {
            val target = validCells.random()
            internalStartNpcMovingTo(npc, target.x, target.y)
        } else {
            // Tenta novamente em 1 minuto se não houver vizinhos válidos na área
            postDelayed({ decidirProximoPassoCirculacao(npc) }, 60000L)
        }
    }

    fun startNpcMovingTo(npcName: String, tx: Int, ty: Int) {
        val npc = npcList.find { it.name == npcName } ?: return
        internalStartNpcMovingTo(npc, tx, ty)
    }

    private fun internalStartNpcMovingTo(npc: StationNPC, tx: Int, ty: Int) {
        val path = findPath(Point(npc.x, npc.y), Point(tx, ty))
        if (path != null && path.size > 1) {
            npc.movePath = path.toMutableList()
            npc.movePath.removeAt(0)
            if (!npc.isMoving) {
                npc.isMoving = true
                processNpcStep(npc)
            }
        }
    }

    private fun processNpcStep(npc: StationNPC) {
        if (npc.movePath.isEmpty()) {
            npc.isMoving = false
            
            if (npc.isCirculating) {
                // Ao chegar no destino da circulação, aguarda 1 minuto para a próxima decisão
                postDelayed({
                    decidirProximoPassoCirculacao(npc)
                }, 60000L)
            } else if (npc.route.isNotEmpty()) {
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
        invalidate()

        postDelayed({ processNpcStep(npc) }, moveInterval)
    }

    private fun loadSprite(resId: Int, targetHeight: Int): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeResource(context.resources, resId, options)
            
            options.inSampleSize = calculateInSampleSize(options, targetHeight, targetHeight)
            options.inJustDecodeBounds = false
            
            val scaledDown = BitmapFactory.decodeResource(context.resources, resId, options)
            ViewUtils.makeTransparent(scaledDown)
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
            
            val interactionTarget = getInteractionTarget(tx, ty)
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

            if (isWalkable(tx, ty)) {
                pendingArmoireAction = false
                pendingNpcName = null
                startMovingTo(tx, ty)
            }
        }
    }

    private fun getInteractionTarget(tx: Int, ty: Int): Point? {
        // Armários Laterais (x=1, y=5..17) -> Interação em x=2
        if (tx == 1 && ty in 5..17) return Point(2, ty)
        if (tx == 2 && ty in 5..17) return Point(2, ty)
        
        // Armários Superiores (x=1..8, y=1..2) -> Interação em y=3
        if (tx in 1..8 && (ty == 1 || ty == 2)) return Point(tx, 3)
        if (tx in 1..8 && ty == 3) return Point(tx, 3)
        
        return null
    }

    private fun startMovingTo(tx: Int, ty: Int) {
        val path = findPath(Point(playerX, playerY), Point(tx, ty))
        if (path != null && path.size > 1) {
            movePath = path.toMutableList()
            movePath.removeAt(0) // Remove posição atual
            if (!isMoving) {
                isMoving = true
                processNextStep()
            }
        }
    }

    private fun processNextStep() {
        if (movePath.isEmpty()) {
            isMoving = false
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
        invalidate()
        
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

        canvas.restore()
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

            if (name.isNotEmpty()) {
                canvas.drawText(name, px, py - targetHeight - 8f, paintPlayerName)
            }
        }
    }
}
