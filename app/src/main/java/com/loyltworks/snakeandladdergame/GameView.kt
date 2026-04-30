package com.loyltworks.snakeandladdergame

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import pl.droidsonroids.gif.GifDrawable

class GameView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val boardSize = 10
    private var cellWidth = 0f
    private var cellHeight = 0f
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var redSnakeImg: Drawable? = null
    private var greenSnakeImg: Drawable? = null
    private var orangeSnakeImg: Drawable? = null
    private var purpleSnakeImg: Drawable? = null
    private var blueSnakeImg: Drawable? = null
    
    private var snakeGifs = mutableMapOf<Int, GifDrawable?>()
    private var activeSnakeHead: Int? = null
    private var activeSnakeGif: GifDrawable? = null
    
    private var ladderImgs = mutableMapOf<Int, Drawable?>()
    private var playerImgs = arrayOfNulls<Drawable>(4)

    init {
        try {
            redSnakeImg = ContextCompat.getDrawable(context, R.drawable.red_snake)
            greenSnakeImg = ContextCompat.getDrawable(context, R.drawable.green_snake)
            orangeSnakeImg = ContextCompat.getDrawable(context, R.drawable.orenge_snake)
            purpleSnakeImg = ContextCompat.getDrawable(context, R.drawable.purpule_snake)
            blueSnakeImg = ContextCompat.getDrawable(context, R.drawable.blue_snake)
            
            ladderImgs[3] = ContextCompat.getDrawable(context, R.drawable.ladder3)
            ladderImgs[5] = ContextCompat.getDrawable(context, R.drawable.ladder5)
            ladderImgs[6] = ContextCompat.getDrawable(context, R.drawable.ladder6)
            ladderImgs[8] = ContextCompat.getDrawable(context, R.drawable.ladder8)
            ladderImgs[17] = ContextCompat.getDrawable(context, R.drawable.ladder17)
            ladderImgs[0] = ContextCompat.getDrawable(context, R.drawable.ladder) // Default

            playerImgs[0] = ContextCompat.getDrawable(context, R.drawable.red_player)
            playerImgs[1] = ContextCompat.getDrawable(context, R.drawable.blue_player)
            playerImgs[2] = ContextCompat.getDrawable(context, R.drawable.green_player)
            playerImgs[3] = ContextCompat.getDrawable(context, R.drawable.white_player)

            // Initialize Snake GIFs
            snakeGifs[98] = GifDrawable(resources, R.raw.red_snake)
            snakeGifs[85] = GifDrawable(resources, R.raw.green_snake)
            snakeGifs[61] = GifDrawable(resources, R.raw.blue_snake)
            snakeGifs[54] = GifDrawable(resources, R.raw.orenge_snake)
            snakeGifs[32] = GifDrawable(resources, R.raw.purple_snake)
            
            // Set callbacks for GIFs to ensure they animate
            snakeGifs.values.forEach { gif ->
                gif?.callback = object : Drawable.Callback {
                    override fun invalidateDrawable(who: Drawable) {
                        // Only invalidate if this is the currently active animation to save resources
                        if (activeSnakeHead != null) {
                            postInvalidateOnAnimation()
                        }
                    }
                    override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {
                        postDelayed(what, `when`)
                    }
                    override fun unscheduleDrawable(who: Drawable, what: Runnable) {
                        removeCallbacks(what)
                    }
                }
                gif?.stop() // Don't play until needed
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private var playerPos = IntArray(4) { 1 }
    private var playerCoords = Array(4) { Pair(0f, 0f) }
    private var playerScales = FloatArray(4) { 1f }
    
    private var animators = arrayOfNulls<ValueAnimator>(4)

    private val snakes = mapOf(
        98 to 2,   // Red
        85 to 52,  // Green
        61 to 40,  // Blue
        54 to 16,  // Orange
        32 to 7    // Purple
    )
    private val ladders = mapOf(
        3 to 24,
        14 to 77,
        30 to 51,
        41 to 62,
        53 to 72,
        88 to 93
    )

    private var offsetX = 0f
    private var offsetY = 0f
    private var boardSizePixels = 0f
    private var boardHeightPixels = 0f
    private var boardMargin = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        boardMargin = 5 * resources.displayMetrics.density
        
        boardSizePixels = w.toFloat() - 2 * boardMargin // Subtract margin from width
        cellWidth = boardSizePixels / boardSize
        
        val verticalIncrease = 5 * resources.displayMetrics.density
        cellHeight = cellWidth + verticalIncrease
        boardHeightPixels = cellHeight * boardSize
        
        offsetX = (w - boardSizePixels) / 2f
        offsetY = (h - boardHeightPixels) / 2f
        updateAllPlayerPixelCoords()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawBackground(canvas)
        drawNumbers(canvas)
        drawSnakes(canvas)
        drawLadders(canvas)
        drawPlayers(canvas)
        drawActiveSnakeGif(canvas)
    }

    private fun drawActiveSnakeGif(canvas: Canvas) {
        val start = activeSnakeHead ?: return
        val gif = snakeGifs[start] ?: return
        val end = snakes[start] ?: return

        val staticImg = when (start) {
            98 -> redSnakeImg
            85 -> greenSnakeImg
            61 -> blueSnakeImg
            54 -> orangeSnakeImg
            32 -> purpleSnakeImg
            else -> redSnakeImg
        } ?: return

        var (sx, sy) = getCoords(start)
        var (ex, ey) = getCoords(end)

        val dx = ex - sx
        val dy = ey - sy
        val length = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
        val nx = if (length > 0) dx / length else 0f
        val ny = if (length > 0) dy / length else 0f

        val shiftDist = cellHeight * 0.35f
        var adjSx = sx - nx * shiftDist
        var adjSy = sy - ny * shiftDist
        var adjEx = ex + nx * shiftDist
        var adjEy = ey + ny * shiftDist

        // Apply same visual adjustments as drawSnakes
        if (start == 54) {
           adjSx = sx + (cellWidth * 0.60f)
           adjSy = sy + (cellHeight * 0.80f)
           adjEx = adjSx - (cellWidth * 3.20f)
           adjEy = adjSy + (cellHeight * 2.40f)
        }
        if (start == 98) {
            adjSx = sx + nx * (shiftDist * 0.3f)
            adjSy = sy + ny * (shiftDist * 0.3f)
            adjEx = ex + (cellWidth * 0.8f)
            adjEy = ey
        }
        if (start == 61) {
            adjSx = sx + cellWidth * 0.45f
            adjEx = ex + cellWidth * 0.45f
        }
        if (start == 32) {
            val spoofS = getCoords(33)
            val spoofE = getCoords(8)
            adjSx = spoofS.first
            adjSy = spoofS.second
            adjEx = spoofE.first
            adjEy = spoofE.second
        }
        if (start == 85) {
            adjSx = sx + (cellWidth * 0.9f)
            adjSy = sy - (cellHeight * 0.4f)
            adjEx = ex - (cellWidth * 1.0f)
            adjEy = ey + (cellHeight * 0.4f)
        }

        val adjDx = adjEx - adjSx
        val adjDy = adjEy - adjSy
        val adjDistance = Math.hypot(adjDx.toDouble(), adjDy.toDouble()).toFloat()
        val angle = Math.toDegrees(Math.atan2(adjDy.toDouble(), adjDx.toDouble())).toFloat()
        
        // Use GIF's own intrinsic aspect ratio with a fallback to static image to prevent 0-size issues
        val gifW = gif.intrinsicWidth.toFloat()
        val gifH = gif.intrinsicHeight.toFloat()
        val aspect = if (gifH > 0) gifW / gifH else (staticImg.intrinsicWidth.toFloat() / staticImg.intrinsicHeight.toFloat())
        
        var thickness = adjDistance * aspect
        val minT = if (start == 98) cellWidth * 1.5f else cellWidth * 0.6f
        thickness = thickness.coerceIn(minT, cellWidth * 2.5f)
        
        canvas.save()
        canvas.translate(adjSx, adjSy)
        canvas.rotate(angle - 90f)
        gif.setBounds((-thickness / 2).toInt(), 0, (thickness / 2).toInt(), adjDistance.toInt())
        gif.draw(canvas)
        canvas.restore()
    }

    private fun drawBackground(canvas: Canvas) {
        // Draw wood-style background border/base
        paint.color = Color.parseColor("#B28741") // Requested wood color
        canvas.drawRect(
            offsetX - boardMargin, offsetY - boardMargin,
            offsetX + boardSizePixels + boardMargin, offsetY + boardHeightPixels + boardMargin,
            paint
        )

        // Draw individual tiles
        val tileMargin = cellWidth * 0.05f
        val tileRadius = cellWidth * 0.15f
        
        for (i in 1..100) {
            val (cx, cy) = getCoords(i)
            
            // Choose alternating colors
            val tileColor = if (i % 2 == 0) "#D8C28A" else "#D4BC7C"
            
            // Draw tile background
            paint.color = Color.parseColor(tileColor)
            paint.style = Paint.Style.FILL
            canvas.drawRoundRect(
                cx - cellWidth / 2 + tileMargin,
                cy - cellHeight / 2 + tileMargin,
                cx + cellWidth / 2 - tileMargin,
                cy + cellHeight / 2 - tileMargin,
                tileRadius, tileRadius,
                paint
            )
            
            // Draw tile border (Subtle)
            paint.color = alphaColor("#000000", 0.05f) // Very subtle darkening
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            canvas.drawRoundRect(
                cx - cellWidth / 2 + tileMargin,
                cy - cellHeight / 2 + tileMargin,
                cx + cellWidth / 2 - tileMargin,
                cy + cellHeight / 2 - tileMargin,
                tileRadius, tileRadius,
                paint
            )
        }
    }

    private fun alphaColor(hex: String, alpha: Float): Int {
        val base = Color.parseColor(hex)
        return Color.argb(
            (alpha * 255).toInt(),
            Color.red(base),
            Color.green(base),
            Color.blue(base)
        )
    }

    private fun drawNumbers(canvas: Canvas) {
        paint.color = Color.parseColor("#513B22") // Dark bronze from image
        paint.textSize = cellWidth * 0.25f
        paint.isFakeBoldText = false
        paint.style = Paint.Style.FILL
        for (i in 1..100) {
            val (x, y) = getCoords(i)
            val numStr = String.format("%02d", i) // Leading zeroes
            // Position numbers with more significant padding from the corners
            canvas.drawText(numStr, x - cellWidth * 0.32f, y - cellHeight * 0.15f, paint)
        }
    }

    private fun getCellPosition(row: Int, col: Int): Int {
        val invertedRow = boardSize - 1 - row
        return if (invertedRow % 2 == 0) {
            invertedRow * boardSize + col + 1
        } else {
            invertedRow * boardSize + (boardSize - col)
        }
    }

    // This getCoords is still used by setPositions and animateMove.
    private fun getCoords(pos: Int): Pair<Float, Float> {
        if (pos <= 0) return Pair(offsetX - cellWidth, offsetY + (boardSize - 0.5f) * cellHeight)
        val row = (pos - 1) / boardSize
        val colInRow = (pos - 1) % boardSize
        val col = if (row % 2 == 0) colInRow else (boardSize - 1 - colInRow)
        val x = offsetX + col * cellWidth + cellWidth / 2
        val y = offsetY + (boardSize - 1 - row) * cellHeight + cellHeight / 2
        return Pair(x, y)
    }

    private fun drawLadders(canvas: Canvas) {
        for ((start, end) in ladders) {
            val img = when (start) {
                3 -> ladderImgs[6]
                14 -> ladderImgs[17]
                30 -> ladderImgs[8]
                41 -> ladderImgs[6]
                53 -> ladderImgs[6]
                88 -> ladderImgs[3]
                else -> ladderImgs[0]
            } ?: continue
            val (sx, sy) = getCoords(start)
            val (ex, ey) = getCoords(end)
            
            // Move endpoints inwards to start at top of start box and end at bottom of end box
            val dx = ex - sx
            val dy = ey - sy
            val length = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
            val nx = if (length > 0) dx / length else 0f
            val ny = if (length > 0) dy / length else 0f
            
            val shiftDist = cellHeight * 0.35f
            var adjSx = sx + nx * shiftDist
            var adjSy = sy + ny * shiftDist
            var adjEx = ex - nx * shiftDist
            var adjEy = ey - ny * shiftDist
            
            var xPadding = cellWidth * 0.4f
            var yPadding = cellHeight * 0.4f
            
            if (start == 3) {
                // The PNG's internal transparent boundaries cause the visible ladder to shrink slightly 
                // when pegged strictly to the mathematical centers. A mild expansion is needed to make 
                // the ladder full-sized and robust without returning to the previous bloated distortion.
                adjSx = sx
                adjSy = sy
                adjEx = ex
                adjEy = ey
                xPadding = cellWidth * 0.10f
                yPadding = cellHeight * 0.10f
            } else if (start == 30 || start == 88) {
                // The vertical ladders (30->51 and 88->93) fill their generic bounds fully, making them appear 
                // much fatter than the thinner diagonal ladders. Narrowing the horizontal padding
                // shrinks their physical width. We set it precisely to 0.28f to give them exactly the requested size.
                xPadding = cellWidth * 0.28f
            }
            
            val left = minOf(adjSx, adjEx) - xPadding
            val top = minOf(adjSy, adjEy) - yPadding
            val right = maxOf(adjSx, adjEx) + xPadding
            val bottom = maxOf(adjSy, adjEy) + yPadding
            
            img.setBounds(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
            img.draw(canvas)
        }
    }

    private fun drawSnakes(canvas: Canvas) {
        for ((start, end) in snakes) {
            if (start == activeSnakeHead) continue // Hide static snake during GIF animation
            
            val img = when (start) {
                98 -> redSnakeImg
                85 -> greenSnakeImg
                61 -> blueSnakeImg
                54 -> orangeSnakeImg
                32 -> purpleSnakeImg
                else -> redSnakeImg
            } ?: continue
            var (sx, sy) = getCoords(start)
            var (ex, ey) = getCoords(end)
            
            val dx = ex - sx
            val dy = ey - sy
            val length = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
            val nx = if (length > 0) dx / length else 0f
            val ny = if (length > 0) dy / length else 0f
            
            // Move outwards so snake head is at top of start box and tail at bottom of end box
            val shiftDist = cellHeight * 0.35f
            var adjSx = sx - nx * shiftDist
            var adjSy = sy - ny * shiftDist
            var adjEx = ex + nx * shiftDist
            var adjEy = ey + ny * shiftDist
            
            if (start == 54) {
               // To perfectly map the completely flipped orange snake PNG (LocalHead=-1w, LocalTail=+1w)
               // onto the trajectory dx=-2, dy=4 (towards box 16), we solved the rigid-body constraints system.
               // R=53.1 degrees, L=4.0w. This cleanly preserves the shape without any visible kinks or fatness.
               adjSx = sx + (cellWidth * 0.60f)
               adjSy = sy + (cellHeight * 0.80f)
               adjEx = adjSx - (cellWidth * 3.20f)
               adjEy = adjSy + (cellHeight * 2.40f)
            }
            
            if (start == 98) {
                // Move head to be slightly closer to the center of the starting box (98)
                adjSx = sx + nx * (shiftDist * 0.3f)
                adjSy = sy + ny * (shiftDist * 0.3f)
                
                // Tail should be in the center of the end box (2)
                // Shift adjEx right to visually offset the pronounced left-curve in the red snake PNG
                adjEx = ex + (cellWidth * 0.8f)
                adjEy = ey
            }
            
            if (start == 61) {
                // Mathematically peg the start and end anchors to the exact center of the boxes
                adjSx = sx
                adjSy = sy
                adjEx = ex
                adjEy = ey
                
                // Visual correction: The blue snake asset leans heavily to the left. 
                // We linearly shift its entire bounding box further to the right so its visually painted parts 
                // land directly over the true center of the boxes without spilling out of bounds.
                adjSx += cellWidth * 0.45f
                adjEx += cellWidth * 0.45f
            }

            
            if (start == 32) {
                // The PNG naturally offsets its visual head and tail to different tiles natively.
                // The user loved how the pixels looked when geometrically anchored to 33 and 8 
                // (which physically dropped the visible paint perfectly into 32 and 7).
                // Now that the functional route is mapped correctly to 32->7, we artificially spoof the visual anchors 
                // explicitly back to 33 and 8 to permanently freeze its perfectly aligned graphical appearance.
                val spoofS = getCoords(33)
                val spoofE = getCoords(8)
                adjSx = spoofS.first
                adjSy = spoofS.second
                adjEx = spoofE.first
                adjEy = spoofE.second
            }
            
            if (start == 85) {
                // The green snake spans diagonally. The user specifically wants its head to lay at the "right side end center" of 85.
                // We push the X anchor strongly to the right to achieve this visual head placement.
                adjSx = sx + (cellWidth * 0.9f)
                adjSy = sy - (cellHeight * 0.4f)
                
                // The graphic intrinsically has its tail curling heavily to the right side of its bounding box. 
                // To keep the visible tail perfectly inside 53 without reaching rightward into 52, 
                // we pull the bottom anchoring leftwards even more strongly.
                adjEx = ex - (cellWidth * 1.0f)
                adjEy = ey + (cellHeight * 0.4f)
            }
            

            val adjDx = adjEx - adjSx
            val adjDy = adjEy - adjSy
            val adjDistance = Math.hypot(adjDx.toDouble(), adjDy.toDouble()).toFloat()
            val angle = Math.toDegrees(Math.atan2(adjDy.toDouble(), adjDx.toDouble())).toFloat()
            
            // Revert snake rendering to use rotation as it looks better for these assets
            var thickness = adjDistance * (img.intrinsicWidth.toFloat() / img.intrinsicHeight.toFloat())
            
            val minT = if (start == 98) cellWidth * 1.5f else cellWidth * 0.6f
            thickness = thickness.coerceIn(minT, cellWidth * 2.5f)
            
            canvas.save()
            canvas.translate(adjSx, adjSy)
            canvas.rotate(angle - 90f)
            img.setBounds((-thickness / 2).toInt(), 0, (thickness / 2).toInt(), adjDistance.toInt())
            img.draw(canvas)
            canvas.restore()
        }
    }

    private fun drawPlayers(canvas: Canvas) {
        val playersInCell = mutableMapOf<Int, MutableList<Int>>()
        for (i in playerPos.indices) {
            val pos = playerPos[i]
            playersInCell.getOrPut(pos) { mutableListOf() }.add(i)
        }

        for ((pos, indices) in playersInCell) {
            val count = indices.size
            for (i in 0 until count) {
                val idx = indices[i]
                val (px, py) = playerCoords[idx]
                val img = playerImgs[idx] ?: continue
                
                // Read original dimensions to prevent squashing and maintain correct aspect ratio
                val imgW = img.intrinsicWidth.toFloat()
                val imgH = img.intrinsicHeight.toFloat()
                val aspect = if (imgH > 0) imgW / imgH else 1.0f
                
                // User requested reducing scale to make them look cleaner. 0.4f scales down from old 0.5f.
                var targetHeight = cellHeight * 0.4f * playerScales[idx]
                if (count > 1) targetHeight *= 0.75f
                val targetWidth = targetHeight * aspect
                
                var ox = 0f
                var oy = 0f
                if (count > 1) {
                    val angle = (360f / count) * i
                    val r = cellWidth * 0.2f
                    ox = (r * Math.cos(Math.toRadians(angle.toDouble()))).toFloat()
                    oy = (r * Math.sin(Math.toRadians(angle.toDouble()))).toFloat()
                }

                val cx = px + ox
                val cy = py + oy
                
                // Anchor the pieces so their bottoms sit securely near the floor of the cell
                val bottomY = cy + cellHeight * 0.35f
                
                img.setBounds(
                    (cx - targetWidth / 2f).toInt(), 
                    (bottomY - targetHeight).toInt(), 
                    (cx + targetWidth / 2f).toInt(), 
                    bottomY.toInt()
                )
                img.draw(canvas)
            }
        }
    }

    fun setPositions(pPositions: IntArray, animateIdx: Int = -1, stepByStep: Boolean = false) {
        for (i in 0 until 4) {
            if (i == animateIdx) {
                animateMove(playerPos[i], pPositions[i], i, stepByStep) { x, y -> 
                    playerCoords[i] = Pair(x, y)
                    invalidate()
                }
            } else {
                val (x, y) = getCoords(pPositions[i])
                playerCoords[i] = Pair(x, y)
            }
            playerPos[i] = pPositions[i]
        }
        invalidate()
    }

    private fun animateMove(from: Int, to: Int, playerIdx: Int, stepByStep: Boolean, onUpdate: (Float, Float) -> Unit) {
        animators[playerIdx]?.cancel()
        
        val animator = if (stepByStep && to > from) {
            val path = (from..to).toList()
            val anim = ValueAnimator.ofFloat(0f, (path.size - 1).toFloat())
            anim.duration = 250L * (path.size - 1)
            anim.interpolator = LinearInterpolator()
            anim.addUpdateListener { animation ->
                val v = animation.animatedValue as Float
                val index = v.toInt()
                val fraction = v - index
                
                if (index >= path.size - 1) {
                    val (x, y) = getCoords(path.last())
                    onUpdate(x, y)
                } else {
                    val (startX, startY) = getCoords(path[index])
                    val (endX, endY) = getCoords(path[index + 1])
                    val x = startX + (endX - startX) * fraction
                    val y = startY + (endY - startY) * fraction
                    onUpdate(x, y)
                }
            }
            anim
        } else {
            val (startX, startY) = getCoords(from)
            val (endX, endY) = getCoords(to)
            val anim = ValueAnimator.ofFloat(0f, 1f)
            anim.duration = 500L
            anim.interpolator = LinearInterpolator()
            anim.addUpdateListener { animation ->
                val fraction = animation.animatedValue as Float
                val x = startX + (endX - startX) * fraction
                val y = startY + (endY - startY) * fraction
                onUpdate(x, y)
            }
            anim
        }
        
        animators[playerIdx] = animator
        animator.start()
    }

    fun playSnakeBiteAnimation(start: Int, end: Int, playerIdx: Int, onAnimationEnd: () -> Unit) {
        // Step 1: Show the GIF and play immediately
        activeSnakeHead = start
        val gif = snakeGifs[start]
        activeSnakeGif = gif
        
        // Robust reset: stop, set loop, seek to 0, and then start
        gif?.stop()
        gif?.loopCount = 0 // Infinite loop during the animation window
        gif?.seekTo(0)
        gif?.setVisible(true, true)
        gif?.start()
        postInvalidateOnAnimation()

        // Step 2: Delay slightly before shrinking player to sync with snake mouth opening
        postDelayed({
            val shrinkAnim = ValueAnimator.ofFloat(1f, 0f)
            shrinkAnim.duration = 500L
            shrinkAnim.addUpdateListener { animation ->
                playerScales[playerIdx] = animation.animatedValue as Float
                invalidate()
            }
            shrinkAnim.start()
        }, 300L) // 300ms delay allows the snake to open its mouth before player shrinks
        
        // Step 3: Wait for 2 seconds (total GIF time)
        postDelayed({
            // Step 4: Teleport player to tail
            playerPos[playerIdx] = end
            playerCoords[playerIdx] = getCoords(end)
            
            // Step 5: Hide GIF and stop it
            activeSnakeHead = null
            activeSnakeGif?.stop()
            activeSnakeGif = null
            invalidate()

            // Step 6: Grow player at tail (0.5s)
            val growAnim = ValueAnimator.ofFloat(0f, 1f)
            growAnim.duration = 500L
            growAnim.addUpdateListener { anim ->
                playerScales[playerIdx] = anim.animatedValue as Float
                invalidate()
            }
            
            growAnim.addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    onAnimationEnd()
                }
            })
            growAnim.start()
            
        }, 2000L) // Exact 2 seconds pause for GIF as requested
    }

    private fun updateAllPlayerPixelCoords() {
        for (i in 0 until 4) {
            val (x, y) = getCoords(playerPos[i])
            playerCoords[i] = Pair(x, y)
        }
    }
}
