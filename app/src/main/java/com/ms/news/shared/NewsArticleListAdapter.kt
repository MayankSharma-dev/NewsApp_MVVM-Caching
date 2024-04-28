package com.ms.news.shared

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import com.ms.news.data.NewsArticle
import com.ms.news.databinding.ItemLayoutBinding

class NewsArticleListAdapter(
    private val onItemClick: (NewsArticle) -> Unit,
private val onBookmarkClick: (NewsArticle) -> Unit
) :
    ListAdapter<NewsArticle, NewsArticleViewHolder>(NewsArticleComparator()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsArticleViewHolder {
        val binding = ItemLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NewsArticleViewHolder(binding,
            onItemClick = { position ->
                val articles = getItem(position)
                if (articles != null) {
                    onItemClick(articles)
                }
            },
            onBookmarkClick = { position ->
                val articles = getItem(position)
                if (articles != null) {
                    onBookmarkClick(articles)
                }
            })
    }

    override fun onBindViewHolder(holder: NewsArticleViewHolder, position: Int) {
        val currentItem = getItem(position)
        if (currentItem != null) {
            holder.bind(currentItem)
        }
    }
}