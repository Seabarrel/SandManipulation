package me.seabarrel.SandManipulation;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.FallingBlock;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.EarthAbility;
import com.projectkorra.projectkorra.ability.ElementalAbility;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import com.projectkorra.projectkorra.util.TempBlock;

public class BurstLand {

	private boolean createSand;
	private long revertTime;
	private int radius;
	private int depth;
	
	private Location location;
	private FallingBlock fallingBlock;
	
	public BurstLand(FallingBlock fb) {
		
		FileConfiguration config = ConfigManager.getConfig();
		createSand = config.getBoolean("ExtraAbilities.Seabarrel.SandManipulation.Burst.CreateSandOnLand");
		revertTime = config.getLong("ExtraAbilities.Seabarrel.SandManipulation.Burst.CreateSandRevertTime");
		radius = config.getInt("ExtraAbilities.Seabarrel.SandManipulation.Burst.CreateSandRadius");
		depth = config.getInt("ExtraAbilities.Seabarrel.SandManipulation.Burst.CreateSandDepth");
		
		fallingBlock = fb;
		location = fb.getLocation();
		
		if (createSand) createSandSphere();
	}
	
	public void createSandSphere() {
		for (final Location l : GeneralMethods.getCircle(location, radius, depth, false, true, 0)) {
			final Block b = l.getBlock();
			
			if (ElementalAbility.isEarth(b) || ElementalAbility.isSand(b)) {
				
				if (EarthAbility.isBendableEarthTempBlock(b)) TempBlock.get(b).revertBlock();
				
				TempBlock sandSphereBlock = new TempBlock(b, fallingBlock.getBlockData().getMaterial());
				sandSphereBlock.setRevertTime(revertTime);
			}
		}
	}
	
}
