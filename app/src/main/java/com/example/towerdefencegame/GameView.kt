package com.example.towerdefencegame

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import android.os.Handler
import android.os.Looper
import android.view.DragEvent
import android.view.MotionEvent
import android.util.Log
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt

class GameView(context: Context) : View(context) {

    private val paint = Paint()
    private lateinit var autoShotRunnable: Runnable
    private var autoShootingStarted = false

    private data class Enemy(
        var x: Float,
        var y: Float,
        var speed: Float,
        var destroyed: Boolean,
        var hasHitTower: Boolean = false,
        var health: Int = 1,
        var maxHealth: Int = 1,
        val targetX: Float,
        val targetY: Float,
        val isBoss: Boolean = false,
        var isDying: Boolean = false,
        var deathStartTime: Long = 0L
    )

    private data class Shot(
        var x: Float,
        var y: Float,
        var speed: Float,
        var active: Boolean,
        val target: Enemy?,
        var dx: Float = 0f,
        var dy: Float = 0f
    )

    private data class Tower (
        val x: Float,
        val y: Float
    )

    private val enemies = mutableListOf<Enemy>()
    private val shots = mutableListOf<Shot>()
    private val towers = mutableListOf<Tower>()

    private var lives = 3
    private var isGameOver = false

    private var wave = 1
    private var enemiesPerWave = 3

    private var score = 0

    private val handler = Handler(Looper.getMainLooper())

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        if (!autoShootingStarted) {
            startNewWave()
            startAutoShooting()
            autoShootingStarted = true
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawColor(Color.WHITE)

        paint.color = Color.BLACK
        paint.textSize = 50f
        canvas.drawText("Score: $score", 50f, 100f, paint)
        canvas.drawText("Wave: $wave", 50f, 170f, paint)
        canvas.drawText("Lives: $lives", 50f, 240f, paint)

        paint.color = Color.BLUE
        for (tower in towers) {
            canvas.drawRect(tower.x - 50, tower.y - 50, tower.x + 50, tower.y + 50, paint)
        }

        paint.color = Color.RED
        for (enemy in enemies) {
            if (!enemy.destroyed) {
                canvas.drawCircle(enemy.x, enemy.y, 50f, paint)
            }
        }

        paint.color = Color.BLACK
        for (shot in shots) {
            canvas.drawRect(shot.x, shot.y, shot.x + 20, shot.y + 20, paint)
        }

        moveEnemies()

        for (enemy in enemies) {
            if (!enemy.destroyed) {
                val healthRatio = enemy.health.toFloat() / enemy.maxHealth
                val red = (255 * (1 - healthRatio)).toInt()
                val green = (255 * healthRatio).toInt()
                paint.color = Color.rgb(red, green, 0)

                val size = if (enemy.isBoss) 100f else 50f

                if (enemy.isDying) {
                    val elapsed = System.currentTimeMillis() - enemy.deathStartTime
                    val progress = elapsed / 300f

                    if (progress >= 1f) {
                        enemy.destroyed = true
                        score++
                    } else {
                        paint.alpha = ((1 - progress) * 255).toInt()
                        paint.color = Color.YELLOW
                        val scale = 1f - 0.5f * progress
                        val size = 50f * scale
                        canvas.drawCircle(enemy.x + 25f, enemy.y + 25f, size, paint)
                        paint.alpha = 255
                    }
                } else {
                    canvas.drawRect(enemy.x, enemy.y, enemy.x + size, enemy.y + size, paint)
                }
            }
        }

        if (enemies.all {it.destroyed}) {
            wave++
            enemiesPerWave++
            startNewWave()
        }

        invalidate()

    }

    private fun moveEnemies() {
        if (isGameOver) return

        for (enemy in enemies) {
            if (!enemy.destroyed) {
                val dx = enemy.targetX - enemy.x
                val dy = enemy.targetY - enemy.y
                val distance = sqrt(dx * dx + dy * dy)

                if (distance > 1f) {
                    val directionX = dx / distance
                    val directionY = dy / distance

                    enemy.x += directionX * enemy.speed
                    enemy.y += directionY * enemy.speed
                }

                val reachedTarget = distance < 50f

                if (reachedTarget && !enemy.hasHitTower) {
                    enemy.hasHitTower = true
                    enemy.destroyed = true
                    lives--

                    if (lives <= 0 && !isGameOver) {
                        isGameOver = true

                        (context as? Activity)?.runOnUiThread {
                            val intent = Intent(context, GameOverActivity::class.java)
                            intent.putExtra("score", score)
                            context.startActivity(intent)
                            (context as Activity).finish()
                        }
                    }
                }
            }
        }

        checkCollisions()

        invalidate()

    }

