package de.tobiyas.enderdragonsplus.entity.dragon;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.naming.OperationNotSupportedException;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_5_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_5_R2.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_5_R2.event.CraftEventFactory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

import de.tobiyas.enderdragonsplus.EnderdragonsPlus;
import de.tobiyas.enderdragonsplus.entity.dragon.age.AgeContainer;
import de.tobiyas.enderdragonsplus.entity.dragon.controllers.DragonHealthController;
import de.tobiyas.enderdragonsplus.entity.dragon.controllers.DragonMoveController;
import de.tobiyas.enderdragonsplus.entity.dragon.controllers.FireballController;
import de.tobiyas.enderdragonsplus.entity.dragon.controllers.ItemLootController;
import de.tobiyas.enderdragonsplus.entity.dragon.controllers.NBTTagDragonStore;
import de.tobiyas.enderdragonsplus.entity.dragon.controllers.NBTTagDragonStore.DragonNBTReturn;
import de.tobiyas.enderdragonsplus.entity.dragon.controllers.PropertyController;
import de.tobiyas.enderdragonsplus.entity.dragon.controllers.TargetController;

import net.minecraft.server.v1_5_R2.DamageSource;
import net.minecraft.server.v1_5_R2.Entity;
import net.minecraft.server.v1_5_R2.EntityComplexPart;
import net.minecraft.server.v1_5_R2.EntityEnderDragon;
import net.minecraft.server.v1_5_R2.EntityLiving;
import net.minecraft.server.v1_5_R2.LocaleI18n;
import net.minecraft.server.v1_5_R2.MathHelper;
import net.minecraft.server.v1_5_R2.NBTTagCompound;
import net.minecraft.server.v1_5_R2.Vec3D;
import net.minecraft.server.v1_5_R2.World;

public class LimitedEnderDragon extends EntityEnderDragon {
	
	private EnderdragonsPlus plugin = EnderdragonsPlus.getPlugin();
	public static int broadcastedError = 0;

	private int logicCall = 0;
	
	private FireballController fireballController;
	private TargetController targetController;
	private ItemLootController itemController;
	private DragonHealthController dragonHealthController;
	private DragonMoveController dragonMoveController;
	private AgeContainer ageContainer;
	private PropertyController propertyController;

	public LimitedEnderDragon(Location location, World world) {
		this(location, world, "Normal");
	}

	public LimitedEnderDragon(Location location, World world, UUID uid) {
		this(location, world, uid, "Normal");
	}

	public LimitedEnderDragon(Location location, World world, String ageType){
		super(world);

		plugin.log("Dragon Spawning: " + ageType);
		setPosition(location.getX(), location.getY(), location.getZ());
		createAllControllers(ageType, location);
	}
	
	public LimitedEnderDragon(Location location, World world, UUID uid, String ageType) {
		super(world);
		changeUUID(uid);
		
		plugin.log("Dragon Spawning: " + ageType + " uuid set");
		setPosition(location.getX(), location.getY(), location.getZ());
		createAllControllers(ageType, location);
	}

	/**
	 * This is the Craft Bukkit spawn
	 * Controllers are set from the NBTTag
	 * @param world
	 */
	public LimitedEnderDragon(World world) {
		super(world);
	}
	
	private void createAllControllers(DragonNBTReturn returnContainer){
		propertyController = new PropertyController(returnContainer);
		ageContainer = new AgeContainer(returnContainer.getAgeName());
		
		targetController = new TargetController(returnContainer.getHomeLocation(), this, ageContainer.isHostile());
		fireballController = new FireballController(targetController);
		itemController = new ItemLootController(this);
		dragonHealthController = new DragonHealthController(this);
		dragonMoveController = new DragonMoveController(this);
		
		initStats();
	}
	
