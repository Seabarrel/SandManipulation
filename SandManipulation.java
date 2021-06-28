package me.seabarrel.SandManipulation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.SandAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.command.Commands;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import com.projectkorra.projectkorra.util.TempBlock;

public class SandManipulation extends SandAbility implements AddonAbility{

    @Attribute(Attribute.COOLDOWN)
    private long cooldown;
    @Attribute(Attribute.RANGE)
    private int range;
    @Attribute(Attribute.SELECT_RANGE)
    private int sourceRange;
    @Attribute("CREATE_SAND_ON_LAND")
    private boolean createSandOnLand;
    @Attribute(Attribute.RADIUS)
    private int radius;
    @Attribute("DEPTH")
    private int depth;
    @Attribute(Attribute.DURATION)
    private long revertTime;
    @Attribute(Attribute.DAMAGE)
    private double hitDamage;
    @Attribute("BLINDNESS")
    private boolean hitBlindness;
    @Attribute("BLINDNESS_DURATION")
    private long hitBlindnessDuration;
    @Attribute("BURST_COOLDOWN")
    private long burstCooldown;
    @Attribute("AMOUNT")
    private int amount;
    @Attribute("VELOCITY")
    private int velocity;
    @Attribute("BURST_DAMAGE")
    private double burstDamage;
    @Attribute("BURST_BLINDNESS")
    private boolean burstBlindness;
    @Attribute("BURST_BLINESNESS_DURATION")
    private long burstBlindnessDuration;
    @Attribute("PARTICLES")
    private boolean particles;

    private Block block;
    private TempBlock sourceBlock;
    private Material sourceType;
    private Material shootType;
    private Location location;
    private boolean started;
    private Location targetLocation;
    private Vector direction;
    private boolean replace;
    private boolean entitiesAreHurt;
    private boolean burst;
    private long burstStart;
    private FallingBlock fb;
    private ArrayList<FallingBlock> burstBlocks;
    private boolean burstSandOnLand;
    private ArrayList<Entity> alreadyHitEntities;

    public SandManipulation(Player player) {
        super(player);

        if (!bPlayer.canBend(this)) return;

        FileConfiguration config = ConfigManager.getConfig();
        cooldown = config.getLong("ExtraAbilities.Seabarrel.SandManipulation.Cooldown");
        range = config.getInt("ExtraAbilities.Seabarrel.SandManipulation.Range");
        sourceRange = config.getInt("ExtraAbilities.Seabarrel.SandManipulation.SourceRange");
        createSandOnLand = config.getBoolean("ExtraAbilities.Seabarrel.SandManipulation.CreateSandOnLand");
        radius = config.getInt("ExtraAbilities.Seabarrel.SandManipulation.CreateSandRadius");
        depth = config.getInt("ExtraAbilities.Seabarrel.SandManipulation.CreateSandDepth");
        revertTime = config.getLong("ExtraAbilities.Seabarrel.SandManipulation.CreateSandRevertTime");

        hitDamage = config.getInt("ExtraAbilities.Seabarrel.SandManipulation.Damage");
        hitBlindness = config.getBoolean("ExtraAbilities.Seabarrel.SandManipulation.Blindness");
        hitBlindnessDuration = config.getLong("ExtraAbilities.Seabarrel.SandManipulation.BlindnessDuration");

        burstCooldown = config.getLong("ExtraAbilities.Seabarrel.SandManipulation.Burst.Cooldown");
        amount = config.getInt("ExtraAbilities.Seabarrel.SandManipulation.Burst.Blocks");
        velocity = config.getInt("ExtraAbilities.Seabarrel.SandManipulation.Burst.Velocity");
        burstSandOnLand = config.getBoolean("ExtraAbilities.Seabarrel.SandManipulation.Burst.CreateSandOnLand");

        burstDamage = config.getDouble("ExtraAbilities.Seabarrel.SandManipulation.Burst.Damage");
        burstBlindness = config.getBoolean("ExtraAbilities.Seabarrel.SandManipulation.Burst.Blindness");
        burstBlindnessDuration = config.getLong("ExtraAbilities.Seabarrel.SandManipulation.Burst.BlindnessDuration");
        particles = config.getBoolean("ExtraAbilities.Seabarrel.SandManipulation.Burst.Particles");

        if (prepare()) {
            start();
        } else {
            remove();
        }
    }

