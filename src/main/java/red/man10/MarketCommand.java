package red.man10;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.omg.CORBA.INTERNAL;

public class MarketCommand implements CommandExecutor {
    private final MarketPlugin plugin;

    //      コンストラクタ
    public MarketCommand(MarketPlugin plugin) {
        this.plugin = plugin;
    }


    String  adminPermision = "red.man10.admin";


    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        Player p = (Player) sender;


        if(args.length == 0){
            plugin.showMenu(p,0);
            return false;
        }


        String command = args[0];


        ////////////////
        //    登録
        if(command.equalsIgnoreCase("register")){
            if(!p.hasPermission("red.man10.market.register")){
                p.sendMessage("§4§lあなたには登録権限がない");
                return false;
            }

            if(args.length != 4){
                p.sendMessage("§c§l/mce register 1)登録名称 2)初期金額 3)ティック(値動き幅) - 手にもったアイテムをマーケットに登録する");
                return false;
            }
            plugin.registerItem(p,args[1], Double.parseDouble(args[2]),Double.parseDouble(args[3]));
            return true;
        }

        ////////////////
        //    リスト
        if(command.equalsIgnoreCase("menu")){

            if(args.length == 2){

                plugin.showMenu(p,Integer.parseInt(args[1]));
                return true;
            }
            plugin.showMenu(p,0);

            return true;
        }

        if(command.equalsIgnoreCase("log")){
            if(args.length == 2){
                plugin.showLog(p,null,Integer.parseInt(args[1]));
                return true;
            }

            plugin.showLog(p,null,0);
            return true;
        }
        if(command.equalsIgnoreCase("userlog")){
            if(args.length == 3){
                plugin.showLog(p,args[1],Integer.parseInt(args[2]));
                return true;
            }
            if(args.length == 2){
                plugin.showLog(p,args[1],0);
            }
            return true;
        }


        //////////////////////////
        //      売り注文
        if(command.equalsIgnoreCase("price")){

            if(args.length == 2){
               // p.sendMessage("length2"+args[1]);
                plugin.showPrice(p,args[1]);
                return true;
            }
            plugin.showPrice(p,null);
            return true;
        }

        //////////////////////////
        //      指値売り注文
        if(command.equalsIgnoreCase("ordersell") || command.equalsIgnoreCase("os")){

            if(!p.hasPermission("red.man10.market.ordersell")){
                p.sendMessage("§4§lあなたには権限がない");
                return false;
            }

            if(args.length != 4){
                p.sendMessage("§2§l/mce ordersell [id/key] [一つあたりの金額] [個数] -  指定した金額で売り注文を出す");
                return false;
            }
            return plugin.orderSell(p,args[1],(double)(int)Double.parseDouble(args[2]),Integer.parseInt(args[3]));
        }

        ////////////////////////////
        //   成り行き売り注文
        if(command.equalsIgnoreCase("marketsell") || command.equalsIgnoreCase("ms")) {
            if(!p.hasPermission("red.man10.market.marketsell")){
                p.sendMessage("§4§lあなたには権限がない");
                return false;
            }
            if(args.length != 3){
                p.sendMessage("§2§l/mce sell [id/key] [個数] - 成り行き注文（市場価格で購入)");
                return false;
            }

            return plugin.marketSell(p,args[1],Integer.parseInt(args[2]));
        }

        //////////////////////////
        //   指値買い注文
        if(command.equalsIgnoreCase("orderbuy") || command.equalsIgnoreCase("ob") ) {
            if(!p.hasPermission("red.man10.market.orderbuy")){
                p.sendMessage("§4§lあなたには権限がない");
                return false;
            }
            if(args.length != 4){
                p.sendMessage("§2§l/mce orderbuy [id/key] [一つあたりの金額] [個数] - 指定した金額で買い注文を出す");
                return false;
            }

            return plugin.orderBuy(p,args[1],(double)(int)Double.parseDouble(args[2]),Integer.parseInt(args[3]));
        }


        ////////////////////////////
        //   成り行き買い注文
        if(command.equalsIgnoreCase("marketbuy") || command.equalsIgnoreCase("mb")) {
            if(!p.hasPermission("red.man10.marketbuy.buy")){
                p.sendMessage("§4§lあなたには権限がない");
                return false;
            }
            if(args.length != 3){
                p.sendMessage("§2§l/mce buy [id/key] [個数] - 成り行き注文（市場価格で購入)");
                return false;
            }

            return plugin.marketBuy(p,args[1],Integer.parseInt(args[2]));
        }

        //    アイテム保存
        if(command.equalsIgnoreCase("store")){

            if(!p.hasPermission("red.man10.market.store")){
                p.sendMessage("§4§lあなたには権限がない");
                return false;
            }
            if(args.length == 1){
                return plugin.storeItem(p,-1);
            }
            if(args.length == 2){
                return plugin.storeItem(p,Integer.parseInt(args[1]));
            }
            p.sendMessage("§2§l/mce store (個数)- 手に持ったアイテムを倉庫にいれる");
            return false;
        }


