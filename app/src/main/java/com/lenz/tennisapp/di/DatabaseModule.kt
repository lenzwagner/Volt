package com.lenz.tennisapp.di

import android.content.Context
import androidx.room.Room
import com.lenz.tennisapp.data.db.TennisDatabase
import com.lenz.tennisapp.data.db.dao.EloDao
import com.lenz.tennisapp.data.db.dao.FollowedPlayerDao
import com.lenz.tennisapp.data.db.dao.MatchDao
import com.lenz.tennisapp.data.db.dao.NotifiedMatchDao
import com.lenz.tennisapp.data.db.dao.PredictionDao
import com.lenz.tennisapp.data.db.dao.RankingDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TennisDatabase =
        Room.databaseBuilder(context, TennisDatabase::class.java, "tennis.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideMatchDao(db: TennisDatabase): MatchDao = db.matchDao()
    @Provides fun provideEloDao(db: TennisDatabase): EloDao = db.eloDao()
    @Provides fun providePredictionDao(db: TennisDatabase): PredictionDao = db.predictionDao()
    @Provides fun provideRankingDao(db: TennisDatabase): RankingDao = db.rankingDao()
    @Provides fun provideFollowedPlayerDao(db: TennisDatabase): FollowedPlayerDao = db.followedPlayerDao()
    @Provides fun provideNotifiedMatchDao(db: TennisDatabase): NotifiedMatchDao = db.notifiedMatchDao()
    @Provides fun providePlayerDao(db: TennisDatabase): com.lenz.tennisapp.data.db.dao.PlayerDao = db.playerDao()
}
