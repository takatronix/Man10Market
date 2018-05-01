package red.man10;

import java.util.UUID;

public class MarketVault {

    private VaultManager vault = null;
    private final MarketPlugin plugin;

    public MarketVault(MarketPlugin plugin) {
        this.plugin = plugin;
        vault = new VaultManager(plugin);
    }

    public boolean withdraw(UUID uuid,double money){
        return this.vault.withdraw(uuid,money);
    }

    public double getBalance(UUID uuid){
        return vault.getBalance(uuid);
    }

    public boolean deposit(UUID uuid,double money){
        return this.vault.deposit(uuid,money);
    }

}
