/*
*    jETeL/Clover - Java based ETL application framework.
*    Copyright (C) 2002-04  David Pavlis <david_pavlis@hotmail.com>
*    
*    This library is free software; you can redistribute it and/or
*    modify it under the terms of the GNU Lesser General Public
*    License as published by the Free Software Foundation; either
*    version 2.1 of the License, or (at your option) any later version.
*    
*    This library is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU    
*    Lesser General Public License for more details.
*    
*    You should have received a copy of the GNU Lesser General Public
*    License along with this library; if not, write to the Free Software
*    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*
*/
package org.jetel.connection.jdbc;
import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

import javax.sql.rowset.serial.SerialBlob;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetel.connection.jdbc.specific.JdbcSpecific;
import org.jetel.connection.jdbc.specific.impl.DefaultJdbcSpecific;
import org.jetel.data.BooleanDataField;
import org.jetel.data.ByteDataField;
import org.jetel.data.DataField;
import org.jetel.data.DataRecord;
import org.jetel.data.DateDataField;
import org.jetel.data.DecimalDataField;
import org.jetel.data.IntegerDataField;
import org.jetel.data.LongDataField;
import org.jetel.data.NumericDataField;
import org.jetel.data.primitive.Decimal;
import org.jetel.data.primitive.HugeDecimal;
import org.jetel.exception.JetelException;
import org.jetel.metadata.DataFieldMetadata;
import org.jetel.metadata.DataRecordMetadata;
import org.jetel.util.string.StringUtils;

/**
 *  Class for creating mappings between CloverETL's DataRecords and JDBC's
 *  ResultSets.<br>
 *  It also contains inner classes for translating various CloverETL's DataField
 *  types onto JDBC types.
 *
 * @author      dpavlis
 * @since       October 7, 2002
 * @revision    $Revision$
 * @created     8. �ervenec 2003
 */
public abstract class CopySQLData {

	/**
	 *  Description of the Field
	 *
	 * @since    October 7, 2002
	 */
	protected int fieldSQL,fieldJetel;
	/**
	 *  Description of the Field
	 *
	 * @since    October 7, 2002
	 */
	protected DataField field;

    protected boolean inBatchUpdate = false; // indicates whether batchMode is used when populating target DB

	static Log logger = LogFactory.getLog(CopySQLData.class);

	/**
	 *  Constructor for the CopySQLData object
	 *
	 * @param  record      Clover record which will be source or target
	 * @param  fieldSQL    index of the field in SQL statement
	 * @param  fieldJetel  index of the field in Clover record
	 * @since              October 7, 2002
	 */
	CopySQLData(DataRecord record, int fieldSQL, int fieldJetel) {
		this.fieldSQL = fieldSQL + 1;
		// fields in ResultSet start with index 1
		this.fieldJetel=fieldJetel;
		field = record.getField(fieldJetel);
	}

	/**
	 * Assigns different DataField than the original used when creating
	 * copy object
	 * 
	 * @param field	New DataField
	 */
	public void setCloverField(DataField field){
	    this.field=field;
	}
	
	
	/**
	 * Assings different DataField (DataRecord). The proper field
	 * is taken from DataRecord based on previously saved field number.
	 * 
	 * @param record	New DataRecord
	 */
	public void setCloverRecord(DataRecord record){
	    this.field=record.getField(fieldJetel);
	}

    
     /**
     * @return Returns the inBatchUpdate.
     */
    public boolean isInBatchUpdate() {
        return inBatchUpdate;
    }

    /**
     * @param inBatchUpdate The inBatchUpdate to set.
     */
    public void setInBatchUpdate(boolean inBatch) {
        this.inBatchUpdate = inBatch;
    }
	
	/**
	 *  Sets value of Jetel/Clover data field based on value from SQL ResultSet
	 *
	 * @param  resultSet         Description of Parameter
	 * @exception  SQLException  Description of Exception
	 * @since                    October 7, 2002
	 */
	public void sql2jetel(ResultSet resultSet) throws SQLException {
		try {
			setJetel(resultSet);
		} catch (SQLException ex) {
			throw new SQLException(ex.getMessage() + " with field " + field.getMetadata().getName());
		} catch (ClassCastException ex){
		    throw new SQLException("Incompatible Clover & JDBC field types - field "+field.getMetadata().getName()+
		            " Clover type: "+SQLUtil.jetelType2Str(field.getMetadata().getType()));
		}
	}


	/**
	 *  Sets value of SQL field in PreparedStatement based on Jetel/Clover data
	 *  field's value
	 *
	 * @param  pStatement        Description of Parameter
	 * @exception  SQLException  Description of Exception
	 * @since                    October 7, 2002
	 */
	public void jetel2sql(PreparedStatement pStatement) throws SQLException {
		try {
			setSQL(pStatement);
		} catch (SQLException ex) {
			throw new SQLException(ex.getMessage() + " with field " + field.getMetadata().getName());
		}catch (ClassCastException ex){
		    throw new SQLException("Incompatible Clover & JDBC field types - field "+field.getMetadata().getName()+
		            " Clover type: "+SQLUtil.jetelType2Str(field.getMetadata().getType()));
		}
	}

	/**
	 * @param resultSet
	 * @return current value from result set from corresponding index
	 * @throws SQLException
	 */
	public abstract Object getDbValue(ResultSet resultSet) throws SQLException;

	/**
	 * @param statement
	 * @return current value from callable statement from corresponding index
	 * @throws SQLException
	 */
	public abstract Object getDbValue(CallableStatement statement) throws SQLException;
	
	/**
	 * @return current value from data record from corresponding field
	 */
	public Object getCloverValue() {
		return field.getValue();
	}


	/**
	 *  Sets the Jetel attribute of the CopySQLData object
	 *
	 * @param  resultSet         The new Jetel value
	 * @exception  SQLException  Description of Exception
	 * @since                    October 7, 2002
	 */
	abstract void setJetel(ResultSet resultSet) throws SQLException;

	abstract void setJetel(CallableStatement statement) throws SQLException;

