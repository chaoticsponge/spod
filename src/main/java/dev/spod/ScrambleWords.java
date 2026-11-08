package dev.spod;

import java.util.List;
import java.util.random.RandomGenerator;

public final class ScrambleWords {
    static final List<String> WORDS = List.of(
            "egg", "apple", "arrow", "beach", "bread", "brick", "bridge", "bucket",
            "button", "candle", "carrot", "castle", "cherry", "chicken", "cloud", "copper",
            "crystal", "desert", "diamond", "dragon", "dream", "feather", "flower", "forest",
            "garden", "glass", "grape", "hammer", "honey", "island", "jungle", "kettle",
            "ladder", "lantern", "lemon", "library", "magnet", "marble", "market", "meadow",
            "melon", "mirror", "mountain", "mushroom", "needle", "ocean", "orange", "pancake",
            "paper", "pencil", "pepper", "pickle", "pillow", "planet", "pocket", "potato",
            "pumpkin", "rabbit", "rainbow", "river", "rocket", "saddle", "shadow", "shield",
            "silver", "spider", "sponge", "spring", "square", "stone", "string", "summer",
            "sunset", "temple", "thunder", "tiger", "tomato", "torch", "turtle", "village",
            "water", "window", "winter", "wizard");

    private ScrambleWords() {}

    public static Puzzle next(RandomGenerator random) {
        String answer = WORDS.get(random.nextInt(WORDS.size()));
        return new Puzzle(answer, jumble(answer, random));
    }

    static String jumble(String word, RandomGenerator random) {
        char[] letters = word.toCharArray();
        for (int i = letters.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temporary = letters[i];
            letters[i] = letters[j];
            letters[j] = temporary;
        }
        if (word.equals(new String(letters))) {
            // Repeated letters can leave a shuffle unchanged. Swap a distinct pair without retrying.
            for (int i = 1; i < letters.length; i++) {
                if (letters[i] != letters[0]) {
                    char temporary = letters[0];
                    letters[0] = letters[i];
                    letters[i] = temporary;
                    break;
                }
            }
        }
        return new String(letters);
    }

    public record Puzzle(String answer, String jumbled) {}
}
