package io.github.yannici.bedwars.Game;

import io.github.yannici.bedwars.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.material.Bed;
import org.bukkit.material.Lever;
import org.bukkit.material.Redstone;

public class Region {
	
	public final static int CHUNK_SIZE = 16;
	
	private Location minCorner = null;
	private Location maxCorner = null;
	private World world = null;
	private String name = null;
	private List<Block> placedBlocks = null;
	private List<Block> breakedBlocks = null;
	private HashMap<Block, Integer> breakedBlockTypes = null;
	private HashMap<Block, Byte> breakedBlockData = null;
	private List<Block> placedUnbreakableBlocks = null;
	private HashMap<Block, Boolean> breakedBlockPower = null;
	private List<Inventory> inventories = null;

	public Region(Location pos1, Location pos2, String name) {
		if (pos1 == null || pos2 == null) {
			return;
		}

		if (!pos1.getWorld().getName().equals(pos2.getWorld().getName())) {
			return;
		}
		
		this.world = pos1.getWorld();
		this.setMinMax(pos1, pos2);
		this.placedBlocks = new ArrayList<Block>();
		this.breakedBlocks = new ArrayList<Block>();
		this.breakedBlockTypes = new HashMap<Block, Integer>();
		this.breakedBlockData = new HashMap<Block, Byte>();
		this.placedUnbreakableBlocks = new ArrayList<Block>();
		this.breakedBlockPower = new HashMap<Block, Boolean>();
		this.inventories = new ArrayList<Inventory>();
		
		this.name = name;
	}

	public Region(World w, int x1, int y1, int z1, int x2, int y2, int z2) {
		this(new Location(w, x1, y1, z1), new Location(w, x2, y2, z2), w.getName());
	}

	public boolean check() {
		return (this.minCorner != null && this.maxCorner != null && this.world != null);
	}

	private void setMinMax(Location pos1, Location pos2) {
		this.minCorner = this.getMinimumCorner(pos1, pos2);
		this.maxCorner = this.getMaximumCorner(pos1, pos2);
	}

	private Location getMinimumCorner(Location pos1, Location pos2) {
		return new Location(this.world, Math.min(pos1.getBlockX(),
				pos2.getBlockX()),
				Math.min(pos1.getBlockY(), pos2.getBlockY()), Math.min(
						pos1.getBlockZ(), pos2.getBlockZ()));
	}

	private Location getMaximumCorner(Location pos1, Location pos2) {
		return new Location(this.world, Math.max(pos1.getBlockX(),
				pos2.getBlockX()),
				Math.max(pos1.getBlockY(), pos2.getBlockY()), Math.max(
						pos1.getBlockZ(), pos2.getBlockZ()));
	}
	
	public List<Inventory> getInventories() {
	    return this.inventories;
	}
	
	public void addInventory(Inventory inventory) {
	    this.inventories.add(inventory);
	}

	public boolean isInRegion(Location location) {
		if(!location.getWorld().equals(this.world)) {
			return false;
		}
		
		return (location.getBlockX() >= this.minCorner.getBlockX()
				&& location.getBlockX() <= this.maxCorner.getBlockX()
				&& location.getBlockY() >= this.minCorner.getBlockY()
				&& location.getBlockY() <= this.maxCorner.getBlockY()
				&& location.getBlockZ() >= this.minCorner.getBlockZ() && location
				.getBlockZ() <= this.maxCorner.getBlockZ());
	}
	
	public boolean chunkIsInRegion(Chunk chunk) {
	    if(chunk.getX() >= this.minCorner.getX()
	            && chunk.getX() <= this.maxCorner.getX()
	            && chunk.getZ() >= this.minCorner.getZ()
	            && chunk.getZ() <= this.maxCorner.getZ()) {
	        return true;
	    }
	    
	    return false;
	}
	
	public boolean chunkIsInRegion(double x, double z) {
	    if(x >= this.minCorner.getX()
	            && x <= this.maxCorner.getX()
	            && z >= this.minCorner.getZ()
	            && z <= this.maxCorner.getZ()) {
	        return true;
	    }
	    
	    return false;
	}

