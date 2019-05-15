package red.man10;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.UUID;

public class Utility {


    static public String getItemString(long amount){
        return String.format("%,d個",amount);
    }
    static public String getPriceString(double price){
        return String.format("$%,.0f",price);
    }

    static public String getColoredPriceString(double price){
        return String.format("§e§l$%,.0f",price);
    }
    static public String getColoredItemString(long amount){
        return String.format("§b§l%,d個",amount);
    }



    public static String getJpBal(double balance){
        long val = (long)balance;
        String addition = "";
        String form = "万";
        long man = val/10000;
        if(val >= 100000000){
            man = val/100000000;
            form = "億";
            long mann = (val - man * 100000000) / 10000;
            addition = mann + "万";
        }
        return man + form + addition;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////
    //  マインクラフトチャットに、ホバーテキストや、クリックコマンドを設定する関数
    // [例1] sendHoverText(player,"ここをクリック",null,"/say おはまん");
    // [例2] sendHoverText(player,"カーソルをあわせて","ヘルプメッセージとか",null);
    // [例3] sendHoverText(player,"カーソルをあわせてクリック","ヘルプメッセージとか","/say おはまん");
    public static void sendHoverText(Player p, String text, String hoverText, String command){
        //////////////////////////////////////////
        //      ホバーテキストとイベントを作成する
        HoverEvent hoverEvent = null;
        if(hoverText != null){
            BaseComponent[] hover = new ComponentBuilder(hoverText).create();
            hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover);
        }

        //////////////////////////////////////////
        //   クリックイベントを作成する
        ClickEvent clickEvent = null;
        if(command != null){
            clickEvent = new ClickEvent(ClickEvent.Action.RUN_COMMAND,command);
        }

        BaseComponent[] message = new ComponentBuilder(text).event(hoverEvent).event(clickEvent). create();
        p.spigot().sendMessage(message);
    }

    //  マインクラフトチャットに、ホバーテキストや、クリックコマンドサジェストを設定する
    public static void sendSuggestCommand(Player p,String text,String hoverText,String command){

        //////////////////////////////////////////
        //      ホバーテキストとイベントを作成する
        HoverEvent hoverEvent = null;
        if(hoverText != null){
            BaseComponent[] hover = new ComponentBuilder(hoverText).create();
            hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover);
        }

        //////////////////////////////////////////
        //   クリックイベントを作成する
        ClickEvent clickEvent = null;
        if(command != null){
            clickEvent = new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND ,command);
        }

        BaseComponent[] message = new ComponentBuilder(text). event(hoverEvent).event(clickEvent). create();
        p.spigot().sendMessage(message);
    }

    public static void playSound(String uuid,Sound sound){
        Player p = Bukkit.getPlayer(UUID.fromString(uuid));
        if(p == null){
            return;
        }
        if(p.isOnline()){
            p.playSound(p.getLocation(),sound,1,1);
        }
    }

}
