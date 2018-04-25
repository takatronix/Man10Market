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

        this.showHelp(p);
        return true;
    }

    void showHelp(CommandSender p){
        p.sendMessage("§e============== §d●§f●§a●§e Man10 Market §d●§f●§a● §e===============");
        p.sendMessage("§e  by takatronix http://man10.red");
        p.sendMessage("§c* 赤いコマンドは管理者用です");
        p.sendMessage("/mm sellall - 手にもったアイテムすべてを市場価格で販売する");

        p.sendMessage("/mm price [id/this] - (id/手に持ったアイテム)の金額を表示する");
        p.sendMessage("/mm order　現在の自分の注文を表示する");
        p.sendMessage("/mm cancel [order_id] 注文をキャンセルする");
        p.sendMessage("/mm orderbuy [id/this] [一つあたりの金額] [個数]- 指定した金額で買い注文を出す");
        p.sendMessage("/mm ordersell [id/this] [一つあたりの金額] [個数] -  指定した金額で売り注文を出す");
        p.sendMessage("/mm marketbuy [id/this] [個数] - 成り行き注文（市場価格で購入)");
        p.sendMessage("/mm marketsell [id/this] [個数] - 成り行き注文（市場価格で売り)");
        p.sendMessage("§c--------------------------------");
        p.sendMessage("§c*/mm register 初期金額 - 手にもったアイテムをマーケットに登録する");
        p.sendMessage("§c*/mm unregister - 手にもったアイテムをマーケットから削除する");
    }
    void showTable(CommandSender p){

    }
}
