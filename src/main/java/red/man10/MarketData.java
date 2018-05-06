package red.man10;

import net.md_5.bungee.api.chat.*;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;


public class MarketData {

    private final MarketPlugin plugin;
    MySQLManager mysql = null;
    public MarketData(MarketPlugin plugin) {
        this.plugin = plugin;
        this.mysql = new MySQLManager(plugin,"Market");
    }

    class ItemIndex{
        int id;
        String key;
        double price;
        double maxPrice;
        double minPrice;
        double bid;
        double ask;
        int sell;
        int buy;
        boolean result;
        int disabled;
        String base64;
        String getString(){
            return "ItemIndex:"+id+" "+key+" price:"+price+" bid:"+bid+" ask:"+ask + " sell:"+sell + " buy:"+buy;
        }
    }
    class ItemStorage{
        int item_id;
        String item_key;
        long amount;
    }
    class TransactionLog{
        int id;
        String item;
        int order_id;
        String uuid;
        String player;
        String action;
        double price;
        int amount;
        Date date;
        Date time;
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
        int initial_amount;

        boolean isBuy = false;
        boolean result;

        Date date;
        Date time;

        String getString(){
            return "OrderInfo:"+id+" id:"+id + " key:"+key+" price:"+price+" amount:"+amount+" player:"+player + " isBuy:"+isBuy;
        }

    }

    // $100,000 -> 10000
    double getPriceFromPriceString(String price) {

        price = price.replace(",","");
        price = price.replace("$","");
        price = price.replace("§l","");
        price = price.replace("§2","");
        price = price.replace("§4","");
        price = price.replace("§0","");
        price = price.replace("§1","");
        price = price.replace("§a","");
        price = price.replace("§c","");


        return Double.parseDouble(price);
    }

    public String getBalanceString(String uuid){

        double price = this.plugin.vault.getBalance(UUID.fromString(uuid));
        return "§e§l$"+getPriceString(price);
    }


    public OrderInfo getOrderByOrderId(int order_id){

        ArrayList<OrderInfo> orders = getOrderByQuery("select * from order_tbl where id="+order_id+";");
        if(orders.size() == 0){
            return null;
        }
        return orders.get(0);
    }

    public boolean cancelOrderByOrderId(int order_id){

        OrderInfo order = getOrderByOrderId(order_id);
        if(order == null){
            return false;
        }

        boolean ret =  mysql.execute("delete from order_tbl where id = "+order_id+";");

        if(order.isBuy == false){
            sendItemToStorage(order.uuid,order.item_id,order.amount);
        }
        if(order.isBuy == true){
            sendMoney(order.uuid,order.item_id,order.price,order.amount,null);
        }

        return ret;
    }


    public int cancelOrderList(ArrayList<OrderInfo> orders){

        int ret = 0;
        for(OrderInfo o : orders){
            if(cancelOrderByOrderId(o.id)) {
                updateCurrentPrice(o.item_id);
                ret++;
            }
        }



        return ret;
    }

    /*
        public boolean cancelOrderByOrderItemId(String uuid,int item_id){
            return mysql.execute("delete from order_tbl where item_id = "+item_id+" and uuid='"+uuid+"';");
        }
        public boolean cancelOrderByUUID(String uuid){
            return mysql.execute("delete from order_tbl where uuid = '"+uuid+"';");
        }
        public boolean cancelOrderByPlayer(String name){
            return mysql.execute("delete from order_tbl where player = '"+name+"';");
        }
    */
    public ArrayList<OrderInfo> getOrderOfUser(Player p,String uuid){
        return getOrderByQuery("select * from order_tbl where uuid = '"+uuid+"';");
    }
    public ArrayList<OrderInfo> getOrderOfItem(Player p,int item_id){
        return getOrderByQuery("select * from order_tbl where item_id = "+item_id+";");
    }
    //  アイテムのオーダー表示
    public ArrayList<OrderInfo> getOrderOfPlayerName(Player p,String name){
        return getOrderByQuery("select * from order_tbl where player = '"+name+"';");
    }

    public ArrayList<OrderInfo> getOrderByQuery(String sql){
        ArrayList<OrderInfo> ret = new ArrayList<OrderInfo>();

        ResultSet rs = mysql.query(sql);
        if(rs == null){
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
                info.initial_amount = rs.getInt("initial_amount");
                info.time = rs.getTime("datetime");
                info.date = rs.getDate("datetime");
                info.isBuy = rs.getBoolean("buy");
                ret.add(info);
            }
            rs.close();
        }
        catch (SQLException e)
        {
  //          showError(p.getUniqueId().toString(),"データ取得失敗");
            Bukkit.getLogger().info("Error executing a query: " + e.getErrorCode());
            return ret;
        }
        mysql.close();

