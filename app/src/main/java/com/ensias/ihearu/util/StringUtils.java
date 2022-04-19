package com.ensias.ihearu.util;

import org.dicio.skill.util.WordExtractor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

public final class StringUtils {
    private static final Pattern PUNCTUATION_PATTERN = Pattern.compile("\\p{Punct}");
    private static final Pattern WORD_DELIMITERS_PATTERN = Pattern.compile("[^\\p{L}0-9]");

    private StringUtils() {
    }

    // Joins strings using delimiter

    public static String join(final String delimiter, final List<String> strings) {
        final StringBuilder builder = new StringBuilder();
        final Iterator<String> iterator = strings.iterator();

        if (iterator.hasNext()) {
            builder.append(iterator.next());
        }
        while (iterator.hasNext()) {
            builder.append(delimiter);
            builder.append(iterator.next());
        }

        return builder.toString();
    }

    // Joins the strings by putting a space in between them.

    public static String join(final List<String> strings) {
        return join(" ", strings);
    }

    // Removes the punctuation in a string

    public static String removePunctuation(final String string) {
        return RegexUtils.replaceAll(PUNCTUATION_PATTERN, string, "");
    }

    public static boolean isNullOrEmpty(final String string) {
        return string == null || string.isEmpty();
    }

    private static String cleanStringForDistance(final String s) {
        return WORD_DELIMITERS_PATTERN.matcher(WordExtractor.nfkdNormalizeWord(s.toLowerCase()))
                .replaceAll("");
    }

    // Returns the dynamic programming memory obtained when calculating the Levenshtein distance.

    private static int[][] levenshteinDistanceMemory(final String a, final String b) {
        // memory already filled with zeros, as it's the default value for int
        final int[][] memory = new int[a.length() + 1][b.length() + 1];

        for (int i = 0; i <= a.length(); ++i) {
            memory[i][0] = i;
        }
        for (int j = 0; j <= b.length(); ++j) {
            memory[0][j] = j;
        }

        for (int i = 0; i < a.length(); ++i) {
            for (int j = 0; j < b.length(); ++j) {
                final int substitutionCost = Character.toLowerCase(a.codePointAt(i))
                        == Character.toLowerCase(b.codePointAt(j)) ? 0 : 1;
                memory[i + 1][j + 1] = Math.min(Math.min(
                        memory[i][j + 1] + 1,
                        memory[i + 1][j] + 1),
                        memory[i][j] + substitutionCost);
            }
        }

        return memory;
    }

    private static class LevenshteinMemoryPos {
        final int i;
        final int j;
        LevenshteinMemoryPos(final int i, final int j) {
            this.i = i;
            this.j = j;
        }
    }

    private static List<LevenshteinMemoryPos> pathInLevenshteinMemory(
            final String a, final String b, final int[][] memory) {
        // follow the path from bottom right (score==distance) to top left (where score==0)
        final List<LevenshteinMemoryPos> positions = new ArrayList<>();
        int i = a.length() - 1;
        int j = b.length() - 1;
        while (i >= 0 && j >= 0) {
            positions.add(new LevenshteinMemoryPos(i, j));
            if (memory[i + 1][j + 1] == memory[i][j + 1] + 1) {
                // the path goes up
                --i;
            } else if (memory[i + 1][j + 1] == memory[i + 1][j] + 1) {
                // the path goes left
                --j;
            } else  {
                // the path goes up-left diagonally (surely either
                // memory[i+1][j+1] == memory[i][j] or memory[i+1][j+1] == memory[i][j] + 1)
                --i;
                --j;
            }
        }
        return positions;
    }


    public static int levenshteinDistance(final String aNotCleaned, final String bNotCleaned) {
        final String a = cleanStringForDistance(aNotCleaned);
        final String b = cleanStringForDistance(bNotCleaned);
        return levenshteinDistanceMemory(a, b)[a.length()][b.length()];
    }


    public static int customStringDistance(final String aNotCleaned, final String bNotCleaned) {
        final String a = cleanStringForDistance(aNotCleaned);
        final String b = cleanStringForDistance(bNotCleaned);
        final int[][] memory = levenshteinDistanceMemory(a, b);

        int matchingCharCount = 0;
        int subsequentChars = 0;
        int maxSubsequentChars = 0;
        for (final LevenshteinMemoryPos pos : pathInLevenshteinMemory(a, b, memory)) {
            if (Character.toLowerCase(a.codePointAt(pos.i))
                    == Character.toLowerCase(b.codePointAt(pos.j))) {
                ++matchingCharCount;
                ++subsequentChars;
                maxSubsequentChars = Math.max(maxSubsequentChars, subsequentChars);
            } else {
                subsequentChars = Math.max(0, subsequentChars - 1);
            }
        }

        return memory[a.length()][b.length()] - maxSubsequentChars - matchingCharCount;
    }
}
