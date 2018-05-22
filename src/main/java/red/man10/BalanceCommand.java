package red.man10;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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



        if(args.length > 2){
            sender.sendMessage("/balance/bal/mbal/mblance [playername]");
            return false;
        }

        if(args.length == 2){
            return showBalance((Player)sender,args[1]);
        }

        if((sender instanceof Player) ){
            return showBalance((Player)sender,null);

        }

        sender.sendMessage("プレイヤーのみ実行できます");
        return false;
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
        double bal = plugin.vault.getBalance(UUID.fromString(uuid));


        OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(uuid));

        userData.updateUserAssetsHistory((Player)player);


        p.sendMessage("§e§l=====================[" + player.getName() + "の資産]======================");
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
        for (MarketData.OrderInfo order : orders) {
            if (order.isBuy) {
                buyAmount += order.amount;
                buyTotal += order.price * order.amount;
            } else {
                sellAmount += order.amount;


                MarketData.ItemIndex itemIndex = plugin.data.getItemPrice(order.item_id);
                sellTotal += itemIndex.price * order.amount;
            }
        }


        Utility.sendHoverText(p, "§9§l--------[注文件数:" + orders.size() + "] => §9§n/mce order", "クリックすると注文を開きます", "/mce order");
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
            uuid = Bukkit.getPlayer(playerName).getUniqueId().toString();
        }


        if ((uuid == null) && (playerName != null))
        {
            uuid = userData.getUUID(playerName);
        }

        if (uuid == null) {
            sender.sendMessage("プレイヤーはこのサーバに存在していません");
            return false;
        }

        showBalanceUUID(sender, uuid);

        return false;
    }

}