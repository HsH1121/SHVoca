package com.baekseok.shvoca.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "kanji_words")
data class KanjiWord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val language: String,
    val kanji: String,
    val furigana: String,
    val meaning: String,
    val bookmarked: Boolean = false,
    val variant: String = "",
    val memo: String = ""
)

data class LanguageSummary(val language: String, val count: Int)
