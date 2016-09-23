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
		if (cmd.getName().equalsIgnoreCase("tour"))
		{
			if (args.length > 0)
			{
				if (args[0].equalsIgnoreCase("list"))
				{
					sender.sendMessage("Ongoing Tours: (" + tours.size() + ")");
					for(Tour tour : tours)
						sender.sendMessage(tour.toString());
				}
				else if (args[0].equalsIgnoreCase("count"))
				{
					if (sender.hasPermission("canCheckPlayerTourStats"))
					{
						if (args.length > 1)
						{
							if (tourStatsCFG.contains(args[1].toLowerCase()))
							{
								sender.sendMessage(args[1] + " has given " + tourStatsCFG.getInt(args[1].toLowerCase()) + " tours.");
							}
							else
							{
								sender.sendMessage(ChatColor.RED + "Could not find tour count for " + args[1] + ".");
							}
						}
						else
						{
							sender.sendMessage(ChatColor.RED + "You must specify the target player's name.");
						}
					}
					else
					{
						sender.sendMessage(ChatColor.DARK_RED + "You do not have permission to use this command.");
					}
				}
				else if (args[0].equalsIgnoreCase("start"))
				{
					if (sender instanceof Player)
					{
						Player p = (Player) sender;
						Tour tour = null;
						for (Tour target : tours)
						{
							if (args.length > 1)
							{
								if (target.getPlayer().getName().equalsIgnoreCase(args[1]))
								{
									if (target.getPlayer() == p)
									{
										sender.sendMessage(ChatColor.RED + "You cannot give a tour to yourself.");
										return true;
									}
									else if (target.getGuide() != null)
									{
										sender.sendMessage(ChatColor.RED + target.getGuide().getName() + " is already giving a tour.");
										return true;
									}
									else
									{
										tour = target;
										break;
									}
								}
							}
							else if (target.getGuide() == null)
							{
								tour = target;
								break;
							}
						}
						if (tour != null)
						{
							tour.startTour(p);
							getServer().broadcastMessage(ChatColor.GREEN + tour.getGuide().getName() + " is now giving " + tour.getPlayer().getName() + " a tour.");
						}
						else
						{
							sender.sendMessage(ChatColor.RED + "Unable to match you to any available tours.");
						}
					}
					else
					{
						sender.sendMessage(ChatColor.RED + "Only players can give tours.");
					}
				}
				else if (args[0].equalsIgnoreCase("end"))
				{
					if (sender instanceof Player)
					{
						Player p = (Player) sender;
						Tour tour = null;
						for (Tour target : tours)
						{
							if (target.getGuide() == p)
							{
								if (args.length > 1)
								{
									if (target.getPlayer().getName().equalsIgnoreCase(args[1]))
									{
										{
											tour = target;
											break;
										}
									}
								}
								else if (target.getGuide() == null)
								{
									tour = target;
									break;
								}
							}
						}
						if (tour != null)
						{
							tour.endTour(p);
							getServer().broadcastMessage(ChatColor.GREEN + tour.getGuide().getName() + " has finished giving " + tour.getPlayer().getName() + " a tour.");
							
							tour.fireworks(tour.getPlayer(), 1);
							tour.fireworks(tour.getGuide(), tourStatsCFG.getInt(tour.getGuide().getName().toLowerCase()));
							
							incrementTourStat((Player)tour.getGuide());
							logTourInformation(tour);
							
							tours.remove(tour);
						}
						else
						{
							sender.sendMessage(ChatColor.RED + "Unable to end any of your active tours.");
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
		
		else if (cmd.getName().equalsIgnoreCase("playertours"))
		{
			if (sender.hasPermission("canChangePlayerToursSettings"))
			{
				if (args.length > 0)
				{
					if (args[0].equalsIgnoreCase("setservername"))
					{
						if (args.length > 1)
						{
							String s = "";
							for (int i = 1; i < args.length; i++)
								s += args[i] + " ";
							
							s = s.trim();
							
							getConfig().set("serverName", s);
							
							sender.sendMessage(ChatColor.GREEN + "Server name successfull updated to: \"" + s + "\"");
						}
						else
						{
							sender.sendMessage(ChatColor.GREEN + "The current server name is: \"" + getConfig().get("serverName") + "\"");
						}
					}
					else if (args[0].equalsIgnoreCase("setnewplayerspawn"))
					{
						if (args.length > 3)
						{
							try
							{
								double x = 0.0D, y = 0.0D, z = 0.0D, pitch = 0.0D, yaw = 0.0D;
								x = Double.valueOf(args[1]);
								y = Double.valueOf(args[2]);
								z = Double.valueOf(args[3]);

								getConfig().set("newPlayerSpawnX", x);
								getConfig().set("newPlayerSpawnY", y);
								getConfig().set("newPlayerSpawnZ", z);
								
								if(args.length > 5)
								{
									pitch = Double.valueOf(args[4]);
									yaw = Double.valueOf(args[5]);
									
									getConfig().set("newPlayerSpawnPitch", pitch);
									getConfig().set("newPlayerSpawnYaw", yaw);
								}
								
								saveConfig();
								reloadConfig();
								String message = ChatColor.GREEN + "New Player Spawn set to: " + x + ", " + y + ", " + z;
								if(args.length > 5)
									message += " (" + pitch + ", " + yaw + ")";
								message += ".";
								sender.sendMessage(message);
							}
							catch(NumberFormatException e)
							{
								sender.sendMessage(ChatColor.RED + "Invalid Arguments.");
								sender.sendMessage(ChatColor.RED + "Correct Usage: /playertours newplayerspawn <x> <y> <z>");
								return true;
							}
						}
						else
						{
							sender.sendMessage(ChatColor.GREEN + "Current player spawn: " + getConfig().getDouble("newPlayerSpawnX")
									+ ", " + getConfig().getDouble("newPlayerSpawnY") + ", " + getConfig().getDouble("newPlayerSpawnZ")
									+ " (" + getConfig().getDouble("newPlayerSpawnPitch") + ", " + getConfig().getDouble("newPlayerSpawnYaw") + ")");
						}
					}
					else
					{
						sender.sendMessage(ChatColor.GREEN + "Correct usage for /playertours:");
						sender.sendMessage(ChatColor.GREEN + "/playertours servername <String>");
						sender.sendMessage(ChatColor.GREEN + "/playertours newplayerspawn <x> <y> <z> [pitch] [yaw]");
					}
				}
			}
			else
			{
				sender.sendMessage(ChatColor.DARK_RED + "You do not have permission to perform this command.");
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