package red.man10;


import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.block.Sign;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.plugin.java.JavaPlugin;
import red.man10.man10bank.BankAPI;

import java.util.ArrayList;
import java.util.HashMap;

import static red.man10.MarketCommand.checkPermission;


public final class MarketPlugin extends JavaPlugin implements Listener {



    //      価格帯マップ
    static public  HashMap<Integer,MarketData.ItemIndex> priceMap = new HashMap<>();

    String  prefix = "§8§l[§4§lm§2§lMarket§8§l]";

    MarketVault vault = null;


    public double buyLimitRatio = 10;
    public double sellLimitRatio = 10;
    public boolean isMarketOpen = false;
    public String csvPath = null;

    public BankAPI bankAPI = new BankAPI(this);


    public void updateCurrentPriceListOnBackground(Player p){


        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {

                p.sendMessage("現在値更新中");
                MarketData data = new MarketData(this);
                data.updatePriceAll(p);
                p.sendMessage("現在値更新終了");


            } catch (Exception e) {
                Bukkit.getLogger().info(e.getMessage());
                System.out.println(e.getMessage());
            }
        });
    }


    public void marketOpen(boolean flag) {


        getConfig().set("marketOpen",flag);
        saveConfig();
        loadConfig();

    }

    ///////////////////////////////
    //      成り行きアイテム購入
    synchronized public void itemBuy(Player p, String target, int amount){

        MarketData data = new MarketData(this);

        if(!checkPermission(p,Settings.itemBuyPermission)){
            return;
        }

        if (checkMarketClosed(p)) {
            return;
        }


        if(amount > 64){
            showError(p,"64個以上のアイテムを購入することはできません");
            return;
        }

        int ret = data.marketBuy(p.getUniqueId().toString(),target,amount);
        if(ret == -1){
            showError(p,"エラーが発生しました");
            return;
        }

        if(ret == 0){
            showMessage(p,"購入できませんでした");
            return;
        }

        showMessage(p,"購入成功:"+ret+"個購入できました");


        MarketData.ItemIndex item = data.getItemPrice(target);

        if(item == null){
            showMessage(p,"アイテムが取得できない");
            return;
        }



        //      アイテム個数をへらす
        String uuid = p.getUniqueId().toString();


        //      アイテムを減らせた
        if(!data.itemBank.reduceItem(uuid, item.id, ret)){
            opLog(p.getName()+"のアイテムを減らすことに失敗した");
            return;
        }

        ItemStack itemStack = MarketData.itemFromBase64(item.base64);
        if(itemStack == null){
            showError(p,"アイテム生成に失敗");
            return;
        }

        itemStack.setAmount(ret);

        p.getInventory().addItem(itemStack);


        data.logTransaction(uuid,"ItemBuy",""+item.id,item.price,ret,0,null);

    }




    public static int getAmount(Player player, ItemStack itemCheck)
    {
        PlayerInventory inventory = player.getInventory();

//        ItemStack[] items = inventory.getContents();
        ItemStack[] items = inventory.getStorageContents();

        int has = 0;
        for (ItemStack item : items)
        {
            if ((item != null) && item.getType() == itemCheck.getType() && (item.getAmount() > 0))
            {
                if (itemCheck instanceof Damageable){
                    if(((Damageable) item).getDamage() != ((Damageable) itemCheck).getDamage()){
                        Bukkit.getLogger().info("getDurability error");
                        continue;
                    }
                }


                //      item meta がある
                if(itemCheck.getItemMeta() != null){
                    if(item.getItemMeta() != null){


                        if(itemCheck.getItemMeta().toString().equals( item.getItemMeta().toString() )){

                            has += item.getAmount();
                        }
                    }

                }else{


                     has += item.getAmount();
                }

            }
        }
        return has;
    }


    ///////////////////////////////
    //     アイテム成り行き売り
    synchronized public void itemSell(Player p, String target, int amount){


        if(!checkPermission(p,Settings.itemSellPermission)){
            return;
        }


        if (checkMarketClosed(p)) {
            return;
        }


        if(p.getGameMode() != GameMode.SURVIVAL){
            showError(p,"サバイバルモード以外でのアイテム売りは禁止されています");
            return;
        }

        MarketData data = new MarketData(this);

        MarketData.ItemIndex itemIndex = data.getItemPrice(target);
        if(itemIndex == null){
            showError(p,"対象アイテムが見つからない");
            return;
        }

        //  買い注文チェック
        if(itemIndex.buy < amount){
            showError(p,"アイテムの買い注文個数は"+itemIndex.buy+"なので"+amount+"個の売りはできません");
            return;
        }

        if(amount > 64){
            showError(p,"64個以上のアイテムを売ることはできません");
            return;
        }

        ItemStack item = MarketData.itemFromBase64(itemIndex.base64);

        int has = getAmount(p,item);
        if(has <= 0){
            showError(p,"アイテムをもっていない");
            return;
        }
        showMessage(p,"インベントリに"+has+"個の"+itemIndex.key+"を持っています");
        if(has < amount){
            showError(p,"インベントリのアイテムが足らない");
            return;
        }
        //      アイテムをへらす
        assert item != null;
        item.setAmount(amount);
        p.getInventory().removeItem(item);


        //ItemBank.ItemStorage store = bank.getItemStorage(uuid,result.id);
        //      アイテムをストレージに入れる
        String uuid = p.getUniqueId().toString();
        //this.itemBank.sendItemToStorage(uuid,itemIndex.id,amount);

        if(!data.itemBank.addItem(uuid, itemIndex.id, amount)){

            showError(p,"アイテムバンクへの登録に失敗しました");
            opLog(p.getName()+"のItemSell時のアイテムバンク登録失敗");
            return;
        }



        int ret = data.marketSell(p.getUniqueId().toString(),target,amount);
        if(ret == -1){
            showError(p,"エラーが発生しました");
            return;
        }


        if(ret == 0){
            showMessage(p,"売却できませんでした");
            return;
        }

        showMessage(p,"売却成功:"+ret+"個売却できました");

    }



    ///////////////////////////////
    //      成り行き購入
    synchronized public void marketBuy(Player p, String target, int amount){


        MarketData data = new MarketData(this);

        if (checkMarketClosed(p)) {
            return;
        }


        int ret = data.marketBuy(p.getUniqueId().toString(),target,amount);
        if(ret == -1){
            showError(p,"§c§lエラーが発生しました");
            return;
        }


        if(ret == 0){
            showMessage(p,"§c§l購入できませんでした");
            return;
        }

        showMessage(p,"§f§l購入成功:"+Utility.getColoredItemString(ret)+"購入できました");

    }
    ///////////////////////////////
    //      成り行き売り
    synchronized public void marketSell(Player p, String target, int amount){


        MarketData data = new MarketData(this);

        if (checkMarketClosed(p)) {
            return;
        }


        int ret = data.marketSell(p.getUniqueId().toString(),target,amount);
        if(ret == -1){
            showError(p,"§c§lエラーが発生しました");
            return;
        }


        if(ret == 0){
            showMessage(p,"§c§l売却できませんでした");
            return;
        }

        showMessage(p,"§f§l売却成功:"+Utility.getColoredItemString(ret)+"売却できました");

    }


    //      すべての注文をキャンセルする
    synchronized public void cancelAll(Player p, String playerName) {

        ArrayList<MarketData.OrderInfo> orders;
        MarketData data = new MarketData(this);

        String uuid;
        if(playerName == null){
            uuid = p.getUniqueId().toString();
            orders = data.getOrderOfUser(p,uuid);
        }else{
            orders = data.getOrderOfPlayerName(p,playerName);
        }

        int ret = data.cancelOrderList(orders);
        if(ret == -1){
            return;
        }

        showMessage(p,""+ret+"件の注文をキャンセルしました");
    }



    public void updatePrice(Player p, String idOrKey) {

        MarketData data = new MarketData(this);

        MarketData.ItemIndex item = data.getItemPrice(idOrKey);
        if(item == null){
            showError(p,"そのアイテムはみつからない");
            return;
        }

        showMessage(p,item.key+"の現在値を調整中");
        data.updateCurrentPrice(item.id);

    }

    boolean checkMarketClosed(Player p){

        //      マーケット
        if(!isMarketOpen){


            if(p.hasPermission(Settings.adminPermission)){
                showMessage(p,"§4マーケットはクローズしていますか、admin権限があるためアクセス可能です");
                return false;
            }


            showError(p,Settings.closedMessage);
            return true;
        }

        return false;
    }


    //  注文キャンセル
    synchronized public void cancelOrder(Player p, String target) {



        if (checkMarketClosed(p)) {
            return;
        }

        int orderNo = -1;
        try{
            orderNo = Integer.parseInt(target);
        }catch (Exception ignored){

        }

        if(orderNo == -1){
            return;
        }

        MarketData data = new MarketData(this);

        MarketData.OrderInfo order = data.getOrderByOrderId(orderNo);
        if(order == null){
            showError(p,"そのIDはみつからない");
            return;
        }

        //      自分の注文ではない
        if(!order.uuid.equalsIgnoreCase(p.getUniqueId().toString())){

            if(!p.hasPermission("red.man10.cancelother")){
                showError(p,"自分以外の注文をキャンセルする権限がない");
                return;
            }
        }


        //
        if(data.cancelOrderByOrderId(orderNo)){
           data.updateCurrentPrice(order.item_id);

           Bukkit.getScheduler().runTask(this, () -> {
               p.sendMessage("注文ID:" + order.id + "をキャンセルしました");
               p.chat("/mce order");
           });
        }

    }


    void showMainMenuLink(Player p){
        Utility.sendHoverText(p," §f§lメインメニューへ戻る => §b§l§n[メインメニュー]","クリックするとメインメニューへ戻ります /mce","/mce menu");
    }

    //  注文表示
    public boolean showOrder(Player p,String target){
        MarketData data = new MarketData(this);


        //  引数なし->自分の注文
        if(target == null){
            return showOrderOfUser(data,p,p.getUniqueId().toString());
        }
        //
        MarketData.ItemIndex pr = data.getItemPrice(target);
        if(pr != null){
            if(pr.key != null){
                return showOrderOfItem(data,p,pr.id);
            }
        }

        Player player = Bukkit.getPlayer(target);
        if(player == null) {
            return showOrderOfUserName(data,p,target);
        }
        return showOrderOfUser(data,p,player.getUniqueId().toString());
    }

    public boolean showOrderOfUser(MarketData data,Player p,String uuid){
        ArrayList<MarketData.OrderInfo> orders = data.getOrderOfUser(p,uuid);
        showOrders(p,orders);
        return true;
    }

    public boolean showOrderOfUserName(MarketData data,Player p,String name){
        ArrayList<MarketData.OrderInfo> orders = data.getOrderOfPlayerName(p,name);
        showOrders(p,orders);
        return true;
    }
    public boolean showOrderOfItem(MarketData data,Player p,int item_id){
        ArrayList<MarketData.OrderInfo> orders = data.getOrderOfItem(p,item_id);
        showOrders(p,orders);
        return true;
    }

    public void sendClickableMessage(Player player, String strmessage, String url) {
        TextComponent message = new TextComponent(strmessage);
        message.setClickEvent( new ClickEvent(ClickEvent.Action.OPEN_URL, url));
        player.sendMessage(message);
    }

    public void showOrders(Player p, ArrayList<MarketData.OrderInfo> orders){

        MarketData data = new MarketData(this);

        sendClickableMessage(p,"xxx","http://man10.red");
        p.sendMessage("§d§l---------[注文リスト]-----------");
        int count = 0;
        for(MarketData.OrderInfo o : orders){

            String buyOrSell = "§a売";
            if(o.isBuy){
                buyOrSell = "§9買";
            }
           // String sttrDate = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(o.datetime);
            String price = "$"+data.getPriceString(o.price);

            //   注文テキストを作る
            String order = String.format("§f§l§n注文ID%d:%3s:%s:%s:%s/%7s個 %s:%s §f§l§n[X]"
                ,o.id,buyOrSell,o.key,o.player,price,data.getPriceString(o.amount),o.date.toString(),o.time.toString()
            );

            Utility.sendHoverText(p, order,"クリックすると注文がキャンセルされます","/mce cancel "+o.id);

            count++;
        }
        if(count == 0){
            p.sendMessage("注文がありません");
           // MarketData.sendHoverText(p, "§f§l§n てすとおおおおお ","クリックすると全ての注文がキャンセルされます","/test ");
        }else{
            p.sendMessage("§b§l"+count+"§f件の注文があります");
            Utility.sendHoverText(p, "         §c§n[全ての注文をキャンセル/Cancel All]","クリックすると全ての注文がキャンセルされます","/mce cancelall");
        }
        showMainMenuLink(p);
        data.close();

    }
    ///////////////////////////////
    //         板情報を表示する
    public void showOrderBook(Player p, int item_id){
        MarketData data = new MarketData(this);

        MarketData.ItemIndex item = data.getItemPrice(String.valueOf(item_id));
        String uuid = p.getUniqueId().toString();

        if(item == null) {
            showError(p,"このアイテムは売買できません");
            return;
        }

        ArrayList<MarketData.OrderInfo> sellList = data.getGroupedOrderList(uuid,item_id,false,-1);
        ArrayList<MarketData.OrderInfo> buyList = data.getGroupedOrderList(uuid,item_id,true,-1);

        p.sendMessage("");
        p.sendMessage("§a§l==============[ 注文状況: "+ item.key+" ]================");
        p.sendMessage(" §b§l売数量    値段   買数量");


        for(int i = 0;i < sellList.size();i++){
            MarketData.OrderInfo o = sellList.get(i);
            String color ="§e§l";
            if(i ==  sellList.size() -1){
                color  = "§a§l";
            }
            p.sendMessage(String.format("%s%7d  %7s",color,o.amount,data.getPriceString(o.price)));
        }
        if(sellList.size() == 0){
            p.sendMessage("§9売り注文がありません");
        }
        for(int i = 0;i < buyList.size();i++){
            MarketData.OrderInfo o = buyList.get(i);
            String color ="§e§l";
            if(i == 0){
                color  = "§c§l";
            }
            p.sendMessage(String.format("%s         %7s  %7d",color,data.getPriceString(o.price),o.amount));
        }
        if(buyList.size() == 0){
            p.sendMessage("§9買い注文がありません");
        }
        data.close();

    }

    ///  売り注文を出す
    synchronized public void orderBuy(Player p, String idOrKey, double price, int amount){

        if (checkMarketClosed(p)) {
            return;
        }
        MarketData data = new MarketData(this);

        if(data.orderBuy(p,idOrKey,price,amount)){
            showMessage(p,"買い注文成功 $"+ data.getPriceString(price) + "/"+amount+"個" );
            return;
        }

        showError(p,"買い注文失敗");
    }



    //      アイテムの値段を表示
    public void showPrice(Player p, String idOrKey){


        MarketData data = new MarketData(this);
        MarketData.ItemIndex item;

        //  IDが指定されてないければ手持ちのアイテム
        if(idOrKey == null){
            ItemStack itemstack = p.getInventory().getItemInMainHand();

            item = data.getItemPrice(p,itemstack);
            if(item == null){
                showError(p,"取得失敗");
                return;
            }
        }

        item = data.getItemPrice(idOrKey);
        if(item == null){
            showError(p,"指定されたアイテムデータは取得できません");
            return;
        }

        //      板表示
        showOrderBook(p,item.id);

        long itemCount = 0;
        double itemPrice = 0;
        ItemBank.ItemStorage storage = data.itemBank.getItemStorage(p.getUniqueId().toString(),item.id);
        if(storage != null){
            itemCount = storage.amount;
            itemPrice = itemCount * item.price;
        }

        //  現在所有個数　

        Utility.sendHoverText(p,  "あなたは"+itemCount+"個の"+item.key+"を所有/所持金:" + data.getBalanceString(p.getUniqueId().toString()),"アイテム評価額:$"+data.getPriceString(itemPrice)+ "\n 販売するにはアイテムボックスにアイテムを登録してください /mitembox /mib","/mib");

        if(item.sell >0){
            Utility.sendSuggestCommand(p,"§2現在売注文数:"+item.sell+"個 $"+ data.getPriceString(item.bid)+"/1個〜 §f=> §a§l§n[成り行き買い]" ,"指定した個数を金額が安い順に買います\n/mce marketbuy(mb) "+item.key +" [買いたい個数] 最大:"+item.sell,"/mce marketbuy "+item.key + " ");
        }

        if(itemCount > 0){
            if(item.buy > 0){
                Utility.sendSuggestCommand(p, "§e現在買注文数:"+item.buy+ "個 $"+data.getPriceString(item.ask)+"/1個〜§f=> §c§l§n[成り行き売り]" ,"指定した個数を金額が高い順に売ります\n/mce marketsell(ms) "+item.key +" [売りたい個数] 最大:"+itemCount,"/mce marketsell "+item.key + " ");
            }
        }


        Utility.sendSuggestCommand(p, "金額を指定して買い注文 [1:金額][2:個数] => §a§l§n[指し値買い注文]" ,"指定した金額、個数の買い注文をします\n§a§l/mce orderbuy(ob) "+item.key +" [金額] [買い個数] §e\n※金額が安すぎる場合、買えない場合があります。\n注文キャンセルすると返金されます","/mce orderbuy "+item.key + " ");
        if(itemCount > 0){
            Utility.sendSuggestCommand(p,  "金額を指定して売り注文 [1:金額][2:個数] => §c§l§n[指し値売り注文]" ,"指定した金額、個数の売り注文をします\n§c§l/mce ordersell(os) "+item.key +" [金額] [売り個数] §e\n※金額が高すぎる場合、売れない場合があります。\n注文キャンセルすると返品されます","/mce ordersell "+item.key + " ");
        }


        p.sendMessage("§bチャートを表示  => §n"+"http://man10.red/mce/"+item.id);

        Utility.sendHoverText(p,  "§e§l最小注文金額 $"+ item.tick+"§c§l§n => 所持数ランキング" ,"ランキング表示\\§c§l/mce ranking "+item.id ,"/mce ranking "+item.id );


        showMainMenuLink(p);
        data.close();

    }

    ///  売り注文を出す
    synchronized public void orderSell(Player p, String idOrKey, double price, int amount){

        if (checkMarketClosed(p)) {
            return;
        }

        MarketData data = new MarketData(this);

        if(data.orderSell(p,idOrKey,price,amount)){
            showMessage(p,"売り注文成功 $"+ data.getPriceString(price) + "/"+amount+"個" );
            data.close();
            return;
        }
        showError(p,"売り注文失敗");
        data.close();

    }


    public boolean storeItem(Player p,int amount){

        ItemStack item = p.getInventory().getItemInMainHand();

        //      もっているだけ販売
        if(amount == -1){
            amount = item.getAmount();

        }else{
            if(item.getAmount() < amount){
                showError(p,"アイテムを"+amount+"個もっていません");
                return false;
            }
        }


        MarketData data = new MarketData(this);
        MarketData.ItemIndex result =  data.getItemPrice(p,item);
        if(result.result){
            showError(p,"このアイテムは登録対象外です");
            return false;
        }

        String uuid = p.getUniqueId().toString();

        //   this.itemBank.sendItemToStorage(uuid,result.id,amount);
        if(!data.itemBank.addItem(uuid, result.id, amount)){
            showError(p,"アイテムバンクへの登録に失敗しました");
            return false;
        }


        //      アイテム個数をへらす
        int from = item.getAmount();
        item.setAmount(from - amount);

        data.close();
        return true;
    }


