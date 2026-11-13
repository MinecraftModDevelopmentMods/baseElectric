package zone.moddev.mc.baseelectric.util.farming;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.junit.BeforeClass;
import org.junit.Test;
import zone.moddev.mc.baseelectric.util.RegistryIds;
import zone.moddev.mc.baseelectric.verification.NativeTestBootstrap;
import static org.junit.Assert.*;

public class VirtualCropNativeTest {
    @BeforeClass public static void bootstrap() { NativeTestBootstrap.initialize(); }

    @Test public void nativeStemLookupPreservesVanillaAndCustomFruitKeysWithoutACodec() {
        assertEquals(Blocks.MELON.builtInRegistryHolder().key(),
                VirtualCrop.stemFruitKey((StemBlock) Blocks.MELON_STEM));
        assertEquals(Blocks.PUMPKIN.builtInRegistryHolder().key(),
                VirtualCrop.stemFruitKey((StemBlock) Blocks.PUMPKIN_STEM));
        var fruit = ResourceKey.create(Registries.BLOCK, Identifier.parse("fixture:custom_fruit"));
        var soil = TagKey.create(Registries.BLOCK, Identifier.parse("fixture:custom_soil"));
        var custom = new StemBlock(fruit,
                ResourceKey.create(Registries.BLOCK, Identifier.parse("fixture:attached_stem")),
                ResourceKey.create(Registries.ITEM, Identifier.parse("fixture:custom_seed")), soil, soil,
                BlockBehaviour.Properties.of().setId(RegistryIds.block("stem_fixture")));
        assertEquals(fruit, VirtualCrop.stemFruitKey(custom));
        assertNotEquals(Blocks.MELON.builtInRegistryHolder().key(), VirtualCrop.stemFruitKey(custom));
    }
}
