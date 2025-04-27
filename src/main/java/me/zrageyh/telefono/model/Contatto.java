package me.zrageyh.telefono.model;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;


@Getter
@Setter
public class Contatto extends Telefono {

    private Player player;
    private int id;
    private String number;
    private String name;
    private String surname;
    private boolean inCall;

    public Contatto(final String sim) {
        super(sim);
    }


    public Contatto(final String sim, final String number, final String name) {
        super(sim);
        this.number = number;
        this.name = name;
        surname = "nessuno";
    }

    public Contatto(final int id, final String sim, final String number, final String name, final String surname) {
        super(sim);
        this.id = id;
        this.number = number;
        this.name = name;
        this.surname = surname;
    }

    public boolean isSaved() {
        return !surname.equalsIgnoreCase("nessuno");
    }

    public String getFullName() {
        return isSaved() ? name + " " + surname : number;
    }

}
