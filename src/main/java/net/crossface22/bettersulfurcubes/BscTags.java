package net.crossface22.bettersulfurcubes;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class BscTags {
    private BscTags() {}

    private static TagKey<Item> archetype(String name) {
        return TagKey.create(Registries.ITEM,
                Identifier.fromNamespaceAndPath("better-sulfur-cubes", "sulfur_cube_archetype/" + name));
    }

    public static final TagKey<Item> TWINKLE  = archetype("twinkle");
    public static final TagKey<Item> REDSTONE = archetype("redstone");
    public static final TagKey<Item> PAINFUL  = archetype("painful");
    public static final TagKey<Item> SOLID    = archetype("solid");
}
