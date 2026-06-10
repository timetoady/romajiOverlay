package com.example.romajioverlay.nlp

import com.atilika.kuromoji.ipadic.Token

object RomajiTranslator {

    // Maps two-character Katakana combinations to Romaji
    private val combinationMap = mapOf(
        "キャ" to "kya", "キュ" to "kyu", "キョ" to "kyo",
        "シャ" to "sha", "シュ" to "shu", "ショ" to "sho",
        "チャ" to "cha", "チュ" to "chu", "チョ" to "cho",
        "ニャ" to "nya", "ニュ" to "nyu", "ニョ" to "nyo",
        "ヒャ" to "hya", "ヒュ" to "hyu", "ヒョ" to "hyo",
        "ミャ" to "mya", "ミュ" to "myu", "ミョ" to "myo",
        "リャ" to "rya", "リュ" to "ryu", "リョ" to "ryo",
        "ギャ" to "gya", "ギュ" to "gyu", "ギョ" to "gyo",
        "ジャ" to "ja", "ジュ" to "ju", "ジョ" to "jo",
        "ヂャ" to "ja", "ヂュ" to "ju", "ヂョ" to "jo",
        "ビャ" to "bya", "ビュ" to "byu", "ビョ" to "byo",
        "ピャ" to "pya", "ピュ" to "pyu", "ピョ" to "pyo",
        "ファ" to "fa", "フィ" to "fi", "フェ" to "fe", "フォ" to "fo", "フュ" to "fyu",
        "ウィ" to "wi", "ウェ" to "we", "ウォ" to "wo",
        "ヴァ" to "va", "ヴィ" to "vi", "ヴ" to "vu", "ヴェ" to "ve", "ヴォ" to "vo",
        "ツァ" to "tsa", "ツィ" to "tsi", "ツェ" to "tse", "ツォ" to "tso",
        "チェ" to "che", "シェ" to "she", "ジェ" to "je"
    )

    // Maps single Katakana characters to Romaji
    private val singleMap = mapOf(
        "ア" to "a", "イ" to "i", "ウ" to "u", "エ" to "e", "オ" to "o",
        "カ" to "ka", "キ" to "ki", "ク" to "ku", "ケ" to "ke", "コ" to "ko",
        "サ" to "sa", "シ" to "shi", "ス" to "su", "セ" to "se", "ソ" to "so",
        "タ" to "ta", "チ" to "chi", "ツ" to "tsu", "テ" to "te", "ト" to "to",
        "ナ" to "na", "ニ" to "ni", "ヌ" to "nu", "ネ" to "ne", "ノ" to "no",
        "ハ" to "ha", "ヒ" to "hi", "フ" to "fu", "ヘ" to "he", "ホ" to "ho",
        "マ" to "ma", "ミ" to "mi", "ム" to "mu", "メ" to "me", "モ" to "mo",
        "ヤ" to "ya", "ユ" to "yu", "ヨ" to "yo",
        "ラ" to "ra", "リ" to "ri", "ル" to "ru", "レ" to "re", "ロ" to "ro",
        "ワ" to "wa", "ヲ" to "o", "ン" to "n",
        "ガ" to "ga", "ギ" to "gi", "グ" to "gu", "ゲ" to "ge", "ゴ" to "go",
        "ザ" to "za", "ジ" to "ji", "ズ" to "zu", "ゼ" to "ze", "ゾ" to "zo",
        "ダ" to "da", "ヂ" to "ji", "ヅ" to "zu", "デ" to "de", "ド" to "do",
        "バ" to "ba", "ビ" to "bi", "ブ" to "bu", "ベ" to "be", "ボ" to "bo",
        "パ" to "pa", "ピ" to "pi", "プ" to "pu", "ペ" to "pa", "ポ" to "po"
    )

    // Japanese vowels for long vowel checks
    private val vowels = setOf('a', 'i', 'u', 'e', 'o', 'ā', 'ī', 'ū', 'ē', 'ō')

