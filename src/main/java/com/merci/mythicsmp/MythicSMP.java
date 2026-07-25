package com.merci.mythicsmp;

import com.merci.mythicsmp.auction.AuctionGuiManager;
import com.merci.mythicsmp.auction.AuctionManager;
import com.merci.mythicsmp.boss.BossManager;
import com.merci.mythicsmp.commands.AuctionOpenCommand;
import com.merci.mythicsmp.commands.AuctionSellCommand;
import com.merci.mythicsmp.commands.BalanceCommand;
import com.merci.mythicsmp.commands.BossSpawnCommand;
import com.merci.mythicsmp.commands.MythicAdminSpellCommand;
import com.merci.mythicsmp.commands.MythicBalTopCommand;
import com.merci.mythicsmp.commands.MythicElementCommand;
import com.merci.mythicsmp.commands.MythicGiveCommand;
import com.merci.mythicsmp.commands.MythicJobCommand;
import com.merci.mythicsmp.commands.MythicJobTopCommand;
import com.merci.mythicsmp.commands.MythicListCommand;
import com.merci.mythicsmp.commands.MythicSpellsCommand;
import com.merci.mythicsmp.commands.MythicUltimateCommand;
import com.merci.mythicsmp.commands.QuestListCommand;
import com.merci.mythicsmp.economy.EconomyManager;
import com.merci.mythicsmp.elements.ElementManager;
import com.merci.mythicsmp.elements.ElementMenuManager;
import com.merci.mythicsmp.forge.ForgeManager;
import com.merci.mythicsmp.forge.ForgeRecipeRegistry;
import com.merci.mythicsmp.items.ItemRegistry;
import com.merci.mythicsmp.jobs.JobManager;
import com.merci.mythicsmp.listeners.AuctionGuiListener;
import com.merci.mythicsmp.listeners.BossDeathListener;
import com.merci.mythicsmp.listeners.CombatEffectsListener;
import com.merci.mythicsmp.listeners.ElementFirstJoinListener;
import com.merci.mythicsmp.listeners.ElementMenuListener;
import com.merci.mythicsmp.listeners.ForgeListener;
import com.merci.mythicsmp.listeners.GemFusionListener;
import com.merci.mythicsmp.listeners.InfinitePowerGemListener;
import com.merci.mythicsmp.listeners.JobListener;
import com.merci.mythicsmp.listeners.PassiveEquipmentTask;
import com.merci.mythicsmp.listeners.PhoenixHeartListener;
import com.merci.mythicsmp.listeners.QuestProgressListener;
import com.merci.mythicsmp.listeners.RangedEffectsListener;
import com.merci.mythicsmp.listeners.SpearListener;
import com.merci.mythicsmp.listeners.SpellRuneListener;
import com.merci.mythicsmp.listeners.SpellWheelListener;
import com.merci.mythicsmp.listeners.ThrowableItemsListener;
import com.merci.mythicsmp.listeners.UltimateMageGemListener;
import com.merci.mythicsmp.listeners.UltimateSpellMenuListener;
import com.merci.mythicsmp.listeners.UtilityItemsListener;
import com.merci.mythicsmp.listeners.WeaponEffectListener;
import com.merci.mythicsmp.listeners.WitherEyeListener;
import com.merci.mythicsmp.quests.QuestManager;
import com.merci.mythicsmp.scoreboard.ScoreboardManager;
import com.merci.mythicsmp.spells.SpellManager;
import com.merci.mythicsmp.spells.SpellRegistry;
import com.merci.mythicsmp.spells.SpellWheelManager;
import com.merci.mythicsmp.ultimate.UltimateMageManager;
import com.merci.mythicsmp.ultimate.UltimateSpellMenuManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Point d'entrée du plugin. Tout le câblage (registre -> listeners ->
 * commandes) se fait ici, une seule fois au démarrage.
 */
public final class MythicSMP extends JavaPlugin {

    private ItemRegistry itemRegistry;
    private EconomyManager economyManager;
    private AuctionManager auctionManager;
    private QuestManager questManager;
    private JobManager jobManager;
    private ElementManager elementManager;
    private SpellManager spellManager;
    private UltimateMageManager ultimateMageManager;

    @Override
    public void onEnable() {
        this.itemRegistry = new ItemRegistry(this);
        this.economyManager = new EconomyManager(this);
        this.questManager = new QuestManager(this, economyManager);
        this.elementManager = new ElementManager(this);
        ElementMenuManager elementMenuManager = new ElementMenuManager(this, elementManager);

        var pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new ElementFirstJoinListener(this, elementManager, elementMenuManager), this);
        pluginManager.registerEvents(new ElementMenuListener(this, elementManager), this);
        pluginManager.registerEvents(new InfinitePowerGemListener(this, elementManager, elementMenuManager), this);
        getCommand("mythicelement").setExecutor(new MythicElementCommand(elementManager, elementMenuManager));

