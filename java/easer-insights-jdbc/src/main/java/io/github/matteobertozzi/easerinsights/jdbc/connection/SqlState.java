/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.matteobertozzi.easerinsights.jdbc.connection;

import java.sql.SQLException;
import java.util.HashMap;

import io.github.matteobertozzi.rednaco.strings.StringUtil;

public enum SqlState {
  /**
   * Completion of the operation was successful and did not result in any type of warning or
   * exception condition.
   */
  SUCCESSFUL_COMPLETION("00000"),
  /**
   * The operation completed with a warning.
   */
  WARNING("01000"),
  /**
   * Returned if you DELETE with and without a Cursor in the same transaction.
   */
  CURSOR_OPERATION_CONFLICT("01001"),
  /**
   * There was an error during execution of the CLI function SQLDisconnect, but you won't be able to
   * see the details because the SQLDisconnect succeeded.
   */
  DISCONNECT_ERROR("01002"),
  /**
   * Null values were eliminated from the argument of a column function.
   */
  NULL_VALUE_ELIMINATED_IN_SET_FUNCTION("01003"),
  /**
   * The value of a string was truncated when assigned to another string data type with a shorter
   * length.
   */
  STRING_DATA_RIGHT_TRUNCATION_ON_READ("01004"),
  /**
   * Every descriptor area has multiple IDAs. You need one IDA per Column of a result set, or one
   * per parameter. Either reduce the number of Columns in the select list or reduce the number of
   * ?s in the SQL statement as a whole.
   */
  INSUFFICIENT_ITEM_DESCRIPTOR_AREAS("01005"),
  /**
   * A privilege was not revoked.
   */
  PRIVILEGE_NOT_REVOKED("01006"),
  /**
   * A privilege was not granted.
   */
  PRIVILEGE_NOT_GRANTED("01007"),
  /**
   * Suppose you say "CREATE TABLE ... CHECK (condition)", and the length of condition is larger
   * than what can be stored in the INFORMATION_SCHEMA View, CHECK_CONSTRAINTS, in its CHECK_CLAUSE
   * Column. The Table will still be created â€” this warning only means you won't be able to see the
   * entire information about the Table when you look at INFORMATION_SCHEMA. See also SqlState 0100A
   * and 0100B.
   */
  SEARCH_CONDITION_TOO_LONG_FOR_INFORMATION_SCHEMA("01009"),
  /**
   * This is the same as warning 01009 except that instead of a search condition (as in a CHECK
   * clause), you're using a query condition (usually SELECT). Thus, if you say "CREATE VIEW ..."
   * with a very long query, the size of Column VIEW_DEFINITION in View VIEWS in INFORMATION_SCHEMA
   * is a limiting factor.
   */
  QUERY_EXPRESSION_TOO_LONG_FOR_INFORMATION_SCHEMA("0100A"),
  /**
   * This is the same as warning 01009 except that instead of a search condition (as in a CHECK
   * clause), you're using a default value.
   */
  DEFAULT_VALUE_TOO_LONG_FOR_INFORMATION_SCHEMA("0100B"),
  /**
   * One or more ad hoc result sets were returned from the procedure.
   */
  DYNAMIC_RESULT_SETS_RETURNED("0100C"),
  /**
   * The cursor that was closed has been reopened on the next result set within the chain.
   */
  ADDITIONAL_RESULT_SETS_RETURNED("0100D"),
  /**
   * The procedure returned too many result sets.
   */
  TOO_MANY_RESULT_SETS("0100E"),
  /**
   * Returned if the character representation of the triggered SQL statement cannot be represented
   * in the Information Schema without truncation.
   */
  STATEMENT_TOO_LONG_FOR_INFORMATION_SCHEMA("0100F"),
  /**
   * Column cannot be mapped to XML.
   */
  WARNING_COLUMN_MAPPING_TO_XML("01010"),
  /**
   * The specified SQL-Java path was too long for information schema.
   */
  WARNING_PATH_TOO_LONG("01011"),
  /**
   * The maximum number of elements in the target array is less than the number of elements in the
   * source array and the extra source elements are all NULL. The database will assign as many of
   * the source element values to the target elements as is possible.
   */
  ARRAY_DATA_RIGHT_TRUNCATION_WARNING("0102F"),
  /**
   * One of the following exceptions occurred:
   * <ol> <li>The result of the SELECT INTO statement or the subselect of the INSERT statement was
   * an empty table.</li> <li>The number of rows identified in the searched UPDATE or DELETE
   * statement was zero./<li> <li>The position of the cursor referenced in the FETCH statement was
   * before the first row or after the last row of the result table.</li> <li>The fetch orientation
   * is invalid.</li> </ol>
   */
  NO_DATA("02000"),
  /**
   * No additional result sets returned.
   */
  NO_ADDITIONAL_DYNAMIC_RESULT_SETS_RETURNED("02001"),
  /**
   * An error occurred while executing dynamic SQL.
   */
  DYNAMIC_SQL_ERROR("07000"),
  /**
   * You might encounter this error if you set the length of a descriptor, then
   * <code>EXECUTE ... USING descriptor</code>. Often this exception results from consistency-check
   * failure during SQLExecute.
   */
  USING_CLAUSE_DOES_NOT_MATCH_DYNAMIC_PARAMETERS("07001"),
  /**
   * Often this exception results from consistency-check failure during SQLExecute.
   * Sometimes this exception results from an incorrect number of parameters. See also SqlState
   * 07008.
   */
  USING_CLAUSE_DOES_NOT_MATCH_TARGET_SPECIFICATIONS("07002"),
  /**
   * Returned if prepared statement does not conform to the Format and Syntax Rules of a dynamic
   * single row select statement.
   */
  CURSOR_SPECIFICATION_CANNOT_BE_EXECUTED("07003"),
  /**
   * You cannot simply EXECUTE an SQL statement which has dynamic parameters â€” you also need to use
   * a USING clause. See also SqlState 07007.
   */
  USING_CLAUSE_REQUIRED_FOR_DYNAMIC_PARAMETERS("07004"),
  /**
   * The statement name of the cursor identifies a prepared statement that cannot be associated with
   * a cursor.
   */
  PREPARED_STATEMENT_NOT_A_CURSOR_SPECIFICATION("07005"),
  /**
   * An input variable, transition variable, or parameter marker cannot be used, because of its data
   * type.
   */
  RESTRICTED_DATA_TYPE_ATTRIBUTE_VIOLATION("07006"),
  /**
   * You cannot simply EXECUTE an SQL statement which has result fields â€” you also need to use a
   * USING clause. See also SqlState 07004.
   */
  USING_CLAUSE_REQUIRED_FOR_RESULT_FIELDS("07007"),
  /**
   * Using the embedded SQL ALLOCATE DESCRIPTOR statement, you allocated a 5-item descriptor. Now
   * you are trying to use the sixth item in that descriptor. See also SqlState 07009.
   */
  INVALID_DESCRIPTOR_COUNT("07008"),
  /**
   * you are using a CLI descriptor function (such as SQLBindCol or SQLBindParameter) and the Column
   * number is less than 1 or greater than the maximum number of Columns. Or, you are using the
   * embedded SQL ALLOCATE DESCRIPTOR statement with a size which is less than 1 or greater than an
   * implementation-defined maximum. See also SqlState 07008.
   */
  INVALID_DESCRIPTOR_INDEX("07009"),
  /**
   * The supplied input or output arguments could not be transformed to the types expected by the
   * dynamic SQL script.
   */
  DATA_TYPE_TRANSFORM_FUNCTION_VIOLATION("0700B"),
  UNDEFINED_DATA_VALUE("0700C"),
  /**
   * Specified a descriptor item name DATA on a descriptor area whose type is ARRAY, ARRAY LOCATOR,
   * MULTISET, or MULTISET LOCATOR.
   */
  INVALID_DATA_TARGET("0700D"),
  /**
   * Invalid LEVEL specified in SET DESCRIPTOR statement.
   */
  INVALID_LEVEL_VALUE("0700E"),
  /**
   * Specified an invalid datetime interval.
   */
  INVALID_DATETIME_INTERVAL_CODE("0700F"),
  /**
   * Unable to establish a database connection.
   */
  CONNECTION_EXCEPTION("08000"),
  /**
   * Returned if the client is unable to connect to the database.
   */
  CANNOT_ESTABLISH_CONNECTION("08001"),
  /**
   * Returned if a connection with the specified name is already established.
   */
  CONNECTION_NAME_IN_USE("08002"),
  /**
   * Attempted to disconnect a non-existent database connection.
   */
  CONNECTION_DOES_NOT_EXIST("08003"),
  /**
   * Returned if the server rejected the client's connection request.
   */
  CONNECTION_REJECTED("08004"),
  /**
   * The specified connection could not be selected.
   */
  CONNECTION_FAILURE("08006"),
  /**
   * Connection lost while committing or rolling back a transaction. The client cannot verify
   * whether the transaction was committed successfully, rolled back or left active.
   */
  TRANSACTION_RESOLUTION_UNKNOWN("08007"),
  /**
   * A triggered SQL statement failed.
   */
  TRIGGERED_ACTION_EXCEPTION("09000"),
  /**
   * The "feature not supported" class identifies exception conditions that relate to features
   * you're trying to use, but that your DBMS hasn't implemented. The Standard does not specify what
   * will cause this SqlState, possibly because the expectation is that all features will be
   * supported.
   */
  FEATURE_NOT_SUPPORTED("0A000"),
  /**
   * A single transaction cannot be performed on multiple servers. Such a feature is sophisticated
   * and rare.
   */
  MULTIPLE_SERVER_TRANSACTIONS("0A001"),
  /**
   * An error occurred specifying a target for data.
   */
  INVALID_TARGET_TYPE_SPECIFICATION("0D000"),
  /**
   * An error occurred while specifying Schema paths.
   */
  INVALID_SCHEMA_NAME_LIST_SPECIFICATION("0E000"),
  /**
   * The "locator exception" class identifies exception conditions that relate to locators: BLOB and
   * CLOB data types, and their values.
   */
  LOCATOR_EXCEPTION("0F000"),
  /**
   * The locator value does not currently represent any value.
   */
  INVALID_LOCATOR_SPECIFICATION("0F001"),
  RESIGNAL_WHEN_HANDLER_NOT_ACTIVE("0K000"),
  /**
   * The specified grantor may not GRANT access.
   */
  INVALID_GRANTOR("0L000"),
  /**
   * Returned if the SQL-session context of the current SQL-session does not include a result set
   * sequence brought into existence by an invocation of SQL-invoked procedure by the active
   * SQL-invoked routine.
   */
  INVALID_SQL_INVOKED_PROCEDURE_REFERENCE("0M000"),
  /**
   * An error has occurred mapping SQL to XML or vice versa.
   */
  XML_MAPPING_ERROR("0N000"),
  UNMAPPABLE_XML_NAME("0N001"),
  /**
   * A character cannot be mapped to a valid XML character.
   */
  INVALID_XML_CHARACTER("0N002"),
  /**
   * Invalid role specification. The specified role does not exist or is not granted to the
   * specified user.
   */
  INVALID_ROLE_SPECIFICATION("0P000"),
  /**
   * A
   * <code>Transform Group name</code> could be invalid if it is used as a qualifier or as the
   * argument of SET TRANSFORM GROUP, and does not refer to an existing Transform Group or is not a
   * valid
   * <code>identifier</code>.
   */
  INVALID_TRANSFORM_GROUP_NAME_SPECIFICATION("0S000"),
  /**
   * Invoked a dynamic update or delete statement but the cursor specification conflicts with the
   * specification of the table it is operating over.
   */
  TARGET_TABLE_DISAGREES_WITH_CURSOR_SPECIFICATION("0T000"),
  /**
   * Attempted to assign a value to a non-updatable column.
   */
  CANNOT_ASSIGN_TO_NON_UPDATABLE_COLUMN("0U000"),
  /**
   * Returned if any object column is directly or indirectly referenced in the
   * <code>order by</code> clause of a dynamic cursor definition.
   */
  CANNOT_ASSIGN_TO_ORDERING_COLUMN("0V000"),
  /**
   * The statement is not allowed in a trigger.
   */
  PROHIBITED_STATEMENT_ENCOUNTERED_DURING_TRIGGER("0W000"),
  INVALID_FOREIGN_SERVER_SPECIFICATION("0X000"),
  PASS_THROUGH_SPECIFIED_CONDITION("0Y000"),
  INVALID_CURSOR_OPTION("0Y001"),
  INVALID_CURSOR_ALLOCATION("0Y002"),
  /**
   * An error occurred while invoking a diagnostics function.
   */
  DIAGNOSTICS_EXCEPTION("0Z000"),
  /**
   * Attempted to PUSH an operation onto the diagnostics stack but the number of operations in the
   * stack exceed the implementation-dependent maximum.
   */
  MAXIMUM_NUMBER_OF_STACKED_DIAGNOSTICS_AREAS_EXCEEDED("0Z001"),
  STACKED_DIAGNOSTICS_ACCESSED_WITHOUT_ACTIVE_HANDLER("0Z002"),
  /**
   * An error has occurred executing an XQuery statement.
   */
  XQUERY_ERROR("10000"),
  /**
   * None of the case statements matched the input.
   */
  CASE_NOT_FOUND("20000"),
  /**
   * The result of a SELECT INTO, scalar fullselect, or subquery of a basic predicate returned more
   * than one value.
   */
  CARDINALITY_VIOLATION("21000"),
  /**
   * The specified data was inappropriate for the column type.
   */
  DATA_EXCEPTION("22000"),
  /**
   * Character data, right truncation occurred; for example, an update or insert value is a string
   * that is too long for the column, or a datetime value cannot be assigned to a host variable,
   * because it is too small. No truncation actually occurs since the SQL statement fails. See
   * SqlState 01004.
   */
  STRING_DATA_RIGHT_TRUNCATION("22001"),
  /**
   * A null value, or the absence of an indicator parameter was detected; for example, the null
   * value cannot be assigned to a host variable, because no indicator variable is specified.
   */
  NULL_VALUE_NO_INDICATOR_PARAMETER("22002"),
  /**
   * A numeric value is out of range. Often this is the result of an arithmetic overflow. For
   * example, "UPDATE ... SET SMALLINT_COLUMN = 9999999999".
   */
  NUMERIC_VALUE_OUT_OF_RANGE("22003"),
  /**
   * A null value is not allowed.
   */
  NULL_VALUE_NOT_ALLOWED_BY_FUNCTION("22004"),
  /**
   * An error occurred on assignment.
   */
  ERROR_IN_ASSIGNMENT("22005"),
  /**
   * Specified an interval with an invalid format. For example, a year-month interval should contain
   * only a year integer, a '-' separator, and a month integer. See also SqlState 22015.
   */
  INVALID_INTERVAL_FORMAT("22006"),
  /**
   * An invalid datetime format was detected; that is, an invalid string representation or value was
   * specified. See also SqlState 22008, 22018.
   */
  INVALID_DATETIME_FORMAT("22007"),
  /**
   * Datetime field overflow occurred; for example, an arithmetic operation on a date or timestamp
   * has a result that is not within the valid range of dates. See also SqlState 22007.
   */
  DATETIME_FIELD_OVERFLOW("22008"),
  /**
   * The time zone displacement value is outside the range -12:59 to 14:00.
   * This could happen for "SET LOCAL TIME ZONE INTERVAL '22:00' HOUR TO MINUTE;", or for "TIMESTAMP
   * '1994-01-01 02:00:00+10:00'". (In the latter case, it is the result of the calculation that is
   * a problem.)
   */
  INVALID_TIME_ZONE_DISPLACEMENT_VALUE("22009"),
  /**
   * Attempted to use an invalid escape character.
   */
  ESCAPE_CHARACTER_CONFLICT("2200B"),
  /**
   * A required escape character was missing or in the wrong location.
   */
  INVALID_USE_OF_ESCAPE_CHARACTER("2200C"),
  /**
   * Returned if the length of the escape octet is not one.
   */
  INVALID_ESCAPE_OCTET("2200D"),
  /**
   * Attempted to assign a value to an array index whose value was null.
   */
  NULL_VALUE_IN_ARRAY_TARGET("2200E"),
  /**
   * Character strings must have a length of one.
   */
  ZERO_LENGTH_CHARACTER_STRING("2200F"),
  /**
   * Returned if the return value of a type-preserving function is not compatible with most specific
   * return type of the function.
   */
  MOST_SPECIFIC_TYPE_MISMATCH("2200G"),
  /**
   * Returned if a sequence generator cannot generate any more numbers because it has already
   * generated its maximum value and it is configured with NO CYCLE.
   */
  SEQUENCE_GENERATOR_LIMIT_EXCEEDED("2200H"),
  NONIDENTICAL_NOTATIONS_WITH_SAME_NAME("2200J"),
  NONIDENTICAL_UNPARSED_NOTATIONS_WITH_SAME_NAME("2200K"),
  /**
   * An XML value is not a well-formed document with a single root element.
   */
  NOT_AN_XML_DOCUMENT("2200L"),
  /**
   * A value failed to parse as a well-formed XML document or validate according to the XML schema.
   */
  INVALID_XML_DOCUMENT("2200M"),
  INVALID_XML_CONTENT("2200N"),
  /**
   * The result of an aggregate function is out of the range of an interval type.
   */
  INTERVAL_VALUE_OUT_OF_RANGE("2200P"),
  /**
   * The result of an aggregate function is out of the range of a multiset type.
   */
  MULTISET_VALUE_OVERFLOW("2200Q"),
  XML_VALUE_OVERFLOW("2200R"),
  /**
   * The XML comment is not valid.
   */
  INVALID_COMMENT("2200S"),
  /**
   * The XML processing instruction is not valid.
   */
  INVALID_PROCESSING_INSTRUCTION("2200T"),
  NOT_XQUERY_NODE("2200U"),
  INVALID_XQUERY_ITEM("2200V"),
  /**
   * An XML value contained data that could not be serialized.
   */
  XQUERY_SERIALIZATION_ERROR("2200W"),
  /**
   * The value of the indicator variable is less than zero but is not equal to -1 (SQL_NULL_DATA).
   */
  INVALID_INDICATOR_PARAMETER_VALUE("22010"),
  /**
   * A substring error occurred; for example, an argument of SUBSTR is out of range.
   */
  SUBSTRING_ERROR("22011"),
  /**
   * Attempted to divide a number by zero.
   */
  DIVISION_BY_ZERO("22012"),
  /**
   * Returned if the window frame bound preceding or following a WINDOW function is negative or
   * null.
   * @see <a href="https://en.wikipedia.org/wiki/Select_%28SQL%29#Window_function">Wiki Select Window Function</a>
   */
  INVALID_SIZE_IN_WINDOW_FUNCTION("22013"),
  /**
   * The value of an interval field exceeded its maximum value. See also SqlState 22006.
   */
  INTERVAL_FIELD_OVERFLOW("22015"),
  INVALID_DATALINK_DATA("22017"),
  /**
   * Tried to convert a value to a data type where the conversion is undefined, or when an error
   * occurred trying to convert.
   */
  INVALID_CHARACTER_VALUE_FOR_CAST("22018"),
  /**
   * The LIKE predicate has an invalid escape character.
   */
  INVALID_ESCAPE_CHARACTER("22019"),
  /**
   * A null argument was passed into a datalink constructor.
   */
  NULL_ARGUMENT_DATALINK_CONSTRUCTOR("2201A"),
  /**
   * Returned if the specified regular expression does not have a valid format.
   */
  INVALID_REGULAR_EXPRESSION("2201B"),
  /**
   * Attempted to insert a null row into a table that disallows them.
   */
  NULL_ROW_NOT_PERMITTED_IN_TABLE("2201C"),
  /**
   * The datalink length exceeds the maximum length.
   */
  DATALINK_TOO_LONG("2201D"),
  /**
   * Passed an invalid argument into a LOG function.
   */
  INVALID_ARGUMENT_FOR_LOGARITHM("2201E"),
  /**
   * Passed an invalid argument into a POWER function.
   */
  INVALID_ARGUMENT_FOR_POWER_FUNCTION("2201F"),
  /**
   * Passed an invalid argument into a WIDTH_BUCKET function.
   */
  INVALID_ARGUMENT_FOR_WIDTH_BUCKET_FUNCTION("2201G"),
  XQUERY_SEQUENCE_CANNOT_BE_VALIDATED("2201J"),
  XQUERY_DOCUMENT_NODE_CANNOT_BE_VALIDATED("2201K"),
  SCHEMA_NOT_FOUND("2201L"),
  ELEMENT_NAMESPACE_NOT_FOUND("2201M"),
  GLOBAL_ELEMENT_NOT_DECLARED("2201N"),
  NO_ELEMENT_WITH_SPECIFIED_QNAME("2201P"),
  NO_ELEMENT_WITH_SPECIFIED_NAMESPACE("2201Q"),
  VALIDATION_FAILURE("2201R"),
  /**
   * The regular expression specified in the xquery expression is invalid.
   */
  INVALID_XQUERY_REGULAR_EXPRESSION("2201S"),
  /**
   * The specified xquery option flag is invalid.
   */
  INVALID_XQUERY_OPTION_FLAG("2201T"),
  /**
   * Attempted to replace a substring that matches an XQuery regular expression with a replacement
   * character string, but the matching substring is a zero-length string.
   */
  ZERO_LENGTH_STRING("2201U"),
  /**
   * The replacement string specified in the xquery expression is invalid.
   */
  INVALID_XQUERY_REPLACEMENT_STRING("2201V"),
  /**
   * A character is not in the coded character set or the conversion is not supported.
   */
  CHARACTER_NOT_IN_REPERTOIRE("22021"),
  /**
   * Indicator is too small for size value.
   */
  INDICATOR_OVERFLOW("22022"),
  /**
   * A parameter or host variable value is invalid.
   */
  INVALID_PARAMETER_VALUE("22023"),
  /**
   * A NULL-terminated input host variable or parameter did not contain a NULL.
   */
  UNTERMINATED_C_STRING("22024"),
  /**
   * The LIKE predicate string pattern contains an invalid occurrence of an escape character.
   */
  INVALID_ESCAPE_SEQUENCE("22025"),
  /**
   * Attempted to update a bit string but the specified value does not match the length of the bit
   * string.
   */
  STRING_DATA_LENGTH_MISMATCH("22026"),
  /**
   * Attempted to invoke the TRIM function with a first argument whose length was greater than one
   * character.
   */
  TRIM_ERROR("22027"),
  /**
   * Returned when an operation inserts a non-character code point into a unicode string.
   */
  NONCHARACTER_IN_UCS_STRING("22029"),
  NULL_VALUE_IN_FIELD_REFERENCE("2202A"),
  /**
   * Attempted to invoke a mutator function on NULL.
   */
  NULL_INSTANCE_USED_IN_MUTATOR_FUNCTION("2202D"),
  /**
   * Attempted to reference an array index which is out of range.
   */
  ARRAY_ELEMENT_ERROR("2202E"),
  /**
   * The maximum number of elements in the target array is less than the number of elements in the
   * source array and the extra source elements are not all NULL.
   */
  ARRAY_DATA_RIGHT_TRUNCATION_EXCEPTION("2202F"),
  /**
   * Invalid repeat argument in SAMPLE clause.
   */
  INVALID_REPEAT_ARGUMENT_IN_SAMPLE_CLAUSE("2202G"),
  /**
   * The sample size was less than 0 or more than 100.
   */
  INVALID_SAMPLE_SIZE("2202H"),
  /**
   * The operation violated a table constraint.
   */
  INTEGRITY_CONSTRAINT_VIOLATION("23000"),
  /**
   * The update or delete of a parent key is prevented by a RESTRICT update or delete rule. See also
   * SqlState 40002.
   */
  RESTRICT_VIOLATION("23001"),
  /**
   * The cursor is closed or has no current row.
   */
  INVALID_CURSOR_STATE("24000"),
  /**
   * The operation violated the transaction constraints.
   */
  INVALID_TRANSACTION_STATE("25000"),
  /**
   * START TRANSACTION or DISCONNECT or SET SESSION AUTHORIZATION or SET ROLE statements cannot be
   * issued if a transaction has already been started.
   */
  ACTIVE_SQL_TRANSACTION("25001"),
  /**
   * SET TRANSACTION LOCAL ..., which applies only in multiple-server contexts, is illegal if a
   * local transaction is already happening.
   */
  BRANCH_TRANSACTION_ALREADY_ACTIVE("25002"),
  /**
   * Returned if the transaction access mode of the SQL-transaction is read-only and transaction
   * access mode specifies READWRITE.
   */
  INAPPROPRIATE_ACCESS_MODE_FOR_BRANCH_TRANSACTION("25003"),
  /**
   * Returned if the isolation level of the SQL-transaction is SERIALIZABLE and level of isolation
   * specifies anything except SERIALIZABLE, or if the isolation level of the SQL-transaction is
   * REPEATABLE READ and level of isolation specifies anything except REPEATABLE READ or
   * SERIALIZABLE, or if the isolation level of the SQL-transaction is READ COMMITTED and level of
   * isolation specifies READ UNCOMMITTED.
   */
  INAPPROPRIATE_ISOLATION_LEVEL_FOR_BRANCH_TRANSACTION("25004"),
  /**
   * Returned if SET LOCAL TRANSACTION is executed and there is no active transaction.
   */
  NO_ACTIVE_SQL_TRANSACTION_FOR_BRANCH_TRANSACTION("25005"),
  /**
   * An update operation is not valid because the transaction is read-only.
   */
  READ_ONLY_SQL_TRANSACTION("25006"),
  /**
   * Some DBMSs do not allow SQL-Schema statements (such as CREATE) to be mixed with SQL-data
   * statements (such as INSERT) in the same transaction.
   */
  SCHEMA_AND_DATA_STATEMENT_MIXING_NOT_SUPPORTED("25007"),
  /**
   * The SET TRANSACTION statement cannot be used to change isolation level if there is a held
   * Cursor made with a different isolation level left over from the last transaction.
   */
  HELD_CURSOR_REQUIRES_SAME_ISOLATION_LEVEL("25008"),
  /**
   * Probable cause: you failed to PREPARE an SQL statement and now you are trying to EXECUTE it.
   */
  INVALID_SQL_STATEMENT_NAME("26000"),
  /**
   * An attempt was made to modify the target table of the MERGE statement by a constraint or
   * trigger. See also SqlState 09000, 40004.
   */
  TRIGGERED_DATA_CHANGE_VIOLATION("27000"),
  /**
   * Authorization name is invalid.
   */
  INVALID_AUTHORIZATION_SPECIFICATION("28000"),
  /**
   * Attempted to "REVOKE GRANT OPTION FOR" with dependent privileges and without a "CASCADE".
   */
  DEPENDENT_PRIVILEGE_DESCRIPTORS_STILL_EXIST("2B000"),
  /**
   * Presumably an invalid Character set name would be one that begins with a digit, contains a
   * non-Latin letter, etc.
   */
  INVALID_CHARACTER_SET_NAME("2C000"),
  /**
   * An error occurred trying to commit or rollback a transaction.
   */
  INVALID_TRANSACTION_TERMINATION("2D000"),
  /**
   * For a CONNECT statement, the argument must be a valid
   * <code>identifier</code>.
   */
  INVALID_CONNECTION_NAME("2E000"),
  /**
   * SQL routine is a procedure or function which is written in SQL. SqlState class 2F identifies
   * exception conditions that relate to SQL routines. (Exceptions for non-SQL routines are class
   * 38.)
   */
  SQL_ROUTINE_EXCEPTION("2F000"),
  /**
   * The SQL function attempted to modify data, but the function was not defined as MODIFIES SQL
   * DATA.
   */
  MODIFYING_SQL_DATA_NOT_PERMITTED_BY_FUNCTION("2F002"),
  /**
   * The statement is not allowed in a function or procedure.
   */
  PROHIBITED_SQL_STATEMENT_ATTEMPTED_BY_FUNCTION("2F003"),
  /**
   * The SQL function attempted to read data, but the function was not defined as READS SQL DATA.
   */
  READING_SQL_DATA_NOT_PERMITTED("2F004"),
  /**
   * The function did not execute a RETURN statement.
   */
  FUNCTION_EXECUTED_NO_RETURN_STATEMENT("2F005"),
  /**
   * A
   * <code>Collation name</code> could be invalid if it is used as a qualifier or as the argument of
   * SET COLLATION, and does not refer to an existing Collation or is not a valid
   * <code>identifier</code>.
   */
  INVALID_COLLATION_NAME("2H000"),
  /**
   * Attempted to refer to a statement that should have been prepared, but was not.
   */
  INVALID_SQL_STATEMENT_IDENTIFIER("30000"),
  /**
   * Returned if, in embedded SQL, you use "EXECUTE ... USING DESCRIPTOR 'X';", a descriptor named X
   * must exist.
   */
  INVALID_SQL_DESCRIPTOR_NAME("33000"),
  /**
   * Cursor name is invalid.
   */
  INVALID_CURSOR_NAME("34000"),
  /**
   * With embedded SQL, you get this by saying "GET DIAGNOSTICS EXCEPTION 0". With the CLI, you get
   * this by calling SQLGetDiagRec or SQLGetDiagField with a RecordNumber parameter less than 1. If
   * RecordNumber is greater than the number of status records, you don't get this error. Instead,
   * you get an NO_DATA return code.
   */
  INVALID_CONDITION_NUMBER("35000"),
  /**
   * The "cursor sensitivity exception" class identifies exception conditions that relate to Cursors
   * and their sensitivity attribute.
   * If a holdable cursor is open during an SQL-transaction
   * <code>T</code> and it is held open for a subsequent SQL-transaction, then whether any
   * significant changes made to SQL-data (by
   * <code>T</code> or any subsequent SQL-transaction in which the cursor is held open) are visible
   * through that cursor in the subsequent SQL-transaction before that cursor is closed is
   * determined as follows:
   * - If the cursor is insensitive, then significant changes are not visible. - If the cursor is
   * sensitive, then the visibility of significant changes is implementation-defined. - If the
   * cursor is asensitive, then the visibility of significant changes.
   */
  CURSOR_SENSITIVITY_EXCEPTION("36000"),
  /**
   * A cursor is insensitive, and the SQL-implementation is unable to guarantee that significant
   * changes will be invisible through the cursor during the SQL-transaction in which it is opened
   * and every subsequent SQL-transaction during which it may be held open.
   */
  CURSOR_SENSITIVITY_REQUEST_REJECTED("36001"),
  /**
   * Returned if a sensitive cursor has not been held into a subsequent SQL-transaction, and the
   * change resulting from the successful execution of this statement could not be made visible to
   * the cursor.
   * For example, an attempt was made to execute a positioned DELETE statement, but there is a
   * sensitive Cursor open, and (for some implementation-dependent reason) the effects of the DELETE
   * cannot be made visible via that Cursor.
   */
  CURSOR_SENSITIVITY_REQUEST_FAILED("36002"),
  /**
   * An error occurred executing an external routine.
   * External routines are stored procedures implemented by non-SQL languages (i.e. Java).
   */
  EXTERNAL_ROUTINE_EXCEPTION("38000"),
  /**
   * The external routine is not allowed to execute SQL statements.
   */
  CONTAINING_SQL_NOT_PERMITTED("38001"),
  /**
   * The routine attempted to modify data, but the routine was not defined as MODIFIES SQL DATA.
   */
  MODIFYING_SQL_DATA_NOT_PERMITTED("38002"),
  /**
   * The statement is not allowed in a routine.
   */
  PROHIBITED_SQL_STATEMENT_ATTEMPTED_BY_ROUTINE("38003"),
  /**
   * The external routine attempted to read data, but the routine was not defined as READS SQL DATA.
   */
  READING_SQL_DATA_NOT_PERMITTED_BY_ROUTINE("38004"),
  /**
   * An error occurred before or after invoking an external routine.
   * External routines are stored procedures implemented by non-SQL languages (i.e. Java).
   */
  EXTERNAL_ROUTINE_INVOCATION_EXCEPTION("39000"),
  /**
   * A null value is not allowed for an IN or INOUT argument when using PARAMETER STYLE GENERAL or
   * an argument that is a Javaâ„¢ primitive type.
   */
  NULL_VALUE_NOT_ALLOWED_BY_EXTERNAL_ROUTINE("39004"),
  /**
   * An error occurred using a savepoint.
   */
  SAVEPOINT_EXCEPTION("3B000"),
  /**
   * The savepoint is not valid.
   */
  INVALID_SAVEPOINT_SPECIFICATION("3B001"),
  /**
   * The maximum number of savepoints has been reached.
   */
  TOO_MANY_SAVEPOINTS("3B002"),
  /**
   * Returned if the statement contains a preparable dynamic cursor name that is ambiguous.
   */
  AMBIGUOUS_CURSOR_NAME("3C000"),
  /**
   * A
   * <code>Catalog name</code> could be invalid if it is used as a qualifier or as the argument of
   * SET CATALOG, and does not refer to an existing Catalog or is not a valid
   * <code>identifier</code>.
   */
  INVALID_CATALOG_NAME("3D000"),
  /**
   * The schema (collection) name is invalid.
   */
  INVALID_SCHEMA_NAME("3F000"),
  /**
   * An error has triggered a rollback the transaction.
   */
  TRANSACTION_ROLLBACK("40000"),
  /**
   * The database engine has detected a deadlock. The transaction of this session has been rolled
   * back to solve the problem. A deadlock occurs when a session tries to lock a table another
   * session has locked, while the other session wants to lock a table the first session has locked.
   * As an example, session 1 has locked table A, while session 2 has locked table B. If session 1
   * now tries to lock table B and session 2 tries to lock table A, a deadlock has occurred.
   * Deadlocks that involve more than two sessions are also possible. To solve deadlock problems, an
   * application should lock tables always in the same order, such as always lock table A before
   * locking table B. For details, see <a href="http://en.wikipedia.org/wiki/Deadlock">Wikipedia
   * Deadlock</a>.
   */
  SERIALIZATION_FAILURE("40001"),
  /**
   * This occurs for COMMIT, if there were deferred Constraints (deferred Constraints aren't checked
   * until COMMIT time unless SET CONSTRAINTS IMMEDIATE is executed). So: you asked for COMMIT, and
   * what you got was ROLLBACK. See also SqlState 23000.
   */
  TRANSACTION_INTEGRITY_CONSTRAINT_VIOLATION("40002"),
  /**
   * The SQL-Connection was lost during execution of an SQL statement.
   */
  STATEMENT_COMPLETION_UNKNOWN("40003"),
  /**
   * This occurs for COMMIT, if there was a deferred Constraint â€” presumably a FOREIGN KEY
   * Constraint unless Triggers are supported by the DBMS â€” and there was an attempt to violate the
   * Constraint. See also SqlState 09000, 27000.
   */
  TRIGGERED_ACTION_EXCEPTION_AT_COMMIT("40004"),
  /**
   * Syntax errors include not just grammar or spelling errors, but "bind problems" such as failure
   * to find an Object. Access violations are due to lack of Privileges. A high security DBMS will
   * try to hide from the user whether the problem is "you don't have access to X" as opposed to "X
   * isn't there"; that's why these two different categories are lumped together in one SqlState
   * (thus users can't discover what the Table names are by trying out all the possibilities).
   */
  SYNTAX_ERROR_OR_ACCESS_RULE_VIOLATION("42000"),

