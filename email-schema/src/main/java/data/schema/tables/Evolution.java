/**
 * This class is generated by jOOQ
 */
package data.schema.tables;


import data.schema.DefaultSchema;
import data.schema.Keys;
import data.schema.tables.records.EvolutionRecord;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Generated;

import org.jooq.Field;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.impl.TableImpl;


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
public class Evolution extends TableImpl<EvolutionRecord> {

	private static final long serialVersionUID = -359850781;

	/**
	 * The reference instance of <code>evolution</code>
	 */
	public static final Evolution EVOLUTION = new Evolution();

	/**
	 * The class holding records for this type
	 */
	@Override
	public Class<EvolutionRecord> getRecordType() {
		return EvolutionRecord.class;
	}

	/**
	 * The column <code>evolution.id</code>.
	 */
	public final TableField<EvolutionRecord, String> ID = createField("id", org.jooq.impl.SQLDataType.VARCHAR.length(32).nullable(false), this, "");

	/**
	 * The column <code>evolution.v</code>.
	 */
	public final TableField<EvolutionRecord, Long> V = createField("v", org.jooq.impl.SQLDataType.BIGINT.nullable(false), this, "");

	/**
	 * The column <code>evolution.c</code>.
	 */
	public final TableField<EvolutionRecord, Timestamp> C = createField("c", org.jooq.impl.SQLDataType.TIMESTAMP.nullable(false), this, "");

	/**
	 * The column <code>evolution.success</code>.
	 */
	public final TableField<EvolutionRecord, Boolean> SUCCESS = createField("success", org.jooq.impl.SQLDataType.BOOLEAN.nullable(false), this, "");

	/**
	 * The column <code>evolution.end</code>.
	 */
	public final TableField<EvolutionRecord, Timestamp> END = createField("end", org.jooq.impl.SQLDataType.TIMESTAMP, this, "");

	/**
	 * Create a <code>evolution</code> table reference
	 */
	public Evolution() {
		this("evolution", null);
	}

	/**
	 * Create an aliased <code>evolution</code> table reference
	 */
	public Evolution(String alias) {
		this(alias, EVOLUTION);
	}

	private Evolution(String alias, Table<EvolutionRecord> aliased) {
		this(alias, aliased, null);
	}

	private Evolution(String alias, Table<EvolutionRecord> aliased, Field<?>[] parameters) {
		super(alias, DefaultSchema.DEFAULT_SCHEMA, aliased, parameters, "");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public UniqueKey<EvolutionRecord> getPrimaryKey() {
		return Keys.PK_EVOLUTION;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<UniqueKey<EvolutionRecord>> getKeys() {
		return Arrays.<UniqueKey<EvolutionRecord>>asList(Keys.PK_EVOLUTION);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Evolution as(String alias) {
		return new Evolution(alias, this);
	}

	/**
	 * Rename this table
	 */
	public Evolution rename(String name) {
		return new Evolution(name, null);
	}
}
