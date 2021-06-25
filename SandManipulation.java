package me.seabarrel.SandManipulation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.SandAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.command.Commands;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.TempBlock;

public class SandManipulation extends SandAbility implements AddonAbility{

	@Attribute(Attribute.COOLDOWN)
	private long cooldown;
	@Attribute(Attribute.RANGE)
	private int range;
	@Attribute(Attribute.SELECT_RANGE)
	private int sourceRange;
	@Attribute("CREATE_SAND_ON_LAND")
	private boolean createSandOnLand;
	@Attribute(Attribute.RADIUS)
	private int radius;
	@Attribute("DEPTH")
	private int depth;
	@Attribute(Attribute.DURATION)
	private long revertTime;
	@Attribute(Attribute.DAMAGE)
	private double hitDamage;
	@Attribute("BLINDNESS")
	private boolean blindness;
	@Attribute("BLINDNESS_DURATION")
	private long blindnessDuration;
	@Attribute("BURST_COOLDOWN")
	private long burstCooldown;
	@Attribute("AMOUNT")
	private int amount;
	@Attribute("VELOCITY")
	private int velocity;
	
	private Block block;
	private TempBlock sourceBlock;
	private Material sourceType;
	private Material shootType;
	private Location location;
	private boolean started;
	private Location targetLocation;
	private Vector direction;
	private long start;
	private boolean replace;
	private boolean entitiesAreHurt;
	private boolean burst;
	private long burstStart;
	private FallingBlock fb;
	private ArrayList<FallingBlock> burstBlocks;
	
	public SandManipulation(Player player) {
		super(player);
		
		if (bPlayer.isOnCooldown(this)) return;
		
		FileConfiguration config = ConfigManager.getConfig();
		cooldown = config.getLong("ExtraAbilities.Seabarrel.SandManipulation.Cooldown");
		range = config.getInt("ExtraAbilities.Seabarrel.SandManipulation.Range");
		sourceRange = config.getInt("ExtraAbilities.Seabarrel.SandManipulation.SourceRange");
		createSandOnLand = config.getBoolean("ExtraAbilities.Seabarrel.SandManipulation.CreateSandOnLand");
		radius = config.getInt("ExtraAbilities.Seabarrel.SandManipulation.CreateSandRadius");
		depth = config.getInt("ExtraAbilities.Seabarrel.SandManipulation.CreateSandDepth");
		revertTime = config.getLong("ExtraAbilities.Seabarrel.SandManipulation.CreateSandRevertTime");
		
		hitDamage = config.getInt("ExtraAbilities.Seabarrel.SandManipulation.Damage");
		blindness = config.getBoolean("ExtraAbilities.Seabarrel.SandManipulation.Blindness");
		blindnessDuration = config.getLong("ExtraAbilities.Seabarrel.SandManipulation.BlindnessDuration");
		
		burstCooldown = config.getLong("ExtraAbilities.Seabarrel.SandManipulation.Burst.Cooldown");
		amount = config.getInt("ExtraAbilities.Seabarrel.SandManipulation.Burst.Blocks");
		velocity = config.getInt("ExtraAbilities.Seabarrel.SandManipulation.Burst.Velocity");
		
		if (prepare()) {
			start();
		} else {
			remove();
		}
	}
	
