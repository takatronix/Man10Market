package red.man10;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
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
        double maxPrice;
        double minPrice;
        int sell;
        int buy;
        boolean result;
    }
    class ItemStorage{
        int item_id;
        String item_key;
        long amount;
    }
    class OrderInfo{
        int id;
        int item_id;
        String key;
        String uuid;
        String player;
        int buy;
        int sell;
        double price;
        int amount;
        boolean isBuy = false;
        boolean result;
//        DateTime datetime;
    }

    void showError(String uuid,String message){
        Player player = Bukkit.getPlayer(UUID.fromString(uuid));
        if(player != null){
            plugin.showError(player,message);
        }
    }

    ///   オーダー情報を得る
    public ArrayList<OrderInfo> getOrderInfo(String uuid,int item_id, double price, boolean buy){
        String sql = "select * from order_tbl where item_id = "+item_id+ " and buy="+buy+" and price="+price+";";

        ArrayList<OrderInfo> ret = new ArrayList<OrderInfo>();

        ResultSet rs = mysql.query(sql);
        if(rs == null){
            showError(uuid,"データ取得失敗");
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
            showError(uuid,"データ取得失敗");
            Bukkit.getLogger().info("Error executing a query: " + e.getErrorCode());
            return ret;
        }

        return ret;
    }


    //////////////////////////////////////////////////////////////////////
    ///             グループ化した価格帯を取得
    public ArrayList<OrderInfo> getGroupedOrderList(String uuid,int item_id,boolean isBuy,int limit){
        String sql = "select sum(amount),price from order_tbl where item_id = "+item_id+ " and buy = "+isBuy+ " group by price order by price desc;";

        ArrayList<OrderInfo> ret = new ArrayList<OrderInfo>();

        ResultSet rs = mysql.query(sql);
        if(rs == null){
            showError(uuid,"データ取得失敗");
            return ret;
        }
        try
        {
            while(rs.next())
            {
                OrderInfo info = new OrderInfo();
                info.price = rs.getDouble("price");
                info.amount  = rs.getInt("sum(amount)");

                if(isBuy){
                    info.isBuy = true;
                    info.buy = info.amount;
                }else{
                    info.isBuy = false;
                    info.sell = info.amount;
                }
                ret.add(info);
            }
        }
        catch (SQLException e)
        {
            showError(uuid,"データ取得失敗");
            return ret;
        }

        return ret;
    }

    //      履歴保存
    public boolean logHistory(int item_id,double price){
        String sql = "insert into price_history values(0,"+item_id+","+price+",'"+currentTime()+"');";
        return mysql.execute(sql);
    }

    //      現在値更新
    public boolean updateCurrentPrice(int item_id){

        //  現在価格を求める
        PriceResult current = getItemPrice(String.valueOf(item_id));
        if(current == null){
            return false;
        }

        ArrayList<OrderInfo> sellList = this.getGroupedOrderList(null,item_id,false,-1);
        ArrayList<OrderInfo> buyList = this.getGroupedOrderList(null,item_id,true,-1);

        if(sellList.size() == 0){
            return false;
        }

        int sell = 0;
        double price = current.price;
        for(OrderInfo o : sellList){
            sell += o.amount;
            price = o.price;  // 高い順のリストなので最後の数値が最安値
        }

        int buy = 0;
        for(OrderInfo o : buyList){
            buy += o.amount;
        }

        double max = current.maxPrice;
        double min = current.minPrice;
        if(min == 0){
            min = current.price;
        }
        //   値上がり
        if(current.price < price){
            plugin.serverMessage( "§a§l"+current.key +": $"+getPriceString(current.price) + "から$"+getPriceString(price)+"へ値上がりしました");


            //      過去最高高値更新
            if(current.maxPrice < price){
                plugin.serverMessage( "§a§lマーケット速報!! "+current.key +": $"+getPriceString(price) + " 過去最高高値更新!!!");
                max = price;
            }


            //  履歴更新
            logHistory(item_id,price);
        }
        //  値下がり
        if(current.price > price){
            plugin.serverMessage( "§c§l"+current.key +": $"+getPriceString(current.price) + "から$"+getPriceString(price)+"へ値下がりしました");
            //      過去最高高値更新
            if(current.minPrice > price){
                plugin.serverMessage( "§c§lマーケット速報!! "+current.key +": $"+getPriceString(price) + " 過去最高安値更新!!!");
                min = price;
            }
            //  履歴更新
            logHistory(item_id,price);
        }


        String sql = "update item_index set price="+price+", sell="+sell+",buy="+buy+",max_price="+max+",min_price="+min+",datetime='"+currentTime()+"' where id="+item_id+";";
        boolean ret = this.mysql.execute(sql);


        return ret;
    }


    ///////////////////////////////
    //         板情報を表示する
    public boolean showOrderBook(Player p,int item_id,int limit){

        PriceResult item = getItemPrice(String.valueOf(item_id));
        String uuid = p.getUniqueId().toString();

        if(item == null) {
            showError(uuid,"このアイテムは売買できません");
            return false;
        }

        ArrayList<OrderInfo> sellList = this.getGroupedOrderList(uuid,item_id,false,-1);
        ArrayList<OrderInfo> buyList = this.getGroupedOrderList(uuid,item_id,true,-1);

        plugin.showMessage(p,"---------["+item.key+"]-----------");
        plugin.showMessage(p," §b§l売数量    値段     買数量");
        for(int i = 0;i < sellList.size();i++){
            OrderInfo o = sellList.get(i);
            String color ="§e§l";
            if(i ==  sellList.size() -1){
                color  = "§a§l";
            }
            plugin.showMessage(p,String.format("%s%7d  %7s",color,o.amount,getPriceString(o.price)));
        }
        for(int i = 0;i < buyList.size();i++){
            OrderInfo o = buyList.get(i);
            String color ="§e§l";
            if(i == 0){
                color  = "§c§l";
            }
            plugin.showMessage(p,String.format("%s         %7s  %7d",color,getPriceString(o.price),o.amount));
        }

        return true;
    }
    
    //      オーダー更新
    public boolean updateOrder(String uuid,int id,int amount){
        String sql = "update order_tbl set amount = "+amount+" where id = "+id+";";
        return mysql.execute(sql);
    }
    //   　オーダー削除
    public boolean deleteOrder(String uuid,int id){
        String sql = "delete from order_tbl where id = "+id+";";
        return mysql.execute(sql);
    }
    ///  ログ
    public boolean logTransaction(String uuid,String action,String idOrKey,double price,int amount,int order_id,String targetUUID){

        OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
        String playerName =player.getName();


        String world = "offline";
        double x = 0;
        double y = 0;
        double z = 0;
        if(player.isOnline()){
            Player online = (Player)player;
            world = online.getWorld().getName().toString();
            x = online.getLocation().getX();
            y = online.getLocation().getY();
            z = online.getLocation().getZ();
        }

        boolean ret = mysql.execute("insert into transaction_log values(0,"
                +"'"+idOrKey+"',"

                +order_id+","
                +"'" +player.getUniqueId() +"',"
                +"'" +targetUUID +"',"
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

        //  opへ通知
        plugin.opLog(" "+playerName + ":"+action+" $"+getPriceString(price) + ":"+amount);
        return ret;
    }


    //  アイテムがうれた
    public boolean sendMoney(String uuid,int item_id,double price,int amount,String uuidBuyer){

        OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
        String playerName = player.getName();

        PriceResult info =  getItemPrice(String.valueOf(item_id));
        double money =  price*amount;


        plugin.vault.deposit(player.getUniqueId(),money);
        logTransaction(uuid,"ReceivedMoney",info.key,price,amount,0,uuidBuyer);

        if(player.isOnline()){
            Player online = (Player)player;
            plugin.showMessage(online,info.key + "が"+amount+"個売れ、§e§l$"+getPriceString(money)+"受け取りました");
            return true;
        }
        return false;
    }
    //  お金支払い
    public boolean payMoney(String uuid,int item_id,double price,int amount){

        OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
        String playerName = player.getName();

        PriceResult info =  getItemPrice(String.valueOf(item_id));
        double money =  price*amount;


        if(plugin.vault.withdraw(player.getUniqueId(),money) == false){
            Player online = (Player)player;
            plugin.showError(online,"$"+getPriceString(money)+"の引き出しに失敗しました");
            return false;

        }

        logTransaction(uuid,"WithdrawMoney",info.key,price,amount,0,"");

        if(player.isOnline()){
            Player online = (Player)player;
            plugin.showMessage(online,info.key + "を"+amount+"個注文し、§e§l$"+getPriceString(money)+"引き出されました");
            return true;
        }
        return true;
    }



    //      アイテムストレージから取得
    public ItemStorage getItemStorage(String uuid,int item_id){
        String sql = "select * from item_storage where item_id = "+item_id +" and uuid= '"+uuid+"';";

        ItemStorage ret = new ItemStorage();
        ret.item_id = 0;
        ret.item_key = null;
        ret.amount = 0;

        ResultSet rs = mysql.query(sql);
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

              //  Bukkit.getLogger().info("amount:"+ret.amount);
            }
        }
        catch (SQLException e)
        {
            Bukkit.getLogger().info("Error executing a query: " + e.getErrorCode());
            return ret;
        }

        return ret;

    }

    public boolean updateItemStorage(String uuid,int item_id,long amount){
        String sql= "update item_storage set amount="+amount+" where item_id = "+item_id+";";
        boolean ret =  this.mysql.execute(sql);


        return ret;
    }
    public boolean insertItemStorage(String uuid,int item_id,long amount) {

        OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
        String playerName = player.getName();

        PriceResult result =  getItemPrice(String.valueOf(item_id));
        boolean ret = mysql.execute("insert into item_storage values(0,"
                +"'" +uuid +"',"
                +"'" +playerName +"',"
                +item_id +","
                +"'" +result.key +"',"
                +amount+","
                +"'"+ currentTime() +"'"
                +");");
        return ret;
    }


    //   アイテムボックスへアイテムを送信する
    public boolean sendItemToStorage(String uuid,int item_id,int amount){


        OfflinePlayer player= Bukkit.getOfflinePlayer(UUID.fromString(uuid));

        PriceResult result =  getItemPrice(String.valueOf(item_id));
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
            plugin.showMessage(online,"アイテム:" + result.key +"が"+ amount+"個ストレージに追加されました/mm ");
            plugin.showMessage(online,"現在トータル:" +total+"個");
        }


        logTransaction(uuid,"ReceivedItem",result.key,result.price,0,0,"");
        return true;
    }

    ////////////////////////////////////////////
    ///         ストレージからアイテムを引き出す
    public boolean removeItemFromStorage(String uuid,int item_id,int amount){


        OfflinePlayer player= Bukkit.getOfflinePlayer(UUID.fromString(uuid));

        PriceResult result =  getItemPrice(String.valueOf(item_id));
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
                plugin.showMessage(online,"アイテム:" + result.key +"が"+ amount+"個ストレージからひきだされました ");
                plugin.showMessage(online,"現在トータル:" +getPriceString(total)+"個");
            }
            logTransaction(uuid,"ReceivedItem",result.key,result.price,0,0,"");
            return true;
        }

        //      引き出し失敗
        if(player.isOnline()){
            Player online = (Player)player;
            plugin.showError(online,"アイテムの引き出しに失敗しました！"+result.key +":"+ amount+"個");
            plugin.showMessage(online,"現在トータル:" +getPriceString(total)+"個");
        }


        return false;
    }



    /////////////////////////////////////////
    //  売り交換処理(ここ重要)
    public int sellExchange(String uuid,int item_id,double price,int amount){
        int soldAmount = 0;
        //  買い注文を列挙
        ArrayList<OrderInfo> orders = getOrderInfo(uuid,item_id,price,true);
        for (OrderInfo o : orders) {

            //   買い注文 < 売り注文
            if(o.amount < amount){
                //  売れるだけさばき次注文へ
                // オーダー削除
                deleteOrder(uuid,o.id);
                //    購入者へアイテムを届ける
                sendItemToStorage(o.uuid,item_id,o.amount);
                //   料金を支払
                sendMoney(uuid.toString(),item_id,price,o.amount,o.uuid);
                amount -= o.amount; //　残注文
                soldAmount += o.amount; // 購入で北数
            //  買い注文 > 売り注文
            }else if(o.amount > amount){
                int leftAmount = o.amount - amount;
                //  購入者の注文量を減らす
                updateOrder(uuid,o.id,leftAmount);
                //    購入者へアイテムを届ける
                sendItemToStorage(o.uuid,item_id,amount);
                //   料金を支払
                sendMoney(uuid,item_id,price,amount,o.uuid);
                return amount;
                //  売り注文 == 買い注文
            }else if(o.amount == amount){
                // オーダー削除
                deleteOrder(uuid,o.id);
                //    購入者へアイテムを届ける
                sendItemToStorage(o.uuid,item_id,amount);
                //   料金を支払
                sendMoney(uuid,item_id,price,amount,o.uuid);
                return amount;
            }
        }
        return soldAmount;
    }

    /////////////////////////////////////////
    //  買い交換処理(ここ重要)
    //   購入できた個数を返す
    public int buyExchange(String uuidBuyer,int item_id,double price,int amount){

        //      売り注文を列挙
        ArrayList<OrderInfo> orders = getOrderInfo(uuidBuyer,item_id,price,false);

        int retOrderAmount = 0;
        for (OrderInfo o : orders) {
            //   買い注文 < 売り注文
            if(o.amount < amount){
                //  買い注文数をへらす
                int leftAmount = o.amount - amount;
                updateOrder(uuidBuyer,o.id,leftAmount);
                //    購入できたぶんアイテムを届ける
                sendItemToStorage(uuidBuyer,item_id,amount);
                //   料金を支払
                sendMoney(o.uuid,item_id,o.price,amount,uuidBuyer);
                return amount;
                //  買い注文 > 売り注文
            }else if(o.amount > amount){

                // オーダー削除
                deleteOrder(uuidBuyer,o.id);

                //    購入者へアイテムを届ける
                sendItemToStorage(uuidBuyer,item_id,amount);

                //   料金を支払
                sendMoney(o.uuid,item_id,o.price,o.amount,uuidBuyer);

                retOrderAmount += amount;
                //  売り注文 == 買い注文
            }else if(o.amount == amount){
                // オーダー削除
                deleteOrder(uuidBuyer,o.id);
                //    購入者へアイテムを届ける
                sendItemToStorage(uuidBuyer,item_id,amount);
                //   料金を支払
                sendMoney(o.uuid,item_id,o.price,amount,uuidBuyer);
                return amount;
            }
        }

        return retOrderAmount;
    }

    public boolean canBuy(Player p,double price,int amount ,PriceResult current) {
        double bal = plugin.vault.getBalance(p.getUniqueId());
        if(bal < price*amount){
            plugin.showError(p,"残額がたりません! 必要金額:$"+getPriceString(price*amount) +" 残額:$"+ getPriceString(bal));
            return false;
        }
        /*
        if(current.price < price){
            plugin.showError(p,"現在値より高い値段で注文はできません。購入したい場合は、成り行き買いをおこなってください /marketbuy or /buy");
            return false;
        }
*/

        return true;
    }
    public boolean orderBuy(Player p,String idOrKey,double price,int amount){

        //      まず現在価格を求める
        PriceResult current = getItemPrice(idOrKey);
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

        //      引き出し
        if(payMoney(uuid,current.id,price,amount) == false){
            return false;
        }

        boolean ret = mysql.execute("insert into order_tbl values(0,"
                +current.id +","
                +"'" +current.key +"',"

                +"'" +uuid +"',"
                +"'" +playerName +"',"
                +price+","
                +amount+",1,"

                +"'"+currentTime()+"'"
                +");");


        logTransaction(uuid,"OrderBuy",current.key,price,amount,0,"");

        updateCurrentPrice(current.id);

        return ret;
    }



    public boolean showboard(Player p,int item_id){
        //      売りデータ
        String sql = "select sum(amount),price from order_tbl where item_id = "+item_id+ " and buy = 0 group by price order by price desc ;";


        return true;
    }


    public boolean canSell(Player p,double price,int amount,PriceResult current){



        ///  値段の正当性チェック
        if(price > current.price * plugin.sellLimitRatio){
            plugin.showError(p,"現在値の" + plugin.sellLimitRatio + "倍以上の金額で売ることはできません");
//            plugin.showPrice(p);
            return false;
        }


        if(price < current.price / plugin.sellLimitRatio){
            plugin.showError(p,"現在値の1/" +plugin.sellLimitRatio +"以下の金額で売ることはできません");
  //          plugin.showPrice(p);
            return false;
        }

        return true;
    }



    public boolean orderSell(Player p,String idOrKey,double price,int amount){

        //      まず現在価格を求める
        PriceResult current = getItemPrice(idOrKey);
        if(current.result == false){
            plugin.showError(p,"このアイテムは販売されていません");
            return false;
        }


        //      値段が正当かチェック
        if(canSell(p,price,amount,current) == false){
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

        logTransaction(uuid,"OrderSell",current.key,price,amount,0,"");


        updateCurrentPrice(current.id);
        //  アイテムの売買をおこなう
        //sellExchange(p,current.id,price,amount);
        return ret;
    }
    /// アイテム価格を取得する
    public PriceResult  getItemPrice(String idOrKey) {

        int id = -1;
        try{
            id = Integer.parseInt(idOrKey);
           // p.sendMessage("id = "+id);
        }catch(Exception e){
           //     p.sendMessage("パース失敗");

        }

        String sql = "";
        //      IDが数値ではない -> key
        if(id == -1){
            sql =  "select * from item_index where item_key = '"+idOrKey+"';";
        }else{
            sql =  "select * from item_index where  id = "+id+";";
        }
        PriceResult ret = new PriceResult();
        ret.result = false;
        ret.price = 0;
        ResultSet rs = mysql.query(sql);
        if(rs == null){
            return null;
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
                ret.minPrice = rs.getDouble("min_price");
                ret.maxPrice = rs.getDouble("max_price");
                ret.result = true;
            }
        }
        catch (SQLException e)
        {
         //   plugin.showError(p,"データ取得失敗");
            Bukkit.getLogger().info("Error executing a query: " + e.getErrorCode());
            return null;
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

        String sql = "select * from item_index where base64 = '"+base64+"';";
        Bukkit.getLogger().info(sql);

        ResultSet rs = mysql.query(sql);
        if(rs == null){
            plugin.showError(p,"データ取得失敗");
            return null;
        }
        try
        {
            while(rs.next())
            {
                Bukkit.getLogger().info("result true");
                ret.id = rs.getInt("id");
                ret.key = rs.getString("item_key");
                ret.price = rs.getDouble("price");
                ret.sell = rs.getInt("sell");
                ret.buy = rs.getInt("buy");
                ret.minPrice = rs.getDouble("min_price");
                ret.maxPrice = rs.getDouble("max_price");
                ret.result = true;
            }
        }
        catch (SQLException e)
        {
            plugin.showError(p,"データ取得失敗");
            Bukkit.getLogger().info("Error executing a query: " + e.getErrorCode());
            return null;
        }

        return ret;
    }


    public boolean showItemList(Player p){

        String sql = "select * from item_index order by id;";

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


                //      ストレージに合う個数
                ItemStorage store = getItemStorage(p.getUniqueId().toString(),id);
                long amount = 0;
                if(store != null){
                    amount = store.amount;
                }

                p.sendMessage(idString + " §f§l"+key+ "(" +amount +") §e§lPrice:$" + getPriceString(price) + " §c§l売り注文数(Sell):"+sell +"/§9§l買い注文数(Sell):"+buy);
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
    public boolean registerItem(Player player, ItemStack item,String key, double initialPrice, double tick){


        String playerName = player.getName();
        String uuid = player.getUniqueId().toString();
        String itemName = item.getItemMeta().getDisplayName();
        String itemType = item.getType().toString();

        if(itemName == null){
            itemName = item.getType().toString();
        }

        boolean ret = mysql.execute("insert into item_index values(0,"
                +"'" +uuid +"',"
                +"'" +playerName +"',"
                +"'" +key +"',"
                +"'" +itemName +"',"
                +"'" +itemType +"',"
                +"" +item.getDurability() +","

                +initialPrice +","
                +initialPrice+","
                +tick+","
                +"'"+ currentTime() +"',0,0,"
                +"'"+ itemToBase64(item) +"',0,0"
                +");");

        logTransaction(uuid,"Register",key,initialPrice,0,0,"");

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