  BASE_TABLE_OR_VIEW_NOT_FOUND("42S02"),
  INVALID_OBJECT_NAME("S0002"),

  /**
   * The INSERT or UPDATE is not allowed, because a resulting row does not satisfy the view
   * definition.
   */
  WITH_CHECK_OPTION_VIOLATION("44000"),
  /**
   * A user-defined exception was not handled.
   */
  UNHANDLED_USER_EXCEPTION("45000"),
  /**
   * An error has occurred with the object language binding (OLB).
   */
  OLB_SPECIFIC_ERROR("46000"),
  /**
   * The URL that was specified on the install or replace JAR procedure did not identify a valid JAR
   * file.
   */
  INVALID_URL("46001"),
  /**
   * The JAR name that was specified on the install, replace, or remove JAR procedure was invalid.
   * For example, this message could be issued for one of the following reasons:
   * 1. The JAR name might have the improper format 2. The JAR procedure cannot be replaced or
   * removed because it does not exist 3. The JAR procedure cannot be installed because it already
   * exists
   */
  INVALID_JAR_NAME("46002"),
  /**
   * The specified class in the jar file is currently in use by a defined routine, or the
   * replacement jar file does not contain the specified class for which a routine is defined.
   */
  INVALID_CLASS_DELETION("46003"),
  INVALID_REPLACEMENT("46005"),
  /**
   * Attempted to replace a JAR which has already been uninstalled.
   */
  CANNOT_REPLACE_UNINSTALLED_JAR("4600A"),
  /**
   * Attempted to remove a JAR which has already been uninstalled.
   */
  CANNOT_REMOVE_UNINSTALLED_JAR("4600B"),
  /**
   * The jar cannot be removed. It is in use.
   */
  INVALID_JAR_REMOVAL("4600C"),
  /**
   * The value provided for the new Java path is invalid.
   */
  INVALID_PATH("4600D"),
  /**
   * The alter of the jar failed because the specified path references itself.
   */
  SELF_REFERENCING_PATH("4600E"),
  INVALID_JAR_NAME_IN_PATH("46102"),
  /**
   * A Java exception occurred while loading a Java class. This error could be caused by a missing
   * class or an I/O error occurring when reading the class.
   */
  UNRESOLVED_CLASS_NAME("46103"),
  /**
   * The specified OLB feature is not supported.
   */
  UNSUPPORTED_FEATURE("46110"),
  /**
   * The class declaration was invalid.
   */
  INVALID_CLASS_DECLARATION("46120"),
  /**
   * The column name was invalid.
   */
  INVALID_COLUMN_NAME("46121"),
  /**
   * The operation contained an invalid number of columns.
   */
  INVALID_NUMBER_OF_COLUMNS("46122"),
  /**
   * The profile had an invalid state and may be corrupt.
   */
  INVALID_PROFILE_STATE("46130"),
  /**
   * An error has occurred while using a foreign data wrapper (FDW).
   */
  HV_ERROR("HV000"),
  HV_MEMORY_ALLOCATION_ERROR("HV001"),
  HV_DYNAMIC_PARAMETER_VALUE_NEEDED("HV002"),
  HV_INVALID_DATA_TYPE("HV004"),
  HV_COLUMN_NAME_NOT_FOUND("HV005"),
  HV_INVALID_TYPE_DESCRIPTORS("HV006"),
  HV_INVALID_COLUMN_NAME("HV007"),
  HV_INVALID_COLUMN_NUMBER("HV008"),
  HV_INVALID_USE_OF_NULL_POINTER("HV009"),
  HV_INVALID_STRING_FORMAT("HV00A"),
  HV_INVALID_HANDLE("HV00B"),
  HV_INVALID_OPTION_INDEX("HV00C"),
  HV_INVALID_OPTION_NAME("HV00D"),
  HV_OPTION_NAME_NOT_FOUND("HV00J"),
  HV_REPLY_HANDLE("HV00K"),
  HV_CANNOT_CREATE_EXECUTION("HV00L"),
  HV_CANNOT_CREATE_REPLY("HV00M"),
  HV_CANNOT_CREATE_CONNECTION("HV00N"),
  HV_NO_SCHEMAS("HV00P"),
  HV_SCHEMA_NOT_FOUND("HV00Q"),
  HV_TABLE_NOT_FOUND("HV00R"),
  HV_FUNCTION_SEQUENCE_ERROR("HV010"),
  HV_TOO_MANY_HANDLES("HV014"),
  HV_INCONSISTENT_DESCRIPTOR_INFORMATION("HV021"),
  HV_INVALID_ATTRIBUTE_VALUE("HV024"),
  HV_INVALID_STRING_OR_BUFFER_LENGTH("HV090"),
  HV_INVALID_DESCRIPTOR_FIELD_IDENTIFIER("HV091"),
  /**
   * An error has occurred while using a datalink.
   */
  DATALINK_ERROR("HW000"),
  EXTERNAL_FILE_NOT_LINKED("HW001"),
  EXTERNAL_FILE_ALREADY_LINKED("HW002"),
  REFERENCED_FILE_DOES_NOT_EXIST("HW003"),
  INVALID_WRITE_TOKEN("HW004"),
  INVALID_DATALINK_CONSTRUCTION("HW005"),
  INVALID_WRITE_PERMISSION_FOR_UPDATE("HW006"),
  REFERENCED_FILE_NOT_VALID("HW007"),
  /**
   * An error occurred in the glue code sitting between SQL and the native programming language.
   */
  CLI_ERROR("HY000"),
  /**
   * An error occurred while an SQL-client interacted with an SQL-server across a communications
   * network using an RDA Application Context.
   */
  REMOTE_DATABASE_ACCESS("HZ000"),
  ATTRIBUTE_NOT_PERMITTED("HZ301"),
  AUTHENTICATION_FAILURE("HZ302"),
  DUPLICATE_REQUEST_IDENT("HZ303"),
  ENCODING_NOT_SUPPORTED("HZ304"),
  FEATURE_NOT_SUPPORTED_MULTIPLE_SERVER_TRANSACTIONS("HZ305"),
  INVALID_ATTRIBUTE_TYPE("HZ306"),
  INVALID_FETCH_COUNT("HZ307"),
  INVALID_MESSAGE_TYPE("HZ308"),
  INVALID_OPERATION_SEQUENCE("HZ309"),
  INVALID_TRANSACTION_OPERATION_CODE("HZ310"),
  MISMATCH_BETWEEN_DESCRIPTOR_AND_ROW("HZ311"),
  NO_CONNECTION_HANDLE_AVAILABLE("HZ312"),
  NUMBER_OF_VALUES_DOES_NOT_MATCH_NUMBER_OF_ITEM_DESCRIPTORS("HZ313"),
  TRANSACTION_CANNOT_COMMIT("HZ314"),
  TRANSACTION_STATE_UNKNOWN("HZ315"),
  TRANSPORT_FAILURE("HZ316"),
  UNEXPECTED_PARAMETER_DESCRIPTOR("HZ317"),
  UNEXPECTED_ROW_DESCRIPTOR("HZ318"),
  UNEXPECTED_ROWS("HZ319"),
  VERSION_NOT_SUPPORTED("HZ320"),
  TCPIP_ERROR("HZ321"),
  TLS_ALERT("HZ322");

