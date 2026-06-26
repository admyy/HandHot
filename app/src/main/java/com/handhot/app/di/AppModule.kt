package com.handhot.app.di

import android.content.Context
import androidx.room.Room
import com.handhot.app.data.local.AppDatabase
import com.handhot.app.data.local.dao.FeedItemDao
import com.handhot.app.data.local.dao.FeedSourceDao
import com.handhot.app.data.remote.fetcher.FetcherFactory
import com.handhot.app.data.remote.fetcher.OkHttpProvider
import com.handhot.app.data.repository.FeedRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "handhot.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideFeedSourceDao(db: AppDatabase): FeedSourceDao = db.feedSourceDao()

    @Provides
    fun provideFeedItemDao(db: AppDatabase): FeedItemDao = db.feedItemDao()

    @Provides
    @Singleton
    fun provideOkHttpProvider(
        cookieInterceptor: com.handhot.app.data.cookie.CookieInterceptor
    ): OkHttpProvider = OkHttpProvider(cookieInterceptor)

    @Provides
    @Provides
    @Singleton
    fun provideFetcherFactory(
        provider: OkHttpProvider,
        @ApplicationContext context: Context
    ): FetcherFactory {
        return FetcherFactory(provider.publicClient, provider.loginClient, context)
    }

    @Provides
    @Singleton
    fun provideFeedRepository(
        sourceDao: FeedSourceDao,
        itemDao: FeedItemDao,
        fetcherFactory: FetcherFactory
    ): FeedRepository = FeedRepository(sourceDao, itemDao, fetcherFactory)
}
