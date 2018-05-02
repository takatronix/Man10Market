package red.man10;

import org.bukkit.Location;
import org.bukkit.block.Sign;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;

public class MarketSignTimer extends Thread {
    private final MarketPlugin plugin;
    HashMap<Location,String[]> signs;
    public MarketSignTimer(MarketPlugin plugin) {
        this.plugin = plugin;
        signs = new HashMap<>();
    }

    public void run() {

        new BukkitRunnable() {
            @Override
            public void run() {
                for(Location loc:signs.keySet()){
                    if (!(loc.getBlock().getState() instanceof Sign)) {
                        plugin.sign.Signdelete(loc);
                        signs.remove(loc);
                        continue;
                    }
                    Sign signb = (Sign)loc.getBlock().getState();
                    String[] itemname = signs.get(loc);
                    if(itemname[1]==null){
                        plugin.getLogger().warning(loc.getWorld().getName()+":"+loc.getX()+":"+loc.getY()+":"+loc.getZ()+"の2行目の個数が数字ではありません");
                        signb.setLine(1,itemname[0]+":1");
                        signb.update();
                        continue;
                    }
                    int bairitu = 0;
                    try {
                        bairitu = Integer.parseInt(itemname[1]);
                    }catch (NumberFormatException e){
                        plugin.getLogger().warning(loc.getWorld().getName()+":"+loc.getX()+":"+loc.getY()+":"+loc.getZ()+"の2行目の個数が数字ではありません");
                        signb.setLine(1,itemname[0]+":1");
                        signb.update();
                        continue;
                    }
                    Double bal = plugin.data.getItemPrice(itemname[0]).price * bairitu;
                    String line = ((Sign)loc.getBlock().getState()).getLine(2).replace("§a","").replace("§c","");
                    if(line.equalsIgnoreCase(bal.toString())){
                        signb.setLine(2,bal.toString());
                        signb.update();
                        continue;
                    }else {
                        Double oldbal = 0.0;
                        try {
                            oldbal = Double.parseDouble(line);
                        }catch (NumberFormatException e){
                            plugin.getLogger().warning(loc.getWorld().getName()+":"+loc.getX()+":"+loc.getY()+":"+loc.getZ()+"の2行目が数字ではありません");
                            signb.setLine(2,bal.toString());
                            signb.update();
                            continue;
                        }
                        if(bal > oldbal) {
                            signb.setLine(2, "§a"+bal.toString());
                        }else{
                            signb.setLine(2, "§c"+bal.toString());
                        }
                        signb.update();
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 100);
    }
}
