package red.man10;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.UUID;

public class ItemPayCommand implements CommandExecutor {

    public ItemPayCommand(MarketPlugin plugin){
        this.plugin = plugin;
    }

    private final MarketPlugin plugin;
    private final HashMap<UUID,String> TWO_TYPE = new HashMap<>();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player))return false;

        Player p = (Player) sender;

        if (!sender.hasPermission(Settings.itemPayPermission)){
            p.sendMessage("§c§lYou don't have permission");
            return  false;
        }

        if (args.length == 0){
            p.sendMessage("§e==========[ItemPay]===========");
            p.sendMessage("§e/ipay [player名] [id/key] [個数] : アイテムを送る");
//            p.sendMessage("§e/ipay log : アイテムを送った・受け取ったログを見る");
//            if (p.hasPermission(Settings.adminPermission)) {
//                p.sendMessage("§c/ipay log [player名] : 他人のアイテムを送った・受け取ったログを見る");
//            }
            p.sendMessage("§e==========[ItemPay]===========");
        }

        if (args.length == 3){

            if (TWO_TYPE.get(p.getUniqueId()).equalsIgnoreCase(args[0]+" "+args[1]+" "+args[2])){
                TWO_TYPE.remove(p.getUniqueId());
                Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
                    long amount;
                    try {
                        amount = Long.parseLong(args[2]);
                    } catch (NumberFormatException e) {
                        p.sendMessage("§c§l個数が数字ではない");
                        return;
                    }

                    PayItem.sendItemFromPlayer(p, args[0], args[1], amount);
                });

                return true;
            }

            p.sendMessage("§7§l確認: もう一度 ”/ipay "+args[0]+" "+args[1]+" "+args[2]+"” と打ってください");
            TWO_TYPE.put(p.getUniqueId(),args[0]+" "+args[1]+" "+args[2]);

            return true;
        }


        return false;
    }
}
