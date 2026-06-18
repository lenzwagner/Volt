package com.lenz.tennisapp.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.lenz.tennisapp.data.db.dao.EloDao
import com.lenz.tennisapp.data.db.dao.FollowedPlayerDao
import com.lenz.tennisapp.data.db.dao.MatchDao
import com.lenz.tennisapp.data.db.dao.NotifiedMatchDao
import com.lenz.tennisapp.data.db.dao.PlayerDao
import com.lenz.tennisapp.data.db.dao.PredictionDao
import com.lenz.tennisapp.data.db.dao.RankingDao
import com.lenz.tennisapp.data.db.entities.EloRatingEntity
import com.lenz.tennisapp.data.db.entities.FollowedPlayerEntity
import com.lenz.tennisapp.data.db.entities.MatchEntity
import com.lenz.tennisapp.data.db.entities.NotifiedMatchEntity
import com.lenz.tennisapp.data.db.entities.PlayerEntity
import com.lenz.tennisapp.data.db.entities.RankingEntity
import com.lenz.tennisapp.data.db.entities.UserPredictionEntity

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
    version = 8,
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
