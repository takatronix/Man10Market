package red.man10;



import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapRenderer;
import red.man10.MarketData;

import javax.lang.model.type.UnionType;
import javax.rmi.CORBA.Util;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;



public class MarketChart {
    static int width = 128;
    static int height = 128;
    public static MarketPlugin plugin = null;
    public static MarketData data = null;


    static int  clickedCount = 0;

// http://www.minecraft-servers-list.org/id-list/


    static public  void showBalance( Graphics2D g,Player player){
        g.setColor(Color.BLACK);
        g.fillRect(0,0,128,128);


        g.setColor(Color.CYAN);
        g.fillRoundRect(4,2,120,30,8,8);
        g.setColor(Color.white);
        g.drawRoundRect(4,2,120,30,8,8);

        //      残高を得る
        double balance = MappRenderer.vaultManager.getBalance(player.getUniqueId());

        g.setColor(Color.RED);


        g.setFont(new Font( "SansSerif", Font.PLAIN,13));
        g.drawString(player.getName()+"の残高",10,14);



        g.setFont(new Font( "SansSerif", Font.PLAIN,15));
        MappDraw.drawOutlineString(g,Utility.getPriceString(balance),Color.YELLOW,Color.BLACK,10,30);





        showItemBank(g,player);
        showOrder(g,player);

    }

    static public  void showOrder( Graphics2D g,Player player) {


        g.setColor(new Color(50,50,100,255));
        g.drawRoundRect(4,85,120,40,8,8);


        ArrayList<MarketData.OrderInfo> orders = plugin.data.getOrderOfUser(player, player.getUniqueId().toString());
        if (orders == null) {
            return;
        }
        if (orders.size() == 0) {
            return;
        }
        g.setColor(new Color(100,100,255,255));
        g.setFont(new Font( "SansSerif", Font.PLAIN,11));


        g.drawString("注文件数:"+orders.size()+"件",10,97);


        long buyAmount = 0L;
        long sellAmount = 0L;
        double buyTotal = 0.0D;
        long sellTotal = 0L;
        String orderPlyer = "";
        for (MarketData.OrderInfo order : orders) {
            MarketData.ItemIndex itemIndex = plugin.data.getItemPrice(order.item_id);
            if (order.isBuy) {
                buyAmount += order.amount;
                buyTotal += order.price * order.amount;
            } else {
                sellAmount += order.amount;
                sellTotal += itemIndex.price * order.amount;
                orderPlyer = order.player;
            }
        }

        g.setColor(new Color(100,100,255,255));
        g.setFont(new Font( "SansSerif", Font.PLAIN,11));

        g.drawString("買:"+Utility.getPriceString(buyTotal),10,110);
        g.drawString("売:"+Utility.getPriceString(sellTotal),10,122);
    }
    static public  void showItemBank( Graphics2D g,Player player) {

        g.setColor(Color.GREEN);
        g.drawRoundRect(4,38,120,42,8,8);


        g.setFont(new Font( "SansSerif", Font.BOLD,14));
        g.drawString("アイテムバンク",10,52);





        String sql = "select * from user_assets_history where uuid = '" + player.getUniqueId().toString() + "' order by id desc limit 2;";

        UserData userData = new UserData(plugin);


        ArrayList<UserData.UserAssetsHistory> his = userData.getAssetHistory(sql);
        if (his.size() == 0) {
            return ;
        }


        UserData.UserAssetsHistory today = ((UserData.UserAssetsHistory)his.get(0));

        g.setColor(Color.CYAN);
        g.setFont(new Font( "SansSerif", Font.BOLD,13));
        g.drawString(Utility.getItemString(today.total_amount),10,65);


        g.setColor(Color.YELLOW);
        g.setFont(new Font( "SansSerif", Font.BOLD,13));
        g.drawString(Utility.getPriceString(today.estimated_value),10,77);



    }

