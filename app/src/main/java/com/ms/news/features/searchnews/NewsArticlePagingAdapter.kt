package com.ms.news.features.searchnews

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import com.ms.news.data.NewsArticle
import com.ms.news.databinding.ItemLayoutBinding
import com.ms.news.shared.NewsArticleComparator
import com.ms.news.shared.NewsArticleViewHolder

class NewsArticlePagingAdapter(
    private val onItemClick: (NewsArticle) -> Unit,
private val onBookmarkClick: (NewsArticle) -> Unit
) : PagingDataAdapter<NewsArticle, NewsArticleViewHolder>(NewsArticleComparator()) {
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