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
    private Scoreboard scoreboard;

    public Scoreboard getScoreboard() {
        if (scoreboard == null) {
            scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        }
        return scoreboard;
    }

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
        if (getScoreboard().getObjective("deathTimer") == null) {
            getScoreboard().registerNewObjective("deathTimer", Criteria.DUMMY, mm.deserialize("Death Timer"));
        }
        Objective deathTimer = getScoreboard().getObjective("deathTimer");
        deathTimer.getScore(head.toString()).setScore(timeInSec);
    }

    public void startCountdownTask() {
        if (getScoreboard().getObjective("deathTimer") == null) {
            getScoreboard().registerNewObjective("deathTimer", Criteria.DUMMY, mm.deserialize("Death Timer"));
        }
        Objective objective = getScoreboard().getObjective("deathTimer");

        Bukkit.getScheduler().runTaskTimer(
            Bukkit.getPluginManager().getPlugin("DeathPlugin"),
            () -> {
                for (String entry : getScoreboard().getEntries()) {
                    int currentScore = objective.getScore(entry).getScore();
                    Entity entity = DeathPlugin.INSTANCE.hm.getEntityByUUID(UUID.fromString(entry));
                    if (currentScore > 0) {
                        objective.getScore(entry).setScore(currentScore - 1);
                        if (entity != null) {
                            for (Entity passenger : entity.getPassengers()) {
                                if (passenger instanceof TextDisplay textDisplay) {
                                    Component currentText = textDisplay.text();
                                    String serialized = mm.serialize(currentText);
                                    String updated = serialized.replaceAll("\\(\\d{1,2}:\\d{1,2}:\\d{1,2}\\)", getTimeLeft(entity.getUniqueId()));
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
                        DeathPlugin.INSTANCE.hm.remove(entity);
                        continue;
                    }
                    Chunk chunk = DeathPlugin.INSTANCE.hm.getChunkFromChunkLocation(DeathPlugin.INSTANCE.hm.removeUUIDFromFile(entry));
                    World world = DeathPlugin.INSTANCE.hm.getEntityByUUID(UUID.fromString(entry)).getWorld();
                    world.loadChunk(chunk);
                    DeathPlugin.INSTANCE.hm.remove(entity);
                    world.unloadChunk(chunk);
                }
            },
            20L, // initial delay (1 second)
            20L  // repeat every second
        );
    }

    public String getTimeLeft(UUID uuid) {
        if (getScoreboard().getObjective("deathTimer") == null) {
            getScoreboard().registerNewObjective("deathTimer", Criteria.DUMMY, mm.deserialize("Death Timer"));
        }
        Objective objective = getScoreboard().getObjective("deathTimer");

        Duration duration = Duration.ofSeconds(objective.getScore(uuid.toString()).getScore());

        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();

        String formattedTime = "(" + hours + ":" + minutes + ":" + seconds + ")";
        return formattedTime;
    }
    public void loadScoreboard(){
        DeathPlugin.INSTANCE.hm.loadScoreboard("deathTimer");
        return;
    }
    public void saveScoreboard(){
        if (scoreboard == null)return;
        if (getScoreboard().getObjective("deathTimer") == null)return;
        Objective objective = getScoreboard().getObjective("deathTimer");
        DeathPlugin.INSTANCE.hm.saveScoreboard(getScoreboard(), objective);
        return;
    }
}