    @Override
    public void progress() {

        if (!bPlayer.canBendIgnoreBindsCooldowns(this)) {
            remove();
            return;
        }

        if (burst) {
            burst();
            return;
        }

        if (sourceBlock.getLocation().distanceSquared(player.getLocation()) > sourceRange * sourceRange && !started) {
            removeWithSource();
        }

        if (!bPlayer.getBoundAbilityName().equalsIgnoreCase(getName())) {
            if (started) {
                removeWithCooldown();
            } else {
                removeWithSource();
            }
            return;
        }

        if (!started) {
            if (block.getType() != sourceType) remove();
            return;
        }

        if (GeneralMethods.isRegionProtectedFromBuild(this, location)) {
            remove();
            return;
        }

        HashSet<Material> ignoredMaterials = getTransparentMaterialSet();
        ignoredMaterials.add(Material.SAND);

        final Entity target = GeneralMethods.getTargetedEntity(player, range);
        targetLocation = player.getTargetBlock(ignoredMaterials, range).getLocation();

        if (target != null) {
            targetLocation = target.getLocation();
        }


        direction = GeneralMethods.getDirection(location, targetLocation).normalize();
        location = this.location.clone().add(direction);

        if (location.distanceSquared(player.getLocation()) > range * range) {
            removeWithCooldown();
            return;
        }

        if (!player.isSneaking()) {
            removeWithCooldown();
            return;
        }

        final List<Entity> entities = GeneralMethods.getEntitiesAroundPoint(location, 1);
        for (final Entity entity : entities) {

            if ((entity instanceof Player && Commands.invincible.contains(entity.getName()))
                    || entity instanceof ArmorStand || entity instanceof FallingBlock) {

                continue;
            }

            entitiesAreHurt = true;
            DamageHandler.damageEntity(entity, hitDamage, this);

            if (hitBlindness && entity instanceof Player) {
                final Player l = (Player) entity;
                l.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, (int)(hitBlindnessDuration / 1000 * 20), 2));
            }
        }
        if (entitiesAreHurt) removeWithCooldown();

        boolean canMoveHere = false;
        boolean replace = true;
        if (isAir(location.getBlock().getType())
                || isSand(location.getBlock().getType())
                || isTransparent(location.getBlock())) {
            if (!isWater(location.getBlock())) canMoveHere = true;
        }

        if (isSand(location.getBlock())) replace = false;

        if (!canMoveHere && createSandOnLand) createSandSphere();

        if (!canMoveHere) {
            removeWithCooldown();
            return;
        }


        if (location == sourceBlock.getLocation()) return;

        if (replace && location != block.getLocation()) {
            TempBlock t = new TempBlock(location.getBlock(), shootType);
            t.setRevertTime(300);
        }
        playSandbendingSound(location);

        return;
    }

    public void createSandSphere() {
        for (final Location l : GeneralMethods.getCircle(location, radius, depth, false, true, 0)) {
            final Block b = l.getBlock();
            if (isEarthbendable(b) || isSandbendable(b)) {

                if (isBendableEarthTempBlock(b)) TempBlock.get(b).revertBlock();

                TempBlock sandSphereBlock = new TempBlock(b, shootType);
                sandSphereBlock.setRevertTime(revertTime);
            }
        }
    }

    public void burst() {

        if (System.currentTimeMillis() - burstStart < 100) return;

        if (burstBlocks == null) {
            int x;
            for (x = 0 ; x < amount ; x++) {
                fb = location.getWorld().spawnFallingBlock(location, shootType.createBlockData());
                fb.setMetadata("sandmanipulation", new FixedMetadataValue(ProjectKorra.plugin, burstSandOnLand));
                fb.setVelocity(getRandomVelocity(player.getLocation().getDirection()));
                fb.setDropItem(false);

                if (burstBlocks == null) {
                    burstBlocks = new ArrayList<FallingBlock>();
                }
                burstBlocks.add(fb);
            }

        } else {

            int alive = amount;
            for (FallingBlock fallingBlock : burstBlocks) {

                if (fallingBlock.isDead()) {
                    alive -= 1;
                    continue;
                }

                if (notValidFallingBLock(fallingBlock)) {
                    fallingBlock.remove();
                    displaySandParticle(fallingBlock.getLocation(), 5, 0.5, 0.5, 0.5, 0.5, shootType == Material.RED_SAND);
                    continue;
                }

                if (particles) sandParticles(fallingBlock.getLocation(), shootType == Material.RED_SAND);

                final List<Entity> entities = GeneralMethods.getEntitiesAroundPoint(fallingBlock.getLocation(), 1);
                for (final Entity entity : entities) {

                    if ((entity instanceof Player && Commands.invincible.contains(entity.getName()))
                            || entity instanceof ArmorStand || entity instanceof FallingBlock) {
                        continue;
                    }

                    displaySandParticle(fallingBlock.getLocation(), 5, 0.5, 0.5, 0.5, 0.5, shootType == Material.RED_SAND);
                    if  (alreadyHitEntities.contains(entity)) {
                        fallingBlock.remove();
                        continue;
                    }

                    alreadyHitEntities.add(entity);
                    DamageHandler.damageEntity(entity, burstDamage, this);
                    playSandbendingSound(fallingBlock.getLocation());

                    if (burstBlindness && entity instanceof Player) {
                        final Player l = (Player) entity;
                        l.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, (int)(burstBlindnessDuration / 1000 * 20), 2));
                    }

                    fallingBlock.remove();

                }


            }

            if (alive < 1 || System.currentTimeMillis() - burstStart > 10000) {
                remove();
            }
        }
    }

    public boolean notValidFallingBLock(FallingBlock fallingBlock) {

        if (isWater(fallingBlock.getLocation().getBlock())) return true;
        if (fallingBlock.getLocation().getY() < 0) return true;
        if (GeneralMethods.isRegionProtectedFromBuild(this, fallingBlock.getLocation())) return true;

        return false;
    }

    public Vector getRandomVelocity(Vector dir) {
        double x = (Math.round(Math.random()) * 2 - 1 ) * Math.random() / 2 + dir.getX();
        double y = (Math.round(Math.random()) * 2 - 1 ) * Math.random() / 2 + dir.getY();
        double z = (Math.round(Math.random()) * 2 - 1 ) * Math.random() / 2 + dir.getZ();
        return new Vector(x, y, z).multiply(velocity);
    }

    public void onClick() {

        if (!started) {
            sourceBlock.revertBlock();
            sourceBlock = new TempBlock(block, Material.AIR);
            sourceBlock.setRevertTime(10000);
            started = true;

            if (block.getLocation() != location) {
                TempBlock t = new TempBlock(location.getBlock(), shootType);
                t.setRevertTime(300);
            }

        } else if (!burst) {

            burst = true;
            burstStart = System.currentTimeMillis();
            alreadyHitEntities = new ArrayList<Entity>();
            bPlayer.addCooldown(this, burstCooldown);
        }
    }

    public void removeWithCooldown() {
        bPlayer.addCooldown("SandManipulation", cooldown);
        remove();
    }

    public void removeWithSource() {
        sourceBlock.revertBlock();
        remove();
    }

    private boolean prepare() {
        block = getEarthSourceBlock(sourceRange);
        if (block == null) return false;
        if (!isSand(block)) return false;
        if (GeneralMethods.isRegionProtectedFromBuild(this, block.getLocation())) return false;

        if (block.getType() == Material.SAND) {

            sourceType = Material.SANDSTONE;
            shootType = Material.SAND;

        } else if (block.getType() == Material.RED_SAND) {

            sourceType = Material.RED_SANDSTONE;
            shootType = Material.RED_SAND;

        } else if (block.getType() == Material.RED_SANDSTONE) {

            sourceType = Material.RED_SAND;
            shootType = Material.RED_SAND;

        } else {

            sourceType = Material.SAND;
            shootType = Material.SAND;

        }

        location = block.getLocation();

        if (isAir(block.getLocation().add(0, 1, 0).getBlock().getType())) location.add(0, 1, 0);

        sourceBlock = new TempBlock(block, sourceType);
        return true;
    }

    public void sandParticles(final Location loc, final boolean red) {
        if (red) {
            GeneralMethods.displayColoredParticle("#a75a22", loc);
        } else {
            GeneralMethods.displayColoredParticle("#e4c189", loc);
        }

    }

    @Override
    public long getCooldown() {
        return cooldown;
    }

    @Override
    public Location getLocation() {
        return location;
    }

    @Override
    public String getName() {
        return "SandManipulation";
    }

    @Override
    public String getDescription() {
        return "A sand move using waterbending styles to manipulate the sand in a fluid-ish state!";
    }

    @Override
    public String getInstructions() {
        return "Hold sneak on a sandbendable block and then left click to manipulate the sand. \n(Burst) Left click while manipulating the sand to make the sand blocks fly in random directions creating a burst!";
    }

    @Override
    public boolean isHarmlessAbility() {
        return false;
    }

    @Override
    public boolean isHiddenAbility() {
        return false;
    }

    @Override
    public boolean isSneakAbility() {
        return true;
    }

    @Override
    public String getAuthor() {
        return "Seabarrel";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public void load() {
        FileConfiguration config = ConfigManager.getConfig();
        config.addDefault("ExtraAbilities.Seabarrel.SandManipulation.Cooldown", 5000);
        config.addDefault("ExtraAbilities.Seabarrel.SandManipulation.Range", 20);
        config.addDefault("ExtraAbilities.Seabarrel.SandManipulation.SourceRange", 10);
        config.addDefault("ExtraAbilities.Seabarrel.SandManipulation.CreateSandOnLand", true);
        config.addDefault("ExtraAbilities.Seabarrel.SandManipulation.CreateSandRadius", 7);
        config.addDefault("ExtraAbilities.Seabarrel.SandManipulation.CreateSandDepth", 2);
        config.addDefault("ExtraAbilities.Seabarrel.SandManipulation.CreateSandRevertTime", 10000);

        config.addDefault("ExtraAbilities.Seabarrel.SandManipulation.Damage", 2.0);
        config.addDefault("ExtraAbilities.Seabarrel.SandManipulation.Blindness", true);
        config.addDefault("ExtraAbilities.Seabarrel.SandManipulation.BlindnessDuration", 2000);

        config.addDefault("ExtraAbilities.Seabarrel.SandManipulation.Burst.Cooldown", 10000);
        config.addDefault("ExtraAbilities.Seabarrel.SandManipulation.Burst.Damage", 1);
        config.addDefault("ExtraAbilities.Seabarrel.SandManipulation.Burst.Blindness", true);
        config.addDefault("ExtraAbilities.Seabarrel.SandManipulation.Burst.BlindnessDuration", 2000);
        config.addDefault("ExtraAbilities.Seabarrel.SandManipulation.Burst.Blocks", 12);
        config.addDefault("ExtraAbilities.Seabarrel.SandManipulation.Burst.CreateSandOnLand", true);
        config.addDefault("ExtraAbilities.Seabarrel.SandManipulation.Burst.CreateSandRadius", 3);
        config.addDefault("ExtraAbilities.Seabarrel.SandManipulation.Burst.CreateSandDepth", 2);
        config.addDefault("ExtraAbilities.Seabarrel.SandManipulation.Burst.CreateSandRevertTime", 8000);
        config.addDefault("ExtraAbilities.Seabarrel.SandManipulation.Burst.Velocity", 1);
        config.addDefault("ExtraAbilities.Seabarrel.SandManipulation.Burst.Particles", true);
        ConfigManager.defaultConfig.save();

        ProjectKorra.plugin.getServer().getPluginManager().registerEvents(new SandManipulationListener(), ProjectKorra.plugin);

        ProjectKorra.plugin.getLogger().info("Successfully enabled " + getName() + " " + getVersion() + " by " + getAuthor());

    }

    @Override
    public void stop() {
        remove();
    }
}
