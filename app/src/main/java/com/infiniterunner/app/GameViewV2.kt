package com.infiniterunner.app

import android.content.Context
import android.graphics.*
import android.media.*
import android.view.*
import kotlin.math.*
import kotlin.random.Random

class GameView(context: Context) : View(context) {
    private val p=Paint(3)
    private val pref=context.getSharedPreferences("game",0)
    private var coins=pref.getInt("coins",0)
    private var char=pref.getInt("char",0)
    private var unlocked=pref.getStringSet("unlocked",setOf("0"))!!.toMutableSet()
    private var over=false; private var shop=false; private var t=0f; private var last=System.nanoTime()
    private var y=0f; private var vy=0f; private var score=0f; private var best=pref.getInt("best",0)
    private var speed=420f; private var obstacleTime=.7f; private var coinTime=2.2f
    private val obs=mutableListOf<RectF>(); private val cs=mutableListOf<RectF>()
    private var music:AudioTrack?=null

    init { p.typeface=Typeface.DEFAULT_BOLD; startMusic(); post{reset()} }
    private val ph get()=102f
    private val ground get()=height-104f

    private fun reset(){over=false;shop=false;t=0f;score=0f;speed=420f;obstacleTime=.7f;coinTime=2.2f;obs.clear();cs.clear();y=ground-ph;last=System.nanoTime()}

    override fun onDraw(c:Canvas){
        val now=System.nanoTime(); val dt=((now-last)/1e9f).coerceIn(0f,.033f); last=now
        if(!over&&!shop) update(dt); draw(c); postInvalidateOnAnimation()
    }

    private fun update(dt:Float){
        t+=dt; score+=dt*10f; speed=(420+score*2.2f).coerceAtMost(900f)
        vy+=1900*dt; y+=vy*dt; if(y>ground-ph){y=ground-ph;vy=0f}
        obstacleTime-=dt; if(obstacleTime<=0){spawnObstacle();obstacleTime=Random.nextDouble(.9,1.55).toFloat()}
        coinTime-=dt; if(coinTime<=0){spawnCoin();coinTime=Random.nextDouble(1.8,4.2).toFloat()}
        val dx=speed*dt;obs.forEach{it.offset(-dx,0f)};cs.forEach{it.offset(-dx,0f)}
        obs.removeAll{it.right<0};cs.removeAll{it.right<0}
        val player=RectF(70f,y+10,145f,y+ph-3)
        if(obs.any{RectF.intersects(player,it)}) end()
        val got=cs.filter{RectF.intersects(player,it)}
        if(got.isNotEmpty()){coins+=got.size;cs.removeAll(got.toSet());pref.edit().putInt("coins",coins).apply()}
    }

    private fun spawnObstacle(){val w=Random.nextInt(58,105).toFloat();val h=Random.nextInt(65,105).toFloat();obs+=RectF(width+30f,ground-h,width+30f+w,ground)}
    private fun spawnCoin(){val yy=ground-ph-Random.nextInt(0,130);cs+=RectF(width+30f,yy,width+78f,yy+48)}

    private fun draw(c:Canvas){
        val phase=((t/18)%4).toInt()
        c.drawColor(when(phase){0->Color.rgb(105,190,245);1->Color.rgb(245,174,112);2->Color.rgb(38,88,145);else->Color.rgb(25,48,78)})
        drawBackground(c,phase);p.color=Color.rgb(52,52,56);c.drawRect(0f,ground.toFloat(),width.toFloat(),height.toFloat(),p)
        p.color=Color.WHITE;var x=-(t*speed%100);while(x<width){c.drawRect(x,ground+42f,x+48,ground+48,p);x+=100}
        drawCharacter(c,105f,y);cs.forEach{drawCoin(c,it)};obs.forEach{drawObstacle(c,it)}
        text(c,"SCORE ${score.toInt()}",28f,45f,32f,Color.WHITE);text(c,"BEST ${best}",28f,78f,22f,Color.WHITE);text(c,"COINS ${coins}",28f,108f,22f,Color.WHITE)
        p.textAlign=Paint.Align.RIGHT;text(c,"CHARACTERS",width-25f,45f,22f,Color.WHITE);p.textAlign=Paint.Align.LEFT
        if(over){p.textAlign=Paint.Align.CENTER;text(c,"GAME OVER",width/2f,height/2f-20,52f,Color.WHITE);text(c,"TAP TO RESTART",width/2f,height/2f+35,26f,Color.WHITE);p.textAlign=Paint.Align.LEFT}
        if(shop)drawShop(c)
    }

    private fun drawBackground(c:Canvas,phase:Int){
        p.color=when(phase){0->Color.rgb(48,115,60);1->Color.rgb(185,188,194);2->Color.rgb(32,142,190);else->Color.rgb(48,55,74)}
        c.drawRect(0f,ground-180f,width.toFloat(),ground.toFloat(),p)
        for(x in 0..width step 120){p.color=when(phase){0->Color.rgb(25,105,48);1->Color.rgb(90,125,155);2->Color.WHITE;else->Color.rgb(255,218,110)};c.drawCircle(x.toFloat(),ground-120f,if(phase==2) 5f else 35f,p)}
    }

