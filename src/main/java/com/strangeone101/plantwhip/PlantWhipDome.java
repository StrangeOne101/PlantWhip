package com.strangeone101.plantwhip;

import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.PlantAbility;
import com.projectkorra.projectkorra.ability.util.Collision;
import com.projectkorra.projectkorra.util.TempBlock;
import commonslang3.projectkorra.lang3.tuple.Pair;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class PlantWhipDome extends PlantAbility implements AddonAbility {

    private static HashMap<Block, Pair<PlantWhipDome, TempBlock>> staticBlocks = new HashMap<>();

    private int radius;
    private boolean fill;
    private Material material;
    private Entity target;
    private List<Block> blocks = new ArrayList<>();
    private HashMap<Block, TempBlock> tempBlocks = new HashMap<>();
    private boolean playedSound;

    public PlantWhipDome(Player player, Entity target, int radius, boolean fill, Material material, boolean randomFlowering, long duration) {
        super(player);

        this.target = target;
        this.radius = radius;
        this.fill = fill;
        this.material = material;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    double distanceSqr = x * x + y * y + z * z;
                    double distance = (int)Math.sqrt(distanceSqr);

                    if ((distance >= radius && distance < radius + 1) || (fill && distance <= radius)) {
                        Location location = target.getLocation().clone().add(x, y, z);
                        Block block = location.getBlock();
                        if (!block.getType().isSolid()) {
                            blocks.add(block);
                            BlockData data = block.getBlockData();
                            BlockData newData = material.createBlockData();
                            if (randomFlowering && System.currentTimeMillis() % 4 == 0) {
                                newData = Material.getMaterial("FLOWERING_AZALEA_LEAVES").createBlockData();
                            }
                            ((Leaves)newData).setPersistent(true);
                            //Adds support for 1.19 leaves being waterloggable
                            if (newData instanceof Waterlogged && data instanceof Waterlogged && ((Waterlogged) data).isWaterlogged()) {
                                ((Waterlogged) newData).setWaterlogged(true);
                            }
                            TempBlock tempBlock = new TempBlock(block, newData, duration + 99);
                            tempBlocks.put(block, tempBlock);
                        }
                    }
                }
            }
        }

        start();
    }

    @Override
    public void progress() {
        Iterator<Block> iterator = blocks.iterator();
        while (iterator.hasNext()) {
            Block block = iterator.next();
            TempBlock tempBlock = tempBlocks.get(block);
            if (tempBlock != null) {
                if (tempBlock.getRevertTime() - System.currentTimeMillis() < 100) {
                    if (destroyBlock(block)) iterator.remove();
                }
            }
        }

        playedSound = false;

        if (blocks.size() == 0) {
            remove();
        }
    }

    private boolean destroyBlock(Block block) {
        //Revert the tempBlock from vineTempBlocks
        if (tempBlocks.containsKey(block)) {
            BlockData data = block.getBlockData();
            tempBlocks.get(block).revertBlock();
            tempBlocks.remove(block);
            staticBlocks.remove(block);
            //blocks.remove(block);

            //Play broken block particles at the block's location
            block.getWorld().spawnParticle(Particle.BLOCK_CRACK, block.getLocation(), 16, 0.5, 0.5, 0.5, data);
            //block.getWorld().playSound(block.getLocation(), Sound.BLOCK_GRASS_BREAK, 0.5F, 1);
            if (!playedSound) {
                PlantWhip.playSound(block);
                playedSound = true;
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean isSneakAbility() {
        return false;
    }

    @Override
    public boolean isHarmlessAbility() {
        return true;
    }

    @Override
    public long getCooldown() {
        return 0;
    }

    @Override
    public String getName() {
        return "PlantWhipDome";
    }

    @Override
    public Location getLocation() {
        return target.getLocation();
    }

    @Override
    public List<Location> getLocations() {
        return blocks.stream().map(Block::getLocation).collect(Collectors.toList());
    }

    @Override
    public double getCollisionRadius() {
        return 0.75;
    }

    @Override
    public void handleCollision(Collision collision) {
        //super.handleCollision(collision);

        if (collision.isRemovingFirst() && collision.getAbilityFirst() instanceof PlantWhipDome) {
            Iterator<Block> iterator = blocks.iterator();
            while (iterator.hasNext()) {
                Block block = iterator.next();
                if (block.getLocation().distance(collision.getLocationSecond()) <= collision.getAbilitySecond().getCollisionRadius() + 0.3) {
                    if (destroyBlock(block)) iterator.remove();
                }
            }
        }
    }

    @Override
    public boolean isHiddenAbility() {
        return true;
    }

    @Override
    public void load() {

    }

    @Override
    public void stop() {

    }

    @Override
    public String getAuthor() {
        return "StrangeOne101";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public void remove() {
        super.remove();

        for (TempBlock tb : tempBlocks.values()) {
            tb.revertBlock();
        }

        for (Block b : blocks) {
            staticBlocks.remove(b);
        }
    }
}
