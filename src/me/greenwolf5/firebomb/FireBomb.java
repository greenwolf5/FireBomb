package me.greenwolf5.firebomb;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ComboAbility;
import com.projectkorra.projectkorra.ability.FireAbility;
import com.projectkorra.projectkorra.ability.util.ComboManager.AbilityInformation;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import com.projectkorra.projectkorra.util.ClickType;
import com.projectkorra.projectkorra.util.DamageHandler;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.util.Vector;

/**
 * FireBomb
 * by Greenwolf5
 * Idea and Tester helper CHKirby
 */
public class FireBomb extends FireAbility implements AddonAbility, ComboAbility{

    @Attribute(Attribute.COOLDOWN)
    private long cooldown;
    @Attribute(Attribute.DAMAGE)
    private double damage;
    @Attribute(Attribute.RANGE)
    private double height;

    private enum State{
        LAUNCHING, FLYING, LANDED
    }

    private List<Entity> affectedEntities;    
    private Permission perm;
    private Location location;
    private double radius;
    private int fireTicks;
    private int knockBack;
    private double horizontalDistance;
    private Random random;
    private int delayCounter;
    private int delayTicks = 20;
    private int launchingCounter = 0;
    private int launchingTick = 1;
    private State state;

    public FireBomb(Player player) {
        super(player);
        if (!bPlayer.canBendIgnoreBinds(this)) return;
        FireBomb existing = getAbility(player, getClass());
		if (existing != null) {
			return;
		}
        cooldown = ConfigManager.getConfig().getLong("ExtraAbilities.Greenwolf5.Fire.FireBomb.Cooldown");
        damage = ConfigManager.getConfig().getInt("ExtraAbilities.Greenwolf5.Fire.FireBomb.Damage");
        height = ConfigManager.getConfig().getInt("ExtraAbilities.Greenwolf5.Fire.FireBomb.Height");
        radius = ConfigManager.getConfig().getDouble("ExtraAbilities.Greenwolf5.Fire.FireBomb.Radius");
        fireTicks = ConfigManager.getConfig().getInt("ExtraAbilities.Greenwolf5.Fire.FireBomb.FireTicks");
        knockBack = ConfigManager.getConfig().getInt("ExtraAbilities.Greenwolf5.Fire.FireBomb.Knockback");
        horizontalDistance = ConfigManager.getConfig().getInt("ExtraAbilities.Greenwolf5.Fire.FireBomb.HorizontalDistance");
        random = new Random();
        delayCounter = 0;
        this.bPlayer.addCooldown(this);
        state = State.LAUNCHING;
        start();
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
        return "FireBomb";
    }

    @Override
    public boolean isHarmlessAbility() {
        return false;
    }

    @Override
    public boolean isSneakAbility() {
        return true;
    }

    @Override
    public void progress() {
        if(!bPlayer.canBendIgnoreBinds(this)){
        }
        switch (state) {
            case LAUNCHING:
                launch();
                break;
            case FLYING:
                flying();
                break;
            case LANDED:
                explode();
                break;
        }
        
        
    }

    private void launch() {
        if(launchingCounter >= launchingTick){
            Vector dir = player.getEyeLocation().getDirection();
		    dir.multiply(horizontalDistance);
            dir.setY(height);
            player.setVelocity(dir);
            location = player.getLocation();
            state = State.FLYING;
        }
        launchingCounter++;
    }

    private void flying() {
        playFirebendingParticles(player.getLocation(), 2, .2, .2, .2);
        player.setFallDistance(0.0F);
        if(random.nextDouble() < .2){
        playFirebendingSound(player.getLocation());
        }
            if(delayCounter < delayTicks)
            delayCounter++;

            if(delayCounter >= delayTicks){
                if(GeneralMethods.isSolid(player.getLocation().getBlock().getRelative(BlockFace.DOWN))){
                    state = State.LANDED;
                }
            }
        }

    private void explode() {
        player.setFallDistance(0.0F);
            if(!(location.equals(player.getLocation())) && GeneralMethods.isSolid(player.getLocation().getBlock().getRelative(BlockFace.DOWN))){
            doExplosion(player.getLocation());
            remove();
            }
        }

