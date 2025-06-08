package me.gaminglounge.deathPlugin;

import java.util.UUID;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class SavedInventoryHolder implements InventoryHolder {
    private final UUID ownerId;

    public SavedInventoryHolder(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    @Override
    public Inventory getInventory() {
        return null; // Not used directly
    }
}
