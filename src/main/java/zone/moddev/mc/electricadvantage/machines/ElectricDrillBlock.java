package zone.moddev.mc.electricadvantage.machines;

import cyano.poweradvantage.api.ConduitType;
import cyano.poweradvantage.api.GUIBlock;
import cyano.poweradvantage.api.ITypedConduit;
import cyano.poweradvantage.api.PowerConnectorContext;
import cyano.poweradvantage.api.PoweredEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import zone.moddev.mc.electricadvantage.init.Power;
import zone.moddev.mc.poweradvantage.conduitnetwork.ConduitRegistry;

public class ElectricDrillBlock extends GUIBlock implements ITypedConduit {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    private final ConduitType[] types = {Power.ELECTRIC_POWER};

    public ElectricDrillBlock() {
        super(Block.Properties.of(Material.PISTON).strength(0.75F).dynamicShape().noOcclusion());
        registerDefaultState(getStateDefinition().any()
                .setValue(ACTIVE, false)
                .setValue(FACING, Direction.DOWN));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE, FACING);
    }

    @Override
    public ElectricMachineTileEntity newBlockEntity(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        return new ElectricDrillTileEntity(pos, state);
    }

    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, world, pos, oldState, isMoving);
        if (state.getBlock() == oldState.getBlock()) return;
        ConduitRegistry.getInstance().conduitBlockPlacedEvent(
                world, pos, Power.ELECTRIC_POWER);
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            ConduitRegistry.getInstance().conduitBlockRemovedEvent(
                    world, pos, Power.ELECTRIC_POWER);
            BlockEntity tile = world.getBlockEntity(pos);
            if (tile instanceof Container) {
                Containers.dropContents(world, pos, (Container) tile);
                world.updateNeighbourForOutputSignal(pos, this);
            }
        }
        super.onRemove(state, world, pos, newState, isMoving);
    }

    @Override
    public void wasExploded(Level world, BlockPos pos, Explosion explosion) {
        ConduitRegistry.getInstance().conduitBlockRemovedEvent(
                world, pos, Power.ELECTRIC_POWER);
        super.wasExploded(world, pos, explosion);
    }

    @Override
    public ConduitType[] getTypes() {
        return types;
    }

    @Override
    public boolean isPowerSink(ConduitType type) {
        return true;
    }

    @Override
    public boolean isPowerSource(ConduitType type) {
        return false;
    }

    @Override
    public boolean canAcceptConnection(PowerConnectorContext connection) {
        return ConduitType.areSameType(Power.ELECTRIC_POWER, connection.powerType);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite());
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state,
                                LivingEntity placer, ItemStack stack) {
        if (stack.hasCustomHoverName()) {
            BlockEntity tile = world.getBlockEntity(pos);
            if (tile instanceof PoweredEntity) {
                ((PoweredEntity) tile).setCustomInventoryName(stack.getHoverName().getString());
            }
        }
    }

    /** Decodes all six legacy facings; bit 3 is the active flag. */
    public BlockState getStateFromLegacyMeta(int meta) {
        return defaultBlockState()
                .setValue(FACING, Direction.from3DDataValue(meta & 0x7))
                .setValue(ACTIVE, (meta & 0x8) != 0);
    }

    public int getLegacyMetaFromState(BlockState state) {
        return state.getValue(FACING).get3DDataValue() | (state.getValue(ACTIVE) ? 0x8 : 0);
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos) {
        BlockEntity tile = world.getBlockEntity(pos);
        return tile instanceof ElectricMachineTileEntity
                ? ((ElectricMachineTileEntity) tile).getComparatorOutput() : 0;
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }
}
