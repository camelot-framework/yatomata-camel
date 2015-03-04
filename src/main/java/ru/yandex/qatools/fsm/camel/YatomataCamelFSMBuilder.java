package ru.yandex.qatools.fsm.camel;

import ru.yandex.qatools.fsm.Yatomata;
import ru.yandex.qatools.fsm.impl.YatomataImpl;

/**
 * @author Ilya Sadykov (mailto: smecsia@yandex-team.ru)
 */
public class YatomataCamelFSMBuilder<T> {
    final Class<T> fsmClass;

    public YatomataCamelFSMBuilder(Class<T> fsmClass) {
        this.fsmClass = fsmClass;
    }

    public Yatomata<T> build(T fsmInstance) {
        return build(null, fsmInstance);
    }

    public Yatomata<T> build(Object state, T fsmInstance) {
        try {
            if (state == null) {
                return new YatomataImpl<>(fsmClass, fsmInstance);
            }
            return new YatomataImpl<>(fsmClass, fsmInstance, state);
        } catch (Exception e) {
            throw new RuntimeException("Could not initialize the FSM Engine for FSM " + fsmClass, e);
        }
    }
}
