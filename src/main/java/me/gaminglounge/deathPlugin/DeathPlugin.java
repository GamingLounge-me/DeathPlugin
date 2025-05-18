package me.gaminglounge.deathPlugin;

import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import me.gaminglounge.deathPlugin.listeners.DeathRegister;

public final class DeathPlugin extends JavaPlugin {

    public static DeathPlugin INSTANCE;


    public static ScoreboardManager sm;

    @Override
    public void onLoad() {
        INSTANCE = this;

    }
    @Override
    public void onEnable() {
        // Plugin startup logic
        sm = new ScoreboardManager();
        sm.startCountdownTask();
        listener();
        
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic

    }

    private void listener() {
        PluginManager pm = getServer().getPluginManager();

        pm.registerEvents(new DeathRegister(), this);
    }
}