package red.man10;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;

public class ItemRanking {

    MarketPlugin plugin = null;
    MarketData data = null;

    public ItemRanking(MarketPlugin plugin){
        this.plugin = plugin;
    }

    public boolean showRanking(Player p, String idOrKey,final int offset){

        data = new MarketData(this.plugin);
        MarketData.ItemIndex item = data.getItemPrice(idOrKey);
        if(item == null){
            plugin.showError(p,"そのアイテムはみつからない");
            return false;
        }

        MarketData.ItemIndex index = MarketPlugin.priceMap.get(item.id);
        if(index == null){
            Bukkit.getLogger().info("item:"+item.id + " is null use db");
            index = data.getItemPrice(item.id);
        }


        p.sendMessage("§e§l===============[§f§l" + item.key +"の所持数ランキング§e§l]==============");


        ArrayList<ItemBank.ItemStorage> rank = data.itemBank.getAmountRanking(item.id,10,offset);
        int no = offset + 1;
        for (ItemBank.ItemStorage s :rank) {

            double price = item.price * s.amount;
            p.sendMessage("§7§l"+no+"位 §f§l "+s.player + " §a§l"+Utility.getColoredItemString(s.amount)  + "   §7§l評価額: " + Utility.getColoredPriceString(price) + " ("+Utility.getJpBal(price) + ")") ;

            no++;
        }

        p.sendMessage("§e§l===============[§f§l" + item.key +"の所持数ランキング§e§l]==============");
        long total = data.itemBank.getTotalAmount(item.id);
        double totalprice = total * item.price;
        p.sendMessage("§7§l"+item.key + "のサーバ内合計個数: " + Utility.getColoredItemString(total)+" ("+Utility.getJpBal(total) + ")"  ) ;
        p.sendMessage("§7§l評価額: " + Utility.getColoredPriceString(totalprice) + " ("+Utility.getJpBal(totalprice) + ")" );





        return true;
    }



}
