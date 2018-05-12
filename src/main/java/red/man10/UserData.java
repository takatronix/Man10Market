package red.man10;

import org.bukkit.entity.Player;

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

public class UserData {

    public  MarketPlugin plugin = null;
    public  MarketData data = null;


    //      ユーザーの資産をアップデート
    int updateUserAssetsHistory(Player p){









        Calendar datetime = Calendar.getInstance();
        datetime.setTime(new Date());
        int year = datetime.get(Calendar.YEAR);
        int month = datetime.get(Calendar.MONTH) + 1;
        int day = datetime.get(Calendar.DAY_OF_MONTH);
        String uuid = p.getUniqueId().toString();

        String where = "where uuid = '"+uuid+"' and year ="+year+" and month="+month+" and day="+day;

        //  ユーザーの本日のデータを一旦消去
        data.mysql.execute("delete from user_assets_history "+where+";");

        double bal = plugin.vault.getBalance(UUID.fromString(uuid));




        String sql = "insert into user_assets_history values(0,'"+uuid+"','"+ p.getName()+"',";





        return 1;
    }

}
