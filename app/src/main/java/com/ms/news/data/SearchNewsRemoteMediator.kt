package com.ms.news.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.ms.news.api.NewsApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import java.io.IOException

private const val NEWS_STARTING_PAGE_INDEX = 1


@OptIn(ExperimentalPagingApi::class)
class SearchNewsRemoteMediator(
    private val searchQuery: String,
    private val newsApi: NewsApi,
    private val newsArticleDatabase: NewsArticleDatabase,
    private val refreshOnInit: Boolean //  for skip or launch new refresh after or before process-death.
): RemoteMediator<Int,NewsArticle>() {

    private val newsArticleDao = newsArticleDatabase.newsArticleDao()
    private val searchQueryRemoteKeyDao = newsArticleDatabase.searchQueryRemoteKeyDao()

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, NewsArticle>
    ): MediatorResult {
        val page = when(loadType){
            LoadType.REFRESH -> NEWS_STARTING_PAGE_INDEX
            LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
            LoadType.APPEND -> searchQueryRemoteKeyDao.getRemoteKey(searchQuery).nextPageKey
        }
        try {
            val response = newsApi.searchNews(searchQuery,page,state.config.pageSize)
            //delay(3000) // artificial delay
            val serverSearchResult= response.articles

            val bookmarkedArticles = newsArticleDao.getAllBookmarkedArticles().first()
            val searchResultArticles= serverSearchResult.map { serverSearchResultArticles ->
                val isBookmarked  = bookmarkedArticles.any{bookmarkedArticles ->
                    bookmarkedArticles.url  == serverSearchResultArticles.url
                }
                NewsArticle(title = serverSearchResultArticles.title,
                    url = serverSearchResultArticles.url,
                    thumbnailUrl = serverSearchResultArticles.urlToImage,
                    isBookmarked = isBookmarked)
            }

            newsArticleDatabase.withTransaction {
                if(loadType == LoadType.REFRESH){
                    newsArticleDao.deleteSearchResultForQuery(searchQuery)
                }
                val lastQueryPosition = newsArticleDao.getLastQueryPosition(searchQuery)?: 0
                var queryPosition = lastQueryPosition + 1

                val searchResult = searchResultArticles.map { articles ->
                    SearchResult(searchQuery,articles.url, queryPosition++)
                }

                val nextPageKey = page+1

                newsArticleDao.insertArticles(searchResultArticles)
                newsArticleDao.insertSearchResults(searchResult)
                searchQueryRemoteKeyDao.insertRemoteKey(
                    SearchQueryRemoteKey(searchQuery,nextPageKey)
                )
            }
            return MediatorResult.Success(endOfPaginationReached = serverSearchResult.isEmpty())
        }catch (e: IOException){
            return MediatorResult.Error(e)
        }catch (e: HttpException){
            return MediatorResult.Error(e)
        }
    }

    // for changing the behaviour of RemoteMediator for not automatically refreshing dataset at instantiating dataset.
    override suspend fun initialize(): InitializeAction {
        return if(refreshOnInit){
            InitializeAction.LAUNCH_INITIAL_REFRESH
        }else{
            InitializeAction.SKIP_INITIAL_REFRESH
        }
    }
}