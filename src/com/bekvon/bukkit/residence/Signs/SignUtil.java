package com.bekvon.bukkit.residence.Signs;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import com.bekvon.bukkit.residence.CommentedYamlConfiguration;
import com.bekvon.bukkit.residence.NewLanguage;
import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.economy.rent.RentedLand;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;

public class SignUtil {

    public static SignInfo Signs = new SignInfo();

    // Sign file
    public static void LoadSigns() {
	Thread threadd = new Thread() {
	    public void run() {

		Signs.GetAllSigns().clear();
		File file = new File(Residence.instance.getDataFolder(), "Signs.yml");
		YamlConfiguration f = YamlConfiguration.loadConfiguration(file);

		if (!f.isConfigurationSection("Signs"))
		    return;

		ConfigurationSection ConfCategory = f.getConfigurationSection("Signs");
		ArrayList<String> categoriesList = new ArrayList<String>(ConfCategory.getKeys(false));
		if (categoriesList.size() == 0)
		    return;
		for (String category : categoriesList) {
		    ConfigurationSection NameSection = ConfCategory.getConfigurationSection(category);
		    Signs newTemp = new Signs();
		    newTemp.setCategory(Integer.valueOf(category));
		    newTemp.setResidence(NameSection.getString("Residence"));
		    newTemp.setWorld(NameSection.getString("World"));
		    newTemp.setX(NameSection.getDouble("X"));
		    newTemp.setY(NameSection.getDouble("Y"));
		    newTemp.setZ(NameSection.getDouble("Z"));

		    newTemp.setLocation(new Location(Bukkit.getWorld(NameSection.getString("World")), NameSection.getDouble("X"), NameSection.getDouble("Y"), NameSection
			.getDouble("Z")));

		    Signs.addSign(newTemp);
		}
		return;
	    }
	};
	threadd.start();
    }

    // Signs save file
    public static void saveSigns() {

	Thread threadd = new Thread() {
	    public void run() {
		File f = new File(Residence.instance.getDataFolder(), "Signs.yml");
		YamlConfiguration conf = YamlConfiguration.loadConfiguration(f);

		CommentedYamlConfiguration writer = new CommentedYamlConfiguration();
		conf.options().copyDefaults(true);

		writer.addComment("Signs", "DO NOT EDIT THIS FILE BY HAND!");

		if (!conf.isConfigurationSection("Signs"))
		    conf.createSection("Signs");

		for (Signs one : Signs.GetAllSigns()) {
		    String path = "Signs." + String.valueOf(one.GetCategory());
		    writer.set(path + ".Residence", one.GetResidence());
		    writer.set(path + ".World", one.GetWorld());
		    writer.set(path + ".X", one.GetX());
		    writer.set(path + ".Y", one.GetY());
		    writer.set(path + ".Z", one.GetZ());
		}

		try {
		    writer.save(f);
		} catch (IOException e) {
		    e.printStackTrace();
		}
		return;
	    }
	};
	threadd.start();
    }

    public static void CheckSign(ClaimedResidence res) {

	List<Signs> signList = new ArrayList<Signs>();

	signList.addAll(SignUtil.Signs.GetAllSigns());

	for (Signs one : signList) {
	    if (!res.getName().equals(one.GetResidence()))
		continue;
	    SignUtil.SignUpdate(one);
	}

    }

