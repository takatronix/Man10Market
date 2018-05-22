package red.man10;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

public class UserData {

    public  MarketPlugin plugin = null;
    public  MarketData data = null;


    UserData(MarketPlugin plugin){
        this.plugin = plugin;
        this.data = plugin.data;
    }

    class UserAssetsHistory{
        String uuid;
        String player;
        double bal;
        double estimated_value;
        long  total_amount;
        int year;
        int month;
        int day;
        String itemList;
    }


    String  createJoinMessage(Player p){


        String sql = "select * from user_assets_history where uuid = '"+p.getUniqueId().toString()+"' order by id desc limit 2;";


        ArrayList<UserAssetsHistory> his = getAssetHistory(sql);
        if(his.size() == 0){
            return null;
        }

        UserAssetsHistory today = his.get(0);
        UserAssetsHistory last =null;

        //      前日データあり
        if(his.size() == 2){
            last = his.get(1);
        }

        String ret = "§f§l残高:"+Utility.getPriceString(today.bal) + "§f§lアイテムバンク評価額:"+Utility.getPriceString(today.estimated_value) +" §f§lアイテム個数:"+Utility.getItemString((int)today.total_amount);


        return ret;
    }

    ArrayList<UserAssetsHistory> getAssetHistory(String sql){

        ArrayList<UserAssetsHistory> ret = new ArrayList<UserAssetsHistory>();

        ResultSet rs = data.mysql.query(sql);
        if(rs == null){
            plugin.getLogger().info("rs=null");
            return ret;
        }
        try
        {
            while(rs.next())
            {
                UserAssetsHistory his = new UserAssetsHistory();

                his.uuid = rs.getString("uuid");
                his.player = rs.getString("player");
                his.total_amount = rs.getLong("total_amount");
                his.bal = rs.getDouble("balance");
                his.estimated_value = rs.getDouble("estimated_valuation");
                his.year = rs.getInt("year");
                his.month = rs.getInt("month");
                his.day = rs.getInt("day");
                his.itemList = rs.getString("itemlist");
                ret.add(his);
            }
            rs.close();
        }
        catch (SQLException e)
        {
            Bukkit.getLogger().info("Error executing a query: " + e.getErrorCode()+" reason:"+e.getLocalizedMessage());
            return ret;
        }
         data.mysql.close();

        return ret;
    }

    String getUUID(String player){

        String sql = "select * from user_assets_history where player = '" +player+";";
        ArrayList<UserAssetsHistory> his = getAssetHistory(sql);
        if(his == null){
            return null;
        }
        if(his.size() == 0){
            return null;
        }
        return his.get(0).uuid;
    }



    //      ユーザーの資産をアップデート
    int updateUserAssetsHistory(Player p){
        String uuid = p.getUniqueId().toString();


        //
        ArrayList<ItemBank.ItemStorage> list = data.itemBank.getStorageList(uuid);

        if(list.size() == 0){
            return 0;
        }


        long totalAmount = 0;
        double estimatedValue = 0;
        String itemList = "";
        for(ItemBank.ItemStorage storage:list){
            totalAmount += storage.amount;
            MarketData.ItemIndex index  = data.getItemPrice(storage.item_id);
            estimatedValue += index.price;

            if(!itemList.isEmpty()){
                itemList += " ";
            }
            itemList += index.id+":"+storage.amount;
        }



        Calendar datetime = Calendar.getInstance();
        datetime.setTime(new Date());
        int year = datetime.get(Calendar.YEAR);
        int month = datetime.get(Calendar.MONTH) + 1;
        int day = datetime.get(Calendar.DAY_OF_MONTH);

        String where = "where uuid = '"+uuid+"' and year ="+year+" and month="+month+" and day="+day;

        //  ユーザーの本日のデータを一旦消去
        data.mysql.execute("delete from user_assets_history "+where+";");

        double bal = plugin.vault.getBalance(UUID.fromString(uuid));


        String sql = "insert into user_assets_history values(0,'"+uuid+"','"+ p.getName()+"',"+bal+","
                +estimatedValue+","
                +totalAmount+","
                +year+","
                +month+","
                +day+",'"
                +itemList+"');";


        if(data.mysql.execute(sql) == false){
           plugin.showError(p,"個人データの更新に失敗");
           return  -1;
        }


        return list.size();
    }

}
