package me.zrageyh.telefono.events;

import me.zrageyh.telefono.Telefono;
import me.zrageyh.telefono.api.TelephoneAPI;
import me.zrageyh.telefono.manager.Database;
import me.zrageyh.telefono.model.Abbonamento;
import me.zrageyh.telefono.model.Contatto;
import me.zrageyh.telefono.model.history.HistoryMessaggio;
import me.zrageyh.telefono.utils.Utils;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.conversation.SimpleConversation;

import java.util.Optional;

public class SendMessage extends SimpleConversation {

    private final Contatto contatto;

    public SendMessage(final Contatto contatto) {
        this.contatto = contatto;
    }

    @Override
    protected Prompt getFirstPrompt() {
        return new Prompt() {
            private final String surnameTarget = contatto.getSurname().equalsIgnoreCase("nessuno") ? "" : contatto.getSurname();
            private final String nameTarget = contatto.getName();

            @Override
            public @NotNull String getPromptText(@NotNull final ConversationContext context) {
                return "§7Scrivi il messaggio da inviare a §f%s %s§7, digita §f\"annulla\" §7per annullare l'invio.".formatted(nameTarget, surnameTarget);
            }

            @Override
            public boolean blocksForInput(@NotNull final ConversationContext context) {
                return true;
            }

            @Override
            public @Nullable Prompt acceptInput(@NotNull final ConversationContext context, @Nullable final String input) {

                final Player player = (Player) context.getForWhom();

                if (input == null || input.equalsIgnoreCase("annulla")) {
                    Messenger.success(player, "Hai annullato l'invio del messaggio.");
                    return null;
                }

                Abbonamento abbonamento = Telefono.getCacheAbbonamento().getCache().getIfPresent(contatto.getSim());
                abbonamento.removeMessage();
                Telefono.getCacheAbbonamento().update(abbonamento);


                final String number = contatto.getNumber();
                final Optional<Player> opt_target = TelephoneAPI.getPlayerByNumber(number);

                final String sim = contatto.getSim();
                final String dateNow = Utils.getDateNow();
                final HistoryMessaggio historyMessaggio = new HistoryMessaggio(sim, number, dateNow, input);
                final HistoryMessaggio historyMessaggioTarget = new HistoryMessaggio(number, sim, dateNow, input);
                historyMessaggioTarget.setRecived(true);

                final Contatto contattoTarget = new Contatto(number, sim, sim);

                if (opt_target.isEmpty()) {
                    historyMessaggio.setLost(true);
                    historyMessaggioTarget.setLost(true);
                    if (!Telefono.getCacheContatti().isSaved(number, sim)) {
                        Database.getInstance().saveContatto(contattoTarget);
                        Telefono.getCacheContatti().put(number, contattoTarget);
                    }

                    Database.getInstance().saveMessaggio(historyMessaggio);
                    Telefono.getCacheHistoryMessaggi().put(sim, historyMessaggio);
                    Telefono.getCacheHistoryMessaggi().put(number, historyMessaggioTarget);
                    Common.tellNoPrefix(player,
                            "&7✉ &f&lSMS: &c%s %s attualmente non è in città e per tanto il messaggio inviato, è stato salvato come perso.".formatted(nameTarget, surnameTarget));
                    return null;
                }

                final Player target = opt_target.get();

                Database.getInstance().saveMessaggio(historyMessaggio);
                Telefono.getCacheHistoryMessaggi().put(sim, historyMessaggio);
                Telefono.getCacheHistoryMessaggi().put(number, historyMessaggioTarget);

                Common.tell(player, "&7✉ &f&lSMS: &7Messaggio inviato a &f%s %s".formatted(nameTarget, surnameTarget));


                final Optional<Contatto> opt_contatto_target = TelephoneAPI.getContattoByNumber(contatto.getNumber(), contatto.getSim());
                if (opt_contatto_target.isEmpty()) {
                    Database.getInstance().saveContatto(contattoTarget);
                    Telefono.getCacheContatti().put(number, contattoTarget);
                    target.sendMessage("§7✉ §f§lSMS: §7Hai un nuovo messaggio da un numero sconosciuto: §f%s".formatted(sim));
                    return null;
                }


                final Contatto objectContattoTarget = opt_contatto_target.get();
                final String namesender = objectContattoTarget.getName();
                final String surnamesender = objectContattoTarget.getSurname();

                Messenger.warn(target, "&7✉ &f&lSMS: &7Hai un nuovo messaggio da &f%s %s".formatted(namesender, surnamesender));
                return Prompt.END_OF_CONVERSATION;

            }
        };
    }

}
