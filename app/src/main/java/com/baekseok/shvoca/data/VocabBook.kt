package com.baekseok.shvoca.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vocab_books")
data class VocabBook(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val languageType: String,
    val hideMode: String = "NONE"
)

data class BookSummary(
    val id: Int,
    val name: String,
    val languageType: String,
    val count: Int
)
