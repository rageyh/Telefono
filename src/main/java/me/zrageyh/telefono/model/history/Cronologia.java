package me.zrageyh.telefono.model.history;

import lombok.Getter;
import lombok.Setter;
import me.zrageyh.telefono.api.TelephoneAPI;
import me.zrageyh.telefono.model.Contatto;

import java.util.Optional;

@Getter
@Setter

public class Cronologia {

    private final String sim;
    private final String number;
    private final String date;
    private int id;
    private boolean isLost = false;

    public Cronologia(final String sim, final String number, final String date, final boolean isLost) {
        this.sim = sim;
        this.number = number;
        this.date = date;
        this.isLost = isLost;
    }


    public String getFullName() {
        final Optional<Contatto> contatto = TelephoneAPI.getContattoByNumber(sim, number);
        if (contatto.isEmpty()) return number;
        return contatto.get().getFullName();
    }


}
