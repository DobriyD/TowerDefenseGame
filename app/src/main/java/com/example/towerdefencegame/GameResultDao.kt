package com.example.towerdefencegame

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface GameResultDao {
    @Insert
    suspend fun insert(result: GameResult)

    @Query("SELECT * FROM game_results ORDER BY score DESC LIMIT 10")
    suspend fun getTopResults(): List<GameResult>
}