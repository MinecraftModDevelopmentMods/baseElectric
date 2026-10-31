package zone.moddev.mc.electricadvantage.machines;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;

public class LaserTurretBlock extends ElectricMachineBlock {

	private static final VoxelShape SHAPE = Shapes.or(
			Block.box(0, 0, 0, 16, 4, 16),
			Block.box(4, 4, 4, 12, 16, 12));

	// Evil laser turrets target players and not hostile mobs
	public final boolean evil;
	public LaserTurretBlock(boolean isEvil) {
		super(Block.Properties.of(Material.PISTON)
				.strength(5.0F, 100.0F).dynamicShape().noOcclusion());
		this.evil = isEvil;
	}

	@Override
	public ElectricMachineTileEntity newBlockEntity(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
		return new LaserTurretTileEntity(pos, state);
	}




	@Override
	public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public void setPlacedBy(Level w, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(w, pos, state, placer, stack);
		BlockEntity te = w.getBlockEntity(pos);
		if(placer instanceof Player && te instanceof LaserTurretTileEntity){
			((LaserTurretTileEntity)te).setOwner((Player)placer);
		}
	}

}
