package org.ipvp.istaff.menu;

import com.google.common.collect.Iterables;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.ipvp.istaff.IStaff;
import org.ipvp.istaff.data.ReportData;
import org.ipvp.istaff.data.TeleportRequest;
import org.ipvp.istaff.listeners.BungeeMessager;
import org.ipvp.istaff.util.Utils;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DisplayReportsMenu implements MenuPage {

    private static Map<String, List<ReportData>> reportDataMap;
    private static boolean doUpdate = true;
    private int pageNumber;
    private boolean forceUpdate;
    private final IStaff plugin;

    public DisplayReportsMenu(IStaff plugin, boolean forceUpdate) {
        this.plugin = plugin;
        this.forceUpdate = forceUpdate;
    }

    private String getTimeString(long time) {
        int hours = (int) TimeUnit.MILLISECONDS.toHours(time);
        time -= TimeUnit.MILLISECONDS.convert(hours, TimeUnit.HOURS);
        int minutes = (int) TimeUnit.MILLISECONDS.toMinutes(time);
        time -= TimeUnit.MILLISECONDS.convert(minutes, TimeUnit.MINUTES);
        int seconds = (int) TimeUnit.MILLISECONDS.toSeconds(time);

        String timeString;
        if (hours > 0) {
            timeString = hours + "hrs ";
            timeString = timeString + minutes + "mins ";
            timeString = timeString + seconds + "sec ";
        } else {
            timeString = minutes + "mins ";
            timeString = timeString + seconds + "sec ";
        }

        return timeString;
    }

    @Override
    public void displayMenu(final Player player, String url) {
        try {
            Map<String, String> args = IStaff.splitQuery(url);
            String value = args.get("page_number");
            try {
                pageNumber = Integer.parseInt(value);
            } catch (NumberFormatException ex) {
                pageNumber = 0;
            }

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (DisplayReportsMenu.doUpdate | DisplayReportsMenu.reportDataMap == null | forceUpdate) {
                        plugin.getIStaffDataBaseManager().closeOldReports();
                        DisplayReportsMenu.reportDataMap = plugin.getIStaffDataBaseManager().getUnresolvedReportsGroupPlayer();
                    }

                    Menu menu = new Menu("Reports", 6);
                    menu.setCloseAction(new Menu.CloseAction() {
                        @Override
                        public boolean run(HumanEntity humanEntity, Inventory inventory) {
                            plugin.getIStaffDataBaseManager().setURL(player.getUniqueId(), "");
                            return true;
                        }
                    });

                    int offset = pageNumber * 45;
                    Iterator<Map.Entry<String, List<ReportData>>> iterator = DisplayReportsMenu.reportDataMap.entrySet().iterator();
                    while (offset > 0) {
                        offset--;
                        iterator.next();
                    }

                    boolean lastReached = false;
                    try {
                        for (int i = 0; i < 45; i++) {
                            if (!iterator.hasNext()) {
                                lastReached = true;
                                break;
                            }

                            final Map.Entry<String, List<ReportData>> entry = iterator.next();
                            final List<ReportData> dataList = entry.getValue();
                            if (dataList != null && !dataList.isEmpty()) {
                                int nextOpenSlot = menu.nextOpenSlot();

                                ItemStack skull = Utils.createHead(Bukkit.getOfflinePlayer(entry.getKey()));
                                skull.setAmount(dataList.size());
                                ItemMeta meta = skull.getItemMeta();
                                meta.setDisplayName(ChatColor.AQUA + entry.getKey());

                                final ReportData firstReportData = dataList.get(0);
                                final ReportData lastReportData = dataList.get(dataList.size() - 1);

                                Date date = firstReportData.reportedAt;
                                long time = new Date().getTime() - date.getTime();
                                String reason = lastReportData.reason;
                                String[] lore = {
                                        ChatColor.GOLD + "Reported by: " + ChatColor.RED + firstReportData.reporterName,
                                        ChatColor.GOLD + "Duration: " + ChatColor.RED + getTimeString(time),
                                        ChatColor.GOLD + "Reported for: " + ChatColor.RED + reason,
                                        ChatColor.GOLD + "Server: " + ChatColor.RED + lastReportData.serverName
                                };

                                meta.setLore(Arrays.asList(lore));
                                skull.setItemMeta(meta);
                                menu.setItem(nextOpenSlot, skull);
                                menu.setAction(nextOpenSlot, new Menu.ItemAction() {
                                    @Override
                                    public void run(Player player, Inventory inventory, ItemStack item, int slot,
                                                    InventoryAction action, InventoryClickEvent event) {

                                        player.closeInventory();
                                        event.setCancelled(true);
                                        plugin.getIStaffDataBaseManager().resolveAllReports(entry.getKey());
                                        for (ReportData data : dataList) {
                                            plugin.getBungeeMessager().sendMessage(player, data.reporterName, ChatColor.DARK_AQUA + player.getName()
                                                    + " is handling your report on " + entry.getKey() + ".");
                                        }

                                        BungeeMessager.sendAdminMessage(ChatColor.AQUA + player.getName() + " is handling " + lastReportData.reporterName + "'s report on " + entry.getKey() + ".");
                                        plugin.getIStaffDataBaseManager().addResolvedReportTally(player);
                                        plugin.getBungeeMessager().sendTeleReq(player, Iterables.getLast(dataList).serverName, new TeleportRequest(null, null, entry.getKey(), reason));
                                    }
                                });
                            }
                        }
                    } catch (IndexOutOfBoundsException ex) {
                        lastReached = true;
                    }

                    if (pageNumber != 0) {
                        menu.setItem(46, menu.createItem(Material.SLIME_BALL, 1, "<<", "", (short) 0));
                        menu.setAction(46, new Menu.ItemAction() {
                            @Override
                            public void run(Player player, Inventory inventory, ItemStack item, int slot, InventoryAction action, InventoryClickEvent event) {
                                event.setCancelled(true);
                                MenuServer.serveMenu(player, "reports?page_number=0", false);
                            }
                        });

                        menu.setItem(45, menu.createItem(Material.SLIME_BALL, 1, "<", "", (short) 0));
                        menu.setAction(45, new Menu.ItemAction() {
                            @Override
                            public void run(Player player, Inventory inventory, ItemStack item, int slot, InventoryAction action, InventoryClickEvent event) {
                                event.setCancelled(true);
                                MenuServer.serveMenu(player, "reports?page_number=" + (pageNumber - 1), false);
                            }
                        });
                    }

                    if (!lastReached) {
                        menu.setItem(53, menu.createItem(Material.SLIME_BALL, 1, ">", "", (short) 0));
                        menu.setAction(53, new Menu.ItemAction() {
                            @Override
                            public void run(Player player, Inventory inventory, ItemStack item, int slot, InventoryAction action, InventoryClickEvent event) {
                                event.setCancelled(true);
                                MenuServer.serveMenu(player, "reports?page_number=" + (pageNumber + 1), false);
                            }
                        });
                    }

                    menu.build();
                    menu.showMenu(player);
                }
            }.runTaskAsynchronously(plugin);
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
        }
    }
}
