package me.gaminglounge.deathPlugin;


import java.time.Duration;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.Chunk;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class ScoreboardManager {

    int timeInSec = 3600;
    MiniMessage mm = MiniMessage.miniMessage();
    Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
    HelperMethods hm = new HelperMethods();

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

    public void addHeadToScoreboard(UUID head) {
        // Create a new scoreboard

        if (scoreboard.getObjective("deathTimer")== null){
            scoreboard.registerNewObjective("deathTimer", Criteria.DUMMY, mm.deserialize("Death Timer"));
        }
        Objective deathTimer = scoreboard.getObjective("deathTimer");
        deathTimer.getScore(head.toString()).setScore(timeInSec);
    }

    public void startCountdownTask() {
        Objective objective = scoreboard.getObjective("deathTimer");
        if (objective == null){
            scoreboard.registerNewObjective("deathTimer", Criteria.DUMMY, mm.deserialize("Death Timer"));
        }

        Bukkit.getScheduler().runTaskTimer(
            Bukkit.getPluginManager().getPlugin("DeathPlugin"),
            () -> {
                for (String entry : scoreboard.getEntries()) {
                    int currentScore = objective.getScore(entry).getScore();
                    Entity entity = hm.getEntityByUUID(UUID.fromString(entry));
                    if (currentScore > 0) {
                        objective.getScore(entry).setScore(currentScore - 1);
                        if(entity != null){
                            for (Entity passenger : entity.getPassengers()) {
                                if (passenger instanceof TextDisplay textDisplay) {
                                    Component currentText = textDisplay.text();
                                    String serialized = mm.serialize(currentText);
                                    String updated = serialized.replaceAll("\\(\\d{1,2};\\d{1,2};\\d{1,2}\\)", getTimeLeft(entity.getUniqueId()));
                                    textDisplay.text(mm.deserialize(updated));
                                }
                            }                        
                        }
                    continue;
                    }
                    if (entity != null) {
                        for (Entity passenger : entity.getPassengers()) {
                            passenger.remove();
                        }
                        objective.getScoreboard().resetScores(entry);
                        hm.remove(entity);
                    continue;
                    }
                    Chunk chunk = hm.getChunkFromChunkLocation(hm.removeUUIDFromFile(entry));
                    World world = hm.getEntityByUUID(UUID.fromString(entry)).getWorld();
                    world.loadChunk(chunk);
                    hm.remove(entity);
                    world.unloadChunk(chunk);
                }
            },
            20L, // initial delay (1 second)
            20L  // repeat every second
        );
    }

    public String getTimeLeft(UUID uuid){
        Objective objective = scoreboard.getObjective("deathTimer");
        if (objective == null){
            scoreboard.registerNewObjective("deathTimer", Criteria.DUMMY, mm.deserialize("Death Timer"));
        }

        Duration duration = Duration.ofSeconds(objective.getScore(uuid.toString()).getScore());

        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();

        String formattedTime = "(" + hours + ";" + minutes + ";" + seconds + ")";
        return formattedTime;
    }
}
