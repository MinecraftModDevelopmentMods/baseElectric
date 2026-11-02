package zone.moddev.mc.baseelectric.init;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.event.FMLMissingMappingsEvent;
import net.minecraftforge.fml.common.event.FMLMissingMappingsEvent.Action;
import net.minecraftforge.fml.common.event.FMLMissingMappingsEvent.MissingMapping;
import net.minecraftforge.fml.common.registry.GameRegistry;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import zone.moddev.mc.baseelectric.ElectricAdvantage;
import zone.moddev.mc.baseelectric.entities.HydroturbineEntity;
import zone.moddev.mc.baseelectric.machines.*;
import zone.moddev.mc.basepower.compat.LegacyIdentity;

import static org.junit.Assert.*;

public class IdentityMigrationTest {
    private static final Class<?>[] TILE_TYPES = {ElectricFurnaceTileEntity.class, ElectricCrusherTileEntity.class,
            ElectricDrillTileEntity.class, ElectricFabricatorTileEntity.class,
            GrowthChamberTileEntity.class, GrowthChamberControllerTileEntity.class,
            ElectricOvenTileEntity.class, LaserTurretTileEntity.class, LEDTileEntity.class,
            HydroelectricGeneratorTileEntity.class, PhotovoltaicGeneratorTileEntity.class,
            SteamPoweredElectricGeneratorTileEntity.class, ElectricBatteryArrayTileEntity.class,
            ElectricPumpTileEntity.class, ElectricStillTileEntity.class, PlasticRefineryTileEntity.class};
    private static final String[] TILE_SUFFIXES = {"electric_furnace", "electric_crusher", "electric_drill", "electric_fabricator",
            "growth_chamber", "growth_chamber_controller", "electric_oven", "laser_turret", "l_e_d",
            "hydroelectric_generator", "photovoltaic_generator", "steam_powered_electric_generator",
            "electric_battery_array", "electric_pump", "electric_still", "plastic_refinery"};
    @Rule public final TemporaryFolder temporary = new TemporaryFolder();

    @BeforeClass public static void bootstrap() {
        Bootstrap.register();
        ElectricAdvantage.INSTANCE = new ElectricAdvantage();
        for (Class<?> type : TILE_TYPES) Entities.registerTileEntity(type.asSubclass(TileEntity.class));
    }

    @Test public void handlerReadsTheOldOwnersMappingsEvenWithoutANewModContainer() {
        ResourceLocation name = new ResourceLocation("baseelectric", "identity_test_block");
        Block block = new Block(Material.ROCK).setRegistryName(name);
        Item itemBlock = new ItemBlock(block).setRegistryName(name);
        Block.REGISTRY.register(3991, name, block);
        Item.REGISTRY.register(31991, name, itemBlock);
        MissingMapping blockMapping = new MissingMapping(GameRegistry.Type.BLOCK,
                new ResourceLocation("electricadvantage", "identity_test_block"), 3990);
        MissingMapping itemMapping = new MissingMapping(GameRegistry.Type.ITEM,
                new ResourceLocation("electricadvantage", "identity_test_block"), 31990);
        MissingMapping unknown = new MissingMapping(GameRegistry.Type.ITEM,
                new ResourceLocation("electricadvantage", "unsupported"), 31989);
        MissingMapping foreign = new MissingMapping(GameRegistry.Type.ITEM,
                new ResourceLocation("unpublishedidentity", "identity_test_block"), 31988);
        ListMultimap<String, MissingMapping> mappings = ArrayListMultimap.create();
        mappings.put("electricadvantage", blockMapping);
        mappings.put("electricadvantage", itemMapping);
        mappings.put("electricadvantage", unknown);
        mappings.put("unpublishedidentity", foreign);
        new ElectricAdvantage().missingMappings(new FMLMissingMappingsEvent(mappings));
        assertEquals(Action.REMAP, blockMapping.getAction());
        assertSame(block, blockMapping.getTarget());
        assertEquals(Action.REMAP, itemMapping.getAction());
        assertSame(itemBlock, itemMapping.getTarget());
        assertEquals(Action.FAIL, unknown.getAction());
        assertEquals(Action.DEFAULT, foreign.getAction());
    }

