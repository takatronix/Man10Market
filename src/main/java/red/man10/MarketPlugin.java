package red.man10;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public final class MarketPlugin extends JavaPlugin implements Listener {


    String  prefix = "§f§l[§2§lm§e§lMarket§f§l]";
    VaultManager vault = null;
    MarketData data = null;


    public double buyLimitRatio = 2;
    public double sellLimitRatio = 2;



    // アイテムの値段を表示
    public boolean showPrice(Player p){
        ItemStack item = p.getInventory().getItemInMainHand();

        MarketData.PriceResult ret = data.getItemPrice(p,item);

        if(ret.result == true){
            double st = ret.price * 64;
            showMessage(p,"現在価格:$" + data.getPriceString(ret.price) +"/個 $"+  data.getPriceString(st)+"/1Stack");
            showMessage(p,"§c§l売り注文数(Sell):"+ret.sell +"/§9§l買い注文数(Sell):"+ret.buy);
        }else{
            showError(p,"データ取得失敗");
        }

        return ret.result;
    }

    ///  売り注文を出す
    public boolean orderSell(Player p,double price,int count){


       // showMessage(p,"orderSell" + price + " count:"+count);

        ItemStack item = p.getInventory().getItemInMainHand();


        //      もっているだけ販売
        if(count == -1){
            count = item.getAmount();

        }else{
            if(item.getAmount() < count){
                showError(p,"アイテムを"+count+"個もっていません");
                return false;
            }
        }

        if(data.orderSell(p,item,price,count)){

          //  ItemStack sold = new ItemStack(item);
           // sold.setAmount(count);
            int from = item.getAmount();
            item.setAmount(from - count);
        //    p.getInventory().remove(sold);

            showMessage(p,"売り注文成功 $"+ data.getPriceString(price) + "/"+count+"個" );
            return true;
        }
        showError(p,"売り注文失敗");

        return false;
    }


    //  アイテムリスト表示
    public void showList(Player p){
        data.showItemList(p);
    }


    // アイテム登録
    public void registerItem(Player p,String key,double price,double tick){
        p.sendMessage("アイテム登録");

        ItemStack item = p.getInventory().getItemInMainHand();
        if(item.getAmount() != 1){
            p.sendMessage("§4§lマーケットに登録するアイテムを手に一つもってコマンドを実行してください");
            return;
        }


        if(data.registerItem(p.getUniqueId(),item,key,price,tick)){
           showMessage(p,"マーケットにアイテムを登録しました");
        }else{
            showError(p,"登録に失敗しました");
        }

    }

    void loadConfig(){
        data = new MarketData(this);
        if(data == null){
            if(data == null) {
                Bukkit.getServer().broadcastMessage("DB接続エラー");
                return;
            }
        }
    }





    //      ログメッセージ
    void log(String text){
        getLogger().info(text);
    }
    //     サーバーメッセージ
    void serverMessage(String text){
        Bukkit.getServer().broadcastMessage(prefix +  text);
    }
    //      プレイヤーメッセージ
    void showMessage(Player p,String text){
        p.sendMessage(prefix + text);
    }

    void showError(Player p,String text){
        p.sendMessage("§4§lエラー:" + text);
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        getServer().getPluginManager().registerEvents (this,this);
        getCommand("mm").setExecutor(new MarketCommand(this));

        vault = new VaultManager(this);
        this.saveDefaultConfig();
        this.loadConfig();

        Bukkit.getServer().broadcastMessage(prefix+"Started");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }




}