	/**
	 *  Sets the SQL attribute of the CopySQLData object
	 *
	 * @param  pStatement        The new SQL value
	 * @exception  SQLException  Description of Exception
	 * @since                    October 7, 2002
	 */
	abstract void setSQL(PreparedStatement pStatement) throws SQLException;


	
	
	/**
	 * Assigns new instance of DataRecord to existing CopySQLData structure (array).
	 * Useful when CopySQLData (aka transmap) was created using some other
	 * DataRecord and new one needs to be used
	 * 
	 * @param transMap	Array of CopySQLData object - the transmap
	 * @param record	New DataRecord object to be used within transmap
	 */
	public static void resetDataRecord(CopySQLData[] transMap,DataRecord record){
	    for(int i=0;i<transMap.length;i++){
	    	if (transMap[i] != null) {
	    		transMap[i].setCloverRecord(record);
	    	}
	    }
	}
	
    /**
     * Set on/off working in batchUpdate mode. In this mode (especially on Oracle 10)
     * we have to create new object for each setDate(), setTimestamp() statement call.
     * 
     * @param transMap
     * @param inBatch
     */
    public static void setBatchUpdate(CopySQLData[] transMap, boolean inBatch){
        for(int i=0;i<transMap.length;transMap[i++].setInBatchUpdate(inBatch));
    }
    
	/**
	 *  Creates translation array for copying data from Database record into Jetel
	 *  record
	 *
	 * @param  metadata    Metadata describing Jetel data record
	 * @param  record      Jetel data record
	 * @param  fieldTypes  Description of the Parameter
	 * @return             Array of CopySQLData objects which can be used when getting
	 *      data from DB into Jetel record
	 * @since              September 26, 2002
	 */
	public static CopySQLData[] sql2JetelTransMap(List fieldTypes, DataRecordMetadata metadata, DataRecord record) {
		/* test that both sides have at least the same number of fields, less
		 * fields on DB side is O.K. (some of Clover fields won't get assigned value).
		 */
		if (fieldTypes.size()>metadata.getNumFields()){
			throw new RuntimeException("CloverETL data record "+metadata.getName()+
					" contains less fields than source JDBC record !");
		}
		
		CopySQLData[] transMap = new CopySQLData[fieldTypes.size()];
		int i = 0;
		for (ListIterator iterator = fieldTypes.listIterator(); iterator.hasNext(); ) {
			transMap[i] = createCopyObject(((Integer) iterator.next()).shortValue(),
					record.getField(i).getMetadata(),
					record, i, i);
			i++;
		}

		return transMap;
	}

	/**
	 *  Creates translation array for copying data from Database record into Jetel
	 *  record
	 *
	 * @param fieldTypes
	 * @param metadata Metadata describing Jetel data record
	 * @param record Jetel data record
	 * @param keyFields fields used for creating translation array
	 * @return Array of CopySQLData objects which can be used when getting
	 *      data from DB into Jetel record
	 */
	public static CopySQLData[] sql2JetelTransMap(List fieldTypes, DataRecordMetadata metadata, DataRecord record,
			String[] keyFields){

		if (fieldTypes.size() != keyFields.length){
			throw new RuntimeException("Number of db fields (" + fieldTypes.size() + ") is diffrent then " +
					"number of key fields " + keyFields.length + ")." );
		}
		CopySQLData[] transMap = new CopySQLData[fieldTypes.size()];
		int fieldIndex;
		for (int i=0; i < keyFields.length; i++) {
			fieldIndex = record.getMetadata().getFieldPosition(keyFields[i]);
			if (fieldIndex == -1) {
				throw new RuntimeException("Field " + StringUtils.quote(keyFields[i]) + " doesn't exist in metadata " +
						StringUtils.quote(record.getMetadata().getName()));
			}
			transMap[i] = createCopyObject(((Integer) fieldTypes.get(i)).shortValue(),
					record.getField(fieldIndex).getMetadata(),
					record, i, metadata.getFieldPosition(keyFields[i]));
		}
		
		return transMap;
		
	}

	/**
	 *  Creates translation array for copying data from Jetel record into Database
	 *  record
	 *
	 * @param  fieldTypes        JDBC field types - of the target JDBC data fields
	 * @param  record            DataRecord which will be used to populate DB
	 * @return                   the transMap (array of CopySQLData) object
	 * @exception  SQLException  Description of Exception
	 * @since                    October 4, 2002
	 */
	public static CopySQLData[] jetel2sqlTransMap(List fieldTypes, DataRecord record) throws SQLException {
		int i = 0;
		/* test that both sides have at least the same number of fields, less
		 * fields on DB side is O.K. (some of Clover fields won't be assigned to JDBC).
		 */
		if (fieldTypes.size()>record.getMetadata().getNumFields()){
			throw new RuntimeException("CloverETL data record "+record.getMetadata().getName()+
					" contains less fields than target JDBC record !");
		}
		CopySQLData[] transMap = new CopySQLData[fieldTypes.size()];
		ListIterator iterator = fieldTypes.listIterator();
		
		while (iterator.hasNext()) {
			transMap[i] = createCopyObject(((Integer) iterator.next()).shortValue(),
					record.getField(i).getMetadata(),
					record, i, i);
			i++;
		}
		return transMap;
	}
	
	


