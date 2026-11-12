package zone.moddev.mc.baseelectric.machines;

import zone.moddev.mc.baseelectric.ElectricAdvantage;
import zone.moddev.mc.baseelectric.init.Power;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.scores.Team;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public class LaserTurretTileEntity extends ElectricMachineTileEntity{

	private final SoundEvent LASER_SOUND;

	private static final float RADIANS_TO_DEGREES = (float)(180 / Math.PI);
	private static final float DEGREES_TO_RADIANS = (float)(Math.PI / 180);
	public final static float IDLE_ROTATION_PER_TICK = 2f; // in degrees per tick
	public static final byte LASER_CHARGEUP_TIME = 31;
	private static final byte BLAST_TIME = 10;
	private static final byte HIT_TIME = 7;
	private final static int BURN_TIME = 3;
	public static float ATTACK_DAMAGE = 7f;
	
	public final static float ENERGY_PER_SHOT = 1000;
	public final static float ENERGY_PER_TICK = 1;
	public final static float MAX_BUFFER = ENERGY_PER_SHOT * 2;
	
	private String teamIdentity = null;
	private String playerOwner = null;

	public static final int BLAME_RANGE = 4;
	public static final int TARGET_RANGE = 16;
	public static final int ATTACK_RANGE = 32;
	/** target rotation around Y axis */
	public float rotTargetYaw=0;
	/** target rotation up and down */
	public float rotTargetPitch=0;
	/** rotation around Y axis */
	public float rotYaw=0;
	/** rotation up and down */
	public float rotPitch=0;
	/** previous rotation around Y axis */
	public float rotOldYaw=0;
	/** previous rotation up and down */
	public float rotOldPitch=0;
	/** true when tracking a target, alse otherwise */
	public boolean targetLocked = false;
	/** entityID of target */
	private int targetID = Integer.MIN_VALUE;
	/** laser attack charge-up */
	private int laserAttack = 0;
	/** used by renderer to draw laser line */
	public double laserBlastLength = 0;
	
	private Vec3 opticPosition = null;
	
	/** rotation speed (non-idle), in degrees per tick */
	public final float speed = 9f;


	public LaserTurretTileEntity(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state){
		super(zone.moddev.mc.baseelectric.init.Entities.LASER_TURRET, pos, state, "tile.baseelectric.laser_turret.name",MAX_BUFFER,0,0,0);
		SoundEvent soundEvent = ElectricAdvantage.INSTANCE == null ? SoundEvents.NOTE_BLOCK_BASS.value()
				: ForgeRegistries.SOUND_EVENTS.getValue(Identifier.parse(ElectricAdvantage.INSTANCE.LASER_SOUND));
		if(soundEvent == null){
			LASER_SOUND = SoundEvents.ENDERMAN_TELEPORT;
		} else {
			LASER_SOUND = soundEvent;
		}
	}

	@Override
	public boolean isActive(){
		return  (this.getEnergy() >= ENERGY_PER_SHOT) && (!this.hasRedstoneSignal());
	}
	
	@Override
	public void tickUpdate(boolean b) {
		Level w = getLevel();
		if(laserAttack > 0){
			laserAttack--;
		}
		boolean isActive = isActive();
		if(w.isClientSide()){
			// client-side only
			if(isActive){
				if(targetLocked == false){
					storeOld();
					rotPitch = 0;
					rotYaw += IDLE_ROTATION_PER_TICK;
					if(rotYaw > 180 ){
						rotYaw -= 360;
						rotOldYaw -= 360;
					}
				} else {
					storeOld();
					Entity e = w.getEntity(targetID);
					if(e != null){
						lookAt(e);
					}
					if((rotTargetYaw - rotYaw) > 180){
						rotYaw += 360;
						rotOldYaw += 360;
					} else if((rotTargetYaw - rotYaw) < -180){
						rotYaw -= 360;
						rotOldYaw -= 360;
					}
					rotYaw = clampDelta(rotYaw,rotTargetYaw,speed);
					rotPitch = clampDelta(rotPitch,rotTargetPitch,speed);
					
					// Laser attack animation
					if(laserAttack > 0 && laserAttack <= BLAST_TIME){
						drawLaserLine();
					}
				}
			} else {
				storeOld();
				if(rotPitch > -30){
					rotPitch -= IDLE_ROTATION_PER_TICK;
				}
			}
			
		} else {
			// Server side only
			if(isActive){
				if(targetLocked == false){
					final Block thisBlock = getLevel().getBlockState(getBlockPos()).getBlock();
					final boolean isEvil;
					if(thisBlock instanceof LaserTurretBlock
							&& ((LaserTurretBlock)thisBlock).evil){
						isEvil = true;
					} else {
						isEvil = false;
					}
					double x = this.getOpticPosition().x;
					double y = this.getOpticPosition().y;
					double z = this.getOpticPosition().z;
					List<LivingEntity> entities = w.getEntitiesOfClass(LivingEntity.class, new AABB(x-TARGET_RANGE, y-TARGET_RANGE, z-TARGET_RANGE, x+TARGET_RANGE, y+TARGET_RANGE, z+TARGET_RANGE));
					final double maxRangeSquared = TARGET_RANGE * TARGET_RANGE;
					if(isEvil){
						for (Entity e : entities) {
							if (!(
									e.getType().getCategory() == MobCategory.MONSTER
									|| e.getType().getCategory() == MobCategory.AMBIENT
								)
							) {
								if (canSeeEntity(e)) {
									setTarget(e);
									break;
								}
							}
						}
					} else {
						for (Entity e : entities) {
							if (e.position().distanceToSqr(getOpticPosition()) > maxRangeSquared) continue;
							if (e instanceof Player) {
								if (isEnemy((Player) e)) {
									setTarget(e);
									break;
								}
							}
							if (e.getType().getCategory() == MobCategory.MONSTER) {
								if (canSeeEntity(e)) {
									setTarget(e);
									break;
								}
							}
						}
					}
				} 

				validateTarget();
				if(targetLocked ){
					if(laserAttack == HIT_TIME){
						// dish out the laser damage
						playSoundEffect(getOpticPosition().x, getOpticPosition().y, getOpticPosition().z,
								LASER_SOUND,1F,1F);
						List<Entity> victims = getLaserAttackVictims();
						Entity e = w.getEntity(targetID);
						for(Entity v : victims){
							hurtVictim(v);
						}
						this.subtractEnergy(ENERGY_PER_SHOT, getEnergyType());
						if(e == null || !e.isAlive()){
							setTarget(null);
						}
						this.sync();
					} else if(laserAttack == 1){
						laserAttack = LASER_CHARGEUP_TIME;
						this.sync();
					}
				}
				this.subtractEnergy(ENERGY_PER_TICK, getEnergyType());
			} else {
				if(targetLocked){
					setTarget(null);
				}
			}

		}
	}
	
	private boolean isEnemy(Player player) {
		if(player.isCreative()) return false;
		if(this.teamIdentity == null || this.teamIdentity.isEmpty()){
			return false;
		} else if(player.getTeam() == null){
			return true;
		} else{
			return !teamIdentity.equals(player.getTeam().getName());
		}
	}

	
	public boolean showLaserLine(){
		return targetLocked && laserAttack > 0 && laserAttack <= BLAST_TIME;
	}
	
	
	private List<Entity> getLaserAttackVictims(){
		final Vec3 origin = getOpticPosition();
		final Vec3 dir;
		final Level w = getLevel();
		final Entity e = w.getEntity(targetID);
		if(e != null){
			dir = e.position().add(0, 0.5*e.getBbHeight(), 0).subtract(origin).normalize();
		} else {
			dir = (new Vec3(Math.cos(DEGREES_TO_RADIANS * rotYaw),Math.sin(rotPitch),Math.sin(rotYaw))).normalize();
		}
		final Vec3 terminus = followRayToSolidBlock(origin,dir,ATTACK_RANGE);
		final double maxDistSqr = origin.distanceToSqr(terminus);
		final double maxDist = Math.sqrt(maxDistSqr);
		List<LivingEntity> potentialVictims = w.getEntitiesOfClass(LivingEntity.class, new AABB(
				origin.x - maxDist, origin.y - maxDist, origin.z - maxDist,
				origin.x + maxDist, origin.y + maxDist, origin.z + maxDist));
		List<Entity> victims = new ArrayList<>();
		for(Entity v : potentialVictims){
			if(origin.distanceToSqr(v.position()) < maxDistSqr
					&& rayIntersectsBoundingBox(origin,dir,v.getBoundingBox())){
				victims.add(v);
			}
		}
		return victims;
	}
	
	

	private void hurtVictim(Entity e){
		e.igniteForSeconds(BURN_TIME);
		if(e instanceof LivingEntity){
			((LivingEntity)e).hurtServer((net.minecraft.server.level.ServerLevel) getLevel(),
                    Power.damageSource(getLevel(), Power.LASER_DAMAGE), ATTACK_DAMAGE);
			BlockPos bp = e.blockPosition();
			if(getLevel().isEmptyBlock(bp) && !getLevel().isEmptyBlock(bp.below())){
				getLevel().setBlockAndUpdate(bp, Blocks.FIRE.defaultBlockState());
			}
			if(playerOwner != null && !playerOwner.isEmpty()){
				for (Player player : getLevel().players()) {
					if (playerOwner.equals(player.getName().getString())) {
						((LivingEntity) e).setLastHurtByMob(player);
						break;
					}
				}
			}
		}
	}
	
	private Vec3 calculateLaserTerminus(){
		Vec3 dir;
		Level w = getLevel();
		Entity e = w.getEntity(targetID);
		if(e != null){
			dir = e.position().add(0, 0.5*e.getBbHeight(), 0).subtract(this.getOpticPosition()).normalize();
		} else {
			dir = (new Vec3(Math.cos(DEGREES_TO_RADIANS * rotYaw),Math.sin(rotPitch),Math.sin(rotYaw))).normalize();
		}
		return followRayToSolidBlock(getOpticPosition(),dir,ATTACK_RANGE);
	}
	private Vec3 followRayToSolidBlock(Vec3 origin, Vec3 dir, double maxRange){
		Vec3 max = origin.add(maxRange * dir.x, maxRange * dir.y, maxRange * dir.z);
		BlockHitResult impact = getLevel().clip(new ClipContext(origin, max,
				ClipContext.Block.COLLIDER, ClipContext.Fluid.SOURCE_ONLY, CollisionContext.empty()) {
			@Override public VoxelShape getBlockShape(BlockState state, BlockGetter world, BlockPos pos) {
				// The optic is inside our collision post; ignore only this block, not nearby cover.
				return pos.equals(LaserTurretTileEntity.this.getBlockPos()) ? Shapes.empty()
						: super.getBlockShape(state, world, pos);
			}
		});
		if(impact.getType() == HitResult.Type.BLOCK){
			return impact.getLocation();
		} else {
			return max;
		}
	}
	
	public static boolean rayIntersectsBoundingBox(Vec3 rayOrigin, Vec3 rayDirection, AABB box){
		if(box == null) {
			return false;
		}
		// algorithm from http://gamedev.stackexchange.com/questions/18436/most-efficient-aabb-vs-ray-collision-algorithms
		Vec3 inverse = new Vec3(1.0 / rayDirection.x, 1.0 / rayDirection.y, 1.0 / rayDirection.z);
		double t1 = (box.minX - rayOrigin.x)*inverse.x;
		double t2 = (box.maxX- rayOrigin.x)*inverse.x;
		double t3 = (box.minY - rayOrigin.y)*inverse.y;
		double t4 = (box.maxY - rayOrigin.y)*inverse.y;
		double t5 = (box.minZ - rayOrigin.z)*inverse.z;
		double t6 = (box.maxZ - rayOrigin.z)*inverse.z;

		double tmin = max(max(min(t1, t2), min(t3, t4)), min(t5, t6));
		double tmax = min(min(max(t1, t2), max(t3, t4)), max(t5, t6));

		// if tmax < 0, ray (line) is intersecting AABB, but whole AABB is behind us
		if (tmax < 0)
		{
			return false;
		}

		// if tmin > tmax, ray doesn't intersect AABB
		if (tmin > tmax)
		{
			return false;
		}

		return true;
	}

	
	private static Vec3 mul(Vec3 a, double b){
		return new Vec3(a.x * b, a.y * b, a.z * b);
	}
	private static double max(double a, double b){
		return Math.max(a, b);
	}
	private static double min(double a, double b){
		return Math.min(a, b);
	}
	
	
	private void drawLaserLine() {
		// client-side only
		Level w = getLevel();
		if(w.isClientSide()){
			this.laserBlastLength = getOpticPosition().distanceTo(calculateLaserTerminus());
		}
	}

	private void validateTarget() {
		if(targetLocked){
			Entity e = getLevel().getEntity(targetID);
			if(e == null || !e.isAlive()){
				setTarget(null);
				return;
			}
			
			if(getOpticPosition().distanceTo(e.position()) > TARGET_RANGE){
				setTarget(null);
				return;
			}
			
			if(!canSeeEntity(e)){
				setTarget(null);
				return;
			}
		}
	}
	
	public boolean canSeeEntity(Entity e){
		if(e == null) return false;
		Vec3 offsetOrigin = getOpticPosition().add(e.position().subtract(getOpticPosition()).normalize());
		BlockHitResult collision = getLevel().clip(new ClipContext(offsetOrigin,
				e.position().add(0, e.getBbHeight()*0.5, 0), ClipContext.Block.COLLIDER,
				ClipContext.Fluid.SOURCE_ONLY, CollisionContext.empty()));
		if(collision.getType() == HitResult.Type.BLOCK){
			return false;
		}
		return true;
	}

	public void setTarget(Entity e){
		if(e == null){
			this.targetID = Integer.MIN_VALUE;
			this.targetLocked = false;
			laserAttack = 0;
		} else {
			this.targetID = e.getId();
			this.targetLocked = true;
			laserAttack = LASER_CHARGEUP_TIME;
			lookAt(e);
		}
		this.sync();
	}

	private void lookAt(Entity e) {
		Vec3 pos = e.position().add(0, e.getBbHeight()*0.5, 0);
		targetPosition(pos);
	}
	
	private Vec3 getOpticPosition(){
		if(opticPosition == null)setOpticPosition();
		return opticPosition;
	}
	
	public void setOwner(Player owner){
		this.setTeam(owner.getTeam());
		playerOwner = owner.getName().getString();
	}
	
	public void setTeam(Team t){
		if(t == null) {
			teamIdentity = null;
		} else {
			teamIdentity = t.getName();
		}
	}
	
	public final void setOpticPosition(){
		BlockPos p = getBlockPos();
		opticPosition = new Vec3(p.getX()+0.5,p.getY()+0.75,p.getZ()+0.5);
	}
	
	private void targetPosition(Vec3 coord) {
		Vec3 pos = getOpticPosition();
		double x = pos.x;
		double y = pos.y;
		double z = pos.z;
		double dist = distance(x,y,z,coord.x, coord.y, coord.z);
		this.rotTargetYaw = RADIANS_TO_DEGREES * atan2(coord.z - z, coord.x - x);
		this.rotTargetPitch = RADIANS_TO_DEGREES * asin((coord.y - y)/dist);
	}
	
	private double distance(double x1,double y1,double z1,double x2,double y2,double z2){
		double dx = x2-x1;
		double dy = y2-y1;
		double dz = z2-z1;
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	private float atan2(double dy, double dx){
		return (float)Math.atan2(dy, dx);
	}
	
	private float asin(double d){
		return (float)Math.asin(d);
	}

	private static float clampDelta(float old, float goal, float maxDelta){
		float delta = goal - old;
		if(Math.abs(delta) > delta){
			if(delta < 0){
				delta = -1*maxDelta;
			} else {
				delta = maxDelta;
			}
			return old + delta;
		}else{
			return goal;
		}
	}
	
	private void storeOld(){
		rotOldYaw = rotYaw;
		rotOldPitch = rotPitch;
	}
	
	
	
	

	/**
	 * Turns the data field NBT into a network packet
	 */
	@Override 
	public ClientboundBlockEntityDataPacket getUpdatePacket(){
		if (getLevel() == null) return null;
		return ClientboundBlockEntityDataPacket.create(this, (tile, registries) -> {
			CompoundTag tag = new CompoundTag();
			writeRenderSync(tag);
			return tag;
		});
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag nbtTag = super.getUpdateTag(registries);
		writeRenderSync(nbtTag);
		return nbtTag;
	}

	@Override
	public void handleUpdateTag(net.minecraft.world.level.storage.ValueInput tag, HolderLookup.Provider registries) {
		super.handleUpdateTag(tag, registries);
		readRenderSync(tag.read(zone.moddev.mc.baseelectric.util.LegacyNbt.ROOT).orElseGet(CompoundTag::new));
	}

	private void writeRenderSync(CompoundTag nbtTag) {
		nbtTag.putFloat("energy", getEnergy());
		nbtTag.putBoolean("lock", targetLocked);
		if(targetLocked){
			if(targetID != Integer.MIN_VALUE && getLevel().getEntity(targetID) != null){
				Entity e = getLevel().getEntity(targetID);
				Vec3 pos = e.position().add(0, e.getBbHeight()*0.5, 0);
				CompoundTag target = new CompoundTag();
				target.putInt("id", this.targetID);
				target.putFloat("yaw", rotTargetYaw);
				target.putFloat("pitch", rotTargetPitch);
				target.putByte("attack", (byte)laserAttack);
				nbtTag.put("target", target);
			}
		}
	}

	/**
	 * Receives the network packet made by <code>getDescriptionPacket()</code>
	 */
	@Override
	public void onDataPacket(Connection net, net.minecraft.world.level.storage.ValueInput data, HolderLookup.Provider registries) {
        readRenderSync(data.read(zone.moddev.mc.baseelectric.util.LegacyNbt.ROOT).orElseGet(CompoundTag::new));
    }

    @Override
	public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
		readRenderSync(packet.getTag());
	}

	private void readRenderSync(CompoundTag tag) {
		if((tag.get("lock") instanceof net.minecraft.nbt.ByteTag)){
			this.targetLocked = tag.getBooleanOr("lock", false);
			if((tag.get("target") instanceof net.minecraft.nbt.CompoundTag)){
				CompoundTag target = tag.getCompoundOrEmpty("target");
				this.targetID = target.getIntOr("id", 0);
				Entity e = getLevel().getEntity(targetID);
				if(e != null){
					this.lookAt(e);
				} else {
					this.rotTargetYaw = target.getFloatOr("yaw", 0F);
					this.rotTargetPitch = target.getFloatOr("pitch", 0F);
				}
				this.laserAttack = target.getByteOr("attack", (byte) 0);
			}
		}
		if((tag.get("energy") instanceof net.minecraft.nbt.NumericTag)){
			this.setEnergy(tag.getFloatOr("energy", 0F),getEnergyType());
		}
	}

	@Override
	public float[] getProgress() {
		// not used
		return new float[0];
	}

	@Override
	protected void saveTo(CompoundTag root) {
		root.putFloat("pitch", rotPitch);
		root.putFloat("yaw", rotYaw);
		root.putBoolean("lock", targetLocked);
		if(this.teamIdentity == null){
			root.putString("team", "");
		} else {
			root.putString("team", teamIdentity);
		}
		if(this.playerOwner == null){
			root.putString("player", "");
		} else {
			root.putString("player", playerOwner);
		}
	}

	@Override
	protected void loadFrom(CompoundTag root) {
		if((root.get("pitch") instanceof net.minecraft.nbt.NumericTag)){
			this.rotPitch = root.getFloatOr("pitch", 0F);
			this.rotOldPitch = rotPitch;
			this.rotTargetPitch = rotPitch;
		}
		if((root.get("yaw") instanceof net.minecraft.nbt.NumericTag)){
			this.rotYaw = root.getFloatOr("yaw", 0F);
			this.rotOldYaw = rotYaw;
			this.rotTargetYaw = rotYaw;
		}
		if((root.get("lock") instanceof net.minecraft.nbt.ByteTag)){
			targetLocked = root.getBooleanOr("lock", false);
		}
		if((root.get("team") instanceof net.minecraft.nbt.StringTag)){
			String teamName = root.getStringOr("team", "");
			if(teamName.isEmpty()){
				this.teamIdentity = null;
			}else{
				this.teamIdentity = teamName;
			}
		}

		if((root.get("player") instanceof net.minecraft.nbt.StringTag)){
			playerOwner = root.getStringOr("player", "");
		}
	}

	@Override
	protected boolean isValidInputItem(ItemStack item) {
		return false;
	}

	@Override
	public int[] getDataFieldArray() {
		// not used
		return new int[0];
	}

	@Override
	public void onDataFieldUpdate() {
		// not used
	}

	@Override
	public void prepareDataFieldsForSync() {
		// not used
	}

	@Override
	public boolean isPowered() {
		return getEnergy() > ENERGY_PER_TICK;
	}
	
	@Override
	public int getComparatorOutput(){
		if(this.targetLocked){
			return 15;
		} else {
			return (int)(8 * (this.getEnergy() / this.getEnergyCapacity()));
		}
	}
	
	// Helps with laser rendering
	final private int renderRange = ATTACK_RANGE * 2;
	public AABB getRenderBoundingBox()
	{
		return new AABB(getBlockPos()).inflate(renderRange);
	}
}