    @Test public void allSixteenReleasedTileAliasesKeepOnlyNewCanonicalWrites() throws Exception {
        Map<?, ?> reads = tileMap("nameToClassMap", "field_145855_i");
        Map<?, ?> writes = tileMap("classToNameMap", "field_145853_j");
        for (int index = 0; index < TILE_TYPES.length; index++) {
            assertSame(TILE_TYPES[index], reads.get("electricadvantage." + TILE_SUFFIXES[index]));
            assertSame(TILE_TYPES[index], reads.get("baseelectric." + TILE_SUFFIXES[index]));
            assertEquals("baseelectric." + TILE_SUFFIXES[index], writes.get(TILE_TYPES[index]));
        }
    }

    @Test public void nativeFurnaceLoadRetainsInventoryEnergyAndPositionAcrossCanonicalReloads() {
        ElectricFurnaceTileEntity original = new ElectricFurnaceTileEntity();
        original.setPos(new BlockPos(-193, 77, 2049));
        original.setEnergy(123, Power.ELECTRIC_POWER);
        original.setInventorySlotContents(0, new ItemStack(net.minecraft.init.Items.IRON_INGOT, 3));
        NBTTagCompound saved = original.writeToNBT(new NBTTagCompound());
        saved.setString("id", "electricadvantage.electric_furnace");
        for (int pass = 0; pass < 3; pass++) {
            TileEntity loaded = TileEntity.create(null, saved);
            assertTrue(loaded instanceof ElectricFurnaceTileEntity);
            ElectricFurnaceTileEntity furnace = (ElectricFurnaceTileEntity) loaded;
            assertEquals(original.getPos(), furnace.getPos());
            assertEquals(123, furnace.getEnergy(Power.ELECTRIC_POWER), 0);
            assertEquals(3, furnace.getStackInSlot(0).stackSize);
            assertSame(net.minecraft.init.Items.IRON_INGOT, furnace.getStackInSlot(0).getItem());
            saved = furnace.writeToNBT(new NBTTagCompound());
            assertEquals("baseelectric.electric_furnace", saved.getString("id"));
        }
    }

    @Test public void releasedTurbineLoadsThroughTheNativeEntityFactoryAndSavesCanonicalId() throws Exception {
        String legacy = "electricadvantage.electricadvantage.Hydroturbine";
        String shortLegacy = "electricadvantage.Hydroturbine";
        String canonical = "baseelectric.Hydroturbine";
        Class<? extends Entity> previousRead = EntityList.NAME_TO_CLASS.put(canonical, HydroturbineEntity.class);
        Class<? extends Entity> previousAlias = EntityList.NAME_TO_CLASS.get(legacy);
        Class<? extends Entity> previousShortAlias = EntityList.NAME_TO_CLASS.get(shortLegacy);
        String previousWrite = EntityList.CLASS_TO_NAME.put(HydroturbineEntity.class, canonical);
        try {
            Entities.registerHydroturbineAliases();
            World world = noTickWorld();
            HydroturbineEntity original = new HydroturbineEntity(world);
            original.setPosition(-193.5, 76, 2049.5);
            for (String oldId : new String[] {legacy, shortLegacy}) {
                NBTTagCompound saved = new NBTTagCompound();
                assertTrue(original.writeToNBTOptional(saved));
                saved.setIntArray("parentPos", new int[] {-194, 77, 2049});
                saved.setString("id", oldId);
                for (int pass = 0; pass < 3; pass++) {
                    Entity loaded = EntityList.createEntityFromNBT(saved, world);
                    assertTrue(oldId, loaded instanceof HydroturbineEntity);
                    NBTTagCompound next = new NBTTagCompound();
                    assertTrue(loaded.writeToNBTOptional(next));
                    assertEquals(canonical, next.getString("id"));
                    assertEquals(saved.getUniqueId("UUID"), next.getUniqueId("UUID"));
                    assertEquals(saved.getTagList("Pos", 6), next.getTagList("Pos", 6));
                    assertArrayEquals(saved.getIntArray("parentPos"), next.getIntArray("parentPos"));
                    saved = next;
                }
            }
        } finally {
            if (previousRead == null) EntityList.NAME_TO_CLASS.remove(canonical);
            else EntityList.NAME_TO_CLASS.put(canonical, previousRead);
            if (previousAlias == null) EntityList.NAME_TO_CLASS.remove(legacy);
            else EntityList.NAME_TO_CLASS.put(legacy, previousAlias);
            if (previousShortAlias == null) EntityList.NAME_TO_CLASS.remove(shortLegacy);
            else EntityList.NAME_TO_CLASS.put(shortLegacy, previousShortAlias);
            if (previousWrite == null) EntityList.CLASS_TO_NAME.remove(HydroturbineEntity.class);
            else EntityList.CLASS_TO_NAME.put(HydroturbineEntity.class, previousWrite);
        }
    }

