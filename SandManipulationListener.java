package me.seabarrel.SandManipulation;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityChangeBlockEvent;
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
            }
        }

    }

    @EventHandler
    public void onShift(PlayerToggleSneakEvent event) {

        if (!event.isSneaking()) return;

        Player player = event.getPlayer();
        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
        if (bPlayer == null) return;

        if (bPlayer.getBoundAbilityName().equalsIgnoreCase("SandManipulation")) {

            if (CoreAbility.hasAbility(event.getPlayer(), SandManipulation.class)) {
                CoreAbility.getAbility(event.getPlayer(), SandManipulation.class).removeWithSource();
                new SandManipulation(event.getPlayer());
            } else {
                new SandManipulation(event.getPlayer());
            }
        }

    }

    @EventHandler
    public void blockLand(EntityChangeBlockEvent event) {

        if (event.getEntityType().equals(EntityType.FALLING_BLOCK)) {
            FallingBlock fb = (FallingBlock) event.getEntity();
            if (fb.hasMetadata("sandmanipulation")) {
                if (fb.getMetadata("sandmanipulation").get(0).value().equals(true)) new BurstLand(fb.getLocation(), fb.getBlockData().getMaterial());
                fb.remove();
                event.setCancelled(true);
            }
        }
    }
}