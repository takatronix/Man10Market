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
import java.util.Date;
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



    public boolean canSell(Player p,ItemStack item,double price,int count,PriceResult current){

        if(item.getAmount() < count){
            plugin.showError(p,"指定された"+count+"個のアイテムをもっていない");
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

    public boolean orderSell(Player p,ItemStack item,double price,int count){

        //      まず現在価格を求める
        PriceResult current = getItemPrice(p,item);
        if(current.result == false){
            plugin.showError(p,"このアイテムは販売できません");
            return false;
        }

        //      値段が正当かチェック
        if(canSell(p,item,price,count,current) == false){
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
                +count+",0,"

                +"'"+currentTime()+"'"
                +");");


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
                ret.key = rs.getString("key");
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

        String sql = "select * from item order_tbl order by id;";

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
                String key = rs.getString("key");
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
