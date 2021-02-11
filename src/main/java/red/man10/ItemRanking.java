package red.man10;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class ItemRanking {

    MarketPlugin plugin;
    MarketData data = null;

    public ItemRanking(MarketPlugin plugin){
        this.plugin = plugin;
    }

    public void showRanking(Player p, String idOrKey, final int offset){

        data = new MarketData(this.plugin);
        MarketData.ItemIndex item = data.getItemPrice(idOrKey);
        if(item == null){
            plugin.showError(p,"そのアイテムはみつからない");
            return;
        }

        MarketData.ItemIndex index = MarketPlugin.priceMap.get(item.id);
        if(index == null){
            Bukkit.getLogger().info("item:"+item.id + " is null use db");
//            index = data.getItemPrice(item.id);
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


        long mce_total = getSellAmount(item.id);
        double mce_totalprice = mce_total * item.price;

        p.sendMessage("§7§l"+item.key + "のサーバ内合計個数(mib): " + Utility.getColoredItemString(total)+" ("+Utility.getJpBal(total) + ")"  ) ;
        p.sendMessage("§7§l評価額(mib): " + Utility.getColoredPriceString(totalprice) + " ("+Utility.getJpBal(totalprice) + ")" );
        p.sendMessage("§7§l"+item.key + "の売り注文数(mce): " + Utility.getColoredItemString(mce_total)+" ("+Utility.getJpBal(mce_total) + ")"  ) ;
        p.sendMessage("§7§l評価額(mce): " + Utility.getColoredPriceString(mce_totalprice) + " ("+Utility.getJpBal(mce_totalprice) + ")" );


    }


    long getSellAmount(int item_id){
        long ret =0;
        String sql = "select sum(amount) from order_tbl where buy = 0 and item_id="+item_id;
        ResultSet rs = data.mysql.query(sql);

        if(rs == null){
            return 0;
        }
        try
        {
            while(rs.next())
            {
                ret = rs.getLong("sum(amount)");
            }
            rs.close();
        }
        catch (SQLException e)
        {
            Bukkit.getLogger().info("Error executing a query: " + e.getErrorCode());
            return 0;
        }
        data.mysql.close();
        return ret;
    }



}
