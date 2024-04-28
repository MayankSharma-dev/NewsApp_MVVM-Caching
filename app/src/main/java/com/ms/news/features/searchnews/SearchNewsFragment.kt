package com.ms.news.features.searchnews

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.LoadState
import androidx.recyclerview.widget.RecyclerView
import com.ms.news.MainActivity
import com.ms.news.R
import com.ms.news.databinding.FragmentSerachNewsBinding
import com.ms.news.util.onQueryTextSubmit
import com.ms.news.util.showIfOrInvisible
import com.ms.news.util.showSnackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SearchNewsFragment : Fragment(R.layout.fragment_serach_news),
    MainActivity.OnBottomNavigationFragmentReselectedListener {

    private val viewModel: SearchNewsViewModel by viewModels()
    private lateinit var newsArticleAdapter: NewsArticlePagingAdapter

    private var currentBinding: FragmentSerachNewsBinding? = null
    private val binding get() = currentBinding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        currentBinding = FragmentSerachNewsBinding.bind(view)

        newsArticleAdapter = NewsArticlePagingAdapter(onItemClick = { article ->
            val uri = Uri.parse(article.url)
            val intent = Intent(Intent.ACTION_VIEW, uri)
            requireActivity().startActivity(intent)
        }, onBookmarkClick = { article ->
            viewModel.onBookmarkClick(article)
        })

        binding.apply {
            recyclerView.apply {
                adapter = newsArticleAdapter.withLoadStateFooter(
                    NewsArticleLoadStateAdapter(retry = newsArticleAdapter::retry)
                )
                setHasFixedSize(true)
                itemAnimator?.changeDuration = 0
            }
            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.searchResults.collectLatest { data ->

                        newsArticleAdapter.submitData(data)
                    }
                }
            }

            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.hasCurrentQuery.collect{ hasCurrentQuery ->
                        instructions.isVisible = !hasCurrentQuery
                        swipeRefreshLayout.isEnabled = hasCurrentQuery
                        if(!hasCurrentQuery){
                            recyclerView.isVisible = false
                        }
                    }
                }
            }

            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    newsArticleAdapter.loadStateFlow.distinctUntilChangedBy { it.source.refresh }
                        .filter { it.source.refresh is LoadState.NotLoading }.collect {
                            if (viewModel.pendingScrollToTopAfterNewQuery) {
                                recyclerView.scrollToPosition(0)
                                viewModel.pendingScrollToTopAfterNewQuery = false
                            }
                            if (viewModel.pendingScrollToTopAfterRefresh && it.mediator?.refresh is LoadState.NotLoading) {
                                recyclerView.scrollToPosition(0)
                                viewModel.pendingScrollToTopAfterRefresh = false
                            }
                        }
                }
            }

            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    newsArticleAdapter.loadStateFlow.collect { loadState ->
                        when (val refresh = loadState.mediator?.refresh) {
                            is LoadState.Loading -> {
                                textViewError.isVisible = false
                                buttonRetry.isVisible = false
                                swipeRefreshLayout.isRefreshing = true
                                noResult.isVisible = false/* same as below for understanding lambda/
                                recyclerView.showIfOrInvisible{
                                    !viewModel.newQueryInProgress && newsArticleAdapter.itemCount > 0
                                }*/
                                recyclerView.showIfOrInvisible(fun(_: RecyclerView): Boolean {
                                    return !viewModel.newQueryInProgress && newsArticleAdapter.itemCount > 0
                                })
                                //recyclerView.isVisible = newsArticleAdapter.itemCount > 0

                                viewModel.refreshInProgress = true
                                viewModel.pendingScrollToTopAfterRefresh = true
                            }

                            is LoadState.NotLoading -> {
                                textViewError.isVisible = false
                                buttonRetry.isVisible = false
                                swipeRefreshLayout.isRefreshing = false
                                recyclerView.isVisible = newsArticleAdapter.itemCount > 0

                                val noResultsCheck =
                                    newsArticleAdapter.itemCount < 1 && loadState.append.endOfPaginationReached && loadState.source.append.endOfPaginationReached

                                noResult.isVisible = noResultsCheck

                                viewModel.refreshInProgress = false
                                viewModel.newQueryInProgress = false
                            }

                            is LoadState.Error -> {
                                swipeRefreshLayout.isRefreshing = false
                                noResult.isVisible = false
                                recyclerView.isVisible = newsArticleAdapter.itemCount > 0

                                val noCachedResult =
                                    newsArticleAdapter.itemCount < 1 && loadState.source.append.endOfPaginationReached

                                noResult.isVisible = noCachedResult
                                buttonRetry.isVisible = noCachedResult

                                val errorMessage = getString(
                                    R.string.could_not_load_search_results,
                                    refresh.error.localizedMessage
                                        ?: getString(R.string.unknown_error_occurred)
                                )

                                textViewError.text = errorMessage

                                if (viewModel.refreshInProgress) showSnackbar(errorMessage)

                                viewModel.refreshInProgress = false
                                viewModel.pendingScrollToTopAfterRefresh = false
                                viewModel.newQueryInProgress = false
                            }

                            null -> {}
                        }
                    }
                }
            }

            swipeRefreshLayout.setOnRefreshListener {
                newsArticleAdapter.refresh()
            }
            buttonRetry.setOnClickListener {
                newsArticleAdapter.retry()
            }
        }
        setHasOptionsMenu(true)
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_search_news, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as SearchView

        /* using extension function instead of this.(only using onQueryTextSubmit in it.)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                TODO("Not yet implemented")
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                TODO("Not yet implemented")
            }

        })*/

        searchView.onQueryTextSubmit { query ->
            viewModel.onSearchQuerySubmit(query)
            searchView.clearFocus()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_refresh -> {
            newsArticleAdapter.refresh()
            true
        }

        else -> {
            super.onOptionsItemSelected(item)
        }
    }

    override fun onBottomNavigationFragmentReselected() {
        binding.recyclerView.scrollToPosition(0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerView.adapter = null
        currentBinding = null
    }

}
