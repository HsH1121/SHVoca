package com.baekseok.shvoca.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [KanjiWord::class, VocabBook::class], version = 6)
abstract class KanjiDatabase : RoomDatabase() {
    abstract fun kanjiDao(): KanjiDao

    companion object {
        @Volatile
        private var INSTANCE: KanjiDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `vocab_books` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `languageType` TEXT NOT NULL
                    )
                """)
                // 기존 kanji_words 의 language 값마다 단어장 생성
                db.execSQL("""
                    INSERT INTO `vocab_books` (`name`, `languageType`)
                    SELECT DISTINCT `language`, `language` FROM `kanji_words`
                """)
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE kanji_words ADD COLUMN bookmarked INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE kanji_words ADD COLUMN variant TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE kanji_words ADD COLUMN memo TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE vocab_books ADD COLUMN hideMode TEXT NOT NULL DEFAULT 'NONE'")
            }
        }

        fun getInstance(context: Context): KanjiDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context.applicationContext, KanjiDatabase::class.java, "kanji_db")
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                INSTANCE?.kanjiDao()?.insertBook(
                                    VocabBook(name = "K-move 한자 1", languageType = "일본어")
                                )
                                INSTANCE?.kanjiDao()?.insertAll(INITIAL_WORDS)
                            }
                        }
                    })
                    .build()
                    .also { INSTANCE = it }
            }

        private val INITIAL_WORDS = listOf(
            KanjiWord(language = "K-move 한자 1", kanji = "山登り", furigana = "やまのぼり", meaning = "등산"),
            KanjiWord(language = "K-move 한자 1", kanji = "火山", furigana = "かざん", meaning = "화산"),
            KanjiWord(language = "K-move 한자 1", kanji = "戦い", furigana = "たたかい", meaning = "싸움"),
            KanjiWord(language = "K-move 한자 1", kanji = "世界", furigana = "せかい", meaning = "세계"),
            KanjiWord(language = "K-move 한자 1", kanji = "胃痛", furigana = "いつう", meaning = "위통"),
            KanjiWord(language = "K-move 한자 1", kanji = "男性", furigana = "だんせい", meaning = "남성"),
            KanjiWord(language = "K-move 한자 1", kanji = "介護", furigana = "かいご", meaning = "간호 · 돌봄"),
            KanjiWord(language = "K-move 한자 1", kanji = "水分", furigana = "すいぶん", meaning = "수분"),
            KanjiWord(language = "K-move 한자 1", kanji = "人気", furigana = "にんき", meaning = "인기"),
            KanjiWord(language = "K-move 한자 1", kanji = "介入", furigana = "かいにゅう", meaning = "개입"),
            KanjiWord(language = "K-move 한자 1", kanji = "永遠", furigana = "えいえん", meaning = "영원"),
            KanjiWord(language = "K-move 한자 1", kanji = "久しぶり", furigana = "ひさしぶり", meaning = "오랜만"),
            KanjiWord(language = "K-move 한자 1", kanji = "引き出し", furigana = "ひきだし", meaning = "서랍 · 꺼내다"),
            KanjiWord(language = "K-move 한자 1", kanji = "単なる", furigana = "たんなる", meaning = "단순한"),
            KanjiWord(language = "K-move 한자 1", kanji = "大人", furigana = "おとな", meaning = "어른"),
            KanjiWord(language = "K-move 한자 1", kanji = "田んぼ", furigana = "たんぼ", meaning = "논"),
            KanjiWord(language = "K-move 한자 1", kanji = "余り", furigana = "あまり", meaning = "나머지"),
            KanjiWord(language = "K-move 한자 1", kanji = "相性", furigana = "あいしょう", meaning = "궁합 · 상성"),
            KanjiWord(language = "K-move 한자 1", kanji = "余所見", furigana = "よそみ", meaning = "한눈팔다"),
            KanjiWord(language = "K-move 한자 1", kanji = "相場", furigana = "そうば", meaning = "시세"),
            KanjiWord(language = "K-move 한자 1", kanji = "ごみ箱", furigana = "ごみばこ", meaning = "쓰레기통"),
            KanjiWord(language = "K-move 한자 1", kanji = "禁ずる", furigana = "きんずる", meaning = "금하다"),
            KanjiWord(language = "K-move 한자 1", kanji = "禁止", furigana = "きんし", meaning = "금지"),
            KanjiWord(language = "K-move 한자 1", kanji = "味方", furigana = "みかた", meaning = "아군 · 내 편"),
            KanjiWord(language = "K-move 한자 1", kanji = "本人", furigana = "ほんにん", meaning = "본인"),
            KanjiWord(language = "K-move 한자 1", kanji = "片思い", furigana = "かたおもい", meaning = "짝사랑"),
            KanjiWord(language = "K-move 한자 1", kanji = "美男", furigana = "びなん", meaning = "미남"),
            KanjiWord(language = "K-move 한자 1", kanji = "かき氷", furigana = "かきごおり", meaning = "빙수"),
            KanjiWord(language = "K-move 한자 1", kanji = "単語", furigana = "たんご", meaning = "단어"),
            KanjiWord(language = "K-move 한자 1", kanji = "水泳", furigana = "すいえい", meaning = "수영"),
            KanjiWord(language = "K-move 한자 1", kanji = "水着", furigana = "みずぎ", meaning = "수영복"),
            KanjiWord(language = "K-move 한자 1", kanji = "出口", furigana = "でぐち", meaning = "출구"),
            KanjiWord(language = "K-move 한자 1", kanji = "入国", furigana = "にゅうこく", meaning = "입국"),
            KanjiWord(language = "K-move 한자 1", kanji = "思い出", furigana = "おもいで", meaning = "추억"),
            KanjiWord(language = "K-move 한자 1", kanji = "天の川", furigana = "あまのがわ", meaning = "은하수"),
            KanjiWord(language = "K-move 한자 1", kanji = "工夫", furigana = "くふう", meaning = "궁리 · 고안"),
            KanjiWord(language = "K-move 한자 1", kanji = "畑", furigana = "はたけ", meaning = "밭"),
            KanjiWord(language = "K-move 한자 1", kanji = "株価", furigana = "かぶか", meaning = "주가"),
            KanjiWord(language = "K-move 한자 1", kanji = "取り除く", furigana = "とりのぞく", meaning = "제거하다"),
            KanjiWord(language = "K-move 한자 1", kanji = "結果", furigana = "けっか", meaning = "결과"),
            KanjiWord(language = "K-move 한자 1", kanji = "愛想", furigana = "あいそ", meaning = "애상 · 붙임성"),
            KanjiWord(language = "K-move 한자 1", kanji = "株式", furigana = "かぶしき", meaning = "주식"),
            KanjiWord(language = "K-move 한자 1", kanji = "学歴", furigana = "がくれき", meaning = "학력"),
            KanjiWord(language = "K-move 한자 1", kanji = "無茶", furigana = "むちゃ", meaning = "터무니없음"),
            KanjiWord(language = "K-move 한자 1", kanji = "未決", furigana = "みけつ", meaning = "미결"),
            KanjiWord(language = "K-move 한자 1", kanji = "日課", furigana = "にっか", meaning = "일과"),
            KanjiWord(language = "K-move 한자 1", kanji = "姉妹", furigana = "しまい", meaning = "자매"),
            KanjiWord(language = "K-move 한자 1", kanji = "見本", furigana = "みほん", meaning = "견본"),
            KanjiWord(language = "K-move 한자 1", kanji = "月末", furigana = "げつまつ", meaning = "월말"),
            KanjiWord(language = "K-move 한자 1", kanji = "個人", furigana = "こじん", meaning = "개인"),
            KanjiWord(language = "K-move 한자 1", kanji = "人件費", furigana = "じんけんひ", meaning = "인건비"),
            KanjiWord(language = "K-move 한자 1", kanji = "保険", furigana = "ほけん", meaning = "보험"),
            KanjiWord(language = "K-move 한자 1", kanji = "在日", furigana = "ざいにち", meaning = "재일 (일본에 있음)"),
            KanjiWord(language = "K-move 한자 1", kanji = "ご存じ", furigana = "ごぞんじ", meaning = "아시다 (존경)"),
            KanjiWord(language = "K-move 한자 1", kanji = "作成", furigana = "さくせい", meaning = "작성"),
            KanjiWord(language = "K-move 한자 1", kanji = "小児科", furigana = "しょうにか", meaning = "소아과"),
            KanjiWord(language = "K-move 한자 1", kanji = "手元", furigana = "てもと", meaning = "수중 · 곁에 있음"),
            KanjiWord(language = "K-move 한자 1", kanji = "完全", furigana = "かんぜん", meaning = "완전"),
            KanjiWord(language = "K-move 한자 1", kanji = "病院", furigana = "びょういん", meaning = "병원"),
            KanjiWord(language = "K-move 한자 1", kanji = "税金", furigana = "ぜいきん", meaning = "세금"),
            KanjiWord(language = "K-move 한자 1", kanji = "説得", furigana = "せっとく", meaning = "설득"),
            KanjiWord(language = "K-move 한자 1", kanji = "兄弟", furigana = "きょうだい", meaning = "형제"),
            KanjiWord(language = "K-move 한자 1", kanji = "競争", furigana = "きょうそう", meaning = "경쟁"),
            KanjiWord(language = "K-move 한자 1", kanji = "お祝い", furigana = "おいわい", meaning = "축하"),
            KanjiWord(language = "K-move 한자 1", kanji = "今にも", furigana = "いまにも", meaning = "금세 · 당장이라도"),
            KanjiWord(language = "K-move 한자 1", kanji = "貧乏", furigana = "びんぼう", meaning = "가난"),
            KanjiWord(language = "K-move 한자 1", kanji = "残念", furigana = "ざんねん", meaning = "유감"),
            KanjiWord(language = "K-move 한자 1", kanji = "合理", furigana = "ごうり", meaning = "합리"),
            KanjiWord(language = "K-move 한자 1", kanji = "給食", furigana = "きゅうしょく", meaning = "급식"),
            KanjiWord(language = "K-move 한자 1", kanji = "回答", furigana = "かいとう", meaning = "회답 · 응답"),
            KanjiWord(language = "K-move 한자 1", kanji = "検討", furigana = "けんとう", meaning = "검토"),
            KanjiWord(language = "K-move 한자 1", kanji = "経験", furigana = "けいけん", meaning = "경험"),
            KanjiWord(language = "K-move 한자 1", kanji = "一休み", furigana = "ひとやすみ", meaning = "잠깐 쉼"),
            KanjiWord(language = "K-move 한자 1", kanji = "名札", furigana = "なふだ", meaning = "명찰"),
            KanjiWord(language = "K-move 한자 1", kanji = "夜明け", furigana = "よあけ", meaning = "새벽"),
            KanjiWord(language = "K-move 한자 1", kanji = "今更", furigana = "いまさら", meaning = "새삼스럽게"),
            KanjiWord(language = "K-move 한자 1", kanji = "多数", furigana = "たすう", meaning = "다수"),
            KanjiWord(language = "K-move 한자 1", kanji = "郵便", furigana = "ゆうびん", meaning = "우편"),
            KanjiWord(language = "K-move 한자 1", kanji = "後回し", furigana = "あとまわし", meaning = "뒤로 미룸"),
            KanjiWord(language = "K-move 한자 1", kanji = "品物", furigana = "しなもの", meaning = "물건"),
            KanjiWord(language = "K-move 한자 1", kanji = "区切り", furigana = "くぎり", meaning = "구분"),
            KanjiWord(language = "K-move 한자 1", kanji = "器", furigana = "うつわ", meaning = "그릇"),
            KanjiWord(language = "K-move 한자 1", kanji = "原因", furigana = "げんいん", meaning = "원인"),
            KanjiWord(language = "K-move 한자 1", kanji = "恩返し", furigana = "おんがえし", meaning = "은혜 갚기"),
            KanjiWord(language = "K-move 한자 1", kanji = "故に", furigana = "ゆえに", meaning = "그런 까닭으로"),
            KanjiWord(language = "K-move 한자 1", kanji = "固定", furigana = "こてい", meaning = "고정"),
            KanjiWord(language = "K-move 한자 1", kanji = "胡麻", furigana = "ごま", meaning = "참깨 (胡麻すり=아첨)"),
            KanjiWord(language = "K-move 한자 1", kanji = "湖", furigana = "みずうみ", meaning = "호수"),
            KanjiWord(language = "K-move 한자 1", kanji = "苦手", furigana = "にがて", meaning = "서툼 · 못함 · 거북함"),
            KanjiWord(language = "K-move 한자 1", kanji = "苦情", furigana = "くじょう", meaning = "불평"),
            KanjiWord(language = "K-move 한자 1", kanji = "若者", furigana = "わかもの", meaning = "젊은이"),
            KanjiWord(language = "K-move 한자 1", kanji = "格別に", furigana = "かくべつに", meaning = "각별히"),
            KanjiWord(language = "K-move 한자 1", kanji = "家路", furigana = "いえじ", meaning = "귀갓길"),
            KanjiWord(language = "K-move 한자 1", kanji = "当日", furigana = "とうじつ", meaning = "당일"),
            KanjiWord(language = "K-move 한자 1", kanji = "先日", furigana = "せんじつ", meaning = "지난번에"),
            KanjiWord(language = "K-move 한자 1", kanji = "日付", furigana = "ひづけ", meaning = "날짜"),
            KanjiWord(language = "K-move 한자 1", kanji = "お使い", furigana = "おつかい", meaning = "심부름"),
            KanjiWord(language = "K-move 한자 1", kanji = "仲介", furigana = "ちゅうかい", meaning = "중개"),
            KanjiWord(language = "K-move 한자 1", kanji = "充実", furigana = "じゅうじつ", meaning = "충실 (가득 참 · 알참)"),
            KanjiWord(language = "K-move 한자 1", kanji = "洒落", furigana = "しゃれ", meaning = "익살 · 농담 (だじゃれ=썰렁 개그)")
        )
    }
}
