package me.zrageyh.telefono.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import me.zrageyh.telefono.model.history.HistoryChiamata;
import me.zrageyh.telefono.utils.Utils;

@RequiredArgsConstructor
@Getter
@Setter
public class Call {

    private final Contatto contattoCalled;
    private final Contatto contattoWhoCall;
    private final Abbonamento abbonamento;
    private boolean inCall = false;


    public void endCall() {
        inCall = false;
    }

    public void startCall() {
        inCall = true;
    }

    public HistoryChiamata getHistoryChiamata(final boolean isLost) {
        return new HistoryChiamata(contattoCalled.getSim(), contattoCalled.getNumber(), Utils.getDateNow(), isLost);

    }

    public HistoryChiamata getHistoryChiamataReverse(final boolean isLost) {
        return new HistoryChiamata(contattoCalled.getNumber(), contattoCalled.getSim(), Utils.getDateNow(), isLost, true);
    }


}
