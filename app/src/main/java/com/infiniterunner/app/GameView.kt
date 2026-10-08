package com.infiniterunner.app

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import kotlin.math.sin
import kotlin.random.Random

class GameView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val prefs = context.getSharedPreferences("game", 0)
    private var playerY = 0f
    private var velocityY = 0f
    private var speed = 420f
    private var score = 0
    private var scoreFloat = 0f
    private var highScore = prefs.getInt("high", 0)
    private var gameOver = false
    private var lastTime = System.nanoTime()
    private var worldTime = 0f
    private var spawnTimer = 0.7f
    private val obstacles = mutableListOf<Obstacle>()

    private data class Obstacle(val rect: RectF, val type: Int)

    init {
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        post { resetGame() }
    }

    private val playerHeight get() = 86f
    private val playerWidth get() = 58f
    private val groundHeight get() = 104f
    private fun groundY() = height.toFloat() - groundHeight

    private fun resetGame() {
        score = 0
        scoreFloat = 0f
        speed = 420f
        velocityY = 0f
        gameOver = false
        worldTime = 0f
        obstacles.clear()
        spawnTimer = 0.7f
        playerY = groundY() - playerHeight
        lastTime = System.nanoTime()
        invalidate()
    }

    private fun spawnObstacle() {
        if (width <= 0 || height <= 0) return
        val type = Random.nextInt(5)
        val w = when (type) { 0 -> 46f; 1 -> 78f; 2 -> 58f; 3 -> 66f; else -> 72f }
        val h = when (type) { 0 -> 62f; 1 -> 82f; 2 -> 68f; 3 -> 74f; else -> 58f }
        val x = width.toFloat() + 40f
        obstacles += Obstacle(RectF(x, groundY() - h, x + w, groundY()), type)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val now = System.nanoTime()
        val dt = ((now - lastTime) / 1_000_000_000f).coerceIn(0f, 0.033f)
        lastTime = now
        if (!gameOver) update(dt)
        drawWorld(canvas)
        postInvalidateOnAnimation()
    }

    private fun update(dt: Float) {
        worldTime += dt
        scoreFloat += dt * 10f
        score = scoreFloat.toInt()
        speed = (420f + score * 2.2f).coerceAtMost(900f)

        velocityY += 1900f * dt
        playerY += velocityY * dt
        val floor = groundY() - playerHeight
        if (playerY >= floor) {
            playerY = floor
            velocityY = 0f
        }

        spawnTimer -= dt
        if (spawnTimer <= 0f) {
            spawnObstacle()
            spawnTimer = Random.nextDouble(0.9, 1.55).toFloat()
        }

        val dx = speed * dt
        obstacles.forEach { it.rect.offset(-dx, 0f) }
        obstacles.removeAll { it.rect.right < -30f }

        val player = RectF(76f, playerY + 12f, 126f, playerY + playerHeight - 3f)
        if (obstacles.any { RectF.intersects(player, it.rect) }) endGame()
    }

    private fun drawWorld(canvas: Canvas) {
        val cycle = (worldTime / 18f) % 4f
        val phase = cycle.toInt()
        val blend = cycle - phase
        val sky = when (phase) {
            0 -> Color.rgb(105, 190, 245)
            1 -> Color.rgb(245, 174, 112)
            2 -> Color.rgb(38, 88, 145)
            else -> Color.rgb(25, 48, 78)
        }
        canvas.drawColor(sky)

        when (phase) {
            0 -> drawForest(canvas, blend)
            1 -> drawCity(canvas, blend)
            2 -> drawSea(canvas, blend)
            else -> drawNight(canvas, blend)
        }

        drawGround(canvas)
        drawPerson(canvas)
        obstacles.forEach { drawObstacle(canvas, it) }

        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 34f
        paint.color = Color.WHITE
        paint.setShadowLayer(4f, 2f, 2f, Color.BLACK)
        canvas.drawText("SCORE  $score", 28f, 48f, paint)
        paint.textSize = 24f
        canvas.drawText("BEST  $highScore", 28f, 80f, paint)
        paint.clearShadowLayer()

        if (gameOver) {
            paint.textAlign = Paint.Align.CENTER
            paint.color = Color.WHITE
            paint.setShadowLayer(8f, 2f, 2f, Color.BLACK)
            paint.textSize = 54f
            canvas.drawText("GAME OVER", width / 2f, height / 2f - 20f, paint)
            paint.textSize = 28f
            canvas.drawText("TAP TO RESTART", width / 2f, height / 2f + 32f, paint)
            paint.clearShadowLayer()
            paint.textAlign = Paint.Align.LEFT
        }
    }

    private fun drawGround(canvas: Canvas) {
        paint.color = Color.rgb(52, 52, 56)
        canvas.drawRect(0f, groundY(), width.toFloat(), height.toFloat(), paint)
        paint.color = Color.rgb(225, 225, 225)
        val offset = (worldTime * speed) % 100f
        for (x in -100f..width.toFloat() step 100f) {
            canvas.drawRect(x - offset, groundY() + 42f, x + 48f - offset, groundY() + 48f, paint)
        }
    }

    private fun drawForest(canvas: Canvas, blend: Float) {
        paint.color = Color.rgb(48, 115, 60)
        canvas.drawRect(0f, groundY() - 180f, width.toFloat(), groundY(), paint)
        for (x in -40..width step 100) {
            val sway = sin(worldTime * 1.5 + x) * 5f
            paint.color = Color.rgb(92, 62, 40)
            canvas.drawRect(x + sway, groundY() - 125f, x + 22f + sway, groundY(), paint)
            paint.color = Color.rgb(25, 105, 48)
            canvas.drawCircle(x + 11f + sway, groundY() - 145f, 48f, paint)
            canvas.drawCircle(x - 18f + sway, groundY() - 115f, 34f, paint)
            canvas.drawCircle(x + 38f + sway, groundY() - 112f, 34f, paint)
        }
    }

    private fun drawCity(canvas: Canvas, blend: Float) {
        paint.color = Color.rgb(185, 188, 194)
        for (x in 0..width step 125) {
            val h = 110 + (x % 3) * 45
            canvas.drawRect(x.toFloat(), groundY() - h, x + 105f, groundY(), paint)
            paint.color = Color.rgb(90, 125, 155)
            for (y in (groundY() - h + 20).toInt() until groundY().toInt() step 34)
                canvas.drawRect(x + 16f, y.toFloat(), x + 34f, y + 16f, paint)
            paint.color = Color.rgb(185, 188, 194)
        }
    }

    private fun drawSea(canvas: Canvas, blend: Float) {
        paint.color = Color.rgb(32, 142, 190)
        canvas.drawRect(0f, groundY() - 155f, width.toFloat(), groundY(), paint)
        paint.color = Color.WHITE
        for (x in -40..width step 90) {
            val y = groundY() - 120f + sin(worldTime * 2f + x) * 8f
            canvas.drawRoundRect(x.toFloat(), y, x + 55f, y + 5f, 5f, 5f, paint)
        }
        paint.color = Color.rgb(235, 205, 105)
        canvas.drawCircle(width - 90f, 95f, 42f, paint)
    }

    private fun drawNight(canvas: Canvas, blend: Float) {
        paint.color = Color.rgb(48, 55, 74)
        for (x in 0..width step 105) {
            val h = 130 + (x % 2) * 60
            canvas.drawRect(x.toFloat(), groundY() - h, x + 88f, groundY(), paint)
            paint.color = Color.rgb(255, 218, 110)
            for (y in (groundY() - h + 22).toInt() until groundY().toInt() step 38)
                canvas.drawRect(x + 15f, y.toFloat(), x + 29f, y + 14f, paint)
            paint.color = Color.rgb(48, 55, 74)
        }
        paint.color = Color.WHITE
        canvas.drawCircle(width - 90f, 85f, 30f, paint)
        paint.color = Color.rgb(48, 55, 74)
        canvas.drawCircle(width - 78f, 75f, 28f, paint)
    }

    private fun drawPerson(canvas: Canvas) {
        val x = 101f
        val y = playerY
        val run = sin(worldTime * 12f) * 7f
        paint.color = Color.rgb(35, 35, 42)
        canvas.drawCircle(x, y + 15f, 16f, paint)
        paint.color = Color.rgb(40, 115, 210)
        canvas.drawRoundRect(x - 18f, y + 29f, x + 18f, y + 65f, 10f, 10f, paint)
        paint.color = Color.rgb(35, 35, 42)
        canvas.drawRect(x - 13f, y + 61f, x - 5f, y + 82f + run, paint)
        canvas.drawRect(x + 5f, y + 61f, x + 13f, y + 82f - run, paint)
        canvas.drawRect(x - 21f, y + 34f, x - 14f, y + 60f + run, paint)
        canvas.drawRect(x + 14f, y + 34f, x + 21f, y + 60f - run, paint)
        paint.color = Color.WHITE
        canvas.drawCircle(x - 5f, y + 11f, 2.5f, paint)
        canvas.drawCircle(x + 5f, y + 11f, 2.5f, paint)
    }

    private fun drawObstacle(canvas: Canvas, o: Obstacle) {
        val r = o.rect
        when (o.type) {
            0 -> {
                paint.color = Color.rgb(245, 125, 35)
                val p = Path()
                p.moveTo(r.centerX(), r.top)
                p.lineTo(r.right, r.bottom)
                p.lineTo(r.left, r.bottom)
                p.close()
                canvas.drawPath(p, paint)
                paint.color = Color.WHITE
                canvas.drawRect(r.left + 8f, r.top + r.height() * .35f, r.right - 8f, r.top + r.height() * .46f, paint)
            }
            1 -> {
                paint.color = Color.rgb(150, 92, 48)
                canvas.drawRoundRect(r, 10f, 10f, paint)
                paint.color = Color.rgb(75, 45, 25)
                canvas.drawCircle(r.centerX(), r.centerY(), r.width() * .22f, paint)
            }
            2 -> {
                paint.color = Color.rgb(75, 105, 70)
                canvas.drawRoundRect(r, 7f, 7f, paint)
                paint.color = Color.rgb(180, 205, 170)
                canvas.drawRect(r.left + 9f, r.top + 12f, r.right - 9f, r.top + 22f, paint)
            }
            3 -> {
                paint.color = Color.rgb(225, 215, 185)
                canvas.drawRect(r.left, r.top + 10f, r.right, r.bottom, paint)
                paint.color = Color.rgb(110, 70, 42)
                canvas.drawRect(r.left + 10f, r.top, r.left + 18f, r.bottom + 5f, paint)
                canvas.drawRect(r.right - 18f, r.top, r.right - 10f, r.bottom + 5f, paint)
            }
            else -> {
                paint.color = Color.rgb(185, 35, 45)
                canvas.drawRect(r.left, r.top + 16f, r.right, r.bottom, paint)
                paint.color = Color.rgb(230, 230, 230)
                canvas.drawRect(r.left + 8f, r.top + 27f, r.right - 8f, r.top + 36f, paint)
            }
        }
    }

    private fun jump() {
        if (gameOver) { resetGame(); return }
        val floor = groundY() - playerHeight
        if (playerY >= floor - 4f) velocityY = -760f
    }

    private fun endGame() {
        gameOver = true
        if (score > highScore) {
            highScore = score
            prefs.edit().putInt("high", highScore).apply()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) jump()
        return true
    }
}