    @Test public void turbineAliasDoesNotChangeCanonicalSerializationOrAddUnpublishedAliases() {
        String legacy = "electricadvantage.electricadvantage.Hydroturbine";
        String shortLegacy = "electricadvantage.Hydroturbine";
        String canonical = "baseelectric.Hydroturbine";
        Class<? extends Entity> previous = EntityList.NAME_TO_CLASS.get(legacy);
        Class<? extends Entity> previousShort = EntityList.NAME_TO_CLASS.get(shortLegacy);
        String previousWrite = EntityList.CLASS_TO_NAME.put(HydroturbineEntity.class, canonical);
        try {
            Entities.registerHydroturbineAliases();
            assertSame(HydroturbineEntity.class, EntityList.NAME_TO_CLASS.get(legacy));
            assertSame(HydroturbineEntity.class, EntityList.NAME_TO_CLASS.get(shortLegacy));
            assertEquals(canonical, EntityList.getEntityStringFromClass(HydroturbineEntity.class));
            assertFalse(EntityList.NAME_TO_CLASS.containsKey("zone.moddev.mc.electricadvantage.Hydroturbine"));
        } finally {
            if (previous == null) EntityList.NAME_TO_CLASS.remove(legacy);
            else EntityList.NAME_TO_CLASS.put(legacy, previous);
            if (previousShort == null) EntityList.NAME_TO_CLASS.remove(shortLegacy);
            else EntityList.NAME_TO_CLASS.put(shortLegacy, previousShort);
            if (previousWrite == null) EntityList.CLASS_TO_NAME.remove(HydroturbineEntity.class);
            else EntityList.CLASS_TO_NAME.put(HydroturbineEntity.class, previousWrite);
        }
    }

    @Test public void conflictingShortAliasCannotPartiallyInstallTheReleasedAlias() {
        String legacy = "electricadvantage.electricadvantage.Hydroturbine";
        String shortLegacy = "electricadvantage.Hydroturbine";
        Class<? extends Entity> previous = EntityList.NAME_TO_CLASS.remove(legacy);
        Class<? extends Entity> previousShort = EntityList.NAME_TO_CLASS.put(shortLegacy,
                net.minecraft.entity.item.EntityItem.class);
        String canonical = EntityList.CLASS_TO_NAME.get(HydroturbineEntity.class);
        try {
            try {
                Entities.registerHydroturbineAliases();
                fail("A conflicting legacy owner must stop startup");
            } catch (IllegalStateException expected) {
                assertTrue(expected.getMessage().contains(shortLegacy));
            }
            assertFalse(EntityList.NAME_TO_CLASS.containsKey(legacy));
            assertSame(net.minecraft.entity.item.EntityItem.class, EntityList.NAME_TO_CLASS.get(shortLegacy));
            assertEquals(canonical, EntityList.CLASS_TO_NAME.get(HydroturbineEntity.class));
        } finally {
            if (previous != null) EntityList.NAME_TO_CLASS.put(legacy, previous);
            if (previousShort == null) EntityList.NAME_TO_CLASS.remove(shortLegacy);
            else EntityList.NAME_TO_CLASS.put(shortLegacy, previousShort);
        }
    }