    private fun drawCharacter(c:Canvas,x:Float,yy:Float){
        val r=sin(t*12)*8
        when(char){
            0->{p.color=Color.DKGRAY;c.drawCircle(x,yy+20,19f,p);p.color=Color.rgb(40,115,210);c.drawRoundRect(x-22,yy+35,x+22,yy+78,10f,10f,p);p.color=Color.DKGRAY;c.drawRect(x-16,yy+74,x-6,yy+102+r,p);c.drawRect(x+6,yy+74,x+16,yy+102-r,p)}
            1->{p.color=Color.rgb(25,25,30);c.drawCircle(x,yy+20,22f,p);p.color=Color.rgb(190,35,45);c.drawRoundRect(x-25,yy+34,x+25,yy+80,10f,10f,p);p.color=Color.DKGRAY;c.drawRect(x-17,yy+76,x-5,yy+104+r,p);c.drawRect(x+5,yy+76,x+17,yy+104-r,p)}
            else->{p.color=Color.LTGRAY;c.drawRoundRect(x-23,yy+4,x+23,yy+49,8f,8f,p);p.color=Color.DKGRAY;c.drawRect(x-26,yy+49,x+26,yy+82,p);p.color=Color.CYAN;c.drawCircle(x-8,yy+24,4f,p);c.drawCircle(x+8,yy+24,4f,p);p.color=Color.DKGRAY;c.drawRect(x-17,yy+80,x-6,yy+104+r,p);c.drawRect(x+6,yy+80,x+17,yy+104-r,p)}
        }
    }

    private fun drawCoin(c:Canvas,r:RectF){p.color=Color.rgb(255,210,45);c.drawCircle(r.centerX(),r.centerY(),24f,p);p.color=Color.rgb(150,95,20);p.textAlign=Paint.Align.CENTER;p.textSize=24f;c.drawText("C",r.centerX(),r.centerY()+8,p);p.textAlign=Paint.Align.LEFT}
    private fun drawObstacle(c:Canvas,r:RectF){p.color=Color.rgb(185,35,45);c.drawRoundRect(r,10f,10f,p);p.color=Color.WHITE;c.drawRect(r.left+10,r.top+12,r.right-10,r.top+24,p)}

    private fun drawShop(c:Canvas){
        p.color=Color.argb(240,10,15,25);c.drawRect(0f,0f,width.toFloat(),height.toFloat(),p);p.textAlign=Paint.Align.CENTER
        text(c,"CHARACTER SHOP",width/2f,58f,38f,Color.WHITE);text(c,"COINS: ${coins}",width/2f,95f,24f,Color.WHITE)
        val xs=floatArrayOf(width*.25f,width*.5f,width*.75f);val names=arrayOf("RUNNER","NINJA","ROBOT");val cost=intArrayOf(0,50,120)
        for(i in 0..2){drawCharacter(c,xs[i],160f,i);val label=if(char==i)"SELECTED"else if(unlocked.contains(i.toString()))"SELECT"else"BUY ${cost[i]}";text(c,names[i],xs[i],280f,22f,Color.WHITE);text(c,label,xs[i],320f,19f,Color.WHITE)}
        text(c,"TAP BELOW TO CLOSE",width/2f,height-25f,19f,Color.WHITE);p.textAlign=Paint.Align.LEFT
    }

    private fun drawCharacter(c:Canvas,x:Float,yy:Float,type:Int){val old=char;char=type;drawCharacter(c,x,yy);char=old}
    private fun text(c:Canvas,s:String,x:Float,y:Float,size:Float,color:Int){p.textSize=size;p.color=color;p.textAlign=Paint.Align.LEFT;c.drawText(s,x,y,p)}

    private fun end(){over=true;val sc=score.toInt();if(sc>best){best=sc;pref.edit().putInt("best",best).apply()}}

    override fun onTouchEvent(e:MotionEvent):Boolean{
        if(e.action!=MotionEvent.ACTION_DOWN)return true
        if(shop){if(e.y>height*.72f){shop=false;return true};val i=when{e.x<width/3f->0;e.x<width*2/3f->1;else->2};val cost=intArrayOf(0,50,120)[i];if(unlocked.contains(i.toString())){char=i;pref.edit().putInt("char",char).apply()}else if(coins>=cost){coins-=cost;unlocked.add(i.toString());char=i;pref.edit().putInt("coins",coins).putInt("char",char).putStringSet("unlocked",unlocked).apply()};return true}
        if(e.x>width-230&&e.y<130){shop=true;return true};if(over)reset()else if(y>=ground-ph-5)vy=-820f;return true
    }

    private fun startMusic(){
        val rate=22050;val notes=intArrayOf(262,330,392,330,294,349,440,349);val n=rate/3;val data=ShortArray(notes.size*n);var k=0
        for(f in notes)for(i in 0 until n)data[k++]=(sin(2*PI*f*i/rate)*2600*(1.0-i.toDouble()/n)).toInt().toShort()
        music=AudioTrack.Builder().setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()).setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()).setBufferSizeInBytes(data.size*2).setTransferMode(AudioTrack.MODE_STATIC).build()
        music?.write(data,0,data.size);music?.setLoopPoints(0,data.size,-1);music?.play()
    }
    override fun onDetachedFromWindow(){music?.stop();music?.release();super.onDetachedFromWindow()}
}
