package net.crossface22.bettersulfurcubes;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BetterSulfurCubes implements ModInitializer {
	public static final String MOD_ID = "better-sulfur-cubes";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		BscConfig cfg = BscConfig.INSTANCE;
		LOGGER.info("[BSC] Config loaded: adhesive={}, explosive={}, musical={}, twinkle={}",
				cfg.enableAdhesive, cfg.enableExplosive, cfg.enableMusical, cfg.enableTwinkle);
	}
}