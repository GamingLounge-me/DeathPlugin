package me.gaminglounge.deathPlugin.listeners;

import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.util.Vector;

import me.gaminglounge.deathPlugin.DeathPlugin;
import me.gaminglounge.deathPlugin.HelperMethods;


import org.bukkit.Material;


import net.kyori.adventure.text.minimessage.MiniMessage;

public class DeathRegister implements Listener {

    MiniMessage mm = MiniMessage.miniMessage();
    HelperMethods hm = new HelperMethods();
    Vector north = new Vector(0, 0, -1);


    @EventHandler
    public void onDeath(PlayerDeathEvent event){
        Player player = event.getPlayer();
        String reason = mm.serialize(event.deathMessage());

        ArmorStand headDisplay = player.getWorld().spawn(player.getLocation(), ArmorStand.class);
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
        skullMeta.setOwningPlayer(player);
        head.setItemMeta(skullMeta);

        headDisplay.setVisible(false);
        headDisplay.setMarker(false);
        headDisplay.setGravity(false);
        headDisplay.setInvulnerable(true);
        headDisplay.getEquipment().setHelmet(head);

        DeathPlugin.sm.addHeadToScoreboard(headDisplay.getUniqueId());
        hm.addUUIDToFile(headDisplay.getUniqueId().toString(), hm.getChunkLocationFromChunk(headDisplay.getChunk()));

        TextDisplay infoDisplay = player.getWorld().spawn(player.getLocation().setDirection(north), TextDisplay.class);
        headDisplay.addPassenger(infoDisplay);
        infoDisplay.setViewRange(20);
        infoDisplay.setBillboard(Billboard.VERTICAL);
        infoDisplay.text(mm.deserialize(player.getName() + "\n" +
                "<red>Death by: " + reason + "\n" +
                "<blue>Time left: " + DeathPlugin.sm.getTimeLeft(headDisplay.getUniqueId())));
    }
}
