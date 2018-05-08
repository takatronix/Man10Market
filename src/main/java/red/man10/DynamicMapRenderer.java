package red.man10;

import jdk.nashorn.internal.ir.GetSplitState;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.Configuration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.map.MinecraftFont;
import org.bukkit.plugin.java.JavaPlugin;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;



///    プラグインのonEnable()で　DynamicMapRenderer.setupMaps(this)
//     で初期化して設定をロードすること


public class DynamicMapRenderer extends MapRenderer {

    ///////////////////////////////////////////////
    //      描画関数インタフェース
    @FunctionalInterface
    public interface DrawFunction{
        boolean draw(String key,Graphics2D g);
    }

    ///////////////////////////////////////////////
    //      "key" ->　関数　をハッシュマップに保存
    static HashMap<String,DrawFunction> drawFunctions = new HashMap<String,DrawFunction>();
    static HashMap<String,Integer> drawRefreshTimeMap = new HashMap<String,Integer>();

    //        描画検索用
    static ArrayList<DynamicMapRenderer> renderers = new ArrayList<DynamicMapRenderer>();
    //      描画関数をキーを登録
    //      key: キー func: 描画関数 refreshIntervalTick:自動更新周期(1tick=1/20秒) 0で自動更新しない
    public static void register(String key,int refreshIntervalTick,DrawFunction func){
        drawRefreshTimeMap.put(key,refreshIntervalTick);
        drawFunctions.put(key,func);
    }


    //     キー
    String key = null;

    //   オフスクリーンバッファを作成する
    //   高速化のためこのバッファに描画し、マップへ転送する
    BufferedImage bufferedImage = new BufferedImage(128,128,BufferedImage.TYPE_INT_RGB);

    //      画面リフレッシュサイクル:tick = 1/20秒
    public int refreshInterval = 0;

    //      一度だけ更新する
    public boolean refreshOnce = false;
    //      マップへ転送する
    public boolean updateMapFlag = false;
    //      描画時間
    public long drawingTime = 0;
    //      描画した回数
    public int updateCount = 0;
    //      bukkitからrenderコールされた回数
    public int renderCount = 0;
    //      デバッグ表示フラグ
    public boolean debugMode = true;


    //////////////////////////////////////
    //      描画関数&速度測定
    //////////////////////////////////////
    void draw(){
        //      関数をキーで取り出し実行
        DrawFunction func = drawFunctions.get(key);
        if(func != null){
            long startTime = System.nanoTime();
            //      描画関数をコール
            if(func.draw(key, bufferedImage.createGraphics())){
                updateMapFlag = true;
            }
            this.drawingTime =  System.nanoTime() - startTime;
           // Bukkit.getLogger().info("drawtime:"+key + ":"+drawingTime);
        }
    }


    int tickRefresh = 0;

    /////////////////////////////////
    //      Tickイベント
    //      描画更新があれば反映
    public void onTick(){

        if (refreshOnce){
            refreshOnce = false;
            draw();
        }

        this.refreshInterval =  drawRefreshTimeMap.getOrDefault(key,0);
        if(refreshInterval == 0){
            return ;
        }
        tickRefresh ++;
        //      インターバル期間をこえていたら画面更新
        if(tickRefresh >= refreshInterval) {
            draw();
            tickRefresh = 0;
        }

    }

    //////////////////////////////////////////////////////////////////////
    //    このイベントは本人がマップを持った場合1tick
    //    他者がみる場合は1secの周期でよばれるため高速描写する必要がある
    //    実際の画像はbufferdImageに作成し、このイベントで転送する
    @Override
    public void render(MapView map, MapCanvas canvas, Player player) {

        //     オフスクリーンバッファからコピー
        if(updateMapFlag){
           // Bukkit.getLogger().info("rendering:"+this.key);
            canvas.drawImage(0,0,bufferedImage);
            updateMapFlag  = false;
            if(debugMode){
                //      描画回数を表示(debug)
                canvas.drawText( 4,4, MinecraftFont.Font, key);
                canvas.drawText(4, 14, MinecraftFont.Font, "update:"+updateCount);
                canvas.drawText( 4,24, MinecraftFont.Font, "render:"+drawingTime+"ns");
            }
            updateCount++;
        }

        renderCount++;
    }

