package me.asofold.bpl.eventmirror;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.plugin.java.JavaPlugin;


/**
 * 
 * 
 * TODO: Add info command: show all event names covered.
 * 
 * @author mc_dev
 *
 */
public class EventMirror extends JavaPlugin implements Listener {
	
	// TODO: add /mirror (event name), set up names from listener class methods arguments.
	private final Set<String> players = new LinkedHashSet<String>();

	@Override
	public void onEnable() {
		Bukkit.getPluginManager().registerEvents(this, this);
		super.onEnable();
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {
		if (sender instanceof Player){
			if (players.contains(sender.getName())){
				players.remove(sender.getName());
				sender.sendMessage("[EventMirror] Unsubscribed from event mirror.");
			}
			else{
				players.add(sender.getName());
				sender.sendMessage("[EventMirror] Subscribed to event mirror.");
			}
			return true;
		}
		return false;
	}
	
	/**
	 * Check if to mirror at all and do it if appropriate.
	 * @param actor
	 * @param event
	 * @param info
	 * @return If mirrored.
	 */
	private final boolean checkMirror(final Entity actor, final Event event, final Object info) {
		if (!(actor instanceof Player)) return false;
		final Player player = (Player) actor;
		if (!players.contains(player.getName())) return false;
		return sendMirror(player, event, info);
	}
	
	/**
	 * Directly send mirror information to a player.
	 * @param player
	 * @param event
	 * @param info
	 * @return true
	 */
	private final boolean sendMirror(final Player player, final Event event, final Object info) {
		final Class<?> clazz =  event.getClass();
		final String cn;
		final String packageName = clazz.getPackage().getName();
		if (packageName == null  || packageName.startsWith("org.bukkit")){
			cn = clazz.getSimpleName();
		}
		else{
			cn = clazz.getName();
		}
		final String sInfo;
		if (info == null){
			sInfo = "no info";
		}
		else{
			if (info instanceof Object[]) sInfo = Arrays.toString((Object[]) info);
			else sInfo = info.toString();
		}
		player.sendMessage("[EventMirror] " + cn + ((event instanceof Cancellable) ? (((Cancellable) event).isCancelled() ? " (cancelled)" : "" ): "") + " : " + sInfo);
		return true;
	}
	
	/**
	 * Directly send mirror info to all registered players.
	 * @param event
	 * @param info
	 * @return true
	 */
	private final boolean sendMirror(final Event event, final Object info) {
		if (players.isEmpty()) return true;
		final Player[] players = getServer().getOnlinePlayers();
		for (int i = 0; i < players.length; i++){
			final Player player = players[i];
			if (this.players.contains(player.getName())){
				sendMirror(player, event, info);
			}
		}
		return true;
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled = false)
	public final void onPlayerTeleport(final PlayerTeleportEvent event){
		checkMirror(event.getPlayer(), event, new Object[]{event.getCause(), event.getFrom(), event.getTo()});
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled = false)
	public final void onEntityTeleport(final EntityTeleportEvent event){
		final Entity entity = event.getEntity();
		if (entity instanceof Vehicle){
			final Entity passenger = entity.getPassenger();
			if (passenger instanceof Player){
				checkMirror((Player) passenger, event, entity.toString() + " -> " + event.getTo());
			}
		}
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled = false)
	public final void onPlayerInteractEntity(final PlayerInteractEntityEvent event){
		checkMirror(event.getPlayer(), event, event.getRightClicked());
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled = false)
	public final void onPlayerInteract(final PlayerInteractEvent event){
		checkMirror(event.getPlayer(), event, new Object[]{event.getAction(), event.getClickedBlock(), event.getBlockFace()});
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled = false)
	public final void onItemSpawn(final ItemSpawnEvent event){
		final Location loc = event.getLocation();
		final Block block = loc.getBlock();
		if (block == null) return;
		Material mat = block.getType();
		if (mat.isSolid() && mat.isOccluding() && !mat.isTransparent()){
			sendMirror(event, new Object[]{"Item spawn in block: ", mat, block});
		}
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled = false)
	public final void onEntityDamage(final EntityDamageEvent event){
		if (!(event instanceof EntityDamageByEntityEvent)) return;
		checkMirror(((EntityDamageByEntityEvent) event).getDamager(), event, event.getEntity());
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled = false)
	public final void onPlayerBlockDamage(final BlockDamageEvent event){
		checkMirror(event.getPlayer(), event, (event.getInstaBreak() ? "insta/" : "") + event.getBlock());
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled = false)
	public final void onPlayerBlockBreak(final BlockBreakEvent event){
		checkMirror(event.getPlayer(), event, event.getBlock());
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled = false)
	public final void onVehicleEnter(final VehicleEnterEvent event){
		final Entity entity = event.getEntered();
		if (entity instanceof Player){
			checkMirror(entity, event, entity.getLocation());
		}
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled = false)
	public final void onVehicleExit(final VehicleExitEvent event){
		final Entity entity = event.getExited();
		if (entity instanceof Player){
			checkMirror(entity, event, entity.getLocation());
		}
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled = false)
	public final void onPlayerSprint(final PlayerToggleSprintEvent event){
		checkMirror(event.getPlayer(), event, "sprint=" + event.isSprinting() + " / food=" + event.getPlayer().getFoodLevel());
	}
	
	@EventHandler(priority=EventPriority.MONITOR)
	public final void onInvClick(final InventoryClickEvent event){
		checkMirror(event.getWhoClicked(), event, "slot=" + event.getSlot() + " shift=" + event.isShiftClick());
	}
	
	@EventHandler(priority=EventPriority.MONITOR)
	public final void onPlayerQuit(final PlayerQuitEvent event){
		onLeave(event.getPlayer());
	}

	@EventHandler(priority=EventPriority.MONITOR)
	public final void onPlayerKick(final PlayerKickEvent event){
		onLeave(event.getPlayer());
	}
	
	private void onLeave(final Player player) {
		if (players.contains(player.getName())){
			System.out.println("[EventMirror] Player quits: " + player.getName() + " sleeping=" + player.isSleeping() + " allowFlight=" + player.getAllowFlight() + " flying=" + player.isFlying());
		}
	}
	
}
