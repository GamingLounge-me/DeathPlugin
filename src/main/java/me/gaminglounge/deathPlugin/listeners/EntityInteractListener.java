package me.gaminglounge.deathPlugin.listeners;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import me.gaminglounge.deathPlugin.DeathPlugin;
import me.gaminglounge.deathPlugin.HelperMethods;
import me.gaminglounge.deathPlugin.SavedInventoryHolder;
import me.gaminglounge.deathPlugin.ScoreboardManager;

public class EntityInteractListener implements Listener {

    ScoreboardManager sm = DeathPlugin.sm;
    HelperMethods hm = DeathPlugin.hm;
    NamespacedKey key = new NamespacedKey("deathplugin", "death_inventory");

    private boolean isTrackedArmorStand(Entity entity) {
        if (!(entity instanceof ArmorStand)) return false;
        Scoreboard scoreboard = DeathPlugin.INSTANCE.sm.getScoreboard();
        Objective objective = scoreboard.getObjective("deathTimer");
        if (objective == null) return false;
        return scoreboard.getEntries().contains(entity.getUniqueId().toString());
    }

    @EventHandler
    public void onEntityClick(PlayerInteractAtEntityEvent event) {
        Entity target = event.getRightClicked();
        Player player = event.getPlayer();

        if (!(target instanceof ArmorStand)) return;
        if (!isTrackedArmorStand(target)) return;

        event.setCancelled(true); // Block default interactions (like removing equipment)

        if (player.isSneaking()) {
            // Sneak right-click: remove grave
            hm.remove(target);
        } else {
            // Normal right-click: open inventory GUI
            hm.openSavedInventoryGUI(player, target, key);
        }
    }

    @EventHandler
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        if (isTrackedArmorStand(event.getRightClicked())) {
            event.setCancelled(true); // Prevent moving or removing equipment
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof SavedInventoryHolder) {
            event.setCancelled(true); // Prevent dragging items in saved GUI
        }
    }
}

