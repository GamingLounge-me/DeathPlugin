package me.gaminglounge.deathPlugin;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.kyori.adventure.text.minimessage.MiniMessage;

public class HelperMethods {

    NamespacedKey key = new NamespacedKey("deathplugin", "death_inventory");
    static InputStream file = DeathPlugin.INSTANCE.getResource("Heads.json");
    MiniMessage mm = MiniMessage.miniMessage();

    public Entity getEntityByUUID(UUID uuid) {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getUniqueId().equals(uuid)) {
                    return entity;
                }
            }
        }
        return null;
    }


    //This is the part, where we have to load the inventorry and drop it.
    public void remove(Entity entity){
        ItemStack[] items = retrieveInventory(entity, key);
        for (ItemStack item : items) {
            if (item != null && item.getType() != org.bukkit.Material.AIR) {
                entity.getWorld().dropItemNaturally(entity.getLocation(), item);
            }
        }
        removeUUIDFromFile(entity.getUniqueId().toString());
        entity.remove();
    }

    public static void addUUIDToFile(String uuid, ChunkLocation chunk) {
        Gson gson = new Gson();
        File folder = DeathPlugin.INSTANCE.getDataFolder();
        if (!folder.exists()) {
            folder.mkdirs();
        }
        File file = new File(folder, "Heads.json");

        // Step 1: Read existing map from file
        Map<String, ChunkLocation> dataMap = new HashMap<>();
        if (file.exists()) {
            try (FileReader reader = new FileReader(file, java.nio.charset.StandardCharsets.UTF_8)) {
                dataMap = gson.fromJson(reader, new TypeToken<Map<String, ChunkLocation>>() {}.getType());
                if (dataMap == null) dataMap = new HashMap<>();
            } catch(IOException e){
                e.printStackTrace();
            }
        }

        // Step 2: Add or overwrite the chunk location for this UUID
        dataMap.put(uuid, chunk);

        // Step 3: Write the updated map back to the file
        try (FileWriter writer = new FileWriter(file, java.nio.charset.StandardCharsets.UTF_8)) {
            gson.toJson(dataMap, writer);
        } catch(IOException e){
            e.printStackTrace();
        }
    }


    @Nullable
    public ChunkLocation removeUUIDFromFile(String uuid) {
        Gson gson = new Gson();
        File folder = DeathPlugin.INSTANCE.getDataFolder();
        if (!folder.exists()) {
            folder.mkdirs();
        }
        File file = new File(folder, "Heads.json");

        // Step 1: Read existing map from file
        Map<String, ChunkLocation> dataMap = new HashMap<>();
        if (file.exists()) {
            try (FileReader reader = new FileReader(file, java.nio.charset.StandardCharsets.UTF_8)) {
                dataMap = gson.fromJson(reader, new TypeToken<Map<String, ChunkLocation>>() {}.getType());
                if (dataMap == null) dataMap = new HashMap<>();
            } catch(IOException e){
                e.printStackTrace();
            }
        }

        // Step 2: Remove and get the chunk location
        ChunkLocation removedChunk = dataMap.remove(uuid);
        if (removedChunk == null) return null;

        // Step 3: Write the updated map back to the file
        try (FileWriter writer = new FileWriter(file, java.nio.charset.StandardCharsets.UTF_8)) {
            gson.toJson(dataMap, writer);
        } catch(IOException e){
            e.printStackTrace();
        }
        return removedChunk;
    }

    public static ChunkLocation getChunkLocationFromChunk(Chunk chunk) {
        return new ChunkLocation(chunk.getX(), chunk.getZ(), chunk.getWorld().getName());
    }  

    public Chunk getChunkFromChunkLocation(ChunkLocation location) {
        World world = Bukkit.getWorld(location.getWorldName());
        if (world == null) return null;

        return world.getChunkAt(location.getChunkX(), location.getChunkZ());
    }

    public void saveScoreboard(Scoreboard scoreboard, Objective objective) {
        Map<String, Integer> dataMap = new HashMap<>();
        for (String entry : scoreboard.getEntries()) {
            Score score = objective.getScore(entry);
            if(score.isScoreSet()){
                dataMap.put(entry, score.getScore());
            }
        }
        Gson gson = new Gson();
        File folder = DeathPlugin.INSTANCE.getDataFolder();
        if (!folder.exists()) {
            folder.mkdirs(); // Ensure the plugin data folder exists
        }
        File file = new File(folder, objective.getName() + "_Scoreboard.json");
        try (FileWriter writer = new FileWriter(file, java.nio.charset.StandardCharsets.UTF_8)) {
            gson.toJson(dataMap, writer);
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    public void loadScoreboard(String objectiveName) {
        Gson gson = new Gson();
        Map<String, Integer> dataMap = new HashMap<>();
        File file = new File(DeathPlugin.INSTANCE.getDataFolder(), objectiveName + "_Scoreboard.json");

        if (file.exists()) {
            try (FileReader reader = new FileReader(file, java.nio.charset.StandardCharsets.UTF_8)) {
                dataMap = gson.fromJson(reader, new TypeToken<Map<String, Integer>>() {}.getType());
                if (dataMap == null) dataMap = new HashMap<>();
                // debugging
                DeathPlugin.INSTANCE.getLogger().log(Level.WARNING, "At this point the map should be loaded");
                DeathPlugin.INSTANCE.getLogger().log(Level.WARNING, dataMap.toString());
            } catch(IOException e) {
                e.printStackTrace();
            }
        }

        Scoreboard scoreboard = DeathPlugin.INSTANCE.sm.getScoreboard();
        if (scoreboard.getObjective("deathTimer") == null) {
            scoreboard.registerNewObjective("deathTimer", Criteria.DUMMY, mm.deserialize("Death Timer"));
        }
        Objective objective = scoreboard.getObjective(objectiveName);
        for(String id : dataMap.keySet()){
            Integer value = dataMap.get(id);
            objective.getScore(id).setScore(value);
        }
    }

    //https://docs.papermc.io/paper/dev/custom-inventory-holder/
    // Serialize ItemStack array to Base64 using DataOutputStream and serializeAsBytes
    public static String serializeItems(ItemStack[] items) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            out.writeInt(items.length); // Save array length
            int nonAirCount = 0;
            for (ItemStack item : items) {
                if (item != null && item.getType() != org.bukkit.Material.AIR) {
                    nonAirCount++;
                }
            }
            out.writeInt(nonAirCount); // Save number of non-air items
            for (int i = 0; i < items.length; i++) {
                ItemStack item = items[i];
                if (item != null && item.getType() != org.bukkit.Material.AIR) {
                    out.writeInt(i); // Save slot index
                    byte[] data = item.serializeAsBytes();
                    out.writeInt(data.length);
                    out.write(data);
                }
            }
            out.close();
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }


    //This method is to copy the inventorry from the given Player to the given Entity using PDC.
    public void copyInventory (Player player, Entity target, NamespacedKey key) {
        target.getPersistentDataContainer().set(
            key,
            PersistentDataType.STRING,
            serializeItems(player.getInventory().getContents())
        );
    }

    // Deserialize ItemStack array from Base64 using DataInputStream and deserializeBytes
    public static ItemStack[] deserializeItems(String data) {
        try {
            byte[] bytes = Base64.getDecoder().decode(data);
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            DataInputStream in = new DataInputStream(bais);
            int length = in.readInt();
            ItemStack[] items = new ItemStack[length];
            int nonAirCount = in.readInt();
            for (int j = 0; j < nonAirCount; j++) {
                int i = in.readInt(); // Slot index
                int itemLen = in.readInt();
                byte[] itemBytes = new byte[itemLen];
                in.readFully(itemBytes);
                items[i] = ItemStack.deserializeBytes(itemBytes);
            }
            in.close();
            return items;
        } catch (IOException e) {
            e.printStackTrace();
            return new ItemStack[0];
        }
    }
    //This method returns the Itemstack from the inventory.
    public ItemStack[] retrieveInventory (Entity entity, NamespacedKey key) {
    PersistentDataContainer container = entity.getPersistentDataContainer();
    
    String data = container.get(key, PersistentDataType.STRING);
        if (data == null || data.isEmpty()) {
            return new ItemStack[0]; // Or null, depending on your use case
        }

        try {
            return deserializeItems(data); // Your custom method from earlier
        } catch (Exception e) {
            e.printStackTrace();
            return new ItemStack[0];
        }
    }
}