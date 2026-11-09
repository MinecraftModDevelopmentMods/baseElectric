package zone.moddev.mc.baseelectric.config;

import java.util.Arrays;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import zone.moddev.mc.baseelectric.ElectricAdvantage;
import zone.moddev.mc.baseelectric.util.crafting.RecipeDeconstructor;

/** Common configuration retaining the legacy gameplay defaults. */
public final class ElectricAdvantageConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue FABRICATOR_RECURSION_LIMIT;
    public static final ForgeConfigSpec.ConfigValue<String> LASER_SOUND;
    public static final ForgeConfigSpec.ConfigValue<String> PLASTIC_FLUIDS;
    private static boolean loaded;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("fabricator");
        FABRICATOR_RECURSION_LIMIT = builder.defineInRange(
                "recursionLimit", RecipeDeconstructor.RECURSION_LIMIT, 1, 255);
        builder.pop();
        builder.push("options");
        LASER_SOUND = builder.define("laserSound", "minecraft:block.note_block.bass");
        PLASTIC_FLUIDS = builder.comment("Semi-colon delimited fluid registry names usable by the plastic refinery.")
                .define("petrolplasticFluids", "refined_oil;basepower:refined_oil;oil");
        builder.pop();
        SPEC = builder.build();
    }

    private ElectricAdvantageConfig() {}

    public static void onConfigLoading(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() != SPEC) return;
        loaded = true;
        apply();
    }

    public static void apply() {
        if (!loaded || ElectricAdvantage.INSTANCE == null) return;
        RecipeDeconstructor.RECURSION_LIMIT = FABRICATOR_RECURSION_LIMIT.get();
        String laserSound = LASER_SOUND.get().trim();
        if ("block.note.bass".equals(laserSound)) {
            laserSound = "minecraft:block.note_block.bass";
        }
        ElectricAdvantage.INSTANCE.LASER_SOUND = laserSound;
        ElectricAdvantage.INSTANCE.PLASTIC_FLUID_MATERIALS.clear();
        Arrays.stream(PLASTIC_FLUIDS.get().split(";"))
                .map(String::trim).filter(value -> !value.isEmpty())
                .forEach(ElectricAdvantage.INSTANCE.PLASTIC_FLUID_MATERIALS::add);
    }
}
