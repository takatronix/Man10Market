package red.man10;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.BatchUpdateException;
import java.util.ArrayList;
import java.util.UUID;

public class BalanceCommand  implements CommandExecutor {

    private final MarketPlugin plugin;

    //      コンストラクタ
    public BalanceCommand(MarketPlugin plugin) {
        this.plugin = plugin;
    }


    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if((sender instanceof Player) == false) {
            sender.sendMessage("プレイヤーのみ実行できます");
        }
        if(args.length > 2){
            sender.sendMessage("/balance/bal/mbal/mblance [playername]");
            return false;
        }

        if(args.length == 1){
            if(sender.hasPermission(Settings.showBalanceOther)){
                return showBalance((Player)sender,args[0]);
            }else{
                sender.sendMessage("あなたには他人の資産をみる権限がない");
                return false;
            }
        }

        return showBalance((Player)sender,null);

    }

    UserData.UserAssetsHistory today = null;
    UserData.UserAssetsHistory last = null;


    boolean showAssetUUID(CommandSender p, String uuid)
    {
        String sql = "select * from user_assets_history where uuid = '" + uuid + "' order by id desc limit 2;";


        ArrayList<UserData.UserAssetsHistory> his = userData.getAssetHistory(sql);
        if (his.size() == 0) {
            return false;
        }


        today = ((UserData.UserAssetsHistory)his.get(0));



        if (his.size() == 2) {
            last = ((UserData.UserAssetsHistory)his.get(1));
        }

        Utility.sendHoverText((Player)p, "§a§l--------[アイテムバンク] => §a§n/mib", "クリックするとアイテムバンクを開きます", "/mib");
        String ret = "§f評価額:" + Utility.getColoredPriceString(today.estimated_value) + " §f: " + Utility.getColoredItemString(today.total_amount);

        p.sendMessage(ret);


        return true;
    }

    boolean showBalanceUUID(Player p, String uuid)
    {



        double bal = 0;
        Player target = Bukkit.getPlayer(UUID.fromString(uuid));

        if(target != null){
            if(target.isOnline()){
                userData.updateUserAssetsHistory(target);
            }
        }

        UserData.UserAssetsHistory asset = userData.getUserAsset(uuid.toString());
        bal = asset.bal;




        p.sendMessage("§e§l===============[§f§l" + asset.player + "の資産§e§l]==============");
        String balMessage = "口座残高:" + Utility.getColoredPriceString(bal);

        p.sendMessage(balMessage);


        showAssetUUID(p, uuid);
        showOrderUUID(p, uuid);

        return true;
    }

    boolean showOrderUUID(Player p, String uuid)
    {
        ArrayList<MarketData.OrderInfo> orders = plugin.data.getOrderOfUser(p, uuid);
        if (orders == null) {
            return false;
        }

        if (orders.size() == 0) {
            return false;
        }



        long buyAmount = 0L;
        long sellAmount = 0L;
        double buyTotal = 0.0D;
        long sellTotal = 0L;
        String orderPlyer = "";
        for (MarketData.OrderInfo order : orders) {
            MarketData.ItemIndex itemIndex = plugin.data.getItemPrice(order.item_id);
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
        if(p.getUniqueId().toString().equalsIgnoreCase(uuid) == false){
            command += " "+orderPlyer;
        }

        Utility.sendHoverText(p, "§9§l--------[注文件数:" + orders.size() + "] => §9§n/mce order", "クリックすると注文を開きます", command);
        p.sendMessage("§f買い注文:" + Utility.getColoredPriceString(buyTotal) + " §f: " + Utility.getColoredItemString(buyAmount));

        p.sendMessage("§f売り注文評価額:" + Utility.getColoredPriceString(sellTotal) + " §f: " + Utility.getColoredItemString(sellAmount));


        return true;
    }



    UserData userData = null;







    boolean showBalance(Player sender, String playerName)
    {
        userData = new UserData(plugin);



        String uuid = null;

        if (playerName == null) {
            Player p = sender;
            uuid = p.getPlayer().getUniqueId().toString();
        } else {
            Player p = Bukkit.getPlayer(playerName);
            if(p != null){
                uuid = p.getUniqueId().toString();
            }
        }


        if ((uuid == null) && (playerName != null))
        {
            uuid = userData.getUUID(playerName);
            sender.sendMessage("プレイヤーがDBからみつかりました UUID: "+uuid);
        }

        if (uuid == null) {
            sender.sendMessage("プレイヤーはこのサーバに存在していません");
            return false;
        }

        showBalanceUUID(sender, uuid);

        return false;
    }

}