        SpellRegistry spellRegistry = new SpellRegistry();
        this.spellManager = new SpellManager(this, spellRegistry);
        SpellWheelManager spellWheelManager = new SpellWheelManager(this, elementManager, spellManager);
        pluginManager.registerEvents(new SpellWheelListener(this, spellManager, spellWheelManager), this);
        pluginManager.registerEvents(new SpellRuneListener(this, itemRegistry, elementManager, spellWheelManager), this);
        getCommand("mythicspells").setExecutor(new MythicSpellsCommand(elementManager, spellWheelManager));

        this.ultimateMageManager = new UltimateMageManager(this);
        UltimateSpellMenuManager ultimateSpellMenuManager = new UltimateSpellMenuManager(this, ultimateMageManager);
        pluginManager.registerEvents(new UltimateMageGemListener(this, elementManager, ultimateMageManager), this);
        pluginManager.registerEvents(new UltimateSpellMenuListener(this, ultimateMageManager, ultimateSpellMenuManager), this);
        getCommand("mythicultimate").setExecutor(new MythicUltimateCommand(ultimateMageManager, ultimateSpellMenuManager));
        getCommand("mythicadminspell").setExecutor(new MythicAdminSpellCommand());

        pluginManager.registerEvents(new SpearListener(this, itemRegistry), this);
        pluginManager.registerEvents(new WeaponEffectListener(this), this);
        pluginManager.registerEvents(new GemFusionListener(this), this);
        pluginManager.registerEvents(new PhoenixHeartListener(this), this);
        pluginManager.registerEvents(new CombatEffectsListener(this, itemRegistry), this);
        pluginManager.registerEvents(new RangedEffectsListener(this), this);
        pluginManager.registerEvents(new ThrowableItemsListener(this), this);
        pluginManager.registerEvents(new UtilityItemsListener(this, itemRegistry), this);
        pluginManager.registerEvents(new WitherEyeListener(this), this);
        pluginManager.registerEvents(new QuestProgressListener(this, questManager), this);

        ForgeRecipeRegistry forgeRecipes = new ForgeRecipeRegistry(itemRegistry);
        ForgeManager forgeManager = new ForgeManager(this, forgeRecipes);
        pluginManager.registerEvents(new ForgeListener(forgeManager, questManager), this);

        BossManager bossManager = new BossManager(this, itemRegistry, economyManager, elementManager, ultimateMageManager);
        pluginManager.registerEvents(new BossDeathListener(bossManager), this);
        getCommand("mythicboss").setExecutor(new BossSpawnCommand(bossManager));

        this.auctionManager = new AuctionManager(this);
        AuctionGuiManager auctionGuiManager = new AuctionGuiManager(this, auctionManager);
        pluginManager.registerEvents(new AuctionGuiListener(auctionManager, auctionGuiManager, economyManager), this);
        getCommand("mythicauction").setExecutor(new AuctionOpenCommand(auctionGuiManager));
        getCommand("mythicsell").setExecutor(new AuctionSellCommand(auctionManager, questManager));
        getCommand("mythicbalance").setExecutor(new BalanceCommand(economyManager));
        getCommand("mythicquests").setExecutor(new QuestListCommand(questManager));

        new PassiveEquipmentTask(this).start();

        this.jobManager = new JobManager(this);
        pluginManager.registerEvents(new JobListener(jobManager), this);
        getCommand("mythicjob").setExecutor(new MythicJobCommand(jobManager));
        getCommand("mythicjobtop").setExecutor(new MythicJobTopCommand(jobManager));
        getCommand("mythicbaltop").setExecutor(new MythicBalTopCommand(economyManager));

        new ScoreboardManager(this, economyManager, jobManager, elementManager).start();

        getCommand("mythicgive").setExecutor(new MythicGiveCommand(itemRegistry));
        getCommand("mythiclist").setExecutor(new MythicListCommand(itemRegistry));

        getLogger().info("MythicSMP activé — " + itemRegistry.all().size() + " objets chargés.");
    }

    @Override
    public void onDisable() {
        if (economyManager != null) economyManager.save();
        if (auctionManager != null) auctionManager.save();
        if (questManager != null) questManager.save();
        if (jobManager != null) jobManager.save();
        if (elementManager != null) elementManager.save();
        if (spellManager != null) spellManager.save();
        if (ultimateMageManager != null) ultimateMageManager.save();
        getLogger().info("MythicSMP désactivé.");
    }

    public ItemRegistry getItemRegistry() {
        return itemRegistry;
    }
}
