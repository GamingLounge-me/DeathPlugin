package me.gaminglounge.deathPlugin;

public class ChunkLocation {
    private int chunkX;
    private int chunkZ;
    private String worldName;

    public ChunkLocation(int chunkX, int chunkZ, String worldName) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.worldName = worldName;
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    public String getWorldName() {
        return worldName;
    }
}