	/**
	 *  Creates translation array for copying data from Jetel record into Database
	 *  record. It allows only certain (specified) Clover fields to be considered
	 *
	 * @param  fieldTypes          DJDBC field types - of the target JDBC data fields
	 * @param  record              Description of the Parameter
	 * @param  cloverFields        array of DataRecord record's field names which should be considered for mapping
	 * @return                     Description of the Return Value
	 * @exception  SQLException    Description of the Exception
	 * @exception  JetelException  Description of the Exception
	 */
	public static CopySQLData[] jetel2sqlTransMap(List fieldTypes, DataRecord record, String[] cloverFields)
			 throws SQLException, JetelException {
		int i = 0;
		int fromIndex = 0;
		int toIndex = 0;
		short jdbcType;

		CopySQLData[] transMap = new CopySQLData[fieldTypes.size()];
		ListIterator iterator = fieldTypes.listIterator();

		while (iterator.hasNext()) {
			jdbcType = ((Integer) iterator.next()).shortValue();
			// from index is index of specified cloverField in the Clover record
			if (i >= cloverFields.length) {
				throw new JetelException(" Number of db fields (" + fieldTypes.size() + ") is diffrent then number of clover fields (" + cloverFields.length + ") !" );
			}
			fromIndex = record.getMetadata().getFieldPosition(cloverFields[i]);
			if (fromIndex == -1) {
				throw new JetelException(" Field \"" + cloverFields[i] + "\" does not exist in DataRecord !");
			}
			// we copy from Clover's field to JDBC - toIndex/fromIndex is switched here
			transMap[i++] = createCopyObject(jdbcType, record.getField(fromIndex).getMetadata(), 
					record, toIndex, fromIndex);
			toIndex++;// we go one by one - order defined by insert/update statement

		}
		return transMap;
	}

	/**
	 *  Creates translation array for copying data from Jetel record into Database
	 *  record. It allows only certain (specified) Clover fields to be considered
	 *
	 * @param  fieldTypes          JDBC field types - of the target JDBC data fields
	 * @param  record              Description of the Parameter
	 * @param  cloverFields        array of DataRecord record's field numbers which should be considered for mapping
	 * @return                     Description of the Return Value
	 * @exception  SQLException    Description of the Exception
	 * @exception  JetelException  Description of the Exception
	 */
	public static CopySQLData[] jetel2sqlTransMap(List fieldTypes,
            DataRecord record, int[] cloverFields) throws SQLException,
            JetelException {
        int i = 0;
        int fromIndex = 0;
        int toIndex = 0;
        short jdbcType;
     
        CopySQLData[] transMap = new CopySQLData[fieldTypes.size()];
        ListIterator iterator = fieldTypes.listIterator();

        while (iterator.hasNext()) {
            jdbcType = ((Integer) iterator.next()).shortValue();
            // from index is index of specified cloverField in the Clover record
            fromIndex = cloverFields[i];
            if (fromIndex == -1) {
                throw new JetelException(" Field \"" + cloverFields[i]
                        + "\" does not exist in DataRecord !");
            }
            // we copy from Clover's field to JDBC - toIndex/fromIndex is
            // switched here
            transMap[i++] = createCopyObject(jdbcType, record.getField(fromIndex).getMetadata(), 
            		record, toIndex, fromIndex);
            toIndex++;// we go one by one - order defined by insert/update statement

        }
        return transMap;
    }
	
	/**
	 * Creates translation array for copying data from Jetel record into Database
	 *  record. It allows only certain (specified) Clover fields to be considered.<br>
	 * <i>Note:</i> the target (JDBC) data types are guessed from Jetel field types - so use this
	 * method only as the last resort.
	 * 
	 * @param record
	 * @param cloverFields  array of DataRecord record's field numbers which should be considered for mapping
	 * @return
	 * @throws JetelException
	 */
	@Deprecated
	public static CopySQLData[] jetel2sqlTransMap(DataRecord record, int[] cloverFields) throws JetelException {
        return jetel2sqlTransMap(record, cloverFields, DefaultJdbcSpecific.getInstance());
    }
	
	public static CopySQLData[] jetel2sqlTransMap(DataRecord record, int[] cloverFields, JdbcSpecific jdbcSpecific) throws JetelException {
        int fromIndex;
        int toIndex;
        int jdbcType;
        DataFieldMetadata jetelField;

        CopySQLData[] transMap = new CopySQLData[cloverFields.length];

       for(int i=0;i<cloverFields.length;i++) {
            jetelField=record.getField(cloverFields[i]).getMetadata();
            jdbcType = jdbcSpecific.jetelType2sql(jetelField);
            
            // from index is index of specified cloverField in the Clover record
            fromIndex = cloverFields[i];
            toIndex=i;
            // we copy from Clover's field to JDBC - toIndex/fromIndex is
            // switched here
            transMap[i] = createCopyObject(jdbcType, jetelField, record,
                    toIndex, fromIndex);
        }
        return transMap;
    }

