package red.man10;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.UUID;

public class ItemBank {


    MarketPlugin plugin = null;
    MarketData data = null;

    class ItemStorage{
        int item_id;
        String item_key;
        long amount;
    }

    //      プレイヤーの持っているストレージリストを得る
    public ArrayList<ItemStorage> getStorageList(String uuid){

        //MarketData data = new MarketData(plugin);
        String sql = "select * from item_storage where uuid= '"+uuid+"' order by item_id;";


        ArrayList<ItemStorage> list = new ArrayList<ItemStorage>();

        ResultSet rs = data.mysql.query(sql);
        //  Bukkit.getLogger().info(sql);
        if(rs == null){
            return list;
        }
        try
        {
            while(rs.next())
            {
                ItemStorage storage = new ItemStorage();
                storage.item_id = rs.getInt("item_id");
                storage.item_key = rs.getString("key");
                storage.amount = rs.getLong("amount");
                list.add(storage);
            }
            rs.close();
        }
        catch (SQLException e)
        {
            Bukkit.getLogger().info("Error executing a query: " + e.getErrorCode());
            return list;
        }


        data.mysql.close();
        return list;

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


    void UpdateUserAsset(String uuid){
        //      ユーザの評価を更新
        Player p = Bukkit.getPlayer(UUID.fromString(uuid));
        if(p != null){
            this.data.userData.updateUserAssetsHistory(p);
        }
    }




    //      アイテム追加
    public boolean addItem(String uuid,int item_id,long amount){

        //      アイテムストレージになければ初期登録
        ItemStorage store = getItemStorage(uuid,item_id);
        if(store.item_key == null){
            return this.insertItemStorage(uuid,item_id,amount);
        }
       // MarketData data = new MarketData(plugin);

        //      追加
        boolean ret = data.mysql.execute("update item_storage set amount = amount + "+amount+" where uuid='"+uuid+"' and item_id="+item_id+";");
        UpdateUserAsset(uuid);

        data.showMessage(uuid,"§f§lアイテムバンクに"+store.item_key+"が"+Utility.getColoredItemString(amount)+"§f§l追加されました");

        Utility.playSound(uuid,Sound.ENTITY_PLAYER_LEVELUP);

        return ret;
    }

    public boolean reduceItem(String uuid,int item_id,long amount){

        //      アイテムストレージになければ初期登録
        ItemStorage store = getItemStorage(uuid,item_id);
        if(store.item_key == null){
            return false;
        }

        if( store.amount < amount){
            return  false;
        }

        boolean ret = data.mysql.execute("update item_storage set amount = amount - "+amount+" where uuid='"+uuid+"' and item_id="+item_id+";");
        UpdateUserAsset(uuid);

        data.showMessage(uuid,"§f§lアイテムバンクから"+store.item_key+"が"+Utility.getColoredItemString(amount)+"§f§l引き出されました");

        Utility.playSound(uuid,Sound.BLOCK_NOTE_PLING);

        return ret;
    }


    public boolean insertItemStorage(String uuid,int item_id,long amount) {

        OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
        String playerName = player.getName();
       // MarketData data = new MarketData(plugin);

        MarketData.ItemIndex result =  data.getItemPrice(String.valueOf(item_id));
        boolean ret = data.mysql.execute("insert into item_storage values(0,"
                +"'" +uuid +"',"
                +"'" +playerName +"',"
                +item_id +","
                +"'" +result.key +"',"
                +amount+","
                +"'"+ data.currentTime() +"'"
                +");");


        UpdateUserAsset(uuid);

        return ret;
    }

    public boolean setItem(String uuid,int item_id,long amount){
        //      アイテムストレージになければ初期登録
        ItemStorage store = getItemStorage(uuid,item_id);
        if(store.item_key == null){
            return this.insertItemStorage(uuid,item_id,amount);
        }
        // MarketData data = new MarketData(plugin);

        //      追加
        boolean ret = data.mysql.execute("update item_storage set amount = "+amount+" where uuid='"+uuid+"' and item_id="+item_id+";");
        UpdateUserAsset(uuid);

        data.showMessage(uuid,"§c§lアイテムバンクの"+store.item_key+"が"+Utility.getColoredItemString(amount)+"§c§lにセットされました");

        return ret;
    }


}