  private final String code;

  /**
   * Creates a new SqlState.
   * @param code the 5-character SqlState code
   */
  SqlState(final String code) {
    this.code = code;
    assert (code.length() == 5): code;
  }

  private static final HashMap<String, SqlState> codeToEnum;
  static {
    final SqlState[] enumValues = values();
    codeToEnum = new HashMap<>(enumValues.length);
    for (final SqlState value: enumValues) {
      final SqlState result = codeToEnum.put(value.getCode(), value);
      assert (result == null) : value.getCode();
    }
  }

  /**
   * Returns the code associated with the enum.
   * @return the code
   */
  public String getCode() {
    return code;
  }

  public static boolean isTableNotExist(final SQLException sqlException) {
    return SqlState.equals(sqlException, SqlState.BASE_TABLE_OR_VIEW_NOT_FOUND) ||
           SqlState.equals(sqlException, SqlState.INVALID_OBJECT_NAME);
  }

  public static boolean isPrimaryKeyExists(final SQLException sqlException) {
    return SqlState.equals(sqlException, SqlState.INTEGRITY_CONSTRAINT_VIOLATION);
  }

  public static boolean equals(final SQLException sqlException, final SqlState sqlState) {
    return StringUtil.equals(sqlState.getCode(), sqlException.getSQLState());
  }

  public static boolean equals(final SQLException sqlException, final SqlState... sqlState) {
    return equals(sqlException.getSQLState(), sqlState);
  }

  public static boolean equals(final String state, final SqlState sqlState) {
    return StringUtil.equals(sqlState.getCode(), state);
  }

  public static boolean equals(final String state, final SqlState... sqlState) {
    for (int i = 0; i < sqlState.length; ++i) {
      if (StringUtil.equals(sqlState[i].getCode(), state)) {
        return true;
      }
    }
    return false;
  }
}
