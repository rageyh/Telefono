package me.zrageyh.telefono.events;

import me.zrageyh.telefono.Telefono;
import me.zrageyh.telefono.api.TelephoneAPI;
import me.zrageyh.telefono.manager.Database;
import me.zrageyh.telefono.model.Abbonamento;
import me.zrageyh.telefono.model.Call;
import me.zrageyh.telefono.model.Contatto;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.conversation.SimpleConversation;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public class StartCall extends SimpleConversation {

    private final Contatto contattoCalled;
    private final Contatto contattoWhoCall;
    private final Player player;
    private final Player target;
    private final String fullNameWhoCall;
    private final String fullNameCalled;
    private final Abbonamento abbonamento;
    private final Call call;

    private final AtomicBoolean callEnded = new AtomicBoolean(false);
    private final ReentrantLock lock = new ReentrantLock();

    public StartCall(final Player player, final Call call) {
        this.call = call;
        contattoCalled = call.getContattoCalled();
        contattoWhoCall = call.getContattoWhoCall();
        this.player = player;
        target = contattoCalled.getPlayer();
        abbonamento = call.getAbbonamento();
        fullNameWhoCall = contattoWhoCall.getFullName();
        fullNameCalled = contattoCalled.getFullName();
        start();
    }

    private void start() {
        // PERFORMANCE FIX: Usa CallBillingManager centralizzato invece di task individuali
        Telefono.getServiceManager().getCallBillingManager().registerCallForBilling(
            contattoWhoCall.getSim(),
            call,
            this::endCallSafely
        );
    }


    /* Thread-safe end call con cleanup completo */
    private void endCallSafely() {
        if (!callEnded.compareAndSet(false, true)) {
            return; // Già terminata
        }

        lock.lock();
        try {
            cleanup();

            // Salva cronologia in modo asincrono
            Database.getInstance().saveChiamata(call.getHistoryChiamata(false))
                .thenRun(() -> {
                    Telefono.getCacheHistoryChiamate().put(contattoCalled.getSim(), call.getHistoryChiamata(false));
                    Telefono.getCacheHistoryChiamate().put(contattoCalled.getNumber(), call.getHistoryChiamataReverse(false));
                })
                .exceptionally(throwable -> {
                    Common.error(throwable, "Errore salvataggio cronologia chiamata");
                    return null;
                });

            Telefono.getCacheAbbonamento().update(abbonamento);
            Telefono.getCacheChiamata().removeData(contattoCalled.getSim());

            // Notifica giocatori
            if (player != null && player.isOnline()) {
                Common.tellNoPrefix(player, "&7✉ &f&lCHIAMATA &8» &cLa chiamata con %s è terminata ed è stata salvata nel registro chiamate".formatted(contattoCalled.getFullName()));
            }
            if (target != null && target.isOnline()) {
                Common.tellNoPrefix(target, "&7✉ &f&lCHIAMATA &8» &cLa chiamata con %s è terminata ed è stata salvata nel registro chiamate".formatted(contattoWhoCall.getFullName()));
            }
        } finally {
            lock.unlock();
        }
    }

    /* Cleanup dei task per evitare memory leak */
    public void cleanup() {
        Telefono.getServiceManager().getCallBillingManager().unregisterCallBilling(contattoWhoCall.getSim());
    }

    @Override
    protected Prompt getFirstPrompt() {
        return new Prompt() {
            @Override
            public @NotNull String getPromptText(@NotNull final ConversationContext context) {
                if (player.equals(contattoWhoCall.getPlayer())) {
                    return "§a%s ha accettato la tua chiamata".formatted(fullNameCalled);
                } else {
                    return "§aHai accettato la chiamata di %s".formatted(fullNameWhoCall);
                }
            }

            @Override
            public boolean blocksForInput(@NotNull final ConversationContext context) {
                return true;
            }

            @Override
            public @Nullable Prompt acceptInput(@NotNull final ConversationContext context, @Nullable final String input) {
                final Player player = (Player) context.getForWhom();

                if (callEnded.get() || !Telefono.getCacheChiamata().containsNumber(contattoWhoCall.getSim())) {
                    player.chat(input);
                    return Prompt.END_OF_CONVERSATION;
                }

                if (player.equals(contattoWhoCall.getPlayer())) {
                    final Player target = contattoCalled.getPlayer();
                    Common.tellNoPrefix(player, "&7✉ &f&lCHIAMATA &8» &fTu: &7%s".formatted(input));
                    if (target != null && target.isOnline()) {
                        Common.tellNoPrefix(target, "&7✉ &f&lCHIAMATA &8» &f%s: &7%s".formatted(fullNameWhoCall, input));
                    }
                } else {
                    final Player target = contattoWhoCall.getPlayer();
                    Common.tellNoPrefix(player, "&7✉ &f&lCHIAMATA &8» &fTu: &7%s".formatted(input));
                    if (target != null && target.isOnline()) {
                        Common.tellNoPrefix(target, "&7✉ &f&lCHIAMATA &8» &f%s: &7%s".formatted(fullNameCalled, input));
                    }
                }
                return this;
            }
        };
    }
}
