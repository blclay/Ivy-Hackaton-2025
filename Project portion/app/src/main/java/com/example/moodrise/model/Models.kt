package com.example.moodrise.model

enum class Category {
    EDUCATE,
    LAUGH,
    MOTIVATE
}

data class ContentItem(
    val id: String,
    val category: Category,
    val type: String,       // "text" or "video"
    val title: String,
    val body: String? = null,
    val link: String? = null,
    val mediaUri: String? = null
)
