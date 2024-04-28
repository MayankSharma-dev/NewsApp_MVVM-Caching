package com.ms.news.data

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NewsArticleDao{

    @Query("SELECT * FROM breaking_news INNER JOIN news_article ON articleUrl = url")
    fun getAllBreakingArticles(): Flow<List<NewsArticle>>

    @Query("SELECT * FROM search_results INNER JOIN news_article ON articleUrl = url WHERE searchQuery= :query ORDER BY queryPosition")
    fun getSearchResultArticlesPaged(query: String): PagingSource<Int,NewsArticle>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticles(articles: List<NewsArticle>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBreakingNews(breakingNews: List<BreakingNews>)

    //
    @Query("SElECT MAX(queryPosition) FROM search_results WHERE searchQuery = :searchQuery")
    suspend fun getLastQueryPosition(searchQuery: String): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE )
    suspend fun insertSearchResults(searchResult: List<SearchResult>)

    @Query("DELETE FROM search_results WHERE searchQuery = :query")
    suspend fun deleteSearchResultForQuery(query: String)
    // \\

    @Update
    suspend fun updateArticle(article: NewsArticle)

    @Query("SELECT * FROM news_article WHERE isBookmarked = 1")
    fun getAllBookmarkedArticles(): Flow<List<NewsArticle>>

    @Query("UPDATE news_article SET isBookmarked = 0")
    suspend fun resetAllBookmarks()

    @Query("DELETE FROM breaking_news")
    suspend fun deleteAllBreakingNews()

    @Query("DELETE FROM news_article WHERE updatedAt < :timeStampInMillis AND isBookmarked = 0")
    suspend fun deleteNonBookmarkedArticlesOlderThan(timeStampInMillis: Long)
}