package red.man10;

import jdk.nashorn.internal.ir.GetSplitState;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.Configuration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.map.MinecraftFont;
import org.bukkit.plugin.java.JavaPlugin;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;


///    (1)
///    プラグインのonEnable()で　DynamicMapRenderer.setup(this)
//     で初期化して設定をロードする
//      pluginsfolder/images/
//      の下に画像をおくと、自動読み込みさあれます

//      (2) onEnable()で描画関数登録

//     (3) 　ボタンをおされたことを検出する場合
//     public void onInteract(PlayerInteractEvent e) {
//
//        //      イベントを通知してやる（ボタン検出用)
//        DynamicMapRenderer.onPlayerInteractEvent(e);
//         //       マップの回転を抑制
//        DynamicMapRenderer.onPlayerInteractEntityEvent(event);

public class DynamicMapRenderer extends MapRenderer {


    ///////////////////////////////////////////////
    //      描画関数インタフェース
    @FunctionalInterface
    public interface DrawFunction{
        boolean draw(String key,int mapId,Graphics2D g);
    }

    //      ボタンクリックイベント
    @FunctionalInterface
    public interface ButtonClickFunction{
        boolean onButtonClicked(String key,int mapId);
    }

    ///////////////////////////////////////////////
    //      "key" ->　関数　をハッシュマップに保存
    static HashMap<String,DrawFunction> drawFunctions = new HashMap<String,DrawFunction>();
    static HashMap<String,Integer> drawRefreshTimeMap = new HashMap<String,Integer>();

    //
    static HashMap<String,ButtonClickFunction> buttonFunctions = new HashMap<String,ButtonClickFunction>();

    //        描画検索用
    static ArrayList<DynamicMapRenderer> renderers = new ArrayList<DynamicMapRenderer>();
    //      描画関数をキーを登録
    //      key: キー func: 描画関数 refreshIntervalTick:自動更新周期(1tick=1/20秒) 0で自動更新しない
    public static void register(String key,int refreshIntervalTick,DrawFunction func){
        drawRefreshTimeMap.put(key,refreshIntervalTick);
        drawFunctions.put(key,func);
    }
    //     ボタンクリックイベントを追加
    public static void registerButtonEvent(String key,ButtonClickFunction func){
        buttonFunctions.put(key,func);
    }