    public static boolean SignUpdate(Signs Sign) {

	String landName = Sign.GetResidence();

	boolean ForSale = Residence.getTransactionManager().isForSale(landName);
	boolean ForRent = Residence.getRentManager().isForRent(landName);

	Location nloc = Sign.GetLocation();
	Block block = nloc.getBlock();

	if (!(block.getState() instanceof Sign))
	    return false;

	Sign sign = (Sign) block.getState();

	if (!ForRent && !ForSale) {
	    block.breakNaturally();
	    Signs.removeSign(Sign);
	    saveSigns();
	    return true;
	}

	if (ForRent) {

	    boolean rented = Residence.getRentManager().isRented(landName);

	    RentedLand rentedPlace = Residence.getRentManager().getRentedLand(landName);
	    long time = 0L;
	    if (rentedPlace != null)
		time = rentedPlace.endTime;

	    SimpleDateFormat formatter = new SimpleDateFormat(Residence.getLanguage().getPhrase("SignDateFormat"));
	    Calendar calendar = Calendar.getInstance();
	    calendar.setTimeInMillis(time);
	    String timeString = formatter.format(calendar.getTime());

	    String endDate = timeString;
	    if (time == 0L)
		endDate = "Unknown";

	    if (Residence.getRentManager().getRentedAutoRepeats(landName))
		endDate = ChatColor.translateAlternateColorCodes('&', Residence.getLanguage().getPhrase("SignRentedAutorenewTrue") + endDate);
	    else
		endDate = ChatColor.translateAlternateColorCodes('&', Residence.getLanguage().getPhrase("SignRentedAutorenewFalse") + endDate);

	    String TopLine = rented ? endDate : Residence.getLanguage().getPhrase("SignForRentTopLine");
	    sign.setLine(0, TopLine);

	    String infoLine = Residence.getLanguage().getPhrase("SignForRentPriceLine", Residence.getRentManager().getCostOfRent(landName) + "." + Residence
		.getRentManager().getRentDays(landName) + "." + Residence.getRentManager().getRentableRepeatable(landName));

	    sign.setLine(1, infoLine);

	    sign.setLine(2, rented ? NewLanguage.getDefaultMessage("Language.SignRentedResName").replace("%1", landName)
		: NewLanguage.getDefaultMessage("Language.SignRentedResName").replace("%1", landName));
	    sign.setLine(3, rented ? Residence.getLanguage().getPhrase("SignRentedBottomLine", Residence.getRentManager().getRentingPlayer(landName))
		: Residence.getLanguage().getPhrase("SignForRentBottomLine"));
	    sign.update();
	}

	if (ForSale) {
	    sign.setLine(0, Residence.getLanguage().getPhrase("SignForSaleTopLine"));
	    String infoLine = Residence.getLanguage().getPhrase("SignForSalePriceLine", String.valueOf(Residence.getTransactionManager().getSaleAmount(landName)));
	    sign.setLine(1, infoLine);
	    sign.setLine(2, NewLanguage.getDefaultMessage("Language.SignRentedResName").replace("%1", landName));
	    sign.setLine(3, Residence.getLanguage().getPhrase("SignForSaleBottomLine"));
	    sign.update();
	}

	return true;
    }

    public static void convertSigns(CommandSender sender) {
	File file = new File("plugins/ResidenceSigns/signs.yml");
	if (!file.exists()) {
	    sender.sendMessage(ChatColor.GOLD + "Can't find ResidenceSign file");
	    return;
	}
	YamlConfiguration conf = YamlConfiguration.loadConfiguration(file);

	if (!conf.contains("signs")) {
	    sender.sendMessage(ChatColor.GOLD + "Incorrect format of signs file");
	    return;
	}

	Set<String> sectionname = conf.getConfigurationSection("signs").getKeys(false);
	ConfigurationSection section = conf.getConfigurationSection("signs");

	int category = 1;
	if (SignUtil.Signs.GetAllSigns().size() > 0)
	    category = SignUtil.Signs.GetAllSigns().get(SignUtil.Signs.GetAllSigns().size() - 1).GetCategory() + 1;

	long time = System.currentTimeMillis();

	int i = 0;
	for (String one : sectionname) {
	    Signs signs = new Signs();
	    String resname = section.getString(one + ".resName");
	    signs.setCategory(category);
	    signs.setResidence(resname);

	    List<String> loc = section.getStringList(one + ".loc");

	    if (loc.size() != 4)
		continue;

	    World world = Bukkit.getWorld(loc.get(0));
	    if (world == null)
		continue;

	    signs.setWorld(world.getName());
	    int x = 0;
	    int y = 0;
	    int z = 0;

	    try {
		x = Integer.parseInt(loc.get(1));
		y = Integer.parseInt(loc.get(2));
		z = Integer.parseInt(loc.get(3));
	    } catch (Exception ex) {
		continue;
	    }

	    signs.setX(x);
	    signs.setY(y);
	    signs.setZ(z);
	    boolean found = false;

	    for (Signs onesigns : SignUtil.Signs.GetAllSigns()) {
		if (!onesigns.GetWorld().equalsIgnoreCase(signs.GetWorld()))
		    continue;
		if (onesigns.GetX() != signs.GetX())
		    continue;
		if (onesigns.GetY() != signs.GetY())
		    continue;
		if (onesigns.GetZ() != signs.GetZ())
		    continue;
		found = true;
	    }

	    if (found)
		continue;

	    Location nloc = signs.GetLocation();
	    Block block = nloc.getBlock();

	    if (!(block.getState() instanceof Sign))
		continue;

	    SignUtil.Signs.addSign(signs);
	    SignUtil.SignUpdate(signs);
	    category++;
	    i++;
	}

	SignUtil.saveSigns();

	sender.sendMessage(ChatColor.GOLD + "" + i + ChatColor.YELLOW + " signs have being converted to new format! It took " + ChatColor.GOLD + (System
	    .currentTimeMillis() - time) + ChatColor.YELLOW + " ms!");
    }

}
