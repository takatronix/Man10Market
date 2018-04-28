package red.man10;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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

        p.sendMessage("length:"+args.length);

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
            plugin.showPrice(p);
            return true;
        }



        //////////////////////////
        //      売り注文
        if(command.equalsIgnoreCase("ordersell")){
            if(args.length < 2 || args.length >3){
                p.sendMessage("§2§l/mm ordersell [一つあたりの金額] (個数) -  指定した金額で売り注文を出す");
                return false;
            }
            if(args.length == 3){
                return plugin.orderSell(p,Double.parseDouble(args[1]), Integer.parseInt(args[2]));
            }
            if(args.length == 2){
                return plugin.orderSell(p,Double.parseDouble(args[1]),-1);
            }
        }

        //////////////////////////
        //   買い注文
        if(command.equalsIgnoreCase("orderbuy")){
            if(args.length != 4){
                p.sendMessage("§2§l/mm orderbuy [id/key] [一つあたりの金額] [個数]- 指定した金額で買い注文を出す");
                return false;
            }

            return plugin.orderBuy(p,args[1],Double.parseDouble(args[2]),Integer.parseInt(args[3]));
        }



        this.showHelp(p);
        return true;
    }

    void showHelp(CommandSender p){
        p.sendMessage("§e============== §d●§f●§a●§e Man10 Market §d●§f●§a● §e===============");
        p.sendMessage("§e  by takatronix http://man10.red");
        p.sendMessage("§c* 赤いコマンドは管理者用です");


        p.sendMessage("§2§l/mm list - 登録アイテムリストと価格を表示する");
        p.sendMessage("§2§l/mm price (id/key) - (id or Key/手に持ったアイテム)の金額を表示する");
        p.sendMessage("§2§l/mm ordersell [一つあたりの金額] (個数) -  指定した金額で売り注文を出す");

        p.sendMessage("§2§l/mm orderbuy [id/key] [一つあたりの金額] [個数]- 指定した金額で買い注文を出す");

        p.sendMessage("/mm order (user) 注文を表示する");
        p.sendMessage("/mm cancel [order_id] 注文をキャンセルする");
        p.sendMessage("/mm cancellall (userid) 全ての注文をキャンセルする");

        p.sendMessage("/mm orderbuy [id/this] [一つあたりの金額] [個数]- 指定した金額で買い注文を出す");

        p.sendMessage("/mm marketbuy [id/this] [個数] - 成り行き注文（市場価格で購入)");
        p.sendMessage("/mm marketsell [id/this] [個数] - 成り行き注文（市場価格で売り)");
        p.sendMessage("§c--------------------------------");
        p.sendMessage("§c§l/mm register 1)登録名称 2)初期金額 3)ティック(値動き幅) - 手にもったアイテムをマーケットに登録する");
        p.sendMessage("§c/mm unregister - 手にもったアイテムをマーケットから削除する");
    }
    void showTable(CommandSender p){

    }
}
