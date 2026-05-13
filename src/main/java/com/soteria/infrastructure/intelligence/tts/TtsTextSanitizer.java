package com.soteria.infrastructure.intelligence.tts;

import com.soteria.infrastructure.intelligence.system.LanguageUtils;

import java.text.Normalizer;

/**
 * Sanitizes text before it is passed to the Kokoro/Sherpa-ONNX engine.
 *
 * <p>Kokoro throws native C++ exceptions on certain Unicode inputs — most
 * notably when Latin romanization or Greek characters are mixed into a
 * Chinese or Japanese text fragment. This class applies script-level
 * filtering based on the active engine language to keep input conservative.</p>
 *
 * <p>Three filtering modes are used:
 * <ul>
 *   <li>{@code PERMISSIVE} — only strips control characters and enforces the
 *       length cap; used for all Latin-script languages.</li>
 *   <li>{@code JA} — keeps kana, Han, and basic ASCII Latin; strips everything
 *       else to prevent ONNX crashes on mixed-script input.</li>
 *   <li>{@code ZH_HANZI_ONLY} — keeps only Han characters, digits, and
 *       punctuation. Activated for {@code zh} <em>and</em> whenever the text
 *       already contains Han regardless of the language hint, because the
 *       engine crashes on mixed garbage even with a wrong hint.</li>
 * </ul>
 */
final class TtsTextSanitizer {

    private static final int MAX_CODE_POINTS = 480;

    private enum Mode {
        PERMISSIVE,
        JA,
        ZH_HANZI_ONLY
    }

    private TtsTextSanitizer() { }

    static String sanitize(String s) {
        return sanitize(s, null);
    }

    static String sanitize(String s, String languageHint) {
        if (s == null || s.isEmpty()) return "";

        String n = Normalizer.normalize(s.trim(), Normalizer.Form.NFC);
        String lang = (languageHint != null && !languageHint.isBlank())
                ? LanguageUtils.isoCode(languageHint)
                : "";

        Mode mode = resolveMode(lang, n);

        StringBuilder b = new StringBuilder(Math.min(n.length(), MAX_CODE_POINTS + 16));
        int count = 0;
        int i = 0;
        while (i < n.length() && count < MAX_CODE_POINTS) {
            int cp = n.codePointAt(i);
            i += Character.charCount(cp);
            if (shouldKeepCodePoint(cp, mode)) {
                b.appendCodePoint(cp);
                count++;
            }
        }
        return b.toString().trim();
    }

    private static Mode resolveMode(String langIso, String normalizedText) {
        if ("ja".equals(langIso)) return Mode.JA;
        if ("zh".equals(langIso) || containsHan(normalizedText)) return Mode.ZH_HANZI_ONLY;
        return Mode.PERMISSIVE;
    }

    private static boolean containsHan(String s) {
        int i = 0;
        while (i < s.length()) {
            int cp = s.codePointAt(i);
            i += Character.charCount(cp);
            if (Character.UnicodeScript.of(cp) == Character.UnicodeScript.HAN) return true;
        }
        return false;
    }

    private static boolean shouldKeepCodePoint(int cp, Mode mode) {
        if (cp == 0xFFFD) return false;
        int t = Character.getType(cp);
        if (t == Character.CONTROL && cp != '\n' && cp != '\r' && cp != '\t') return false;
        return mode == Mode.PERMISSIVE || isAllowed(cp, t, mode);
    }

    private static boolean isAllowed(int cp, int type, Mode mode) {
        if (isGreekOrGreekExtended(cp)) return false;
        if (isCombiningMark(type)) return true;
        if (Character.isWhitespace(cp) || Character.isDigit(cp)) return true;
        if (type == Character.LETTER_NUMBER) return true;
        if (Character.isLetter(cp)) {
            Character.UnicodeScript sc = Character.UnicodeScript.of(cp);
            return switch (mode) {
                case JA -> sc == Character.UnicodeScript.HIRAGANA
                        || sc == Character.UnicodeScript.KATAKANA
                        || sc == Character.UnicodeScript.HAN
                        || (sc == Character.UnicodeScript.LATIN && isBasicLatinLetter(cp));
                case ZH_HANZI_ONLY -> sc == Character.UnicodeScript.HAN;
                default -> true;
            };
        }
        return isPunctuationCategory(type);
    }

    private static boolean isBasicLatinLetter(int cp) {
        return (cp >= 'A' && cp <= 'Z') || (cp >= 'a' && cp <= 'z');
    }

    private static boolean isGreekOrGreekExtended(int cp) {
        return (cp >= 0x0370 && cp <= 0x03FF) || (cp >= 0x1F00 && cp <= 0x1FFF);
    }

    private static boolean isCombiningMark(int type) {
        return type == Character.NON_SPACING_MARK
                || type == Character.COMBINING_SPACING_MARK
                || type == Character.ENCLOSING_MARK;
    }

    private static boolean isPunctuationCategory(int type) {
        return type == Character.DASH_PUNCTUATION
                || type == Character.START_PUNCTUATION
                || type == Character.END_PUNCTUATION
                || type == Character.CONNECTOR_PUNCTUATION
                || type == Character.OTHER_PUNCTUATION
                || type == Character.INITIAL_QUOTE_PUNCTUATION
                || type == Character.FINAL_QUOTE_PUNCTUATION;
    }
}
