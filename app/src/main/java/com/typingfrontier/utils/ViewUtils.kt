package com.typingfrontier.utils

import androidx.appcompat.app.AlertDialog
import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import com.typingfrontier.R

object ViewUtils {

    private var cachedCoinBitmap: Bitmap? = null

    /**
     * Retorna o Drawable da moeda processado com transparência (Flood Fill).
     * Utiliza cache para evitar re-processamento.
     */
    fun getCoinDrawable(context: Context): Drawable {
        if (cachedCoinBitmap == null) {
            val original = BitmapFactory.decodeResource(context.resources, R.drawable.fron_coin)
            if (original != null) {
                cachedCoinBitmap = makeTransparent(original)
            }
        }
        
        return if (cachedCoinBitmap != null) {
            BitmapDrawable(context.resources, cachedCoinBitmap)
        } else {
            AppCompatResources.getDrawable(context, R.drawable.fron_coin)!!
        }
    }

    /**
     * Remove o fundo branco usando um algoritmo de Flood Fill a partir das bordas.
     * Além de transparência, remove pequenos resíduos de marca d'água desconectados.
     */
    fun makeTransparent(src: Bitmap): Bitmap {
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

    fun showZoomDialog(
        context: Context,
        imageRes: Int,
        title: String,
        description: String? = null,
        colorFilter: ColorFilter? = null,
        alpha: Float = 1.0f,
        applySilhouette: Boolean = false,
        silhouetteThicknessDp: Float = 2f
    ) {
        val builder = AlertDialog.Builder(context, R.style.Theme_TypingFrontier_MentalDialog)
        val inflater = LayoutInflater.from(context)
        val dialogView = inflater.inflate(R.layout.dialog_zoom, null)
        
        val imgZoom = dialogView.findViewById<ImageView>(R.id.imgZoom)
        val txtTitle = dialogView.findViewById<TextView>(R.id.txtZoomTitle)
        val txtDesc = dialogView.findViewById<TextView>(R.id.txtZoomDesc)
        
        if (applySilhouette) {
            val isDesbloqueada = colorFilter == null && alpha == 1.0f
            imgZoom.setImageDrawable(getInsigniaWithSilhouette(context, imageRes, isDesbloqueada, silhouetteThicknessDp))
            // O ColorFilter e Alpha da AchievementAdapter serão aplicados ao SilhouetteDrawable pelo ImageView
            imgZoom.colorFilter = colorFilter
            imgZoom.alpha = alpha
        } else {
            imgZoom.setImageResource(imageRes)
            imgZoom.colorFilter = colorFilter
            imgZoom.alpha = alpha
        }
        
        txtTitle.text = title
        if (description != null) {
            txtDesc.text = description
            txtDesc.visibility = View.VISIBLE
        } else {
            txtDesc.visibility = View.GONE
        }
        
        builder.setView(dialogView)
        builder.setPositiveButton("Fechar", null)
        
        val dialog = builder.create()
        dialog.show()

        // Ajuste de opacidade para tornar o diálogo sólido
        val color = Color.parseColor("#FB121212")
        dialog.window?.findViewById<android.view.View>(androidx.appcompat.R.id.parentPanel)?.let { panel ->
            panel.backgroundTintList = android.content.res.ColorStateList.valueOf(color)
        }
        dialog.window?.findViewById<android.view.View>(androidx.appcompat.R.id.buttonPanel)?.setBackgroundColor(color)
    }

    /**
     * Cria um Drawable especial com contorno de silhueta real.
     * Utiliza um mecanismo de renderização por loop (Sticker Effect) para garantir
     * que o contorno acompanhe o Alpha da imagem sem gaps ou clipping.
     * 
     * @param thicknessDp Espessura do contorno em DP (Default: 2dp para Conquistas/Avatares)
     */
    fun getInsigniaWithSilhouette(context: Context, resId: Int, desbloqueada: Boolean, thicknessDp: Float = 2f): Drawable {
        val original = AppCompatResources.getDrawable(context, resId)?.mutate() ?: 
                       return AppCompatResources.getDrawable(context, android.R.drawable.ic_menu_report_image)!!
        
        val density = context.resources.displayMetrics.density
        val thicknessPx = thicknessDp * density

        // Configuração de cores RPG
        val colorOutline = if (desbloqueada) Color.WHITE else Color.parseColor("#BBFFFFFF")
        val colorGlow = if (desbloqueada) Color.parseColor("#4474C6E0") else Color.TRANSPARENT
        val colorShadow = Color.parseColor("#66000000")

        return SilhouetteDrawable(original, colorOutline, colorGlow, colorShadow, thicknessPx, density)
    }

    /**
     * Custom Drawable que renderiza o contorno baseado na silhueta (Alpha) da imagem.
     */
    private class SilhouetteDrawable(
        private val original: Drawable,
        private val outlineColor: Int,
        private val glowColor: Int,
        private val shadowColor: Int,
        private val thickness: Float,
        private val density: Float
    ) : Drawable() {

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        private var silhouetteBitmap: Bitmap? = null
        private var externalColorFilter: ColorFilter? = null

        override fun draw(canvas: Canvas) {
            val b = bounds
            if (b.isEmpty) return

            // 1. Calcular Escala de Segurança
            // Para um contorno real de 20dp, precisamos de espaço.
            // Se a View é pequena (ex: 64dp na lista), a insígnia encolhe para dar lugar ao contorno.
            val margin = thickness + (6 * density) // Espaço para outline + glow + shadow
            val scale = (b.width() / (b.width() + 2 * margin)).coerceAtMost(b.height() / (b.height() + 2 * margin))

            // 2. Preparar Bitmap da Silhueta
            val bmp = getSilhouetteBitmap(b.width(), b.height()) ?: return

            // 3. Aplicar Filtro Externo (ex: Grayscale do Adapter) ao Canvas inteiro se necessário
            val saveCount = if (externalColorFilter != null) {
                canvas.saveLayer(null, Paint().apply { colorFilter = externalColorFilter })
            } else {
                canvas.save()
            }

            canvas.scale(scale, scale, b.centerX().toFloat(), b.centerY().toFloat())

            // --- RENDERIZAÇÃO DAS CAMADAS ---
            
            // A. Sombra (Deslocada)
            val shadowOff = 3f * density
            drawLayer(canvas, bmp, b.centerX() + shadowOff, b.centerY() + shadowOff, shadowColor, thickness + 1f * density)

            // B. Glow Ciano (Expandido)
            if (glowColor != Color.TRANSPARENT) {
                drawLayer(canvas, bmp, b.centerX().toFloat(), b.centerY().toFloat(), glowColor, thickness + 2.5f * density)
            }

            // C. Contorno Branco Real (Base)
            drawLayer(canvas, bmp, b.centerX().toFloat(), b.centerY().toFloat(), outlineColor, thickness)

            // D. Imagem Original (Topo)
            original.setBounds(b.left, b.top, b.right, b.bottom)
            original.draw(canvas)

            canvas.restoreToCount(saveCount)
        }

        private fun drawLayer(canvas: Canvas, bmp: Bitmap, cx: Float, cy: Float, color: Int, radius: Float) {
            paint.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
            
            // Mecanismo de Loop Circular (Sticker Effect)
            // Desenha a imagem em múltiplos ângulos para formar um contorno sólido e contínuo.
            // Para 20dp, usamos passos finos para evitar "serrilhado" ou gaps.
            val steps = (radius * 1.5f).toInt().coerceIn(32, 90)
            val angleStep = 2.0 * Math.PI / steps
            
            for (i in 0 until steps) {
                val angle = i * angleStep
                val x = cx + (Math.cos(angle) * radius).toFloat() - bmp.width / 2f
                val y = cy + (Math.sin(angle) * radius).toFloat() - bmp.height / 2f
                canvas.drawBitmap(bmp, x, y, paint)
            }
        }

        private fun getSilhouetteBitmap(w: Int, h: Int): Bitmap? {
            if (silhouetteBitmap == null && w > 0 && h > 0) {
                try {
                    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    val c = Canvas(bmp)
                    original.setBounds(0, 0, w, h)
                    original.draw(c)
                    silhouetteBitmap = bmp
                } catch (e: Exception) {
                    return null
                }
            }
            return silhouetteBitmap
        }

        override fun setAlpha(alpha: Int) { paint.alpha = alpha }
        
        override fun setColorFilter(colorFilter: ColorFilter?) {
            // Guarda o filtro vindo de fora (como o Grayscale do RecyclerView)
            this.externalColorFilter = colorFilter
            invalidateSelf()
        }

        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    }
}
