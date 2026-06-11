package com.example.romajioverlay

import com.atilika.kuromoji.ipadic.Tokenizer
import com.example.romajioverlay.nlp.RomajiTranslator
import com.example.romajioverlay.utils.TextCleanupUtils
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Test

class RomajiTranslatorTest {

    companion object {
        private lateinit var tokenizer: Tokenizer

        @JvmStatic
        @BeforeClass
        fun setUp() {
            // Instantiate the Kuromoji tokenizer once for all tests
            tokenizer = Tokenizer.Builder().build()
        }
    }

    @Test
    fun testBasicKatakanaTranslation() {
        assertEquals("a", RomajiTranslator.katakanaToRomaji("ア"))
        assertEquals("ka", RomajiTranslator.katakanaToRomaji("カ"))
        assertEquals("sushi", RomajiTranslator.katakanaToRomaji("スシ"))
        assertEquals("teriyaki", RomajiTranslator.katakanaToRomaji("テリヤキ"))
    }

    @Test
    fun testSokuonDoubleConsonants() {
        // Double consonant: ppa, tte
        assertEquals("kappu", RomajiTranslator.katakanaToRomaji("カップ"))
        
        // Double consonant before "ch" should double as "t" (e.g., matchi, not macchi)
        assertEquals("matchi", RomajiTranslator.katakanaToRomaji("マッチ"))
        
        // Hiragana sokuon conversion check
        assertEquals("itte", RomajiTranslator.katakanaToRomaji("いって"))
    }

    @Test
    fun testChouonpuLongVowels() {
        // Chouonpu mappings
        assertEquals("shō", RomajiTranslator.katakanaToRomaji("ショー"))
        assertEquals("sūpā", RomajiTranslator.katakanaToRomaji("スーパー"))
        assertEquals("takushī", RomajiTranslator.katakanaToRomaji("タクシー"))
        assertEquals("kēki", RomajiTranslator.katakanaToRomaji("ケーキ"))
    }

    @Test
    fun testSyllabicN() {
        // Syllabic n (ン) mapping
        assertEquals("pan", RomajiTranslator.katakanaToRomaji("パン"))
        
        // Followed by vowel or 'y' requires an apostrophe (e.g. n'y)
        assertEquals("kin'yū", RomajiTranslator.katakanaToRomaji("キンユウ"))
        assertEquals("ren'ai", RomajiTranslator.katakanaToRomaji("レンアイ"))
    }

    @Test
    fun testSentenceTokenizerAndSpacing() {
        // 1. Particle "は" (topic marker) -> "wa", and spacing
        val tokens1 = tokenizer.tokenize("私は学生です")
        val result1 = RomajiTranslator.translateTokens(tokens1)
        assertEquals("watashi wa gakusei desu", result1)

        // 2. Particle "を" (object marker) -> "o"
        val tokens2 = tokenizer.tokenize("お酒を飲みます")
        val result2 = RomajiTranslator.translateTokens(tokens2)
        assertEquals("osake o nomimasu", result2)

        // 3. Direction particle "へ" -> "e", verb grouping, and long vowel "ō"
        val tokens3 = tokenizer.tokenize("東京へ行きます")
        val result3 = RomajiTranslator.translateTokens(tokens3)
        assertEquals("tōkyō e ikimasu", result3)

        // 4. Locative particle "に", prefix "お" (no space before "mise"), and auxiliary verb "masu" (no space)
        val tokens4 = tokenizer.tokenize("お店に行きます")
        val result4 = RomajiTranslator.translateTokens(tokens4)
        assertEquals("omise ni ikimasu", result4)
    }

    @Test
    fun testSokuonContractionsInSentences() {
        val tokens1 = tokenizer.tokenize("作った")
        val result1 = RomajiTranslator.translateTokens(tokens1)
        assertEquals("tsukutta", result1)

        val tokens2 = tokenizer.tokenize("そっか")
        val result2 = RomajiTranslator.translateTokens(tokens2)
        assertEquals("sokka", result2)

        val tokens3 = tokenizer.tokenize("合ってる")
        val result3 = RomajiTranslator.translateTokens(tokens3)
        assertEquals("atteru", result3)
    }

    @Test
    fun testMessengerTextCleanup() {
        // Case 1: Standard incoming message with sender name and double tap instructions
        val raw1 = "Traci, なんでバスケットの形にしたんかね, double tap to see sent/receive date and time, double tap and hold to react on message"
        assertEquals("なんでバスケットの形にしたんかね", TextCleanupUtils.cleanMessengerText(raw1))

        // Case 2: Outgoing message starting with "You"
        val raw2 = "You, これ、テストです。, double tap to see details"
        assertEquals("これ、テストです。", TextCleanupUtils.cleanMessengerText(raw2))

        // Case 3: No comma in prefix
        val raw3 = "Traci なんでバスケットの形にしたんかね, double tap to see details"
        assertEquals("なんでバスケットの形にしたんかね", TextCleanupUtils.cleanMessengerText(raw3))

        // Case 4: No Japanese at all (should return original string, though service filters this first)
        val raw4 = "Hello, this is just a test"
        assertEquals("Hello, this is just a test", TextCleanupUtils.cleanMessengerText(raw4))

        // Case 5: Japanese with English words inside the message (should keep the English words inside)
        val raw5 = "You, Googleで検索して, double tap to see details"
        assertEquals("Googleで検索して", TextCleanupUtils.cleanMessengerText(raw5))
    }
}