    //     キー
    String key = null;
    int    mapId = -1;
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
            if(func.draw(key,mapId, bufferedImage.createGraphics())){
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
                canvas.drawText( 4,4, MinecraftFont.Font, key + "/map:" + mapId);
                canvas.drawText(4, 14, MinecraftFont.Font, "update:"+updateCount +"/"+this.refreshInterval+"tick");
                canvas.drawText( 4,24, MinecraftFont.Font, "render:"+drawingTime+"ns");
            }
            updateCount++;
        }

        renderCount++;
    }

    static public boolean onPlayerInteractEntityEvent(PlayerInteractEntityEvent e){

        Entity ent = e.getRightClicked();
        if(ent instanceof ItemFrame){
            //  クリックしたアイテムフレームのアイテムがマップでなければ抜け
            ItemFrame frame = (ItemFrame) ent;
            ItemStack item = frame.getItem();
            if(item.getType() != Material.MAP) {
                return false;
            }

            //      DurabilityにいれてあるのがマップID
            int mapId = (int)item.getDurability();
            String key = findKey(mapId);
            if(key == null){
                return false;
            }

           // Bukkit.getLogger().info("DynamicMapRendererMapなので回転を禁止する");
            e.setCancelled(true);
           return true;
        }
        return false;
    }

    //      ボタンイベントを検出する
    static public int onPlayerInteractEvent(PlayerInteractEvent e){

        //      右ボタン以外は無視
        if(e.getAction()!=Action.RIGHT_CLICK_BLOCK) {
            return  -1;
        }
        //
        if(e.getClickedBlock()==null){
            return -1;
        }

        Block clickedBlock = e.getClickedBlock();
        Location loc = clickedBlock.getLocation();
        if(clickedBlock.getType()== Material.WOOD_BUTTON || clickedBlock.getType()== Material.STONE_BUTTON) {

            //     クリックしたボタンの近くのエンティティを集める
            Collection<Entity> entities = getNearbyEntities(loc,1);

            for (Entity en : entities) {
                //     アイテムフレーム以外は無視
                if (en instanceof ItemFrame != true) {
                    continue;
                }
                //     アイテムフレームにあるのはマップか？
                ItemFrame frame = (ItemFrame) en;
                ItemStack item = frame.getItem();
                if(item.getType() != Material.MAP) {
                    continue;
                }

                //      DurabilityにいれてあるのがマップID
                int mapId = (int)item.getDurability();
                String key = findKey(mapId);
                if(key == null){
                    continue;
                }


                //      ボタン用メソッドをコール
                ButtonClickFunction func = buttonFunctions.get(key);
                if(func != null){
                    Bukkit.getLogger().info("ボタンが押された => map key = "+key);
                    if(func.onButtonClicked(key,mapId)){
                        refresh(key);
                    }
                }


            }
        }
        return -1;
    }

    public static List<Entity> getNearbyEntities(Location where, int range) {
        List<Entity> found = new ArrayList<Entity>();

        for (Entity entity : where.getWorld().getEntities()) {
            if (isInBorder(where, entity.getLocation(), range)) {
                found.add(entity);
            }
        }
        return found;
    }
    public static boolean isInBorder(Location center, Location notCenter, int range) {
        int x = center.getBlockX(), z = center.getBlockZ();
        int x1 = notCenter.getBlockX(), z1 = notCenter.getBlockZ();

        if (x1 >= (x + range) || z1 >= (z + range) || x1 <= (x - range) || z1 <= (z - range)) {
            return false;
        }
        return true;
    }





    static public void setup(JavaPlugin plugin){

        loadImages(plugin);
        setupMaps(plugin);
    }



    //////////////////////////////////////////////////////////////////////
    ///    サーバーシャットダウンでレンダラはは初期化されてしまうので
    ///    再起動後にマップを作成する必要がある　
    ///    プラグインのonEnable()で　DynamicMapRenderer.setupMaps(this)
    //     で初期化して設定をロードすること
    static void setupMaps(JavaPlugin plugin) {

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
            renderer.mapId = id;
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

        //      設定データ保存
        config.set("Maps", mlist);
        plugin.saveConfig();

        for (MapRenderer mr : map.getRenderers()) {
            map.removeRenderer(mr);
        }

       DynamicMapRenderer renderer = new DynamicMapRenderer();
       renderer.key = key;
       renderer.refreshOnce = true;
       renderer.mapId = mapId;
       map.addRenderer(renderer);

       ItemMeta im = m.getItemMeta();
       im.addEnchant(Enchantment.DURABILITY, 1, true);
       m.setItemMeta(im);
       m.setDurability(map.getId());

       //       識別用に保存
       renderers.add(renderer);

       return m;
    }

    //      mapIdからキーを検索
    static String findKey(int mapId){
        for(DynamicMapRenderer renderer:renderers){
            if(renderer.mapId == mapId){
                return renderer.key;
            }
        }
        return null;
    }


    //      描画する
    //      一致したキーの数を返す
    static int refresh(String key){

        if(key == null){
            return 0;
        }
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


    //      イメージマップ　
    static HashMap<String,BufferedImage> imageMap =  new HashMap<String,BufferedImage>();


    ///////////////////////////////////////////////////
    //      プラグインフォルダの画像を読み込む
    static public int loadImages(JavaPlugin plugin) {

        imageMap.clear();
        int ret = 0;
        File folder = new File(plugin.getDataFolder(), File.separator + "images");
        Bukkit.getLogger().info(folder.toString());
        File[] files = folder.listFiles();
        for (File f : files) {
            if (f.isFile()){
                String filename = f.getName();

                if(filename.substring(0,1).equalsIgnoreCase(".")){
                    continue;
                }

                String key = filename.substring(0,filename.lastIndexOf('.'));
                BufferedImage image = null;

                try {
                    image = ImageIO.read(new File(f.getAbsolutePath()));
                    imageMap.put(key,image);
                    Bukkit.getLogger().info((key)+" registered.");
                    ret++;

                } catch (Exception e) {
                    e.printStackTrace();
                    image = null;
                }
            }
        }

        return ret;
    }

    static BufferedImage image(String  key){
        return imageMap.get(key);
    }
}