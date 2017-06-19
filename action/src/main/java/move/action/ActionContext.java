package move.action;

import io.vertx.core.Context;

/**
 *
 */
public class ActionContext {

  public final long started = System.currentTimeMillis();
  public final long timesOutAt;
  public final ActionProvider entry;
  public final Context context;
  public Object data;

  public ActionContext(long timeoutMillis, ActionProvider entry, Context context) {
    this.timesOutAt = started + timeoutMillis;
    this.entry = entry;
    this.context = context;
  }

  public <T> T data() {
    return (T) data;
  }

  @Override
  public String toString() {
    return "ActionContext{" +
        "started=" + started +
        ", timesOutAt=" + timesOutAt +
        ", entry=" + entry + "[" + entry.getActionClass().getCanonicalName() + "]" +
        ", context=" + context +
        ", data=" + data +
        '}';
  }
}
