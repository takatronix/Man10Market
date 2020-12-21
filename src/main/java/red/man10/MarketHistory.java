package red.man10;

import org.bukkit.Bukkit;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class MarketHistory {

    public  MarketPlugin plugin = null;
    public  MarketData data = null;

    class Candle{
        int id;
        int item_id;
        double high;
        double low;
        double open;
        double close;
        int volume;
        int year;
        int month;
        int day;
        int hour;
        int min;
    }



    public boolean update(int item_id ,double price,int year,int month,int day,int hour,int minute){

        long dayVolume = data.getDayVolume(item_id,year,month,day);
        long hourVolume = data.getHourVolume(item_id,year,month,day,hour);
        updateDay(item_id,price,(int)dayVolume,year,month,day);
        updateHour(item_id,price,(int)hourVolume,year,month,day,hour);


        return true;
    }
    boolean updateHour(int item_id ,double price,int volume,int year,int month,int day,int hour){
        //  data.opLog("updateDay");
      //  int hour = 0;
        int minute = 0;
        Candle candle = getHourCandle(item_id,year,month,day,hour);
        if(candle == null){
            // data.opLog("candle == nul -> instert");
            if(!data.mysql.execute("insert into history_hour values(0," + item_id + "," + price + "," + price + "," + price + "," + price + "," + volume + "," + year + "," + month + "," + day + "," + hour + "," + minute + ");")){
                data.opLog("Candle insert error");
                return false;
            }

            return true;
        }
        //   data.opLog("candle -> update");

        candle.volume = volume;

        candle.close = price;
        if(candle.high < price){
            candle.high = price;
        }
        if(candle.low > price){
            candle.low = price;
        }
        //  data.opLog("candle update");

        if(data.mysql.execute("update history_hour set open="+candle.open + ",close="+candle.close + ",high="+candle.high + ",low="+candle.low+",volume="+candle.volume+" where id="+candle.id+";") == false){
            data.opLog("Candle update error");
            return false;
        }

        return true;
    }
    boolean updateDay(int item_id ,double price,int volume,int year,int month,int day){
      //  data.opLog("updateDay");
        int hour = 0;
        int minute = 0;
        Candle candle = getDayCandle(item_id,year,month,day);
        if(candle == null){
           // data.opLog("candle == nul -> instert");
            if(data.mysql.execute("insert into history_day values(0,"+item_id+","+price+","+price+","+price+","+price+","+volume+","+year+","+month+","+day+","+hour+","+minute+");") == false){
                data.opLog("Candle insert error");
                return false;
            }

            return true;
        }

        candle.volume = volume;
        candle.close = price;
        if(candle.high < price){
            candle.high = price;
        }
        if(candle.low > price){
            candle.low = price;
        }
      //  data.opLog("candle update");

        if(data.mysql.execute("update history_day set open="+candle.open + ",close="+candle.close + ",high="+candle.high + ",low="+candle.low+",volume="+candle.volume+" where id="+candle.id+";") == false){
            data.opLog("Candle update error");
            return false;
        }

        return true;
    }

    //      日足を得る
    Candle getYesterdayCandle(int item_id) {

        String sql = "select * from history_day where item_id =" + item_id + " order by id desc limit 1 offset 1;";
        //data.opLog(sql);
        ArrayList<Candle> candles = getCandleList(sql);
        if(candles == null){
            //     data.opLog("取得できない");
            return null;
        }

        int count= candles.size();
        // data.opLog("Candle:"+count);
        if(count >= 1){

            return candles.get(0);
        }
        return null;
    }
    Candle getDayCandle(int item_id,int year,int month,int day) {

        String sql = "select * from history_day where item_id =" + item_id + " and year=" + year + " and  month=" + month + " and  day=" + day + ";";
        //data.opLog(sql);
        ArrayList<Candle> candles = getCandleList(sql);
        if(candles == null){
            //     data.opLog("取得できない");
            return null;
        }

        int count= candles.size();
        // data.opLog("Candle:"+count);
        if(count >= 1){

            return candles.get(0);
        }
        return null;
    }
    Candle getHourCandle(int item_id,int year,int month,int day,int hour) {

        String sql = "select * from history_hour where item_id =" + item_id + " and year=" + year + " and  month=" + month + " and  day=" + day + " and hour="+hour+";";
        //data.opLog(sql);
        ArrayList<Candle> candles = getCandleList(sql);
        if(candles == null){
            //     data.opLog("取得できない");
            return null;
        }

        int count= candles.size();
        // data.opLog("Candle:"+count);
        if(count >= 1){

            return candles.get(0);
        }
        return null;
    }

    boolean saveHourCSV(String path,int item_id,int n){

        ArrayList<Candle> list = getHourCandles(item_id,n);


    //    File dir = new File(path + item_id);
     //   dir.mkdir();


        String filePath =  path + item_id +"/hour.csv";


        Bukkit.getLogger().info("file writing"+filePath);
        try{
            File file = new File(filePath);
            FileWriter filewriter = new FileWriter(file);
            filewriter.write("Date,Open,High,Low,Close,Volume\n");
            for(Candle c : list){
                String date = c.year + "/" +c.month + "/" + c.day+ " "+c.hour;
                String line = date + ","+c.open + ","+c.high+","+c.low +","+c.close +","+c.volume +"\n";
                filewriter.write(line);
            }

            filewriter.close();
        }catch(IOException e){
            System.out.println(e);
            Bukkit.getLogger().info("error:"+e.getMessage());
            return false;
        }
        Bukkit.getLogger().info("done:"+filePath);

        return true;
    }


    boolean saveIndexCSV(String path, MarketData.ItemIndex current){


        File dir = new File(path + current.id);
        dir.mkdir();


        String filePath =  path + current.id +"/index.csv";
        Bukkit.getLogger().info("file writing"+filePath);
        try{
            File file = new File(filePath);
            FileWriter filewriter = new FileWriter(file);

            String line = current.key + "\t"+ Utility.getPriceString(current.price)+"\t"+"http://man10.red/mce/image/item"+current.id+".png";
            filewriter.write(line);

            filewriter.close();
        }catch(IOException e){
            System.out.println(e);
            Bukkit.getLogger().info("error:"+e.getMessage());
            return false;
        }
        Bukkit.getLogger().info("done:"+filePath);

        return true;
    }

    boolean saveDayCSV(String path,int item_id,int n){

        ArrayList<Candle> list = getDayCandles(item_id,n);


     //  File dir = new File(path + item_id);
     //   dir.mkdir();


        String filePath =  path + item_id +"/day.csv";
        Bukkit.getLogger().info("file writing"+filePath);
        try{
            File file = new File(filePath);
            FileWriter filewriter = new FileWriter(file);
            filewriter.write("Date,Open,High,Low,Close,Volume\n");
            for(Candle c : list){
                String date = String.format("%04d/%02d/%02d",c.year,c.month,c.day);
                String line = date + ","+c.open + ","+c.high+","+c.low +","+c.close +","+c.volume +"\n";
                filewriter.write(line);
            }

            filewriter.close();
        }catch(IOException e){
            System.out.println(e);
            Bukkit.getLogger().info("error:"+e.getMessage());
            return false;
        }
        Bukkit.getLogger().info("done:"+filePath);

        return true;
    }


    ArrayList<Candle> getHourCandles(int item_id,int n){
        String sql = "select * from history_hour where item_id =" + item_id +" order by id desc limit "+n+";";
        return getCandleList(sql);
    }
    ArrayList<Candle> getDayCandles(int item_id,int n){
        String sql = "select * from history_day where item_id =" + item_id +" order by id desc limit "+n+";";
        return getCandleList(sql);
    }

    //         キャンドルリストを得る
    ArrayList<Candle> getCandleList(String sql){
        ResultSet rs = data.mysql.query(sql);
        ArrayList<Candle> ret = new ArrayList<Candle>();
        if(rs == null){
            return null;
        }
        try
        {
            while(rs.next())
            {
                Candle candle = new Candle();
                candle.id = rs.getInt("id");
                candle.item_id = rs.getInt("item_id");
                candle.high = rs.getDouble("high");
                candle.low = rs.getDouble("low");
                candle.open = rs.getDouble("open");
                candle.close = rs.getDouble("close");
                candle.year = rs.getInt("year");
                candle.month = rs.getInt("month");
                candle.day = rs.getInt("day");
                candle.hour = rs.getInt("hour");
                candle.min = rs.getInt("min");
                candle.volume = rs.getInt("volume");
                ret.add(candle);
            }
            rs.close();
        }
        catch (SQLException e)
        {
            data.mysql.close();
            return null;
        }
        data.mysql.close();
        return ret;
    }




}