    static void createBalanceApp(){
        MappRenderer.draw( "balance", 0, (String key,int mapId, Graphics2D g) -> {


            //      画面更新をする
            return true;
        });
        MappRenderer.displayTouchEvent("balance", (String key, int mapId, Player player, int x, int y) -> {

            //////////////////////////////////////////////
            //  Get Graphics context for drawing
            //  描画用コンテキスト取得
            Graphics2D g = MappRenderer.getGraphics(mapId);
            if(g == null){
                return false;
            }

            showBalance(g,player);

            if(y < 40){

                player.chat("/atm");
            }else if (y< 85){

                player.chat("/mib");

            }else{

                player.chat("/mce order");
            }


            //    true -> call drawing logic :描画更新
            return true;
        });

        MappRenderer.draw( "balance", 0, (String key,int mapId, Graphics2D g) -> {


            //      画面更新をする
            return true;
        });

        MappRenderer.plateEvent("balance", (String key, int mapId,Player player) -> {
            Graphics2D g = MappRenderer.getGraphics(mapId);

            g.fillRect(0,0,128,128);
            showBalance(g,player);



            return true;
        });

    }

    static HashMap<Integer,Integer> gameDataMap = new HashMap<Integer, Integer>();
    public static void registerFuncs(){


        createBalanceApp();


        //      "time" -> 時計
        MappRenderer.draw( "time", 20*60, (String key,int mapId, Graphics2D g) -> {

            //      背景を黒に
            g.setColor(Color.BLACK);
            g.fillRect(0,0,width,height);

            LocalDateTime now = LocalDateTime.now();
            String time = DateTimeFormatter.ofPattern("HH:mm:ss").format(now);

            g.setColor(Color.RED);
            g.setFont(new Font( "SansSerif", Font.BOLD ,20 ));
            g.drawString(time,10,60);

            //      画面更新をする
            return true;
        });





        ArrayList<MarketData.ItemIndex> items = data.getItemIndexList("select * from item_index order by id;");

        int     itemmax = items.size();
        Bukkit.getServer().broadcastMessage("itemIndex: "+itemmax);
        //
        for (int n = 0;n < itemmax;n++){
            int no = items.get(n).id;
            MappRenderer.draw( "price:"+no,0,(String key,int mapId,Graphics2D g) -> {
                return drawPrice(g,getId(key));
            });

            MappRenderer.displayTouchEvent("price:"+no,(String key,int mapId,Player player, int x,int y) ->{
                String[] item = key.split(":");
                player.chat("/mce price "+item[1]);
                return false;
            });


            MappRenderer.draw( "buy:"+no,0,(String key,int mapId,Graphics2D g) -> {
                return drawBuy(g,getId(key));
            });

            MappRenderer.displayTouchEvent("buy:"+no,(String key,int mapId,Player player, int x,int y) ->{
                String[] item = key.split(":");
                int item_id = Integer.parseInt(item[1]);

                MarketData.ItemIndex index = data.getItemPrice(item_id);

                player.chat("/mce buy "+item_id + " "+index.lot);
                return false;
            });

            MappRenderer.draw( "sell:"+no,0,(String key,int mapId,Graphics2D g) -> {
                return drawSell(g,getId(key));
            });

            MappRenderer.displayTouchEvent("sell:"+no,(String key,int mapId,Player player, int x,int y) ->{
                String[] item = key.split(":");
                int item_id = Integer.parseInt(item[1]);

                MarketData.ItemIndex index = data.getItemPrice(item_id);
                player.chat("/mce sell "+ item_id +" "+index.lot);
                return false;
            });
            MappRenderer.draw( "chart:"+no, 0,(String key,int mapId,Graphics2D g) -> {
                return drawChart(g,getId(key));
            });

        }


    }
    static int getId(String key){
        String[] spilit = key.split(":");
        return Integer.parseInt(spilit[1]);
    }

