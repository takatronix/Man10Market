package red.man10;

import org.bukkit.entity.Player;

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




    }


    public void playerNews(Player p){



        plugin.serverMessage("Man10中央取引所ニュース！！！ /mce news");



        plugin.serverMessage("");



    }


}