	@SuppressWarnings("deprecation")
    public void reset(Game game) {
		this.loadChunks();
		for(Inventory inventory : this.inventories) {
		    inventory.clear();
		}
		
		for(Block placed : this.placedBlocks) {
		    Block blockInWorld = this.world.getBlockAt(placed.getLocation());
		    if(blockInWorld.getType() == Material.AIR) {
		        continue;
		    }
		    
		    if(blockInWorld.equals(placed)) {
		        blockInWorld.setType(Material.AIR);
		    }
		}
		
		this.placedBlocks.clear();
		
		for(Block placed : this.placedUnbreakableBlocks) {
		    Block blockInWorld = this.world.getBlockAt(placed.getLocation());
		    if(blockInWorld.getType() == Material.AIR) {
		        continue;
		    }
		    
		    if(blockInWorld.getLocation().equals(placed.getLocation())) {
		        blockInWorld.setType(Material.AIR);
		    }
		}
		
		this.placedUnbreakableBlocks.clear();
		
		for(Block block : this.breakedBlocks) {
		    Block theBlock = this.getWorld().getBlockAt(block.getLocation());
		    theBlock.setTypeId(this.breakedBlockTypes.get(block));
		    theBlock.setData(this.breakedBlockData.get(block));
		    
		    if(theBlock.getState().getData() instanceof Lever) {
		        Lever attach = (Lever)theBlock.getState().getData();
		        BlockState supportState = theBlock.getState();
		        BlockState initalState = theBlock.getState();
		        attach.setPowered(this.breakedBlockPower.get(block));
		        theBlock.getState().setData(attach);
		        
		        supportState.setType(Material.AIR);
		        supportState.update(true, false);
		        initalState.update(true);
		    } else {
		        theBlock.getState().update(true, true);
		    }
		}
		
		this.breakedBlocks.clear();
		
		Material targetMaterial = Utils.getMaterialByConfig("game-block", Material.BED_BLOCK);
		for(Team team : game.getTeams().values()) {
			if(team.getHeadTarget() == null) {
				continue;
			}
			
			if((targetMaterial.equals(Material.BED_BLOCK)
					|| targetMaterial.equals(Material.BED))
					&& team.getFeetTarget() != null) {
				Block blockHead = this.world.getBlockAt(team.getHeadTarget().getLocation());
				Block blockFeed = this.world.getBlockAt(team.getFeetTarget().getLocation());
				BlockState headState = blockHead.getState();
				BlockState feedState = blockFeed.getState();
				
				headState.setType(Material.BED_BLOCK);
				feedState.setType(Material.BED_BLOCK);
				headState.setRawData((byte)0x0);
				feedState.setRawData((byte)0x8);
				feedState.update(true, false);
				headState.update(true, false);
				
				Bed bedHead = (Bed)headState.getData();
				bedHead.setHeadOfBed(true);
				bedHead.setFacingDirection(blockHead.getFace(blockFeed).getOppositeFace());
				
				Bed bedFeed = (Bed)feedState.getData();
				bedFeed.setHeadOfBed(false);
				bedFeed.setFacingDirection(blockFeed.getFace(blockHead));
				
				feedState.update(true, false);
				headState.update(true, true);
			} else {
				Block blockHead = this.world.getBlockAt(team.getHeadTarget().getLocation());
				BlockState headState = blockHead.getState();
				
				headState.setType(targetMaterial);
				headState.update(true, true);
			}
		}
		
		Iterator<Entity> entityIterator = this.world.getEntities().iterator();
		while (entityIterator.hasNext()) {
			Entity e = entityIterator.next();
			
			if(!this.isInRegion(e.getLocation())) {
			    continue;
			}
			
			if (e instanceof Item) {
				e.remove();
				continue;
			}
			
			if(e.getType().equals(EntityType.CREEPER)
			        || e.getType().equals(EntityType.CAVE_SPIDER)
			        || e.getType().equals(EntityType.SPIDER)
			        || e.getType().equals(EntityType.ZOMBIE)
			        || e.getType().equals(EntityType.SKELETON)
			        || e.getType().equals(EntityType.SILVERFISH)
			        || e.getType().equals(EntityType.ARROW)) {
			    e.remove();
			    continue;
			}

			if (e instanceof LivingEntity) {
				LivingEntity le = (LivingEntity) e;
				le.setRemoveWhenFarAway(false);
			}
		}
	}

	public World getWorld() {
		return this.minCorner.getWorld();
	}

    public boolean isPlacedBlock(Block block) {
       return this.placedBlocks.contains(block);
    }

    @SuppressWarnings("deprecation")
	public void addPlacedBlock(Block placeBlock, BlockState replacedBlock) {
        this.placedBlocks.add(placeBlock);
        if(replacedBlock != null) {
        	this.breakedBlockTypes.put(replacedBlock.getBlock(), replacedBlock.getTypeId());
        	this.breakedBlockData.put(replacedBlock.getBlock(), replacedBlock.getData().getData());
        	
        	this.breakedBlocks.add(replacedBlock.getBlock());
        }
    }
    
    public void removePlacedBlock(Block block) {
        this.placedBlocks.remove(block);
    }
    
    public String getName() {
    	if(this.name == null) {
    		this.name = this.world.getName();
    	}
    	
    	return this.name;
    }
    
    @SuppressWarnings("deprecation")
    public void addBreakedBlock(Block bedBlock) {
        this.breakedBlockTypes.put(bedBlock, bedBlock.getTypeId());
        this.breakedBlockData.put(bedBlock, bedBlock.getData());
        
        if(bedBlock.getState().getData() instanceof Redstone) {
            this.breakedBlockPower.put(bedBlock, ((Redstone)bedBlock.getState().getData()).isPowered());
        }
        
        this.breakedBlocks.add(bedBlock);
    }
    
	@SuppressWarnings("deprecation")
    public void addPlacedUnbreakableBlock(Block placed, BlockState replaced) {
		this.placedUnbreakableBlocks.add(placed);
        if(replaced != null) {
            this.breakedBlockTypes.put(replaced.getBlock(), replaced.getTypeId());
            this.breakedBlockData.put(replaced.getBlock(), replaced.getData().getData());
            this.breakedBlocks.add(replaced.getBlock());
            
            if(replaced.getData() instanceof Redstone) {
                this.breakedBlockPower.put(placed, ((Redstone)replaced.getData()).isPowered());
            }
        }
	}

	public void removePlacedUnbreakableBlock(Block block) {
		this.placedUnbreakableBlocks.remove(block);
	}

	public void loadChunks() {
		int minX = (int)Math.floor(this.minCorner.getX());
		int maxX = (int)Math.ceil(this.maxCorner.getX());
		int minZ = (int)Math.floor(this.minCorner.getZ());
		int maxZ = (int)Math.ceil(this.maxCorner.getZ());
		
		for(int x = minX; x <= maxX; x += Region.CHUNK_SIZE) {
			for(int z = minZ; z <= maxZ; z += Region.CHUNK_SIZE) {
				Chunk chunk = this.world.getChunkAt(x, z);
				if(!chunk.isLoaded()) {
					chunk.load(true);
				}
			}
		}
	}

    public boolean isPlacedUnbreakableBlock(Block clickedBlock) {
        return this.placedUnbreakableBlocks.contains(clickedBlock);
    }

}
