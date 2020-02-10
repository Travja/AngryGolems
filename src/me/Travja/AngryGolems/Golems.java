package me.Travja.AngryGolems;

import java.io.File;
import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class Golems extends JavaPlugin {
	public Listener spawnManager = new mobManager(this);
	public Listener Damage = new Damage(this);
	public FileConfiguration config;
	public ArrayList<String> water = new ArrayList<String>();
	public void onEnable(){
		config = this.getConfig();
		config.options().copyDefaults();
		if(!new File(getDataFolder(), "config.yml").exists())
			this.saveDefaultConfig();
		getServer().getPluginManager().registerEvents(spawnManager, this);
		getServer().getPluginManager().registerEvents(Damage, this);
		mobManager.water();
		mobManager.init();
	}
	public void onDisable(){
		for(World w: Bukkit.getServer().getWorlds()){
			for(Entity e: w.getEntities()){
				if(mobManager.blocks.containsKey(e)){
					for(Block b: mobManager.blocks.get(e)){
						b.setType(Material.AIR);
						b.getState().update();
					}
				}
			}
		}
	}
}