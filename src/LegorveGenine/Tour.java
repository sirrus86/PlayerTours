package LegorveGenine;

import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Tour 
{
	private UUID tourGuide = null, newPlayer = null;
	private Location previousLocation = null;
	
	public Tour(Player p)
	{
		newPlayer = p.getUniqueId();
	}
	
	public boolean startTour(Player p)
	{
		return startTour(p, getPlayer().getName());
	}
	
	public boolean startTour(Player p, String playername)
	{
		if(p == getPlayer()) return false;
		if(!playername.equalsIgnoreCase(getPlayer().getName())) return false;
		if(tourGuide != null) return false;
		
		tourGuide = p.getUniqueId();
		previousLocation = p.getLocation();

		((Player)(getGuide())).teleport((Player)(getPlayer()));
		
		playerGlow(getGuide(), true);
		playerGlow(getPlayer(), true);
		return true;
	}

	public boolean endTour(Player p)
	{
		return endTour(p, getPlayer().getName());
	}
	
	public boolean endTour(Player p, String playername)
	{
		if(!playername.equalsIgnoreCase(getPlayer().getName())) return false;
		if(!getGuide().getName().equalsIgnoreCase(p.getName())) return false;
		
		((Player)(getGuide())).teleport(previousLocation);
		
		playerGlow(getGuide(), false);
		playerGlow(getPlayer(), false);
		return true;
	}
	
	public void playerGlow(OfflinePlayer player, boolean on)
	{
		Player p = (Player)player;
		if (on)
			p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 72000, 0));
		else
			p.removePotionEffect(PotionEffectType.GLOWING);
	}
	
	public void fireworks(OfflinePlayer player, int count)
	{
		Player p = (Player)player;
		
		//Counter for the number of fireworks to be spawned
		for(int i = 0; i < count; i++)
		{
			//Create a random number generator
			Random r = new Random();
			
			//Generate 2 random colors
			Color c1 = Color.fromRGB(r.nextInt(256), r.nextInt(256), r.nextInt(256));
			Color c2 = Color.fromRGB(r.nextInt(256), r.nextInt(256), r.nextInt(256));
			
			//Array of firework types to randomize
			Type[] types = {Type.BALL, Type.BALL_LARGE, Type.BURST, Type.CREEPER, Type.STAR};
			
			//Create the firework at the player's location
			Firework f = (Firework) p.getWorld().spawn(p.getLocation(), Firework.class);
			
			//Edit the firework's meta
			FireworkMeta fm = f.getFireworkMeta();
			fm.addEffect(FireworkEffect.builder()
					.flicker(r.nextBoolean())					//Randomly decide if the firework should flicker
					.trail(r.nextBoolean())						//Randomly decide if the firework should have a trail
					.with(types[r.nextInt(types.length)])		//Randomly decide the firework's type
					.withColor(c1)								//Assign the random primary color
					.withFade(c2)								//Assign the random secondary color
					.build());									//Apply all of the desired effects to the meta
			fm.setPower(r.nextInt(3) + 1);						//Randomly decide the firework's power level
			f.setFireworkMeta(fm);								//Apply the firework meta to the firework
		}
	}
	
	public OfflinePlayer getGuide()
	{
		return tourGuide != null ? Bukkit.getServer().getOfflinePlayer(tourGuide) : null;
	}
	
	public OfflinePlayer getPlayer()
	{
		return Bukkit.getServer().getOfflinePlayer(newPlayer);
	}
	
	public String toString()
	{
		String result = "";
		if (tourGuide == null)
			result += "Nobody";
		else
			result += getGuide().getName();
		return result + " is giving " + getPlayer().getName() + " a tour.";
	}
}
