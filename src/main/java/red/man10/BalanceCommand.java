package red.man10;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.UUID;

public class BalanceCommand  implements CommandExecutor {

    private final MarketPlugin plugin;


    MarketData data;

    //      コンストラクタ
    public BalanceCommand(MarketPlugin plugin) {
        this.plugin = plugin;
        this.data = new MarketData(plugin);
    }


    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {

        if(!(sender instanceof Player)) {
            sender.sendMessage("プレイヤーのみ実行できます");
            return false;
        }
        if(args.length > 2){
            sender.sendMessage("/balance/bal/mbal/mblance [playername]");
            return false;
        }

        if(args.length == 1){
            if(sender.hasPermission(Settings.showBalanceOther)){

                Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
                    try {
                        showBalance((Player)sender,args[0]);
                    } catch (Exception e) {
                        Bukkit.getLogger().info(e.getMessage());
                        System.out.println(e.getMessage());
                    }
                });



                return true;
            }else{
                sender.sendMessage("あなたには他人の資産をみる権限がない");
                return false;
            }
        }


        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            try {
                showBalance((Player)sender,sender.getName());
            } catch (Exception e) {
                Bukkit.getLogger().info(e.getMessage());
                System.out.println(e.getMessage());
            }
        });


        return true;

    }

    UserData.UserAssetsHistory today = null;
    UserData.UserAssetsHistory last = null;


    void showAssetUUID(CommandSender p, String uuid)
    {
        String sql = "select * from user_assets_history where uuid = '" + uuid + "' order by id desc limit 2;";


        ArrayList<UserData.UserAssetsHistory> his = data.userData.getAssetHistory(sql);
        if (his.size() == 0) {
            return;
        }


        today = his.get(0);

        if (his.size() == 2) {
            last = his.get(1);
        }

        Utility.sendHoverText((Player)p, "§a§l--------[アイテムバンク] => §a§n/mib", "クリックするとアイテムバンクを開きます", "/mib");
        String ret = "§f評価額:" + Utility.getColoredPriceString(today.estimated_value) + " §f: " + Utility.getColoredItemString(today.total_amount);

        p.sendMessage(ret);


    }

    void showBalanceUUID(Player p, String uuid)
    {

        double bal;

        Player target = Bukkit.getPlayer(UUID.fromString(uuid));

        if(target != null){
            if(target.isOnline()){
                data.userData.updateUserAssetsHistory(target);
            }
        }

        UserData.UserAssetsHistory asset = data.userData.getUserAsset(uuid);
        if(asset == null){
            return;
        }
        bal = asset.bal;


        p.sendMessage("§e§l===============[§f§l" + asset.player + "の資産§e§l]==============");
        p.sendMessage("所持金:" + Utility.getColoredPriceString(bal));
        p.sendMessage("口座残高:"+ plugin.bankAPI.getBalance(UUID.fromString(uuid)));


        showAssetUUID(p, uuid);
        showOrderUUID(p, uuid);

//        data.userData.showEarnings(p,uuid);

    }

    void showOrderUUID(Player p, String uuid)
    {
        MarketData data = new MarketData(plugin);
        ArrayList<MarketData.OrderInfo> orders = data.getOrderOfUser(p, uuid);
        if (orders == null) {
            return;
        }

        if (orders.size() == 0) {
            return;
        }

        long buyAmount = 0L;
        long sellAmount = 0L;
        double buyTotal = 0.0D;
        long sellTotal = 0L;
        String orderPlyer = "";
        for (MarketData.OrderInfo order : orders) {
            MarketData.ItemIndex itemIndex = data.getItemPrice(order.item_id);
            if (order.isBuy) {
                buyAmount += order.amount;
                buyTotal += order.price * order.amount;
            } else {
                sellAmount += order.amount;
                sellTotal += itemIndex.price * order.amount;
                orderPlyer = order.player;
            }
        }

        String command = "/mce order";
        if(!p.getUniqueId().toString().equalsIgnoreCase(uuid)){
            command += " "+orderPlyer;
        }

        Utility.sendHoverText(p, "§9§l--------[注文件数:" + orders.size() + "] => §9§n/mce order", "クリックすると注文を開きます", command);
        p.sendMessage("§f買い注文:" + Utility.getColoredPriceString(buyTotal) + " §f: " + Utility.getColoredItemString(buyAmount));

        p.sendMessage("§f売り注文評価額:" + Utility.getColoredPriceString(sellTotal) + " §f: " + Utility.getColoredItemString(sellAmount));


    }

    void showBalance(Player sender, String playerName)
    {

        Bukkit.getLogger().info("showBalance:"+playerName);



        String uuid = null;

        if (playerName == null) {
            uuid = sender.getUniqueId().toString();
        } else {
            Player p = Bukkit.getPlayer(playerName);
            if(p != null){
                uuid = p.getUniqueId().toString();
            }
        }

        if ((uuid == null) && (playerName != null)) {
            uuid = data.userData.getUUID(playerName);
            sender.sendMessage("プレイヤーがDBからみつかりました UUID: "+uuid);
        }

        if (uuid == null) {
            sender.sendMessage("プレイヤーはこのサーバに存在していません");
            return;
        }

        showBalanceUUID(sender, uuid);

    }

}