	/**
	 *  Creates copy object - bridge between JDBC data types and Clover data types
	 *
	 * @param  SQLType         Description of the Parameter
	 * @param  fieldMetadata  Description of the Parameter
	 * @param  record          Description of the Parameter
	 * @param  fromIndex       Description of the Parameter
	 * @param  toIndex         Description of the Parameter
	 * @return                 Description of the Return Value
	 */
	public static CopySQLData createCopyObject(int SQLType, DataFieldMetadata fieldMetadata, 
			DataRecord record, int fromIndex, int toIndex) {
		String format = fieldMetadata.getFormatStr();
		char jetelType = fieldMetadata.getType();
		switch (SQLType) {
			case Types.CHAR:
			case Types.LONGVARCHAR:
			case Types.VARCHAR:
				return new CopyString(record, fromIndex, toIndex);
			case Types.INTEGER:
			case Types.SMALLINT:
				return new CopyInteger(record, fromIndex, toIndex);
			case Types.BIGINT:
			    return new CopyLong(record,fromIndex,toIndex);
			case Types.DECIMAL:
			case Types.DOUBLE:
			case Types.FLOAT:
			case Types.NUMERIC:
			case Types.REAL:
				// fix for copying when target is numeric and
				// clover source is integer - no precision can be
				// lost so we can use CopyInteger
				if (jetelType == DataFieldMetadata.INTEGER_FIELD) {
					return new CopyInteger(record, fromIndex, toIndex);
				} else if (jetelType == DataFieldMetadata.LONG_FIELD) {
					return new CopyLong(record, fromIndex, toIndex);
				} else if(jetelType == DataFieldMetadata.NUMERIC_FIELD) {
				    return new CopyNumeric(record, fromIndex, toIndex);
				} else {
					return new CopyDecimal(record, fromIndex, toIndex);
				}
			case Types.DATE:
				if (StringUtils.isEmpty(format)) {
					return new CopyDate(record, fromIndex, toIndex);
				}				
			case Types.TIME:
				if (StringUtils.isEmpty(format)) {
					return new CopyTime(record, fromIndex, toIndex);
				}				
			case Types.TIMESTAMP:
				if (StringUtils.isEmpty(format)) {
					return new CopyTimestamp(record, fromIndex, toIndex);
				}
				boolean isDate = fieldMetadata.isDateFormat();
				boolean isTime = fieldMetadata.isTimeFormat();
				if (isDate && isTime) {
					return new CopyTimestamp(record, fromIndex, toIndex);
				}else if (isDate) {
					return new CopyDate(record, fromIndex, toIndex);
				}else{
					return new CopyTime(record, fromIndex, toIndex);
				}
			case Types.BOOLEAN:
			case Types.BIT:
				if (jetelType == DataFieldMetadata.BOOLEAN_FIELD) {
					return new CopyBoolean(record, fromIndex, toIndex);
				} 
        		logger.warn("Metadata mismatch; type:" + jetelType + " SQLType:"+SQLType+" - using CopyString object.");
				return new CopyString(record, fromIndex, toIndex);
            case Types.BINARY:
            case Types.VARBINARY:
            case Types.LONGVARBINARY:
            case Types.BLOB:
            	if (!StringUtils.isEmpty(format) && format.equalsIgnoreCase(SQLUtil.BLOB_FORMAT_STRING)) {
                	return new CopyBlob(record, fromIndex, toIndex);
            	}
            	if (!StringUtils.isEmpty(format) && !format.equalsIgnoreCase(SQLUtil.BINARY_FORMAT_STRING)){
            		logger.warn("Unknown format " + StringUtils.quote(format) + " - using CopyByte object.");
            	}
                return new CopyByte(record, fromIndex, toIndex);
			// when Types.OTHER or unknown, try to copy it as STRING
			// this works for most of the NCHAR/NVARCHAR types on Oracle, MSSQL, etc.
			default:
			//case Types.OTHER:// When other, try to copy it as STRING - should work for NCHAR/NVARCHAR
				return new CopyString(record, fromIndex, toIndex);
			//default:
			//	throw new RuntimeException("SQL data type not supported: " + SQLType);
		}
	}


	/**
	 *  Description of the Class
	 *
	 * @author      dpavlis
	 * @since       October 7, 2002
	 * @revision    $Revision$
	 * @created     8. �ervenec 2003
	 */
	static class CopyNumeric extends CopySQLData {
		/**
		 *  Constructor for the CopyNumeric object
		 *
		 * @param  record      Description of Parameter
		 * @param  fieldSQL    Description of Parameter
		 * @param  fieldJetel  Description of Parameter
		 * @since              October 7, 2002
		 */
		CopyNumeric(DataRecord record, int fieldSQL, int fieldJetel) {
			super(record, fieldSQL, fieldJetel);
		}


		/**
		 *  Sets the Jetel attribute of the CopyNumeric object
		 *
		 * @param  resultSet         The new Jetel value
		 * @exception  SQLException  Description of Exception
		 * @since                    October 7, 2002
		 */
		void setJetel(ResultSet resultSet) throws SQLException {
			double i = resultSet.getDouble(fieldSQL);
			if (resultSet.wasNull()) {
				((NumericDataField) field).setValue((Object)null);
			} else {
				((NumericDataField) field).setValue(i);
			}
		}

		void setJetel(CallableStatement statement) throws SQLException{
			double i = statement.getDouble(fieldSQL);
			if (statement.wasNull()) {
				((NumericDataField) field).setValue((Object)null);
			} else {
				((NumericDataField) field).setValue(i);
			}
		}

		/**
		 *  Sets the SQL attribute of the CopyNumeric object
		 *
		 * @param  pStatement        The new SQL value
		 * @exception  SQLException  Description of Exception
		 * @since                    October 7, 2002
		 */
		void setSQL(PreparedStatement pStatement) throws SQLException {
			if (!field.isNull()) {
				pStatement.setDouble(fieldSQL, ((NumericDataField) field).getDouble());
			} else {
				pStatement.setNull(fieldSQL, java.sql.Types.NUMERIC);
			}

		}


		@Override
		public Object getDbValue(ResultSet resultSet) throws SQLException {
			double i = resultSet.getDouble(fieldSQL);
			return resultSet.wasNull() ? null : i;
		}


		@Override
		public Object getDbValue(CallableStatement statement)
				throws SQLException {
			double i = statement.getDouble(fieldSQL);
			return statement.wasNull() ? null : i;
		}
	}


	/**
	 *  Description of the Class
	 *
	 * @author      dpavlis
	 * @since       October 7, 2002
	 * @revision    $Revision$
	 * @created     8. �ervenec 2003
	 */
	static class CopyDecimal extends CopySQLData {
		/**
		 *  Constructor for the CopyDecimal object
		 *
		 * @param  record      Description of Parameter
		 * @param  fieldSQL    Description of Parameter
		 * @param  fieldJetel  Description of Parameter
		 * @since              October 7, 2002
		 */
		CopyDecimal(DataRecord record, int fieldSQL, int fieldJetel) {
			super(record, fieldSQL, fieldJetel);
		}


		/**
		 *  Sets the Jetel attribute of the CopyDecimal object
		 *
		 * @param  resultSet         The new Jetel value
		 * @exception  SQLException  Description of Exception
		 * @since                    October 7, 2002
		 */
		void setJetel(ResultSet resultSet) throws SQLException {
			BigDecimal i = resultSet.getBigDecimal(fieldSQL);
			if (resultSet.wasNull()) {
				((DecimalDataField) field).setValue((Object)null);
			} else {
				((DecimalDataField) field).setValue(new HugeDecimal(i, Integer.parseInt(field.getMetadata().getProperty(DataFieldMetadata.LENGTH_ATTR)), Integer.parseInt(field.getMetadata().getProperty(DataFieldMetadata.SCALE_ATTR)), false));
			}
		}