	@Override
	public void progress() {

		if (sourceBlock.getLocation().distanceSquared(player.getLocation()) > sourceRange * sourceRange && !started) {
			removeWithSource();
		}
		
		if (!bPlayer.getBoundAbilityName().equalsIgnoreCase(getName())) {
			if (started) {
				removeWithCooldown();
				return;
			} else {
				removeWithSource();
				return;
			}
		}
		
		if (!started) return;
		
		if (burst) {
			burst();
			return;
		}
		
		HashSet<Material> ignoredMaterials = getTransparentMaterialSet();
		ignoredMaterials.add(Material.SAND);
		
		final Entity target = GeneralMethods.getTargetedEntity(player, range);
		targetLocation = player.getTargetBlock(ignoredMaterials, range).getLocation();
		
		if (target != null) {
			targetLocation = target.getLocation();
		}
		
		
		direction = GeneralMethods.getDirection(location, targetLocation).normalize();
		location = this.location.clone().add(direction);
		
		if (location.distanceSquared(player.getLocation()) > range * range) {
			removeWithCooldown();
			return;
		}
		
		if (!player.isSneaking()) {
			removeWithCooldown();
			return;
		}
		
		final List<Entity> entities = GeneralMethods.getEntitiesAroundPoint(location, 1);
		for (final Entity entity : entities) {
			
			if ((entity instanceof Player && Commands.invincible.contains(entity.getName()))
					|| entity instanceof ArmorStand || entity instanceof FallingBlock) {
				
				continue;
				
			} else {
				
				entitiesAreHurt = true;
				DamageHandler.damageEntity(entity, hitDamage, this);
				
				if (blindness && entity instanceof Player) {
					final Player l = (Player) entity;
					l.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, (int)(blindnessDuration / 1000 * 20), 2));
				}
			}

		}
		if (entitiesAreHurt) removeWithCooldown();
		
		boolean canMoveHere = false;
		boolean replace = true;
		if (isAir(location.getBlock().getType())
				|| isSand(location.getBlock().getType())
					|| isTransparent(location.getBlock())) {
			if (!isWater(location.getBlock())) canMoveHere = true;
		}
		
		if (!canMoveHere && System.currentTimeMillis() - start < 150
				&& sourceBlock.getLocation().distanceSquared(location) < 4) {
			replace = false;
			canMoveHere = true;
		}
		
		if (isSand(location.getBlock())) replace = false; 
		
		if (!canMoveHere && createSandOnLand) createSandSphere(); 
		
		if (!canMoveHere) {
			removeWithCooldown();
			return;
		}
	
		
		if (location == sourceBlock.getLocation()) return;
		
		if (replace) {
			TempBlock t = new TempBlock(location.getBlock(), shootType);
			t.setRevertTime(300);
		}
		playSandbendingSound(location);
		
		return;
	}
	
	public void createSandSphere() {
		for (final Location l : GeneralMethods.getCircle(location, radius, depth, false, true, 0)) {
			final Block b = l.getBlock();
			if (isEarthbendable(b) || isSandbendable(b)) {
				
				if (isBendableEarthTempBlock(b)) TempBlock.get(b).revertBlock();
				
				TempBlock sandSphereBlock = new TempBlock(b, shootType);
				sandSphereBlock.setRevertTime(revertTime);
			}
		}
	}
	
	public void burst() {

		if (System.currentTimeMillis() - burstStart < 100) return;

		if (burstBlocks == null) {
			int x;
			for (x = 0 ; x < amount ; x++) {
				fb = location.getWorld().spawnFallingBlock(location, shootType.createBlockData());
				fb.setMetadata("sandmanipulation", new FixedMetadataValue(ProjectKorra.plugin, 1));
				fb.setVelocity(getRandomVelocity(player.getLocation().getDirection()));
				fb.setDropItem(false);
				
				if (burstBlocks == null) {
					burstBlocks = new ArrayList<FallingBlock>();
				}
				burstBlocks.add(fb);
			}

		} else {
			
			if (burstBlocks.size() > 0) {
				for (FallingBlock fallingBlock : burstBlocks) {
					
				}
			} else {
				remove();
			}
		}
	}
	
	public Vector getRandomVelocity(Vector dir) {
		double x = (Math.round(Math.random()) * 2 - 1 ) * Math.random() / 2 + dir.getX();
		double y = (Math.round(Math.random()) * 2 - 1 ) * Math.random() / 2 + dir.getY();
		double z = (Math.round(Math.random()) * 2 - 1 ) * Math.random() / 2 + dir.getZ();
		return new Vector(x, y, z).multiply(velocity);
	}
	
	public void onClick() {

		if (!started) {
			sourceBlock.revertBlock();
			sourceBlock = new TempBlock(block, Material.AIR);
			sourceBlock.setRevertTime(10000);
			started = true;
			start = System.currentTimeMillis();
			
		} else if (!burst) {
			
			burst = true;
			burstStart = System.currentTimeMillis();
			bPlayer.addCooldown(this, burstCooldown);
		}
	}
	
	public void removeWithCooldown() {
		bPlayer.addCooldown("SandManipulation", cooldown);
		remove();
	}
	
	public void removeWithSource() {
		sourceBlock.revertBlock();
		remove();
	}
	
	private boolean prepare() {
		block = getEarthSourceBlock(sourceRange);
		if (block == null) return false;
		if (!isSand(block)) return false;
		if (block.getType() == Material.SAND) {
			
			sourceType = Material.SANDSTONE;
			shootType = Material.SAND;
			
		} else if (block.getType() == Material.RED_SAND) {
			
			sourceType = Material.RED_SANDSTONE;
			shootType = Material.RED_SAND;
			
		} else if (block.getType() == Material.RED_SANDSTONE) {
			
			sourceType = Material.RED_SAND;
			shootType = Material.RED_SAND;
			
		} else {
			
			sourceType = Material.SAND;
			shootType = Material.SAND;
			
		}
		
		location = block.getLocation();
		sourceBlock = new TempBlock(block, sourceType);
		return true;
	}
	
	@Override
	public long getCooldown() {
		return cooldown;
	}
	
	@Override
	public Location getLocation() {
		return location;
	}
	
	@Override
	public String getName() {
		return "SandManipulation";
	}
	
	@Override
	public String getDescription() {
		return "A sand move using waterbending styles to manipulate the sand in a fluid-ish state!";
	}
	
	@Override
	public String getInstructions() {
		return "Hold sneak on a sandbendable block and then left click to manipulate the sand. \n(Burst) Left click while manipulating the sand to make the sand blocks fly in random directions creating a burst!";
	}
	
	@Override
	public boolean isHarmlessAbility() {
		return false;
	}
	
	@Override
	public boolean isHiddenAbility() {
		return false;
	}
	
	@Override
	public boolean isSneakAbility() {
		return true;
	}
	
	@Override
	public String getAuthor() {
		return "Seabarrel";
	}
	
	@Override
	public String getVersion() {
		return "1.0.0";
	}
	
	@Override
	public void load() {
		FileConfiguration config = ConfigManager.getConfig();
		config.addDefault("ExtraAbilities.Seabarrel.SandManipulation.Cooldown", 5000);
		config.addDefault("ExtraAbilities.Seabarrel.SandManipulation.Range", 20);
		config.addDefault("ExtraAbilities.Seabarrel.SandManipulation.SourceRange", 10);
		config.addDefault("ExtraAbilities.Seabarrel.SandManipulation.CreateSandOnLand", true);
		config.addDefault("ExtraAbilities.Seabarrel.SandManipulation.CreateSandRadius", 5);
		config.addDefault("ExtraAbilities.Seabarrel.SandManipulation.CreateSandDepth", 2);
		config.addDefault("ExtraAbilities.Seabarrel.SandManipulation.CreateSandRevertTime", 10000);
		
		config.addDefault("ExtraAbilities.Seabarrel.SandManipulation.Damage", 2.0);
		config.addDefault("ExtraAbilities.Seabarrel.SandManipulation.Blindness", true);
		config.addDefault("ExtraAbilities.Seabarrel.SandManipulation.BlindnessDuration", 2000);
		
		config.addDefault("ExtraAbilities.Seabarrel.SandManipulation.Burst.Cooldown", 10000);
		config.addDefault("ExtraAbilities.Seabarrel.SandManipulation.Burst.Blocks", 10);
		config.addDefault("ExtraAbilities.Seabarrel.SandManipulation.Burst.Damage", 1);
		config.addDefault("ExtraAbilities.Seabarrel.SandManipulation.Burst.CreateSandOnLand", true);
		config.addDefault("ExtraAbilities.Seabarrel.SandManipulation.Burst.CreateSandRadius", 3);
		config.addDefault("ExtraAbilities.Seabarrel.SandManipulation.Burst.CreateSandDepth", 2);
		config.addDefault("ExtraAbilities.Seabarrel.SandManipulation.Burst.CreateSandRevertTime", 8000);
		config.addDefault("ExtraAbilities.Seabarrel.SandManipulation.Burst.Velocity", 1);
		ConfigManager.defaultConfig.save();
		
		ProjectKorra.plugin.getServer().getPluginManager().registerEvents(new SandManipulationListener(), ProjectKorra.plugin);
		
		ProjectKorra.plugin.getLogger().info("Successfully enabled " + getName() + " " + getVersion() + " by " + getAuthor());
		
	}
	
	@Override
	public void stop() {
		remove();
	}
}
