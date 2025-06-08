package me.gaminglounge.deathPlugin.listeners;

import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.util.Vector;

import me.gaminglounge.deathPlugin.DeathPlugin;
import me.gaminglounge.deathPlugin.HelperMethods;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;

import net.kyori.adventure.text.minimessage.MiniMessage;

public class DeathRegister implements Listener {

    NamespacedKey key = new NamespacedKey("deathplugin", "death_inventory");
    MiniMessage mm = MiniMessage.miniMessage();
    HelperMethods hm = DeathPlugin.hm;
    Vector north = new Vector(0, 0, -1);

    private boolean isStandable(Material type) {
        return type.isSolid() ||
           type == Material.MUD ||
           type == Material.MUDDY_MANGROVE_ROOTS ||
           type == Material.FARMLAND ||
           type == Material.DIRT_PATH ||
           type == Material.SOUL_SAND ||
           type == Material.SOUL_SOIL ||
           type.name().contains("SLAB") ||
           type.name().contains("STAIRS") ||
           type.name().contains("CARPET") ||
           type.name().contains("PRESSURE_PLATE") ||
           type.name().contains("FENCE") ||
           type.name().contains("WALL") ||
           type.name().contains("TRAPDOOR") ||
           type.name().contains("RAIL");
    }

@EventHandler
    public void onDeath(PlayerDeathEvent event){
        Player player = event.getPlayer();

        if (player.getInventory().isEmpty() & event.getDroppedExp()<= 1)return;

        String reason = mm.serialize(event.deathMessage());

        Block currentBlock = player.getLocation().getBlock();
        int minHeight = currentBlock.getWorld().getMinHeight() + 4;
        EntityDamageEvent lastDamage = player.getLastDamageCause();
        Location graveLocation = null;
        int attempts = 0;

        Location defaultGraveLocation = player.getLocation().clone();
        defaultGraveLocation.setY(Math.max(4, defaultGraveLocation.getY())); // Fallback if needed


        while (attempts++ < 50) {
            Block belowBlock = currentBlock.getRelative(0, -1, 0);
            Material currentType = currentBlock.getType();

            // If the current block is lava, move up until out of lava
            if (currentType == Material.LAVA || currentType == Material.LAVA_CAULDRON) {
                currentBlock = currentBlock.getRelative(0, 1, 0);
                continue;
            }

            // If suffocated, keep grave where they died
            if (lastDamage != null && lastDamage.getCause() == EntityDamageEvent.DamageCause.SUFFOCATION) {
                graveLocation = player.getLocation();
                break;
            }

            // If we find solid ground below and air at current — place grave here
            if (
                isStandable(currentType) &&
                currentBlock.getRelative(0, 1, 0).getType() == Material.AIR
            ) {
                graveLocation = currentBlock.getLocation().add(0, 1, 0); // Place grave just above current
                break;
            }

            // If still above min height, keep going down
            if (currentBlock.getY() > minHeight) {
                currentBlock = belowBlock;
                continue;
            }

            // If we hit min height and nothing worked, place grave there
            graveLocation = new Location(player.getWorld(), currentBlock.getX(), minHeight, currentBlock.getZ());
            break;
        }

            // Fallback if nothing valid found
        if (graveLocation == null) {
            graveLocation = defaultGraveLocation;
            Bukkit.getLogger().warning("Grave fallback used at: " + graveLocation);
        }


        // Now use graveLocation for the rest of your logic
        ArmorStand headDisplay = player.getWorld().spawn(graveLocation.add(0, -1.4375, 0), ArmorStand.class);
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
        skullMeta.setOwningPlayer(player);
        head.setItemMeta(skullMeta);

        //armorestand settings
        headDisplay.setVisible(false);
        headDisplay.setMarker(false);
        headDisplay.setGravity(false);
        headDisplay.setInvulnerable(true);
        headDisplay.getEquipment().setHelmet(head);

        hm.copyInventory(player, headDisplay, key);

        DeathPlugin.sm.addHeadToScoreboard(headDisplay.getUniqueId());
        hm.addUUIDToFile(headDisplay.getUniqueId().toString(), hm.getChunkLocationFromChunk(headDisplay.getChunk()));

        TextDisplay infoDisplay = player.getWorld().spawn(graveLocation.setDirection(north), TextDisplay.class);
        headDisplay.addPassenger(infoDisplay);
        infoDisplay.setViewRange(20);
        infoDisplay.setBillboard(Billboard.VERTICAL);
        infoDisplay.text(mm.deserialize(player.getName() + "\n" +
        "<red>"+ reason + "\n" +
        "<blue>Time left: " + DeathPlugin.sm.getTimeLeft(headDisplay.getUniqueId())));

        //stop the player from dropping Items
        event.getDrops().clear();
    }
}