		void setJetel(CallableStatement statement) throws SQLException {
			BigDecimal i = statement.getBigDecimal(fieldSQL);
			if (statement.wasNull()) {
				((DecimalDataField) field).setValue((Object)null);
			} else {
				((DecimalDataField) field).setValue(new HugeDecimal(i, Integer.parseInt(field.getMetadata().getProperty(DataFieldMetadata.LENGTH_ATTR)), Integer.parseInt(field.getMetadata().getProperty(DataFieldMetadata.SCALE_ATTR)), false));
			}
		}

		/**
		 *  Sets the SQL attribute of the CopyNumeric object
		 *
		 * @param  pStatement        The new SQL value
		 * @exception  SQLException  Description of Exception
		 * @since                    October 7, 2002
		 */
		void setSQL(PreparedStatement pStatement) throws SQLException {
			if (!field.isNull()) {
				pStatement.setBigDecimal(fieldSQL, ((Decimal) ((DecimalDataField) field).getValue()).getBigDecimalOutput());
			} else {
				pStatement.setNull(fieldSQL, java.sql.Types.DECIMAL);
			}

		}
		
		@Override
		public Object getDbValue(ResultSet resultSet) throws SQLException {
			BigDecimal i = resultSet.getBigDecimal(fieldSQL);
			return resultSet.wasNull() ? null : i;
		}


		@Override
		public Object getDbValue(CallableStatement statement)
				throws SQLException {
			BigDecimal i = statement.getBigDecimal(fieldSQL);
			return statement.wasNull() ? null : i;
		}

	}

	/**
	 *  Description of the Class
	 *
	 * @author      dpavlis
	 * @since       2. b�ezen 2004
	 * @revision    $Revision$
	 * @created     8. �ervenec 2003
	 */
	static class CopyInteger extends CopySQLData {
		/**
		 *  Constructor for the CopyNumeric object
		 *
		 * @param  record      Description of Parameter
		 * @param  fieldSQL    Description of Parameter
		 * @param  fieldJetel  Description of Parameter
		 * @since              October 7, 2002
		 */
		CopyInteger(DataRecord record, int fieldSQL, int fieldJetel) {
			super(record, fieldSQL, fieldJetel);
		}


		/**
		 *  Sets the Jetel attribute of the CopyNumeric object
		 *
		 * @param  resultSet         The new Jetel value
		 * @exception  SQLException  Description of Exception
		 * @since                    October 7, 2002
		 */
		void setJetel(ResultSet resultSet) throws SQLException {
			int i = resultSet.getInt(fieldSQL);
			if (resultSet.wasNull()) {
				((IntegerDataField) field).setValue((Object)null);
			} else {
				((IntegerDataField) field).setValue(i);
			}
		}

		void setJetel(CallableStatement statement) throws SQLException {
			int i = statement.getInt(fieldSQL);
			if (statement.wasNull()) {
				((IntegerDataField) field).setValue((Object)null);
			} else {
				((IntegerDataField) field).setValue(i);
			}
		}

		/**
		 *  Sets the SQL attribute of the CopyNumeric object
		 *
		 * @param  pStatement        The new SQL value
		 * @exception  SQLException  Description of Exception
		 * @since                    October 7, 2002
		 */
		void setSQL(PreparedStatement pStatement) throws SQLException {
			if (!field.isNull()) {
				pStatement.setInt(fieldSQL, ((IntegerDataField) field).getInt());
			} else {
				pStatement.setNull(fieldSQL, java.sql.Types.INTEGER);
			}

		}
		
		@Override
		public Object getDbValue(ResultSet resultSet) throws SQLException {
			int i = resultSet.getInt(fieldSQL);
			return resultSet.wasNull() ? null : i;
		}


		@Override
		public Object getDbValue(CallableStatement statement)
				throws SQLException {
			int i = statement.getInt(fieldSQL);
			return statement.wasNull() ? null : i;
		}

	}

	static class CopyLong extends CopySQLData {
		/**
		 *  Constructor for the CopyNumeric object
		 *
		 * @param  record      Description of Parameter
		 * @param  fieldSQL    Description of Parameter
		 * @param  fieldJetel  Description of Parameter
		 * @since              October 7, 2002
		 */
		CopyLong(DataRecord record, int fieldSQL, int fieldJetel) {
			super(record, fieldSQL, fieldJetel);
		}


		/**
		 *  Sets the Jetel attribute of the CopyNumeric object
		 *
		 * @param  resultSet         The new Jetel value
		 * @exception  SQLException  Description of Exception
		 * @since                    October 7, 2002
		 */
		void setJetel(ResultSet resultSet) throws SQLException {
			long i = resultSet.getLong(fieldSQL);
			if (resultSet.wasNull()) {
				((LongDataField) field).setValue((Object)null);
			} else {
				((LongDataField) field).setValue(i);
			}
		}

		void setJetel(CallableStatement statement) throws SQLException {
			long i = statement.getLong(fieldSQL);
			if (statement.wasNull()) {
				((LongDataField) field).setValue((Object)null);
			} else {
				((LongDataField) field).setValue(i);
			}
		}

		/**
		 *  Sets the SQL attribute of the CopyNumeric object
		 *
		 * @param  pStatement        The new SQL value
		 * @exception  SQLException  Description of Exception
		 * @since                    October 7, 2002
		 */
		void setSQL(PreparedStatement pStatement) throws SQLException {
			if (!field.isNull()) {
				pStatement.setLong(fieldSQL, ((LongDataField) field).getLong());
			} else {
				pStatement.setNull(fieldSQL, java.sql.Types.BIGINT);
			}

		}
		
		@Override
		public Object getDbValue(ResultSet resultSet) throws SQLException {
			long i = resultSet.getLong(fieldSQL);
			return resultSet.wasNull() ? null : i;
		}


