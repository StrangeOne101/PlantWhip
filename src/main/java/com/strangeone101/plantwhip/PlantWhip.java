package com.strangeone101.plantwhip;

import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.ability.PlantAbility;
import com.projectkorra.projectkorra.ability.util.Collision;
import com.projectkorra.projectkorra.ability.util.CollisionInitializer;
import com.projectkorra.projectkorra.ability.util.CollisionManager;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.TempBlock;
import com.projectkorra.projectkorra.waterbending.plant.PlantRegrowth;
import commonslang3.projectkorra.lang3.tuple.Pair;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class PlantWhip extends PlantAbility implements AddonAbility, Listener {

    private static HashMap<FallingBlock, Pair<PlantWhip, Long>> fallingBlocks = new HashMap<>();
    private static final boolean DEBUG = true;

    @Attribute(Attribute.COOLDOWN)
    private long cooldown = 3000;
    @Attribute(Attribute.RANGE)
    private double range = 25;
    @Attribute(Attribute.SELECT_RANGE)
    private double selectRange = 55;
    @Attribute(Attribute.DAMAGE)
    private double damage = 3;
    @Attribute(Attribute.SPEED)
    private double speed = 0.6;
    @Attribute("CreateDome")
    private boolean createDome = true;
    @Attribute("DomeHollow")
    private boolean hollowDome = true;
    @Attribute("DomeRadius")
    private int domeRadius = 2;
    @Attribute(Attribute.DURATION)
    private long duration = 10000;
    @Attribute("DomeDuration")
    private long domeDuration = 30000;
    @Attribute("FallWhenBroken")
    private boolean fallWhenBroken = true;
    private boolean onlyLeaves = false;
    @Attribute("DamageOnce")
    private boolean damageOnce = false;

    private Location location;
    private Location target;
    private Block source;
    private Material blockType;
    private boolean randomFlowering;
    private List<Block> vine = new ArrayList<>();
    private HashMap<Block, TempBlock> vineTempBlocks = new HashMap<>();
    private double distanceTraveled;
    private List<Entity> damagedEntities = new ArrayList<>();
    private boolean makeDome;

    private List<Block> brokenVine = new ArrayList<>();
    private HashMap<Block, TempBlock> brokenVineTempBlocks = new HashMap<>();
    private List<FallingBlock> fallingBlocksInstance = new ArrayList<>();


    public PlantWhip(Player player) {
        super(player);

        Collection<PlantWhip> existing = getAbilities(player, PlantWhip.class);
        for (PlantWhip ability : existing) {
            if (ability.target == null) {
                ability.remove();
            }
        }

        setFields();

        this.source = getPlantSourceBlock(player, selectRange, onlyLeaves);
        if (this.source != null) {
            this.location = this.source.getLocation();

            this.blockType = getType(this.source);
            if (this.blockType.name().equalsIgnoreCase("AZALEA_LEAVES")) {
                this.randomFlowering = true;
            }

            start();
        }
    }

    public void click() {
        Location newTarget = GeneralMethods.getTargetedLocation(player, selectRange, false);
        if (target == null) { //Remove the source
            new PlantRegrowth(this.player, this.source);
            this.source.setType(Material.AIR, false);
            bPlayer.addCooldown(this);
            this.location.add(this.location.clone().subtract(newTarget).getDirection().clone()); //Start 1 block ahead of the source. Hopefully prevent it ending early
            addBlock(this.source);

        }

        this.target = newTarget;
    }

    public void setFields() {
        //Get the values from the config file from ExtraAbilities.StrangeOne101.PlantWhip
        this.cooldown = PlantWhip.getConfig().getLong("ExtraAbilities.StrangeOne101.PlantWhip.Cooldown");
        this.range = PlantWhip.getConfig().getDouble("ExtraAbilities.StrangeOne101.PlantWhip.Range");
        this.selectRange = PlantWhip.getConfig().getDouble("ExtraAbilities.StrangeOne101.PlantWhip.SelectRange");
        this.damage = PlantWhip.getConfig().getDouble("ExtraAbilities.StrangeOne101.PlantWhip.Damage");
        this.speed = PlantWhip.getConfig().getDouble("ExtraAbilities.StrangeOne101.PlantWhip.Speed");
        this.createDome = PlantWhip.getConfig().getBoolean("ExtraAbilities.StrangeOne101.PlantWhip.CreateDome");
        this.hollowDome = PlantWhip.getConfig().getBoolean("ExtraAbilities.StrangeOne101.PlantWhip.DomeHollow");
        this.domeRadius = PlantWhip.getConfig().getInt("ExtraAbilities.StrangeOne101.PlantWhip.DomeRadius");
        this.duration = PlantWhip.getConfig().getLong("ExtraAbilities.StrangeOne101.PlantWhip.Duration");
        this.fallWhenBroken = PlantWhip.getConfig().getBoolean("ExtraAbilities.StrangeOne101.PlantWhip.FallWhenBroken");
        this.onlyLeaves = PlantWhip.getConfig().getBoolean("ExtraAbilities.StrangeOne101.PlantWhip.OnlyLeaves");
        this.damageOnce = PlantWhip.getConfig().getBoolean("ExtraAbilities.StrangeOne101.PlantWhip.DamageOnce");
        this.domeDuration = PlantWhip.getConfig().getLong("ExtraAbilities.StrangeOne101.PlantWhip.DomeDuration");
    }

    @Override
    public void progress() {
        if (this.target == null) {
            if (!bPlayer.canBend(this) || player.getWorld() != source.getWorld()) {
                remove();
                return;
            }

            playFocusWaterEffect(source);
            return;
        }

        if (player.isSneaking()) {
            makeDome = true;
        }

        if (this.distanceTraveled < this.range) {
            Vector direction = target.toVector().subtract(location.toVector()).normalize();
            double d = 0;
            boolean playedSound = false;
            do {
                d += 0.5;
                if (d > speed) d = speed;
                Location cloned = location.clone().add(direction.clone().multiply(d));
                Block block = cloned.getBlock();

                //Damage players and mobs around the end of the vine
                for (Entity e : GeneralMethods.getEntitiesAroundPoint(cloned, 0.8)) {
                    if (e instanceof LivingEntity && !(e instanceof ArmorStand) && !damagedEntities.contains(e) && e != player) {
                        DamageHandler.damageEntity(e, damage, this);
                        damagedEntities.add(e);

                        if (damageOnce) {
                            this.distanceTraveled = this.range; //Stop going anything further
                            return;
                        }

                        if (makeDome && createDome) {
                            new PlantWhipDome(player, e, domeRadius, !hollowDome, blockType, randomFlowering, domeDuration);
                            this.distanceTraveled = this.range; //Stop going anything further
                            return;
                        }
                    }
                }

                //If it hits a block, stop
                if (block.getType().isSolid() && block != source && !vine.contains(block)) {
                    //Try to not make it go in the ground so easily by testing ONE block up
                    block = block.getRelative(BlockFace.UP);
                    if (block.getType().isSolid() && block != source && !vine.contains(block)) {
                        this.distanceTraveled = this.range; //Stop going anything further
                        return;
                    }
                }

                //Create a new block of the vine if the block location is new
                if (!vine.contains(block)) {
                    addBlock(block);
                    block.getWorld().spawnParticle(Particle.BLOCK_CRACK, block.getLocation(), 16, 0.6, 0.6, 0.6, blockType.createBlockData());
                    if (!playedSound) {
                        playSound(block);
                        playedSound = true;
                    }
                }
            } while (d < speed);

            location.add(direction.clone().multiply(d));
            distanceTraveled += speed;
        }

        //Clear all the blocks that are expiring
        Iterator<Block> it = vine.iterator();
        while (it.hasNext()) {
            Block block = it.next();
            TempBlock tempBlock = vineTempBlocks.get(block);
            if (tempBlock != null) {
                if (tempBlock.getRevertTime() - System.currentTimeMillis() < 100) {
                    if (destroyBlock(block)) it.remove();
                }
            }
        }

        it = brokenVine.iterator();
        while (it.hasNext()) {
            Block block = it.next();
            TempBlock tempBlock = brokenVineTempBlocks.get(block);
            if (tempBlock != null) {
                if (tempBlock.getRevertTime() - System.currentTimeMillis() < 100) {
                    if (destroyBlock(block)) it.remove();
                }
            }
        }

        for (FallingBlock fallingBlock : fallingBlocksInstance) {
            if (fallingBlock.isDead()) {
                placeFallingBlock(fallingBlock);
            }
        }

        if (vine.size() == 0 && brokenVine.size() == 0) {
            remove();
        }
    }

    private void addBlock(Block block) {
        BlockData data = block.getBlockData();
        BlockData newData = blockType.createBlockData();
        if (this.randomFlowering && System.currentTimeMillis() % 4 == 0) {
            newData = Material.getMaterial("FLOWERING_AZALEA_LEAVES").createBlockData();
        }
        ((Leaves)newData).setPersistent(true);
        //Adds support for 1.19 leaves being waterloggable
        if (newData instanceof Waterlogged && data instanceof Waterlogged && ((Waterlogged) data).isWaterlogged()) {
            ((Waterlogged) newData).setWaterlogged(true);
        }
        TempBlock tempBlock = new TempBlock(block, newData, duration + 99);
        vineTempBlocks.put(block, tempBlock);
        vine.add(block);
    }

    public static void playSound(Block block) {
        block.getWorld().playSound(block.getLocation(), Sound.BLOCK_GRASS_BREAK, 0.5F, 1);
    }

    private boolean destroyBlock(Block block) {
        //Revert the tempBlock from vineTempBlocks
        if (vineTempBlocks.containsKey(block)) {
            BlockData data = block.getBlockData();
            vineTempBlocks.get(block).revertBlock();
            vineTempBlocks.remove(block);
            //vine.remove(block);

            //Play broken block particles at the block's location
            block.getWorld().spawnParticle(Particle.BLOCK_CRACK, block.getLocation(), 16, 0.5, 0.5, 0.5, data);
            //block.getWorld().playSound(block.getLocation(), Sound.BLOCK_GRASS_BREAK, 0.5F, 1);
            playSound(block);
            return true;
        }
        return false;
    }

    private void fallBlock(Block block) {
        if (vineTempBlocks.containsKey(block)) {
            TempBlock tb = vineTempBlocks.get(block);
            BlockData data = block.getBlockData();
            tb.revertBlock();
            vineTempBlocks.remove(block);
            vine.remove(block);

            block.getWorld().spawnParticle(Particle.BLOCK_CRACK, block.getLocation(), 8, 0.5, 0.5, 0.5, block.getBlockData());
            FallingBlock fallingblock = block.getWorld().spawnFallingBlock(block.getLocation(), data);
            fallingblock.setDropItem(false);
            fallingblock.setTicksLived(1);
            fallingblock.setHurtEntities(true);

            fallingBlocks.put(fallingblock, Pair.of(this, tb.getRevertTime()));
            fallingBlocksInstance.add(fallingblock);
        }
    }

    /**
     * Checks the surrounding blocks to see if they are solid. If none of them are solid,
     * then the block should fall and return true.
     * @param block The block
     * @return True if no solid blocks are found, false if one or more are found.
     */
    private static boolean shouldFall(Block block) {
        for (BlockFace face : new BlockFace[] {BlockFace.WEST, BlockFace.EAST, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.UP, BlockFace.DOWN}) {
            if (block.getRelative(face).getType().isSolid()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Place a falling block
     * @param block
     */
    private void placeFallingBlock(FallingBlock block) {
        Block b = block.getLocation().getBlock();
        TempBlock tempBlock = new TempBlock(b, block.getBlockData(), fallingBlocks.get(block).getRight() - System.currentTimeMillis());
        this.brokenVine.add(b);
        this.brokenVineTempBlocks.put(b, tempBlock);
        this.fallingBlocksInstance.remove(block);
        fallingBlocks.remove(block);
    }

    private static Material getType(Block block) {
        switch (block.getType().name()) {
            case "SPRUCE_SAPLING":
            case "SWEET_BERRY_BUSH":
            case "SPRUCE_LEAVES": return Material.SPRUCE_LEAVES;
            case "BIRCH_SAPLING":
            case "BIRCH_LEAVES": return Material.BIRCH_LEAVES;
            case "COCOA":
            case "BAMBOO":
            case "BAMBOO_SAPLING":
            case "JUNGLE_SAPLING":
            case "JUNGLE_LEAVES": return Material.JUNGLE_LEAVES;
            case "DARK_OAK_SAPLING":
            case "DARK_OAK_LEAVES": return Material.DARK_OAK_LEAVES;
            case "ACACIA_SAPLING":
            case "ACACIA_LEAVES": return Material.ACACIA_LEAVES;
            case "AZALEA":
            case "FLOWERING_AZALEA":
            case "AZALEA_LEAVES": return Material.getMaterial("AZALEA_LEAVES");
            case "MANGROVE_LEAVES": return Material.getMaterial("MANGROVE_LEAVES");
            case "MANGROVE_PROPAGULE":
            case "MANGROVE_ROOTS": return Material.getMaterial("MANGROVE_ROOTS");
            default: return Material.OAK_LEAVES;
        }
    }

    @Override
    public boolean isSneakAbility() {
        return true;
    }

    @Override
    public boolean isHarmlessAbility() {
        return false;
    }

    @Override
    public long getCooldown() {
        return cooldown;
    }

    @Override
    public String getName() {
        return "PlantWhip";
    }

    @Override
    public Location getLocation() {
        return location;
    }

    @Override
    public void load() {
        getConfig().addDefault("ExtraAbilities.StrangeOne101.PlantWhip.Cooldown", 3_000);
        getConfig().addDefault("ExtraAbilities.StrangeOne101.PlantWhip.Range", 25);
        getConfig().addDefault("ExtraAbilities.StrangeOne101.PlantWhip.SelectRange", 55);
        getConfig().addDefault("ExtraAbilities.StrangeOne101.PlantWhip.Damage", 3.0);
        getConfig().addDefault("ExtraAbilities.StrangeOne101.PlantWhip.Speed", 0.6);
        getConfig().addDefault("ExtraAbilities.StrangeOne101.PlantWhip.CreateDome", true);
        getConfig().addDefault("ExtraAbilities.StrangeOne101.PlantWhip.DomeHollow", true);
        getConfig().addDefault("ExtraAbilities.StrangeOne101.PlantWhip.DomeRadius", 2);
        getConfig().addDefault("ExtraAbilities.StrangeOne101.PlantWhip.Duration", 10_000);
        getConfig().addDefault("ExtraAbilities.StrangeOne101.PlantWhip.FallWhenBroken", true);
        getConfig().addDefault("ExtraAbilities.StrangeOne101.PlantWhip.DamageOnce", false);
        getConfig().addDefault("ExtraAbilities.StrangeOne101.PlantWhip.DomeDuration", 30_000);


        ConfigManager.defaultConfig.save();

        ConfigManager.languageConfig.get().addDefault("Abilities.Water.PlantWhip.Description", "Whip players with a vine stemming from your Plantbending! You can even trap them with the vine if you wish!");
        ConfigManager.languageConfig.get().addDefault("Abilities.Water.PlantWhip.Instructions", "Tap sneak while looking at a plant, and click to fire it towards where you are looking. While it's traveling, you can tap sneak again to trap the target in a dome on hit. Shift clicking the dome releases it.");
        ConfigManager.languageConfig.get().addDefault("Abilities.Water.PlantWhip.DeathMessage", "{victim} was whipped to death by {attacker}'s {ability}");
        ConfigManager.languageConfig.save();

        Bukkit.getPluginManager().registerEvents(this, ProjectKorra.plugin);

        ProjectKorra.getCollisionInitializer().addSmallAbility(this);

    }

    @Override
    public void stop() {}

    @Override
    public String getAuthor() {
        return "StrangeOne101";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public double getCollisionRadius() {
        return 0.75;
    }

    @Override
    public List<Location> getLocations() {
        return vine.stream().map(Block::getLocation).collect(Collectors.toList());
    }

    @Override
    public void handleCollision(Collision collision) {
        //super.handleCollision(collision);
        if (collision.getAbilitySecond() instanceof PlantWhipDome) {
            return;
        }
        if (collision.isRemovingFirst() && collision.getAbilityFirst() instanceof PlantWhip) {
            int amount = 1;
            if (collision.getAbilitySecond().getCollisionRadius() > 1)
                amount = (int)collision.getAbilitySecond().getCollisionRadius();

            Block start = collision.getLocationFirst().getBlock();
            PlantWhip instance = (PlantWhip) collision.getAbilityFirst();
            int index = instance.vine.indexOf(start);
            if (index != -1) {
                //The ones to destroy
                for (int i = 1; i < amount + 1; i++) {
                    int offset = (i / 2) * (i % 2 == 0 ? 1 : -1);
                    int newIndex = index + offset;
                    if (newIndex > 0 && newIndex < instance.vine.size()) {
                        Block block = instance.vine.get(newIndex);
                        instance.destroyBlock(block);
                        instance.vine.remove(block);
                    }
                }

                //Go in both directions
                if (fallWhenBroken) {
                    for (int multiplier : new int[] {1, -1}) {
                        int lastSolid = 0;
                        int startIndex = ((amount) * multiplier) + index;
                        int testingIndex = startIndex;
                        while (testingIndex > 0 && testingIndex < instance.vine.size()) {
                            Block block = instance.vine.get(testingIndex);
                            if (shouldFall(block)) {
                                lastSolid++;
                            } else {
                                break;
                            }
                            if (lastSolid > 2) {
                                int fallIndex = testingIndex - 3;

                                Block fallBlock = instance.vine.get(fallIndex);
                                instance.fallBlock(fallBlock);

                            }
                            testingIndex += multiplier;
                        }
                    }
                }
            }

            //this.remove();
        }
    }

    @Override
    public void remove() {
        super.remove();

        for (TempBlock tb : vineTempBlocks.values()) {
            tb.revertBlock();
        }

        for (TempBlock tb : brokenVineTempBlocks.values()) {
            tb.revertBlock();
        }
    }

    @EventHandler
    public void onFallingBlockPlace(EntityChangeBlockEvent event) {
        if (event.getEntityType() == EntityType.FALLING_BLOCK && fallingBlocks.containsKey((FallingBlock) event.getEntity())) {
            FallingBlock fallingBlock = (FallingBlock) event.getEntity();
            Pair<PlantWhip, Long> pair = fallingBlocks.get(fallingBlock);
            PlantWhip instance = pair.getLeft();
            instance.placeFallingBlock(fallingBlock);

        }
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        if (event.isSneaking()) {
            BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(event.getPlayer());

            if (bPlayer.canBend(this)) {
                new PlantWhip(event.getPlayer());
            }
        }
    }

    @EventHandler
    public void onClick(PlayerAnimationEvent event) {
        BendingPlayer bendingPlayer = BendingPlayer.getBendingPlayer(event.getPlayer());
        if (event.getAnimationType() == PlayerAnimationType.ARM_SWING && bendingPlayer.getBoundAbilityName().equals(getName())) {
            Collection<PlantWhip> instances = CoreAbility.getAbilities(event.getPlayer(), PlantWhip.class);
            for (PlantWhip instance : instances) {
                instance.click();
            }

            if (event.getPlayer().isSneaking()) {
                Collection<PlantWhipDome> domeInstances = CoreAbility.getAbilities(event.getPlayer(), PlantWhipDome.class);
                Location target = GeneralMethods.getTargetedLocation(event.getPlayer(), range + 20);
                for (PlantWhipDome instance : domeInstances) {
                    for (Location loc : instance.getLocations()) {
                        if (loc.getBlock().getLocation().distance(target) < 2) {
                            instance.getLocations().forEach(l -> l.getWorld().spawnParticle(Particle.BLOCK_CRACK, l, 8, 0.5, 0.5, 0.5, l.getBlock().getType().createBlockData()));
                            instance.remove();
                            playSound(instance.getLocation().getBlock()); //The center of it
                            return;
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void asyncChatEvent(AsyncPlayerChatEvent event) {
        if (event.getMessage().equalsIgnoreCase("!debug plantwhip") && DEBUG) {
            event.setCancelled(true);
            Collection<PlantWhip> coll = CoreAbility.getAbilities(PlantWhip.class);
            for (PlantWhip instance : coll) {
                ProjectKorra.log.warning(instance.toString());
            }
            event.getPlayer().sendMessage(ChatColor.GREEN + "Debugged " + coll.size() + " instances of PlantWhip");
        }
    }

}
