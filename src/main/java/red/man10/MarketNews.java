package red.man10;

import org.bukkit.entity.Player;

public class MarketNews {

    public  MarketPlugin plugin = null;
    public  MarketData data = null;


    public void broadCastNews(){
        plugin.serverMessage("Man10中央取引所ニュース！！！ /mce news");



        plugin.serverMessage("");



    }


    public void playerNews(Player p){
        plugin.serverMessage("Man10中央取引所ニュース！！！ /mce news");



        plugin.serverMessage("");



    }


}
