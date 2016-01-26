package sns.event;

import json.SNSGeneralMessage;

/**
 * Created by admin on 1/25/16.
 */
public class UnsubscribeEvent extends SNSEvent {
    private SNSGeneralMessage message;

    public UnsubscribeEvent(SNSGeneralMessage message) {
        this.message = message;
    }

    public SNSGeneralMessage getMessage() {
        return message;
    }

    public void setMessage(SNSGeneralMessage message) {
        this.message = message;
    }
}