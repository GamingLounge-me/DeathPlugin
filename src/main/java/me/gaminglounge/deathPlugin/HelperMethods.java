package me.gaminglounge.deathPlugin;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
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

    NamespacedKey inventory_key = new NamespacedKey("deathplugin", "death_inventory");
    NamespacedKey xp_key = new NamespacedKey("deathplugin", "death_xp");
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
        ItemStack[] items = retrieveInventory(entity, inventory_key);
        for (ItemStack item : items) {
            if (item != null && item.getType() != org.bukkit.Material.AIR) {
                entity.getWorld().dropItemNaturally(entity.getLocation().add(0, 1.9375, 0), item);
            }
        }
        Integer xp = entity.getPersistentDataContainer().get(xp_key, PersistentDataType.INTEGER);
        if (xp != null && xp > 0) {
            entity.getWorld().spawn(entity.getLocation().add(0, 1.9375, 0), org.bukkit.entity.ExperienceOrb.class, orb -> orb.setExperience(xp));
        }
        removeUUIDFromFile(entity.getUniqueId().toString());

        // Remove from scoreboard
        Scoreboard scoreboard = DeathPlugin.INSTANCE.sm.getScoreboard();
        scoreboard.resetScores(entity.getUniqueId().toString());
        for (Entity passenger : entity.getPassengers()) {
            passenger.remove();
        }
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
                DeathPlugin.INSTANCE.getLogger().log(Level.SEVERE, "Error while reading Heads.json", e);
            }
        }

        // Step 2: Add or overwrite the chunk location for this UUID
        dataMap.put(uuid, chunk);

        // Step 3: Write the updated map back to the file
        try (FileWriter writer = new FileWriter(file, java.nio.charset.StandardCharsets.UTF_8)) {
            gson.toJson(dataMap, writer);
        } catch(IOException e){
            DeathPlugin.INSTANCE.getLogger().log(Level.SEVERE, "Error while writing to Heads.json", e);
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
                DeathPlugin.INSTANCE.getLogger().log(Level.SEVERE, "Error while reading Heads.json", e);
            }
        }

        // Step 2: Remove and get the chunk location
        ChunkLocation removedChunk = dataMap.remove(uuid);
        if (removedChunk == null) return null;

        // Step 3: Write the updated map back to the file
        try (FileWriter writer = new FileWriter(file, java.nio.charset.StandardCharsets.UTF_8)) {
            gson.toJson(dataMap, writer);
        } catch(IOException e){
            DeathPlugin.INSTANCE.getLogger().log(Level.SEVERE, "Error while writing to Heads.json", e);
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
            DeathPlugin.INSTANCE.getLogger().log(Level.SEVERE, "Error while writing to Scoreboard.json", e);
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
            DeathPlugin.INSTANCE.getLogger().log(Level.SEVERE, "Error while reading Scoreboard.json", e);
            }
        }

        Scoreboard scoreboard = DeathPlugin.INSTANCE.sm.getScoreboard();
        if (scoreboard.getObjective("deathTimer") == null) {
            scoreboard.registerNewObjective("deathTimer", Criteria.DUMMY, mm.deserialize("Death Timer"));
        }
        Objective objective = scoreboard.getObjective(objectiveName);
        if (objective != null) {
            for(String id : dataMap.keySet()){
                Integer value = dataMap.get(id);
                objective.getScore(id).setScore(value);
            }
        } else {
            DeathPlugin.INSTANCE.getLogger().log(Level.SEVERE, "Objective ''{0}'' not found on scoreboard.", objectiveName);
        }
    }

    //https://docs.papermc.io/paper/dev/custom-inventory-holder/
    // Serialize main, armor, and offhand items
    public static String serializeItems(PlayerInventory inv) {
        ItemStack[] contents = inv.getContents(); // includes main, armor, and offhand in modern Paper
        return serializeItems(contents);
    }

    // Overload for ItemStack[]
    public static String serializeItems(ItemStack[] items) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (DataOutputStream out = new DataOutputStream(baos)) {
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
            } // Save array length
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            DeathPlugin.INSTANCE.getLogger().log(Level.SEVERE, "Error during item serialization", e);
            return "";
        }
    }


    //This method is to copy the inventorry from the given Player to the given Entity using PDC.
    public void copyInventory (Player player, Entity target, NamespacedKey inventory_key) {
        target.getPersistentDataContainer().set(
            inventory_key,
            PersistentDataType.STRING,
            serializeItems(player.getInventory()) // Pass PlayerInventory, not just getContents()
        );
    }

    // Deserialize to full inventory (main, armor, offhand)
    public static ItemStack[] deserializeItems(String data) {
        try {
            byte[] bytes = Base64.getDecoder().decode(data);
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            ItemStack[] items;
            try (DataInputStream in = new DataInputStream(bais)) {
                int length = in.readInt();
                items = new ItemStack[length];
                int nonAirCount = in.readInt();
                for (int j = 0; j < nonAirCount; j++) {
                    int i = in.readInt(); // Slot index
                    int itemLen = in.readInt();
                    byte[] itemBytes = new byte[itemLen];
                    in.readFully(itemBytes);
                    items[i] = ItemStack.deserializeBytes(itemBytes);
                }
            }
            return items;
        } catch (IOException e) {
            DeathPlugin.INSTANCE.getLogger().log(Level.SEVERE, "Error during item deserialization", e);
            return new ItemStack[0];
        }
    }
    //This method returns the Itemstack from the inventory.
    public ItemStack[] retrieveInventory (Entity entity, NamespacedKey inventory_key) {
    PersistentDataContainer container = entity.getPersistentDataContainer();
    
    String data = container.get(inventory_key, PersistentDataType.STRING);
        if (data == null || data.isEmpty()) {
            return new ItemStack[0]; // Or null, depending on your use case
        }

        try {
            return deserializeItems(data); // Your custom method from earlier
        } catch (Exception e) {
            DeathPlugin.INSTANCE.getLogger().log(Level.SEVERE, "Error during item deserialization", e);
            return new ItemStack[0];
        }
    }

    
    // This method opens a GUI showing the saved inventory to the specified player
    public void openSavedInventoryGUI(Player viewer, Entity source, NamespacedKey inventory_key) {
        if (viewer == null || source == null || inventory_key == null) return;

        ItemStack[] savedItems = retrieveInventory(source, inventory_key);

        if (savedItems.length == 0) {
            viewer.sendMessage("§cNo saved inventory found.");
            return;
        }

        // Try to get the player's name from the TextDisplay passenger
        String playerName = "Saved Inventory";
        for (Entity passenger : source.getPassengers()) {
            if (passenger instanceof org.bukkit.entity.TextDisplay textDisplay) {
                String text = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(textDisplay.text());
                // Extract the first line (player name)
                if (!text.isEmpty()) {
                    playerName = text.split("\n")[0];
                }
                break;
            }
        }

        // Calculate inventory size: round up to the next multiple of 9, max 54
        int rows = (int) Math.ceil(savedItems.length / 9.0);
        int size = Math.min(rows * 9, 54);

        Inventory gui = Bukkit.createInventory(
            new SavedInventoryHolder(source.getUniqueId()),
            size,
            mm.deserialize(playerName + "'s <bold><gradient:#de000b:#d94e50>Inventorry</gradient></bold>")
        );

        for (int i = 0; i < savedItems.length && i < size; i++) {
            gui.setItem(i, savedItems[i]);
        }

        viewer.openInventory(gui);
    }

        //This method is to save the player xp in the entity's PDC.
    public void saveXP (Integer xp, Entity target, NamespacedKey xp_key) {
        target.getPersistentDataContainer().set(
            xp_key,
            PersistentDataType.INTEGER,
            xp
        );
    }
}