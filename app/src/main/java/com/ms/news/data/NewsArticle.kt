package com.ms.news.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// counter-part to newsArticleDTO
// this will put into database and used throughout the app.
// This represents table in database
@Entity(tableName = "news_article")
data class NewsArticle(
    val title: String?,
    @PrimaryKey val url: String,
    val thumbnailUrl: String?,
    val isBookmarked: Boolean,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "breaking_news")
data class BreakingNews(
    val articleUrl: String,
    @PrimaryKey(autoGenerate = true) val id: Int = 0
)

@Entity(tableName = "search_results", primaryKeys = ["searchQuery","articleUrl"])
data class SearchResult(
    val searchQuery: String,
    val articleUrl: String,
    val queryPosition: Int
)

//@Entity(tableName = "breaking_news")
//data class SearchNews(
//    val articleUrl: String,
//    @PrimaryKey(autoGenerate = true) val id: Int = 0
//)