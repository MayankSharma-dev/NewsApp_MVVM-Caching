package com.ms.news.features.bookmarks

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.ms.news.MainActivity
import com.ms.news.R
import com.ms.news.databinding.FragmentBookmarksBinding
import com.ms.news.shared.NewsArticleListAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BookmarksFragment : Fragment(R.layout.fragment_bookmarks),
    MainActivity.OnBottomNavigationFragmentReselectedListener {

    private val viewModel: BookmarksVIewModel by viewModels()

    private var currentBinding: FragmentBookmarksBinding? = null
    private val binding get() = currentBinding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        currentBinding = FragmentBookmarksBinding.bind(view)

        val bookmarksAdapter = NewsArticleListAdapter(onItemClick = { article ->
            val uri = Uri.parse(article.url)
            val intent = Intent(Intent.ACTION_VIEW, uri)
            requireActivity().startActivity(intent)
        }, onBookmarkClick = { article ->
            viewModel.onBookmarkClick(article)
        })

        bookmarksAdapter.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY


        binding.apply {
            recyclerView.apply {
                adapter = bookmarksAdapter
                setHasFixedSize(true)
            }

            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.bookmarks.collect {
                        val bookmarks = it ?: return@collect


                        bookmarksAdapter.submitList(bookmarks)
                        noBookmarks.isVisible = bookmarks.isEmpty()
                        recyclerView.isVisible = bookmarks.isNotEmpty()
                    }
                }
            }
        }
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_bookmarks, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_delete_all_bookmarks -> {
            viewModel.onDeleteAllBookmarks()
            true
        }

        else -> super.onOptionsItemSelected(item)
    }

    override fun onBottomNavigationFragmentReselected() {
        binding.recyclerView.scrollToPosition(0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        currentBinding = null
    }
}