package red.man10;

import org.bukkit.entity.Item;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class MarketNews {

    public  MarketPlugin plugin = null;
    public  MarketData data = null;


    public void broadCastNews(){

        //    amount ranking
        String amountRank = "select item_id ,sum(amount) as amount from item_storage   group by item_id order by amount desc;";

        Calendar datetime = Calendar.getInstance();
        datetime.setTime(new Date());
        int year = datetime.get(Calendar.YEAR);
        int month = datetime.get(Calendar.MONTH) + 1;
        int day = datetime.get(Calendar.DAY_OF_MONTH);
        int hour = datetime.get(Calendar.HOUR_OF_DAY);

        String title = "§d§l Man10中央取引所"+year+"/"+month+"/"+day+" "+hour+"時マーケットニュース! => /mce news";
        plugin.serverMessage(title);

        ArrayList<MarketData.ItemIndex> list = data.getItemIndexList("select * from item_index order by id");

        String text = "§f§l現在価格(前日比)";

        for(MarketData.ItemIndex item:list){


            //      前日
            double lastPrice = item.price;
            MarketHistory.Candle yesterday = data.history.getYesterdayCandle(item.id);
            if(yesterday != null){
                lastPrice = yesterday.close;
            }


            String price = Utility.getPriceString(item.price);
            String per = "";

            if(lastPrice != 0){
                per = String.format("(%.1f%%)",(item.price / lastPrice) * 100 -100);
            }
            if(lastPrice > item.price){
                text += " §c§l" + item.key+":"+price + per+ "↓";

            }
            //
            else if(item.price > lastPrice){
                text += " §a§l" + item.key+":"+price + per+ "↑";

            }else {
                text += " §e" + item.key+":"+price;
            }

        }

        plugin.serverMessage(text);

    }


    public void playerNews(Player p){



        plugin.serverMessage("Man10中央取引所ニュース！！！ /mce news");



        plugin.serverMessage("");



    }


}
