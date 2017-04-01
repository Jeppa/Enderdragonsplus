package de.tobiyas.enderdragonsplus.entity.firebreath;


import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.util.Vector;

import de.tobiyas.enderdragonsplus.entity.dragon.LimitedED;

public class FireBreath {

	private Location location;
	private Vector direction;
	private LimitedED shooter;
	
	private double speed = 1;
	private boolean alive = true;
	private boolean alreadyHit = false;
	private int distance= 200; //Jeppa: max Entfernung für Breath
	
	boolean TimerReset = false;
	
	private BurningBlocksContainer blockContainer;
	
	
	public FireBreath(Location location, Vector direction, LimitedED shooter) {
		this.location = location;
		this.shooter = shooter;

		this.direction = direction.normalize();
		
		blockContainer = new BurningBlocksContainer();
	}
	
	/**
	 * Ticks this pseudo Entity
	 * Returns true if the entity is still alive.
	 * Returns false if the entity is dead and will not be called.
	 * 
	 * @return
	 */
	public boolean tick(){
		if(!alive){
			return false; 
		}
		// --> Jeppa Neu:
		if(!alreadyHit){						//Jeppa: noch kein Treffer ? -> Kollision abfragen...
			checkCollision(); 					//wenn Treffer (nicht AIR oder Feuer) Block eintragen in Liste und alreadyHit auf true setzen  
			if(!alreadyHit) {					//wenn nicht:
				tickLocation(); 				//Erhöht die Location in Blickrichtung um einen Block....
				distance--;						//max. Entfernung ... bei 0 return false.. solange bis TimerTask beendet wird...
				if (distance >0) { 
					return true; 				//Routine im Taskaufruf nicht beenden -> nächster Aufruf sofort, nicht erst bei nächstem Aufruf 
				} else this.alive = false; 		//Notfallabbruch (bei Schuss ins Leere...)
			} else 	{							//Treffer! 
				spreadFire(); 					//
			}
		}else{ 									//weiter ticken
			spreadFire(); 						//
		}
		return false;							
	}
	
	private void checkCollision(){
		Material mat = location.getBlock().getType();
		if((mat != Material.AIR) && (mat != Material.FIRE)){	
			alreadyHit = true; 			
			if ((mat.isSolid()) || (mat == Material.STATIONARY_LAVA) || (mat == Material.LAVA)) antitickLocation();	//Jeppa: einen Block zurück 
			Location Blockloc = location.getBlock().getLocation();	
			if (mat != Material.STATIONARY_WATER) blockContainer.addBlock(Blockloc);
			else this.alive = false;
		}
	}
	
	//spreads the fire one block in all directions , ticks the blocks
	private void spreadFire(){
		blockContainer.tick();
		if(blockContainer.areAllTicksDone()){
			this.alive = false; 							
		}
	}
	
	private void tickLocation(){
		this.location = location.add(direction); 	
		location.getWorld().playEffect(location, Effect.MOBSPAWNER_FLAMES, 1); //Fireeffect
	}
	private void antitickLocation(){
		this.location = location.subtract(direction); 				// Location -1
	}
	
	/**
	 * Updates speed + Vector of speed
	 * @param newSpeed
	 */
	public void setSpeed(double newSpeed){
		this.speed = newSpeed;
		
		this.direction = direction.normalize().multiply(newSpeed);
	}
	
	public double getSpeed(){
		return speed;
	}

	public LimitedED getShooter() {
		return shooter;
	}
}
