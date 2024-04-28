package com.ms.news.features.breakingnews

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ms.news.data.NewsArticle
import com.ms.news.data.NewsRepository
import com.ms.news.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class BreakingNewsViewModel @Inject constructor(
    private val repository: NewsRepository
) : ViewModel() {

    private val eventChannel = Channel<Event>()
    val events = eventChannel.receiveAsFlow()

    //// here Channel is used to trigger Manual Refresh Logic which triggers and to get new Data**.
    // **(this Channel use is bit complex this comment is just for my own easy understanding to link to other things in it).
    private val refreshTriggerChannel = Channel<Refresh>()
    private val refreshTrigger = refreshTriggerChannel.receiveAsFlow()
    //// \\

    var pendingScrollToTopAfterRefresh = false

    val breakingNews = refreshTrigger.flatMapLatest { refresh ->
        ///breakdown of flatMapLatest
        //map : used to transform values
        //flat : will convert Flow<Flow<Data>> into Flow<Data>
        //latest : will cancel the old block or Flow in order to work with newOne to save memory and all.
        repository.getBreakingNews(
            refresh == Refresh.FORCE,
            onFetchSuccess = {
                pendingScrollToTopAfterRefresh = true
            },
            onFetchFailed = {
                    t ->
                viewModelScope.launch { eventChannel.send(Event.ShowErrorMessage(t)) }
            }
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)
        // stateIn() converts the normal cold flow into hot State Flow which retains its latest value(helpful in configChange).

    init {
        viewModelScope.launch {
            // deleting non bookmarked articles from news_table which are older than 7 days.
            repository.deleteNonBookmarkedArticlesOlderThan(
                System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
            )
        }
    }
    /*
    val breakingNews =
        repository.getBreakingNews()
            .stateIn(viewModelScope, SharingStarted.Lazily, null)
    // stateIn() converts the normal cold flow into hot State Flow which retains its latest value(helpful in configChange).
    */

    fun onStart(){
        if(breakingNews.value !is Resource.Loading){
            // if we are already loading so to prevent loading again and removing previous
            // unfinished Flow this condition is applied.
            viewModelScope.launch {
                refreshTriggerChannel.send(Refresh.NORMAL)
            }
        }
    }

    fun onManualRefresh(){
        if(breakingNews.value !is Resource.Loading){
            // if we are already loading so to prevent loading again and removing previous
            // unfinished Flow this condition is applied.
            viewModelScope.launch {
                refreshTriggerChannel.send(Refresh.FORCE)
            }
        }
    }

    fun onBookmarkClick(article: NewsArticle){
        val currentlyBookmarked = article.isBookmarked
        val updatedArticle = article.copy(isBookmarked = !currentlyBookmarked)
        viewModelScope.launch{
            repository.updateArticle(updatedArticle)
        }
    }

    /*
    private val breakingNewsFlow = MutableStateFlow<List<NewsArticle>>(emptyList())
    val breakingNews: Flow<List<NewsArticle>> = breakingNewsFlow

    init {
        viewModelScope.launch {
            val news = repository.getBreakingNews()
            breakingNewsFlow.value = news
        }
    }*/

    enum class Refresh{
        FORCE,NORMAL
    }

    sealed class Event{
        data class ShowErrorMessage(val error: Throwable): Event()
    }
}