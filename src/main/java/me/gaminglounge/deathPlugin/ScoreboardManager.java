package me.gaminglounge.deathPlugin;


import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import net.kyori.adventure.text.minimessage.MiniMessage;

public class ScoreboardManager {

    int timeInSec = 3600;
    MiniMessage mm = MiniMessage.miniMessage();
    Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();

public Entity getEntityFromUUID(UUID uuid) {
    // Try to get as a player first
    Player player = Bukkit.getPlayer(uuid);
    if (player != null) {
        return player;
    }
    // Otherwise, search all worlds for the entity
    for (World world : Bukkit.getWorlds()) {
        Entity entity = world.getEntity(uuid);
        if (entity != null) {
            return entity;
        }
    }
    return null; // Not found
}

    public void addHeadToScoreboard(UUID head, UUID playerUUID) {
        // Create a new scoreboard

        if (scoreboard.getObjective("deathTimer")== null){
            scoreboard.registerNewObjective("deathTimer", Criteria.DUMMY, mm.deserialize("Death Timer"));
        }
        
        Objective objective = scoreboard.getObjective("deathTimer");
        objective.getScore(head.toString()).setScore(timeInSec);
    }
    public void startCountdownTask() {
        Objective objective = scoreboard.getObjective("deathTimer");
        if (objective == null) return;

        Bukkit.getScheduler().runTaskTimer(
            Bukkit.getPluginManager().getPlugin("DeathPlugin"),
            () -> {
                for (String entry : scoreboard.getEntries()) {
                    int currentScore = objective.getScore(entry).getScore();
                    if (currentScore > 0) {
                        objective.getScore(entry).setScore(currentScore - 1);
                    }
                    else{
                        UUID uuid = entry.toString();
                    }
                }
            },
            20L, // initial delay (1 second)
            20L  // repeat every second
        );
    }
}
