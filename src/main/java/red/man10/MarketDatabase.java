package red.man10;

public class MarketDatabase {

    static String history_day = "CREATE TABLE `history_day` (\n" +
            "  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,\n" +
            "  `item_id` int(11) DEFAULT NULL,\n" +
            "  `high` double DEFAULT NULL,\n" +
            "  `low` double DEFAULT NULL,\n" +
            "  `open` double DEFAULT NULL,\n" +
            "  `close` double DEFAULT NULL,\n" +
            "  `volume` int(11) DEFAULT NULL,\n" +
            "  `year` int(4) DEFAULT NULL,\n" +
            "  `month` int(2) DEFAULT NULL,\n" +
            "  `day` int(2) DEFAULT NULL,\n" +
            "  `hour` int(2) DEFAULT NULL,\n" +
            "  `min` int(2) DEFAULT NULL,\n" +
            "  PRIMARY KEY (`id`)\n" +
            ") ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8;";

    static String history_hour = "CREATE TABLE `history_hour` (\n" +
            "  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,\n" +
            "  `item_id` int(11) DEFAULT NULL,\n" +
            "  `high` double DEFAULT NULL,\n" +
            "  `low` double DEFAULT NULL,\n" +
            "  `open` double DEFAULT NULL,\n" +
            "  `close` double DEFAULT NULL,\n" +
            "  `volume` int(11) DEFAULT NULL,\n" +
            "  `year` int(4) DEFAULT NULL,\n" +
            "  `month` int(2) DEFAULT NULL,\n" +
            "  `day` int(2) DEFAULT NULL,\n" +
            "  `hour` int(2) DEFAULT NULL,\n" +
            "  `min` int(2) DEFAULT NULL,\n" +
            "  PRIMARY KEY (`id`)\n" +
            ") ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8;";


    static String item_storage = "CREATE TABLE `item_storage` (\n" +
            "  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,\n" +
            "  `uuid` varchar(40) DEFAULT NULL,\n" +
            "  `player` varchar(20) DEFAULT NULL,\n" +
            "  `item_id` int(11) DEFAULT NULL,\n" +
            "  `key` varchar(256) DEFAULT NULL,\n" +
            "  `amount` bigint(20) DEFAULT NULL,\n" +
            "  `datetime` datetime DEFAULT NULL,\n" +
            "  PRIMARY KEY (`id`)\n" +
            ") ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8;";

    static String order_history ="CREATE TABLE `order_history` (\n" +
            "  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,\n" +
            "  PRIMARY KEY (`id`)\n" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8;";

    static String order_tbl = "CREATE TABLE `order_tbl` (\n" +
            "  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,\n" +
            "  `item_id` int(11) NOT NULL,\n" +
            "  `key` varchar(256) NOT NULL DEFAULT '',\n" +
            "  `uuid` varchar(40) NOT NULL DEFAULT '',\n" +
            "  `player` varchar(20) NOT NULL DEFAULT '',\n" +
            "  `price` double NOT NULL,\n" +
            "  `amount` int(11) DEFAULT NULL,\n" +
            "  `initial_amount` int(11) DEFAULT NULL,\n" +
            "  `buy` tinyint(1) DEFAULT NULL,\n" +
            "  `datetime` datetime DEFAULT NULL,\n" +
            "  PRIMARY KEY (`id`)\n" +
            ") ENGINE=InnoDB AUTO_INCREMENT=116 DEFAULT CHARSET=utf8;";
    static String price_history = "CREATE TABLE `price_history` (\n" +
            "  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,\n" +
            "  `item_id` int(11) DEFAULT NULL,\n" +
            "  `price` double DEFAULT NULL,\n" +
            "  `datetime` datetime DEFAULT NULL,\n" +
            "  PRIMARY KEY (`id`)\n" +
            ") ENGINE=InnoDB AUTO_INCREMENT=465 DEFAULT CHARSET=utf8;";

    static String sign_location = "CREATE TABLE `sign_location` (\n" +
            "  `id` int(11) NOT NULL AUTO_INCREMENT,\n" +
            "  `world` varchar(256) DEFAULT NULL,\n" +
            "  `x` double DEFAULT NULL,\n" +
            "  `y` double DEFAULT NULL,\n" +
            "  `z` double DEFAULT NULL,\n" +
            "  PRIMARY KEY (`id`)\n" +
            ") ENGINE=InnoDB AUTO_INCREMENT=70 DEFAULT CHARSET=utf8;";

    static String transaction_log = "CREATE TABLE `transaction_log` (\n" +
            "  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,\n" +
            "  `item` varchar(40) DEFAULT NULL,\n" +
            "  `order_id` int(11) DEFAULT NULL,\n" +
            "  `uuid` varchar(40) DEFAULT NULL,\n" +
            "  `target_uuid` varchar(40) DEFAULT NULL,\n" +
            "  `player` varchar(20) DEFAULT NULL,\n" +
            "  `action` varchar(32) DEFAULT NULL,\n" +
            "  `price` double DEFAULT NULL,\n" +
            "  `amount` int(11) DEFAULT NULL,\n" +
            "  `world` varchar(256) DEFAULT NULL,\n" +
            "  `x` double DEFAULT NULL,\n" +
            "  `y` double DEFAULT NULL,\n" +
            "  `z` double DEFAULT NULL,\n" +
            "  `datetime` datetime DEFAULT NULL,\n" +
            "  PRIMARY KEY (`id`)\n" +
            ") ENGINE=InnoDB AUTO_INCREMENT=4877 DEFAULT CHARSET=utf8;";

    static String user_index="CREATE TABLE `user_index` (\n" +
            "  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,\n" +
            "  `uuid` varchar(40) DEFAULT NULL,\n" +
            "  `name` varchar(20) DEFAULT NULL,\n" +
            "  `company` int(4) DEFAULT NULL,\n" +
            "  `company_owner` varchar(40) DEFAULT NULL,\n" +
            "  `balance` double DEFAULT NULL,\n" +
            "  `lock` tinyint(1) DEFAULT NULL,\n" +
            "  PRIMARY KEY (`id`)\n" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8;";
}


