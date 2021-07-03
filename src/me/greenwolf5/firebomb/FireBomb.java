package me.greenwolf5.firebomb;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.BlueFireAbility;
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
    private double verticalSpeed;
    private double horizontalSpeed;
    private double radius;
    private int fireTicks;
    private int knockBack;
    private boolean preventFallDamage;


    private enum State{
        LAUNCHING, FLYING, LANDED
    }
    private State state;

    private Permission perm;


    private List<Entity> affectedEntities;    
    private Location location;
    private Random random;
    private int delayCounter;
    private int delayTicks = 5;
    private int launchingCounter = 0;
    private int launchingTick = 1;

    public FireBomb(Player player) {
        super(player);
        if (!bPlayer.canBendIgnoreBinds(this)) return;
        FireBomb existing = getAbility(player, getClass());
		if (existing != null) {
			return;
		}
        setFields();
        random = new Random();
        delayCounter = 0;
        this.bPlayer.addCooldown(this);
        state = State.LAUNCHING;
        start();
    }

    public void setFields(){
        cooldown = ConfigManager.getConfig().getLong("ExtraAbilities.Greenwolf5.Fire.FireBomb.Cooldown");
        damage = ConfigManager.getConfig().getInt("ExtraAbilities.Greenwolf5.Fire.FireBomb.Damage");
        verticalSpeed = ConfigManager.getConfig().getInt("ExtraAbilities.Greenwolf5.Fire.FireBomb.VerticalSpeed");
        horizontalSpeed = ConfigManager.getConfig().getInt("ExtraAbilities.Greenwolf5.Fire.FireBomb.HorizontalSpeed");
        radius = ConfigManager.getConfig().getDouble("ExtraAbilities.Greenwolf5.Fire.FireBomb.Radius");
        fireTicks = ConfigManager.getConfig().getInt("ExtraAbilities.Greenwolf5.Fire.FireBomb.FireTicks");
        knockBack = ConfigManager.getConfig().getInt("ExtraAbilities.Greenwolf5.Fire.FireBomb.Knockback");
        preventFallDamage = ConfigManager.getConfig().getBoolean("ExtraAbilities.Greenwolf5.Fire.FireBomb.PreventFallDamage");
        applyModifiers(damage,cooldown,radius);
    }

    private void applyModifiers(double damage, double cooldown, double radius) {
        int damageMod = 0;
        damageMod = (int)(getDayFactor(damage) - damage);
        damageMod = (int)(this.bPlayer.canUseSubElement(Element.SubElement.BLUE_FIRE) ? (BlueFireAbility.getDamageFactor() * damage - damage + damageMod) : damageMod);
        this.damage += damageMod;
        int cooldownMod = 0;
        int rangeMod = 0;
        if(this.bPlayer.canUseSubElement(Element.SubElement.BLUE_FIRE)){
            cooldownMod = (int)(BlueFireAbility.getCooldownFactor()* cooldown - cooldown);
            this.cooldown += cooldownMod;
            rangeMod = (int)(BlueFireAbility.getRangeFactor() * radius - radius);
            this.radius += rangeMod;
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
            if(!(horizontalSpeed == 0.0) || !(verticalSpeed == 0.0)){ //if both are 0, it does a bug, not *exactly* sure why, so I just don't
            Vector dir = player.getEyeLocation().getDirection();
		    dir.multiply(horizontalSpeed);  
            dir.setY(verticalSpeed);
            player.setVelocity(dir.normalize());
                //after normalzing the dir, it moves a lot better than it without, so I'm doing that
            }
            state = State.FLYING;
        }
        launchingCounter++;
    }

    private void flying() {
        playFirebendingParticles(player.getLocation(), 2, .2, .2, .2);
        if(preventFallDamage){
            player.setFallDistance(0.0F);
        }
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
        if(preventFallDamage){
            player.setFallDistance(0.0F);
        }
        if( GeneralMethods.isSolid(player.getLocation().getBlock().getRelative(BlockFace.DOWN))){
            doExplosion(player.getLocation());
            remove();
        }
    }

    private void doExplosion(Location location) {
        affectedEntities = GeneralMethods.getEntitiesAroundPoint(location, radius);
        playExplosionParticles(location);
        playFirebendingSound(location);
        player.getWorld().playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 15, 0F); //play sounds here so it's quietier lol
        
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
        return "1.0.1";
    }

    @Override
    public void load() {
         perm = new Permission("bending.ability.firebomb");
         perm.setDefault(PermissionDefault.OP);
         ProjectKorra.plugin.getServer().getPluginManager().addPermission(perm);
         ConfigManager.getConfig().addDefault("ExtraAbilities.Greenwolf5.Fire.FireBomb.Cooldown", 8000);
         ConfigManager.getConfig().addDefault("ExtraAbilities.Greenwolf5.Fire.FireBomb.Damage", 2);
         ConfigManager.getConfig().addDefault("ExtraAbilities.Greenwolf5.Fire.FireBomb.VerticalSpeed", 1.3);
         ConfigManager.getConfig().addDefault("ExtraAbilities.Greenwolf5.Fire.FireBomb.HorizontalSpeed", 1);
         ConfigManager.getConfig().addDefault("ExtraAbilities.Greenwolf5.Fire.FireBomb.Radius", 5);
         ConfigManager.getConfig().addDefault("ExtraAbilities.Greenwolf5.Fire.FireBomb.FireTicks", 0);
         ConfigManager.getConfig().addDefault("ExtraAbilities.Greenwolf5.Fire.FireBomb.Knockback", 2.5);
         ConfigManager.getConfig().addDefault("ExtraAbilities.Greenwolf5.Fire.FireBomb.PreventFallDamage", true);
         ConfigManager.defaultConfig.save();
         ProjectKorra.plugin.getLogger().info(getName() + " " + getVersion() + " by " + getAuthor() + " has been sucessfuly enabled. Plus Kiam's cool");
        
    }

    @Override
    public void stop() {
        ProjectKorra.plugin.getServer().getPluginManager().removePermission(perm);
    }

    @Override
    public String getDescription() {
        return "Shoot yourself upwards, with a velocity based on where you're looking, as you're falling you take no fall damage, allowing the fire bender to use it to jump off from high heights! It can be used when falling or fire-jetting for some extra damage or to give you a safe landing with the knockback.";
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