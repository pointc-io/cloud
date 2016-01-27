/**
 * This class is generated by jOOQ
 */
package data.schema.tables.records;


import data.schema.tables.EmailJnl;

import java.sql.Timestamp;

import javax.annotation.Generated;

import org.jooq.Field;
import org.jooq.Record13;
import org.jooq.Record2;
import org.jooq.Row13;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@Generated(
	value = {
		"http://www.jooq.org",
		"jOOQ version:3.7.2"
	},
	comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class EmailJnlRecord extends UpdatableRecordImpl<EmailJnlRecord> implements Record13<String, Long, Timestamp, String, String, String, String, String, String, String, String, Boolean, String> {

	private static final long serialVersionUID = -786504756;

	/**
	 * Setter for <code>email_jnl.id</code>.
	 */
	public EmailJnlRecord setId(String value) {
		setValue(0, value);
		return this;
	}

	/**
	 * Getter for <code>email_jnl.id</code>.
	 */
	public String getId() {
		return (String) getValue(0);
	}

	/**
	 * Setter for <code>email_jnl.v</code>.
	 */
	public EmailJnlRecord setV(Long value) {
		setValue(1, value);
		return this;
	}

	/**
	 * Getter for <code>email_jnl.v</code>.
	 */
	public Long getV() {
		return (Long) getValue(1);
	}

	/**
	 * Setter for <code>email_jnl.c</code>.
	 */
	public EmailJnlRecord setC(Timestamp value) {
		setValue(2, value);
		return this;
	}

	/**
	 * Getter for <code>email_jnl.c</code>.
	 */
	public Timestamp getC() {
		return (Timestamp) getValue(2);
	}

	/**
	 * Setter for <code>email_jnl.user_id</code>.
	 */
	public EmailJnlRecord setUserId(String value) {
		setValue(3, value);
		return this;
	}

	/**
	 * Getter for <code>email_jnl.user_id</code>.
	 */
	public String getUserId() {
		return (String) getValue(3);
	}

	/**
	 * Setter for <code>email_jnl.to</code>.
	 */
	public EmailJnlRecord setTo(String value) {
		setValue(4, value);
		return this;
	}

	/**
	 * Getter for <code>email_jnl.to</code>.
	 */
	public String getTo() {
		return (String) getValue(4);
	}

	/**
	 * Setter for <code>email_jnl.cc</code>.
	 */
	public EmailJnlRecord setCc(String value) {
		setValue(5, value);
		return this;
	}

	/**
	 * Getter for <code>email_jnl.cc</code>.
	 */
	public String getCc() {
		return (String) getValue(5);
	}

	/**
	 * Setter for <code>email_jnl.from</code>.
	 */
	public EmailJnlRecord setFrom(String value) {
		setValue(6, value);
		return this;
	}

	/**
	 * Getter for <code>email_jnl.from</code>.
	 */
	public String getFrom() {
		return (String) getValue(6);
	}

	/**
	 * Setter for <code>email_jnl.reply_to</code>.
	 */
	public EmailJnlRecord setReplyTo(String value) {
		setValue(7, value);
		return this;
	}

	/**
	 * Getter for <code>email_jnl.reply_to</code>.
	 */
	public String getReplyTo() {
		return (String) getValue(7);
	}

	/**
	 * Setter for <code>email_jnl.subject</code>.
	 */
	public EmailJnlRecord setSubject(String value) {
		setValue(8, value);
		return this;
	}

	/**
	 * Getter for <code>email_jnl.subject</code>.
	 */
	public String getSubject() {
		return (String) getValue(8);
	}

	/**
	 * Setter for <code>email_jnl.text_body</code>.
	 */
	public EmailJnlRecord setTextBody(String value) {
		setValue(9, value);
		return this;
	}

	/**
	 * Getter for <code>email_jnl.text_body</code>.
	 */
	public String getTextBody() {
		return (String) getValue(9);
	}

	/**
	 * Setter for <code>email_jnl.html_body</code>.
	 */
	public EmailJnlRecord setHtmlBody(String value) {
		setValue(10, value);
		return this;
	}

	/**
	 * Getter for <code>email_jnl.html_body</code>.
	 */
	public String getHtmlBody() {
		return (String) getValue(10);
	}

	/**
	 * Setter for <code>email_jnl.attachments</code>.
	 */
	public EmailJnlRecord setAttachments(Boolean value) {
		setValue(11, value);
		return this;
	}

	/**
	 * Getter for <code>email_jnl.attachments</code>.
	 */
	public Boolean getAttachments() {
		return (Boolean) getValue(11);
	}

	/**
	 * Setter for <code>email_jnl.message_id</code>.
	 */
	public EmailJnlRecord setMessageId(String value) {
		setValue(12, value);
		return this;
	}

	/**
	 * Getter for <code>email_jnl.message_id</code>.
	 */
	public String getMessageId() {
		return (String) getValue(12);
	}

	// -------------------------------------------------------------------------
	// Primary key information
	// -------------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Record2<String, Long> key() {
		return (Record2) super.key();
	}

	// -------------------------------------------------------------------------
	// Record13 type implementation
	// -------------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Row13<String, Long, Timestamp, String, String, String, String, String, String, String, String, Boolean, String> fieldsRow() {
		return (Row13) super.fieldsRow();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Row13<String, Long, Timestamp, String, String, String, String, String, String, String, String, Boolean, String> valuesRow() {
		return (Row13) super.valuesRow();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Field<String> field1() {
		return EmailJnl.EMAIL_JNL.ID;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Field<Long> field2() {
		return EmailJnl.EMAIL_JNL.V;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Field<Timestamp> field3() {
		return EmailJnl.EMAIL_JNL.C;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Field<String> field4() {
		return EmailJnl.EMAIL_JNL.USER_ID;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Field<String> field5() {
		return EmailJnl.EMAIL_JNL.TO;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Field<String> field6() {
		return EmailJnl.EMAIL_JNL.CC;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Field<String> field7() {
		return EmailJnl.EMAIL_JNL.FROM;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Field<String> field8() {
		return EmailJnl.EMAIL_JNL.REPLY_TO;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Field<String> field9() {
		return EmailJnl.EMAIL_JNL.SUBJECT;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Field<String> field10() {
		return EmailJnl.EMAIL_JNL.TEXT_BODY;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Field<String> field11() {
		return EmailJnl.EMAIL_JNL.HTML_BODY;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Field<Boolean> field12() {
		return EmailJnl.EMAIL_JNL.ATTACHMENTS;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Field<String> field13() {
		return EmailJnl.EMAIL_JNL.MESSAGE_ID;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String value1() {
		return getId();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Long value2() {
		return getV();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Timestamp value3() {
		return getC();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String value4() {
		return getUserId();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String value5() {
		return getTo();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String value6() {
		return getCc();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String value7() {
		return getFrom();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String value8() {
		return getReplyTo();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String value9() {
		return getSubject();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String value10() {
		return getTextBody();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String value11() {
		return getHtmlBody();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Boolean value12() {
		return getAttachments();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String value13() {
		return getMessageId();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EmailJnlRecord value1(String value) {
		setId(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EmailJnlRecord value2(Long value) {
		setV(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EmailJnlRecord value3(Timestamp value) {
		setC(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EmailJnlRecord value4(String value) {
		setUserId(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EmailJnlRecord value5(String value) {
		setTo(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EmailJnlRecord value6(String value) {
		setCc(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EmailJnlRecord value7(String value) {
		setFrom(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EmailJnlRecord value8(String value) {
		setReplyTo(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EmailJnlRecord value9(String value) {
		setSubject(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EmailJnlRecord value10(String value) {
		setTextBody(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EmailJnlRecord value11(String value) {
		setHtmlBody(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EmailJnlRecord value12(Boolean value) {
		setAttachments(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EmailJnlRecord value13(String value) {
		setMessageId(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EmailJnlRecord values(String value1, Long value2, Timestamp value3, String value4, String value5, String value6, String value7, String value8, String value9, String value10, String value11, Boolean value12, String value13) {
		value1(value1);
		value2(value2);
		value3(value3);
		value4(value4);
		value5(value5);
		value6(value6);
		value7(value7);
		value8(value8);
		value9(value9);
		value10(value10);
		value11(value11);
		value12(value12);
		value13(value13);
		return this;
	}

	// -------------------------------------------------------------------------
	// Constructors
	// -------------------------------------------------------------------------

	/**
	 * Create a detached EmailJnlRecord
	 */
	public EmailJnlRecord() {
		super(EmailJnl.EMAIL_JNL);
	}

	/**
	 * Create a detached, initialised EmailJnlRecord
	 */
	public EmailJnlRecord(String id, Long v, Timestamp c, String userId, String to, String cc, String from, String replyTo, String subject, String textBody, String htmlBody, Boolean attachments, String messageId) {
		super(EmailJnl.EMAIL_JNL);

		setValue(0, id);
		setValue(1, v);
		setValue(2, c);
		setValue(3, userId);
		setValue(4, to);
		setValue(5, cc);
		setValue(6, from);
		setValue(7, replyTo);
		setValue(8, subject);
		setValue(9, textBody);
		setValue(10, htmlBody);
		setValue(11, attachments);
		setValue(12, messageId);
	}
}
