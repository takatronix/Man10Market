package red.man10;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.logging.Logger;

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

    public boolean update(int item_id ,double price,int volume,int year,int month,int day,int hour,int minute){

        updateDay(item_id,price,volume,year,month,day);
        updateHour(item_id,price,volume,year,month,day,hour);


        return true;
    }
    boolean updateHour(int item_id ,double price,int volume,int year,int month,int day,int hour){
        //  data.opLog("updateDay");
      //  int hour = 0;
        int minute = 0;
        Candle candle = getHourCandle(item_id,year,month,day,hour);
        if(candle == null){
            // data.opLog("candle == nul -> instert");
            if(data.mysql.execute("insert into history_hour values(0,"+item_id+","+price+","+price+","+price+","+price+","+volume+","+year+","+month+","+day+","+hour+","+minute+");") == false){
                data.opLog("Candle insert error");
                return false;
            }

            return true;
        }
        //   data.opLog("candle -> update");


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
     //   data.opLog("candle -> update");


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
