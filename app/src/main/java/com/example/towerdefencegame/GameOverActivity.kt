package com.example.towerdefencegame

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class GameOverActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_over)

        val score = intent.getIntExtra("score", 0)
        val playerName = "Player1"
        val scoreText: TextView = findViewById(R.id.scoreText)
        scoreText.text = "Game Over\nScore: $score"

        val restarButton: Button = findViewById(R.id.restartButton)
        restarButton.setOnClickListener {

            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        lifecycleScope.launch {
            val db = GameDatabase.getDatabase(this@GameOverActivity)
            val dao = db.gameResultDao()

            val result = GameResult(
                playerName = playerName,
                score = score,
                date = System.currentTimeMillis()
            )
            dao.insert(result)
        }

    }

}