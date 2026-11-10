package zone.moddev.mc.baseelectric.machines;

import cyano.poweradvantage.api.ConduitType;
import cyano.poweradvantage.api.simple.BlockSimplePowerMachine;
import net.minecraft.world.level.block.Block;
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

public abstract class ElectricMachineBlock extends BlockSimplePowerMachine {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    public static final BooleanProperty POWERED = BooleanProperty.create("powered");

    public ElectricMachineBlock() {
        this(machineProperties(0.75f));
    }

	protected ElectricMachineBlock(Block.Properties properties) {
		this(properties, Power.ELECTRIC_POWER);
	}

	protected ElectricMachineBlock(Block.Properties properties, ConduitType type) {
		super(properties, type);
		initializeDefaultState();
	}

	private void initializeDefaultState() {
        registerDefaultState(getStateDefinition().any()
                .setValue(ACTIVE, false)
                .setValue(POWERED, false)
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE, FACING, POWERED);
    }

    /** Decodes the 1.10/1.12 metadata used by world migration fixtures. */
    public BlockState getStateFromLegacyMeta(int meta) {
        return defaultBlockState()
                .setValue(FACING, metaToFacing(meta))
                .setValue(ACTIVE, (meta & 0x4) != 0)
                .setValue(POWERED, (meta & 0x8) != 0);
    }

    /** Encodes the legacy metadata layout for migration tests. */
    public int getLegacyMetaFromState(BlockState state) {
        int meta = facingToMeta(state.getValue(FACING));
        if (state.getValue(ACTIVE)) meta |= 0x4;
        if (state.getValue(POWERED)) meta |= 0x8;
        return meta;
    }

    private static int facingToMeta(Direction facing) {
        switch (facing) {
            case WEST: return 1;
            case SOUTH: return 2;
            case EAST: return 3;
            default: return 0;
        }
    }

    private static Direction metaToFacing(int meta) {
        switch (meta & 0x3) {
            case 1: return Direction.WEST;
            case 2: return Direction.SOUTH;
            case 3: return Direction.EAST;
            default: return Direction.NORTH;
        }
    }

    @Override
    public abstract ElectricMachineTileEntity newBlockEntity(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state);

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
        return true;
    }

    @Override
    public boolean isPowerSource(ConduitType type) {
        return false;
    }

    public ConduitType getType() {
        return Power.ELECTRIC_POWER;
    }
}
