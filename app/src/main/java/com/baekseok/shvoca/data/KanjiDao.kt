package com.baekseok.shvoca.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
abstract class KanjiDao {

    // ── 기존 단어 쿼리 ────────────────────────────────────────────────────────

    @Query("SELECT language, COUNT(*) as count FROM kanji_words GROUP BY language ORDER BY language ASC")
    abstract fun getLanguageSummaries(): Flow<List<LanguageSummary>>

    @Query("SELECT * FROM kanji_words WHERE language = :language ORDER BY id ASC")
    abstract fun getByLanguage(language: String): Flow<List<KanjiWord>>

    @Query("SELECT * FROM vocab_books WHERE name = :name LIMIT 1")
    abstract fun getBookByName(name: String): Flow<VocabBook?>

    @Query("UPDATE vocab_books SET hideMode = :hideMode WHERE name = :name")
    abstract suspend fun setHideMode(name: String, hideMode: String)

    @Insert
    abstract suspend fun insertAll(words: List<KanjiWord>)

    @Query("UPDATE kanji_words SET bookmarked = :bookmarked WHERE id = :id")
    abstract suspend fun setBookmarked(id: Int, bookmarked: Boolean)

    @Query("UPDATE kanji_words SET memo = :memo WHERE id = :id")
    abstract suspend fun setMemo(id: Int, memo: String)

    // ── 단어장(VocabBook) ────────────────────────────────────────────────────

    @Query("""
        SELECT vb.id, vb.name, vb.languageType, COUNT(kw.id) AS count
        FROM vocab_books vb
        LEFT JOIN kanji_words kw ON kw.language = vb.name
        GROUP BY vb.id
        ORDER BY vb.id ASC
    """)
    abstract fun getBookSummaries(): Flow<List<BookSummary>>

    @Insert
    abstract suspend fun insertBook(book: VocabBook)

    @Update
    abstract suspend fun updateBook(book: VocabBook)

    @Delete
    abstract suspend fun deleteBook(book: VocabBook)

    @Query("UPDATE kanji_words SET language = :newName WHERE language = :oldName")
    abstract suspend fun renameWordsBook(oldName: String, newName: String)

    @Query("DELETE FROM kanji_words WHERE language = :bookName")
    abstract suspend fun deleteWordsByBook(bookName: String)

    // ── 트랜잭션 ─────────────────────────────────────────────────────────────

    @Transaction
    open suspend fun renameBook(oldName: String, book: VocabBook) {
        renameWordsBook(oldName, book.name)
        updateBook(book)
    }

    @Transaction
    open suspend fun deleteBookWithWords(book: VocabBook) {
        deleteWordsByBook(book.name)
        deleteBook(book)
    }
}
