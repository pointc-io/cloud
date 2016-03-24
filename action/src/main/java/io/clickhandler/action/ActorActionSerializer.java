package io.clickhandler.action;

/**
 *
 */
public interface ActorActionSerializer {
    byte[] byteify(Object value);

    <T> T parse(Class<T> type, byte[] data);

    <T> T parse(Class<T> type, byte[] data, int offset, int length);
}