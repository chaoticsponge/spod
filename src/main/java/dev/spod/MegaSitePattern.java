package dev.spod;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import com.google.gson.Gson;
import net.minecraft.core.BlockPos;

/** Block fingerprints sampled from Mega Showdown's structure templates. */
record MegaSitePattern(String name, String anchor, String symbol, int color, List<Sample> samples) {
    record Sample(int x, int y, int z, String block) {}

    static List<MegaSitePattern> load() throws IOException {
        try (var reader = new InputStreamReader(Objects.requireNonNull(
                MegaSitePattern.class.getResourceAsStream("/mega_showdown_sites.json")), StandardCharsets.UTF_8)) {
            return List.of(new Gson().fromJson(reader, MegaSitePattern[].class));
        }
    }

    boolean matches(BlockPos anchorPos, Function<BlockPos, String> blockAt) {
        for (int mirror : new int[]{1, -1}) {
            for (int rotation = 0; rotation < 4; rotation++) {
                int missing = 0;
                for (Sample sample : samples) {
                    int x = sample.x * mirror;
                    int z = sample.z;
                    for (int turn = 0; turn < rotation; turn++) {
                        int previousX = x;
                        x = -z;
                        z = previousX;
                    }
                    if (!sample.block.equals(blockAt.apply(anchorPos.offset(x, sample.y, z))) && ++missing > 2) break;
                }
                if (missing <= 2) return true;
            }
        }
        return false;
    }
}
