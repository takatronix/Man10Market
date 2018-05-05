package red.man10;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.UUID;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class MarketPlugin extends JavaPlugin implements Listener {

    //Mr_IK push test
    // takatronix push test
    String  prefix = "§f§l[§2§lm§e§lMarket§f§l]";

    MarketData data = null;
    MarketVault vault = null;
    MarketSignEvent sign = null;

    public double buyLimitRatio = 10;
    public double sellLimitRatio = 10;


    ///////////////////////////////
    //      成り行き購入
    public boolean marketBuy(Player p, String target, int amount){

        int ret = data.marketBuy(p.getUniqueId().toString(),target,amount);
        if(ret == -1){
            showError(p,"エラーが発生しました");
            return false;
        }


        if(ret == 0){
            showMessage(p,"購入できませんでした");
            return true;
        }

        showMessage(p,"購入成功:"+ret+"個購入できました");

        return true;
    }
    ///////////////////////////////
    //      成り行き売り
    public boolean marketSell(Player p, String target, int amount){

        int ret = data.marketSell(p.getUniqueId().toString(),target,amount);
        if(ret == -1){
            showError(p,"エラーが発生しました");
            return false;
        }


        if(ret == 0){
            showMessage(p,"売却できませんでした");
            return true;
        }

        showMessage(p,"売却成功:"+ret+"個売却できました");

        return true;
    }


    //      すべての注文をキャンセルする
    public boolean cancelAll(Player p,String playerName) {

        ArrayList<MarketData.OrderInfo> orders = null;

        String uuid = null;
        if(playerName == null){
            uuid = p.getUniqueId().toString();
            orders = data.getOrderOfUser(p,uuid);
        }else{
            orders = data.getOrderOfPlayerName(p,playerName);
        }

        int ret = data.cancelOrderList(orders);
        if(ret == -1){
            return false;
        }

        showMessage(p,""+ret+"件の注文をキャンセルしました");
        return true;
    }


    //  注文キャンセル
    public boolean cancelOrder(Player p,String target) {

        int orderNo = -1;
        try{
            orderNo = Integer.parseInt(target);
        }catch (Exception e){

        }

        if(orderNo == -1){
            return false;
        }

        MarketData.OrderInfo order = data.getOrderByOrderId(orderNo);
        if(order == null){
            showError(p,"そのIDはみつからない");
            return false;
        }

        //      自分の注文ではない
        if(order.uuid.equalsIgnoreCase(p.getUniqueId().toString()) == false){

            if(p.hasPermission("red.man10.cancelother") == false){
                showError(p,"自分以外の注文をキャンセルする権限がない");
                return false;
            }
        }


        //
        if(data.cancelOrderByOrderId(orderNo)){
           data.updateCurrentPrice(order.item_id);

            p.sendMessage("注文ID:"+order.id +"をキャンセルしました");
            p.chat("/mm order");
            return true;
        }

        return false;
    }




    //  注文表示
    public boolean showOrder(Player p,String target){


        //  引数なし->自分の注文
        if(target == null){
            return showOrderOfUser(p,p.getUniqueId().toString());
        }


        //
        MarketData.ItemIndex pr = data.getItemPrice(target);
        if(pr != null){
            if(pr.key != null){
                return showOrderOfItem(p,pr.id);
            }
        }

        Player player = Bukkit.getPlayer(target);
        if(player == null) {
            return showOrderOfUserName(p,target);
        }
        if(player != null){
            return showOrderOfUser(p,player.getUniqueId().toString());
        }
        return false;
    }

    public boolean showOrderOfUser(Player p,String uuid){
        ArrayList<MarketData.OrderInfo> orders = data.getOrderOfUser(p,uuid);
        showOrders(p,orders);
        return true;
    }

    public boolean showOrderOfUserName(Player p,String name){
        ArrayList<MarketData.OrderInfo> orders = data.getOrderOfPlayerName(p,name);
        showOrders(p,orders);
        return true;
    }
    public boolean showOrderOfItem(Player p,int item_id){
        ArrayList<MarketData.OrderInfo> orders = data.getOrderOfItem(p,item_id);
        showOrders(p,orders);
        return true;
    }

    public void sendClickableMessage(Player player, String message, String url) {
        Bukkit.getServer().dispatchCommand(
                Bukkit.getConsoleSender(),
                "/tellraw " + player.getName() +
                        " {text:\"" + message + "\",clickEvent:{action:open_url,value:\"" +
                        url + "\"}}");
    }

    public boolean showOrders(Player p, ArrayList<MarketData.OrderInfo> orders){


        sendClickableMessage(p,"xxx","http://man10.red");
        showMessage(p,"§f§l---------[注文リスト]-----------");
        int count = 0;
        for(MarketData.OrderInfo o : orders){

            String buyOrSell = "§a売";
            if(o.isBuy){
                buyOrSell = "§9買";
            }
           // String strDate = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(o.datetime);
            String price = "$"+data.getPriceString(o.price);

            //   注文テキストを作る
            String order = String.format("§f§l§n注文ID%d:%3s:%s:%s:%s/%7s個 %s:%s §f§l§n[X]"
                ,o.id,buyOrSell,o.key,o.player,price,data.getPriceString(o.amount),o.date.toString(),o.time.toString()
            );

            MarketData.sendHoverText(p, order,"クリックすると注文がキャンセルされます","/mm cancel "+o.id);

/*
            //  クリックキャンセルイベント
            ClickEvent clickEvent = new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/mm cancel "+o.id);
            HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(order + "§f§lをキャンセルします").create());

            BaseComponent[] cancel = new ComponentBuilder("§f§l§n[CANCEL]").event(hoverEvent).event(clickEvent). create();
            BaseComponent[] orderText = new ComponentBuilder( order).append(cancel).create();
            p.spigot().sendMessage(orderText);
*/
            count++;
        }
        if(count == 0){
            showMessage(p,"注文がありません");
        }else{
            MarketData.sendHoverText(p,prefix + "§f§l§n [全ての注文をキャンセル/Cancel All]","クリックすると全ての注文がキャンセルされます","/mm cancelall");
            showMessage(p,""+count+"件の注文があります");


        }

/*
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
*/
        return true;
    }
    ///////////////////////////////
    //         板情報を表示する
    public boolean showOrderBook(Player p,int item_id,int limit){

        MarketData.ItemIndex item = data.getItemPrice(String.valueOf(item_id));
        String uuid = p.getUniqueId().toString();

        if(item == null) {
            showError(p,"このアイテムは売買できません");
            return false;
        }

        ArrayList<MarketData.OrderInfo> sellList = data.getGroupedOrderList(uuid,item_id,false,-1);
        ArrayList<MarketData.OrderInfo> buyList = data.getGroupedOrderList(uuid,item_id,true,-1);

        showMessage(p,"---------["+item.key+"]-----------");
        showMessage(p," §b§l売数量    値段   買数量");
        for(int i = 0;i < sellList.size();i++){
            MarketData.OrderInfo o = sellList.get(i);
            String color ="§e§l";
            if(i ==  sellList.size() -1){
                color  = "§a§l";
            }
            showMessage(p,String.format("%s%7d  %7s",color,o.amount,data.getPriceString(o.price)));
        }
        for(int i = 0;i < buyList.size();i++){
            MarketData.OrderInfo o = buyList.get(i);
            String color ="§e§l";
            if(i == 0){
                color  = "§c§l";
            }
            showMessage(p,String.format("%s         %7s  %7d",color,data.getPriceString(o.price),o.amount));
        }

        return true;
    }

    ///  売り注文を出す
    public boolean orderBuy(Player p,String idOrKey,double price,int amount){

        if(data.orderBuy(p,idOrKey,price,amount)){
            showMessage(p,"買い注文成功 $"+ data.getPriceString(price) + "/"+amount+"個" );


            return true;
        }


        return false;
    }



    //      アイテムの値段を表示
    public boolean showPrice(Player p,String idOrKey){


        if(idOrKey == null){
            ItemStack item = p.getInventory().getItemInMainHand();

            MarketData.ItemIndex ret = data.getItemPrice(p,item);
            if(ret == null){
                showError(p,"取得失敗");
                return false;
            }

            if(ret.result == true){
                double st = ret.price * 64;
                showMessage(p,"現在価格:$" + data.getPriceString(ret.price) +"/個 $"+  data.getPriceString(st)+"/1Stack");
                showMessage(p,"§c§l売り注文数(Sell):"+ret.sell +"/§9§l買い注文数(Buy):"+ret.buy);

                //      板表示
                showOrderBook(p,ret.id,-1);
            }else{
                showError(p,"データ取得失敗");
            }

            return ret.result;

        }

        MarketData.ItemIndex ret = data.getItemPrice(idOrKey);
        if(ret == null){
            showError(p,"指定されたアイテムデータは取得できません");
            return false;
        }

        double st = ret.price * 64;
        showMessage(p,"現在価格:$" + data.getPriceString(ret.price) +"/個 $"+  data.getPriceString(st)+"/1Stack");
        showMessage(p,"§c§l売り注文数(Sell):"+ret.sell +"/§9§l買い注文数(Buy):"+ret.buy);

        //      板表示
        showOrderBook(p,ret.id,-1);

        return true;
    }

    ///  売り注文を出す
    public boolean orderSell(Player p,String idOrKey,double price,int amount){

        if(data.orderSell(p,idOrKey,price,amount)){
            showMessage(p,"売り注文成功 $"+ data.getPriceString(price) + "/"+amount+"個" );
            return true;
        }
        showError(p,"売り注文失敗");

        return false;
    }


    public boolean storeItem(Player p,int amount){

        ItemStack item = p.getInventory().getItemInMainHand();
        if(item == null){
            showError(p,"空のアイテムは登録できません");
            return false;
        }

        //      もっているだけ販売
        if(amount == -1){
            amount = item.getAmount();

        }else{
            if(item.getAmount() < amount){
                showError(p,"アイテムを"+amount+"個もっていません");
                return false;
            }
        }

        MarketData.ItemIndex result =  data.getItemPrice(p,item);
        if(result.result == false){
            showError(p,"このアイテムは登録対象外です");
            return false;
        }

        String uuid = p.getUniqueId().toString();

        MarketData.ItemStorage store = data.getItemStorage(uuid,result.id);
        this.data.sendItemToStorage(uuid,result.id,amount);


        //      アイテム個数をへらす
        int from = item.getAmount();
        item.setAmount(from - amount);


        return true;
    }
    //  アイテムリスト表示
    public void showList(Player p){
        data.showItemList(p);
    }


    // アイテム登録
    public void registerItem(Player p,String key,double price,double tick){
        p.sendMessage("アイテム登録");

        ItemStack item = p.getInventory().getItemInMainHand();
        if(item.getAmount() != 1){
            p.sendMessage("§4§lマーケットに登録するアイテムを手に一つもってコマンドを実行してください");
            return;
        }


        if(data.registerItem(p,item,key,price,tick)){
           showMessage(p,"マーケットにアイテムを登録しました");



        }else{
            showError(p,"登録に失敗しました");
        }

    }

    void loadConfig(){
        data = new MarketData(this);
        if(data == null){
            if(data == null) {
                Bukkit.getServer().broadcastMessage("DB接続エラー");
                return;
            }
        }
    }





    //      ログメッセージ
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
        // Plugin startup logic
        getServer().getPluginManager().registerEvents (this,this);
        getCommand("mm").setExecutor(new MarketCommand(this));


        vault = new MarketVault(this);

        this.saveDefaultConfig();
        this.loadConfig();

        sign = new MarketSignEvent(this);

        Bukkit.getServer().broadcastMessage(prefix+"Started");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @EventHandler
    public void onSigncreate(SignChangeEvent e){
        if(e.getLine(0).equalsIgnoreCase("[Man10market]")){
            if(!e.getPlayer().hasPermission("red.man10.market.sign.create")){
                e.getPlayer().sendMessage(prefix + "§4あなたにマーケット看板を作成する権限はありません!");
                e.getBlock().breakNaturally();
                return;
            }
            sign.Signcreate(e.getPlayer(),e.getBlock().getLocation(),e.getLine(1));
            e.setLine(0,prefix);
            String line3 = e.getLine(3);
            if (line3.equalsIgnoreCase("[購入]")) {
                e.setLine(3,"§a§l[購入]");
            }else if (line3.equalsIgnoreCase("[売却]")) {
                e.setLine(3,"§6§l[売却]");
            }else if (line3.equalsIgnoreCase("[現在値]")) {
                e.setLine(3,"§e§l[現在値]");
            }else if (line3.equalsIgnoreCase("[buy]")) {
                e.setLine(3,"§a§l[buy]");
            }else if (line3.equalsIgnoreCase("[sell]")) {
                e.setLine(3,"§6§l[sell]");
            }else if (line3.equalsIgnoreCase("[price]")) {
                e.setLine(3,"§e§l[price]");
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (e.getClickedBlock().getState() instanceof Sign) {
                Sign signs = (Sign) e.getClickedBlock().getState();
                if(signs.getLine(0).equalsIgnoreCase(prefix)) {
                    String line3 = signs.getLine(3);
                    if (line3.equalsIgnoreCase("§a§l[購入]")||line3.equalsIgnoreCase("§a§l[buy]")) {
                        String[] line1 = signs.getLine(1).split(":",2);
                        if (line1[1] != null) {
                            p.chat("/mm buy " + line1[0] + " " + line1[1]);
                        }
                    } else if (line3.equalsIgnoreCase("§6§l[売却]")||line3.equalsIgnoreCase("§6§l[sell]")) {
                        String[] line1 = signs.getLine(1).split(":",2);
                        if (line1[1] != null) {
                            p.chat("/mm sell " + line1[0] + " " + line1[1]);
                        }
                    } else if (line3.equalsIgnoreCase("§e§l[現在値]")||line3.equalsIgnoreCase("§e§l[price]")) {
                        String[] line1 = signs.getLine(1).split(":",2);
                        if (line1[1] != null) {
                            p.chat("/mm price " + line1[0]);
                        }
                    } else {
                        e.getPlayer().sendMessage(prefix + "§4この看板には右クリックアクションが実装されていません");
                    }
                }
            }
        }
    }

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
}
