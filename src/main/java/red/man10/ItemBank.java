package red.man10;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class ItemBank {

//    pri MarketPlugin plugin;
   // private final MarketData data;

    MarketPlugin plugin = null;
    MarketData data = null;


    class ItemStorage{
        int item_id;
        String item_key;
        long amount;
    }




    //      アイテムアイテムバンクから取得
    public ItemStorage getItemStorage(String uuid, int item_id){
        String sql = "select * from item_storage where item_id = "+item_id +" and uuid= '"+uuid+"';";

        ItemStorage ret = new ItemStorage();
        ret.item_id = 0;
        ret.item_key = null;
        ret.amount = 0;

        ResultSet rs = data.mysql.query(sql);
        //  Bukkit.getLogger().info(sql);
        if(rs == null){
            return ret;
        }
        try
        {
            while(rs.next())
            {
                ret.item_id = rs.getInt("item_id");
                ret.item_key = rs.getString("key");
                ret.amount = rs.getLong("amount");
            }
            rs.close();
        }
        catch (SQLException e)
        {
            Bukkit.getLogger().info("Error executing a query: " + e.getErrorCode());
            return ret;
        }


        data.mysql.close();
        return ret;

    }

    public boolean updateItemStorage(String uuid,int item_id,long amount){
        String sql= "update item_storage set amount="+amount+" where item_id = "+item_id+" and uuid='"+uuid+"';";
        boolean ret =  data.mysql.execute(sql);


        return ret;
    }
    public boolean insertItemStorage(String uuid,int item_id,long amount) {

        OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
        String playerName = player.getName();

        MarketData.ItemIndex result =  data.getItemPrice(String.valueOf(item_id));
        boolean ret = data.mysql.execute("insert into item_storage values(0,"
                +"'" +uuid +"',"
                +"'" +playerName +"',"
                +item_id +","
                +"'" +result.key +"',"
                +amount+","
                +"'"+ data.currentTime() +"'"
                +");");
        return ret;
    }


    //   アイテムボックスへアイテムを送信する
    public boolean sendItemToStorage(String uuid,int item_id,int amount){


        OfflinePlayer player= Bukkit.getOfflinePlayer(UUID.fromString(uuid));

        MarketData.ItemIndex result =  data.getItemPrice(String.valueOf(item_id));
        if(result == null){
            if(player.isOnline()){
                plugin.showError((Player)player,"登録されていない");
            }
            return false;
        }

        long total = amount;
        ItemStorage store = getItemStorage(uuid,item_id);
        if(store.item_key == null){
            this.insertItemStorage(uuid,item_id,total);
        }else{
            total += store.amount;
            this.updateItemStorage(uuid,item_id,total);
        }

        if(player.isOnline()){
            Player online = (Player)player;
            plugin.showMessage(online,"アイテム:" + result.key +"が"+ amount+"個アイテムバンクに追加されました => "+total+"個");
//            plugin.showMessage(online,"現在トータル:" +total+"個");
        }


        data.logTransaction(uuid,"ReceivedItem",result.key,result.price,0,0,"");
        return true;
    }

    ////////////////////////////////////////////
    ///         アイテムバンクからアイテムを引き出す
    public boolean removeItemFromStorage(String uuid,int item_id,int amount){


        OfflinePlayer player= Bukkit.getOfflinePlayer(UUID.fromString(uuid));

        MarketData.ItemIndex result =  data.getItemPrice(String.valueOf(item_id));
        if(result == null){
            if(player.isOnline()){
                plugin.showError((Player)player,"登録されていない");
            }
            return false;
        }


        ItemStorage store = getItemStorage(uuid,item_id);
        if(store.item_key == null){
            return false;
        }
        long total = store.amount - amount;
        if(total > 0){
            this.updateItemStorage(uuid,item_id,total);
            if(player.isOnline()){
                Player online = (Player)player;
                plugin.showMessage(online,"アイテム:" + result.key +"が"+ amount+"個アイテムバンクからひきだされました ");
                plugin.showMessage(online,"現在トータル:" +data.getPriceString(total)+"個");
            }
            data.logTransaction(uuid,"ReceivedItem",result.key,result.price,0,0,"");
            return true;
        }

        //      引き出し失敗
        if(player.isOnline()){
            Player online = (Player)player;
            plugin.showError(online,"アイテムの引き出しに失敗しました！"+result.key +":"+ amount+"個");
            plugin.showMessage(online,"現在トータル:" +data.getPriceString(total)+"個");
        }

        return false;
    }




}