		@Override
		public Object getDbValue(CallableStatement statement)
				throws SQLException {
			long i = statement.getLong(fieldSQL);
			return statement.wasNull() ? null : i;
		}

	}

	/**
	 *  Description of the Class
	 *
	 * @author      dpavlis
	 * @since       October 7, 2002
	 * @revision    $Revision$
	 * @created     8. �ervenec 2003
	 */
	static class CopyString extends CopySQLData {
		/**
		 *  Constructor for the CopyString object
		 *
		 * @param  record      Description of Parameter
		 * @param  fieldSQL    Description of Parameter
		 * @param  fieldJetel  Description of Parameter
		 * @since              October 7, 2002
		 */
		CopyString(DataRecord record, int fieldSQL, int fieldJetel) {
			super(record, fieldSQL, fieldJetel);
		}


		/**
		 *  Sets the Jetel attribute of the CopyString object
		 *
		 * @param  resultSet         The new Jetel value
		 * @exception  SQLException  Description of Exception
		 * @since                    October 7, 2002
		 */
		void setJetel(ResultSet resultSet) throws SQLException {
			String fieldVal = resultSet.getString(fieldSQL);
			if (resultSet.wasNull()) {
				field.fromString(null);
			} else {
				field.fromString(fieldVal);
			}
		}

		void setJetel(CallableStatement statement) throws SQLException {
			String fieldVal = statement.getString(fieldSQL);
			if (statement.wasNull()) {
				field.fromString(null);
			} else {
				field.fromString(fieldVal);
			}
		}

		/**
		 *  Sets the SQL attribute of the CopyString object
		 *
		 * @param  pStatement        The new SQL value
		 * @exception  SQLException  Description of Exception
		 * @since                    October 7, 2002
		 */
		void setSQL(PreparedStatement pStatement) throws SQLException {
			if (!field.isNull()) {
		    	pStatement.setString(fieldSQL, field.toString());
		   	}else{
			   	pStatement.setNull(fieldSQL, java.sql.Types.VARCHAR);
		   	}
		}
		
		@Override
		public Object getDbValue(ResultSet resultSet) throws SQLException {
			String fieldVal = resultSet.getString(fieldSQL);
			return resultSet.wasNull() ? null : fieldVal;
		}


		@Override
		public Object getDbValue(CallableStatement statement)
				throws SQLException {
			String fieldVal = statement.getString(fieldSQL);
			return statement.wasNull() ? null : fieldVal;
		}


	}
	/**
	 *  Description of the Class
	 *
	 * @author      dpavlis
	 * @since       October 7, 2002
	 * @revision    $Revision$
	 * @created     8. �ervenec 2003
	 */
	static class CopyDate extends CopySQLData {

		java.sql.Date dateValue;


		/**
		 *  Constructor for the CopyDate object
		 *
		 * @param  record      Description of Parameter
		 * @param  fieldSQL    Description of Parameter
		 * @param  fieldJetel  Description of Parameter
		 * @since              October 7, 2002
		 */
		CopyDate(DataRecord record, int fieldSQL, int fieldJetel) {
			super(record, fieldSQL, fieldJetel);
			dateValue = new java.sql.Date(0);
		}


		/**
		 *  Sets the Jetel attribute of the CopyDate object
		 *
		 * @param  resultSet         The new Jetel value
		 * @exception  SQLException  Description of Exception
		 * @since                    October 7, 2002
		 */
		void setJetel(ResultSet resultSet) throws SQLException {
			Date date = resultSet.getDate(fieldSQL);
			if (resultSet.wasNull()) {
				((DateDataField) field).setValue((Object)null);
			}else{
				((DateDataField) field).setValue(date);
			}
			
		}

		void setJetel(CallableStatement statement) throws SQLException {
			Date date = statement.getDate(fieldSQL);
			if (statement.wasNull()) {
				((DateDataField) field).setValue((Object)null);
			}else{
				((DateDataField) field).setValue(date);
			}
			
		}

		/**
		 *  Sets the SQL attribute of the CopyDate object
		 *
		 * @param  pStatement        The new SQL value
		 * @exception  SQLException  Description of Exception
		 * @since                    October 7, 2002
		 */
		void setSQL(PreparedStatement pStatement) throws SQLException {
			if (!field.isNull()) {
			    if (inBatchUpdate){
                    pStatement.setDate(fieldSQL, new java.sql.Date(((DateDataField) field).getDate().getTime()));
			    }else{
			        dateValue.setTime(((DateDataField) field).getDate().getTime());
			        pStatement.setDate(fieldSQL, dateValue);
			    }
			} else {
				pStatement.setNull(fieldSQL, java.sql.Types.DATE);
			}
		}
		
		@Override
		public Object getDbValue(ResultSet resultSet) throws SQLException {
			Date date = resultSet.getDate(fieldSQL);
			return resultSet.wasNull() ? null : date;
		}


		@Override
		public Object getDbValue(CallableStatement statement)
				throws SQLException {
			Date date = statement.getDate(fieldSQL);
			return statement.wasNull() ? null : date;
		}

	}


	/**
	 *  Description of the Class
	 *
	 * @author      dpavlis
	 * @since       October 7, 2002
	 * @revision    $Revision$
	 * @created     8. �ervenec 2003
	 */
	static class CopyTime extends CopySQLData {

		java.sql.Time timeValue;


		/**
		 *  Constructor for the CopyDate object
		 *
		 * @param  record      Description of Parameter
		 * @param  fieldSQL    Description of Parameter
		 * @param  fieldJetel  Description of Parameter
		 * @since              October 7, 2002
		 */
		CopyTime(DataRecord record, int fieldSQL, int fieldJetel) {
			super(record, fieldSQL, fieldJetel);
			timeValue = new java.sql.Time(0);
		}


		/**
		 *  Sets the Jetel attribute of the CopyDate object
		 *
		 * @param  resultSet         The new Jetel value
		 * @exception  SQLException  Description of Exception
		 * @since                    October 7, 2002
		 */
		void setJetel(ResultSet resultSet) throws SQLException {
			Date time = resultSet.getTime(fieldSQL);
			if (resultSet.wasNull()) {
				((DateDataField) field).setValue((Object)null);
			}else{
				((DateDataField) field).setValue(time);
			}
			
		}

