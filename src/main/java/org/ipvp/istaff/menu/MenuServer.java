package org.ipvp.istaff.menu;

import org.bukkit.entity.Player;
import org.ipvp.istaff.IStaff;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public abstract class MenuServer {

    private static final String DEFAULT_URL = "reports";

    public static final Map<String, MenuPage> urlMenupages = new HashMap<>();

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
            } catch (NullPointerException ex) {
                if (!url.isEmpty()) {
                    IStaff.getPlugin().getLogger().log(Level.INFO, "No such page: " + root);
                }

                MenuPage menuPage = urlMenupages.get(DEFAULT_URL);
                menuPage.displayMenu(player, DEFAULT_URL);
            }

            if (!noSave) {
                IStaff.getPlugin().getIStaffDataBaseManager().setURL(player.getUniqueId(), url);
            }
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
        }
    }
}
