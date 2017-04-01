package de.tobiyas.enderdragonsplus.commands;

import java.util.HashSet;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Vector;

import de.tobiyas.enderdragonsplus.EnderdragonsPlus;
import de.tobiyas.enderdragonsplus.API.DragonAPI;
import de.tobiyas.enderdragonsplus.entity.dragon.LimitedED;
import de.tobiyas.enderdragonsplus.entity.dragon.controllers.fireball.FireballController;
import de.tobiyas.enderdragonsplus.entity.dragon.controllers.move.DragonMoveController;
import de.tobiyas.enderdragonsplus.entity.dragon.controllers.targeting.ITargetController;
import de.tobiyas.enderdragonsplus.entity.firebreath.FireBreath;
import de.tobiyas.enderdragonsplus.permissions.PermissionNode;

public class CommandRide implements CommandExecutor {

private EnderdragonsPlus plugin;
	
	public CommandRide(){
		plugin = EnderdragonsPlus.getPlugin();
		try{
			plugin.getCommand("edpride").setExecutor(this);
		}catch(Exception e){
			plugin.log("Could not register command: /edpride");
		}
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label,
			String[] args) {
		if(!(sender instanceof Player)){
			sender.sendMessage(ChatColor.RED + "Only players can use this command.");
			return true;
		}
		
		if(!plugin.getPermissionManager().checkPermissions(sender, PermissionNode.ride)){
			return true;
		}
		
		int speed = 1;
		boolean collision=false; //Jeppa: NEU, default wie bisher... keine Collision 

		if(args.length > 0){
			try{
				speed = Integer.parseInt(args[0]);
			}catch(NumberFormatException exp){}
		}
		if(args.length > 1){ //Jeppa: 2. Argument angegeben (Zahl) macht Collision AN
			try{
				int Intcollision = Integer.parseInt(args[1]);
				if (Intcollision >0) collision = true;
			}catch(NumberFormatException exp){}
		}
		
		int maxSpeed = 4;
		speed = Math.min(speed, maxSpeed);
		
		Player player = (Player) sender;
		Location PlayerLoc = player.getLocation();
		if (collision) PlayerLoc.setY(PlayerLoc.getY()+2); //Jeppa: 2 Block hoeher!! (wenn Collison an ist... damit man nicht haengen bleibt...)
		LivingEntity entity = DragonAPI.spawnNewEnderdragon(PlayerLoc); 
		LimitedED dragon = DragonAPI.getDragonByEntity(entity);
		entity.setPassenger(player);
		dragon.setDragonMoveController(new DumbMoveController(dragon, player, speed, collision));
		dragon.setFireballController(new PlayerFireFireballController(dragon, dragon.getTargetController(), player));
		
		player.sendMessage(ChatColor.GREEN + "Mounted you on a dragon.");
		
		return true;
	}
	
	
	/**
	 * A simple Move controller forwarding the Player move directions.
	 * 
	 * @author Tobiyas
	 *
	 */
	private class DumbMoveController extends DragonMoveController{

		/**
		 * Speed of the Dragon
		 */
		private final int speed;
		
		
		public DumbMoveController(LimitedED dragon, Player player, int speed, boolean Collision) {
			super(dragon);
			
			this.speed = speed;
			//Jeppa: Hack...
			dragon.getCollisionController().setCollision(Collision); 

		}

		
		@Override
		public void moveDragon() {
			//remove dragon when no passenger.
			if(dragon.getPassenger() == null){
				dragon.getBukkitEntity().remove();
			}

			for(int i = 0; i < speed; i++){
				dragon.playerMovedEntity(0.89f, 0.89f);
			}
		}
	}
	
	/**
	 * The Fireball Controller listening to stuff.
	 * 
	 * @author Tobiyas
	 *
	 */
	private class PlayerFireFireballController extends FireballController implements Listener{

		/**
		 * The Player
		 */
		private final Player player;
		