    /**
     * Converts a string of Hiragana characters into Katakana.
     */
    fun hiraganaToKatakana(text: String): String {
        val sb = StringBuilder()
        for (char in text) {
            if (char in '\u3041'..'\u3096') {
                sb.append((char.code + 0x60).toChar())
            } else {
                sb.append(char)
            }
        }
        return sb.toString()
    }

    /**
     * Converts a single Katakana word/token reading into Modified Hepburn Romaji.
     */
    fun katakanaToRomaji(katakana: String): String {
        if (katakana.isEmpty() || katakana == "*") return ""

        val normalized = hiraganaToKatakana(katakana)
        val sb = StringBuilder()
        var i = 0
        var doubleConsonant = false

        while (i < normalized.length) {
            val char = normalized[i]

            // Handle sokuon (ッ / ッ)
            if (char == 'ッ' || char == 'ッ' || char == 'っ') {
                doubleConsonant = true
                i++
                continue
            }

            // Handle long vowel chouonpu (ー)
            if (char == 'ー') {
                if (sb.isNotEmpty()) {
                    val prevCharIndex = sb.length - 1
                    val prevChar = sb[prevCharIndex]
                    val replacement = when (prevChar) {
                        'a' -> 'ā'
                        'i' -> 'ī' // Modified Hepburn represents long i as "ī"
                        'u' -> 'ū'
                        'e' -> 'ē'
                        'o' -> 'ō'
                        else -> null
                    }
                    if (replacement != null) {
                        sb.setCharAt(prevCharIndex, replacement)
                    } else {
                        sb.append('ー')
                    }
                } else {
                    sb.append('ー')
                }
                i++
                continue
            }

            // Try combination mapping (2 characters)
            if (i + 1 < normalized.length) {
                val pair = normalized.substring(i, i + 2)
                val romaji = combinationMap[pair]
                if (romaji != null) {
                    var syllable = romaji
                    if (doubleConsonant) {
                        syllable = if (syllable.startsWith("ch")) {
                            "t$syllable"
                        } else {
                            "${syllable[0]}$syllable"
                        }
                        doubleConsonant = false
                    }
                    sb.append(syllable)
                    i += 2
                    continue
                }
            }

            // Fallback to single character mapping (1 character)
            val romaji = singleMap[char.toString()]
            if (romaji != null) {
                var merged = false
                if (sb.isNotEmpty()) {
                    val prevCharIndex = sb.length - 1
                    val prevChar = sb[prevCharIndex]
                    if (char == 'ア' && prevChar == 'a') {
                        sb.setCharAt(prevCharIndex, 'ā')
                        merged = true
                    } else if (char == 'イ' && prevChar == 'i') {
                        sb.setCharAt(prevCharIndex, 'ī')
                        merged = true
                    } else if (char == 'ウ' && (prevChar == 'u' || prevChar == 'o')) {
                        sb.setCharAt(prevCharIndex, if (prevChar == 'u') 'ū' else 'ō')
                        merged = true
                    } else if (char == 'エ' && prevChar == 'e') {
                        sb.setCharAt(prevCharIndex, 'ē')
                        merged = true
                    } else if (char == 'オ' && prevChar == 'o') {
                        sb.setCharAt(prevCharIndex, 'ō')
                        merged = true
                    }
                }

                if (merged) {
                    i++
                    continue
                }

                var syllable = romaji

                // Syllabic 'n' (ン) checks
                if (char == 'ン' || char == 'ン' || char == 'ん') {
                    if (i + 1 < normalized.length) {
                        val nextChar = normalized[i + 1]
                        val nextRomaji = singleMap[nextChar.toString()] ?: combinationMap[if (i + 2 <= normalized.length) normalized.substring(i + 1, i + 2) else ""]
                        if (nextRomaji != null && (nextRomaji.startsWith("a") || nextRomaji.startsWith("i") || nextRomaji.startsWith("u") || nextRomaji.startsWith("e") || nextRomaji.startsWith("o") || nextRomaji.startsWith("y"))) {
                            syllable = "n'"
                        }
                    }
                }

                if (doubleConsonant) {
                    syllable = if (syllable.startsWith("ch")) {
                        "t$syllable"
                    } else {
                        "${syllable[0]}$syllable"
                    }
                    doubleConsonant = false
                }
                sb.append(syllable)
                i++
            } else {
                // Keep unrecognized characters (punctuation, numbers, English) as-is
                if (doubleConsonant) {
                    sb.append('ッ')
                    doubleConsonant = false
                }
                sb.append(char)
                i++
            }
        }

        // Edge case: dangling sokuon at the end
        if (doubleConsonant) {
            sb.append('ッ')
        }

        return sb.toString()
    }

