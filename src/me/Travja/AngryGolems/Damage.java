package me.Travja.AngryGolems;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class Damage implements Listener {
	public Golems plugin;
	public Damage(Golems golems) {
		this.plugin = golems;
	}
	@EventHandler
	public void onDamage(EntityDamageByEntityEvent event){
		Entity ed = event.getDamager();
		if(ed instanceof Projectile){
			Projectile p = (Projectile) ed;
			if(p instanceof Snowball){
				Snowball sb = (Snowball) p;
				if(((Entity) sb.getShooter()).getType()== EntityType.SNOWMAN){
					event.setDamage(plugin.config.getDouble("damage"));
				}
			}
		}
	}
}
