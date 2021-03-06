package red.man10;

public class Settings {

    static String versionName = "2019/05/15";
    static String adminPermission = "red.man10.market.admin";

    static String deposit = "red.man10.market.deposit";

    static String showBalanceOther = "red.man10.market.balanceother";

    static String broadcastPermission = "red.man10.market.broadcast";
    static String newsPermission = "red.man10.market.news";


    //      マーケット Open/Close
    static String openPermission = "red.man10.market.open";
    static String closePermission = "red.man10.market.close";

    //    オーダー管理
    static String cancelPermission = "red.man10.market.cancel";
    static String orderPermission = "red.man10.market.order";

    //    アイテム売買(だれれでもOK)
    static String itemBuyPermission = "red.man10.market.itembuy";
    static String itemSellPermission = "red.man10.market.itemsell";

    //      指値注文
    static String marketBuyPermission = "red.man10.market.marketbuy";
    static String marketSellPermission = "red.man10.market.marketsell";

    //      成り行き注文
    static String orderBuyPermission = "red.man10.market.orderbuy";
    static String orderSellPermission = "red.man10.market.ordersell";

    static String itemPayPermission = "red.man10.market.itemPay";

    static String rankingPermission = "red.man10.market.ranking";

    public static String closedMessage = "マーケットはクローズ中です。オープンするまでおまちください";
}
