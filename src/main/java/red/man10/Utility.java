package red.man10;

public class Utility {


    static public String getPriceString(double price){
        return String.format("$%,.0f",price);
    }
    static public String getColorPriceString(double price){
        return String.format("§e§l$%,.0f",price);
    }

    static public String getItemString(long amount){
        return String.format("§b§l$%,d個",amount);
    }

}
