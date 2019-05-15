package red.man10;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
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



    class UserInformation{
        double balance;
    }


    ////////////////////////////////////////////
    //     get user information
    ////////////////////////////////////////////
    UserInformation getUserInformation(String uuid){


        String sql = "select * from user_index where uuid='"+uuid+"';";
        ResultSet rs = data.mysql.query(sql);
        UserInformation ui = null;
        if(rs == null){
            return null;
        }
        try {
            while (rs.next()) {
                ui = new UserInformation();
                ui.balance = rs.getDouble("balance");
                break;
            }
            rs.close();

        }catch(Exception e){

        }

        data.mysql.close();
        return ui;
    }


    boolean insertUserInformation(String uuid){

        Player p = Bukkit.getPlayer(UUID.fromString(uuid));
        if(p == null){
            Bukkit.getLogger().info("insertUserInformation 取得失敗");
            return false;
        }
        Bukkit.getLogger().info("ユーザーデータ挿入中");

        String sql = "insert into user_index values(0,'" + uuid + "','"+p.getName()+"',0,null,0,0);";
        return  data.mysql.execute(sql);
    }


    boolean deposit(String uuid,double money){

        UserInformation ui = getUserInformation(uuid);
        if(ui == null){
            if(insertUserInformation(uuid) == false){
                return false;
            }
        }


        //      追加
        boolean ret = data.mysql.execute("update user_index set balance = balance + "+money+" where uuid='"+uuid+"';");


        return true;
    }


    boolean withdraw(String uuid,double money){

        UserInformation ui = getUserInformation(uuid);
        if(ui == null){
            return false;
        }

        if(ui.balance < money){
            data.showError(uuid,"残額より多くはひきだせない");
            return false;
        }

        boolean ret = data.mysql.execute("update user_index set balance = balance - "+money+" where uuid='"+uuid+"';");



        if(ret){
            this.plugin.vault.deposit(UUID.fromString(uuid),money);
            data.showMessage(uuid,Utility.getColoredPriceString(money)+"§f§l口座に追加されました");
        }else{
            data.showError(uuid,"口座からの引き出しに失敗した");

        }

        return true;
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



    //          売上を表示するs
    void showEarnings(Player p,String uuid){
        ///
        UserData.UserInformation ui = this.getUserInformation(uuid);
        if(ui != null){
            if(ui.balance != 0){
                Utility.sendHoverText(p,"§k§l $$$$ §f§l§nあなたの売上金額:"+Utility.getColoredPriceString(ui.balance) +" §l§n [引き出す] => /mce withdraw","クリックすると支払われます /mce withdraw","/mce withdraw");
            }
        }
    }



    //      ユーザーの資産をアップデート
    int updateUserAssetsHistory(Player p){

        if(p == null){
            Bukkit.getLogger().info("updateUserAssetsHistory null");
            return 0;
        }
        String uuid = p.getUniqueId().toString();

        Bukkit.getLogger().info("data.itemBank.getStorageList :" + p.getName());

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
            estimatedValue += index.price * storage.amount;

            //Bukkit.getLogger().info("key:"+storage.item_key +"$" + index.price * storage.amount);

            if(!itemList.isEmpty()){
                itemList += " ";
            }
            itemList += index.id+":"+storage.amount;
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
        Bukkit.getLogger().info("get balcne"+ bal);


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


        Bukkit.getLogger().info("ユーザーデータ更新成功"+p.getName());


        return list.size();
    }

}
