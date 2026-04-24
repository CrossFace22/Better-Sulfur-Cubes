package net.crossface22.bettersulfurcubes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class BscConfig {

    private static final Path PATH =
            FabricLoader.getInstance().getConfigDir().resolve("better-sulfur-cubes.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static BscConfig INSTANCE = load();

    public boolean enableAdhesive = true;
    public boolean enableExplosive = true;
    public boolean enableMusical   = true;
    public boolean enableTwinkle   = true;
    public boolean enableRedstone  = true;
    public boolean enablePainful   = true;
    public boolean enableSolid     = true;

    private static BscConfig load() {
        if (Files.exists(PATH)) {
            try (Reader r = Files.newBufferedReader(PATH)) {
                BscConfig loaded = GSON.fromJson(r, BscConfig.class);
                if (loaded != null) return loaded;
            } catch (IOException e) {
                BetterSulfurCubes.LOGGER.warn("[BSC] Can't load config: {}", e.getMessage());
            }
        }
        BscConfig defaults = new BscConfig();
        defaults.save();
        return defaults;
    }

    public void save() {
        try (Writer w = Files.newBufferedWriter(PATH)) {
            GSON.toJson(this, w);
        } catch (IOException e) {
            BetterSulfurCubes.LOGGER.warn("[BSC] Can't save config: {}", e.getMessage());
        }
    }
}
