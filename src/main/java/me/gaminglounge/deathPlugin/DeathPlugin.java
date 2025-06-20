package me.gaminglounge.deathPlugin;

import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import me.gaminglounge.deathPlugin.listeners.DeathRegister;
import me.gaminglounge.deathPlugin.listeners.EntityInteractListener;

public final class DeathPlugin extends JavaPlugin {

    public static DeathPlugin INSTANCE;


    public static ScoreboardManager sm;
    public static HelperMethods hm;

    @Override
    public void onLoad() {
        INSTANCE = this;

    }
    @Override
    public void onEnable() {
        // Plugin startup logic
        hm = new HelperMethods();
        sm = new ScoreboardManager();
        sm.loadScoreboard();
        sm.startCountdownTask();
        listener();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        sm.saveScoreboard();
    }

    private void listener() {
        PluginManager pm = getServer().getPluginManager();

        pm.registerEvents(new DeathRegister(), this);
        pm.registerEvents(new EntityInteractListener(), this);
    }
}