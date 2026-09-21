package com.typingfrontier.station

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.ImageView

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
        var currentWaypointIndex: Int = -1
    )

    interface InteractionListener {
        fun onArmoireTapped()
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
    fun setNpcData(name: String, x: Int, y: Int, direction: String, sprites: Map<String, Int>, route: List<Point> = emptyList()) {
        val npc = StationNPC(name, x, y, direction, route = route)
        val targetProcessingHeight = 256
        
        sprites.forEach { (dir, resId) ->
            loadSprite(resId, targetProcessingHeight)?.let {
                npc.sprites[dir] = it
            }
        }
        
        npcList.removeAll { it.name == name }
        npcList.add(npc)
        invalidate()
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
            
            // Lógica de Rota: Se houver rota definida, avança para o próximo waypoint
            if (npc.route.isNotEmpty()) {
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
            makeTransparent(scaledDown)
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
        val width = src.width
        val height = src.height
        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        src.getPixels(pixels, 0, width, 0, 0, width, height)
        
        val backgroundMask = java.util.BitSet(width * height)
        val queue = java.util.ArrayDeque<Int>()
        
        // 1. Flood Fill das bordas (Fundo e Marcas d'água conectadas)
        for (x in 0 until width) {
            if (!backgroundMask.get(x)) { queue.add(x); backgroundMask.set(x) }
            val b = (height - 1) * width + x
            if (!backgroundMask.get(b)) { queue.add(b); backgroundMask.set(b) }
        }
        for (y in 0 until height) {
            val l = y * width
            if (!backgroundMask.get(l)) { queue.add(l); backgroundMask.set(l) }
            val r = y * width + (width - 1)
            if (!backgroundMask.get(r)) { queue.add(r); backgroundMask.set(r) }
        }
        
        while (queue.isNotEmpty()) {
            val node = queue.poll() ?: continue
            val color = pixels[node]
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF
            
            // Threshold agressivo para fundo branco/claro (RGB > 185) em imagens JPEG compactadas
            if (r > 185 && g > 185 && b > 185) {
                pixels[node] = Color.TRANSPARENT
                val x = node % width
                val y = node / width
                val dx = intArrayOf(-1, 1, 0, 0)
                val dy = intArrayOf(0, 0, -1, 1)
                for (i in 0 until 4) {
                    val nx = x + dx[i]; val ny = y + dy[i]
                    if (nx in 0 until width && ny in 0 until height) {
                        val next = ny * width + nx
                        if (!backgroundMask.get(next)) { backgroundMask.set(next); queue.add(next) }
                    }
                }
            }
        }

        // 2. Limpeza de Ilhas (Resíduos de marca d'água que não tocam a borda mas são claros)
        // Varre áreas externas ao personagem para remover "pós" residual.
        for (i in pixels.indices) {
            val color = pixels[i]
            if (color != Color.TRANSPARENT) {
                val r = (color shr 16) and 0xFF
                val g = (color shr 8) and 0xFF
                val b = color and 0xFF
                // Se for muito claro e estiver em uma região periférica (longe do centro), remove.
                if (r > 240 && g > 240 && b > 240) {
                    pixels[i] = Color.TRANSPARENT
                }
            }
        }
        
        out.setPixels(pixels, 0, width, 0, 0, width, height)
        return out
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

            if (isWalkable(tx, ty)) {
                pendingArmoireAction = false
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

        // 2. Desenhar Grade (REMOVIDO DA RENDERIZAÇÃO FINAL - Mantido apenas para lógica interna)
        /*
        for (y in 0 until rows) {
            for (x in 0 until cols) {
                if (isWalkable(x, y)) {
                    val l = imageRect.left + x * cellWidth
                    val t = imageRect.top + y * cellHeight
                    val r = l + cellWidth
                    val b = t + cellHeight
                    
                    if (!isWalkable(x - 1, y)) canvas.drawLine(l, t, l, b, paintGrid)
                    if (!isWalkable(x + 1, y)) canvas.drawLine(r, t, r, b, paintGrid)
                    if (!isWalkable(x, y - 1)) canvas.drawLine(l, t, r, t, paintGrid)
                    if (!isWalkable(x, y + 1)) canvas.drawLine(l, b, r, b, paintGrid)
                    
                    val paintDivider = Paint(paintGrid).apply { alpha = 20 }
                    if (isWalkable(x + 1, y)) canvas.drawLine(r, t + 5f, r, b - 5f, paintDivider)
                    if (isWalkable(x, y + 1)) canvas.drawLine(l + 5f, b, r - 5f, b, paintDivider)
                }
            }
        }
        */

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
