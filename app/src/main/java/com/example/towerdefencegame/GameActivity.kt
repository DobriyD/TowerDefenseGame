package com.example.towerdefencegame

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class GameActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val gameView = GameView(this)
        setContentView(gameView)
    }
}