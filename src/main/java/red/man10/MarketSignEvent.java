package red.man10;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

import java.sql.ResultSet;
import java.sql.SQLException;

public class MarketSignEvent {
    private final MarketPlugin plugin;
    private final MarketSignTimer timer;
    public MarketSignEvent(MarketPlugin plugin) {
        this.plugin = plugin;
        this.timer = new MarketSignTimer(plugin);
        this.timer.start();
        Signdataput();
    }

    public boolean Signcreate(Player p, Location loc, String line_one){
        if(Signcontain(loc)){
            plugin.showError(p,"その座標の看板はすでに登録されています");
            loc.getBlock().breakNaturally();
            return false;
        }
        if (!(loc.getBlock().getState() instanceof Sign)) {
            plugin.showError(p,"その座標のブロックは看板ではありません");
            loc.getBlock().breakNaturally();
            return false;
        }
        String[] line1 = line_one.split(":",2);
        if(line1[0] == null){
            plugin.showError(p,"空のアイテムは登録できません");
            loc.getBlock().breakNaturally();
            return false;
        }
        MarketData.ItemIndex item = plugin.data.getItemPrice(line1[0]);
        if(item == null) {
            plugin.showError(p,"このアイテムは売買できません");
            loc.getBlock().breakNaturally();
            return false;
        }
        String world = loc.getWorld().getName();
        Double x = loc.getX();
        Double y = loc.getY();
        Double z = loc.getZ();
        String sql = "INSERT INTO sign_location (world, x, y, z) VALUES ('"+world+"', "+x+", "+y+", "+z+");";
        plugin.data.mysql.execute(sql);
        this.timer.signs.put(loc,line1);
        plugin.showMessage(p,"看板を登録しました。");
        return true;
    }
    public boolean Signdelete(Player p, Location loc){
        String world = loc.getWorld().getName();
        Double x = loc.getX();
        Double y = loc.getY();
        Double z = loc.getZ();
        if(!Signcontain(loc)){
            plugin.showError(p,"その座標の看板は存在しません");
            return false;
        }
        String sql = "DELETE FROM sign_location WHERE world = '"+world+"' AND x = "+x+" AND y = "+y+" AND z = "+z+";";
        plugin.data.mysql.execute(sql);
        plugin.showMessage(p,"看板を削除しました。");
        loc.getBlock().breakNaturally();
        return true;
    }
    public boolean Signdelete(Location loc){
        String world = loc.getWorld().getName();
        Double x = loc.getX();
        Double y = loc.getY();
        Double z = loc.getZ();
        if(!Signcontain(loc)){
            return false;
        }
        String sql = "DELETE FROM sign_location WHERE world = '"+world+"' AND x = "+x+" AND y = "+y+" AND z = "+z+";";
        plugin.data.mysql.execute(sql);
        loc.getBlock().breakNaturally();
        return true;
    }
    public boolean Signcontain(Location loc) {
        String world = loc.getWorld().getName();
        Double x = loc.getX();
        Double y = loc.getY();
        Double z = loc.getZ();
        String sql = "SELECT * FROM sign_location WHERE world = '"+world+"' AND x = "+x+" AND y = "+y+" AND z = "+z+";";
        ResultSet rs = plugin.data.mysql.query(sql);
        try {
            if(rs.next()) {
                // 一致するデータが見つかった
                return true;
            }
            return false;
        } catch (SQLException e1) {
            e1.printStackTrace();
            return false;
        }
    }
    public boolean Signdataput(){
        String sql = "SELECT * FROM sign_location;";
        ResultSet rs = plugin.data.mysql.query(sql);
        try {
            for(int i = 0; i <= plugin.data.mysql.countRows("sign_location"); i++) {
                if (rs.next()) {
                    String world = rs.getString("world");
                    Double x = rs.getDouble("x");
                    Double y = rs.getDouble("y");
                    Double z = rs.getDouble("z");
                    Location loc = new Location(Bukkit.getWorld(world), x, y, z);
                    String[] line = ((Sign)loc.getBlock().getState()).getLine(1).split(":",2);
                    timer.signs.put(loc, line);
                }else {
                    break;
                }
            }
            return true;
        } catch (SQLException e1) {
            e1.printStackTrace();
            return false;
        }
    }
    public Location toLocation(String world,Double x,Double y,Double z){
        if(Bukkit.getWorld(world)==null){
            return null;
        }
        Location loc = new Location(Bukkit.getWorld(world),x,y,z);
        return loc;
    }
}
