package provided.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Abstract base class providing utility methods for Data Access Object (DAO) implementations.
 * Facilitates database operations including transaction management, parameter binding, and
 * object extraction from result sets using JDBC and reflection.
 */
public abstract class DaoBase {

    /**
     * Initiates a database transaction by disabling auto-commit mode, allowing multiple
     * operations to be grouped and committed explicitly.
     *
     * @param conn The database connection to start the transaction on.
     * @throws SQLException If an error occurs while initiating the transaction.
     */
    protected void startTransaction(Connection conn) throws SQLException {
        conn.setAutoCommit(false);
    }

    /**
     * Commits the current transaction, persisting all changes to the database.
     *
     * @param conn The database connection to commit the transaction on.
     * @throws SQLException If an error occurs during the commit operation.
     */
    protected void commitTransaction(Connection conn) throws SQLException {
        conn.commit();
    }

    /**
     * Rolls back the current transaction, discarding all uncommitted changes.
     *
     * @param conn The database connection to roll back the transaction on.
     * @throws SQLException If an error occurs during the rollback operation.
     */
    protected void rollbackTransaction(Connection conn) throws SQLException {
        conn.rollback();
    }

    /**
     * Binds a parameter to a prepared statement, handling null values and mapping
     * Java types to appropriate SQL types.
     *
     * @param stmt The prepared statement to set the parameter on.
     * @param parameterIndex The 1-based index of the parameter in the SQL statement.
     * @param value The value to bind, which may be null.
     * @param classType The Java class type of the parameter, used to determine the SQL type.
     * @throws SQLException If an error occurs while setting the parameter.
     * @throws DaoException If the parameter type is unsupported.
     */
    protected void setParameter(PreparedStatement stmt, int parameterIndex, Object value,
            Class<?> classType) throws SQLException {
        int sqlType = convertJavaClassToSqlType(classType);

        if (Objects.isNull(value)) {
            stmt.setNull(parameterIndex, sqlType);
        } else {
            switch (sqlType) {
                case Types.DECIMAL:
                    stmt.setBigDecimal(parameterIndex, (BigDecimal) value);
                    break;
                case Types.DOUBLE:
                    stmt.setDouble(parameterIndex, (Double) value);
                    break;
                case Types.INTEGER:
                    stmt.setInt(parameterIndex, (Integer) value);
                    break;
                case Types.OTHER:
                    stmt.setObject(parameterIndex, value);
                    break;
                case Types.VARCHAR:
                    stmt.setString(parameterIndex, (String) value);
                    break;
                default:
                    throw new DaoException("Unsupported parameter type: " + classType);
            }
        }
    }

    /**
     * Maps a Java class type to a corresponding SQL type from {@link java.sql.Types}.
     *
     * @param classType The Java class type to map.
     * @return The corresponding SQL type.
     * @throws DaoException If the class type is not supported.
     */
    private int convertJavaClassToSqlType(Class<?> classType) {
        if (Integer.class.equals(classType)) {
            return Types.INTEGER;
        }
        if (String.class.equals(classType)) {
            return Types.VARCHAR;
        }
        if (Double.class.equals(classType)) {
            return Types.DOUBLE;
        }
        if (BigDecimal.class.equals(classType)) {
            return Types.DECIMAL;
        }
        if (LocalTime.class.equals(classType)) {
            return Types.OTHER;
        }
        throw new DaoException("Unsupported class type: " + classType.getName());
    }

    /**
     * Determines the next sequence number for a child entity by counting existing
     * child rows for a parent entity and incrementing by one. Suitable for basic
     * ordering but does not support reordering or deletion of entities.
     *
     * @param conn The database connection.
     * @param id The ID of the parent entity.
     * @param tableName The name of the table containing child rows.
     * @param idName The name of the column referencing the parent ID.
     * @return The next sequence number (count of child rows plus one).
     * @throws SQLException If a database error occurs.
     */
    protected Integer getNextSequenceNumber(Connection conn, Integer id, String tableName,
            String idName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + tableName + " WHERE " + idName + " = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            setParameter(stmt, 1, id, Integer.class);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) + 1;
                }
                return 1;
            }
        }
    }

    /**
     * Retrieves the primary key value of the most recently inserted row in the
     * specified table using the MySQL LAST_INSERT_ID() function.
     *
     * @param conn The database connection.
     * @param table The name of the table to query.
     * @return The primary key value of the last inserted row.
     * @throws SQLException If a database error occurs or no result is returned.
     */
    protected Integer getLastInsertId(Connection conn, String table) throws SQLException {
        String sql = String.format("SELECT LAST_INSERT_ID() FROM %s", table);

        try (Statement stmt = conn.createStatement()) {
            try (ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                throw new SQLException("Unable to retrieve the primary key value. No result set returned.");
            }
        }
    }

    /**
     * Constructs an object of the specified type from a result set using reflection.
     * Maps result set columns (in snake_case) to object fields (in camelCase), preserving
     * default field values for columns not present in the result set. The target class
     * must have a no-argument constructor.
     *
     * @param <T> The type of object to construct.
     * @param rs The result set, positioned on the row to extract.
     * @param classType The class type of the object to create.
     * @return A populated object of the specified type.
     * @throws DaoException If an error occurs during object construction or field population.
     */
    protected <T> T extract(ResultSet rs, Class<T> classType) {
        try {
            Constructor<T> con = classType.getConstructor();
            T obj = con.newInstance();

            for (Field field : classType.getDeclaredFields()) {
                String colName = camelCaseToSnakeCase(field.getName());
                Class<?> fieldType = field.getType();
                field.setAccessible(true);
                Object fieldValue = null;

                try {
                    fieldValue = rs.getObject(colName);
                } catch (SQLException e) {
                    // Column not found in result set; field retains its default value.
                }

                if (Objects.nonNull(fieldValue)) {
                    if (fieldValue instanceof Time && fieldType.equals(LocalTime.class)) {
                        fieldValue = ((Time) fieldValue).toLocalTime();
                    } else if (fieldValue instanceof Timestamp && fieldType.equals(LocalDateTime.class)) {
                        fieldValue = ((Timestamp) fieldValue).toLocalDateTime();
                    }
                    field.set(obj, fieldValue);
                }
            }

            return obj;
        } catch (Exception e) {
            throw new DaoException("Unable to construct object of type " + classType.getName(), e);
        }
    }

    /**
     * Converts a camelCase identifier to snake_case for mapping Java field names
     * to SQL column names.
     *
     * @param identifier The camelCase identifier to convert.
     * @return The snake_case equivalent of the identifier.
     */
    private String camelCaseToSnakeCase(String identifier) {
        StringBuilder nameBuilder = new StringBuilder();

        for (char ch : identifier.toCharArray()) {
            if (Character.isUpperCase(ch)) {
                nameBuilder.append('_').append(Character.toLowerCase(ch));
            } else {
                nameBuilder.append(ch);
            }
        }

        return nameBuilder.toString();
    }

    /**
     * Custom exception for errors occurring in {@link DaoBase} operations, wrapping
     * {@link RuntimeException} to provide detailed error messages and causes.
     */
    @SuppressWarnings("serial")
    static class DaoException extends RuntimeException {

        /**
         * Constructs a {@code DaoException} with the specified message and cause.
         *
         * @param message The error message.
         * @param cause The underlying cause of the exception.
         */
        public DaoException(String message, Throwable cause) {
            super(message, cause);
        }

        /**
         * Constructs a {@code DaoException} with the specified message.
         *
         * @param message The error message.
         */
        public DaoException(String message) {
            super(message);
        }
    }
}