	private void createAllControllers(String ageType, Location homeLocation){
		boolean hostile = plugin.interactConfig().getConfig_dragonsAreHostile();
		if(!ageType.equals(""))
			ageContainer = new AgeContainer(ageType);
		
		propertyController = new PropertyController();
		targetController = new TargetController(homeLocation, this, hostile);
		fireballController = new FireballController(targetController);
		itemController = new ItemLootController(this);
		dragonHealthController = new DragonHealthController(this);
		dragonMoveController = new DragonMoveController(this);
		
		initStats();
	}
	
	private void initStats(){
		expToDrop = ageContainer.getExp();
		setHealth(ageContainer.getSpawnHealth());
		maxHealth = ageContainer.getMaxHealth();
	}
	
	@Override
	public int getMaxHealth(){
		if(ageContainer == null) return 200; //Dirty and bad hack :(
		return ageContainer.getMaxHealth();
	}
	
	/** This method sets the Life of the EnderDragon
	 * @see net.minecraft.server.v1_5_R2.EntityEnderDragon#a()
	 */
	@Override
	protected void a() {
		super.a();
		/*  Actually doing this: 
		this.datawatcher.a(8, Integer.valueOf(0));
		this.datawatcher.a(9, Byte.valueOf(0));
		this.datawatcher.a(16, new Integer(this.getMaxHealth()));*/
	}
	
	@Override
	protected void dropDeathLoot(boolean flag, int i) {
		// CraftBukkit start - whole method
		List<ItemStack> loot = generateLoot();

        CraftEventFactory.callEntityDeathEvent(this, loot); // raise event even for those times when the entity does not drop loot
        // CraftBukkit end
    }

	

	@Override
	public String getLocalizedName() {
		return LocaleI18n.get("entity.EnderDragon.name");
	}

	
	@Override
	public boolean dealDamage(DamageSource damagesource, int i) { // CraftBukkit - protected -> public
		dragonHealthController.rememberDamage(damagesource, i);
		return super.dealDamage(damagesource, i);
	}
	