    private fun startNewWave() {

        towers.add(Tower(width - 100f, height / 2f))

        enemies.clear()

        val random = java.util.Random()

        val isBossWave = wave % 3 == 0

        val enemySize = 60f
        val verticalSpacing = 5f

        val totalHeight = height.toFloat()
        val maxEnemies = (totalHeight / (enemySize + verticalSpacing)).toInt()

        val actualEnemyCount = minOf(enemiesPerWave, maxEnemies)

        val usedYPosition = mutableListOf<Float>()

        repeat(actualEnemyCount) {

            var y: Float

            do {
                y = random.nextInt((totalHeight - enemySize).toInt()).toFloat()
            } while (usedYPosition.any { abs(it - y) < enemySize + verticalSpacing })

            usedYPosition.add(y)

            val minSpeed = 2f + wave
            val maxSpeed = 4f + wave
            val speed = minSpeed + random.nextFloat() * (maxSpeed - minSpeed)

            val health = 1 + random.nextInt(3)

            enemies.add(
                Enemy(
                    x = 0f,
                    y = y,
                    speed = speed,
                    destroyed = false,
                    health = health,
                    maxHealth = health,
                    targetX = width - 100f,
                    targetY = height / 2f,
                    isBoss = false
                )
            )
        }

        if (isBossWave) {
            enemies.add(
                Enemy(
                    x = 0f,
                    y = height / 2f,
                    speed = 1.5f,
                    destroyed = false,
                    health = 7 * wave,
                    maxHealth = 7 * wave,
                    targetX = width.toFloat() - 100f,
                    targetY = height / 2f,
                    isBoss = true
                )
            )
        }
    }

    private fun restartGame() {
        stopAutoShooting()

        score = 0
        wave = 1
        lives = 3
        enemiesPerWave = 3
        isGameOver = false
        enemies.clear()
        shots.clear()

        startNewWave()
        startAutoShooting()
        invalidate()
    }

    private fun startAutoShooting() {
        autoShotRunnable = object : Runnable {
            override fun run() {
                if(!isGameOver) {
                    shootAtNearestEnemy()
                    handler.postDelayed(this, 800)
                    invalidate()
                }
            }
        }
        handler.postDelayed(autoShotRunnable, 800)
    }

    private fun checkCollisions() {

        val iterator = shots.iterator()
        while (iterator.hasNext()) {
            val shot = iterator.next()
            shot.x -= shot.dx * shot.speed
            shot.y -= shot.dy * shot.speed

           val target = shot.target
            if (target != null && enemies.contains(target)) {
                if (shot.x < target.x) {
                    shot.x +=min(5f, target.x - shot.x)
                }

                if (shot.y < target.y) {
                    shot.y += min(15f, target.y - shot.y)
                } else if (shot.y > target.y) {
                    shot.y -= min(15f, shot.y - target.y)
                }
            }

            if (shot.x > width) {
                iterator.remove()
                continue
            }

            for (enemy in enemies) {
                if (!enemy.destroyed &&
                    shot.x + 20 >= enemy.x &&
                    shot.x <= enemy.x + 50 &&
                    shot.y >= enemy.y &&
                    shot.y <= enemy.y + 50
                ) {

                    enemy.health--

                    if (enemy.health <= 0 && !enemy.isDying) {
                        if (enemy.isBoss) {
                            addNewTower()
                        }
                        enemy.isDying = true
                        enemy.deathStartTime = System.currentTimeMillis()
                        //enemy.destroyed = true
                        //enemies.remove(enemy)
                        //score++
                    }

                    iterator.remove()
                    break

                }
            }
        }
    }

    private fun addNewTower() {
        val possibleYPosition = listOf(700f, 1700f)

        for (y in possibleYPosition) {
            val occupied = towers.any { abs(it.y - y) < 80 }
            if (!occupied) {
                towers.add(Tower(width - 100f, y))
                break
            }
        }
    }

    private fun stopAutoShooting() {
        if (::autoShotRunnable.isInitialized) {
            handler.removeCallbacks(autoShotRunnable)
        }
    }

    private fun shootAtNearestEnemy() {
        val speed = -15f
        val active = true

        for (tower in towers) {
            val nearest = enemies
                .filter { !it.destroyed }
                .minByOrNull { abs(it.x - tower.x) + abs(it.y - tower.y) }

            if (nearest != null) {

                val dx = nearest.x - tower.x
                val dy = nearest.y - tower.y
                val length = sqrt(dx * dx + dy * dy)

                val unitX = dx / length
                val unitY = dy / length

                shots.add(Shot(tower.x, tower.y, speed, active, nearest, unitX, unitY))
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {

            if (isGameOver) {
                restartGame()
            } else {
                shootAtNearestEnemy()
            }
            return true
        }
        return super.onTouchEvent(event)
    }
}
