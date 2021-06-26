package me.seabarrel.SandManipulation;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.EarthAbility;
import com.projectkorra.projectkorra.ability.ElementalAbility;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import com.projectkorra.projectkorra.util.TempBlock;

public class BurstLand {
	
	private FileConfiguration config = ConfigManager.getConfig();
	private long revertTime = config.getLong("ExtraAbilities.Seabarrel.SandManipulation.Burst.CreateSandRevertTime");
	private int radius = config.getInt("ExtraAbilities.Seabarrel.SandManipulation.Burst.CreateSandRadius");
	private int depth = config.getInt("ExtraAbilities.Seabarrel.SandManipulation.Burst.CreateSandDepth");
	
	public BurstLand(Location location, Material material) {
		createSandSphere(location, material);
	}
	
	public void createSandSphere(Location location, Material material) {
		for (final Location l : GeneralMethods.getCircle(location, radius, depth, false, true, 0)) {
			final Block b = l.getBlock();
			
			if (ElementalAbility.isEarth(b) || ElementalAbility.isSand(b)) {
				
				if (EarthAbility.isBendableEarthTempBlock(b)) TempBlock.get(b).revertBlock();
				
				TempBlock sandSphereBlock = new TempBlock(b, material);
				sandSphereBlock.setRevertTime(revertTime);
			}
		}
	}
	
}