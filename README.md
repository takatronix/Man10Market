# Minecraft Market Plugin

## 株や為替のようにアイテムを売買するプラグインです

###コマンド

        p.sendMessage("§2§l/mm list - 登録アイテムリストと価格を表示する");
        p.sendMessage("§2§l/mm price (id/key) - (id or Key/手に持ったアイテム)の金額を表示する");
        p.sendMessage("§2§l/mm ordersell [一つあたりの金額] (個数) -  指定した金額で売り注文を出す");

        p.sendMessage("§2§l/mm orderbuy [id/key] [一つあたりの金額] [個数]- 指定した金額で買い注文を出す");

        p.sendMessage("/mm order (user) 注文を表示する");
        p.sendMessage("/mm cancel [order_id] 注文をキャンセルする");
        p.sendMessage("/mm cancellall (userid) 全ての注文をキャンセルする");

        p.sendMessage("/mm orderbuy [id/this] [一つあたりの金額] [個数]- 指定した金額で買い注文を出す");

        p.sendMessage("/mm marketbuy [id/this] [個数] - 成り行き注文（市場価格で購入)");
        p.sendMessage("/mm marketsell [id/this] [個数] - 成り行き注文（市場価格で売り)");
        p.sendMessage("§c--------------------------------");
        p.sendMessage("§c§l/mm register 1)登録名称 2)初期金額 3)ティック(値動き幅) - 手にもったアイテムをマーケットに登録する");
        p.sendMessage("§c/mm unregister - 手にもったアイテムをマーケットから削除する");
