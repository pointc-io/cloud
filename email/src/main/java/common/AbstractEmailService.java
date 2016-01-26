package common;

import com.google.common.util.concurrent.AbstractIdleService;

/**
 * Created by Brad Behnke on 1/26/16.
 */
public abstract class AbstractEmailService<T extends AbstractSendRequest> extends AbstractIdleService {
    public abstract void send(T sendRequest);
}