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
            return showBalance(sender,args[1]);
        }

        if((sender instanceof Player) ){
            return showBalance(sender,null);

        }

        sender.sendMessage("プレイヤーのみ実行できます");
        return false;
    }

    UserData.UserAssetsHistory today = null;
    UserData.UserAssetsHistory last = null;


    boolean showAssetUUID(Command p,String uuid){

        String sql = "select * from user_assets_history where uuid = '"+uuid+"' order by id desc limit 2;";


        ArrayList<UserData.UserAssetsHistory> his = userData.getAssetHistory(sql);
        if(his.size() == 0){
            return false;
        }


        today = his.get(0);


        //      前日データあり
        if(his.size() == 2){
            last = his.get(1);
        }

        String ret = "§f§l残高:"+Utility.getPriceString(today.bal) + "§f§lアイテムバンク評価額:"+Utility.getPriceString(today.estimated_value) +" §f§lアイテム個数:"+Utility.getItemString((int)today.total_amount);



        return true;
    }

    boolean showBalanceUUID(CommandSender p,String uuid){

        double bal = plugin.vault.getBalance(UUID.fromString(uuid));


        p.sendMessage("=====================[あなたの資産]======================");
        String balMessage = "口座残高:" + Utility.getColoredPriceString(bal) + "  " + Utility.getJpBal(bal);

        p.sendMessage(balMessage);










        return true;
    }

    UserData userData = null;




    boolean showBalance(CommandSender sender,String playerName){


        userData = new UserData(plugin);

        String uuid = null;

        if(playerName == null){
            Player p = (Player)sender;
            uuid = p.getPlayer().getUniqueId().toString();
        }else{
            uuid = Bukkit.getPlayer(playerName).getUniqueId().toString();
        }

        //     Player名をUUIDから取得
        if(uuid == null && playerName != null){

            uuid = userData.getUUID(playerName);
        }

        if(uuid == null){
            sender.sendMessage("プレイヤーはこのサーバに存在していません");
            return false;
        }

        showBalanceUUID(sender,uuid);

        return false;
    }


}