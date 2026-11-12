package zone.moddev.mc.baseelectric.entities;

import zone.moddev.mc.baseelectric.init.Power;
import zone.moddev.mc.baseelectric.machines.HydroelectricGeneratorTileEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;

import java.util.List;

public class HydroturbineEntity extends net.minecraft.world.entity.Entity{

	public static final float DEGREES_PER_TICK = 30;
	private static final float RADIANS_TO_DEGREES = (float)(180 / Math.PI);
	private static final float DEGREES_TO_RADIANS = (float)(Math.PI / 180);
	public static final float PROPELLER_DAMAGE = 5f;
	public float rotation = 0;
	public boolean isSpinning = false;
	public BlockEntity parent = null;
	private BlockPos parentCoord = null;
	private final long roffset;
	
	public HydroturbineEntity(EntityType<?> type, Level w) {
		super(type, w);
		this.blocksBuilding = true;
		roffset = w.getRandom().nextInt(8);
	}
	public HydroturbineEntity(Level w, BlockPos parentTileEntity) {
		this(w, w.getBlockEntity(parentTileEntity));
	}

	public HydroturbineEntity(Level w, BlockEntity parentTileEntity) {
		this(zone.moddev.mc.baseelectric.init.Entities.HYDROTURBINE, w);
		this.parent = parentTileEntity;
		BlockPos p = parentTileEntity.getBlockPos();
		this.parentCoord = p;
		this.setPos(p.getX()+0.5, p.getY()-1, p.getZ()+0.5);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		// do nothing
	}

	@Override
	public boolean isPushedByFluid() {
		// This is anchored to its generator; flow drives rotation, not linear movement.
		return false;
	}

	@Override
	public void tick(){
		super.tick();
		FluidState fluidState = this.level().getFluidState(blockPosition());
		Vec3 directionVector;
		if(!fluidState.isEmpty()){
			directionVector = fluidState.getFlow(level(), blockPosition());
			isSpinning = !(directionVector.x == 0 && directionVector.z == 0);
		} else {
			isSpinning = false;
			directionVector = new Vec3(0,0,0);
		}
		if(this.level().isClientSide()){
			// client-side only
			if(isSpinning){
				rotation += DEGREES_PER_TICK;
				this.setYRot(RADIANS_TO_DEGREES * (float)Math.atan2(-directionVector.z, directionVector.x) -90);
			} else {
				// do nothing
			}
		} else {
			// server-side only
			if((parent == null || parent.isRemoved()) && parentCoord != null){
				if(!this.level().hasChunkAt(parentCoord)) return;
				parent = this.level().getBlockEntity(parentCoord);
			}
			if(parent == null || parent.isRemoved()){
				this.discard();
			} else if(parent instanceof HydroelectricGeneratorTileEntity){
				HydroelectricGeneratorTileEntity generator = (HydroelectricGeneratorTileEntity)parent;
				if(isSpinning){
					generator.addEnergy(HydroelectricGeneratorTileEntity.ENERGY_PER_TICK, Power.ELECTRIC_POWER);
				}
				generator.setActive(isSpinning);
			}
			if(isSpinning && level().getGameTime() % 8 == roffset){
				// slice-n-dice unfortunate souls caught in the blades
				List<LivingEntity> victims = level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox());
				for(Entity e : victims){
					e.hurtServer((net.minecraft.server.level.ServerLevel) level(),
                            Power.damageSource(level(), Power.PROPELLER_DAMAGE), PROPELLER_DAMAGE);
				}
			}
		}
	}
	
	@Override
	protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {
        readAdditionalSaveData(input.read(zone.moddev.mc.baseelectric.util.LegacyNbt.ROOT).orElseGet(CompoundTag::new));
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {
        CompoundTag root = new CompoundTag();
        addAdditionalSaveData(root);
        output.store(zone.moddev.mc.baseelectric.util.LegacyNbt.ROOT, root);
    }

    @Override
    public boolean hurtServer(net.minecraft.server.level.ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount) {
        return false;
    }

	protected void readAdditionalSaveData(CompoundTag root) {
		parent = null;
		parentCoord = null;
		if((root.get("parentPos") instanceof net.minecraft.nbt.IntArrayTag)){
			int[] coords = root.getIntArray("parentPos").orElseGet(() -> new int[0]);
			if(coords.length >= 3){
				parentCoord = new BlockPos(coords[0],coords[1],coords[2]);
			}
		}
	}

	protected void addAdditionalSaveData(CompoundTag root) {
		BlockPos p = parent != null ? parent.getBlockPos() : parentCoord;
		if(p != null){
			int[] coords = new int[3];
			coords[0] = p.getX();
			coords[1] = p.getY();
			coords[2] = p.getZ();
			root.putIntArray("parentPos", coords);
		}
	}
	
}