		private int burningLength = 20; //Jeppa: TODO: move this to config? (see BurningBlockContainer...)
		private int spreadRange = 5;
		
		public PlayerFireFireballController(LimitedED dragon, ITargetController iTargetController, Player player) {
			super(dragon, iTargetController);
			
			this.player = player;
			Bukkit.getPluginManager().registerEvents(this, plugin);
		}
		
		
		@EventHandler
		public void fireFireball(PlayerDropItemEvent event){
			if(event.getPlayer() != player){
				return;
			}
			
			boolean Breath = plugin.getPermissionManager().checkPermissionsSilent(player, PermissionNode.shootFirebreath);
			boolean Balls = plugin.getPermissionManager().checkPermissionsSilent(player, PermissionNode.shootFireballs);
			if((!Balls)&&(!Breath)){
				return;
			}
			
			if(dragon == null || dragon.getBukkitEntity().isDead()){
				HandlerList.unregisterAll(this);
				return;
			}
			
			event.setCancelled(true); 
			//Jeppa: Test for Item
			if(Balls && event.getItemDrop().getItemStack().getType().equals(Material.valueOf(plugin.interactConfig().config_fireItem1()))){
				FireTheBall();
			} else // Firebreath...?
				if(Breath && event.getItemDrop().getItemStack().getType().equals(Material.valueOf(plugin.interactConfig().config_fireItem2()))){ 
					FireTheBreath();
			}
		}
		
		@EventHandler
		public void playerQuit(PlayerQuitEvent event){
			if(event.getPlayer() == player){
				targetController.getDragon().remove();
				HandlerList.unregisterAll(this);
			}
			
		}

		@EventHandler
		public void playerKicked(PlayerKickEvent event){
			if(event.getPlayer() == player){
				targetController.getDragon().remove();
				HandlerList.unregisterAll(this);
			}
			
		}
		

		//Jeppa: New Handler for klick (left/right)
		@EventHandler
		public void fireFireball2(PlayerInteractEvent event){
			if(event.getPlayer() != player){
				return;
			}
			
			boolean Breath = plugin.getPermissionManager().checkPermissionsSilent(player, PermissionNode.shootFirebreath);
			boolean Balls = plugin.getPermissionManager().checkPermissionsSilent(player, PermissionNode.shootFireballs);
			if((!Balls)&&(!Breath)){
				return;
			}
			
			if(dragon == null || dragon.getBukkitEntity().isDead()){
				HandlerList.unregisterAll(this);
				return;
			}
			
			event.setCancelled(true); 
			//Jeppa: test for Item
			if(Balls && event.getItem()!=null && event.getItem().getType().equals(Material.valueOf(plugin.interactConfig().config_fireItem1()))){
				FireTheBall();
			} else // Hier nun noch den Firebreath!!!  
				if(Breath && event.getItem().getType().equals(Material.valueOf(plugin.interactConfig().config_fireItem2()))){ 
					FireTheBreath();
				}
		}
		
		private void FireTheBall(){
			List<Block> locs = player.getLineOfSight((HashSet<Byte>)null, 200); 
			for(Block block : locs){
				if(block.getType() != Material.AIR){
					Location loc = block.getLocation();
					this.fireFireballOnLocation(loc);
					break;
				}
			}
		}
		private void FireTheBreath(){
			int time = 20;
			Vector direction = player.getEyeLocation().getDirection();
			final FireBreath testBreath = new FireBreath(player.getLocation(), direction, null);
			Runnable breathTicker = new Runnable() {
				@Override 
				public void run() {
					while(testBreath.tick()); {	//Jeppa : while true = direkt weiter/nochmal 
					}
				}
			};
			final int ThisTaskInt = Bukkit.getServer().getScheduler().runTaskTimer(plugin, breathTicker, 5, time).getTaskId();
			Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable(){
				public void run(){
					Bukkit.getServer().getScheduler().cancelTask(ThisTaskInt); //Jeppa: Timer-Task beenden
				}
			}, (burningLength+spreadRange+5)*time);
		}

	}

}
