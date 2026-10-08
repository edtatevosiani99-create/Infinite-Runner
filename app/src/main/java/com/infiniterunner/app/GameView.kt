package com.infiniterunner.app

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
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
    private var spawnTimer = 0.7f
    private val obstacles = mutableListOf<RectF>()

    init {
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        post { resetGame() }
    }

    private val playerHeight get() = 72f
    private val playerWidth get() = 62f
    private val groundHeight get() = 96f
    private fun groundY() = height.toFloat() - groundHeight

    private fun resetGame() {
        score = 0
        scoreFloat = 0f
        speed = 420f
        velocityY = 0f
        gameOver = false
        obstacles.clear()
        spawnTimer = 0.7f
        playerY = groundY() - playerHeight
        lastTime = System.nanoTime()
        invalidate()
    }

    private fun spawnObstacle() {
        if (width <= 0 || height <= 0) return
        val h = Random.nextInt(55, 115).toFloat()
        val w = Random.nextInt(38, 68).toFloat()
        val x = width.toFloat() + 40f
        obstacles += RectF(x, groundY() - h, x + w, groundY())
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
        obstacles.forEach { it.offset(-dx, 0f) }
        obstacles.removeAll { it.right < -20f }

        val player = RectF(77f, playerY + 6f, 125f, playerY + playerHeight - 4f)
        if (obstacles.any { RectF.intersects(player, it) }) endGame()
    }

    private fun drawWorld(canvas: Canvas) {
        canvas.drawColor(Color.rgb(10, 15, 25))
        paint.color = Color.rgb(20, 28, 42)
        canvas.drawRect(0f, groundY(), width.toFloat(), height.toFloat(), paint)
        paint.color = Color.rgb(90, 210, 255)
        canvas.drawRect(0f, groundY() - 5f, width.toFloat(), groundY(), paint)

        paint.color = Color.rgb(70, 190, 255)
        canvas.drawRoundRect(70f, playerY, 70f + playerWidth, playerY + playerHeight, 18f, 18f, paint)
        paint.color = Color.WHITE
        canvas.drawCircle(113f, playerY + 22f, 5f, paint)

        paint.color = Color.rgb(255, 82, 92)
        obstacles.forEach { canvas.drawRoundRect(it, 10f, 10f, paint) }

        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 34f
        paint.color = Color.WHITE
        canvas.drawText("SCORE  $score", 28f, 48f, paint)
        paint.textSize = 24f
        canvas.drawText("BEST  $highScore", 28f, 80f, paint)

        if (gameOver) {
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = 54f
            canvas.drawText("GAME OVER", width / 2f, height / 2f - 20f, paint)
            paint.textSize = 28f
            canvas.drawText("TAP TO RESTART", width / 2f, height / 2f + 32f, paint)
            paint.textAlign = Paint.Align.LEFT
        }
    }

    private fun jump() {
        if (gameOver) {
            resetGame()
            return
        }
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
