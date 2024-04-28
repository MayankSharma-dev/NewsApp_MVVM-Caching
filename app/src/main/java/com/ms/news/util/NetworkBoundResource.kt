package com.ms.news.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// ResultType is List<NewsArticle>
// RequestType is List<NewsArticleDTO>
// Generic Typed Args makes it TypeSafety
inline fun <ResultType, RequestType> networkBoundResource(
    //Database Request.
    crossinline query: () -> Flow<ResultType>,
    //Fetching new data from API.
    crossinline fetch: suspend () -> RequestType,
    //Used to store fetched data to Database.
    crossinline saveFetchResult: suspend (RequestType) -> Unit,
    //Decides whether should fetch new data from API or if the old data is still ok.
    crossinline shouldFetch: (ResultType) -> Boolean = { true }, // by default from web
    crossinline onFetchFailed: (Throwable) -> Unit = { } ,
    crossinline onFetchSuccess: () -> Unit = { }
) = channelFlow {
    // get current data from Database to check later whether it is stale or not.
    // first will collect only one value i.e, only one List<NewsArticle> from database.
    val data = query().first()

    // checking if the data is stale or not.
    if (shouldFetch(data)) {
        ///// true if new data to be fetched..
        // For loading state..
        // it is launched in new Coroutine because while in loading state
        // user should be able to marks bookmarks so that it does not freezes while loading.
        val loading = launch {
            query().collect { send(Resource.Loading(it)) }// send kinda == emit
        }
        try {
            // not necessarily required just to cope up with fast internet to show loading bar.
            // you should remove it(delay).
            //delay(2000)

            // this will save the new Data to Database.
            saveFetchResult(fetch())
            //
            onFetchSuccess()
            // this will cancel the loading.
            loading.cancel()
            // this will tell and send the new data and stop the loading.
            query().collect { send(Resource.Success(it)) }
        } catch (t: Throwable) {
            // this will be used to show error.
            // this will be executed when there is a problem with the API request..

            onFetchFailed(t)
            loading.cancel()
            query().collect { send(Resource.Error(t,it)) }
        }
    }else{
        ///// false if new data need not to be fetched..
        // no new data from remote API.
        query().collect { send(Resource.Success(it)) }
    }
}

/*
fun <ResultType, RequestType> networkBoundResource(
    //Database Request.
    query: () -> Flow<ResultType>,
    //Fetching new data from API.
    fetch: suspend () -> RequestType,
    //Used to store fetched data to Database.
    saveFetchResult: suspend (RequestType) -> Unit,
    //Decides whether should fetch new data from API or if the old data is still ok.
    shouldFetch: (ResultType) -> Boolean = { true } // by default from web
) = channelFlow {
    // get current data from Database to check later whether it is stale or not.
    // first will collect only one value i.e, only one List<NewsArticle> from database.
    val data = query().first()

    // checking if the data is stale or not.
    if (shouldFetch(data)) {
        ///// true if new data to be fetched..
        // For loading state..
        // it is launched in new Coroutine because while in loading state
        // user should be able to marks bookmarks so that it does not freezes while loading.
        val loading = launch {
            query().collect { send(Resource.Loading(it)) }// send kinda == emit
        }
        try {
            // this will save the new Data to Database.
            saveFetchResult(fetch())
            // this will cancel the loading.
            loading.cancel()
            // this will tell and send the new data and stop the loading.
            query().collect { send(Resource.Success(it)) }
        } catch (t: Throwable) {
            // this will be used to show error.
            // this will be executed when there is a problem with the API request..
            loading.cancel()
            query().collect { send(Resource.Error(t,it)) }
        }
    }else{
        ///// false if new data need not to be fetched..
        // no new data from remote API.
        query().collect { send(Resource.Success(it)) }
    }
}*/