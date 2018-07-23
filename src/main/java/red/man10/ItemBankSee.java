package red.man10;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.UUID;

// 7/23 IK追加 他人のアイテムバンクを見たりセットしたりできるクラス
public class ItemBankSee {
    static ItemBank ib;

    public static void EnableLoad(MarketData data){
        ib = data.itemBank;
    }


    public static void viewOtherMIB(Player viewer,UUID target,String targetname){
        ArrayList<ItemBank.ItemStorage> iblist = ib.getStorageList(target.toString());
        viewer.sendMessage("§6=============§f§lMIB ビューア― §b("+targetname+"さん)§6=============");
        for(ItemBank.ItemStorage ibdata:iblist) {
            String itemname = ib.data.getItemPrice(ibdata.item_id).key;
            long haveamount = ibdata.amount;
            String haveitemMoney = complexJpyBalForm((long)(ib.data.getItemPrice(ibdata.item_id).last_price * haveamount));
            Utility.sendSuggestCommand(viewer,"§e"+ibdata.item_id+": §f"+itemname+" §a"+haveamount+"個所持" +
                    " 推定価値 §e"+haveitemMoney+"円","§eクリックで編集",
                    "/mce ibedit "+targetname+" "+ibdata.item_id);
        }
        viewer.sendMessage("§6=============§f§lMIB ビューア― §b("+targetname+"さん)§6=============");
    }

    public static void viewOtherMIB(Player viewer,UUID target,String id,String targetname){
        if(ib.data.getItemPrice(id) == null){
            viewer.sendMessage(ib.plugin.prefix+"§c§lそのID/KEYは存在しない");
            return;
        }
        ItemBank.ItemStorage ibdata = ib.getItemStorage(target.toString(),ib.data.getItemPrice(id).id);
        viewer.sendMessage("§6=============§f§lMIB ビューア― §b("+targetname+"さん)§6=============");
        String itemname = ib.data.getItemPrice(ibdata.item_id).key;
        long haveamount = ibdata.amount;
        String haveitemMoney = complexJpyBalForm((long)(ib.data.getItemPrice(ibdata.item_id).last_price * haveamount));
        Utility.sendSuggestCommand(viewer,"§e"+ibdata.item_id+": §f"+itemname+" §a"+haveamount+"個所持" +
                        " 推定価値 §e"+haveitemMoney+"円","§eクリックで編集",
                "/mce ibedit "+targetname+" "+ibdata.item_id);
        viewer.sendMessage("§6=============§f§lMIB ビューア― §b("+targetname+"さん)§6=============");
    }

    synchronized public static void setOtherMIB(Player viewer,UUID target,String id,long amount,String targetname){
        if(ib.data.getItemPrice(id) == null){
            viewer.sendMessage(ib.plugin.prefix+"§c§lそのID/KEYは存在しない");
            return;
        }
        if(ib.setItem(target.toString(),ib.data.getItemPrice(id).id,amount)){
            viewer.sendMessage(ib.plugin.prefix+"§f§l"+targetname+"のアイテムバンク"+ib.data.getItemPrice(id).key+"を"+amount+"個にセットした");
        }else{
            viewer.sendMessage(ib.plugin.prefix+"§c§l"+targetname+"のアイテムバンクセットに失敗した");
        }
    }

    private static String complexJpyBalForm(Long val){
        if(val < 10000){
            return String.valueOf(val);
        }
        if(val < 100000000){
            long man = val/10000;
            String left = String.valueOf(val).substring(String.valueOf(val).length() - 4);
            if(Long.parseLong(left) == 0){
                return man + "万";
            }
            return man + "万" + Long.parseLong(left);
        }
        if(val < 100000000000L){
            long oku = val/100000000;
            String man = String.valueOf(val).substring(String.valueOf(val).length() - 8);
            String te = man.substring(0, 4);
            String left = String.valueOf(val).substring(String.valueOf(val).length() - 4);
            if(Long.parseLong(te)  == 0){
                if( Long.parseLong(left) == 0){
                    return oku + "億";
                }else{
                    return oku + "億"+ Long.parseLong(left);
                }
            }else{
                if( Long.parseLong(left) == 0){
                    return oku + "億" + Long.parseLong(te) + "万";
                }
            }
            return oku + "億" + Long.parseLong(te) + "万" + Long.parseLong(left);
        }
        return "Null";
    }

}
