package com.ms.news.shared

import androidx.recyclerview.widget.DiffUtil
import com.ms.news.data.NewsArticle

class NewsArticleComparator: DiffUtil.ItemCallback<NewsArticle>() {

    override fun areItemsTheSame(oldItem: NewsArticle, newItem: NewsArticle): Boolean {
        return  oldItem.url == newItem.url
    }

    override fun areContentsTheSame(oldItem: NewsArticle, newItem: NewsArticle): Boolean {
        return oldItem == newItem
    }
}