        //    注文リスト
        if(command.equalsIgnoreCase("order")){
            if(!p.hasPermission("red.man10.market.order")){
                p.sendMessage("§4§lあなたには権限がない");
                return false;
            }
            if(args.length == 2){
                if(p.hasPermission("red.man10.market.orderother")){
                    return plugin.showOrder(p,args[1]);
                }else{
                    p.sendMessage("§4§lあなたには権限がない");
                    return false;
                }

            }
            return plugin.showOrder(p,null);
        }


        //    注文キャンセル
        if(command.equalsIgnoreCase("cancel")){
            if(!p.hasPermission("red.man10.market.cancel")){
                p.sendMessage("§4§lあなたには権限がない");
                return false;
            }
            if(args.length == 2){
                return plugin.cancelOrder(p,args[1]);
            }
            p.sendMessage("/mce cancel [order_id] 注文をキャンセルする");
            return false;
        }

        /////////////////////////////////////////////
        //    全注文キャンセル
        if(command.equalsIgnoreCase("cancelall")){
            if(!p.hasPermission("red.man10.market.cancel")){
                p.sendMessage("§4§lあなたには権限がない");
                return false;
            }

            if(args.length == 1){
                return plugin.cancelAll(p,null);
            }

            //  管理者は人の注文をキャンセルできる
            if(p.hasPermission(adminPermision)){
                if(args.length == 2){
                    return plugin.cancelAll(p,args[1]);
                }
            }

            p.sendMessage("/mce cancelall すべての注文をキャンセルする");
            return false;
        }

        //    注文キャンセル
        if(command.equalsIgnoreCase("update")){
            if(!p.hasPermission("red.man10.market.update")){
                p.sendMessage("§4§lあなたには権限がない");
                return false;
            }
            if(args.length == 2){
                return plugin.updatePrice(p,args[1]);
            }
            p.sendMessage("/mce update [id/key] 金額を調整する");
            return false;
        }
        if(command.equalsIgnoreCase("help")) {
            this.showHelp(p);
            return true;
        }



        return true;

    }

    void showHelp(CommandSender p){
        p.sendMessage("§e============== §d●§f●§a●§e Man10 Market §d●§f●§a● §e===============");
        p.sendMessage("§c-------アイテム登録--------------");
        p.sendMessage("§2§l/mce store (個数)- 手に持ったアイテムを倉庫にいれる");
        p.sendMessage("§2§l/mce restore [id/key] [個数] 倉庫からアイテムを引き出す");
        p.sendMessage("§c--------------------------------");
        p.sendMessage("§2§l/mce list - 登録アイテムリストと価格を表示する");
        p.sendMessage("§2§l/mce price (id/key) - (id/Key/手に持ったアイテム)の金額を表示する");

        p.sendMessage("§c--成り行き注文（現在値で買う)-------------");
        p.sendMessage("/mce buy [個数] - 成り行き注文（市場価格で購入)");
        p.sendMessage("/mce sell [個数] - 成り行き注文（市場価格で売り)");

        p.sendMessage("§c--指値注文(金額を指定して注文を入れる)---------");
        p.sendMessage("§2§l/mce ordersell/os [id/key] [一つあたりの金額] [個数] - 指定した金額で売り注文を出す");
        p.sendMessage("§2§l/mce orderbuy/ob  [id/key] [一つあたりの金額] [個数] - 指定した金額で買い注文を出す");

        p.sendMessage("§c-------注文管理------------------");
        p.sendMessage("/mce order  注文を表示する");
        p.sendMessage("/mce cancel [order_id] 注文をキャンセルする");
        p.sendMessage("/mce cancelall  全ての注文をキャンセルする");
        p.sendMessage("/mce canceltem [id/key]");


        p.sendMessage("§c--------------------------------");
        p.sendMessage("§e created by takatronix http://twitter.com/takatronix");
        p.sendMessage("§e http://man10.red");

        if(p.hasPermission("red.man10.admin")){
            showAdminHelp(p);
        }

    }
    void showAdminHelp(CommandSender p){
        p.sendMessage("§c-----------Admin Commands---------------------");
        p.sendMessage("§c§l/mce order (user/id/key) 注文を表示する");
        p.sendMessage("/mce cancellall  全ての注文をキャンセルする");
        p.sendMessage("§c§l/mce userlog (user) ユーザーの注文履歴");

        p.sendMessage("§c§l/mce register 1)登録名称 2)初期金額 3)ティック(値動き幅) - 手にもったアイテムをマーケットに登録する");
        p.sendMessage("§c/mce unregister - 手にもったアイテムをマーケットから削除する");

    }
}