		void setJetel(CallableStatement statement) throws SQLException {
			Date time = statement.getTime(fieldSQL);
			if (statement.wasNull()) {
				((DateDataField) field).setValue((Object)null);
			}else{
				((DateDataField) field).setValue(time);
			}
			
		}

		/**
		 *  Sets the SQL attribute of the CopyDate object
		 *
		 * @param  pStatement        The new SQL value
		 * @exception  SQLException  Description of Exception
		 * @since                    October 7, 2002
		 */
		void setSQL(PreparedStatement pStatement) throws SQLException {
			if (!field.isNull()) {
			    if (inBatchUpdate){
                    pStatement.setTime(fieldSQL, new java.sql.Time(((DateDataField) field).getDate().getTime()));
			    }else{
			        timeValue.setTime(((DateDataField) field).getDate().getTime());
			        pStatement.setTime(fieldSQL, timeValue);
			    }
			} else {
				pStatement.setNull(fieldSQL, java.sql.Types.DATE);
			}
		}
		
		@Override
		public Object getDbValue(ResultSet resultSet) throws SQLException {
			Date time = resultSet.getTime(fieldSQL);
			return resultSet.wasNull() ? null : time;
		}


		@Override
		public Object getDbValue(CallableStatement statement)
				throws SQLException {
			Date time = statement.getTime(fieldSQL);
			return statement.wasNull() ? null : time;
		}

	}




	/**
	 *  Description of the Class
	 *
	 * @author      dpavlis
	 * @since       October 7, 2002
	 * @revision    $Revision$
	 * @created     8. �ervenec 2003
	 */
	static class CopyTimestamp extends CopySQLData {

		Timestamp timeValue;


		/**
		 *  Constructor for the CopyTimestamp object
		 *
		 * @param  record      Description of Parameter
		 * @param  fieldSQL    Description of Parameter
		 * @param  fieldJetel  Description of Parameter
		 * @since              October 7, 2002
		 */
		CopyTimestamp(DataRecord record, int fieldSQL, int fieldJetel) {
			super(record, fieldSQL, fieldJetel);
			timeValue = new Timestamp(0);
			timeValue.setNanos(0);// we don't count with nanos!
		}


		/**
		 *  Sets the Jetel attribute of the CopyTimestamp object
		 *
		 * @param  resultSet         The new Jetel value
		 * @exception  SQLException  Description of Exception
		 * @since                    October 7, 2002
		 */
		void setJetel(ResultSet resultSet) throws SQLException {
			Timestamp timestamp = resultSet.getTimestamp(fieldSQL);
			if (resultSet.wasNull()) {
				((DateDataField) field).setValue((Object)null);
			}else{
				((DateDataField) field).setValue(timestamp);
			}
			
		}

		void setJetel(CallableStatement statement) throws SQLException {
			Timestamp timestamp = statement.getTimestamp(fieldSQL);
			if (statement.wasNull()) {
				((DateDataField) field).setValue((Object)null);
			}else{
				((DateDataField) field).setValue(timestamp);
			}
			
		}

		/**
		 *  Sets the SQL attribute of the CopyTimestamp object
		 *
		 * @param  pStatement        The new SQL value
		 * @exception  SQLException  Description of Exception
		 * @since                    October 7, 2002
		 */
		void setSQL(PreparedStatement pStatement) throws SQLException {
			if (!field.isNull()) {
			    if (inBatchUpdate){
                    pStatement.setTimestamp(fieldSQL, new Timestamp(((DateDataField) field).getDate().getTime()));
			    }else{
			        timeValue.setTime(((DateDataField) field).getDate().getTime());
			        pStatement.setTimestamp(fieldSQL, timeValue);
			    }
			} else {
				pStatement.setNull(fieldSQL, java.sql.Types.TIMESTAMP);
			}
		}
		
		@Override
		public Object getDbValue(ResultSet resultSet) throws SQLException {
			Timestamp timestamp = resultSet.getTimestamp(fieldSQL);
			return resultSet.wasNull() ? null : timestamp;
		}


		@Override
		public Object getDbValue(CallableStatement statement)
				throws SQLException {
			Timestamp timestamp = statement.getTimestamp(fieldSQL);
			return statement.wasNull() ? null : timestamp;
		}

	}


	/**
	 *  Copy object for boolean/bit type fields. Expects String data
	 *  representation on Clover's side
	 *  for logical
	 *
	 * @author      dpavlis
	 * @since       November 27, 2003
	 * @revision    $Revision$
	 */
	static class CopyBoolean extends CopySQLData {

		/**
		 *  Constructor for the CopyBoolean object
		 *
		 * @param  record      Description of Parameter
		 * @param  fieldSQL    Description of Parameter
		 * @param  fieldJetel  Description of Parameter
		 * @since              October 7, 2002
		 */
		CopyBoolean(DataRecord record, int fieldSQL, int fieldJetel) {
			super(record, fieldSQL, fieldJetel);
		}


		/**
		 *  Sets the Jetel attribute of the CopyBoolean object
		 *
		 * @param  resultSet         The new Jetel value
		 * @exception  SQLException  Description of Exception
		 * @since                    October 7, 2002
		 */
		void setJetel(ResultSet resultSet) throws SQLException {
			boolean b = resultSet.getBoolean(fieldSQL);
			if (resultSet.wasNull()) {
				field.setValue((Object)null);
			}else{
				field.setValue(b);	
			}
			
		}

		void setJetel(CallableStatement statement) throws SQLException {
			boolean b = statement.getBoolean(fieldSQL);
			if (statement.wasNull()) {
				field.setValue((Object)null);
			}else{
				field.setValue(b);	
			}
			
		}

