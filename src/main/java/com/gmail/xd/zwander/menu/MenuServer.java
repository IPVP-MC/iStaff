package com.gmail.xd.zwander.menu;

import com.connorl.istaff.ISDataBaseManager;
import com.connorl.istaff.IStaff;
import org.bukkit.entity.Player;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.logging.Level;

public abstract class MenuServer {

    private final static String DEFAULT_URL = "reports";
    public static HashMap<String, MenuPage> urlMenupages = new HashMap<>();

    public static void serveMenu(Player player, String url, boolean noSave) {
        if (url == null) {
            url = DEFAULT_URL;
        }

        try {
            String root = URLDecoder.decode(url.split("\\?", 2)[0], "UTF-8");
            try {
                player.getOpenInventory().close();
                MenuPage mp = urlMenupages.get(root);
                mp.displayMenu(player, url);
            } catch (NullPointerException e) {
                if (!url.isEmpty()) {
                    IStaff.getPlugin().getLogger().log(Level.INFO, "No such page: " + root);
                }

                MenuPage menuPage = urlMenupages.get(DEFAULT_URL);
                menuPage.displayMenu(player, DEFAULT_URL);
            }

            if (!noSave) {
                ISDataBaseManager.setURL(player, url);
            }
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
        }
    }
}
