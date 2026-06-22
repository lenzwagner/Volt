package com.lenz.tennisapp.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.lenz.tennisapp.data.db.dao.*
import com.lenz.tennisapp.data.db.entities.*

@Database(
    entities = [
        MatchEntity::class, 
        EloRatingEntity::class, 
        UserPredictionEntity::class, 
        RankingEntity::class,
        FollowedPlayerEntity::class,
        NotifiedMatchEntity::class,
        PlayerEntity::class
    ],
    version = 10,
    exportSchema = false
)
abstract class TennisDatabase : RoomDatabase() {
    abstract fun matchDao(): MatchDao
    abstract fun eloDao(): EloDao
    abstract fun predictionDao(): PredictionDao
    abstract fun rankingDao(): RankingDao
    abstract fun followedPlayerDao(): FollowedPlayerDao
    abstract fun notifiedMatchDao(): NotifiedMatchDao
    abstract fun playerDao(): PlayerDao
}
