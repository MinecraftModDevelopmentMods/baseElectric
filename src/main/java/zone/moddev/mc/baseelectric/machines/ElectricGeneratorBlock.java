package zone.moddev.mc.baseelectric.machines;

import cyano.poweradvantage.api.ConduitType;
import cyano.poweradvantage.api.simple.BlockSimplePowerMachine;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import zone.moddev.mc.baseelectric.init.Power;

public abstract class ElectricGeneratorBlock extends BlockSimplePowerMachine {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public ElectricGeneratorBlock() {
        this(Material.PISTON);
    }

    public ElectricGeneratorBlock(Material material) {
        super(material, 0.75f, Power.ELECTRIC_POWER);
		initializeDefaultState();
	}

	protected ElectricGeneratorBlock(Block.Properties properties) {
		super(properties, Power.ELECTRIC_POWER);
		initializeDefaultState();
	}

	private void initializeDefaultState() {
        registerDefaultState(getStateDefinition().any()
                .setValue(ACTIVE, false)
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE, FACING);
    }

    /** Decodes the 1.10/1.12 metadata used by world migration fixtures. */
    public BlockState getStateFromLegacyMeta(int meta) {
        Direction facing = Direction.from3DDataValue(meta & 0x7);
        if (facing.getAxis() == Direction.Axis.Y) facing = Direction.NORTH;
        return defaultBlockState().setValue(FACING, facing).setValue(ACTIVE, (meta & 0x8) != 0);
    }

    public int getLegacyMetaFromState(BlockState state) {
        return state.getValue(FACING).get3DDataValue() | (state.getValue(ACTIVE) ? 0x8 : 0);
    }

    @Override
    public abstract ElectricGeneratorTileEntity newBlockEntity(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state);

    @Override
    public int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos) {
        BlockEntity tile = world.getBlockEntity(pos);
        return tile instanceof ElectricGeneratorTileEntity
                ? ((ElectricGeneratorTileEntity) tile).getComparatorOutput() : 0;
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity tile = world.getBlockEntity(pos);
            if (tile instanceof Container) {
                Containers.dropContents(world, pos, (Container) tile);
                ((Container) tile).clearContent();
                world.updateNeighbourForOutputSignal(pos, this);
            }
        }
        super.onRemove(state, world, pos, newState, isMoving);
    }

    @Override
    public boolean isPowerSink(ConduitType type) {
        return false;
    }

    @Override
    public boolean isPowerSource(ConduitType type) {
        return true;
    }

    public ConduitType getType() {
        return Power.ELECTRIC_POWER;
    }
}
