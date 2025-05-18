package me.gaminglounge.deathPlugin;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public class HelperMethods {

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

    public void remove(Entity entity){
        if(entity instanceof InventoryHolder iH){
            Inventory inventory = iH.getInventory();
            for(ItemStack item: inventory.getContents()){
                if(item != null){
                    entity.getWorld().dropItemNaturally(entity.getLocation(), item);
                }
            }
        }
        removeUUIDFromFile(entity.getUniqueId().toString());
        remove(entity);
    }

public static void addUUIDToFile(String uuid, ChunkLocation chunk) {
    File file = new File("src/main/resources/Heads.json");
    Gson gson = new GsonBuilder().setPrettyPrinting().create();

    try {
        // Step 1: Read existing map from file
        Map<String, ChunkLocation> dataMap = new HashMap<>();

        if (file.exists()) {
            try (Reader reader = new FileReader(file)) {
                dataMap = gson.fromJson(reader, new TypeToken<Map<String, ChunkLocation>>() {}.getType());
                if (dataMap == null) dataMap = new HashMap<>();
            }
        }

        // Step 2: Add or overwrite the chunk location for this UUID
        dataMap.put(uuid, chunk);

        // Step 3: Write the updated map back to the file
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(dataMap, writer);
        }

    } catch (IOException e) {
        e.printStackTrace();
    }
}


    @Nullable
    public ChunkLocation removeUUIDFromFile(String uuid) {
        File file = new File("src/main/resources/Heads.json");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        try {
            // Step 1: Read existing map from file
            Map<String, ChunkLocation> dataMap = new HashMap<>();

            if (file.exists()) {
                try (Reader reader = new FileReader(file)) {
                    dataMap = gson.fromJson(reader, new TypeToken<Map<String, ChunkLocation>>() {}.getType());
                    if (dataMap == null) dataMap = new HashMap<>();
                }
            }

            // Step 2: Remove and get the chunk location
            ChunkLocation removedChunk = dataMap.remove(uuid);
            if (removedChunk == null) return null;

            // Step 3: Write the updated map back to the file
            try (FileWriter writer = new FileWriter(file)) {
                gson.toJson(dataMap, writer);
            }

            return removedChunk;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static ChunkLocation getChunkLocationFromChunk(Chunk chunk) {
        return new ChunkLocation(chunk.getX(), chunk.getZ(), chunk.getWorld().getName());
    }  

    public Chunk getChunkFromChunkLocation(ChunkLocation location) {
        World world = Bukkit.getWorld(location.getWorldName());
        if (world == null) return null;

        return world.getChunkAt(location.getChunkX(), location.getChunkZ());
    }
}