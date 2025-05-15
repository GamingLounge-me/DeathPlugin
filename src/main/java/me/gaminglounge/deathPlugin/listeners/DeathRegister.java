package me.gaminglounge.deathPlugin.listeners;

import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockType;

import net.kyori.adventure.text.minimessage.MiniMessage;

public class DeathRegister implements Listener {

    MiniMessage mm = MiniMessage.miniMessage();

    @EventHandler
    public void onDeath(PlayerDeathEvent event){
        Player player = event.getPlayer();
        String reason = event.deathMessage().examinableName();
        String time = String.valueOf(System.currentTimeMillis());

        BlockDisplay headDisplay = player.getWorld().spawn(player.getLocation(), BlockDisplay.class);
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
        skullMeta.setOwningPlayer(player);
        head.setItemMeta(skullMeta);
        headDisplay.setBlock(head);

        TextDisplay infoDisplay = player.getWorld().spawn(player.getLocation(), TextDisplay.class);
        headDisplay.addPassenger(infoDisplay)
        infoDisplay.text(mm.deserialize(player.getName() + "\n" +
                "<red>Death by: " + reason + "\n" +
                "<blue>Time: " + timeLeft));
    }
}
