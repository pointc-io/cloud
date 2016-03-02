/**
 * This class is generated by jOOQ
 */
package io.clickhandler.data.schema;


import io.clickhandler.data.schema.tables.Email;
import io.clickhandler.data.schema.tables.EmailAttachment;
import io.clickhandler.data.schema.tables.EmailAttachmentJnl;
import io.clickhandler.data.schema.tables.EmailJnl;
import io.clickhandler.data.schema.tables.EmailRecipient;
import io.clickhandler.data.schema.tables.EmailRecipientJnl;
import io.clickhandler.data.schema.tables.Evolution;
import io.clickhandler.data.schema.tables.EvolutionChange;
import io.clickhandler.data.schema.tables.File;
import io.clickhandler.data.schema.tables.FileJnl;
import io.clickhandler.data.schema.tables.records.EmailAttachmentJnlRecord;
import io.clickhandler.data.schema.tables.records.EmailAttachmentRecord;
import io.clickhandler.data.schema.tables.records.EmailJnlRecord;
import io.clickhandler.data.schema.tables.records.EmailRecipientJnlRecord;
import io.clickhandler.data.schema.tables.records.EmailRecipientRecord;
import io.clickhandler.data.schema.tables.records.EmailRecord;
import io.clickhandler.data.schema.tables.records.EvolutionChangeRecord;
import io.clickhandler.data.schema.tables.records.EvolutionRecord;
import io.clickhandler.data.schema.tables.records.FileJnlRecord;
import io.clickhandler.data.schema.tables.records.FileRecord;

import javax.annotation.Generated;

import org.jooq.UniqueKey;
import org.jooq.impl.AbstractKeys;


/**
 * A class modelling foreign key relationships between tables of the <code></code> 
 * schema
 */
@Generated(
	value = {
		"http://www.jooq.org",
		"jOOQ version:3.7.2"
	},
	comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Keys {

	// -------------------------------------------------------------------------
	// IDENTITY definitions
	// -------------------------------------------------------------------------


	// -------------------------------------------------------------------------
	// UNIQUE and PRIMARY KEY definitions
	// -------------------------------------------------------------------------

	public static final UniqueKey<EmailAttachmentRecord> PK_EMAIL_ATTACHMENT = UniqueKeys0.PK_EMAIL_ATTACHMENT;
	public static final UniqueKey<EmailAttachmentJnlRecord> PK_EMAIL_ATTACHMENT_JNL = UniqueKeys0.PK_EMAIL_ATTACHMENT_JNL;
	public static final UniqueKey<EvolutionRecord> PK_EVOLUTION = UniqueKeys0.PK_EVOLUTION;
	public static final UniqueKey<FileRecord> PK_FILE = UniqueKeys0.PK_FILE;
	public static final UniqueKey<FileJnlRecord> PK_FILE_JNL = UniqueKeys0.PK_FILE_JNL;
	public static final UniqueKey<EvolutionChangeRecord> PK_EVOLUTION_CHANGE = UniqueKeys0.PK_EVOLUTION_CHANGE;
	public static final UniqueKey<EmailRecord> PK_EMAIL = UniqueKeys0.PK_EMAIL;
	public static final UniqueKey<EmailJnlRecord> PK_EMAIL_JNL = UniqueKeys0.PK_EMAIL_JNL;
	public static final UniqueKey<EmailRecipientRecord> PK_EMAIL_RECIPIENT = UniqueKeys0.PK_EMAIL_RECIPIENT;
	public static final UniqueKey<EmailRecipientJnlRecord> PK_EMAIL_RECIPIENT_JNL = UniqueKeys0.PK_EMAIL_RECIPIENT_JNL;

	// -------------------------------------------------------------------------
	// FOREIGN KEY definitions
	// -------------------------------------------------------------------------


	// -------------------------------------------------------------------------
	// [#1459] distribute members to avoid static initialisers > 64kb
	// -------------------------------------------------------------------------

	private static class UniqueKeys0 extends AbstractKeys {
		public static final UniqueKey<EmailAttachmentRecord> PK_EMAIL_ATTACHMENT = createUniqueKey(EmailAttachment.EMAIL_ATTACHMENT, EmailAttachment.EMAIL_ATTACHMENT.ID);
		public static final UniqueKey<EmailAttachmentJnlRecord> PK_EMAIL_ATTACHMENT_JNL = createUniqueKey(EmailAttachmentJnl.EMAIL_ATTACHMENT_JNL, EmailAttachmentJnl.EMAIL_ATTACHMENT_JNL.ID, EmailAttachmentJnl.EMAIL_ATTACHMENT_JNL.V);
		public static final UniqueKey<EvolutionRecord> PK_EVOLUTION = createUniqueKey(Evolution.EVOLUTION, Evolution.EVOLUTION.ID);
		public static final UniqueKey<FileRecord> PK_FILE = createUniqueKey(File.FILE, File.FILE.ID);
		public static final UniqueKey<FileJnlRecord> PK_FILE_JNL = createUniqueKey(FileJnl.FILE_JNL, FileJnl.FILE_JNL.ID, FileJnl.FILE_JNL.V);
		public static final UniqueKey<EvolutionChangeRecord> PK_EVOLUTION_CHANGE = createUniqueKey(EvolutionChange.EVOLUTION_CHANGE, EvolutionChange.EVOLUTION_CHANGE.ID);
		public static final UniqueKey<EmailRecord> PK_EMAIL = createUniqueKey(Email.EMAIL, Email.EMAIL.ID);
		public static final UniqueKey<EmailJnlRecord> PK_EMAIL_JNL = createUniqueKey(EmailJnl.EMAIL_JNL, EmailJnl.EMAIL_JNL.ID, EmailJnl.EMAIL_JNL.V);
		public static final UniqueKey<EmailRecipientRecord> PK_EMAIL_RECIPIENT = createUniqueKey(EmailRecipient.EMAIL_RECIPIENT, EmailRecipient.EMAIL_RECIPIENT.ID);
		public static final UniqueKey<EmailRecipientJnlRecord> PK_EMAIL_RECIPIENT_JNL = createUniqueKey(EmailRecipientJnl.EMAIL_RECIPIENT_JNL, EmailRecipientJnl.EMAIL_RECIPIENT_JNL.ID, EmailRecipientJnl.EMAIL_RECIPIENT_JNL.V);
	}
}
