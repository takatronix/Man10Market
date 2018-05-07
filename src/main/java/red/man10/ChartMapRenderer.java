package red.man10;

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
import java.util.ArrayList;
import java.util.List;


///   Chart描画用レンダラ
public class ChartMapRenderer extends MapRenderer {

    String key = null;

    //          オフスクリーンバッファを作成する
    BufferedImage bufferedImage = new BufferedImage(128,128,BufferedImage.TYPE_INT_RGB);
    Color backgroundColor = new Color(50,20,30);

//    public JavaPlugin plugin = null;

    public boolean updateMapFlag = false;
    int updateCount = 0;
    public int drawCount = 0;
    public boolean debugMode = true;

    public void updateBuffer(String name,String price){

        Graphics2D gr = bufferedImage.createGraphics();

       gr.setBackground(backgroundColor);
        gr.setColor(backgroundColor);
        gr.fillRect(0,0,bufferedImage.getWidth(),bufferedImage.getHeight());

        gr.setColor(Color.red);
        gr.setFont(new Font( "SansSerif", Font.PLAIN, 14 ));

        gr.drawString(name,10,16);
        gr.drawString(price,50,50);

    }

    public void drawMap(String key,String param){

        updateBuffer(key,param);

        //
        this.updateMapFlag = true;

    }


    //////////////////////////////////////////////////////////////////////
    //    このイベントは本人がマップを持った場合1tick
    //    他者がみる場合は1secの周期でよばれるため高速描写する必要がある
    //    実際の画像はbufferdImageに作成し、このイベントで転送する
    @Override
    public void render(MapView map, MapCanvas canvas, Player player) {

        //     オフスクリーンバッファからコピー
        if(updateMapFlag){
            canvas.drawImage(0,0,bufferedImage);
            updateMapFlag  = false;
            Bukkit.getLogger().info("update:"+key);
            if(debugMode){
                //      描画回数を表示(debug)
                canvas.drawText(20, 20, MinecraftFont.Font, "Draw:"+drawCount);
                canvas.drawText( 20,40, MinecraftFont.Font, key);
            }
            updateCount++;
        }

        drawCount++;
    }

    //////////////////////////////////////////////////////////////////////
    ///    サーバーシャットダウンでレンダラはは初期化されてしまうので
    ///    再起動後にマップを作成する必要がある　
    ///    プラグインのonEnable()で　ChartMapRenderer.setupMaps(this)
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
            String[] split = ids.split(", ");
            int id = Integer.parseInt(split[0]);
            String key = split[1];

            //     mapIDから新規にマップを作成する
            MapView map = Bukkit.getMap((short) id);
            if (map == null) {
                map = Bukkit.createMap(Bukkit.getWorlds().get(0));
            }
            for (MapRenderer mr : map.getRenderers()) {
                map.removeRenderer(mr);
            }

            ChartMapRenderer renderer = new ChartMapRenderer();
            renderer.key = key;
            renderer.initialize(map);

            //     レンダラを追加
            map.addRenderer(renderer);

            //     描画用に保存
            renderers.add(renderer);

            nmlist.add(key);// STOPSHIP: 2018/05/07
        }

        //      マップを保存し直す
        config.set("Maps", nmlist);
        plugin.saveConfig();
    }

    //////////////////////////////////////////
    /// 　   描画用マップを取得する
    ///     key : 描画を切り替えるためのキー
   static public ItemStack getMapItem(JavaPlugin plugin,String key) {

        Configuration config = plugin.getConfig();

        List<String> mlist = config.getStringList("Maps");

        ItemStack m = new ItemStack(Material.MAP);
        MapView map = Bukkit.createMap(Bukkit.getWorlds().get(0));

        //      mapID,keyのフォーマットで必要データを保存
        mlist.add((int) map.getId() + ", " + key);

        //      設定データ保存
        config.set("Maps", mlist);
        plugin.saveConfig();

        for (MapRenderer mr : map.getRenderers()) {
            map.removeRenderer(mr);
        }

       ChartMapRenderer renderer = new ChartMapRenderer();
       renderer.key = key;
       map.addRenderer(renderer);

       ItemMeta im = m.getItemMeta();
       im.setDisplayName("§2§o" + key);
       im.addEnchant(Enchantment.DURABILITY, 1, true);
       m.setItemMeta(im);
       m.setDurability(map.getId());

       renderer.updateMapFlag = true;
       renderers.add(renderer);

       return m;
    }

    //      描画する
    //      一致したキーの数を返す
    static int draw(String key,String param){

        int ret = 0;
        for(ChartMapRenderer renderer:renderers){
            if(renderer.key.equals(key)){

                renderer.drawMap(key,param);
                ret++;
            }
        }

        return ret;
    }
    static public void updateAll() {

        for(ChartMapRenderer renderer:renderers){
            renderer.updateMapFlag = true;
        }

        return ;
    }

    //        描画検索用
    static ArrayList<ChartMapRenderer> renderers = new ArrayList<ChartMapRenderer>();
}