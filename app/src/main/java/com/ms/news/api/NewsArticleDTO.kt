package com.ms.news.api

// DTO stands for Data Transfer Object just a name.
data class NewsArticleDTO(
    val title: String?,
    val url: String,
    val urlToImage: String?
)