        return ret;
    }
    void opLog(String message){
        plugin.opLog(message);
    }
    void showMessage(String uuid,String message){
        Player player = Bukkit.getPlayer(UUID.fromString(uuid));
        if(player != null){
            plugin.showMessage(player,message);
        }

    }

    void showError(String uuid,String message){
        Player player = Bukkit.getPlayer(UUID.fromString(uuid));
        if(player != null){
            plugin.showError(player,message);
        }
    }

    ///   オーダー情報を得る
    public ArrayList<OrderInfo> getOrderInfo(String uuid,int item_id, double price, boolean buy){
        return getOrderByQuery("select * from order_tbl where item_id = "+item_id+ " and buy="+buy+" and price="+price+";");
    }

    public boolean showTransaction(Player p, String uuid,int offset){

        int total = getTransactionCount(uuid);
        if(total < 0){
            showError(p.getUniqueId().toString(),"トランザクション取得失敗");
            return false;
        }
        String sql = "select * from transaction_log where uuid = '" + uuid +"' order by id desc limit 10 offset "+offset+";";
        ArrayList<TransactionLog> logs = getTransactionList(sql,p.getUniqueId().toString());
        showTransactionLog(p,logs,offset);



        if(total >= 10){
            int n = offset+ 1;
            int next = offset + logs.size();
            String linkText = String.format("------No%d- : (Total:%d)----- => §d[更に読み込む]",n,total );
            sendHoverText(p,linkText,"クリックするとさかのぼります","/mce log "+next);
        }


        return true;
    }
    public boolean showPlayerTransaction(Player p, String player,int offset){

        int total = getPlayerTransactionCount(player);
        if(total < 0){
            showError(p.getUniqueId().toString(),"トランザクション取得失敗");
            return false;
        }
        String sql = "select * from transaction_log where player = '" + player +"' order by id desc limit 10 offset "+offset+";";


        ArrayList<TransactionLog> logs = getTransactionList(sql,p.getUniqueId().toString());
        showTransactionLog(p,logs,offset);

        int n = offset+ 1;
        int next = offset + logs.size();
        String linkText = String.format("------No%d-:Total:%d----- => §d[更に読み込む]",n,total );
        sendHoverText(p,linkText,"クリックするとさかのぼります","/mce userlog "+ player+" "+next);
        return true;
    }

    void showTransactionLog(Player p, ArrayList<TransactionLog> logs, int offset){

        for(TransactionLog log : logs){
            p.sendMessage(String.format("%s %s %s §c%s §b%d個 §e$%s",log.date.toString(),log.time.toString(),log.item,log.action,log.amount,getPriceString(log.price)));
        }
    }

    ///     トランザクションの個数
    public int getTransactionCount(String uuid){
        String sql = "select count(*) from transaction_log where uuid = '" + uuid+"';";
        int count = 0;
        ResultSet rs = mysql.query(sql);
        if(rs == null){
            showError(uuid,"データ取得失敗");
            mysql.close();
            return -1;
        }
        try {
            while (rs.next()) {
                count = rs.getInt("count(*)");
            }
        }

        catch (SQLException e)
        {
            mysql.close();
            showError(uuid,"データ取得失敗");
            return -1;
        }
        mysql.close();
        return count;
    }
    public int getPlayerTransactionCount(String player){
        String sql = "select count(*) from transaction_log where player = '" + player+"';";
        int count = 0;
        ResultSet rs = mysql.query(sql);
        if(rs == null){
            mysql.close();
            return -1;
        }
        try {
            while (rs.next()) {
                count = rs.getInt("count(*)");
            }
        }

        catch (SQLException e)
        {
            mysql.close();
            return -1;
        }
        mysql.close();
        return count;
    }
    public ArrayList<TransactionLog> getTransactionList(String sql,String uuid){

        ArrayList<TransactionLog> ret = new ArrayList<TransactionLog>();

        ResultSet rs = mysql.query(sql);
        if(rs == null){
            showError(uuid,"データ取得失敗");
            return ret;
        }
        try
        {
            while(rs.next())
            {
                TransactionLog log = new TransactionLog();
                log.id = rs.getInt("id");
                log.item = rs.getString("item");
                log.uuid = rs.getString("uuid");
                log.action = rs.getString("action");
                log.amount = rs.getInt("amount");
                log.time = rs.getTime("datetime");
                log.date = rs.getDate("datetime");
                log.price = rs.getDouble("price");
                ret.add(log);
            }
            rs.close();
        }
        catch (SQLException e)
        {
            mysql.close();
            showError(uuid,"データ取得失敗");
            return ret;
        }
        mysql.close();
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
            rs.close();
        }
        catch (SQLException e)
        {
            showError(uuid,"データ取得失敗");
            mysql.close();
            return ret;
        }
        mysql.close();
        return ret;
    }

    //      履歴保存
    public boolean logHistory(int item_id,double price){
        String sql = "insert into price_history values(0,"+item_id+","+price+",'"+currentTime()+"');";
        return mysql.execute(sql);
    }


    //      測定する price/bid/ask
    public ItemIndex getRealPrice(int item_id){
        ItemIndex current = getItemPrice(item_id);
        if(current == null){
            return null;
        }
        ArrayList<OrderInfo> sellList = this.getGroupedOrderList(null,item_id,false,-1);
        ArrayList<OrderInfo> buyList = this.getGroupedOrderList(null,item_id,true,-1);

        if(sellList.size() == 0){
            return null;
        }
        int sell = 0;
        double bid = current.price;
        for(OrderInfo o : sellList){
            sell += o.amount;
            bid = o.price;  // 高い順のリストなので最後の数値が最安値
        }

        int buy = 0;
        double ask = current.price;
        boolean isFirst = true;
        for(OrderInfo o : buyList){
            buy += o.amount;
            //  高値＝ask
            if(isFirst){
                ask = o.price;
                isFirst  = false;
            }
        }
        //  現在値はbid/askの中間点とする
        double price = (bid+ask)/2;

        ItemIndex ret = new ItemIndex();
        ret.ask = ask;
        ret.bid = bid;
        ret.price = price;
        return ret;
    }


    //      現在値更新
    public boolean updateCurrentPrice(int item_id){

        //  現在価格を求める
        ItemIndex current = getItemPrice(item_id);
        if(current == null){
            return false;
        }

        ArrayList<OrderInfo> sellList = this.getGroupedOrderList(null,item_id,false,-1);
        ArrayList<OrderInfo> buyList = this.getGroupedOrderList(null,item_id,true,-1);

        if(sellList.size() == 0){
//            return false;
        }

        int sell = 0;
        double bid = current.price;
        for(OrderInfo o : sellList){
            sell += o.amount;
            bid = o.price;  // 高い順のリストなので最後の数値が最安値
        }

        int buy = 0;
        double ask = current.price;
        boolean isFirst = true;
        for(OrderInfo o : buyList){
            buy += o.amount;
            //  高値＝ask
            if(isFirst){
                ask = o.price;
                isFirst  = false;
            }
        }

        //  現在値はbid/askの中間点とする
        double price = (bid+ask)/2;
        double max = current.maxPrice;
        double min = current.minPrice;
        if(min == 0){
            min = current.price;
        }
        //Mr_IK 追加、Signにデータを通す
        plugin.sign.updateSign(current.key,price);
        plugin.sign.updateSign(String.valueOf(current.id),price);
        //   値上がり
        if((int)current.price < (int)price){
            plugin.serverMessage( "§a§l"+current.key +": $"+getPriceString(current.price) + "から$"+getPriceString(price)+"へ値上がりしました");


            //      過去最高高値更新
            if((int)current.maxPrice < (int)price){
                plugin.serverMessage( "§a§lマーケット速報!! "+current.key +": $"+getPriceString(price) + " 過去最高高値更新!!!");
                max = price;
            }
            //  履歴更新
            logHistory(item_id,price);
        }
        //  値下がり
        if((int)current.price > (int)price){
            plugin.serverMessage( "§c§l"+current.key +": $"+getPriceString(current.price) + "から$"+getPriceString(price)+"へ値下がりしました");
            //      過去最高高値更新
            if((int)current.minPrice > (int)price){
                plugin.serverMessage( "§c§lマーケット速報!! "+current.key +": $"+getPriceString(price) + " 過去最高安値更新!!!");
                min = price;
            }
            //  履歴更新
            logHistory(item_id,price);
        }

        String sql = "update item_index set price="+price+", sell="+sell+",buy="+buy+",max_price="+max+",min_price="+min+"" +
                ",bid="+bid+
                ",ask="+ask+
                ",datetime='"+currentTime()+"' where id="+item_id+";";
        boolean ret = this.mysql.execute(sql);

        return ret;
    }

    
    //      オーダー更新
    public boolean updateOrder(int id,int amount){
        String sql = "update order_tbl set amount = "+amount+" where id = "+id+";";
        boolean ret = mysql.execute(sql);
        if(ret == false){
            opLog("オーダーの更新に失敗!! order_id:"+id+ " amount:"+amount);
        }
        return ret;
    }
    //   　オーダー削除
    public boolean deleteOrder(int id){
        String sql = "delete from order_tbl where id = "+id+";";
        boolean ret =  mysql.execute(sql);
        if(ret == false){
            opLog("オーダーの削除に失敗!! order_id:"+id);
        }
        return ret;
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

        ItemIndex info =  getItemPrice(String.valueOf(item_id));
        double money =  price*amount;


        plugin.vault.deposit(player.getUniqueId(),money);
        logTransaction(uuid,"ReceivedMoney",info.key,price,amount,0,uuidBuyer);

        if(player.isOnline()){
            Player online = (Player)player;
            plugin.showMessage(online,info.key + "が"+amount+"個移動し、§e§l$"+getPriceString(money)+"受け取りました");
            return true;
        }
        return false;
    }

    //  お金支払い
    public boolean payMoney(String uuid,int item_id,double price,int amount){

        OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
        String playerName = player.getName();

        ItemIndex info =  getItemPrice(String.valueOf(item_id));
        double money =  price*amount;


       // opLog("payMoney: "+ uuid + " player:"+playerName);



        if(plugin.vault.withdraw(player.getUniqueId(),money) == false){
            Player online = (Player)player;
            plugin.showError(online,"$"+getPriceString(money)+"の引き出しに失敗しました");
            return false;
        }

//        plugin.vault.deposit()


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
            }
            rs.close();
        }
        catch (SQLException e)
        {
            Bukkit.getLogger().info("Error executing a query: " + e.getErrorCode());
            return ret;
        }


        mysql.close();
        return ret;

    }

    public boolean updateItemStorage(String uuid,int item_id,long amount){
        String sql= "update item_storage set amount="+amount+" where item_id = "+item_id+" and uuid='"+uuid+"';";
        boolean ret =  this.mysql.execute(sql);


        return ret;
    }
    public boolean insertItemStorage(String uuid,int item_id,long amount) {

        OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
        String playerName = player.getName();

        ItemIndex result =  getItemPrice(String.valueOf(item_id));
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

        ItemIndex result =  getItemPrice(String.valueOf(item_id));
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
            plugin.showMessage(online,"アイテム:" + result.key +"が"+ amount+"個ストレージに追加されました");
            plugin.showMessage(online,"現在トータル:" +total+"個");
        }


        logTransaction(uuid,"ReceivedItem",result.key,result.price,0,0,"");
        return true;
    }

    ////////////////////////////////////////////
    ///         ストレージからアイテムを引き出す
    public boolean removeItemFromStorage(String uuid,int item_id,int amount){


        OfflinePlayer player= Bukkit.getOfflinePlayer(UUID.fromString(uuid));

        ItemIndex result =  getItemPrice(String.valueOf(item_id));
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




    ///////////////////////////////////////////////
    //      買い注文を処理する ココ重要
    ///////////////////////////////////////////////
    public int executeBuyOrders(int item_id){

        ItemIndex itemPrice = getRealPrice(item_id);
        if(itemPrice == null){
            return -1;
        }

        //   売り注文の最安より高値の買い注文を列挙
        ArrayList<OrderInfo> orders = getOrderByQuery("select * from order_tbl where item_id = "+item_id+ " and buy=1 and price>="+itemPrice.bid+";");
        if(orders == null){
            return -1;
        }
        //      データがない場合はそのまま抜け
        if(orders.size() == 0){
            return 0;
        }

        int excutedTotal = 0;

        //
        for(OrderInfo o : orders){
           // opLog("買い注文実行中:" + o.getString());
            int executed = buyExchange(o.uuid,o.item_id,o.price,o.amount);
          //  opLog("購入できた個数->"+executed);
            if(executed == 0){
                continue;
            }

            // 買い注文個数 = 約定個数
            if(o.amount == executed){
                //opLog( "すべてが約定した！！ -> 買い注文削除");

                if(deleteOrder(o.id)){
                //    opLog("買い注文削除成功");
                }else{
                    opLog("なんてこったい！　買い注文削除失敗 order_id="+o.id);
                }

            }
            // 買い注文個数 > 約定個数
            if(o.amount > executed){

                int rest = o.amount - executed;
               // opLog( "一部約定 -> 買い注文を更新 "+ o.amount + " -> " + rest);

                if(updateOrder(o.id,rest)){
                  //  opLog("買い更新成功");
                }else{
                    opLog("なんてこったい！　\"買い更新失敗 order_id="+o.id);
                }
            }

            // 買い注文個数 < 約定個数
            if(o.amount < executed){
                opLog("なんてこったい！　買い注文個数 < 約定個数  なんてありえない");
            }

            excutedTotal += executed;
        }

      //  opLog("トータル:"+excutedTotal+"個の買い注文を処理した! ");
        return excutedTotal;
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
                deleteOrder(o.id);
                //    購入者へアイテムを届ける
                sendItemToStorage(o.uuid,item_id,o.amount);
                //   料金を支払
                sendMoney(uuid.toString(),item_id,price,o.amount,o.uuid);
                amount -= o.amount; //　残注文
                soldAmount += o.amount; // 購入できた個数
            //  買い注文 > 売り注文
            }else if(o.amount > amount){
                int leftAmount = o.amount - amount;
                //  購入者の注文量を減らす
                updateOrder(o.id,leftAmount);
                //    購入者へアイテムを届ける
                sendItemToStorage(o.uuid,item_id,amount);
                //   料金を支払
                sendMoney(uuid,item_id,price,amount,o.uuid);
                return amount;
                //  売り注文 == 買い注文
            }else if(o.amount == amount){
                // オーダー削除
                deleteOrder(o.id);
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
          //  opLog("buyExchange() ->売り注文を処理する-> : "+o.toString());
            //   買い注文 < 売り注文
            if(o.amount > amount){
                //  買い注文数をへらす
                int leftAmount = o.amount - amount;
                //opLog("買い注文 < 売り注文 ->買い注文数をへらす 残り注文数"+leftAmount);
                updateOrder(o.id,leftAmount);
                //    購入できたぶんアイテムを届ける
                sendItemToStorage(uuidBuyer,item_id,amount);
                //   料金を支払
                sendMoney(o.uuid,item_id,o.price,amount,uuidBuyer);
                return amount;
                //  買い注文 > 売り注文
            }else if(o.amount < amount){
               // opLog("買い注文 <  売り注文 -> 売り注文を削除する order_id"+o.id);
                // オーダー削除
                deleteOrder(o.id);
                //    購入者へアイテムを届ける
                sendItemToStorage(uuidBuyer,item_id,amount);
                //   料金を支払
                sendMoney(o.uuid,item_id,o.price,o.amount,uuidBuyer);
                retOrderAmount += o.amount;
                //  売り注文 == 買い注文
            }else if(o.amount == amount){
               // opLog("買い注文 > 売り注文 -> 売り注文を削除する order_id"+o.id);
                // オーダー削除
                deleteOrder(o.id);
                //    購入者へアイテムを届ける
                sendItemToStorage(uuidBuyer,item_id,amount);
                //   料金を支払
                sendMoney(o.uuid,item_id,o.price,amount,uuidBuyer);
                return amount;
            }
        }

        return retOrderAmount;
    }


    public boolean canBuy(Player p,double price,int amount ,ItemIndex current) {

        if(price < 1){
            showError(p.getUniqueId().toString(), "1円未満の注文はできない");
            return false;
        }

        if(amount <= 0) {
            showError(p.getUniqueId().toString(), "0個以下の注文はできない");
            return false;
        }


        double bal = plugin.vault.getBalance(p.getUniqueId());
        if(bal < price*amount){
            plugin.showError(p,"残額がたりません! 必要金額:$"+getPriceString(price*amount) +" 残額:$"+ getPriceString(bal));
            return false;
        }
        return true;
    }


    ///
    ///
    ///
    public int marketSell(String uuid,int item_id,int amount) {

        if(amount <= 0) {
            showError(uuid, "0個以下の注文はできない");
            return 0;
        }


        //  安い順の売り注文を列挙
        ArrayList<OrderInfo> sellOrders = this.getOrderByQuery("select * from order_tbl where item_id = " + item_id+" and buy = 1 order by price desc,id asc");


        ItemStorage storage = getItemStorage(uuid,item_id);
        if(storage == null){
            showError(uuid,"アイテムを所有していない!!");
            return 0;
        }
        if(storage.amount < amount){
            showError(uuid,"あなたの"+storage.item_key + "の所有個数は"+storage.amount+"なのに"+amount+"個を売ることはできない");
            return 0;
        }

        int totalAmount = 0;
        for(OrderInfo o : sellOrders){

           // opLog("成売り"+o.getString());

            // 　買い注文 > 売り注文
            if(o.amount > amount ){
                sendMoney(uuid,item_id,o.price,amount,o.uuid);
                sendItemToStorage(o.uuid,o.item_id,amount);
                //  売れるだけうり、注文を修正する
                int rest = o.amount - amount;
                totalAmount += amount;
                updateOrder(o.id,rest);

                long newAmount = storage.amount - amount;
                showMessage(uuid,"アイテム個数が"+newAmount+"になりました");
                updateItemStorage(uuid,item_id,newAmount);

                return totalAmount;
            }
            // 　買い注文 < 売り注文
            else if(o.amount < amount){
                sendMoney(uuid,item_id,o.price,o.amount,o.uuid);
                sendItemToStorage(o.uuid,o.item_id,o.amount);
                deleteOrder(o.id);
                totalAmount += o.amount;
                amount -= o.amount;


                long newAmount = storage.amount - o.amount;
                showMessage(uuid,"アイテム個数が"+newAmount+"になりました");
                updateItemStorage(uuid,item_id,newAmount);


            }
            //     同量
            else if(o.amount == amount){
                totalAmount += amount;
                deleteOrder(o.id);
                sendMoney(uuid,o.item_id,o.price,amount,o.uuid);
                sendItemToStorage(o.uuid,o.item_id,o.amount);


                long newAmount = storage.amount - o.amount;
                showMessage(uuid,"アイテム個数が"+newAmount+"になりました");
                updateItemStorage(uuid,item_id,newAmount);
                return totalAmount;
            }
        }

        return totalAmount;
    }


    ///
    public int marketSell(String uuid,String idOrKey,int amount){

        if(amount == 0){
            showError(uuid,"0個以下の注文はできない");
            return 0;
        }

        ItemIndex current = getItemPrice(idOrKey);
        if(current == null){
            showError(uuid,"このアイテムは売却できません");
            return -1;
        }

        int ret = marketSell(uuid,current.id,amount);
        if(ret == -1) {
            return -1;
        }

        updateCurrentPrice(current.id);
        return ret;
    }

    ///
    public int marketBuy(String uuid,int item_id,int amount) {

        if(amount <= 0) {
            showError(uuid, "0個以下の注文はできない");
            return 0;
        }

            //  安い順の売り注文を列挙
        ArrayList<OrderInfo> sellOrders = this.getOrderByQuery("select * from order_tbl where item_id = " + item_id+" and buy = 0 order by price,id desc");

        int totalAmount = 0;
        for(OrderInfo o : sellOrders){
            //opLog("成買い"+o.getString());
            // 　　売り注文 > 買い注文
            if(o.amount > amount ){
               // opLog("marketBuy>:"+o.player +":amount:"+o.amount +" price:"+o.price);
                if(this.payMoney(uuid,o.item_id,o.price,amount)){
                    //      お金送信
                    sendMoney(o.uuid,o.item_id,o.price,amount,uuid);
                    sendItemToStorage(uuid,o.item_id,amount);
                    totalAmount += amount;
                    int rest = o.amount - amount;
                    updateOrder(o.id,rest);
                    return totalAmount;
                }else{
                    opLog("金がたらないので注文キャンセル");
                }
                return totalAmount;
            }
            //   売り注文　＜　買い注文　
            else if(o.amount < amount){
                //opLog("marketBuy<:"+o.player +":amount:"+o.amount +" price:"+o.price);
                if(this.payMoney(uuid,o.item_id,o.price,o.amount)){
                    sendMoney(o.uuid,o.item_id,o.price,o.amount,uuid);

                    sendItemToStorage(uuid,o.item_id,o.amount);
                    totalAmount += o.amount;
                    amount -= o.amount;
                    deleteOrder(o.id);
                }else{
                    opLog("金がたらないので注文キャンセル");
                    return totalAmount;
                }
            }
            //   同量
            else if(o.amount == amount){
               // opLog("marketBuy=:"+o.player +":amount:"+o.amount +" price:"+o.price);
                if(this.payMoney(uuid,o.item_id,o.price,amount)){
                    sendMoney(o.uuid,o.item_id,o.price,amount,uuid);

                    sendItemToStorage(uuid,o.item_id,amount);
                    totalAmount += amount;
                    deleteOrder(o.id);
                    return totalAmount;
                }else{
                    opLog("金がたらないので注文キャンセル");
                }
                return totalAmount;
            }
        }

        return totalAmount;
    }


    public int marketBuy(String uuid,String idOrKey,int amount){

        if(amount <= 0) {
            showError(uuid, "0個以下の注文はできない");
            return 0;
        }

        ItemIndex current = getItemPrice(idOrKey);
        if(current == null){
            showError(uuid,"このアイテムは販売されていません");
            return -1;
        }

        int ret = marketBuy(uuid,current.id,amount);
        if(ret == -1) {
            return -1;
        }
        updateCurrentPrice(current.id);

        return ret;
    }


    public boolean orderBuy(Player p,String idOrKey,double price,int amount){

        //      まず現在価格を求める
        ItemIndex current = getItemPrice(idOrKey);
        if(current == null){
            plugin.showError(p,"このアイテムは販売されていません");
            return false;
        }
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
                +amount+","
                +amount
                +",1,"
                +"'"+currentTime()+"'"
                +");");

        logTransaction(uuid,"OrderBuy",current.key,price,amount,0,"");

        //   買い注文を処理する
        executeBuyOrders(current.id);
        //  現在値を更新
        updateCurrentPrice(current.id);

        return ret;
    }

    //
    //
    public boolean canSell(Player p,double price,int amount,ItemIndex item){
        if(price < 1){
            showError(p.getUniqueId().toString(), "1円未満の注文はできない");
            return false;
        }

        if(amount <= 0){
            plugin.showError(p,"0個以下の注文はできない");
            return false;
        }

        //      自分のストレージをチェック
        ItemStorage storage = getItemStorage(p.getUniqueId().toString(),item.id);
        if(storage == null){
            plugin.showError(p,"このアイテムを所有していません");
            return false;
        }

        if(storage.amount < amount){
            plugin.showError(p,"アイテムの個数がたりません");
            return false;
        }


        ///  値段の正当性チェック
        if(price > item.price * plugin.sellLimitRatio){
            plugin.showError(p,"現在値の" + plugin.sellLimitRatio + "倍以上の金額で売ることはできません");
            return false;
        }

        if(price < item.price / plugin.sellLimitRatio){
            plugin.showError(p,"現在値の1/" +plugin.sellLimitRatio +"以下の金額で売ることはできません");
            return false;
        }
        return true;
    }

    //
    //
    public boolean orderSell(Player p,String idOrKey,double price,int amount){

        //      まず現在価格を求める
        ItemIndex current = getItemPrice(idOrKey);
        if(current == null){
            plugin.showError(p,"このアイテムは販売されていません");
            return false;
        }
        if(current.result == false){
            plugin.showError(p,"このアイテムは販売されていません");
            return false;
        }

        //      値段が正当かチェック
        if(canSell(p,price,amount,current) == false){
            return false;
        }

        // 自分のアイテム
        ItemStorage storage = getItemStorage(p.getUniqueId().toString(),current.id);
        //p.sendMessage("所有個数"+storage.amount + " 売り子数"+amount);
        long left = storage.amount - amount;
        //p.sendMessage("left:"+left);


        if(updateItemStorage(p.getUniqueId().toString(),current.id,left) == false){
            plugin.showError(p,"自分のアイテムをへらせませんでした");
            return false;
        }

        p.sendMessage(current.key +"が"+left+"個になりました");

        String playerName = p.getName();
        String uuid = p.getUniqueId().toString();
        boolean ret = mysql.execute("insert into order_tbl values(0,"
                +current.id +","
                +"'" +current.key +"',"

                +"'" +uuid +"',"
                +"'" +playerName +"',"
                +price+","
                +amount+","
                +amount
                +",0,"

                +"'"+currentTime()+"'"
                +");");

        logTransaction(uuid,"OrderSell",current.key,price,amount,0,"");


        executeBuyOrders(current.id);

        updateCurrentPrice(current.id);
        //  アイテムの売買をおこなう
        //sellExchange(p,current.id,price,amount);
        return ret;
    }



    //      アイテムインデクスリスト取得
    public ArrayList<ItemIndex> getItemIndexList(String sql){
        ArrayList<ItemIndex> ret = new ArrayList<ItemIndex>();

        ResultSet rs = mysql.query(sql);
        if(rs == null){
            return null;
        }
        try
        {
            while(rs.next())
            {
                ItemIndex item = new ItemIndex();
                item.result = false;
                item.id = rs.getInt("id");
                item.key = rs.getString("item_key");
                item.price = rs.getDouble("price");
                item.sell = rs.getInt("sell");
                item.buy = rs.getInt("buy");
                item.minPrice = rs.getDouble("min_price");
                item.maxPrice = rs.getDouble("max_price");
                item.bid = rs.getDouble("bid");
                item.ask = rs.getDouble("ask");
                item.disabled = rs.getInt("disabled");
                item.base64 = rs.getString("base64");
                item.result = true;
                ret.add(item);
            }
            rs.close();
        }
        catch (SQLException e)
        {
            //   plugin.showError(p,"データ取得失敗");
            Bukkit.getLogger().info("Error executing a query: " + e.getErrorCode());
            return null;
        }
        mysql.close();
        return ret;
    }

    ///  現在値を得る
    public ItemIndex  getItemPrice(int item_id){
        ArrayList<ItemIndex>  list = getItemIndexList("select * from item_index where  id = "+item_id+" and disabled = 0;");
        if(list == null){
            return null;
        }
        if(list.size() == 0){
            return null;
        }
        return list.get(0);
    }

    ///  アイテム価格を取得する
    public ItemIndex  getItemPrice(String idOrKey) {

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

        ArrayList<ItemIndex>  list = getItemIndexList(sql);
        if(list == null){
            return null;
        }
        if(list.size() == 0){
            return null;
        }
        return list.get(0);
    }

    /// アイテム価格を取得する
    public ItemIndex  getItemPrice(Player p,ItemStack item){
        ItemStack newItem = new ItemStack(item);
        newItem.setAmount(1);
        String base64 = itemToBase64(newItem);

        ItemIndex ret = new ItemIndex();
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
                ret.base64 = rs.getString("base64");
                ret.disabled = rs.getInt("disabled");
                ret.result = true;
            }
            rs.close();
        }
        catch (SQLException e)
        {
            plugin.showError(p,"データ取得失敗");
            Bukkit.getLogger().info("Error executing a query: " + e.getErrorCode());
            return null;
        }
        mysql.close();
        return ret;
    }


    public boolean showItemList(Player p,int targetPageNo){

        String uuid = p.getUniqueId().toString();

        p.sendMessage("§b§l==================================================");
        p.sendMessage(" §d§lM§7§lan10 §f§lC§7§lentral §a§lE§7§lxchange / man10中央取引所 §b§l/MCE");
        p.sendMessage("§b§l==================================================");


        ArrayList<ItemIndex> items = getItemIndexList("select * from item_index where disabled = 0 order by id;");

        int pageLimit = 10;
        int maxpage = items.size() / pageLimit;

        double totalEstimated = 0;
        int index = 0;
        for(ItemIndex item : items){

            //      ストレージある
            ItemStorage store = getItemStorage(uuid,item.id);
            long amount = 0;
            if(store != null){
                amount = store.amount;
            }

            double estimated = amount * item.price;
            totalEstimated += estimated;

            int pageNo = index / pageLimit;


            //      ターゲットページなら表示
            if(pageNo == targetPageNo){
                String text = "§1ID:"+item.id  + " §f"+item.key + " §b"+getPriceString(store.amount) + "個§fx§e$" + getPriceString(item.price) +" §6評価額:$"+getPriceString(estimated) + " §a§n[注文]";
                String hover = "クリックすると現在の注文状況を表示";
                sendHoverText(p, text,hover,"/mce price "+item.key);
            }


            index ++;

        }

        int curPage  = targetPageNo + 1;
        int nextPage = targetPageNo + 1;
        int prevPage = targetPageNo - 1;

        //////////////////////////////////////////
        //   クリックイベントを作成する

        BaseComponent[] pageLink = null;
        String pageText = "§f§l "+curPage + "/"+maxpage+" ";
        BaseComponent[] nextLink = null;
        BaseComponent[] prevLink = null;

        String br = "§f§l---------";
        //      前に戻るページ
        if( targetPageNo > 0 ){
            ClickEvent clickPrev = null;
            clickPrev = new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/mce menu "+prevPage);
            prevLink = new ComponentBuilder("§b§l §n[<<]").event(clickPrev).create();
        }
        //      次の進むページ
        if( curPage < maxpage ){
            ClickEvent clickNext = null;
            clickNext = new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/mce menu "+nextPage);
            nextLink = new ComponentBuilder("§b§l §n[>>]").event(clickNext).create();
        }


        if(prevLink == null && nextLink!= null){
            pageLink = new ComponentBuilder(br + pageText).append(nextLink).append(br).create();
        }

        if(prevLink != null && nextLink!= null){
            pageLink = new ComponentBuilder(br).append(prevLink).append(pageText). append(nextLink).append(br).create();

        }
        if(prevLink != null && nextLink==null){
            pageLink = new ComponentBuilder(br).append(prevLink).append(pageText).append(br).create();

        }
        p.spigot().sendMessage(pageLink);


        p.sendMessage(" §f§lあなたの所持金:"+getBalanceString(uuid) + " §6§lアイテム評価額:$"+getPriceString(totalEstimated));


        //   注文管理
        ArrayList<MarketData.OrderInfo> orders =  getOrderOfUser(p,uuid);
        if(orders != null){
            if(orders.size() > 0){
                sendHoverText(p, " §b§l"+orders.size()+"件§fの注文があります " +"=> §d§n[注文管理]","注文をキャンセルするにはクリックします /mce order","/mce order");
            }
        }

        sendHoverText(p, " 過去の注文を参照する => §1§n[注文履歴]","注文の履歴を表示します /mce log","/mce log");
        return true;


/*

        double totalPrice = 0;
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
                double bid = rs.getDouble("bid");
                double ask = rs.getDouble("ask");

                int sell = rs.getInt("sell");
                int buy = rs.getInt("buy");


                //      ストレージに合う個数
                ItemStorage store = getItemStorage(p.getUniqueId().toString(),id);
                long amount = 0;
                if(store != null){
                    amount = store.amount;
                }

                double estimated = amount * price;
                totalPrice += estimated;

                showMessage(p.getUniqueId().toString(),"§f§l"+idString +" " +key+ ":$"+ getPriceString(price) + " §b§l所有個数:"+getPriceString(amount) + " §e§l評価額:"+getPriceString(estimated) + " §c§l売"+sell +"個/§9§l買:"+buy+"個");
//                p.sendMessage(idString + " §f§l"+key+ "(" +amount +") §e§lPrice:$" + getPriceString(price) + " §c§l売り注文数(Sell):"+sell +"/§9§l買い注文数(Sell):"+buy);
            }
            rs.close();
            showMessage(p.getUniqueId().toString(),"§e§l 現在のアイテム資産評価額 $"+getPriceString(totalPrice));
        }
        catch (SQLException e)
        {
            plugin.showError(p,"データ取得失敗");
            Bukkit.getLogger().info("Error executing a query: " + e.getErrorCode());
            return false;
        }
        mysql.close();
        */
       // return true;
    }


    public String getPriceString(double price){
        return String.format("%,d",(int)(price));
    }
    //1,301 などを1301.0に変換する
    public Double getPricedouble(String price){
        Double db = 0.0;
        price = price.replace(",","");
        price = price+".0";
        try {
            db = Double.parseDouble(price);
        }catch (NumberFormatException e){
            return null;
        }
        return db;
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
                +"'"+ itemToBase64(item) +"',0,0,"
                +initialPrice +","
                +initialPrice

                +",0);");

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



    ////////////////////////////////////////////////////////////////////////////////////////////
    //  マインクラフトチャットに、ホバーテキストや、クリックコマンドを設定する関数
    // [例1] sendHoverText(player,"ここをクリック",null,"/say おはまん");
    // [例2] sendHoverText(player,"カーソルをあわせて","ヘルプメッセージとか",null);
    // [例3] sendHoverText(player,"カーソルをあわせてクリック","ヘルプメッセージとか","/say おはまん");
    public static void sendHoverText(Player p,String text,String hoverText,String command){
        //////////////////////////////////////////
        //      ホバーテキストとイベントを作成する
        HoverEvent hoverEvent = null;
        if(hoverText != null){
            BaseComponent[] hover = new ComponentBuilder(hoverText).create();
            hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover);
        }

        //////////////////////////////////////////
        //   クリックイベントを作成する
        ClickEvent clickEvent = null;
        if(command != null){
            clickEvent = new ClickEvent(ClickEvent.Action.RUN_COMMAND,command);
        }

        BaseComponent[] message = new ComponentBuilder(text).event(hoverEvent).event(clickEvent). create();
        p.spigot().sendMessage(message);
    }

    //  マインクラフトチャットに、ホバーテキストや、クリックコマンドサジェストを設定する
    public static void sendSuggestCommand(Player p,String text,String hoverText,String command){

        //////////////////////////////////////////
        //      ホバーテキストとイベントを作成する
        HoverEvent hoverEvent = null;
        if(hoverText != null){
            BaseComponent[] hover = new ComponentBuilder(hoverText).create();
            hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover);
        }

        //////////////////////////////////////////
        //   クリックイベントを作成する
        ClickEvent clickEvent = null;
        if(command != null){
            clickEvent = new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND ,command);
        }

        BaseComponent[] message = new ComponentBuilder(text). event(hoverEvent).event(clickEvent). create();
        p.spigot().sendMessage(message);
    }

}
