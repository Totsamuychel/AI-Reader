package com.bookmind.di

import android.content.Context
import androidx.room.Room
import com.bookmind.llm.AnswerProviding
import com.bookmind.llm.AnswerService
import com.bookmind.llm.EchoMediaPipeBridge
import com.bookmind.llm.GemmaClient
import com.bookmind.llm.LLMClient
import com.bookmind.llm.MediaPipeBridge
import com.bookmind.llm.MediaPipeGemmaBridge
import com.bookmind.llm.ModelDownloadService
import com.bookmind.memory.MemoryWriting
import com.bookmind.memory.RoomMemoryWriter
import com.bookmind.persistence.AppDatabase
import com.bookmind.persistence.BookStoring
import com.bookmind.persistence.ReadingProgressStoring
import com.bookmind.persistence.QuoteStoring
import com.bookmind.persistence.RoomBookStore
import com.bookmind.persistence.RoomProgressStore
import com.bookmind.persistence.RoomQuoteStore
import com.bookmind.persistence.dao.BookDao
import com.bookmind.persistence.dao.CharacterDao
import com.bookmind.persistence.dao.ChunkDao
import com.bookmind.persistence.dao.EventDao
import com.bookmind.persistence.dao.FactDao
import com.bookmind.persistence.dao.ProgressDao
import com.bookmind.persistence.dao.ReadingSessionDao
import com.bookmind.persistence.dao.RecapDao
import com.bookmind.persistence.dao.ShelfDao
import com.bookmind.persistence.dao.UserQuoteDao
import com.bookmind.retrieval.ChunkSearchService
import com.bookmind.retrieval.DuckDuckGoWikipediaSearch
import com.bookmind.retrieval.CharacterLookupService
import com.bookmind.retrieval.ContextRetrieving
import com.bookmind.retrieval.EventLookupService
import com.bookmind.retrieval.FactSearchService
import com.bookmind.retrieval.PromptContextAssembler
import com.bookmind.retrieval.PromptContextAssembling
import com.bookmind.retrieval.RecapLookupService
import com.bookmind.retrieval.RetrievalEngine
import com.bookmind.retrieval.RoomCharacterLookup
import com.bookmind.retrieval.RoomChunkSearch
import com.bookmind.retrieval.RoomEventLookup
import com.bookmind.retrieval.RoomFactSearch
import com.bookmind.retrieval.RoomRecapLookup
import com.bookmind.retrieval.WebSearching
import com.bookmind.safety.HeuristicSpoilerScanner
import com.bookmind.safety.ResponseSpoilerScanning
import com.bookmind.safety.SpoilerBoundaryResolver
import com.bookmind.safety.SpoilerBoundaryResolving
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Provider
import javax.inject.Qualifier
import javax.inject.Singleton

/** Application-lifetime scope for work that must outlive a ViewModel (e.g. saving
 *  a reading session when the reader screen is torn down). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object CoroutineModule {
    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
}

/** Provides the Room database + DAOs. = iOS `DatabaseManager` wiring. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "bookmind.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun bookDao(db: AppDatabase): BookDao = db.bookDao()
    @Provides fun progressDao(db: AppDatabase): ProgressDao = db.progressDao()
    @Provides fun chunkDao(db: AppDatabase): ChunkDao = db.chunkDao()
    @Provides fun factDao(db: AppDatabase): FactDao = db.factDao()
    @Provides fun characterDao(db: AppDatabase): CharacterDao = db.characterDao()
    @Provides fun recapDao(db: AppDatabase): RecapDao = db.recapDao()
    @Provides fun eventDao(db: AppDatabase): EventDao = db.eventDao()
    @Provides fun userQuoteDao(db: AppDatabase): UserQuoteDao = db.userQuoteDao()
    @Provides fun shelfDao(db: AppDatabase): ShelfDao = db.shelfDao()
    @Provides fun readingSessionDao(db: AppDatabase): ReadingSessionDao = db.readingSessionDao()
}

/** Chooses the LLM bridge: real MediaPipe when the model is downloaded, else a stub. */
@Module
@InstallIn(SingletonComponent::class)
object LlmModule {

    @Provides
    @Singleton
    fun provideBridge(
        modelDownload: ModelDownloadService,
        mediaPipe: Provider<MediaPipeGemmaBridge>,
        echo: EchoMediaPipeBridge
    ): MediaPipeBridge =
        if (modelDownload.isModelPresent) mediaPipe.get() else echo
}

/** Binds module interfaces to their implementations. */
@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {

    @Binds abstract fun bookStore(impl: RoomBookStore): BookStoring
    @Binds abstract fun progressStore(impl: RoomProgressStore): ReadingProgressStoring
    @Binds abstract fun memoryWriter(impl: RoomMemoryWriter): MemoryWriting
    @Binds abstract fun quoteStore(impl: RoomQuoteStore): QuoteStoring
    @Binds abstract fun webSearch(impl: DuckDuckGoWikipediaSearch): WebSearching

    @Binds abstract fun characterLookup(impl: RoomCharacterLookup): CharacterLookupService
    @Binds abstract fun factSearch(impl: RoomFactSearch): FactSearchService
    @Binds abstract fun chunkSearch(impl: RoomChunkSearch): ChunkSearchService
    @Binds abstract fun recapLookup(impl: RoomRecapLookup): RecapLookupService
    @Binds abstract fun eventLookup(impl: RoomEventLookup): EventLookupService
    @Binds abstract fun contextRetrieving(impl: RetrievalEngine): ContextRetrieving
    @Binds abstract fun promptAssembler(impl: PromptContextAssembler): PromptContextAssembling

    @Binds abstract fun boundaryResolver(impl: SpoilerBoundaryResolver): SpoilerBoundaryResolving
    @Binds abstract fun spoilerScanner(impl: HeuristicSpoilerScanner): ResponseSpoilerScanning

    @Binds abstract fun llmClient(impl: GemmaClient): LLMClient
    @Binds abstract fun answerProviding(impl: AnswerService): AnswerProviding
}
