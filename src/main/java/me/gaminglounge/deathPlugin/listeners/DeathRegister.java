package me.gaminglounge.deathPlugin.listeners;

import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.util.Vector;

import me.gaminglounge.deathPlugin.DeathPlugin;
import me.gaminglounge.deathPlugin.HelperMethods;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class DeathRegister implements Listener {

    NamespacedKey key = new NamespacedKey("deathplugin", "death_inventory");
    NamespacedKey xp_key = new NamespacedKey("deathplugin", "death_xp");
    MiniMessage mm = MiniMessage.miniMessage();
    HelperMethods hm = DeathPlugin.hm;
    //this only get's used once, but the compiler should see this.
    Vector north = new Vector(0, 0, -1);


    //Logic that defines Blocks the Head can spawn on, lava is there and not watter, cause we want it to go underwater, but not under lava and also want it to respect slabs and so on.
    private boolean isStandable(Material type) {
        return type.isSolid() ||
           type == Material.MUD ||
           type == Material.MUDDY_MANGROVE_ROOTS ||
           type == Material.FARMLAND ||
           type == Material.DIRT_PATH ||
           type == Material.SOUL_SAND ||
           type == Material.SOUL_SOIL ||
           type == Material.LAVA ||
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
        //ignores empty graves
        if (player.getInventory().isEmpty() & event.getDroppedExp()<= 1)return;

        String reason;
        if (event.deathMessage() != null) {
            reason = mm.serialize(event.deathMessage());
        } else {
            reason = "died";
        }

        Block currentBlock = player.getLocation().getBlock();
        int minHeight = currentBlock.getWorld().getMinHeight() + 4;
        EntityDamageEvent lastDamage = player.getLastDamageCause();
        Location graveLocation = null;
        int attempts = 0;

        Location defaultGraveLocation = player.getLocation().clone();
        defaultGraveLocation.setY(Math.max(4, defaultGraveLocation.getY())); // Fallback if needed

        //This loop just moves the position of the grave block by block, till the attempts run out.
        while (attempts++ < 50) {
            Block belowBlock = currentBlock.getRelative(0, -1, 0);
            Material currentType = currentBlock.getType();

            // If the current block is lava, move up until out of lava
            if (currentType == Material.LAVA || currentType == Material.LAVA_CAULDRON) {
                currentBlock = currentBlock.getRelative(0, 1, 0);
                continue;
            }

            // If suffocated, keep grave where they died to not have weired grave placements.
            if (lastDamage != null && lastDamage.getCause() == EntityDamageEvent.DamageCause.SUFFOCATION) {
                graveLocation = player.getLocation();
                break;
            }

            // If we find solid ground below and air at current or are underwater — place grave here
            if (
                isStandable(currentType) &&
                (currentBlock.getRelative(0, 1, 0).getType() == Material.AIR
                || currentBlock.getRelative(0, 1, 0).getType() == Material.WATER
                || currentBlock.getRelative(0, 1, 0).getType() == Material.SEA_PICKLE
                || currentBlock.getRelative(0, 1, 0).getType().isSolid() == false)
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
            graveLocation = new Location(player.getWorld(), currentBlock.getX() + 0.5, minHeight, currentBlock.getZ() + 0.5);
            break;
        }

        // Fallback if nothing valid found
        if (graveLocation == null) {
            graveLocation = defaultGraveLocation;
            Bukkit.getLogger().log(Level.WARNING, "Grave fallback used at: {0}", graveLocation);
        }


        // Now use graveLocation for the rest of your logic
        ArmorStand headDisplay = player.getWorld().spawn(graveLocation.add(0.5, -1.4375, 0.5), ArmorStand.class);
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
        hm.saveXP(event.getDroppedExp(), headDisplay, xp_key);

        DeathPlugin.sm.addHeadToScoreboard(headDisplay.getUniqueId());
        hm.addUUIDToFile(headDisplay.getUniqueId().toString(), hm.getChunkLocationFromChunk(headDisplay.getChunk()));

        TextDisplay infoDisplay = player.getWorld().spawn(graveLocation.setDirection(north), TextDisplay.class);
        headDisplay.addPassenger(infoDisplay);
        infoDisplay.setViewRange(20);
        infoDisplay.setBillboard(Billboard.VERTICAL);
        //editing this part can break the counter logic cause it is saved in the text, not the actual world or anything else.
        infoDisplay.text(mm.deserialize(player.getName() + "\n" +
        "<red>"+ reason + "\n" +
        "<blue>Time left: " + DeathPlugin.sm.getTimeLeft(headDisplay.getUniqueId())));

        //stop the player from dropping Items and XP
        event.getDrops().clear();
        event.setDroppedExp(0);
    }
}
