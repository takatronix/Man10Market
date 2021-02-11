package red.man10;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import static red.man10.MarketCommand.checkPermission;

// 7/26 IK追加、 ItemPayのコマンドを扱うクラス
public class PayItemCommand {
    private final MarketPlugin plugin;
    private final HashMap<UUID,String> TWO_TYPE = new HashMap<>();

    //      コンストラクタ
    public PayItemCommand(MarketPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getLogger().info(" PayItemCommand () ->");
    }


    @Deprecated
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        Player p = (Player) sender;
        if (!checkPermission(p, Settings.itemPayPermission)) {
            p.sendMessage("§cYou don't have permission");
            return false;
        }
        if(args.length == 0) {
            p.sendMessage("§e==========[ItemPay]===========");
            p.sendMessage("§e/ipay [player名] [id/key] [個数] : アイテムを送る");
            p.sendMessage("§e/ipay log : アイテムを送った・受け取ったログを見る");
            if (checkPermission(p, Settings.adminPermission)) {
                p.sendMessage("§c/ipay log [player名] : 他人のアイテムを送った・受け取ったログを見る");
            }
            p.sendMessage("§e==========[ItemPay]===========");
            return true;
        }else if(args.length == 1){
            if(args[0].equalsIgnoreCase("log")){
                Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
                    ArrayList<PayItem.PayLog> loglist = PayItem.viewLog(p.getUniqueId());
                    if (loglist == null || loglist.size() == 0) {
                        p.sendMessage("§c§lあなたのログは存在しない");
                        return;
                    }
                    PayItem.sendLogMessage(p,loglist,0);
                });
                return true;
            }
        }else if(args.length == 2){
            if(args[0].equalsIgnoreCase("log")){
                Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
                    String toUUID;
                    if ((toUUID = PayItem.containPlayer(args[1])) == null) {
                        p.sendMessage(plugin.prefix + "§c§lプレイヤーはこのサーバに存在していません");
                        return;
                    }
                    ArrayList<PayItem.PayLog> loglist = PayItem.viewLog(UUID.fromString(toUUID));
                    if (loglist == null || loglist.size() == 0) {
                        p.sendMessage("§c§lその人のログは存在しない");
                        return;
                    }
                    PayItem.sendLogMessage(p,loglist,0);
                });
                return true;
            }
        }else if(args.length == 3){
            if(TWO_TYPE.containsKey(p.getUniqueId())) {
                if(TWO_TYPE.get(p.getUniqueId()).equalsIgnoreCase(args[0]+" "+args[1]+" "+args[2])) {
                    TWO_TYPE.remove(p.getUniqueId());
                    Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
                        long amount;
                        try {
                            amount = Long.parseLong(args[2]);
                        } catch (NumberFormatException e) {
                            p.sendMessage("§c§l個数が数字ではない");
                            return;
                        }
                        PayItem.sendItemfromPlayer(p, args[0], args[1], amount);
                    });
                    return true;
                }
            }
            p.sendMessage("§7§l確認: もう一度 /ipay "+args[0]+" "+args[1]+" "+args[2]+" と打ってください");
            TWO_TYPE.put(p.getUniqueId(),args[0]+" "+args[1]+" "+args[2]);
            return true;
        }
        return false;
    }
}
