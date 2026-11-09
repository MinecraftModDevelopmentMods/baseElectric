package zone.moddev.mc.baseelectric.blocks;

import zone.moddev.mc.baseelectric.init.Power;
import net.minecraft.world.level.material.Material;

public class ElectricConduitBlock  extends cyano.poweradvantage.api.simple.BlockSimplePowerConduit{

	public ElectricConduitBlock() {
		super(Material.PISTON, 0.75f, 2f/16f, Power.ELECTRIC_POWER);
	}
}
