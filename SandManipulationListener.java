package me.seabarrel.SandManipulation;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;

import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.ability.CoreAbility;

public class SandManipulationListener implements Listener {
	@EventHandler
	public void onClick(PlayerInteractEvent event) {
			
		if (event.getHand() != EquipmentSlot.HAND
				|| (event.getAction() != Action.LEFT_CLICK_AIR 
					&& event.getAction() != Action.LEFT_CLICK_BLOCK))
			return;
		
		Player player = event.getPlayer();
		BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
		if (bPlayer == null) return;

		
		if (bPlayer.getBoundAbilityName().equalsIgnoreCase("SandManipulation")) {
			
			if (CoreAbility.hasAbility(event.getPlayer(), SandManipulation.class)) {
				CoreAbility.getAbility(event.getPlayer(), SandManipulation.class).onClick();
			} else {
				new SandManipulation(event.getPlayer());
			}
		}
		
		}
	
	@EventHandler
	public void onClick(PlayerToggleSneakEvent event) {
			
		if (!event.isSneaking()) return;
		
		Player player = event.getPlayer();
		BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
		if (bPlayer == null) return;

		
		if (bPlayer.getBoundAbilityName().equalsIgnoreCase("SandManipulation")) {
			
			if (CoreAbility.hasAbility(event.getPlayer(), SandManipulation.class)) {
				CoreAbility.getAbility(event.getPlayer(), SandManipulation.class).onSource();
			} else {
				new SandManipulation(event.getPlayer());
			}
		}
		
		}
}
