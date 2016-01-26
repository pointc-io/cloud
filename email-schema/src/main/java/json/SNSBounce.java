package json;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by admin on 1/20/16.
 */
public class SNSBounce {
    @JsonProperty
    private String bounceType;
    @JsonProperty
    private String bounceSubType;
    @JsonProperty
    private String reportingMTA;
    @JsonProperty
    private List<SNSRecipient> bouncedRecipients;
    @JsonProperty
    private String timestamp;
    @JsonProperty
    private String feedbackId;

    @JsonGetter
    public List<SNSRecipient> getBouncedRecipients() {
        return bouncedRecipients;
    }
    @JsonSetter
    public void setBouncedRecipients(List<SNSRecipient> bouncedRecipients) {
        this.bouncedRecipients = bouncedRecipients;
    }
    @JsonGetter
    public String getBounceSubType() {
        return bounceSubType;
    }
    @JsonSetter
    public void setBounceSubType(String bounceSubType) {
        this.bounceSubType = bounceSubType;
    }
    @JsonGetter
    public String getBounceType() {
        return bounceType;
    }
    @JsonSetter
    public void setBounceType(String bounceType) {
        this.bounceType = bounceType;
    }
    @JsonGetter
    public String getFeedbackId() {
        return feedbackId;
    }
    @JsonSetter
    public void setFeedbackId(String feedbackId) {
        this.feedbackId = feedbackId;
    }
    @JsonGetter
    public String getReportingMTA() {
        return reportingMTA;
    }
    @JsonSetter
    public void setReportingMTA(String reportingMTA) {
        this.reportingMTA = reportingMTA;
    }
    @JsonGetter
    public String getTimestamp() {
        return timestamp;
    }
    @JsonSetter
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    @JsonIgnore
    public List<String> getStringRecipients(){
        return bouncedRecipients.stream().map(SNSRecipient::getEmailAddress).collect(Collectors.toList());
    }
}