    //      現在値を表示
    static boolean drawPrice(Graphics2D g,int id){

        MarketData.ItemIndex item = data.getItemPrice(id);
        if(item == null){
            return false;
        }

        //      背景を黒に
        g.setColor(Color.GRAY);
        g.fillRect(0,0,width,height);


        MappDraw.drawImage(g,"item"+id,64,20,64,64);




        g.setColor(Color.WHITE);


        int titleSize = 20;
        if(item.key.length() > 6){
            titleSize = 12;
        }

        g.setFont(new Font( "SansSerif", Font.BOLD ,titleSize ));

        MappDraw.drawShadowString(g,item.key,Color.WHITE,Color.BLACK,5,20);

 //       g.drawString(item.key,10,20);

      // g.setFont(new Font( "SansSerif", Font.BOLD ,14 ));



        g.setFont(new Font( "SansSerif", Font.BOLD ,20 ));

        Color col = Color.YELLOW;

        String strPrice = Utility.getPriceString(item.price);

        if(item.price > item.last_price){
            col = Color.GREEN;
            strPrice += "↑";
        }else if(item.price > item.last_price){
            col = Color.RED;
            strPrice += "↓";
        }

        g.setColor(col);
//        g.drawString(strPrice,10,50);
        MappDraw.drawShadowString(g,strPrice,col,Color.BLACK,10,50);




        g.setColor(Color.GREEN);
        g.setFont(new Font( "SansSerif", Font.BOLD ,16 ));
        if(item.sell == 0){
            //g.drawString("売り注文なし",4,80);
            MappDraw.drawOutlineString(g,"売り注文なし",Color.RED,Color.black,4,100);
        }else{

            g.drawString("買:"+Utility.getPriceString(item.bid) +"-",4,98 );
        }

        g.setColor(Color.RED);
        if (item.buy == 0) {
            MappDraw.drawOutlineString(g,"買い注文なし",Color.GREEN,Color.black,4,80);


        }else{
            g.setFont(new Font( "SansSerif", Font.BOLD ,16 ));
            g.drawString("売:"+Utility.getPriceString(item.ask) +"-",4,80 );
        }








        drawGauge(g,item.sell,item.buy);




        return true;
    }

    static void drawGauge(Graphics g,int green,int red){

        int x = 12;
        int w = 100;
        int y = 108;
        int h = 10;

        int glen = 100;
        int blen = 0;

        if(red != 0){

            double r = (double)green / ((double)green + (double)red);
            glen = (int)((double)w * r);
            blen = w - glen;
        }

        if(green == 0 && red == 0){
            g.setColor(Color.BLACK);
            g.fillRect(x,y,w,h);
            //      枠
            g.setColor(Color.WHITE);
            g.drawRect(x,y,w,h);
            return;
        }


        g.setColor(Color.GREEN);
        g.fillRect(x,y,glen,h);

        g.setColor(Color.RED);
        g.fillRect(x+glen,y,blen,h);

        //      枠
        g.setColor(Color.WHITE);
        g.drawRect(x,y,w,h);


    }



    //      現在値を表示
    static boolean drawBuy(Graphics2D g,int id){

        MarketData.ItemIndex item = data.getItemPrice(id);
        if(item == null){
            return false;
        }

        //      背景を黒に
        g.setColor(Color.black);
        g.fillRect(0,0,width,height);


        MappDraw.drawImage(g,"money10",10,34,32,32);
        MappDraw.drawImage(g,"arrow_right",64-16,32,32,32);
        MappDraw.drawImage(g,"item"+id,85,32,32,32);

        g.setColor(Color.WHITE);
        g.setFont(new Font( "SansSerif", Font.PLAIN ,14 ));

      //  MappDraw.drawShadowString(g,item.key,Color.WHITE,Color.BLACK,10,20);

        //       g.drawString(item.key,10,20);

        // g.setFont(new Font( "SansSerif", Font.BOLD ,14 ));


        g.setColor(Color.GREEN);
        g.setFont(new Font( "SansSerif", Font.PLAIN ,30 ));
        g.drawString("買うx"+item.lot,8,30);



        g.setFont(new Font( "SansSerif", Font.BOLD ,17 ));

        String onePrice = Utility.getPriceString(item.bid);



        double price =  item.bid*item.lot;
        String stPrice = Utility.getPriceString(price);
        if(stPrice.length() >= 10){
            g.setFont(new Font( "SansSerif", Font.BOLD ,15 ));
        }

//        g.drawString(onePrice+"/1個",0,55);
        g.drawString(stPrice+"/"+item.lot+"個",0,80);


        g.setColor(Color.WHITE);
        g.drawString("のこり",10,100);
        g.setColor(Color.CYAN);
        g.drawString(Utility.getItemString(item.sell),10,120);

        return true;
    }