    //////////////////////////////////////////////////////////////////////
    ///    サーバーシャットダウンでレンダラはは初期化されてしまうので
    ///    再起動後にマップを作成する必要がある　
    ///    プラグインのonEnable()で　DynamicMapRenderer.setupMaps(this)
    //     で初期化して設定をロードすること
    static public void setupMaps(JavaPlugin plugin) {

        Configuration config = plugin.getConfig();
        if (config.getStringList("Maps").size() == 0) {
            return;
        }
        List<String> mlist = config.getStringList("Maps");
        List<String> nmlist = new ArrayList<String>();
        renderers.clear();

        for (String ids : mlist) {

            //      mapId,keyのデータを取得
            String[] split = ids.split(",");
            int id = Integer.parseInt(split[0]);
            String  key = ids;
            if(split.length == 2){
                 key = split[1];
            }

            //     mapIDから新規にマップを作成する
            MapView map = Bukkit.getMap((short) id);
            if (map == null) {
                map = Bukkit.createMap(Bukkit.getWorlds().get(0));
            }
            for (MapRenderer mr : map.getRenderers()) {
                map.removeRenderer(mr);
            }

            DynamicMapRenderer renderer = new DynamicMapRenderer();

            renderer.refreshOnce = true;
            renderer.refreshInterval = drawRefreshTimeMap.getOrDefault(key,0);
            renderer.key = key;
            renderer.initialize(map);

            //     レンダラを追加
            map.addRenderer(renderer);

            //     描画用に保存
            renderers.add(renderer);

            Bukkit.getLogger().info("setupMap: key:"+key + "id:"+id);
            nmlist.add(ids);
        }

        //      マップを保存し直す
        config.set("Maps", nmlist);
        plugin.saveConfig();


        ////////////////////////////////
        //      タイマーを作成する
        Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                DynamicMapRenderer.onTimerTick();;
            }
        }, 0, 1);

    }

    //////////////////////////////////////////
    /// 　   描画用マップを取得する
    ///     key : 描画を切り替えるためのキー
   static public ItemStack getMapItem(JavaPlugin plugin,String key) {

        Configuration config = plugin.getConfig();

        List<String> mlist = config.getStringList("Maps");

        ItemStack m = new ItemStack(Material.MAP);
        MapView map = Bukkit.createMap(Bukkit.getWorlds().get(0));

        //      mapID,keyのフォーマットで必要データを保存;
       int mapId = (int) map.getId();
        mlist.add(mapId + "," + key);

      // Bukkit.getLogger().info("mapp getMapItem: key:"+mapId + "ikey:"+key);
        //      設定データ保存
        config.set("Maps", mlist);
        plugin.saveConfig();

        for (MapRenderer mr : map.getRenderers()) {
            map.removeRenderer(mr);
        }

       DynamicMapRenderer renderer = new DynamicMapRenderer();
       renderer.key = key;



       renderer.refreshOnce = true;

       map.addRenderer(renderer);

       ItemMeta im = m.getItemMeta();


       im.addEnchant(Enchantment.DURABILITY, 1, true);
       m.setItemMeta(im);
       m.setDurability(map.getId());


       renderers.add(renderer);

       return m;
    }

    //      描画する
    //      一致したキーの数を返す
    static int refresh(String key){

        int ret = 0;
        for(DynamicMapRenderer renderer:renderers){
            if(renderer.key.equals(key)){
                renderer.refreshOnce = true;
                ret++;
            }
        }

        return ret;
    }

    static  void onTimerTick() {
        for(DynamicMapRenderer renderer:renderers){
            renderer.onTick();
        }
    }
    static public void updateAll() {

        Bukkit.getLogger().info("UpdateAll");
        for(DynamicMapRenderer renderer:renderers){
            renderer.refreshOnce = true;
        }

        return ;
    }


}