package red.man10;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.omg.CORBA.INTERNAL;

public class MarketCommand implements CommandExecutor {
    private final MarketPlugin plugin;

    //      コンストラクタ
    public MarketCommand(MarketPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getLogger().info("MarketCommand () ->");
    }


    public boolean cancelProc(Player p,String[] args){

        if(!checkPermission(p,Settings.cancelPermission)){
            return false;
        }
        if(args.length == 2){
            return plugin.cancelOrder(p,args[1]);
        }
        p.sendMessage("/mce cancel [order_id] 注文をキャンセルする");
        return false;
    }

    public  boolean cancelAllProc(Player p,String[] args){



        if(!checkPermission(p,Settings.cancelPermission)){
            return false;
        }

        if(args.length == 1){
            return plugin.cancelAll(p,null);
        }

        //  管理者は人の注文をキャンセルできる
        if(p.hasPermission(Settings.adminPermission)){
            if(args.length == 2){
                return plugin.cancelAll(p,args[1]);
            }
        }

        p.sendMessage("/mce cancelall すべての注文をキャンセルする");
        return false;
    }


    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        Player p = (Player) sender;


        if(args.length == 0){


            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
                try {
                    plugin.showMenu(p,0);
                } catch (Exception e) {
                    Bukkit.getLogger().info(e.getMessage());
                    System.out.println(e.getMessage());
                }
            });


            return false;
        }


        String command = args[0];


        //     withdraw
        if(command.equalsIgnoreCase("withdraw")){
            if(!checkPermission(p,Settings.withdraw)){
                return false;
            }

            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
                try {
                    plugin.withdrawAll(p);
                } catch (Exception e) {
                    Bukkit.getLogger().info(e.getMessage());
                    System.out.println(e.getMessage());
                }
            });

            return true;
        }

        //      market broadcast
        if(command.equalsIgnoreCase("updateall")){
            if(!checkPermission(p,Settings.broadcastPermission)){
                return false;
            }
            plugin.updateCurrentPriceListOnBackground(p);
            return true;
        }


        //      market broadcast
        if(command.equalsIgnoreCase("broadcast")){
            if(!checkPermission(p,Settings.broadcastPermission)){
                return false;
            }


            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
                try {
                    MarketData data = new MarketData(plugin);
                    data.news.broadCastNews();
                } catch (Exception e) {
                    Bukkit.getLogger().info(e.getMessage());
                    System.out.println(e.getMessage());
                }
            });

            return true;
        }



        //      upate chartdata
        if(command.equalsIgnoreCase("updatechart")){
            if(!checkPermission(p,Settings.adminPermission)){
                return false;
            }


            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
                try {
                    MarketData data = new MarketData(plugin);
                    data.updateChartData();

                } catch (Exception e) {
                    Bukkit.getLogger().info(e.getMessage());
                    System.out.println(e.getMessage());
                }
            });

            return true;
        }




        if(command.equalsIgnoreCase("news")){
            if(!checkPermission(p,Settings.newsPermission)){
                return false;
            }

            return true;
        }



        //      market open
        if(command.equalsIgnoreCase("open")){
            if(!checkPermission(p,Settings.openPermission)){
                return false;
            }
            plugin.marketOpen(p,true);
            return true;
        }
        //      market close
        if(command.equalsIgnoreCase("close")){
            if(!checkPermission(p,Settings.closePermission)){
                return false;
            }
            plugin.marketOpen(p,false);
            return true;
        }

        //   version
        if(command.equalsIgnoreCase("version" ) || command.equalsIgnoreCase("ver" )){

            p.sendMessage("Version:"+Settings.versionName);
            return true;
        }

        ////////////////
        //    登録
        if(command.equalsIgnoreCase("register")){
            if(!checkPermission(p,Settings.adminPermission)){
                return false;
            }

            if(args.length != 4){
                p.sendMessage("§c§l/mce register 1)登録名称 2)初期金額 3)ティック(値動き幅) - 手にもったアイテムをマーケットに登録する");
                return false;
            }
            plugin.registerItem(p,args[1], Double.parseDouble(args[2]),Double.parseDouble(args[3]));
            return true;
        }

        ////////////////
        //    リスト
        if(command.equalsIgnoreCase("menu")){

            if(args.length == 2){


                Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
                    try {
                        plugin.showMenu(p,Integer.parseInt(args[1]));

                    } catch (Exception e) {
                        Bukkit.getLogger().info(e.getMessage());
                        System.out.println(e.getMessage());
                    }
                });

                return true;
            }


            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
                try {
                    plugin.showMenu(p,0);

                } catch (Exception e) {
                    Bukkit.getLogger().info(e.getMessage());
                    System.out.println(e.getMessage());
                }
            });

            return true;
        }

        if(command.equalsIgnoreCase("log")){
            if(args.length == 2){


                Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
                    try {
                        plugin.showLog(p,null,Integer.parseInt(args[1]));

                    } catch (Exception e) {
                        Bukkit.getLogger().info(e.getMessage());
                        System.out.println(e.getMessage());
                    }
                });

                return true;
            }

            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
                try {
                    plugin.showLog(p,null,0);

                } catch (Exception e) {
                    Bukkit.getLogger().info(e.getMessage());
                    System.out.println(e.getMessage());
                }
            });


            return true;
        }

        if(command.equalsIgnoreCase("map")){
            if(!checkPermission(p,Settings.adminPermission)){
                return false;
            }

            if(args.length == 2){
                plugin.giveMap(p,args[1]);
                return true;
            }

            plugin.giveMap(p,null);
            return true;
        }

        if(command.equalsIgnoreCase("userlog")){


            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
                try {
                    if(args.length == 3){
                        plugin.showLog(p,args[1],Integer.parseInt(args[2]));
                    }
                    if(args.length == 2){
                        plugin.showLog(p,args[1],0);
                    }
                } catch (Exception e) {
                    Bukkit.getLogger().info(e.getMessage());
                    System.out.println(e.getMessage());
                }
            });





            return true;
        }


        //////////////////////////
        //      売り注文
        if(command.equalsIgnoreCase("price")){




            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
                try {

                    if(args.length == 2){
                        plugin.showPrice(p,args[1]);
                    }else{
                        plugin.showPrice(p,null);
                    }



                } catch (Exception e) {
                    Bukkit.getLogger().info(e.getMessage());
                    System.out.println(e.getMessage());
                }
            });


            return true;
        }

        //////////////////////////
        //      指値売り注文
        if(command.equalsIgnoreCase("ordersell") || command.equalsIgnoreCase("os")) {
            if (!checkPermission(p, Settings.orderSellPermission)) {
                return false;
            }

            if (args.length != 4) {
                p.sendMessage("§2§l/mce ordersell [id/key] [一つあたりの金額] [個数] -  指定した金額で売り注文を出す");
                return false;
            }

            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
                try {

                    plugin.orderSell(p, args[1], (double) (int) Double.parseDouble(args[2]), Integer.parseInt(args[3]));

                } catch (Exception e) {
                    Bukkit.getLogger().info(e.getMessage());
                    System.out.println(e.getMessage());
                }
            });

        }

        ////////////////////////////
        //   成り行き売り注文
        if(command.equalsIgnoreCase("marketsell") || command.equalsIgnoreCase("ms")) {
            if(!checkPermission(p,Settings.marketSellPermission)){
                return false;
            }

            if(args.length != 3){
                p.sendMessage("§2§l/mce marketsell [id/key] [個数] - 成り行き注文（買値の高い順に売る)");
                return false;
            }

            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
                try {
                    plugin.marketSell(p,args[1],Integer.parseInt(args[2]));
                } catch (Exception e) {
                    Bukkit.getLogger().info(e.getMessage());
                    System.out.println(e.getMessage());
                }
            });

        }


        if(command.equalsIgnoreCase("itemsell") || command.equalsIgnoreCase("sell")) {
            if(!checkPermission(p,Settings.itemSellPermission)){
                return false;
            }
            if(args.length != 3){
                p.sendMessage("§2§l/mce sell [id/key] [個数] - アイテム成り行き売り（買値の高い順に売る)");
                return false;
            }

            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
                try {
                        plugin.itemSell(p,args[1],Integer.parseInt(args[2]));
                } catch (Exception e) {
                    Bukkit.getLogger().info(e.getMessage());
                    System.out.println(e.getMessage());
                }
            });


        }

        //////////////////////////
        //   指値買い注文
        if(command.equalsIgnoreCase("orderbuy") || command.equalsIgnoreCase("ob") ) {
            if(!checkPermission(p,Settings.orderBuyPermission)){
                return false;
            }
            if(args.length != 4){
                p.sendMessage("§2§l/mce orderbuy [id/key] [一つあたりの金額] [個数] - 指定した金額で買い注文を出す");
                return false;
            }


            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
                try {
                    plugin.orderBuy(p,args[1],(double)(int)Double.parseDouble(args[2]),Integer.parseInt(args[3]));
                } catch (Exception e) {
                    Bukkit.getLogger().info(e.getMessage());
                    System.out.println(e.getMessage());
                }
            });

        }


        ////////////////////////////
        //   成り行き買い注文
        if(command.equalsIgnoreCase("marketbuy") || command.equalsIgnoreCase("mb")) {
            if(!checkPermission(p,Settings.marketBuyPermission)){
                return false;
            }
            if(args.length != 3){
                p.sendMessage("§2§l/mce marketbuy [id/key] [個数] - 成り行き注文（売値の安い順に購入)");
                return false;
            }


            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
                try {
                    plugin.marketBuy(p,args[1],Integer.parseInt(args[2]));
                } catch (Exception e) {
                    Bukkit.getLogger().info(e.getMessage());
                    System.out.println(e.getMessage());
                }
            });

        }

        if(command.equalsIgnoreCase("buy") || command.equalsIgnoreCase("itembuy")) {
            if(!checkPermission(p,Settings.itemBuyPermission)){
                return false;
            }
            if(args.length != 3){
                p.sendMessage("§2§l/mce buy [id/key] [個数] - アイテム注文（売値の安い順に購入)");
                return false;
            }


            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
                try {
                    plugin.itemBuy(p,args[1],Integer.parseInt(args[2]));
                } catch (Exception e) {
                    Bukkit.getLogger().info(e.getMessage());
                    System.out.println(e.getMessage());
                }
            });

        }



        //    アイテム保存
        if(command.equalsIgnoreCase("store")){
            if(!checkPermission(p,Settings.adminPermission)){
                return false;
            }
            if(args.length == 1){
                return plugin.storeItem(p,-1);
            }
            if(args.length == 2){
                return plugin.storeItem(p,Integer.parseInt(args[1]));
            }
            p.sendMessage("§2§l/mce store (個数)- 手に持ったアイテムを倉庫にいれる");
            return false;
        }


        //    注文リスト
        if(command.equalsIgnoreCase("order")){
            if(!checkPermission(p,Settings.orderPermission)){
                return false;
            }
            if(args.length == 2){
                if(p.hasPermission(Settings.adminPermission)){
                    return plugin.showOrder(p,args[1]);
                }else{
                    p.sendMessage("§4§lあなたには権限がない");
                    return false;
                }

            }

            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
                try {
                    plugin.showOrder(p,null);
                } catch (Exception e) {
                    Bukkit.getLogger().info(e.getMessage());
                    System.out.println(e.getMessage());
                }
            });


        }


        //    注文キャンセル
        if(command.equalsIgnoreCase("cancel")){


            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
                try {
                        cancelProc(p,args);

                } catch (Exception e) {
                    Bukkit.getLogger().info(e.getMessage());
                    System.out.println(e.getMessage());
                }
            });

        }




        /////////////////////////////////////////////
        //    全注文キャンセル
        if(command.equalsIgnoreCase("cancelall")){


            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
                try {
                        cancelAllProc(p,args);

                } catch (Exception e) {
                    Bukkit.getLogger().info(e.getMessage());
                    System.out.println(e.getMessage());
                }
            });

        }

        //   アップデート
        if(command.equalsIgnoreCase("update")){
            if(!checkPermission(p,Settings.adminPermission)){
                return false;
            }
            if(args.length == 2){


                Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
                    try {
                        plugin.updatePrice(p,args[1]);
                    } catch (Exception e) {
                        Bukkit.getLogger().info(e.getMessage());
                        System.out.println(e.getMessage());
                    }
                });


            }
            p.sendMessage("/mce update [id/key] 最新価格アップデート");
            return false;
        }
        if(command.equalsIgnoreCase("help")) {
            this.showHelp(p);
            return true;
        }



        return true;

    }

    void showHelp(CommandSender p){
        p.sendMessage("§e============== §d●§f●§a●§e Man10 Market §d●§f●§a● §e===============");
        p.sendMessage("§c-------アイテム登録--------------");
        p.sendMessage("§2§l/mce store (個数)- 手に持ったアイテムを倉庫にいれる");
        p.sendMessage("§2§l/mce restore [id/key] [個数] 倉庫からアイテムを引き出す");
        p.sendMessage("§c--------------------------------");
        p.sendMessage("§2§l/mce list - 登録アイテムリストと価格を表示する");
        p.sendMessage("§2§l/mce price (id/key) - (id/Key/手に持ったアイテム)の金額を表示する");

        p.sendMessage("§c--成り行き注文（現在値で買う)-------------");
        p.sendMessage("/mce buy [個数] - 成り行き注文（市場価格で購入)");
        p.sendMessage("/mce sell [個数] - 成り行き注文（市場価格で売り)");

        p.sendMessage("§c--指値注文(金額を指定して注文を入れる)---------");
        p.sendMessage("§2§l/mce ordersell/os [id/key] [一つあたりの金額] [個数] - 指定した金額で売り注文を出す");
        p.sendMessage("§2§l/mce orderbuy/ob  [id/key] [一つあたりの金額] [個数] - 指定した金額で買い注文を出す");

        p.sendMessage("§c-------注文管理------------------");
        p.sendMessage("/mce order  注文を表示する");
        p.sendMessage("/mce cancel [order_id] 注文をキャンセルする");
        p.sendMessage("/mce cancelall  全ての注文をキャンセルする");
        p.sendMessage("/mce canceltem [id/key]");

        p.sendMessage("/mce withdraw - 売上をすべて引き出す");


        p.sendMessage("§c--------------------------------");
        p.sendMessage("§e created by takatronix http://twitter.com/takatronix");
        p.sendMessage("§e http://man10.red");

        if(p.hasPermission("red.man10.admin")){
            showAdminHelp(p);
        }

    }
    void showAdminHelp(CommandSender p){
        p.sendMessage("§c-----------Admin Commands---------------------");
        p.sendMessage("§c§l/mce order (user/id/key) 注文を表示する");
        p.sendMessage("/mce cancellall  全ての注文をキャンセルする");
        p.sendMessage("§c§l/mce userlog (user) ユーザーの注文履歴");

        p.sendMessage("§c§l/mce register 1)登録名称 2)初期金額 3)ティック(値動き幅) - 手にもったアイテムをマーケットに登録する");
        p.sendMessage("§c/mce unregister - 手にもったアイテムをマーケットから削除する");

    }

    static public boolean checkPermission(Player p,String permission){
        if(p.hasPermission(permission)){
            return true;
        }

        p.sendMessage("§c§lYou don't have permission.");
        return  false;
    }

}
