package me.zrageyh.telefono.command;

import me.zrageyh.telefono.Telefono;
import me.zrageyh.telefono.api.TelephoneAPI;
import me.zrageyh.telefono.manager.Database;
import me.zrageyh.telefono.model.Abbonamento;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.mineacademy.fo.command.SimpleSubCommand;

import java.util.*;
import java.util.concurrent.TimeUnit;

enum TipoAbbonamento {
    BASIC,
    BUSINESS,
    ECONOMY,
    PREMIUM
}

public final class SubCommandAbbonamento extends SimpleSubCommand {

    final Telefono instance;
    private final List<String> abbonamenti = Arrays.asList("basic", "business", "economy", "premium");


    public SubCommandAbbonamento(String perm) {
        super("abbonamento");
        setDescription("Rinnovi l'abbonamento del telefono ad un numero di telefono");
        setUsage("<numero> <abbonamento>");
        setPermission(perm);
        setMinArguments(2);
        setCooldown(10, TimeUnit.SECONDS);
        setCooldownMessage("§cDevi aspettare 10 secondi prima di effettuare un nuovo abbonamento");
        instance = ((Telefono) Telefono.getInstance());
    }

    @Override
    protected void onCommand() {
        checkConsole();

        String number = args[0];
        String subscription = args[1];

        Map<TipoAbbonamento, Abbonamento> mappaAbbonamenti = new HashMap<>() {

            {
                put(TipoAbbonamento.BASIC, new Abbonamento("Basic", 50, 10));
                put(TipoAbbonamento.BUSINESS, new Abbonamento("Business", 150, 30));
                put(TipoAbbonamento.ECONOMY, new Abbonamento("Economy", 300, 60));
                put(TipoAbbonamento.PREMIUM, new Abbonamento("Premium", 1000, 350));
            }
        };

        Optional<Abbonamento> opt_subscription = findSubscription(mappaAbbonamenti, subscription);

        if (opt_subscription.isEmpty()) {
            tellError("&aAbbonamento non trovato scegli tra: " + Arrays.toString(abbonamenti.toArray()).replace("[", "").replace("]", ""));
            return;
        }

        if (!TelephoneAPI.numberExists(number)) {
            tellError("&cIl numero di telefono %s non esiste".formatted(number));
            return;
        }

        Abbonamento abbonamento_new = opt_subscription.get();
        abbonamento_new.setSim(number);

        Telefono.getCacheAbbonamento().getCache().put(number, abbonamento_new);
        Database.getInstance().saveSubscription(abbonamento_new);

        Optional<Player> target = TelephoneAPI.getPlayerByNumber(number);

        if (target.isPresent()) {
            target.get().sendMessage("§9 ");
            target.get().sendMessage("§9 §lɢ-ᴍᴏʙɪʟᴇ");
            target.get().sendMessage("§9 §7ʟ'ᴀʙʙᴏɴᴀᴍᴇɴᴛᴏ ᴅᴇʟ ᴛᴜᴏ ᴛᴇʟᴇғᴏɴᴏ (§9%s§7) è sᴛᴀᴛᴏ ʀɪɴɴᴏᴠᴀᴛᴏ".formatted(number));
            target.get().sendMessage("§9 §7ᴀʙʙᴏɴᴀᴍᴇɴᴛᴏ: §9" + abbonamento_new.getAbbonamento());
            target.get().sendMessage("§9 §7ᴍᴇssᴀɢɢɪ ᴅɪsᴘᴏɴɪʙɪʟɪ: §9" + abbonamento_new.getMessages());
            target.get().sendMessage("§9 §7ᴍɪɴᴜᴛɪ ᴄʜɪᴀᴍᴀᴛᴀ: §9" + abbonamento_new.getCalls());
            target.get().sendMessage("§9 ");
            target.get().playSound(target.get().getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 5F, 1F);
        }

        tellSuccess("&aHai rinnovato l'abbonamento del numero %s a %s".formatted(number, abbonamento_new.getAbbonamento()));
        getPlayer().playSound(getPlayer().getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 5F, 1F);

    }

    public Optional<Abbonamento> findSubscription(Map<TipoAbbonamento, Abbonamento> mappaAbbonamenti, String nomeAbbonamento) {
        nomeAbbonamento = nomeAbbonamento.toUpperCase();
        return switch (nomeAbbonamento) {
            case "BASIC" -> Optional.of(mappaAbbonamenti.get(TipoAbbonamento.BASIC));
            case "BUSINESS" -> Optional.of(mappaAbbonamenti.get(TipoAbbonamento.BUSINESS));
            case "ECONOMY" -> Optional.of(mappaAbbonamenti.get(TipoAbbonamento.ECONOMY));
            case "PREMIUM" -> Optional.of(mappaAbbonamenti.get(TipoAbbonamento.PREMIUM));
            default -> Optional.empty();
        };
    }

    @Override
    protected List<String> tabComplete() {
        return switch (args.length) {
            case 1 -> Telefono.getCacheNumeri().getNumbers();
            case 2 -> completeLastWord(abbonamenti);
            default -> Collections.emptyList();
        };
    }
}