    //      現在値を表示
    static boolean drawSell(Graphics2D g,int id){

        MarketData.ItemIndex item = data.getItemPrice(id);
        if(item == null){
            return false;
        }

        //      背景を黒に
        g.setColor(Color.black);
        g.fillRect(0,0,width,height);


        MappDraw.drawImage(g,"item"+id,10,32,32,32);
        MappDraw.drawImage(g,"arrow_right",64-16,32,32,32);
        MappDraw.drawImage(g,"money10",90,34,32,32);

        g.setFont(new Font( "SansSerif", Font.BOLD ,17 ));
        g.setColor(Color.RED);

        g.setFont(new Font( "SansSerif", Font.PLAIN ,30 ));
        g.drawString("売るx"+item.lot,8,30);


        g.setFont(new Font( "SansSerif", Font.BOLD ,17 ));

        Color col = Color.RED;


    //    String onePrice = Utility.getPriceString(item.ask);

        double price =  item.ask*item.lot;
        String stPrice = Utility.getPriceString(price);
        if(stPrice.length() >= 10){
            g.setFont(new Font( "SansSerif", Font.BOLD ,15 ));
        }

      //  g.drawString(onePrice+"/1個",0,55);
        g.drawString(stPrice+"/"+item.lot+"個",0,80);



        g.setColor(Color.WHITE);
        g.setFont(new Font( "SansSerif", Font.BOLD ,17 ));
        g.drawString("売れる数",10,100);
        g.setColor(Color.CYAN);
        g.drawString(Utility.getItemString(item.buy),10,120);

        return true;
    }
    //      現在値を表示
    static boolean drawChart(Graphics2D g,int id){

        MarketData.ItemIndex item = data.getItemPrice(id);
        if(item == null){
            return false;
        }

        //      背景を黒に
        g.setColor(Color.BLACK);
        g.fillRect(0,0,width,height);

        g.setColor(Color.GREEN);
        g.setFont(new Font( "SansSerif", Font.BOLD ,20 ));
        g.drawString(item.key,10,20);

         g.setFont(new Font( "SansSerif", Font.PLAIN ,10 ));
        g.drawString("$"+data.getPriceString(item.price),10,50);


        ArrayList<MarketHistory.Candle> candles = data.history.getHourCandles(id);
        double max = 0;
        double min = 999999999;
        for(MarketHistory.Candle candle: candles){
            if(candle.high > max){
                max = candle.high;
            }
            if(candle.low > min){
                min = candle.low;
            }
        }

        int x = width ;
        int y = height/2;
        int x2 = x;
        int y2 = y;
        double ratio = 0.05;
        for(MarketHistory.Candle candle: candles) {

            x -= 10;
            y = (int)(candle.close * ratio) + height / 2;
            g.drawLine(x,y,x2,y2);
            x2 = x;
            y2 = y;
        }


        return true;
    }



    //     例: 時計を描写(ラムダ式で記述)
    static MappRenderer.DrawFunction clock = (String key,int mapId,Graphics2D g) -> {

            //      背景を黒に
            g.setColor(Color.BLACK);
            g.fillRect(0,0,width,height);

            LocalDateTime now = LocalDateTime.now();
            String date = DateTimeFormatter.ofPattern("yyyy/MM/dd").format(now);
            String time = DateTimeFormatter.ofPattern("HH:mm:ss").format(now);

            g.setColor(Color.RED);
            g.setFont(new Font( "SansSerif", Font.BOLD ,18 ));
            g.drawString(date,10,30);
            g.drawString(time,10,60);

            return true;
        };






}
