package me.zrageyh.telefono.model;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.mineacademy.fo.collection.SerializedMap;

import java.util.UUID;

@Getter
@Setter
public class Abbonamento {


    private String sim;
    private String abbonamento;
    private int messages;
    private int calls;

    public Abbonamento() {

    }

    public Abbonamento(final String sim, final String abbonamento, final int messages, final int calls) {
        this.sim = sim;
        this.abbonamento = abbonamento;
        this.messages = messages;
        this.calls = calls;
    }

    public Abbonamento(final String abbonamento, final int messages, final int calls) {
        this.abbonamento = abbonamento;
        this.messages = messages;
        this.calls = calls;
    }

    public static Abbonamento deserialize(final SerializedMap map) {
        final Abbonamento abbonamento = new Abbonamento();
        abbonamento.setSim(map.get("UUID", String.class));
        abbonamento.setAbbonamento(map.get("abbonamento", String.class));
        abbonamento.setMessages(map.get("messaggi", Integer.class));
        abbonamento.setCalls(map.get("chiamate", Integer.class));
        return abbonamento;
    }

    public boolean hasCreditoToMessage() {
        return messages > 0;
    }

    public boolean hasCreditoToCall() {
        return calls > 0;
    }

    public void removeMinute() {
        calls--;
    }

    public void removeMessage() {

    }

    public String getName() {
        return Bukkit.getOfflinePlayer(UUID.fromString(sim)).getName();
    }

    @Override
    public String toString() {
        return "ObjectAbbonamento{" +
                "uuid='" + sim + '\'' +
                ", abbonamento='" + abbonamento + '\'' +
                ", messages=" + messages +
                ", calls=" + calls +
                '}';
    }

    public SerializedMap serialize() {
        final SerializedMap map = new SerializedMap();
        map.put("UUID", sim);
        map.put("abbonamento", abbonamento);
        map.putIf("messages", messages);
        map.put("calls", calls);
        return map;
    }


}