	/**
	 *  Logic call. All Dragon logic on tick
	 * @see net.minecraft.server.v1_4_R1.Enderdragon#
	 */
	@Override
	public void c(){
		try{
			internalLogicTick();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	
	@SuppressWarnings("unchecked")
	public void internalLogicTick(){
		logicCall++;
		this.bN = this.bO;
		
		int mappedHealth = dragonHealthController.mapHealth();
		boolean shouldSitDown = false;
		
		if(!plugin.interactConfig().getConfig_disableDragonHealthBar())
			this.datawatcher.watch(16, Integer.valueOf(mappedHealth));

		float f;
		float f1;
		float f2;

		if (this.health <= 0)
			return;

		dragonHealthController.checkRegainHealth();
		
		f = 0.2F / (MathHelper.sqrt(this.motX * this.motX + this.motZ
				* this.motZ) * 10.0F + 1);
		f *= (float) Math.pow(2.0D, this.motY);
		
		if (this.bQ) {
			this.bO += f * 0.5F;
		} else {
			this.bO += f;
		}

		this.yaw = MathHelper.g(this.yaw);

		if (this.e < 0) {
            for (int d05 = 0; d05 < this.d.length; ++d05) {
                this.d[d05][0] = (double) this.yaw;
                this.d[d05][1] = this.locY;
            }
        }
		
		if (++this.e == this.d.length) {
            this.e = 0;
        }

        this.d[this.e][0] = this.yaw;
        this.d[this.e][1] = this.locY;


		double d0 = this.a - this.locX;
		double d1 = this.b - this.locY;
		double d2 = this.c - this.locZ;
		double d3 = d0 * d0 + d1 * d1 + d2 * d2;
		double f3 = 0.6;
		
		Entity currentTarget = targetController.getCurrentTarget();
		boolean attackingMode = true;
		if (currentTarget != null) {
			this.a = currentTarget.locX;
			this.c = currentTarget.locZ;
			
			double d4 = this.a - this.locX;
			double d5 = this.c - this.locZ;
			double d6 = Math.sqrt(d4 * d4 + d5 * d5);
			double d7 = 0.4 + d6 / 80D - 1;

			if (d7 > 10.0D) {
				d7 = 10.0D;
			}

			this.b = currentTarget.boundingBox.b + d7;
		} else {
			shouldSitDown = plugin.interactConfig().getConfig_dragonsSitDownIfInactive();
			if(!targetController.hasTargets() && shouldSitDown){
				attackingMode = false;
				this.motX = 0;
				this.motY = 0;
				this.motZ = 0;
				
				this.a = this.locX;
				this.b = this.locY;
				this.c = this.locZ;
				this.yaw = 0;
				
				Location loc = this.getLocation().clone();
				loc.subtract(0, 1, 0);
				if(loc.getBlock().getType() == Material.AIR){
					this.motY = -0.2;
					this.b = this.locY-0.2;
				}
			}else{
				this.a += this.random.nextGaussian() * 2D;
				this.c += this.random.nextGaussian() * 2D;
			}
			
		}

		if (this.bP || (d3 < 100.0D) || d3 > 22500D || this.positionChanged
				|| this.H) {
			targetController.changeTarget(false);
		}

		d1 /= MathHelper.sqrt(d0 * d0 + d2 * d2);
		f3 = 0.6F;
		if (d1 < -f3) {
			d1 = -f3;
		}

		if (d1 > f3) {
			d1 = f3;
		}
		
		this.motY += d1 * 0.1;
        this.yaw = MathHelper.g(this.yaw);
		double d8 = 180.0D - Math.atan2(d0, d2) * 180.0D / Math.PI;
		double d9 = MathHelper.g(d8 - (double) this.yaw);

		if (d9 > 50.0D) {
			d9 = 50.0D;
		}

		if (d9 < -50.0D) {
			d9 = -50.0D;
		}

		Vec3D vec3d = this.world.getVec3DPool().create(
				this.a - this.locX, 
				this.b - this.locY, 
				this.c - this.locZ
			).a();
		
		double directionDegree = this.yaw * Math.PI / 180.0F;
        Vec3D vec3d1 = this.world.getVec3DPool().create(
        		MathHelper.sin((float) directionDegree), 
        		this.motY, 
        		-MathHelper.cos((float) directionDegree)
        	).a();
	
		float f4 = (float) (vec3d1.b(vec3d) + 0.5D) / 1.5F;

		if (f4 < 0.0F) {
			f4 = 0.0F;
		}

		this.bF *= 0.8F;
		float f5 = MathHelper.sqrt(this.motX * this.motX + this.motZ
				* this.motZ) + 1;
		double d10 = Math.sqrt(this.motX * this.motX + this.motZ * this.motZ) + 1.0D;

		if (d10 > 40.0D) {
			d10 = 40.0D;
		}

		this.bF = (float) (this.bF + d9 * (0.7 / d10 / f5));
		this.yaw += this.bF * 0.1;
		directionDegree = this.yaw * Math.PI / 180.0F; //recalculation
		float f6 = (float) (2.0D / (d10 + 1.0D));
		float f7 = 0.06F;

		this.a(0, -1.0F, f7 * (f4 * f6 + (1.0F - f6)));
		if (this.bQ) {
			this.move(this.motX * 0.8, this.motY * 0.8, this.motZ * 0.8);
		} else {
			this.move(this.motX, this.motY, this.motZ);
		}

		Vec3D vec3d2 = this.world.getVec3DPool().create(this.motX, this.motY, this.motZ).a();
		float f8 = (float) (vec3d2.b(vec3d1) + 1.0D) / 2.0F;

		f8 = 0.8F + 0.15F * f8;
		this.motX *= f8;
		this.motZ *= f8;
		this.motY *= 0.91;

		this.ay = this.yaw;
        this.g.width = this.g.length = 3.0F;
        this.i.width = this.i.length = 2.0F;
        this.j.width = this.j.length = 2.0F;
        this.bK.width = this.bL.length = 2.0F;
        this.h.length = 3.0F;
        this.h.width = 5.0F;
        this.bL.length = 2.0F;
        this.bL.width = 4.0F;
        this.bM.length = 3.0F;
        this.bM.width = 4.0F;
        
		f1 = (float) ((this.b(5, 1.0F)[1] - this.b(10, 1.0F)[1]) * 10.0F / 180.0F
				* Math.PI);
		f2 = MathHelper.cos(f1);
		float f9 = -MathHelper.sin((float) f1);
		float f10 = (float)directionDegree;
		float f11 = MathHelper.sin(f10);
		float f12 = MathHelper.cos(f10);

		this.h.l_();
		this.h.setPositionRotation(this.locX + (f11 * 0.5F),
				this.locY, this.locZ - (f12 * 0.5F), 0.0F, 0.0F);
		this.bL.l_();
		this.bL.setPositionRotation(this.locX + (f12 * 4.5F),
				this.locY + 2.0D, this.locZ + (f11 * 4.5F), 0.0F, 0.0F);
		this.bM.l_();
		this.bM.setPositionRotation(this.locX - (f12 * 4.5F),
				this.locY + 2.0D, this.locZ - (f11 * 4.5F), 0.0F, 0.0F);

		if (this.hurtTicks == 0 && attackingMode) {
			dragonMoveController.knockbackNearbyEntities(this.world.getEntities(this, this.bL.boundingBox.grow(4.0D, 2.0D, 4.0D).d(0.0D, -2.0D, 0)));
			dragonMoveController.knockbackNearbyEntities(this.world.getEntities(this, this.bM.boundingBox.grow(4.0D, 2.0D, 4.0D).d(0.0D, -2.0D, 0)));
			dragonHealthController.damageEntities(this.world.getEntities(this, this.g.boundingBox.grow(1.0D, 1.0D, 1.0D)));
		}

		// LimitedEnderDragon - begin: Added FireBalls here!
		fireballController.checkSpitFireBall();
		// LimitedEnderDragon - end

		double[] adouble = this.b(5, 1.0F);
		double[] adouble1 = this.b(0, 1.0F);

		f3 = MathHelper.sin((float) (directionDegree - this.bF * 0.01F));
		float f13 = MathHelper.cos((float)directionDegree
				- this.bF * 0.01F);

		this.g.l_();
		this.g.setPositionRotation(this.locX + (f3 * 5.5F * f2),
				this.locY + (adouble1[1] - adouble[1])
						+ (f9 * 5.5F), this.locZ
						- (f13 * 5.5F * f2), 0, 0);

		for (int j = 0; j < 3; ++j) {
			EntityComplexPart entitycomplexpart = null;

			if (j == 0) {
				entitycomplexpart = this.i;
			}

			if (j == 1) {
				entitycomplexpart = this.j;
			}

			if (j == 2) {
				entitycomplexpart = this.bK;
			}

			double[] adouble2 = this.b(12 + j * 2, 1F);
			float f14 = (float) (directionDegree + MathHelper.g(adouble2[0] - adouble[0]) * Math.PI / 180F);
			float f15 = MathHelper.sin(f14);
			float f16 = MathHelper.cos(f14);
			float f17 = 1.5F;
			float f18 = (j + 1) * 2.0F;

			entitycomplexpart.l_();
			entitycomplexpart.setPositionRotation(this.locX - ((f11 * f17 + f15 * f18) * f2), 
					this.locY + (adouble2[1] - adouble[1]) * 1.0D - ((f18 + f17) * f9) + 1.5D, 
					this.locZ + ((f12 * f17 + f16 * f18) * f2), 0.0F, 0.0F);
		}

		this.bQ = dragonMoveController.checkHitBlocks(this.g.boundingBox)
				| dragonMoveController.checkHitBlocks(this.h.boundingBox);
	}

	public boolean spitFireBallOnTarget(Entity target) {
		if (target == null)
			return false;

		fireballController.fireFireball(target);
		return true;
	}

	/** 
	 * Function to drop the EXP if a Dragon is dead
	 * ORIGINAL: aP()
	 * Moved to: ItemLootController
	 * 
	 * @see net.minecraft.server.v1_5_R2.EntityEnderDragon#aO()
	 */
	@Override
	protected void aS() {
		itemController.deathTick();
	}
	
	
	@Override
	public void a(NBTTagCompound compound){
		super.a(compound);
		DragonNBTReturn returnContainer = NBTTagDragonStore.loadFromNBT(this, compound);
		createAllControllers(returnContainer);
	}
	
	/**
	 * Saving dragon data to NBT Compound
	 * @see net.minecraft.server.v1_5_R2.EntityLiving#b(net.minecraft.server.v1_5_R2.NBTTagCompound)
	 */
	@Override
	public void b(NBTTagCompound compound){
		super.b(compound);
		NBTTagDragonStore.saveToNBT(this, compound, propertyController.getAllProperties());
	}

	public void remove() {
		getBukkitEntity().remove();
	}

	public String getName() {
		return "EnderDragon";
	}

	@Override
	public int getExpReward() {
		return plugin.interactConfig().getConfig_dropEXP();
	}

	public Location getLocation() {
		return getBukkitEntity().getLocation();
	}

	public boolean spawn() {
		return spawnCraftBukkit();
	}

	private boolean spawnCraftBukkit() {
		World world = ((CraftWorld) getLocation().getWorld()).getHandle();
		Chunk chunk = getLocation().getChunk();
		if(!chunk.isLoaded())
			this.getLocation().getChunk().load();
		
		if (!world.addEntity(this))
			return false;
		setPosition(locX, locY, locZ);
		return true;
	}	

	public Location getHomeLocation() {
		return targetController.getHomeLocation();
	}

	public int getID() {
		return getBukkitEntity().getEntityId();
	}

	public boolean isFlyingHome() {
		return targetController.isFlyingHome();
	}

	public void setTarget(LivingEntity entity) {
		EntityLiving convertedEntity = (EntityLiving) ((CraftEntity) entity).getHandle();
		targetController.forceTarget(convertedEntity);
	}

	public org.bukkit.entity.Entity getTarget() {
		Entity entity = targetController.getCurrentTarget();
		if(entity == null)
			return null;
		
		return entity.getBukkitEntity();
	}

	public int getLogicCalls() {
		int calls = logicCall;
		logicCall = 0;
		return calls;
	}

	public void goToLocation(Location location) {
		targetController.setNewTarget(location, true);
	}

	public void changeUUID(UUID uID) {
		this.uniqueID = UUID.fromString(uID.toString());
	}

	public UUID getUUID() {
		return this.getBukkitEntity().getUniqueId();
	}
	
	public Location getForceLocation(){
		return targetController.getForceGoTo();
	}
	
	public void addEnemy(org.bukkit.entity.Entity entity){
		CraftEntity craftEntity = (CraftEntity) entity;
		targetController.addTarget((EntityLiving)craftEntity.getHandle());
	}
	
	public boolean isInRange(Location loc, double range){
		return targetController.isInRange(loc, range);
	}
	
	public Map<String, Integer> getPlayerDamageDone(){
		return dragonHealthController.getPlayerDamage();
	}
	
	public String getLastPlayerAttacked(){
		return dragonHealthController.getLastPlayerAttacked();
	}

	public int getDamageByPlayer(String player) {
		return dragonHealthController.getDamageByPlayer(player);
	}

	public int getMeeleDamage() {
		return ageContainer.getDmg();
	}
	
	public List<ItemStack> generateLoot(){
		return itemController.getItemDrops(ageContainer.getDrops());
	}
	
	public String getAgeName(){
		return ageContainer.getAgeName();
	}

	public boolean isHostile() {
		return ageContainer.isHostile();
	}

	public void forceFlyHome(boolean flyingHome) {
		targetController.forceFlyingHome(flyingHome);
	}

	public void setNewHome(Location newHomeLocation) {
		targetController.setHomeLocation(newHomeLocation);
	}

	public void setProperty(String property, Object value) throws OperationNotSupportedException{
		propertyController.addProperty(property, value);
	}

	public Object getProperty(String property) {
		return propertyController.getProperty(property);
	}
}