package zone.moddev.mc.baseelectric.blocks;

import zone.moddev.mc.baseelectric.init.Power;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public class ElectricConduitBlock  extends cyano.poweradvantage.api.simple.BlockSimplePowerConduit{

	public ElectricConduitBlock() {
        this("electric_conduit");
    }

    protected ElectricConduitBlock(String id) {
		super(Properties.of().setId(zone.moddev.mc.baseelectric.util.RegistryIds.block(id))
                .mapColor(MapColor.STONE).pushReaction(PushReaction.IMMOVEABLE)
				.forceSolidOn().strength(0.75f), 2f/16f, Power.ELECTRIC_POWER);
	}
}
