package zone.moddev.mc.baseelectric.machines;

import cyano.poweradvantage.api.ConduitType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import zone.moddev.mc.baseelectric.init.Entities;
import zone.moddev.mc.baseelectric.blocks.ElectricConduitBlock;

/** Electric conduit that consumes a small amount of power to emit light. */
public class LEDBlock extends ElectricConduitBlock implements EntityBlock {
    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    public LEDBlock() {
        super("led_bar");
        registerDefaultState(getStateDefinition().any()
                .setValue(WEST, false).setValue(DOWN, false).setValue(SOUTH, false)
                .setValue(EAST, false).setValue(UP, false).setValue(NORTH, false)
                .setValue(LIT, false));
    }

    @Override
    public BlockEntity newBlockEntity(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        return new LEDTileEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == Entities.LED ? (world, pos, blockState, tile) -> ((LEDTileEntity) tile).tick() : null;
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter world, BlockPos pos) {
        return state.getBlock() == this && state.getValue(LIT) ? 15 : 0;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LIT);
    }

    public BlockState getStateFromLegacyMeta(int meta) {
        return defaultBlockState().setValue(LIT, (meta & 1) != 0);
    }

    public int getLegacyMetaFromState(BlockState state) {
        return state.getValue(LIT) ? 1 : 0;
    }

    @Override
    public boolean isPowerSink(ConduitType type) {
        return true;
    }
}
