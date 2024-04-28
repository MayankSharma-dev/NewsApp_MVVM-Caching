package com.ms.news.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.room.withTransaction
import com.ms.news.api.NewsApi
import com.ms.news.util.Resource
import com.ms.news.util.networkBoundResource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class NewsRepository @Inject constructor(
    private val newsApi: NewsApi, private val newsArticleDatabase: NewsArticleDatabase
) {
    private val newsArticleDao = newsArticleDatabase.newsArticleDao()

    fun getBreakingNews(
        forceRefresh: Boolean,
        onFetchFailed: (Throwable) -> Unit,
        onFetchSuccess: () -> Unit
    ): Flow<Resource<List<NewsArticle>>> =
        networkBoundResource(query = { // passing function as lambda..
            newsArticleDao.getAllBreakingArticles()
        }, fetch = {
            val response = newsApi.getBreakingNews()
            // passing NewsResponse to fetch()
            response.articles
        }, saveFetchResult = { serverBreakingNewsArticles ->
            //receiving List<NewsArticleDTO> i.e, (it -> lambda) from fetch() which is passed into saveFetchResult(ResultType) in NetworkBoundResource.

            // get currently all bookmarkedArticles
            val bookmarkedArticles = newsArticleDao.getAllBookmarkedArticles().first()

            // for articles database table
            val breakingNewsArticles = serverBreakingNewsArticles.map {

                val isBookmarked = bookmarkedArticles.any{bookmarkedArticle ->
                    bookmarkedArticle.url == it.url
                }

                NewsArticle(
                    title = it.title,
                    url = it.url,
                    thumbnailUrl = it.urlToImage,
                    isBookmarked = isBookmarked
                )
            }

            // for different breaking news database table
            val breakingNews = breakingNewsArticles.map { articles ->
                BreakingNews(articles.url)
            }

            // performing in database transaction i.e,
            // either all operations in this transaction succeed or none
            newsArticleDatabase.withTransaction {
                newsArticleDao.deleteAllBreakingNews()
                newsArticleDao.insertArticles(breakingNewsArticles)
                newsArticleDao.insertBreakingNews(breakingNews)
            }

        }, shouldFetch = { cacheArticles ->
            if(forceRefresh){
                true
            }else {
                val sortedArticles = cacheArticles.sortedBy { article ->
                    article.updatedAt
                }
                val oldestTimeStamp = sortedArticles.firstOrNull()?.updatedAt
                val needsRefresh =
                    oldestTimeStamp == null || oldestTimeStamp < System.currentTimeMillis() -
                            TimeUnit.MINUTES.toMillis(5) // set to 60 in final not necessary but Sir Florian set that.
                needsRefresh
            }
        }, onFetchSuccess = onFetchSuccess, onFetchFailed = { t ->
            if (t !is HttpException && t !is IOException) {
                throw t
            }
            onFetchFailed(t)
        })

    suspend fun deleteNonBookmarkedArticlesOlderThan(timeStampInMillis: Long){
        newsArticleDao.deleteNonBookmarkedArticlesOlderThan(timeStampInMillis)
    }

    suspend fun updateArticle(article: NewsArticle){
        newsArticleDao.updateArticle(article)
    }

    fun getAllBookmarkedArticles(): Flow<List<NewsArticle>> =
        newsArticleDao.getAllBookmarkedArticles()

    suspend fun resetAllBookmarks(){
        newsArticleDao.resetAllBookmarks()
    }

    @OptIn(ExperimentalPagingApi::class)
    fun getSearchResultPaged(query: String, refreshOnInit: Boolean) : Flow<PagingData<NewsArticle>> =
        Pager(
            config = PagingConfig(pageSize = 20, maxSize = 200),
            remoteMediator = SearchNewsRemoteMediator(query, newsApi, newsArticleDatabase, refreshOnInit),
            pagingSourceFactory = { newsArticleDao.getSearchResultArticlesPaged(query) }
        ).flow

    /*
    // getting data directly from the network without offline caching and NetworkBoundResource and more..
    suspend fun getBreakingNews(): List<NewsArticle> {
        val response = newsApi.getBreakingNews()
        val serverBreakingNewsArticles = response.articles

        val breakingNewsArticle = serverBreakingNewsArticles.map {
            NewsArticle(
                title = it.title,
                url = it.url,
                thumbnailUrl = it.urlToImage,
                isBookmarked = false
            )
        }
        return breakingNewsArticle
    }*/
}
