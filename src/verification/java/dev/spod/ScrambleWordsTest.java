package dev.spod;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;

public final class ScrambleWordsTest {
    public static void main(String[] args) {
        if (new HashSet<>(ScrambleWords.WORDS).size() != ScrambleWords.WORDS.size()) {
            throw new AssertionError("Duplicate answers bias word selection");
        }
        Random identityShuffle = new Random(0) {
            @Override
            public int nextInt(int bound) {
                return bound - 1;
            }
        };
        for (String word : ScrambleWords.WORDS) {
            if (!word.matches("[a-z]{3,}")) {
                throw new AssertionError("Answers must be simple lowercase words");
            }
            check(word, ScrambleWords.jumble(word, identityShuffle));
            for (int seed = 0; seed < 100; seed++) {
                check(word, ScrambleWords.jumble(word, new Random(seed)));
            }
        }
        for (int seed = 0; seed < 1000; seed++) {
            var puzzle = ScrambleWords.next(new Random(seed));
            if (!ScrambleWords.WORDS.contains(puzzle.answer())) {
                throw new AssertionError("Puzzle answer is outside the curated word list");
            }
            check(puzzle.answer(), puzzle.jumbled());
        }
        System.out.println("Scramble checks passed: every answer, repeated letters, unchanged-shuffle fallback and random puzzles.");
    }

    private static void check(String answer, String puzzle) {
        char[] original = answer.toCharArray();
        char[] shuffled = puzzle.toCharArray();
        Arrays.sort(original);
        Arrays.sort(shuffled);
        if (answer.equals(puzzle) || !Arrays.equals(original, shuffled)) {
            throw new AssertionError("Puzzle must change the order and preserve every letter: " + answer + " -> " + puzzle);
        }
    }
}
