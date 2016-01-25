package event;

import sns.json.SNSEmailMessage;

/**
 * Created by admin on 1/25/16.
 */
public class EmailComplaintEvent {

    private SNSEmailMessage message;

    public EmailComplaintEvent(SNSEmailMessage message) {
        this.message = message;
    }

    public SNSEmailMessage getMessage() {
        return message;
    }

    public void setMessage(SNSEmailMessage message) {
        this.message = message;
    }
}
