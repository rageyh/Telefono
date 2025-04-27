package me.zrageyh.telefono.model.history;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HistoryMessaggio extends Cronologia {

    private final String message;
    private boolean recived;


    public HistoryMessaggio(String sim, String number, String date, String message) {
        super(sim, number, date, false);
        this.message = message;
    }

    public HistoryMessaggio(String sim, String number, String date, String message, boolean isLost, boolean recived) {
        super(sim, number, date, isLost);
        this.message = message;
        this.recived = recived;
    }


    public String getTextFormat() {
        return recived ? "§7[§a✔§7] §7[§e←§7] §f%s §7- §f%s §7ᴛɪ ʜᴀ sᴄʀɪᴛᴛᴏ: §f%s ".formatted(getDate(), getFullName(), message) : "§7[§a✔§7] §7[§e→§7] §f%s §7- §f%s §7ʜᴀɪ sᴄʀɪᴛᴛᴏ: §f%s".formatted(getDate(), getFullName(), message);
    }

    public String getTextFormatLost() {
        return recived ? "§7[§c❌§7] §7[§e←§7] §f%s §7- §f%s §7ᴛɪ ʜᴀ sᴄʀɪᴛᴛᴏ §f%s ".formatted(getDate(), getFullName(), message) : "§7[§c❌§7] §7[§e→§7] §f%s §7- §f%s §7ʜᴀɪ sᴄʀɪᴛᴛᴏ: §f%s".formatted(getDate(), getFullName(), message);
    }


}
