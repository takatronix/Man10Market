package red.man10;



import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import red.man10.MarketData;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;


public class MarketChart {
    static int width = 128;
    static int height = 128;
    public static MarketPlugin plugin = null;
    public static MarketData data = null;


    static int  clickedCount = 0;

// http://www.minecraft-servers-list.org/id-list/

    static HashMap<Integer,Integer> gameDataMap = new HashMap<Integer, Integer>();
    public static void registerFuncs(){



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

        int     itemmax = 40;
        //
        for (int i = 1;i <= itemmax;i++){
            MappRenderer.draw( "price:"+i,0,(String key,int mapId,Graphics2D g) -> {
                return drawPrice(g,getId(key));
            });

        }
        for (int i = 1;i <= itemmax;i++){
            MappRenderer.draw( "chart:"+i, 0,(String key,int mapId,Graphics2D g) -> {
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
        g.setColor(Color.BLACK);
       // g.fillRect(0,0,width,height);


        MappDraw.drawImage(g,"item"+id,0,0,128,128);


        g.setColor(Color.WHITE);
        g.setFont(new Font( "SansSerif", Font.BOLD ,20 ));
        g.drawString(item.key,10,20);

      // g.setFont(new Font( "SansSerif", Font.BOLD ,14 ));




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
        g.drawString(strPrice,10,50);




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
