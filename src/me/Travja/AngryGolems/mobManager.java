package me.Travja.AngryGolems;

import org.bukkit.*;
import org.bukkit.World.Environment;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class mobManager implements Listener {
    public static Golems plugin;
    private static int LEVEL = 4;

    public mobManager(Golems m) {
        plugin = m;
    }

    @EventHandler
    public void onMobSpawn(CreatureSpawnEvent event) {
        Random r = new Random();
        Entity e = event.getEntity();
        World world = e.getWorld();
        if (isHostile(e)) {
            if (e.getWorld().getEnvironment() == Environment.NORMAL) {
                int chance = r.nextInt(100);
                Location l = e.getLocation();
                if (chance <= plugin.config.getInt("spawnChance")) {
                    if (!plugin.config.getBoolean("snowOnly") || (plugin.config.getBoolean("snowOnly") && (l.getBlock().getType() == Material.SNOW || l.getBlock().getRelative(BlockFace.DOWN).getType() == Material.SNOW_BLOCK))) {
                        if (!plugin.config.getBoolean("snowBiome") || (plugin.config.getBoolean("snowBiome") &&
                                (l.getBlock().getBiome() == Biome.SNOWY_BEACH ||
                                        l.getBlock().getBiome() == Biome.TAIGA ||
                                        l.getBlock().getBiome() == Biome.TAIGA_HILLS ||
                                        l.getBlock().getBiome() == Biome.TAIGA_MOUNTAINS ||
                                        l.getBlock().getBiome() == Biome.FROZEN_RIVER ||
                                        l.getBlock().getBiome() == Biome.FROZEN_OCEAN ||
                                        l.getBlock().getBiome() == Biome.ICE_SPIKES ||
                                        l.getBlock().getBiome() == Biome.SNOWY_MOUNTAINS ||
                                        l.getBlock().getBiome() == Biome.SNOWY_TAIGA ||
                                        l.getBlock().getBiome() == Biome.SNOWY_TAIGA_HILLS ||
                                        l.getBlock().getBiome() == Biome.SNOWY_TAIGA_MOUNTAINS ||
                                        l.getBlock().getBiome() == Biome.SNOWY_TUNDRA))) {
                            Entity snowman = world.spawnEntity(l, EntityType.SNOWMAN);
                            if (snowman instanceof Damageable) {
                                LivingEntity sman = (LivingEntity) snowman;
                                sman.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20);
                                sman.setHealth(20);
                                sman.setRemoveWhenFarAway(true);
                            }
                            event.setCancelled(true);
                        }
                    }
                }
            }
        }
        if (e.getType() == EntityType.SNOWMAN) {
            if (e instanceof Damageable) {
                LivingEntity sman = (LivingEntity) e;
                sman.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20);
                sman.setHealth(20);
                sman.setRemoveWhenFarAway(true);
            }
        }
    }

    private HashMap<UUID, Integer> men = new HashMap<>();

    @EventHandler(priority = EventPriority.HIGHEST)
    public void projectile(ProjectileLaunchEvent event) {
        Projectile p = event.getEntity();
        if (p instanceof Snowball) {
            if (p.getShooter() instanceof Snowman) {
                if (plugin.config.getInt("fireRate") == 0) {
                    event.setCancelled(true);
                } else {
                    UUID id = ((Entity) p.getShooter()).getUniqueId();
                    if (men.containsKey(id)) {
                        int i = men.get(id);
                        if (plugin.config.getInt("fireRate") == i) {
                            event.setCancelled(true);
                            men.put(id, 0);
                        } else {
                            men.put(id, i++);
                        }
                    } else {
                        men.put(id, 1);
                    }
                }
            }
        }
    }

    @EventHandler
    public void cancelTarget(EntityTargetEvent event) {
        Entity e = event.getEntity();
        Entity target = event.getTarget();
        if (e.getType() == EntityType.SNOWMAN)
            if (!(target instanceof Player))
                event.setCancelled(true);
    }

    @EventHandler
    public void target(PlayerMoveEvent event) {
        Player p = event.getPlayer();
        List<Entity> nearby = p.getNearbyEntities(16, 16, 16);

        try {
            Class entLiving = Class.forName("net.minecraft.server." + version + ".EntityLiving");
            Class craftPlayer = getCraftClass("entity.CraftHumanEntity");
            Class craftSnowman = getCraftClass("entity.CraftGolem");
            Method handle = getMethod(craftSnowman, "getHandle");
            Method pHandle = getMethod(craftPlayer, "getHandle");

            for (Entity near : nearby) {
                if (near.getType() == EntityType.SNOWMAN) {
                    Object snowman = craftSnowman.cast(near);
                    Object entSnowman = Class.forName("net.minecraft.server." + version + ".EntitySnowman").cast(handle.invoke(snowman));
                    Method setTarget = getMethod(entSnowman, "setGoalTarget", entLiving, EntityTargetEvent.TargetReason.class, boolean.class);
                    Method getTarget = getMethod(entSnowman, "getGoalTarget");
                    Object target = getTarget.invoke(entSnowman);
                    if (target != null) {
                        if (target instanceof Player) {
                            Player pt = (Player) target;
                            if (!pt.getNearbyEntities(16, 16, 16).contains(near)) {
                                setTarget.invoke(entSnowman, pHandle.invoke(craftPlayer.cast(p)), EntityTargetEvent.TargetReason.CLOSEST_PLAYER, true);
                            } else {
                                setTarget.invoke(entSnowman, pHandle.invoke(craftPlayer.cast(pt)), EntityTargetEvent.TargetReason.CLOSEST_PLAYER, true);
                            }
                        }
                    } else {
                        setTarget.invoke(entSnowman, pHandle.invoke(craftPlayer.cast(p)), EntityTargetEvent.TargetReason.CLOSEST_PLAYER, true);
                    }
                }
            }
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }


        /*for (Entity near : nearby) {
            if (near.getType() == EntityType.SNOWMAN && near instanceof CraftSnowman) {
                CraftSnowman snow = (CraftSnowman) near;
                EntityLiving target = snow.getHandle().getGoalTarget();
                if (target != null) {
                    if (target instanceof Player) {
                        Player pt = (Player) target;
                        if (!pt.getNearbyEntities(16, 16, 16).contains(near)) {
                            snow.getHandle().setGoalTarget(((CraftPlayer) p).getHandle());
                        } else {
                            snow.getHandle().setGoalTarget(((CraftPlayer) pt).getHandle());
                        }
                    }
                } else {
                    snow.getHandle().setGoalTarget(((CraftPlayer) p).getHandle(), EntityTargetEvent.TargetReason.CLOSEST_PLAYER, true);
                }
            }
        }*/
    }

    private final String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];

    @EventHandler
    public void damage(EntityDamageByEntityEvent event) {
        Entity e = event.getEntity();
        Entity ed = event.getDamager();
        if (e.getType() == EntityType.SNOWMAN) {
            try {
                Class entLiving = Class.forName("net.minecraft.server." + version + ".EntityLiving");
                Class craftPlayer = getCraftClass("entity.CraftHumanEntity");
                Class craftSnowman = getCraftClass("entity.CraftGolem");
                Object snowman = craftSnowman.cast(e);
                Method handle = getMethod(snowman, "getHandle");
                Object entSnowman = Class.forName("net.minecraft.server." + version + ".EntitySnowman").cast(handle.invoke(snowman));
                Method setTarget = getMethod(entSnowman, "setGoalTarget", entLiving, EntityTargetEvent.TargetReason.class, boolean.class);
                if (entLiving.isInstance(ed)) {
                    Object craftLiving = entLiving.cast(ed);
                    Method livHandle = getMethod(craftLiving, "getHandle");
                    setTarget.invoke(entSnowman, livHandle.invoke(craftLiving), EntityTargetEvent.TargetReason.CLOSEST_ENTITY, true);
                } else if (ed instanceof Projectile) {
                    if (entLiving.isInstance(((Projectile) ed).getShooter())) {
                        Object craftLiving = entLiving.cast(((Projectile) ed).getShooter());
                        Method livHandle = getMethod(craftLiving, "getHandle");
                        setTarget.invoke(entSnowman, livHandle.invoke(craftLiving), EntityTargetEvent.TargetReason.COLLISION, true);
                    } else if (craftPlayer.isInstance(((Projectile) ed).getShooter())) {
                        Object craftLiving = craftPlayer.cast(((Projectile) ed).getShooter());
                        Method livHandle = getMethod(craftLiving, "getHandle");
                        setTarget.invoke(entSnowman, livHandle.invoke(craftLiving), EntityTargetEvent.TargetReason.COLLISION, true);
                    }
                }
            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
                plugin.getLogger().warning("Could not set target for golem. Error is: ");
                ex.printStackTrace();
            }
            /*CraftSnowman snow = (CraftSnowman) e;
            if (ed instanceof EntityLiving) {
                snow.getHandle().setGoalTarget(((CraftLivingEntity) ed).getHandle());
            }else if (ed instanceof Projectile) {
                if (((Projectile) ed).getShooter() instanceof EntityLiving) {
                    snow.getHandle().setGoalTarget(((CraftLivingEntity) ((Projectile) ed).getShooter()).getHandle());
                }
                if (((Projectile) ed).getShooter() instanceof CraftPlayer) {
                    snow.getHandle().setGoalTarget(((CraftPlayer) ((Projectile) ed).getShooter()).getHandle());
                }
            }*/
        }
    }

    public Class getCraftClass(String location) throws ClassNotFoundException {
        return Class.forName("org.bukkit.craftbukkit." + version + "." + location);
    }

    public Method getMethod(Class clazz, String method, Class<?>... parameters) throws NoSuchMethodException {
        return clazz.getMethod(method, parameters);
    }

    public Method getMethod(Object clazz, String method, Class<?>... parameters) throws NoSuchMethodException {
        return clazz.getClass().getMethod(method, parameters);
    }

    static HashMap<Location, Integer> w = new HashMap<>();

    @SuppressWarnings("deprecation")
    @EventHandler
    public void sdeath(EntityDeathEvent event) {
        final Entity e = event.getEntity();
        final Location bl = e.getLocation().getBlock().getLocation();
        if (e.getType() == EntityType.SNOWMAN && plugin.config.getBoolean("puddle")) {
            if (e.getLocation().getBlock().getRelative(BlockFace.DOWN).getType() != Material.AIR || e.getLocation().getBlock().getRelative(BlockFace.DOWN).isLiquid())
                if (!e.getLocation().getBlock().isLiquid() && (e.getLocation().getBlock().getType() == Material.AIR || e.getLocation().getBlock().getType() == Material.SNOW)) {
                    Levelled level = (Levelled) Material.WATER.createBlockData();
                    level.setLevel(LEVEL);
                    bl.getBlock().setBlockData(level);
                    bl.getBlock().getState().update();
                    w.put(bl, LEVEL);
                    plugin.water.add(bl.getWorld() + "," + bl.getX() + "," + bl.getY() + "," + bl.getZ());
                    plugin.config.set("water", plugin.water);
                    plugin.saveConfig();
                    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                        if (bl.getBlock().getType() == Material.WATER)
                            e.getLocation().getBlock().setType(Material.AIR);
                        plugin.water.remove(bl.getWorld() + "," + bl.getX() + "," + bl.getY() + "," + bl.getZ());
                        plugin.config.set("water", plugin.water);
                        plugin.saveConfig();
                        w.remove(bl);
                    }, 1200);
                }
        }
    }

    @EventHandler
    public void place(BlockPlaceEvent event) {
        if (w.containsKey(event.getBlock().getLocation()))
            w.remove(event.getBlock().getLocation());
    }

    @EventHandler
    public void evaporate(BlockFromToEvent event) {
        Block b = event.getBlock();
        if (b.getType() == Material.WATER)
            if (w.containsKey(b.getLocation())) {
                event.setCancelled(true);
                Levelled level = (Levelled) Material.WATER.createBlockData();
                level.setLevel(LEVEL);
                b.setBlockData(level);
                b.getState().update();
            }
    }

    public static HashMap<Entity, LinkedList<Block>> blocks = new HashMap<>();
    public static ArrayList<Entity> snowmen = new ArrayList<>();

    @EventHandler
    public void path(EntityBlockFormEvent event) {
        Entity e = event.getEntity();
        Block b = event.getBlock();
        if (e.getType() == EntityType.SNOWMAN) {
            if (!plugin.config.getBoolean("snowPath")) {
                if (b.getBiome() != Biome.FROZEN_OCEAN || b.getBiome() != Biome.FROZEN_RIVER || b.getBiome() != Biome.SNOWY_MOUNTAINS || b.getBiome() != Biome.ICE_SPIKES) {
                    if (!blocks.containsKey(e)) {
                        blocks.put(e, new LinkedList<>());
                        snowmen.add(e);
                    }
                    blocks.get(e).add(b);
                }
            }
        }
    }

    public boolean isHostile(Entity e) {
        EntityType et = e.getType();
        if (et == EntityType.CAVE_SPIDER || et == EntityType.CREEPER || et == EntityType.ENDERMAN || et == EntityType.SKELETON ||
                et == EntityType.SLIME || et == EntityType.SPIDER || et == EntityType.ZOMBIE) {
            return true;
        }
        return false;
    }

    public static void water() {
        if (plugin.config.contains("water") && plugin.config.get("water") != null)
            for (String loc : plugin.config.getStringList("water")) {
                World w = Bukkit.getWorld(loc.split(",")[0]);
                double x = Double.parseDouble(loc.split(",")[1]);
                double y = Double.parseDouble(loc.split(",")[2]);
                double z = Double.parseDouble(loc.split(",")[3]);
                Location l = new Location(w, x, y, z);
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                    if (l != null && l.getBlock() != null && l.getBlock().getType() == Material.WATER)
                        l.getBlock().setType(Material.AIR);
                }, 600L);
            }
    }

    static HashMap<Entity, Integer> time = new HashMap<>();
    static HashMap<Entity, Integer> task = new HashMap<>();
    static ArrayList<Entity> killing = new ArrayList<>();

    public static void init() {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            for (Location l : w.keySet()) {
                Block b = l.getBlock();
                if (b.getType() != Material.WATER) {
                    b.breakNaturally();
                    Levelled level = (Levelled) Material.WATER.createBlockData();
                    level.setLevel(LEVEL);
                    b.setBlockData(level);
                    b.getState().update();
                }
            }
            List<Entity> remove = new ArrayList<>();
            for (Entity e : snowmen) {
                if (blocks.containsKey(e)) {
                    if (!blocks.get(e).isEmpty()) {
                        if (e.isDead() || !e.isValid()) {
                            for (Block b : blocks.get(e)) {
                                if (!plugin.water.contains(b.getLocation().getWorld() + "," + b.getLocation().getX() + "," + b.getLocation().getY() + "," + b.getLocation().getZ())) {
                                    b.setType(Material.AIR);
                                    b.getState().update();
                                }
                            }
                            blocks.get(e).clear();
                        } else {
                            while (blocks.get(e).size() > 6) {
                                blocks.get(e).getFirst().setType(Material.AIR);
                                blocks.get(e).getFirst().getState().update();
                                blocks.get(e).removeFirst();
                            }
                        }
                    } else {
                        remove.add(e);
                    }
                }
            }
            for (Entity e : remove) {
                snowmen.remove(e);
            }
        }, 0, 5);
        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            for (final World w : Bukkit.getWorlds()) {
                if (w.getEnvironment() == Environment.NORMAL) {
                    for (final Entity e : w.getEntities()) {
                        if (e.getType() == EntityType.SNOWMAN) {
                            if (!killing.contains(e)) {
                                if (w.getTime() <= 24000 && (w.getTime() >= 23700 || w.getTime() <= 12700) && !w.hasStorm()) {
                                    time.put(e, 10);
                                    task.put(e, Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
                                        if (canSeeSky(e)) {
                                            Location l = e.getLocation();
                                            killing.add(e);
                                            if (time.get(e) == 1 || e.isDead()) {
                                                Bukkit.getScheduler().cancelTask(task.get(e));
                                            } else {
                                                time.put(e, time.get(e) - 1);
                                            }
                                            l.setY(e.getLocation().getY() + 1);
                                            w.playSound(l, Sound.ENTITY_SNOW_GOLEM_HURT, 1f, 1f);
                                            w.spawnParticle(Particle.BLOCK_CRACK, l, 15, 0.3, 0.3, 0.3, 0.2, new ItemStack(Material.WATER).getData());
                                            if (((LivingEntity) e).getHealth() > 2) {
                                                ((LivingEntity) e).damage(2);
                                            } else {
                                                ((LivingEntity) e).damage(2);
                                                killing.remove(e);
                                            }
                                        } else {
                                            Bukkit.getScheduler().cancelTask(task.get(e));
                                        }
                                    }, 0, 20));
                                }
                            }
                        }
                    }
                }
            }
        }, 0, 40);
    }

    public static boolean canSeeSky(Entity e) {
        Location l = e.getLocation();
        l.setY(l.getY() + 1);
        Block b = l.getBlock();
        double y = b.getLocation().getY() + 30;
        while (b.getLocation().getY() <= y) {
            b = l.getBlock();
            if (b.getType() != Material.AIR) {
                l.setY(y + 1);
                return false;
            } else {
                l.setY(l.getY() + 1);
            }
        }
        return true;
    }
}