    /**
     * Helper to determine if a token is a punctuation or special symbol.
     */
    private fun isPunctuation(token: Token): Boolean {
        val pos1 = token.partOfSpeechLevel1
        val surface = token.surface
        return pos1 == "記号" || surface.matches(Regex("[\\p{Punct}\\s　、。！？•・…]+"))
    }

    /**
     * Translates a list of Kuromoji tokens into formatted Romaji text,
     * applying particle pronunciation rules and grammar-aware word spacing.
     */
    fun translateTokens(tokens: List<Token>): String {
        if (tokens.isEmpty()) return ""

        // 1. Group adjacent tokens that should not be separated by spaces
        class TokenGroup {
            val tokens = mutableListOf<Token>()
        }

        val groups = mutableListOf<TokenGroup>()
        var currentGroup = TokenGroup()
        groups.add(currentGroup)

        for (idx in tokens.indices) {
            val token = tokens[idx]
            val pos1 = token.partOfSpeechLevel1
            val surface = token.surface

            // Determine if a space is needed before this token
            val needsSpace = if (idx == 0) {
                false
            } else {
                val prevToken = tokens[idx - 1]
                val prevPos1 = prevToken.partOfSpeechLevel1

                val isCurrentSuffix = pos1 == "接尾辞" || token.partOfSpeechLevel2 == "接尾"
                val isCurrentAuxVerb = pos1 == "助動詞" && surface != "です" && surface != "だ"
                val isPrevPrefix = prevPos1 == "接頭詞"
                val isCurrentPunctuation = isPunctuation(token)
                val isPrevPunctuation = isPunctuation(prevToken)

                !(isCurrentSuffix || isCurrentAuxVerb || isPrevPrefix || isCurrentPunctuation || isPrevPunctuation)
            }

            if (needsSpace && currentGroup.tokens.isNotEmpty()) {
                currentGroup = TokenGroup()
                groups.add(currentGroup)
            }

            currentGroup.tokens.add(token)
        }

        // 2. Translate each group as a single consolidated unit
        val sb = StringBuilder()
        for (group in groups) {
            if (group.tokens.isEmpty()) continue

            var groupRomaji = ""

            // Handle particle override only if the group is a single particle token
            if (group.tokens.size == 1) {
                val singleToken = group.tokens[0]
                if (singleToken.partOfSpeechLevel1 == "助詞") {
                    groupRomaji = when (singleToken.surface) {
                        "は" -> "wa"
                        "へ" -> "e"
                        "を" -> "o"
                        else -> {
                            var reading = singleToken.reading
                            if (reading == null || reading == "*") {
                                reading = hiraganaToKatakana(singleToken.surface)
                            }
                            katakanaToRomaji(reading)
                        }
                    }
                }
            }

            if (groupRomaji.isEmpty()) {
                // Concatenate readings of all tokens in this group
                val groupReading = StringBuilder()
                for (token in group.tokens) {
                    var reading = token.reading
                    if (reading == null || reading == "*") {
                        reading = hiraganaToKatakana(token.surface)
                    }
                    groupReading.append(reading)
                }
                groupRomaji = katakanaToRomaji(groupReading.toString())
            }

            if (groupRomaji.isEmpty()) {
                // Fallback to surface text if translation is empty (e.g. English, digits)
                val surfaceText = group.tokens.joinToString("") { it.surface }
                if (surfaceText.matches(Regex("[a-zA-Z0-9]+"))) {
                    groupRomaji = surfaceText
                }
            }

            if (groupRomaji.isNotEmpty()) {
                if (sb.isNotEmpty()) {
                    sb.append(" ")
                }
                sb.append(groupRomaji)
            }
        }

        return sb.toString().trim()
    }
}
