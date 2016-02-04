package sns.data.json.email.notify;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.List;

/**
 * SNS JSON Mail Object for Email Notifications
 *
 * @author Brad Behnke
 */
public class NotifyMail {
    @JsonProperty
    private String timestamp;
    @JsonProperty
    private String source;
    @JsonProperty
    private String sourceArn;
    @JsonProperty
    private String sendingAccountId;
    @JsonProperty
    private String messageId;
    @JsonProperty
    private List<String> destination;

    @JsonGetter
    public List<String> getDestination() {
        return destination;
    }
    @JsonSetter
    public void setDestination(List<String> destination) {
        this.destination = destination;
    }
    @JsonGetter
    public String getMessageId() {
        return messageId;
    }
    @JsonSetter
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
    @JsonGetter
    public String getSendingAccountId() {
        return sendingAccountId;
    }
    @JsonSetter
    public void setSendingAccountId(String sendingAccountId) {
        this.sendingAccountId = sendingAccountId;
    }
    @JsonGetter
    public String getSource() {
        return source;
    }
    @JsonSetter
    public void setSource(String source) {
        this.source = source;
    }
    @JsonGetter
    public String getSourceArn() {
        return sourceArn;
    }
    @JsonSetter
    public void setSourceArn(String sourceArn) {
        this.sourceArn = sourceArn;
    }
    @JsonGetter
    public String getTimestamp() {
        return timestamp;
    }
    @JsonSetter
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
