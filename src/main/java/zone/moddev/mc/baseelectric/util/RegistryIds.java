package zone.moddev.mc.baseelectric.util;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/** Native properties require their persistent registry key before construction. */
public final class RegistryIds {
    private RegistryIds() {}

    public static ResourceKey<Block> block(String path) {
        return ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("baseelectric", path));
    }

    public static ResourceKey<Item> item(String path) {
        return ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("baseelectric", path));
    }
}
