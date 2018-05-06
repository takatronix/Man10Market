package red.man10;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.map.MinecraftFont;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.net.URL;
import java.sql.Time;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.UUID;

import static javax.imageio.ImageIO.read;

public class ChartMapRenderer extends MapRenderer {


    BufferedImage bufferedImage = new BufferedImage(128,128,BufferedImage.TYPE_INT_RGB);

    String target;
    int item_id;

    public int no;
    public MarketPlugin plugin = null;
    public String key = "abcd";
    public int drawCount = 0;
    public String name = "";




    @Override
    public void render(MapView map, MapCanvas canvas, Player player) {
        // here's where you do your drawing - see the Javadocs for the MapCanvas class for
        // the methods you can use

        String name = player.getName().toString();

        int x = map.getCenterX();
        int z = map.getCenterZ();

        String text = target;

        canvas.drawImage(0,0,bufferedImage);

//        String a = map.
        try{
           // canvas.drawImage(0,0, read(new URL("http://man10.red/img/scratch.png")));


        }catch (Exception e){

        }
        LocalDateTime now = LocalDateTime.now();
        String time = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").format(now);
        canvas.drawText(10, 10, MinecraftFont.Font, text);
        canvas.drawText(20, 20, MinecraftFont.Font, "a"+drawCount);

        canvas.drawText (10,30, MinecraftFont.Font,name);
        canvas.drawText (10,60, MinecraftFont.Font,time);
        drawCount++;
    }
}