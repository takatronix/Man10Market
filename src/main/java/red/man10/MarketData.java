package red.man10;

import org.bukkit.Bukkit;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class MarketData {

    private final MarketPlugin plugin;
    MySQLManager mysql = null;
    public MarketData(MarketPlugin plugin) {
        this.plugin = plugin;
        this.mysql = new MySQLManager(plugin,"Market");
    }

    class PriceResult{
        int id;
        String key;
        double price;
        int sell;
        int buy;
        boolean result;
    }

    class OrderInfo{
        int id;
        int item_id;
        String key;
        String uuid;
        String player;
        double price;
        int amount;
        boolean isBuy = false;
        boolean result;
//        DateTime datetime;
    }

    ///   オーダー情報を得る
    public List<OrderInfo> getOrderInfo(Player p,int item_id, double price, boolean buy){
        String sql = "select * from order_tbl where item_id = "+item_id+ " and buy="+buy+";";

        ArrayList<OrderInfo> ret = new ArrayList<OrderInfo>();

        ResultSet rs = mysql.query(sql);
        if(rs == null){
            plugin.showError(p,"データ取得失敗");
            return ret;
        }
        try
        {
            while(rs.next())
            {
                OrderInfo info = new OrderInfo();
                info.id = rs.getInt("id");
                info.item_id = rs.getInt("item_id");
                info.key = rs.getString("key");
                info.uuid = rs.getString("uuid");
                info.player = rs.getString("player");
                info.price = rs.getDouble("price");
                info.amount = rs.getInt("amount");
                ret.add(info);
            }
        }
        catch (SQLException e)
        {
            plugin.showError(p,"データ取得失敗");
            Bukkit.getLogger().info("Error executing a query: " + e.getErrorCode());
            return ret;
        }

        return ret;
    }


    ///  ログ
    public boolean logTransaction(Player player,String action,double price,int amount){
        String playerName =player.getName();
        String world = player.getLocation().getWorld().getName().toString();
        double x = player.getLocation().getX();
        double y = player.getLocation().getY();
        double z = player.getLocation().getZ();

        boolean ret = mysql.execute("insert into transaction_log values(0,"
                +"'" +player.getUniqueId() +"',"
                +"'" +playerName +"',"
                +"'" +action +"',"
                +price+","
                +amount+","
                +"'" +world +"',"
                +x+","
                +y+","
                +z+","

                +"'"+ currentTime() +"'"
                +");");



        plugin.opLog(" "+playerName + ":"+action+" $"+getPriceString(price) + ":"+amount);
        return ret;


    }

    public boolean sellExchange(Player p,int item_id,double price,int amount){

        // 同じ価格で、買いがあれば、交換
        List<OrderInfo> orders = getOrderInfo(p,item_id,price,true);
        for (OrderInfo o : orders) {

            //   買い注文 < 売り注文
            if(o.amount < amount){



            //  売り注文 > 買い注文
            }else if(o.amount > amount){


                //  売り注文 == 買い注
                // 消滅
            }else if(o.amount == amount){

            }


        }



        return true;
    }



    public boolean canBuy(Player p,double price,int amount ,PriceResult current) {
        double bal = plugin.vault.getBalance(p.getUniqueId());
        if(bal < price*amount){
            plugin.showError(p,"残額がたりません! 必要金額:$"+getPriceString(price*amount) +" 残額:$"+ getPriceString(bal));
            return false;
        }
        if(current.price < price){
            plugin.showError(p,"現在値より高い値段で注文はできません。購入したい場合は、成り行き買いをおこなってください /marketbuy or /buy");
            return false;
        }


        return true;
    }
    public boolean orderBuy(Player p,String idOrKey,double price,int amount){

        //      まず現在価格を求める
        PriceResult current = getItemPrice(p,idOrKey);
        if(current.result == false){
            plugin.showError(p,"このアイテムは販売されていません");
            return false;
        }
        //      値段が正当かチェック
        if(canBuy(p,price,amount,current) == false){
            return false;
        }



        String playerName = p.getName();
        String uuid = p.getUniqueId().toString();


        boolean ret = mysql.execute("insert into order_tbl values(0,"
                +current.id +","
                +"'" +current.key +"',"

                +"'" +uuid +"',"
                +"'" +playerName +"',"
                +price+","
                +amount+",1,"

                +"'"+currentTime()+"'"
                +");");

        return ret;
    }



    public boolean showboard(Player p,int item_id){
        //      売りデータ
        String sql = "select sum(amount),price from order_tbl where item_id = "+item_id+ " and buy = 0 group by price order by price desc ;";


        return true;
    }


    public boolean canSell(Player p,ItemStack item,double price,int amount,PriceResult current){

        if(item.getAmount() < amount){
            plugin.showError(p,"指定された"+amount+"個のアイテムをもっていない");
            return false;
        }


        ///  値段の正当性チェック
        if(price > current.price * plugin.sellLimitRatio){
            plugin.showError(p,"現在値の" + plugin.sellLimitRatio + "倍以上の金額で売ることはできません");
            plugin.showPrice(p);
            return false;
        }


        if(price < current.price / plugin.sellLimitRatio){
            plugin.showError(p,"現在値の1/" +plugin.sellLimitRatio +"以下の金額で売ることはできません");
            plugin.showPrice(p);
            return false;
        }

        return true;
    }

    public boolean orderSell(Player p,ItemStack item,double price,int amount){

        //      まず現在価格を求める
        PriceResult current = getItemPrice(p,item);
        if(current.result == false){
            plugin.showError(p,"このアイテムは販売できません");
            return false;
        }

        //      値段が正当かチェック
        if(canSell(p,item,price,amount,current) == false){
            return false;
        }


        String playerName = p.getName();
        String uuid = p.getUniqueId().toString();


        boolean ret = mysql.execute("insert into order_tbl values(0,"
                +current.id +","
                +"'" +current.key +"',"

                +"'" +uuid +"',"
                +"'" +playerName +"',"
                +price+","
                +amount+",0,"

                +"'"+currentTime()+"'"
                +");");


        return ret;
    }
    /// アイテム価格を取得する
    public PriceResult  getItemPrice(Player p,String idOrKey) {
    //    p.sendMessage("getItemPrice()->"+idOrKey);

        int id = -1;
        try{
            id = Integer.parseInt(idOrKey);
           // p.sendMessage("id = "+id);
        }catch(Exception e){
           //     p.sendMessage("パース失敗");

          //  plugin.log(e.getMessage());
        }

        String sql = "";
        //      IDが数値ではない -> key
        if(id == -1){
            sql =  "select * from item where item_key = '"+idOrKey+"';";
        }else{
            sql =  "select * from item where  id = "+id+";";
        }
//        p.sendMessage(sql);
        PriceResult ret = new PriceResult();
        ret.result = false;
        ret.price = 0;
        ResultSet rs = mysql.query(sql);
        if(rs == null){
            plugin.showError(p,"データ取得失敗");
            return ret;
        }
        try
        {
            while(rs.next())
            {
                ret.id = rs.getInt("id");
                ret.key = rs.getString("item_key");
                ret.price = rs.getDouble("price");
                ret.sell = rs.getInt("sell");
                ret.buy = rs.getInt("buy");
                ret.result = true;
            }
        }
        catch (SQLException e)
        {
            plugin.showError(p,"データ取得失敗");
            Bukkit.getLogger().info("Error executing a query: " + e.getErrorCode());
            return ret;
        }

        return ret;
    }

    /// アイテム価格を取得する
    public PriceResult  getItemPrice(Player p,ItemStack item){
        ItemStack newItem = new ItemStack(item);
        newItem.setAmount(1);
        String base64 = itemToBase64(newItem);

        PriceResult ret = new PriceResult();
        ret.result = false;
        ret.price = 0;

        String sql = "select * from item where base64 = '"+base64+"';";

        ResultSet rs = mysql.query(sql);
        if(rs == null){
            plugin.showError(p,"データ取得失敗");
            return ret;
        }
        try
        {
            while(rs.next())
            {
                ret.id = rs.getInt("id");
                ret.key = rs.getString("item_key");
                ret.price = rs.getDouble("price");
                ret.sell = rs.getInt("sell");
                ret.buy = rs.getInt("buy");
                ret.result = true;
            }
        }
        catch (SQLException e)
        {
            plugin.showError(p,"データ取得失敗");
            Bukkit.getLogger().info("Error executing a query: " + e.getErrorCode());
            return ret;
        }

        return ret;
    }


    public boolean showItemList(Player p){

        String sql = "select * from item order by id;";

        ResultSet rs = mysql.query(sql);
        if(rs == null){
            plugin.showError(p,"データ取得失敗");
            return false;
        }
        try
        {
            while(rs.next())
            {
                int id = rs.getInt("id");
                String idString = String.format("ID:%3d",id);
                String key = rs.getString("item_key");
                double price = rs.getDouble("price");
                int sell = rs.getInt("sell");
                int buy = rs.getInt("buy");

                p.sendMessage(idString + " §f§l"+key+ " §e§lPrice:$" + getPriceString(price) + " §c§l売り注文数(Sell):"+sell +"/§9§l買い注文数(Sell):"+buy);
            }
        }
        catch (SQLException e)
        {
            plugin.showError(p,"データ取得失敗");
            Bukkit.getLogger().info("Error executing a query: " + e.getErrorCode());
            return false;
        }

        return true;
    }


    public String getPriceString(double price){
        return String.format("%,d",(int)(price));
    }


    // アイテム登録
    public boolean registerItem(UUID uuid, ItemStack item,String key, double initialPrice, double tick){


        String playerName = Bukkit.getOfflinePlayer(uuid).getName();

        String itemName = item.getItemMeta().getDisplayName();
        String itemType = item.getType().toString();

        if(itemName == null){
            itemName = item.getType().toString();
        }

        boolean ret = mysql.execute("insert into item values(0,"
                +"'" +uuid.toString() +"',"
                +"'" +playerName +"',"
                +"'" +key +"',"
                +"'" +itemName +"',"
                +"'" +itemType +"',"
                +"" +item.getDurability() +","

                +initialPrice +","
                +initialPrice+","
                +tick+","
                +"'"+ currentTime() +"',0,0,"
                +"'"+ itemToBase64(item) +"'"
                +");");


        return ret;
    }


    public String currentTime(){
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy'-'MM'-'dd' 'HH':'mm':'ss");
        String currentTime = sdf.format(date);
        return currentTime;
    }

    public static ItemStack itemFromBase64(String data) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemStack[] items = new ItemStack[dataInput.readInt()];

            // Read the serialized inventory
            for (int i = 0; i < items.length; i++) {
                items[i] = (ItemStack) dataInput.readObject();
            }

            dataInput.close();
            return items[0];
        } catch (Exception e) {
            return null;
        }
    }

    public static String itemToBase64(ItemStack item) throws IllegalStateException {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            ItemStack[] items = new ItemStack[1];
            items[0] = item;
            dataOutput.writeInt(items.length);

            for (int i = 0; i < items.length; i++) {
                dataOutput.writeObject(items[i]);
            }

            dataOutput.close();
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to save item stacks.", e);
        }
    }

}
