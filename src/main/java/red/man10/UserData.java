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

    public  MarketPlugin plugin;
    public  MarketData data = null;


    UserData(MarketPlugin plugin){
        this.plugin = plugin;
    }

    static class UserAssetsHistory{
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






    void deposit(String uuid, double money){

        plugin.bankAPI.deposit(UUID.fromString(uuid),money,"Man10Market deposit");

    }

    public UserAssetsHistory getUserAsset(String uuid){
        String sql = "select * from user_assets_history where uuid = '"+uuid+"' order by id desc limit 1;";


        ArrayList<UserAssetsHistory> his = getAssetHistory(sql);
        if(his.size() == 0){
            return null;
        }
        return his.get(0);
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

        String sql = "select * from user_assets_history where player = '" +player+"';";
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
    void updateUserAssetsHistory(Player p){

        if(p == null){
            Bukkit.getLogger().info("updateUserAssetsHistory null");
            return;
        }
        String uuid = p.getUniqueId().toString();

       // Bukkit.getLogger().info("data.itemBank.getStorageList :" + p.getName());

        //
        ArrayList<ItemBank.ItemStorage> list = data.itemBank.getStorageList(uuid);

        if(list.size() == 0){
            return;
        }



        long totalAmount = 0;
        double estimatedValue = 0;
        StringBuilder itemList = new StringBuilder();
        for(ItemBank.ItemStorage storage:list){
            totalAmount += storage.amount;
            MarketData.ItemIndex index  = data.getItemPrice(storage.item_id);
            estimatedValue += index.price * storage.amount;

            //Bukkit.getLogger().info("key:"+storage.item_key +"$" + index.price * storage.amount);

            if(itemList.length() > 0){
                itemList.append(" ");
            }
            itemList.append(index.id).append(":").append(storage.amount);
        }

        Bukkit.getLogger().info("price checked");


        Calendar datetime = Calendar.getInstance();
        datetime.setTime(new Date());
        int year = datetime.get(Calendar.YEAR);
        int month = datetime.get(Calendar.MONTH) + 1;
        int day = datetime.get(Calendar.DAY_OF_MONTH);

        String where = "where uuid = '"+uuid+"' and year ="+year+" and month="+month+" and day="+day;

        //  ユーザーの本日のデータを一旦消去
        data.mysql.execute("delete from user_assets_history "+where+";");

        double bal = plugin.vault.getBalance(UUID.fromString(uuid));
       // Bukkit.getLogger().info("get balcne"+ bal);


        String sql = "insert into user_assets_history values(0,'"+uuid+"','"+ p.getName()+"',"+bal+","
                +estimatedValue+","
                +totalAmount+","
                +year+","
                +month+","
                +day+",'"
                +itemList+"');";


        if(!data.mysql.execute(sql)){
            plugin.showError(p,"個人データの更新に失敗");
            return;
        }


        Bukkit.getLogger().info("ユーザーデータ更新成功"+p.getName());


    }

}
