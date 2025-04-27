package me.zrageyh.telefono.model.history;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class HistoryChiamata extends Cronologia {


    private boolean recived;


    public HistoryChiamata(final String sim, final String number, final String date, final boolean isLost) {
        super(sim, number, date, isLost);
    }

    public HistoryChiamata(final String sim, final String number, final String date, final boolean isLost, final boolean recived) {
        super(sim, number, date, isLost);
        this.recived = recived;
    }

    public String getTextFormat() {
        return recived ? "§7[§a✔§7] §7[§e←§7] §f%s §7- §f%s ".formatted(getDate(), getFullName()) : "§7[§a✔§7] §7[§e→§7] §f%s §7- §f%s".formatted(getDate(), getFullName());
    }

    public String getTextFormatLost() {
        return recived ? "§7[§c❌§7] §7[§e←§7] §f%s §7- §f%s ".formatted(getDate(), getFullName()) : "§7[§c❌§7] §7[§e→§7] §f%s §7- §f%s".formatted(getDate(), getFullName());
    }


}
