package red.man10;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.UUID;

// 7/26 IK追加、 ItemPayを扱うクラス
public class PayItem {

    static class PayLog{
        int id;
        UUID from_uuid;
        String from_name;
        UUID to_uuid;
        String to_name;
        int item_id;
        long amount;
    }

    static ItemBank ib;

    public static void EnableLoad(MarketData data){
        ib = data.itemBank;
    }

    synchronized public static void sendItemfromPlayer(Player from, String to, String idorkey, long amount){
        if(ib.data.getItemPrice(idorkey) == null){
            from.sendMessage(ib.plugin.prefix+"§c§lそのId/Keyは存在しません");
            return;
        }
        if(amount < 1){
            from.sendMessage(ib.plugin.prefix+"§c§l1個以上しか送れません");
            return;
        }
        String toUUID;
        if((toUUID = containPlayer(to)) == null){
            from.sendMessage(ib.plugin.prefix+"§c§lプレイヤーはこのサーバに存在していません");
            return;
        }
        if(from.getUniqueId().toString().equalsIgnoreCase(toUUID)){
            from.sendMessage(ib.plugin.prefix+"§c§l自分自身には送れません");
            return;
        }
        MarketData.ItemIndex itemIndex = ib.data.getItemPrice(idorkey);
        ItemBank.ItemStorage itemStorage = ib.getItemStorage(from.getUniqueId().toString(),itemIndex.id);
        if(itemStorage == null){
            from.sendMessage(ib.plugin.prefix+"§c§lあなたはアイテムバンクに§f§l"+itemIndex.key+"§c§lを持っていません");
            return;
        }
        if(itemStorage.amount < amount){
            from.sendMessage(ib.plugin.prefix+"§c§lあなたはアイテムバンクに§f§l"+itemIndex.key+"§c§lを必要数持っていません");
            from.sendMessage(ib.plugin.prefix+"§c§l必要数: "+amount+"個");
            return;
        }
        if(!ib.reduceItem(from.getUniqueId().toString(),itemIndex.id,amount)) {
            ib.data.showError(from.getUniqueId().toString(),"アイテムの引き出しに失敗した(重大)");
            ib.data.opLog(from.getName()+"のipayがSQLエラーで失敗した");
            ib.data.opLog("From: "+from.getUniqueId().toString()+" Name: "+from.getName()
                    +" To: "+toUUID+" Name: "+to+" ItemId: "+ itemIndex.id+" Amount: "+amount);
            ib.plugin.log("Error: itemPayにてSQL接続失敗、Logを残します。");
            ib.plugin.log("From: "+from.getUniqueId().toString()+" Name: "+from.getName()
                    +" To: "+toUUID+" Name: "+to+" ItemId: "+ itemIndex.id+" Amount: "+amount);
            return;
        }
        if(!ib.addItem(toUUID, itemIndex.id, amount)) {
            ib.data.showError(from.getUniqueId().toString(),"アイテムの追加に失敗した(重大)");
            ib.data.opLog(from.getName()+"のipayがSQLエラーで失敗した");
            ib.data.opLog("From: "+from.getUniqueId().toString()+" Name: "+from.getName()
                    +" To: "+toUUID+" Name: "+to+" ItemId: "+ itemIndex.id+" Amount: "+amount);
            ib.plugin.log("Error: itemPayにてSQL接続失敗、Logを残します。");
            ib.plugin.log("From: "+from.getUniqueId().toString()+" Name: "+from.getName()
                    +" To: "+toUUID+" Name: "+to+" ItemId: "+ itemIndex.id+" Amount: "+amount);
            return;
        }
        if(!createPayLog(from,toUUID,to,itemIndex.id,amount)){
            ib.plugin.log("Error: payLogにてSQL接続失敗、Logを残します。");
            ib.plugin.log("From: "+from.getUniqueId().toString()+" Name: "+from.getName()
                    +" To: "+toUUID+" Name: "+to+" ItemId: "+ itemIndex.id+" Amount: "+amount);
        }
        from.sendMessage(ib.plugin.prefix+"§f§lあなたは§6§l"+to+"§f§lに§e§l"+itemIndex.key+"§f§lを§b§l"+amount+"個§f§l送りました");

        OfflinePlayer offP = Bukkit.getOfflinePlayer(UUID.fromString(toUUID));

        if(offP.isOnline()){
            Player onP = offP.getPlayer();
            assert onP != null;
            offP.getPlayer().sendMessage(ib.plugin.prefix+"§6§l"+from.getName()+"§f§lさんから§e§l"+itemIndex.key+"§f§lが§b§l"+amount+"個§f§l送られてきました");
        }
    }

    public static String containPlayer(String playerName){
        String uuid;
        uuid = ib.data.userData.getUUID(playerName);
        return uuid;
    }

    public static boolean createPayLog(Player from, String to,String toname,int itemid,long amount){
        return ib.data.mysql.execute("insert into ipay_log values(0,"
                +"'" +from.getUniqueId().toString()+"',"
                +"'" +from.getName()+"',"
                +"'" +to+"',"
                +"'" +toname+"',"
                +itemid+","
                +amount +");");
    }

    public static ArrayList<PayLog> viewLog(UUID uuid){
        String sql = "select * from ipay_log where to_uuid = '"+uuid.toString()+"' or from_uuid= '"+uuid.toString()+"';";
        ArrayList<PayLog> loglist = new ArrayList<>();


        ResultSet rs = ib.data.mysql.query(sql);
        if(rs == null){
            ib.data.mysql.close();
            return loglist;
        }
        try {
            while(rs.next()) {
                PayLog log = new PayLog();
                log.amount = rs.getInt("amount");
                log.id = rs.getInt("id");
                log.item_id = rs.getInt("item_id");
                log.from_uuid = UUID.fromString(rs.getString("from_uuid"));
                log.from_name = rs.getString("from_name");
                log.to_name = rs.getString("to_name");
                log.to_uuid = UUID.fromString(rs.getString("to_uuid"));
                loglist.add(log);
            }
            rs.close();
        }catch (SQLException e) {
            Bukkit.getLogger().info("Error executing a query: " + e.getErrorCode());
            return loglist;
        }


        ib.data.mysql.close();
        return loglist;
    }


    public static void sendLogMessage(Player p,ArrayList<PayLog> loglist,int page){
        p.sendMessage("§e==========[ItemPayLog]===========");
        int count = 15;
        int minuspage = page * 15;
        for (int i = loglist.size()-1-minuspage; i >= 0; i--) {
            if(count <= 0){
                break;
            }
            count--;
            PayItem.PayLog log = loglist.get(i);
            String itemname = ib.data.getItemPrice(log.item_id).key;
            p.sendMessage("§e" + log.id + " §a" + log.from_name + " -> " + log.to_name + " §6" + itemname + " §e" + log.amount + "個");
        }
        p.sendMessage("§e==========[ItemPayLog]===========");
    }
}