		/**
		 *  Sets the SQL attribute of the CopyString object
		 *
		 * @param  pStatement        The new SQL value
		 * @exception  SQLException  Description of Exception
		 * @since                    October 7, 2002
		 */
		void setSQL(PreparedStatement pStatement) throws SQLException {
			if (!field.isNull()) {
				boolean value = ((BooleanDataField) field).getBoolean();
				pStatement.setBoolean(fieldSQL,	value);
			}else{
				pStatement.setNull(fieldSQL, java.sql.Types.BOOLEAN);
			}
		}

		@Override
		public Object getDbValue(ResultSet resultSet) throws SQLException {
			boolean b = resultSet.getBoolean(fieldSQL);
			return resultSet.wasNull() ? null : b;
		}


		@Override
		public Object getDbValue(CallableStatement statement)
				throws SQLException {
			boolean b = statement.getBoolean(fieldSQL);
			return statement.wasNull() ? null : b;
		}

	}
    
    static class CopyByte extends CopySQLData {
        /**
         *  Constructor for the CopyByte object
         *
         * @param  record      Description of Parameter
         * @param  fieldSQL    Description of Parameter
         * @param  fieldJetel  Description of Parameter
         * @since              October 7, 2002
         */
        CopyByte(DataRecord record, int fieldSQL, int fieldJetel) {
            super(record, fieldSQL, fieldJetel);
        }


        /**
         *  Sets the Jetel attribute of the CopyByte object
         *
         * @param  resultSet         The new Jetel value
         * @exception  SQLException  Description of Exception
         * @since                    October 7, 2002
         */
        void setJetel(ResultSet resultSet) throws SQLException {
            byte[] i = resultSet.getBytes(fieldSQL);
            if (resultSet.wasNull()) {
                ((ByteDataField) field).setValue((Object)null);
            } else {
                ((ByteDataField) field).setValue(i);
            }
        }

        void setJetel(CallableStatement statement) throws SQLException {
            byte[] i = statement.getBytes(fieldSQL);
            if (statement.wasNull()) {
                ((ByteDataField) field).setValue((Object)null);
            } else {
                ((ByteDataField) field).setValue(i);
            }
        }

        /**
         *  Sets the SQL attribute of the CopyByte object
         *
         * @param  pStatement        The new SQL value
         * @exception  SQLException  Description of Exception
         * @since                    October 7, 2002
         */
        void setSQL(PreparedStatement pStatement) throws SQLException {
            if (!field.isNull()) {
                pStatement.setBytes(fieldSQL, ((ByteDataField) field).getByteArray());
            } else {
                pStatement.setNull(fieldSQL, java.sql.Types.BINARY);
            }

        }
        
		@Override
		public Object getDbValue(ResultSet resultSet) throws SQLException {
            byte[] i = resultSet.getBytes(fieldSQL);
			return resultSet.wasNull() ? null : i;
		}


		@Override
		public Object getDbValue(CallableStatement statement)
				throws SQLException {
            byte[] i = statement.getBytes(fieldSQL);
			return statement.wasNull() ? null : i;
		}

    }

    static class CopyBlob extends CopySQLData {
    	    	
    	Blob blob;
    	
        /**
         *  Constructor for the CopyByte object
         *
         * @param  record      Description of Parameter
         * @param  fieldSQL    Description of Parameter
         * @param  fieldJetel  Description of Parameter
         * @since              October 7, 2002
         */
        CopyBlob(DataRecord record, int fieldSQL, int fieldJetel) {
            super(record, fieldSQL, fieldJetel);
        }


        /**
         *  Sets the Jetel attribute of the CopyByte object
         *
         * @param  resultSet         The new Jetel value
         * @exception  SQLException  Description of Exception
         * @since                    October 7, 2002
         */
        void setJetel(ResultSet resultSet) throws SQLException {
        	blob = resultSet.getBlob(fieldSQL);
			if (blob != null) {
				blob = new SerialBlob(blob);
				if (blob.length() > Integer.MAX_VALUE) {
					throw new SQLException("Blob value is too long: " + blob.length());
				}
			}            
            if (resultSet.wasNull()) {
                ((ByteDataField) field).setValue((Object)null);
            } else {
                ((ByteDataField) field).setValue(blob.getBytes(1, (int)blob.length()));
            }
        }

        void setJetel(CallableStatement statement) throws SQLException {
        	blob = statement.getBlob(fieldSQL);
			if (blob != null) {
				blob = new SerialBlob(blob);
				if (blob.length() > Integer.MAX_VALUE) {
					throw new SQLException("Blob value is too long: " + blob.length());
				}
			}            
            if (statement.wasNull()) {
                ((ByteDataField) field).setValue((Object)null);
            } else {
                ((ByteDataField) field).setValue(blob.getBytes(1, (int)blob.length()));
            }
        }

        /**
         *  Sets the SQL attribute of the CopyByte object
         *
         * @param  pStatement        The new SQL value
         * @exception  SQLException  Description of Exception
         * @since                    October 7, 2002
         */
        void setSQL(PreparedStatement pStatement) throws SQLException {
            if (!field.isNull()) {
                pStatement.setBlob(fieldSQL, new SerialBlob(((ByteDataField) field).getByteArray()));
            } else {
                pStatement.setNull(fieldSQL, java.sql.Types.BLOB);
            }

        }
        
		@Override
		public Object getDbValue(ResultSet resultSet) throws SQLException {
        	blob = resultSet.getBlob(fieldSQL);
			if (blob != null) {
				blob = new SerialBlob(blob);
				if (blob.length() > Integer.MAX_VALUE) {
					throw new SQLException("Blob value is too long: " + blob.length());
				}
			}            
			return resultSet.wasNull() ? null : blob;
		}


		@Override
		public Object getDbValue(CallableStatement statement)
				throws SQLException {
        	blob = statement.getBlob(fieldSQL);
			if (blob != null) {
				blob = new SerialBlob(blob);
				if (blob.length() > Integer.MAX_VALUE) {
					throw new SQLException("Blob value is too long: " + blob.length());
				}
			}            
			return statement.wasNull() ? null : blob;
		}

    }
}


