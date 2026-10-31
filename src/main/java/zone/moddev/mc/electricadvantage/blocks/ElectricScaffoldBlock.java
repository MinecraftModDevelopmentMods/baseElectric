package zone.moddev.mc.electricadvantage.blocks;

import cyano.poweradvantage.api.simple.BlockSimplePowerConduit;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.BlockGetter;
import zone.moddev.mc.electricadvantage.init.Power;

/** Full-block electric track/scaffold with conduit behavior. */
public class ElectricScaffoldBlock extends BlockSimplePowerConduit {
    public ElectricScaffoldBlock() {
        super(Properties.of(Material.PISTON).strength(0.75F).noOcclusion()
                .isRedstoneConductor((state, world, pos) -> false),
                2.0F / 16.0F, Power.ELECTRIC_POWER);
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) { return Shapes.block(); }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) { return Shapes.block(); }
}
