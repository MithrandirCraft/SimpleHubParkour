package es.mithrandircraft.simplehubparkour;

import es.mithrandircraft.simplehubparkour.commands.SHP;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public final class SimpleHubParkour extends JavaPlugin implements Listener {

    //Playing parkour Hash Map:
    public static HashMap<String, ParkourSession> sessions = new HashMap<String, ParkourSession>();

    //Time format:
    SimpleDateFormat sdf = new SimpleDateFormat("mm:ss");

    //Exit barrier stack configuration:
    ItemStack barrier;
    ItemMeta barrierMeta;

    @Override
    public void onEnable() {
        //Config load:
        getConfig().options().copyDefaults();
        saveDefaultConfig();

        //Commands:
        getCommand("SHP").setExecutor(new SHP(this));

        //Events:
        getServer().getPluginManager().registerEvents(this, this);

        //Runnables:
        SessionsRunnable();

        //Exit barrier stack initialization:
        barrier = new ItemStack(Material.BARRIER, 1);
        barrierMeta = barrier.getItemMeta();
        barrierMeta.setDisplayName(ChatColor.YELLOW + getConfig().getString("ExitBarrierName"));
        barrier.setItemMeta(barrierMeta);
    }

    @Override
    public void onDisable() {
        for (HashMap.Entry<String, ParkourSession> entry : sessions.entrySet()) //Clear parkour sessions
        {
            TerminateParkourSession(getServer().getPlayer(entry.getKey()));
        }
    }

    public void StartParkourSession(Player p, PlayerInteractEvent e)
    {
        //Disable flying for player:
        if(getConfig().getBoolean("DenyFlight")) p.setAllowFlight(false);
        //Add player to parkour sessions:
        sessions.put(p.getName(), new ParkourSession(new Location(e.getClickedBlock().getWorld(), getConfig().getLong("FirstSpawnX"), getConfig().getLong("FirstSpawnY"), getConfig().getLong("FirstSpawnZ"))));
        //Add exit barrier to player's hotbar:
        p.getInventory().setItem(8, barrier);
    }

    public void TerminateParkourSession(Player p)
    {
        //Remove barrier item:
        p.getInventory().removeItem(barrier);
        if(p.getEquipment().getHelmet() != null && p.getEquipment().getHelmet().equals(barrier)) p.getEquipment().setHelmet(null);
        if(p.getEquipment().getItemInOffHand().equals(barrier)) p.getEquipment().setItemInOffHand(null);
        //Remove player from parkour sessions:
        sessions.remove(p.getName()); //Player removed from hashmap if left server
    }

    public void SessionsRunnable() //Performs sessions HashMap updates at scheduled time
    {
        new BukkitRunnable()
        {
            @Override
            public void run()
            {
                Set sessionsKeySet = sessions.keySet();
                Iterator sessionsIterator = sessionsKeySet.iterator();
                while (sessionsIterator.hasNext()) {
                    String key = (String) sessionsIterator.next();
                    Material u = getServer().getPlayer(key).getLocation().getBlock().getRelative(BlockFace.DOWN).getType();
                    int listSize = getConfig().getStringList("ParkourValidBlocks").size();
                    boolean respawn = true;
                    for(int i = 0; i < listSize; i++){ //Check if session player is on top of a valid block
                        if (u == Material.getMaterial(getConfig().getStringList("ParkourValidBlocks").get(i))) {
                            respawn = false;
                            break;
                        }
                    }
                    if(respawn)
                    {
                        sessions.get(key).addFall();
                        getServer().getPlayer(key).teleport(sessions.get(key).savePoint); //Back to last spawnpoint
                        getServer().getPlayer(key).playSound(getServer().getPlayer(key).getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, 1, 10);
                        if(sessions.get(key).savePoint.getX() == getConfig().getLong("FirstSpawnX")
                            && sessions.get(key).savePoint.getY() == getConfig().getLong("FirstSpawnY")
                            && sessions.get(key).savePoint.getZ() == getConfig().getLong("FirstSpawnZ")) { //It's first spawnpoint

                            TerminateParkourSession(getServer().getPlayer(key));
                        }
                    }
                }
            }
        }.runTaskTimer(this, 20, 20);
    }

    @EventHandler
    public void InteractionEvent(PlayerInteractEvent ev) {
        Block b = ev.getClickedBlock();
        if(b != null && b.getType() == Material.HEAVY_WEIGHTED_PRESSURE_PLATE) //Iron plate
        {
            ParkourSession s = sessions.get(ev.getPlayer().getName());

            //Start plate coords check:
            if(ev.getClickedBlock().getLocation().getX() == getConfig().getInt("StartPlateX")
            && ev.getClickedBlock().getLocation().getY() == getConfig().getInt("StartPlateY")
            && ev.getClickedBlock().getLocation().getZ() == getConfig().getInt("StartPlateZ")) { //Start plate coords
                if (s == null) //No session, create session
                {
                    StartParkourSession(ev.getPlayer(), ev);
                }
            }
            //End plate coords check:
            else if(ev.getClickedBlock().getLocation().getX() == getConfig().getInt("EndPlateX")
            && ev.getClickedBlock().getLocation().getY() == getConfig().getInt("EndPlateY")
            && ev.getClickedBlock().getLocation().getZ() == getConfig().getInt("EndPlateZ")) { //End plate coords, destroy session
                if (s != null) //There is a session, end effects + destroy session
                {
                    //End effects:
                    ev.getPlayer().playSound(ev.getPlayer().getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, -5);
                    ev.getPlayer().spawnParticle(Particle.FIREWORKS_SPARK,  ev.getPlayer().getLocation().getX(), ev.getPlayer().getLocation().getY(), ev.getPlayer().getLocation().getZ(), 100);

                    //End message display:
                    String chronoFormat = sdf.format(new Date(sessions.get(ev.getPlayer().getName()).stopwatch.elapsed(TimeUnit.MILLISECONDS)));
                    ev.getPlayer().sendMessage(PlaceholderManager.SubstitutePlaceholders(getConfig().getString("ParkourEndMessage"), chronoFormat, sessions.get(ev.getPlayer().getName()).falls));

                    TerminateParkourSession(ev.getPlayer());
                }
            }
        }
        else if(b != null && b.getType() == Material.LIGHT_WEIGHTED_PRESSURE_PLATE) //Gold plate
        {
            ParkourSession s = sessions.get(ev.getPlayer().getName());
            if (s != null) //Save spawnpoint
            {
                Location l = new Location(ev.getPlayer().getWorld(), ev.getClickedBlock().getLocation().getX() + 0.5, ev.getClickedBlock().getLocation().getY() + 0.5, ev.getClickedBlock().getLocation().getZ() + 0.5);
                if(l.getX() != s.savePoint.getX() && l.getY() != s.savePoint.getY() && l.getZ() != s.savePoint.getZ()) {
                    s.savePoint = l;
                    ev.getPlayer().sendMessage(getConfig().getString("ParkourCheckpointMessage"));
                }
            }
        }

        //Right click while holding barrier:
        if(ev.getPlayer().getInventory().getItemInMainHand().equals(barrier))
        {
            if(ev.getAction() == Action.RIGHT_CLICK_AIR || ev.getAction() == Action.RIGHT_CLICK_BLOCK) {
                ParkourSession s = sessions.get(ev.getPlayer().getName());
                if (s != null) {
                    TerminateParkourSession(ev.getPlayer());
                    //Send to first spawn:
                    ev.getPlayer().teleport(new Location(ev.getPlayer().getWorld(), getConfig().getLong("FirstSpawnX"), getConfig().getLong("FirstSpawnY"), getConfig().getLong("FirstSpawnZ")));
                }
            }
        }
    }

    @EventHandler
    public void PlayerLeftServerEvent(PlayerQuitEvent ev)
    {
        if(sessions.get(ev.getPlayer().getName()) != null) {
            TerminateParkourSession(ev.getPlayer());
            //Send to first spawn:
            ev.getPlayer().teleport(new Location(ev.getPlayer().getWorld(), getConfig().getLong("FirstSpawnX"), getConfig().getLong("FirstSpawnY"), getConfig().getLong("FirstSpawnZ")));
        }
    }

    @EventHandler
    public void PlayerCommandPreprocess(PlayerCommandPreprocessEvent ev){
        if(ev.getMessage().equals("/fly") && getConfig().getBoolean("DenyFlight") && sessions.containsKey(ev.getPlayer().getName())) ev.setCancelled(true); //Cancel /fly attempts if in parkour session
    }
}
