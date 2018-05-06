# Minecraft Market Plugin

## 株や為替のようにアイテムを売買するプラグインです

###コマンド

        p.sendMessage("§2§l/mce list - 登録アイテムリストと価格を表示する");
        p.sendMessage("§2§l/mce price (id/key) - (id or Key/手に持ったアイテム)の金額を表示する");
        p.sendMessage("§2§l/mce ordersell [一つあたりの金額] (個数) -  指定した金額で売り注文を出す");

        p.sendMessage("§2§l/mce orderbuy [id/key] [一つあたりの金額] [個数]- 指定した金額で買い注文を出す");

        p.sendMessage("/mce order (user) 注文を表示する");
        p.sendMessage("/mce cancel [order_id] 注文をキャンセルする");
        p.sendMessage("/mce cancellall (userid) 全ての注文をキャンセルする");

        p.sendMessage("/mce orderbuy [id/this] [一つあたりの金額] [個数]- 指定した金額で買い注文を出す");

        p.sendMessage("/mce marketbuy [id/this] [個数] - 成り行き注文（市場価格で購入)");
        p.sendMessage("/mce marketsell [id/this] [個数] - 成り行き注文（市場価格で売り)");
        p.sendMessage("§c--------------------------------");
        p.sendMessage("§c§l/mce register 1)登録名称 2)初期金額 3)ティック(値動き幅) - 手にもったアイテムをマーケットに登録する");
        p.sendMessage("§c/mce unregister - 手にもったアイテムをマーケットから削除する");