    private void doExplosion(Location location) {
        affectedEntities = GeneralMethods.getEntitiesAroundPoint(location, radius);
        playExplosionParticles(location);
        playFirebendingSound(location);
        player.getWorld().playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 15, 0F);
        
        for (Entity entity : affectedEntities) {
                if(entity.getUniqueId() == player.getUniqueId()){
                    continue;
                }
                DamageHandler.damageEntity(entity, damage, this);
                if(fireTicks != 0){
                    entity.setFireTicks(fireTicks);
                }
                if(knockBack != 0){
                    if(entity instanceof LivingEntity){
                    Vector knockBackVector = GeneralMethods.getDirection(player.getEyeLocation(), ((LivingEntity)entity).getEyeLocation()).normalize().multiply(knockBack);
                    entity.setVelocity(knockBackVector);
                    }
                }
            }
        }

    private void playExplosionParticles(Location location) {
        for (double theta = 0; theta < 180; theta += 5) {
            for (double phi = 0; phi < 360; phi += 5) {
                final Location display = location.clone().add(radius / 1.5 * Math.cos(phi) * Math.sin(theta), 0, radius / 1.5 * Math.sin(phi) * Math.sin(theta));
                if (random.nextInt(4) == 0) {
                    playFirebendingParticles(display, 1, 0.1, 0.1, 0.1);
                }
            }
        }
    }

    @Override
    public Object createNewComboInstance(Player arg0) {
        return new FireBomb(arg0);
    }

    @Override
    public ArrayList<AbilityInformation> getCombination() {
        ArrayList<AbilityInformation> combo = new ArrayList<>();
        combo.add(new AbilityInformation("FireBlast",ClickType.SHIFT_DOWN));
        combo.add(new AbilityInformation("FireBlast",ClickType.SHIFT_UP));
        combo.add(new AbilityInformation("FireBlast",ClickType.SHIFT_DOWN));
        combo.add(new AbilityInformation("FireBlast",ClickType.SHIFT_UP));
        combo.add(new AbilityInformation("FireShield",ClickType.SHIFT_DOWN));
        return combo;
    }

    @Override
    public String getAuthor() {
        return "Greenwolf5";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public void load() {
         perm = new Permission("bending.ability.firebomb");
         perm.setDefault(PermissionDefault.OP);
         ProjectKorra.plugin.getServer().getPluginManager().addPermission(perm);
         ConfigManager.getConfig().addDefault("ExtraAbilities.Greenwolf5.Fire.FireBomb.Cooldown", 8000);
         ConfigManager.getConfig().addDefault("ExtraAbilities.Greenwolf5.Fire.FireBomb.Damage", 2);
         ConfigManager.getConfig().addDefault("ExtraAbilities.Greenwolf5.Fire.FireBomb.Height", 1);
         ConfigManager.getConfig().addDefault("ExtraAbilities.Greenwolf5.Fire.FireBomb.HorizontalDistance", 1);
         ConfigManager.getConfig().addDefault("ExtraAbilities.Greenwolf5.Fire.FireBomb.Radius", 5);
         ConfigManager.getConfig().addDefault("ExtraAbilities.Greenwolf5.Fire.FireBomb.FireTicks", 0);
         ConfigManager.getConfig().addDefault("ExtraAbilities.Greenwolf5.Fire.FireBomb.Knockback", 2.5);
         ConfigManager.defaultConfig.save();
         ProjectKorra.plugin.getLogger().info(getName() + " " + getVersion() + " by " + getAuthor() + " has been sucessfuly enabled. Plus Kiam's cool");
        
    }

    @Override
    public void stop() {
        ProjectKorra.plugin.getServer().getPluginManager().removePermission(perm);
    }

    @Override
    public String getDescription() {
        return "Jump up into the air and slam down with a firey explosion!";
    }

    @Override
    public String getInstructions() {
        return "Fireblast (Tap Shift) > FireBlast (Tap Shift) > FireShield (Tap Shift)";
    }
    
    @Override
    public boolean isEnabled() {
        return true;
    }
    @Override
    public void remove() {
        // incase I need to edit remove
        super.remove();
    }
}