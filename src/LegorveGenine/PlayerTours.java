package LegorveGenine;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class PlayerTours extends JavaPlugin implements Listener
{
	FileConfiguration dataCFG;
	File data;
	
	ArrayList<Tour> tours = new ArrayList<Tour>();
	
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
		
		if (!getDataFolder().exists())
			getDataFolder().mkdir();
		
		data = new File(getDataFolder(), "data.yml");
		
		if (!data.exists()) 
		{
			try 
			{
				data.createNewFile();
            }
            catch (IOException e)
			{
            	getLogger().severe(ChatColor.RED + "<PlayerTours> Could not create data.yml!");
            }
        }

		dataCFG = YamlConfiguration.loadConfiguration(data);
		
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
		if (sender == getServer().getConsoleSender())
		{
			if(label.equalsIgnoreCase("tour"))
			{
				if(args.length >= 2 && args[0].equalsIgnoreCase("count"))
				{
					String name = args[1].toLowerCase();
					getLogger().info(name + " has given " + dataCFG.getInt(name) + " tours.");
				}
			}
			return true;
		}
		
		if (!(sender instanceof Player)) return false;
		
		Player p = (Player)sender;
		
		if (cmd.getName().equalsIgnoreCase("tour"))
		{
			if(args.length == 1 && args[0].equalsIgnoreCase("list"))
			{
				p.sendMessage("Ongoing Tours: (" + tours.size() + ")");
				for(Tour tour : tours)
					p.sendMessage(tour.toString());
			}
			else if (args.length >= 2 && args[0].equalsIgnoreCase("count"))
			{
				if (!p.hasPermission("canCheckPlayerTourStats"))
				{
					p.sendMessage(ChatColor.DARK_RED + "You do not have permission to use this command.");
					return true;
				}
				
				String name = args[1].toLowerCase();
				p.sendMessage(args[1] + " has given " + dataCFG.getInt(name) + " tours.");
			}
			else if (args.length >= 1 && args[0].equalsIgnoreCase("start"))
			{
				for(Tour tour : tours)
				{	
					if ((args.length > 1 && tour.startTour(p, args[1])) || (args.length == 1 && tour.startTour(p)))
					{
						getServer().broadcastMessage(ChatColor.GREEN + tour.tourGuide.getName() + " is now giving " + tour.newPlayer.getName() + " a tour.");
						break;
					}
					else
					{
						if (!args[1].equalsIgnoreCase(tour.newPlayer.getName()))
							p.sendMessage(ChatColor.RED + "You cannot give a tour to " + args[1] + ".");
						else if (args[1].equalsIgnoreCase(p.getName()))
							p.sendMessage(ChatColor.RED + "You cannot give a tour to yourself.");
						else if (tour.tourGuide != null)
							p.sendMessage(ChatColor.RED + tour.tourGuide.getName() + " is already giving a tour.");
						else
							p.sendMessage(ChatColor.DARK_RED + "Could not start the tour.");
						break;
					}
				}
			}
			else if (args.length >= 1 && args[0].equalsIgnoreCase("end"))
			{
				for(Tour tour : tours)
				{
					if ((args.length > 1 && tour.endTour(p, args[1])) || (args.length == 1 && tour.endTour(p)))
					{
						getServer().broadcastMessage(ChatColor.GREEN + tour.tourGuide.getName() + " has finished giving " + tour.newPlayer.getName() + " a tour.");
						
						tour.fireworks(tour.newPlayer, 1);
						tour.fireworks(tour.tourGuide, dataCFG.getInt(tour.tourGuide.getName().toLowerCase()));
						
						incrementTourStat((Player)tour.tourGuide);
						tours.remove(tour);
						break;
					}
					else
					{
						if (!args[1].equalsIgnoreCase(tour.newPlayer.getName()))
							p.sendMessage(ChatColor.RED + "You cannot end a tour with " + tour.newPlayer.getName() + ".");
						else if (tour.tourGuide != p)
							p.sendMessage(ChatColor.RED + "You cannot end a tour you are not giving");
						else
							p.sendMessage(ChatColor.DARK_RED + "Could not end the tour.");
						break;
					}
				}
			}
			else
			{
				p.sendMessage(ChatColor.GREEN + "Correct usage for /tour:");
				p.sendMessage(ChatColor.GREEN + "/tour start <player>");
				p.sendMessage(ChatColor.GREEN + "/tour end <player>");
				p.sendMessage(ChatColor.GREEN + "/tour count <player>");
				p.sendMessage(ChatColor.GREEN + "/tour list");
			}
			
			return true;
		}
		
		if (cmd.getName().equalsIgnoreCase("playertours"))
		{
			if (!p.hasPermission("canChangePlayerToursSettings"))
			{
				p.sendMessage(ChatColor.DARK_RED + "You do not have permission to perform this command.");
				return true;
			}
			
			if (args.length > 0 && args[0].equalsIgnoreCase("setservername"))
			{
				String s = "";
				for (int i = 1; i < args.length; i++)
					s += args[i] + " ";
				
				s = s.trim();
				
				getConfig().set("serverName", s);
				
				p.sendMessage(ChatColor.GREEN + "Server name successfull updated to: \"" + s + "\"");
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
					p.sendMessage(message);
				}
				catch(NumberFormatException e)
				{
					p.sendMessage(ChatColor.RED + "Invalid Arguments.");
					p.sendMessage(ChatColor.RED + "Correct Usage: /playertours newplayerspawn <x> <y> <z>");
				}
			}
			else
			{
				p.sendMessage(ChatColor.GREEN + "Correct usage for /playertours:");
				p.sendMessage(ChatColor.GREEN + "/playertours servername <String>");
				p.sendMessage(ChatColor.GREEN + "/playertours newplayerspawn <x> <y> <z> [pitch] [yaw]");
			}
			
			return true;
		}
		return false;
	}
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e)
	{
		boolean debug = false; //set to true to start a tour whenever anybody joins.
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
	
	public void incrementTourStat(Player p)
	{
		String n = p.getName().toLowerCase();

		if (dataCFG.isSet(n))
			dataCFG.set(n, dataCFG.getInt(n) + 1);
		else
			dataCFG.set(n, 1);
		
		saveData();
	}
		
	public void saveData() 
	{
		try 
		{
			dataCFG.save(data);
		}
		catch (IOException e) 
		{
			getServer().getLogger().severe(ChatColor.RED + "<PlayerTours> Could not save data.yml!");
		}
		
		dataCFG = YamlConfiguration.loadConfiguration(data);
	}
}