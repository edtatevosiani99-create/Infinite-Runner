package com.infiniterunner.app

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import kotlin.math.max
import kotlin.random.Random

class GameView(context: Context) : View(context) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private var playerY = 0f
    private var velocity = 0f
    private var speed = 9f
    private var score = 0
    private var high = context.getSharedPreferences("game",0).getInt("high",0)
    private var over = false
    private var last = System.currentTimeMillis()
    private val obstacles = mutableListOf<RectF>()

    init { p.typeface = Typeface.DEFAULT_BOLD; setBackgroundColor(Color.rgb(12,16,24)); post { playerY = height-180f; spawn() ; invalidate() } }

    private fun spawn() {
        val h = 45f + Random.nextInt(55)
        val w = 35f + Random.nextInt(35)
        val x = width + 40f
        obstacles += RectF(x, height-90f-h, x+w, height-90f)
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val now=System.currentTimeMillis(); val dt=max(0.001f,(now-last)/1000f); last=now
        if (!over) update(dt)
        drawWorld(c)
        postInvalidateOnAnimation()
    }

    private fun update(dt:Float) {
        speed += dt*0.12f
        score += (dt*10).toInt()
        velocity += 1800f*dt; playerY += velocity*dt
        val ground=height-90f-70f
        if(playerY>ground){playerY=ground;velocity=0f}
        obstacles.forEach{it.offset(-speed*60f*dt,0f)}
        if(obstacles.isEmpty() || obstacles.last().left < width-300) spawn()
        obstacles.removeAll{it.right<0}
        val player=RectF(70f,playerY,135f,playerY+70f)
        if(obstacles.any{RectF.intersects(player,it)}) gameOver()
    }

    private fun drawWorld(c:Canvas) {
        p.color=Color.rgb(22,28,40); c.drawRect(0f,height-90f,width.toFloat(),height.toFloat(),p)
        p.color=Color.WHITE; c.drawRect(0f,height-92f,width.toFloat(),height-88f,p)
        p.color=Color.rgb(70,190,255); c.drawRoundRect(70f,playerY,135f,playerY+70f,18f,18f,p)
        p.color=Color.rgb(255,90,90); obstacles.forEach{c.drawRoundRect(it,10f,10f,p)}
        p.textSize=42f; p.color=Color.WHITE; c.drawText("SCORE  $score",30f,55f,p)
        p.textSize=26f; c.drawText("BEST  $high",30f,90f,p)
        if(over){p.textSize=64f;c.drawText("GAME OVER",width/2f-190f,height/2f-20f,p);p.textSize=30f;c.drawText("TAP TO RESTART",width/2f-125f,height/2f+35f,p)}
    }

    private fun jump(){ if(over){restart();return}; val ground=height-90f-70f;if(playerY>=ground-2) velocity=-650f }
    private fun gameOver(){ over=true; if(score>high){high=score;context.getSharedPreferences("game",0).edit().putInt("high",high).apply()} }
    private fun restart(){score=0;speed=9f;velocity=0f;obstacles.clear();playerY=height-160f;over=false;spawn()}
    override fun onTouchEvent(e:MotionEvent):Boolean { if(e.action==MotionEvent.ACTION_DOWN) jump(); return true }
}
