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


    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        Player p = (Player) sender;


        if(args.length == 0){
            showHelp(p);
            return false;
        }


        String command = args[0];


        ////////////////
        //    登録
        if(command.equalsIgnoreCase("register")){
            if(args.length != 4){
                p.sendMessage("§c§l/mm register 1)登録名称 2)初期金額 3)ティック(値動き幅) - 手にもったアイテムをマーケットに登録する");
                return false;
            }
            plugin.registerItem(p,args[1], Double.parseDouble(args[2]),Double.parseDouble(args[3]));
            return true;
        }

        ////////////////
        //    リスト
        if(command.equalsIgnoreCase("list")){
            plugin.showList(p);
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
            if(args.length != 4){
                p.sendMessage("§2§l/mm ordersell [id/key] [一つあたりの金額] [個数] -  指定した金額で売り注文を出す");
                return false;
            }
            return plugin.orderSell(p,args[1],Double.parseDouble(args[2]),Integer.parseInt(args[3]));
        }

        //////////////////////////
        //   指値買い注文
        if(command.equalsIgnoreCase("orderbuy") || command.equalsIgnoreCase("ob") )

        {
            if(args.length != 4){
                p.sendMessage("§2§l/mm orderbuy [id/key] [一つあたりの金額] [個数] - 指定した金額で買い注文を出す");
                return false;
            }

            return plugin.orderBuy(p,args[1],Double.parseDouble(args[2]),Integer.parseInt(args[3]));
        }


        //    アイテム保存
        if(command.equalsIgnoreCase("store")){
            if(args.length == 1){
                return plugin.storeItem(p,-1);
            }
            if(args.length == 2){
                return plugin.storeItem(p,Integer.parseInt(args[1]));
            }
            p.sendMessage("§2§l/mm store (個数)- 手に持ったアイテムを倉庫にいれる");
            return false;
        }




        this.showHelp(p);
        return true;
    }

    void showHelp(CommandSender p){
        p.sendMessage("§e============== §d●§f●§a●§e Man10 Market §d●§f●§a● §e===============");

        p.sendMessage("§c-------アイテム登録--------------");
        p.sendMessage("§2§l/mm store (個数)- 手に持ったアイテムを倉庫にいれる");
        p.sendMessage("§2§l/mm restore [id/key] [個数] 倉庫からアイテムを引き出す");

        p.sendMessage("§c--------------------------------");
        p.sendMessage("§2§l/mm list - 登録アイテムリストと価格を表示する");
        p.sendMessage("§2§l/mm price (id/key) - (id or Key/手に持ったアイテム)の金額を表示する");

        p.sendMessage("§c-------指値注文------------------");
        p.sendMessage("§2§l/mm ordersell/os [id/key] [一つあたりの金額] [個数] - 指定した金額で売り注文を出す");
        p.sendMessage("§2§l/mm orderbuy/ob  [id/key] [一つあたりの金額] [個数] - 指定した金額で買い注文を出す");

        p.sendMessage("§c--------------------------------");

        p.sendMessage("/mm order (user) 注文を表示する");
        p.sendMessage("/mm cancel [order_id] 注文をキャンセルする");
        p.sendMessage("/mm cancellall (userid) 全ての注文をキャンセルする");

        p.sendMessage("/mm marketbuy/mb [id/this] [個数] - 成り行き注文（市場価格で購入)");
        p.sendMessage("/mm marketsell/b [id/this] [個数] - 成り行き注文（市場価格で売り)");
        p.sendMessage("§c--------------------------------");
        p.sendMessage("§c§l/mm register 1)登録名称 2)初期金額 3)ティック(値動き幅) - 手にもったアイテムをマーケットに登録する");
        p.sendMessage("§c/mm unregister - 手にもったアイテムをマーケットから削除する");
        p.sendMessage("§c--------------------------------");
        p.sendMessage("§e created by takatronix http://twitter.com/takatronix");
        p.sendMessage("§e http://man10.red");

    }
    void showTable(CommandSender p){

    }
}
