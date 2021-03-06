package net.Indyuce.mb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;

import net.Indyuce.mb.api.BowModifier;
import net.Indyuce.mb.api.MoarBow;
import net.Indyuce.mb.command.MoarBowsCommand;
import net.Indyuce.mb.command.completion.MoarBowsCompletion;
import net.Indyuce.mb.comp.Version_1_12;
import net.Indyuce.mb.comp.WorldGuardUtils;
import net.Indyuce.mb.listener.ArrowLand;
import net.Indyuce.mb.listener.HandParticles;
import net.Indyuce.mb.listener.HitEntity;
import net.Indyuce.mb.listener.ItemPrevents;
import net.Indyuce.mb.listener.ShootBow;
import net.Indyuce.mb.resource.CustomMessage;
import net.Indyuce.mb.resource.DefaultBow;
import net.Indyuce.mb.resource.bow.MarkedBow;
import net.Indyuce.mb.util.Utils;
import net.Indyuce.mb.util.VersionUtils;

public class Main extends JavaPlugin {
	public static HashMap<String, MoarBow> map = new HashMap<String, MoarBow>();
	public static Main plugin;
	public static FileConfiguration bows;
	public static FileConfiguration messages;

	private static boolean registration = true;

	public void onLoad() {
		plugin = this;

		// load WG flags
		if (getServer().getPluginManager().getPlugin("WorldGuard") != null)
			WorldGuardUtils.setup();

		// load default bows
		for (DefaultBow b : DefaultBow.values())
			new MoarBow(b.interfaceClass, b.name(), b.name, b.lore, 0, b.cd, b.eff, b.craft, b.mods).register(false);
	}

	@SuppressWarnings("deprecation")
	public void onEnable() {

		// other plugins must register bows while the plugin is loading
		registration = false;

		Bukkit.getServer().getPluginManager().registerEvents(new Utils(), this);
		Bukkit.getServer().getPluginManager().registerEvents(new GUI(), this);

		Bukkit.getServer().getPluginManager().registerEvents(new ShootBow(), this);
		Bukkit.getServer().getPluginManager().registerEvents(new ItemPrevents(), this);
		Bukkit.getServer().getPluginManager().registerEvents(new HitEntity(), this);
		Bukkit.getServer().getPluginManager().registerEvents(new ArrowLand(), this);
		HandParticles.initialize();

		Bukkit.getServer().getPluginManager().registerEvents(new MarkedBow(), this);

		// version
		try {
			VersionUtils.version = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];
			Bukkit.getConsoleSender().sendMessage("[MoarBows] " + ChatColor.DARK_GRAY + "Detected Bukkit Version: " + VersionUtils.version);
		} catch (Exception e) {
			Bukkit.getConsoleSender().sendMessage("[MoarBows] " + ChatColor.RED + "Your server version is not compatible.");
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}

		// worldguard flags
		if (getServer().getPluginManager().isPluginEnabled("WorldGuard"))
			Bukkit.getConsoleSender().sendMessage("[MoarBows] " + ChatColor.DARK_GRAY + "WorldGuard detected: enabled flags.");

		// config files
		saveDefaultConfig();

		// initialize bow configs
		FileConfiguration bows = ConfigData.getCD(this, "", "bows");
		for (MoarBow b : map.values()) {
			if (!bows.contains(b.getID()))
				bows.createSection(b.getID());

			List<String> lore = new ArrayList<String>(Arrays.asList(b.getLore()));
			if (lore != null && !lore.isEmpty()) {
				lore.add(0, "&8&m------------------------------");
				lore.add("&8&m------------------------------");
			}

			String[] paths = new String[] { "name", "lore", "cooldown", "durability", "craft-enabled", "craft", "eff" };
			Object[] values = new Object[] { b.getName(), lore, b.getCooldown(), b.getDurability(), b.getFormattedCraftingRecipe().length > 0, Arrays.asList(b.getFormattedCraftingRecipe()), b.getParticleEffect() };
			ConfigurationSection section = bows.getConfigurationSection(b.getID());
			for (int j = 0; j < paths.length; j++)
				if (!section.contains(paths[j]))
					bows.set(b.getID() + "." + paths[j], values[j]);

			for (BowModifier mod : b.getModifiers())
				if (!section.contains(mod.getPath()))
					bows.set(b.getID() + "." + mod.getPath(), mod.getDefaultValue());

			b.update(bows);
		}
		ConfigData.saveCD(this, bows, "", "bows");
		Main.bows = bows;

		// in-game messages
		FileConfiguration messages = ConfigData.getCD(this, "", "messages");
		for (CustomMessage pa : CustomMessage.values()) {
			String path = pa.name().toLowerCase().replace("_", "-");
			if (!messages.contains(path))
				messages.set(path, pa.value);
		}
		ConfigData.saveCD(this, messages, "", "messages");
		Main.messages = messages;

		// commands
		getCommand("moarbows").setExecutor(new MoarBowsCommand());
		getCommand("moarbows").setTabCompleter(new MoarBowsCompletion());

		// crafting recipes
		for (MoarBow b : map.values())
			if (bows.getBoolean(b.getID() + ".craft-enabled") && !getConfig().getBoolean("disable-all-bow-craftings")) {
				ShapedRecipe recipe = VersionUtils.isBelow(1, 11) ? new ShapedRecipe(b.a()) : new ShapedRecipe(Version_1_12.key(b.getID()), b.a());
				recipe.shape(new String[] { "ABC", "DEF", "GHI" });
				char[] chars = new char[] { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I' };
				boolean check = true;
				List<String> list = bows.getStringList(b.getID() + ".craft");
				for (int j = 0; j < 9; j++) {
					char c = chars[j];
					if (list.size() != 3) {
						Bukkit.getConsoleSender().sendMessage("[MoarBows] " + ChatColor.RED + "Couldn't create the crafting recipe of " + b.getID() + ". Format error.");
						check = false;
						break;
					}
					List<String> line = Arrays.asList(list.get(j / 3).split(Pattern.quote(",")));
					if (line.size() < 3) {
						Bukkit.getConsoleSender().sendMessage("[MoarBows] " + ChatColor.RED + "Couldn't create the crafting recipe of " + b.getID() + ". Format error.");
						check = false;
						break;
					}
					String s = line.get(j % 3);
					Material material = null;
					try {
						material = Material.valueOf(s.split(Pattern.quote(":"))[0].replace("-", "_").toUpperCase());
					} catch (Exception e1) {
						Bukkit.getConsoleSender().sendMessage("[MoarBows] " + ChatColor.RED + "Couldn't create the crafting recipe of " + b.getID() + ". " + s.split(Pattern.quote(":"))[0] + " is not a valid material.");
						check = false;
						break;
					}
					if (s.contains(":")) {
						int durability = 0;
						try {
							durability = Integer.parseInt(s.split(Pattern.quote(":"))[1]);
						} catch (Exception e1) {
							Bukkit.getConsoleSender().sendMessage("[MoarBows] " + ChatColor.RED + "Couldn't create the crafting recipe of " + b.getID() + ". " + s.split(Pattern.quote(":"))[1] + " is not a valid number.");
							check = false;
							break;
						}
						recipe.setIngredient(c, material, durability);
						continue;
					}
					recipe.setIngredient(c, material);
				}
				if (check)
					getServer().addRecipe(recipe);
			}
	}

	public static boolean canRegisterBows() {
		return registration;
	}
}