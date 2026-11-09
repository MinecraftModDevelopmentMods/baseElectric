package zone.moddev.mc.baseelectric.blocks;

import zone.moddev.mc.baseelectric.init.Power;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public class ElectricConduitBlock  extends cyano.poweradvantage.api.simple.BlockSimplePowerConduit{

	public ElectricConduitBlock() {
		super(Properties.of().mapColor(MapColor.STONE).pushReaction(PushReaction.BLOCK)
				.forceSolidOn().strength(0.75f), 2f/16f, Power.ELECTRIC_POWER);
	}
}