    @Test public void releasedConfigurationIsCopiedAndAnExistingNewConfigurationWins() throws Exception {
        Path config = temporary.newFolder().toPath();
        Path old = config.resolve("electricadvantage.cfg");
        Path canonical = config.resolve("baseelectric.cfg");
        byte[] original = "options { S:laser_sound=block.note.harp }\r\n".getBytes(StandardCharsets.UTF_8);
        Files.write(old, original);
        assertEquals(canonical.toFile(), LegacyIdentity.migrateConfiguration(canonical.toFile(), "electricadvantage"));
        assertArrayEquals(original, Files.readAllBytes(canonical));
        byte[] custom = "new user settings".getBytes(StandardCharsets.UTF_8);
        Files.write(canonical, custom);
        LegacyIdentity.migrateConfiguration(canonical.toFile(), "electricadvantage");
        assertArrayEquals(original, Files.readAllBytes(old));
        assertArrayEquals(custom, Files.readAllBytes(canonical));
    }

    @Test public void importedCustomLootSurvivesDefaultGenerationAndCannotBeInjectedTwice() throws Exception {
        Path config = temporary.newFolder().toPath();
        Path old = config.resolve("additional-loot-tables/electricadvantage/chests/simple_dungeon.json");
        Files.createDirectories(old.getParent());
        byte[] original = "{\"pools\":[{\"rolls\":9,\"entries\":[{\"type\":\"item\",\"name\":\"electricadvantage:psu\",\"weight\":37}]}]}"
                .getBytes(StandardCharsets.UTF_8);
        Files.write(old, original);
        LegacyIdentity.migrateAdditionalLootTables(config, "electricadvantage", "baseelectric");
        Path canonical = config.resolve("additional-loot-tables/baseelectric/chests/simple_dungeon.json");
        byte[] imported = Files.readAllBytes(canonical);
        Field initialized = TreasureChests.class.getDeclaredField("initDone");
        initialized.setAccessible(true);
        boolean previous = initialized.getBoolean(null);
        try {
            initialized.setBoolean(null, false);
            TreasureChests.init(config);
            assertArrayEquals(imported, Files.readAllBytes(canonical));
            assertTrue(new String(imported, StandardCharsets.UTF_8).contains("baseelectric:psu"));
            assertFalse(Files.exists(old));
            assertArrayEquals(original, Files.readAllBytes(old.resolveSibling("simple_dungeon.json.before-baseelectric")));
            LegacyIdentity.migrateAdditionalLootTables(config, "electricadvantage", "baseelectric");
            assertArrayEquals(imported, Files.readAllBytes(canonical));
        } finally {
            initialized.setBoolean(null, previous);
        }
    }

    private static Map<?, ?> tileMap(String mcp, String srg) throws Exception {
        Field field;
        try { field = TileEntity.class.getDeclaredField(mcp); }
        catch (NoSuchFieldException missing) { field = TileEntity.class.getDeclaredField(srg); }
        field.setAccessible(true);
        return (Map<?, ?>) field.get(null);
    }

    private static World noTickWorld() throws Exception {
        Class<?> allocator = Class.forName("sun.misc.Unsafe");
        Field instance = allocator.getDeclaredField("theUnsafe");
        instance.setAccessible(true);
        World world = (World) allocator.getMethod("allocateInstance", Class.class)
                .invoke(instance.get(null), NoTickWorld.class);
        Field random = World.class.getField("rand");
        random.setAccessible(true);
        random.set(world, new Random(0));
        Field provider = World.class.getField("provider");
        provider.setAccessible(true);
        provider.set(world, new WorldProviderSurface());
        return world;
    }

    private static final class NoTickWorld extends WorldServer {
        private NoTickWorld() { super(null, null, null, 0, null); }
    }
}
