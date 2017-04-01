package de.tobiyas.enderdragonsplus.entity.firebreath;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.Calendar;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

public class BurningBlocksContainer{

	private List<BurningBlock> burningBlocks;
	private List<BurningBlock> spreadBlocks;
	
	private boolean allTicksDone = false;
	
	private int burningLength = 20; //TODO: -> in Config ? see CommandRide ...!
	private int spreadRange = 4;
	private int spreadRangeBase = spreadRange;
	
	public BurningBlocksContainer() {
		burningBlocks = new LinkedList<BurningBlock>();
		spreadBlocks = new LinkedList<BurningBlock>();
	}
	
	//spreads the fire one block in all directions
	public void tick(){ 
		boolean somethingTicked = false;
		
		if(spreadRange >= 0){
			somethingTicked = true;
			spreadRange--; 
			int lesstime=(spreadRangeBase-spreadRange);	
			for(BurningBlock block : burningBlocks){
				if(!block.isDone()){	
					block.tick();		
				} 
				Location Block1Loc = block.getLocation();
				if (Block1Loc.getBlock().getType() == Material.FIRE) {
					//Jeppa: only burning blocks...
					for (double x=-1; x<=1; x++) {
						for (double y=-1; y<=1; y++) {
							for (double z=-1; z<=1; z++) {
								Location BlockLoc = new Location(Block1Loc.getWorld(),Block1Loc.getX()+x,Block1Loc.getY()+y,Block1Loc.getZ()+z);
								Material mat = BlockLoc.getBlock().getType();
								if((!mat.isSolid()) && (mat != Material.FIRE) && (!BlockLoc.getBlock().isLiquid())){
									addSpreadBlock(BlockLoc , lesstime); 
								} else BlockLoc = null; 
							}
						}
					}
				}
			}
			for(BurningBlock block : spreadBlocks){
				burningBlocks.add(block); //Jeppa: move blocks from spreadBlocks to burningBlocks 
			}
			spreadBlocks=new LinkedList<BurningBlock>();
		} else {
			for(BurningBlock block : burningBlocks){
				if(!block.isDone()){
					block.tick(); 
					somethingTicked = true; 
				} 
			}
		}
	
		if(!somethingTicked){
			allTicksDone = true; 
			burningBlocks=new LinkedList<BurningBlock>();	
		} 
	}
	
	public boolean containsLocation(Location location){ 
		for(BurningBlock block : burningBlocks){
			if(block.getLocation().equals(location)){ 
				return true;				
			}
		}
		return false;
	}

	
	public boolean areAllTicksDone() {
		return allTicksDone;
	}
	
	public void addBlock(Location location){ 
		if (!hasBlock(location,burningBlocks))
		burningBlocks.add(new BurningBlock(location, burningLength));
	}

	public void addSpreadBlock(Location location, int lesstime){ 
		if ((!hasBlock(location, spreadBlocks)) && (!hasBlock(location, burningBlocks))) { 
			int ReducedBurnTime=(int) Math.floor((burningLength-lesstime)*0.8); 
			if (ReducedBurnTime>0) spreadBlocks.add(new BurningBlock(location, ReducedBurnTime)); 
		}
	}
	//Jeppa: new CheckBlock Routine 
	public boolean hasBlock(Location location, List <BurningBlock> Liste ){ 
		for (BurningBlock Block : Liste) {
			if ((Block.getLocation() == location) || (Block.getLocation().equals(location)) || (Block.isDone())) { 
				return true;
			}
		}
		return false;
	}

}