//    synchronized public boolean withdrawAll(Player p){
//        MarketData data = new MarketData(this);
//        UserData.UserInformation ui = data.userData.getUserInformation(p.getUniqueId().toString());
//        return data.userData.withdraw(p.getUniqueId().toString(),ui.balance);
//    }

    public void giveMap(Player p, String target){

        //      アイテム作成
        ItemStack map = MappRenderer.getMapItem(this,target);
        if(map == null){
            p.sendMessage("マップが取得できなかった");
            return;
        }
        p.getInventory().addItem(map);

        MappRenderer.updateAll();

    }

    public void updateMapList(int item_id){
        MappRenderer.refresh("price:"+item_id);
        MappRenderer.refresh("buy:"+item_id);
        MappRenderer.refresh("sell:"+item_id);
        MappRenderer.refresh("chart:"+item_id);
    }


    public void showLog(Player p, String target, int offset) {

        p.sendMessage("§9§l-----------[注文履歴]-------------");
        MarketData data = new MarketData(this);

        if (target != null) {
            data.showPlayerTransaction(p, target, offset);
            showMainMenuLink(p);
            return;
        }

        showMainMenuLink(p);
        data.close();

    }
    //  アイテムリスト表示
    public void showMenu(Player p,int pageNo){
        MarketData data = new MarketData(this);
        data.showItemList(p,pageNo);
        data.close();
    }

    // アイテム登録
    public void registerItem(Player p,String key,double price,double tick){
        p.sendMessage("アイテム登録");

        ItemStack item = p.getInventory().getItemInMainHand();
        if(item.getAmount() != 1){
            p.sendMessage("§4§lマーケットに登録するアイテムを手に一つもってコマンドを実行してください");
            return;
        }

        MarketData data = new MarketData(this);

        if(data.registerItem(p,item,key,price,tick)){
           showMessage(p,"マーケットにアイテムを登録しました");



        }else{
            showError(p,"登録に失敗しました");
        }

    }

    // アイテム削除
    public void unregisterItem(Player p) {
        p.sendMessage("アイテム削除");

        ItemStack item = p.getInventory().getItemInMainHand();
        if(item.getAmount() != 1){
            p.sendMessage("§4§lマーケットから削除するアイテムを手に一つもってコマンドを実行してください");
            return;
        }

        MarketData data = new MarketData(this);

        if (data.unregisterItem(p, item)) {
            showMessage(p,"マーケットからアイテムを削除しました");
        }else{
            showError(p,"削除に失敗しました");
        }
    }

    public void setTick(Player p,String idOrKey,double tick){

        MarketData data = new MarketData(this);

        MarketData.ItemIndex item = data.getItemPrice(idOrKey);
        if(item == null){
            showError(p,"そのアイテムは存在しない");
            return;
        }
        if(!data.setTickPrice(item.id, tick)){
            showError(p,"値段設定に失敗した");
            return;
        }


        p.sendMessage(item.key+"のtick値を"+tick+"に変更しました");
    }



    void loadConfig(){
        serverMessage("§bMan10 Central Exchange loading....");


/*
        MarketData data = new MarketData(this);


        if(data == null){
            serverMessage("§cDatabase Error");
            return;
        }
*/

        Bukkit.getLogger().info("init item bank");

        this.csvPath = getConfig().getString("csvPath","");
        this.isMarketOpen =  getConfig().getBoolean("marketOpen",false);

        if(isMarketOpen){
            serverMessage("§e§lMan10中央取引所オープンしました！ /MCE");
        }else{
            serverMessage("§e§lMan10中央取引所クローズ中です");
        }

        Bukkit.getLogger().info("load config done");
    }





    //      ログメッセージji
    void log(String text){
        getLogger().info(text);
    }
    //     サーバーメッセージ
    void serverMessage(String text){
        log(text);
        Bukkit.getServer().broadcastMessage(prefix +  text);
    }
    void opLog(String text){
        log(text);
        for(Player p : Bukkit.getServer().getOnlinePlayers()){
            if(p.isOp()){
                p.sendMessage("§4§l[mMarket(OP)] "+text);
            }
        }
    }
    //      プレイヤーメッセージ
    void showMessage(Player p,String text){
        if(p != null){
            p.sendMessage(prefix + text);
        }
    }

    void showError(Player p,String text){
        if(p != null){
            p.sendMessage("§4§lエラー:" + text);
        }
    }

    @Override
    public void onEnable() {


        Bukkit.getLogger().info("setup maps");

        ////////////////////////////////
        //      マップレンダラ初期化
        MappRenderer.setup(this);
        Bukkit.getLogger().info("setup maps done !");


        // Plugin startup logic
        getServer().getPluginManager().registerEvents (this,this);
        getCommand("mce").setExecutor(new MarketCommand(this));

        Bukkit.getLogger().info("MarketCommand");


        getCommand("balance").setExecutor(new BalanceCommand(this));
        getCommand("bal").setExecutor(new BalanceCommand(this));
        getCommand("mbal").setExecutor(new BalanceCommand(this));

        Bukkit.getLogger().info("BalanceCommand");

        getCommand("ipay").setExecutor(new PayItemCommand(this));

        Bukkit.getLogger().info("ItemPayCommand");

        vault = new MarketVault(this);
        //MarketData data = new MarketData(this);
        Bukkit.getLogger().info("Vault完了");

        this.saveDefaultConfig();
        this.loadConfig();
        Bukkit.getLogger().info("config loaded -> register funcs");


/*
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {

                //      マップ関用関数登録
                MarketChart.plugin = this;
                MarketChart.registerFuncs();

            } catch (Exception e) {
                Bukkit.getLogger().info(e.getMessage());
                System.out.println(e.getMessage());
            }
        });
*/

        //      マップ関用関数登録
        MarketChart.plugin = this;
        MarketChart.registerFuncs();

        Bukkit.getServer().broadcastMessage(prefix+"Started");

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }


    @EventHandler
    public void onItemInteract(PlayerInteractEntityEvent event){
        //           回転抑制用
     //   MappRenderer.onPlayerInteractEntityEvent(event);
    }
    @EventHandler
    public void onInteract(PlayerInteractEvent e) {

        //      イベントを通知してやる（ボタン検出用)
       // MappRenderer.onPlayerInteractEvent(e);

        Player p = e.getPlayer();



        if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {


            if (e.getClickedBlock().getState() instanceof ItemFrame) {
                opLog("ItemFrame");
                e.setCancelled(true);
                return;
            }
            if (e.getClickedBlock().getState() instanceof Sign) {
                Sign signs = (Sign) e.getClickedBlock().getState();
                if(signs.getLine(0).equalsIgnoreCase(prefix)) {
                    String line3 = signs.getLine(3);



                    if (line3.equalsIgnoreCase("§2§l[買う]")||line3.equalsIgnoreCase("§2§l[buy]")) {
                        String[] line1 = signs.getLine(1).split(":",2);
                        if (line1[1] != null) {
                            p.chat("/mce buy " + line1[0] + " " + line1[1]);
                        }
                    } else if (line3.equalsIgnoreCase("§4§l[売る]")||line3.equalsIgnoreCase("§4§l[sell]")) {
                        String[] line1 = signs.getLine(1).split(":",2);
                        if (line1[1] != null) {
                            p.chat("/mce sell " + line1[0] + " " + line1[1]);
                        }
                    } else if (line3.equalsIgnoreCase("§6§l[現在値]")||line3.equalsIgnoreCase("§6§l[price]")) {
                        p.chat("/mce price "+ signs.getLine(1));
                                //String[] line1 = signs.getLine(1).split(":",2);
                        //if (line1[1] != null) {
                        //    p.chat("/mce price " + line1[0]);
                       // }

                } else if (line3.equalsIgnoreCase("§1§l[メニュー]")||line3.equalsIgnoreCase("§1§l[menu]")) {
                    p.chat("/mce menu");
                }
                    else {
                        e.getPlayer().sendMessage(prefix + "§4この看板には右クリックアクションが実装されていません");
                    }
                }
            }
        }
    }

    /*
    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if (e.getBlock().getState() instanceof Sign) {
            Sign signs = (Sign) e.getBlock().getState();
            if(signs.getLine(0).equalsIgnoreCase(prefix)) {
                if(!p.hasPermission("red.man10.market.sign.break")){
                    p.sendMessage(prefix + "§4あなたにマーケット看板を破壊する権限はありません");
                    e.setCancelled(true);
                    return;
                }
                sign.Signdelete(p,e.getBlock().getLocation());
                sign.signss.remove(e.getBlock().getLocation());
            }
        }
    }
*/


    //
    @EventHandler void onPlayerJoin(PlayerJoinEvent event){


        Player p = event.getPlayer();
        Bukkit.getLogger().info(p.getName()+" onPlayerJoin");



        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {

                MarketData data = new MarketData(this);

                data.userData.updateUserAssetsHistory(p);


//                String uuid = p.getUniqueId().toString();
//                UserData.UserInformation ui = data.userData.getUserInformation(uuid);
//                if(ui == null){
//                    Bukkit.getLogger().info(p.getName()+"のUserデータを作成中");
//                    data.userData.insertUserInformation(uuid);
//                }
                Bukkit.getScheduler().runTask(this, () -> p.chat("/bal"));


            } catch (Exception e) {
                Bukkit.getLogger().info(e.getMessage());
                System.out.println(e.getMessage());
            }
        });


    }
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event){

        Player p = event.getPlayer();
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {

                MarketData data = new MarketData(this);

                data.userData.updateUserAssetsHistory(p);


            } catch (Exception e) {
                Bukkit.getLogger().info(e.getMessage());
                System.out.println(e.getMessage());
            }
        });
    }
}
