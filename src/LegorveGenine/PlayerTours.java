package LegorveGenine;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class PlayerTours extends JavaPlugin implements Listener
{
	YamlConfiguration tourStatsCFG;
	File tourStats;
	
	YamlConfiguration tourLogCFG;
	File tourLog;
	
	Map<File, YamlConfiguration> configs = new HashMap<File, YamlConfiguration>();
	Set<Tour> tours = new HashSet<Tour>();
	
	@Override
	public void onEnable()
	{
		getConfig().addDefault("newPlayerSpawnX", 0.0);
		getConfig().addDefault("newPlayerSpawnY", 65.0);
		getConfig().addDefault("newPlayerSpawnZ", 0.0);
		getConfig().addDefault("newPlayerSpawnPitch", 180.0);
		getConfig().addDefault("newPlayerSpawnYaw", 0.0);
		getConfig().addDefault("serverName", "the server");
		getConfig().options().copyDefaults(true);
		saveConfig();

		tourLog = new File(getDataFolder(), "tourLog.yml");
		tourStats = new File(getDataFolder(), "tourStats.yml");
		configs.put(tourLog, tourLogCFG);
		configs.put(tourStats, tourStatsCFG);
		
		if (!getDataFolder().exists())
			getDataFolder().mkdir();
		
		for (File file : configs.keySet())
		{
			try
			{
				if (!file.exists())
					file.createNewFile();
				configs.get(file).load(file);
			}
			catch (Exception e)
			{
				getLogger().severe("<PlayerTours> Could not load " + file.getName() + "!");
				e.printStackTrace();
			}
		}
		
		tourLogCFG.addDefault("tourNumber", 0);
		tourLogCFG.options().copyDefaults(true);
		
		getServer().getPluginManager().registerEvents(this, this);
	}
	
	@Override
	public void onDisable()
	{	
		saveConfig();
		saveData();
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
	{	
//		if (sender == getServer().getConsoleSender())
//		{
//			if(label.equalsIgnoreCase("tour"))
//			{
//				if(args.length >= 2 && args[0].equalsIgnoreCase("count"))
//				{
//					String name = args[1].toLowerCase();
//					getLogger().info(name + " has given " + tourStatsCFG.getInt(name) + " tours.");
//				}
//			}
//			return true;
//		}
//		
//		if (!(sender instanceof Player)) return false;
//		
//		Player p = (Player)sender;
//		
		if (cmd.getName().equalsIgnoreCase("tour"))
		{
			if(args.length == 1 && args[0].equalsIgnoreCase("list"))
			{
				sender.sendMessage("Ongoing Tours: (" + tours.size() + ")");
				for(Tour tour : tours)
					sender.sendMessage(tour.toString());
			}
			else if (args.length >= 2 && args[0].equalsIgnoreCase("count"))
			{
				if (!sender.hasPermission("canCheckPlayerTourStats"))
				{
					sender.sendMessage(ChatColor.DARK_RED + "You do not have permission to use this command.");
					return true;
				}
				
				String name = args[1].toLowerCase();
				sender.sendMessage(args[1] + " has given " + tourStatsCFG.getInt(name) + " tours.");
			}
			else if (args.length >= 1 && args[0].equalsIgnoreCase("start"))
			{
				if (sender instanceof Player)
				{
					Player p = (Player) sender;
					for(Tour tour : tours)
					{	
						if ((args.length > 1 && tour.startTour(p, args[1])) || (args.length == 1 && tour.startTour(p)))
						{
							getServer().broadcastMessage(ChatColor.GREEN + tour.getGuide().getName() + " is now giving " + tour.getPlayer().getName() + " a tour.");
							break;
						}
						else
						{
							if (!args[1].equalsIgnoreCase(tour.getPlayer().getName()))
								p.sendMessage(ChatColor.RED + "You cannot give a tour to " + args[1] + ".");
							else if (args[1].equalsIgnoreCase(p.getName()))
								p.sendMessage(ChatColor.RED + "You cannot give a tour to yourself.");
							else if (tour.getGuide() != null)
								p.sendMessage(ChatColor.RED + tour.getGuide().getName() + " is already giving a tour.");
							else
								p.sendMessage(ChatColor.DARK_RED + "Could not start the tour.");
						}
					}
				}
				else
				{
					sender.sendMessage(ChatColor.RED + "Only players can give tours.");
				}
			}
			else if (args.length >= 1 && args[0].equalsIgnoreCase("end"))
			{
				if (sender instanceof Player)
				{
					Player p = (Player) sender;
					for(Tour tour : tours)
					{
						if ((args.length > 1 && tour.endTour(p, args[1])) || (args.length == 1 && tour.endTour(p)))
						{
							getServer().broadcastMessage(ChatColor.GREEN + tour.getGuide().getName() + " has finished giving " + tour.getPlayer().getName() + " a tour.");
							
							tour.fireworks(tour.getPlayer(), 1);
							tour.fireworks(tour.getGuide(), tourStatsCFG.getInt(tour.getGuide().getName().toLowerCase()));
							
							incrementTourStat((Player)tour.getGuide());
							logTourInformation(tour);
							
							tours.remove(tour);
							break;
						}
						else
						{
							if (!args[1].equalsIgnoreCase(tour.getPlayer().getName()))
								p.sendMessage(ChatColor.RED + "You cannot end a tour with " + tour.getPlayer().getName() + ".");
							else if (tour.getGuide() != p)
								p.sendMessage(ChatColor.RED + "You cannot end a tour you are not giving");
							else
								p.sendMessage(ChatColor.DARK_RED + "Could not end the tour.");
							break;
						}
					}
				}
				else
				{
					sender.sendMessage(ChatColor.RED + "Only players can end tours.");
				}
			}
			else
			{
				sender.sendMessage(ChatColor.GREEN + "Correct usage for /tour:");
				sender.sendMessage(ChatColor.GREEN + "/tour start <player>");
				sender.sendMessage(ChatColor.GREEN + "/tour end <player>");
				sender.sendMessage(ChatColor.GREEN + "/tour count <player>");
				sender.sendMessage(ChatColor.GREEN + "/tour list");
			}
			
			return true;
		}
		
		if (cmd.getName().equalsIgnoreCase("playertours"))
		{
			if (!sender.hasPermission("canChangePlayerToursSettings"))
			{
				sender.sendMessage(ChatColor.DARK_RED + "You do not have permission to perform this command.");
				return true;
			}
			
			if (args.length > 0 && args[0].equalsIgnoreCase("setservername"))
			{
				String s = "";
				for (int i = 1; i < args.length; i++)
					s += args[i] + " ";
				
				s = s.trim();
				
				getConfig().set("serverName", s);
				
				sender.sendMessage(ChatColor.GREEN + "Server name successfull updated to: \"" + s + "\"");
			}
			else if (args.length >= 4 && args[0].equalsIgnoreCase("setnewplayerspawn"))
			{
				try
				{
					double x = Double.valueOf(args[1]);
					double y = Double.valueOf(args[2]);
					double z = Double.valueOf(args[3]);

					getConfig().set("newPlayerSpawnX", x);
					getConfig().set("newPlayerSpawnY", y);
					getConfig().set("newPlayerSpawnZ", z);

					double pitch = 0.0;
					double yaw = 0.0;
					
					if(args.length >= 6)
					{
						pitch = Double.valueOf(args[4]);
						yaw = Double.valueOf(args[5]);
						
						getConfig().set("newPlayerSpawnPitch", pitch);
						getConfig().set("newPlayerSpawnYaw", yaw);
					}
					
					saveConfig();
					reloadConfig();
					String message = ChatColor.GREEN + "New Player Spawn set to: " + x + ", " + y + ", " + z;
					if(args.length >= 6)
						message += " (" + pitch + ", " + yaw + ")";
					message += ".";
					sender.sendMessage(message);
				}
				catch(NumberFormatException e)
				{
					sender.sendMessage(ChatColor.RED + "Invalid Arguments.");
					sender.sendMessage(ChatColor.RED + "Correct Usage: /playertours newplayerspawn <x> <y> <z>");
				}
			}
			else
			{
				sender.sendMessage(ChatColor.GREEN + "Correct usage for /playertours:");
				sender.sendMessage(ChatColor.GREEN + "/playertours servername <String>");
				sender.sendMessage(ChatColor.GREEN + "/playertours newplayerspawn <x> <y> <z> [pitch] [yaw]");
			}
			
			return true;
		}
		return false;
	}
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e)
	{
		boolean debug = true; //set to true to start a tour whenever anybody joins.
		if (e.getPlayer().hasPlayedBefore() && !debug) return;
		
		Player p = e.getPlayer();
		
		e.setJoinMessage(ChatColor.YELLOW + "Welcome " + p.getName() + " to " + getConfig().getString("serverName") + " for the first time!");	

		tours.add(new Tour(p));
		
		double x = getConfig().getDouble("newPlayerSpawnX");
		double y = getConfig().getDouble("newPlayerSpawnY");
		double z = getConfig().getDouble("newPlayerSpawnZ");
		float pitch = (float)getConfig().getDouble("newPlayerSpawnPitch");
		float yaw = (float)getConfig().getDouble("newPlayerSpawnYaw");
		
		Location newPlayerSpawn = new Location(p.getWorld(), x, y, z, pitch, yaw);
		p.teleport(newPlayerSpawn);
	}
	
	public void logTourInformation(Tour tour)
	{
		Calendar currentDate = Calendar.getInstance();
		SimpleDateFormat formatter = new SimpleDateFormat("MMM/dd/yyyy HH:mm");
		String dateNow = formatter.format(currentDate.getTime());
		
		String information = tour.getGuide().getName() + " toured " + tour.getPlayer().getName() + " at " + dateNow;
		
		int number = tourLogCFG.getInt("tourNumber");
		tourLogCFG.set("Tour #" + number, information);
		
		saveData();
	}
	
	public void incrementTourStat(Player p)
	{
		String n = p.getName().toLowerCase();

		if (tourStatsCFG.isSet(n))
			tourStatsCFG.set(n, tourStatsCFG.getInt(n) + 1);
		else
			tourStatsCFG.set(n, 1);
		
		saveData();
	}
		
	public void saveData() 
	{
		for (File file : configs.keySet())
		{
			try
			{
				configs.get(file).save(file);
			}
			catch (IOException e)
			{
				getServer().getLogger().severe(ChatColor.RED + "<PlayerTours> Could not save " + file.getName() + "!");
			}
		}
	}
}