package me.seabarrel.SandManipulation;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.SandAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import com.projectkorra.projectkorra.util.BlockSource;
import com.projectkorra.projectkorra.util.ClickType;
import com.projectkorra.projectkorra.util.TempBlock;

public class SandManipulation extends SandAbility implements AddonAbility{

	@Attribute(Attribute.COOLDOWN)
	private long cooldown;
	@Attribute(Attribute.RANGE)
	private int range;
	@Attribute(Attribute.SELECT_RANGE)
	private int sourceRange;
	@Attribute("AMOUNT")
	private int amount;
	
	private Block sourceBlock;
	private Material sourceType;
	private Location location;
	private boolean started;
	
	public SandManipulation(Player player) {
		super(player);
		
		if (bPlayer.isOnCooldown(this)) return;
		
		cooldown = ConfigManager.getConfig().getLong("ExtraAbilities.Seabarrel.SandManipulation.Cooldown");
		range = ConfigManager.getConfig().getInt("ExtraAbilities.Seabarrel.SandManipulation.Range");
		sourceRange = ConfigManager.getConfig().getInt("ExtraAbilities.Seabarrel.SandManipulation.SourceRange");
		amount = ConfigManager.getConfig().getInt("ExtraAbilities.Seabarrel.SandManipulation.Blocks");
		
		location = player.getLocation();
		
		sourceBlock = BlockSource.getEarthSourceBlock(player, sourceRange, ClickType.SHIFT_DOWN);
		if (!isSand(sourceBlock)) return;
		focusBlock();
		start();
	}
	
	@Override
	public void progress() {
		
		if (this.sourceBlock.getLocation().distanceSquared(this.player.getLocation()) > this.sourceRange * this.sourceRange) {
			sourceBlock.setType(sourceType);
			remove();
		}
		
		if (!started) return;
		removeWithCooldown();
		return;
	}
	
	public void onClick() {
		if (!started) {
			sourceBlock.setType(sourceType);
			TempBlock tb = new TempBlock(sourceBlock, Material.AIR);
			tb.setRevertTime(10000);
			started = true;
		} else {
			
		}
	}
	
	public void removeWithCooldown() {
		bPlayer.addCooldown("SandManipulation", cooldown);
		remove();
	}
	
	public void removeWithSource() {
		sourceBlock.setType(sourceType);
		remove();
	}
	
	private void focusBlock() {
		if (sourceBlock.getType() == Material.SAND) {
			sourceType = Material.SAND;
			sourceBlock.setType(Material.SANDSTONE);
		} else if (sourceBlock.getType() == Material.RED_SAND) {
			sourceType = Material.RED_SAND;
			sourceBlock.setType(Material.RED_SANDSTONE);
		} else {
			sourceType = sourceBlock.getType();
			sourceBlock.setType(Material.SAND);
		}

		this.location = this.sourceBlock.getLocation();
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
		return "sand fluid yeye";
	}
	
	@Override
	public String getInstructions() {
		return "Tab sneak on a sandbendable block and then hold shift and left click to manipulate the sand.";
	}
	
	@Override
	public boolean isHarmlessAbility() {
		return true;
	}
	
	@Override
	public boolean isHiddenAbility() {
		return false;
	}
	
	@Override
	public boolean isSneakAbility() {
		return false;
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
		ConfigManager.getConfig().addDefault("ExtraAbilities.Seabarrel.SandManipulation.Cooldown", 6000);
		ConfigManager.getConfig().addDefault("ExtraAbilities.Seabarrel.SandManipulation.Range", 20);
		ConfigManager.getConfig().addDefault("ExtraAbilities.Seabarrel.SandManipulation.SourceRange", 10);
		ConfigManager.getConfig().addDefault("ExtraAbilities.Seabarrel.SandManipulation.blocks", 5);
		ConfigManager.defaultConfig.save();
		
		ProjectKorra.plugin.getServer().getPluginManager().registerEvents(new SandManipulationListener(), ProjectKorra.plugin);
		
		ProjectKorra.plugin.getLogger().info("Successfully enabled " + getName() + " " + getVersion() + " by " + getAuthor());
		
	}
	
	@Override
	public void stop() {
		remove();
	}
}
