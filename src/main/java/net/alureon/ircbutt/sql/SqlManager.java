package net.alureon.ircbutt.sql;

import net.alureon.ircbutt.IRCbutt;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;

/**
 * SqlManager contains methods for working with the SQL database on
 * a more general level.  This class gets connections, creates tables,
 * handles disconnections, and houses other convenience methods for
 * working with the bot's SQL database.
 *
 * Note: The temptation to make methods in this class static is overwhelming.
 * I've done it time and time again, only to realize that it's a bad idea.
 * This class represents an INSTANCE of a connection to a database.  It can't be
 * static!  PreparedStatement objects REQUIRE a database connection to be made,
 * therefore these methods cannot be static.
 */
public class SqlManager {


    private Connection connection;
    private IRCbutt butt;
    private String ip;
    private String username;
    private String password;
    private int port;
    private String database;
    private static final Logger log = LogManager.getLogger();
    private static final int SQL_WAIT_TIME = 10;

    /**
     * Constructor for the SqlManager class.
     * @param butt The singleton IRCbutt object.
     */
    public SqlManager(final IRCbutt butt) {
        this.butt = butt;
        this.ip = butt.getYamlConfigurationFile().getSqlIp();
        this.username = butt.getYamlConfigurationFile().getSqlUsername();
        this.password = butt.getYamlConfigurationFile().getSqlPassword();
        this.port = butt.getYamlConfigurationFile().getSqlPort();
        this.database = butt.getYamlConfigurationFile().getSqlDatabase();
    }

    /**
     * This method connects the the database using the information supplied in
     * the YAML configuration file for the bot.
     */
    public void connectToDatabase() {
        String url = "jdbc:mysql://" + this.ip + ":" + this.port + "/" + this.database + "?autoreconnect=true";
        log.debug(url);
        try {
            connection = DriverManager.getConnection(url, this.username, this.password);
        } catch (SQLException ex) {
            log.error("Failed to establish SQL connection: ", ex);
            return;
        }
        log.info("[SQL backend connected]");
    }

    /**
     * Creates the database if it doesn't exist with UTF-8 character set.
     */
    public void createDatabaseIfNotExists() {
        boolean result = sqlUpdate("CREATE DATABASE IF NOT EXISTS ircbutt CHARACTER SET utf8;");
        log.debug("Create database returned {}", result);
    }

    /**
     * Creates the necessary SQL tables the bot needs.  Requires access to the
     * IRCbutt object to get custom table prefixes.
     */
    public void createTablesIfNotExists() {
        sqlUpdate("CREATE TABLE IF NOT EXISTS `" + butt.getYamlConfigurationFile().getSqlTablePrefix() + "_quotes` "
                + "(`id` SMALLINT PRIMARY KEY NOT NULL AUTO_INCREMENT, `user` VARCHAR(16) NOT NULL,"
                + "`quote` VARCHAR(300) NOT NULL, `grabbed_by` VARCHAR(16) NOT NULL,"
                + "`timestamp` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP) ENGINE=MyISAM");
        sqlUpdate("CREATE TABLE IF NOT EXISTS `" + butt.getYamlConfigurationFile().getSqlTablePrefix() + "_knowledge` "
                + "(`id` SMALLINT PRIMARY KEY NOT NULL AUTO_INCREMENT, `item` VARCHAR(32) NOT NULL UNIQUE,"
                + "`data` VARCHAR(300) NOT NULL, `added_by` VARCHAR(16) NOT NULL,"
                + "`timestamp` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP) ENGINE=MyISAM");
        sqlUpdate("CREATE TABLE IF NOT EXISTS `" + butt.getYamlConfigurationFile().getSqlTablePrefix() + "_karma` "
                + "(`id` SMALLINT PRIMARY KEY NOT NULL AUTO_INCREMENT, `item` VARCHAR(32) NOT NULL,"
                + "`karma` SMALLINT NOT NULL) ENGINE=MyISAM");
    }

    /**
     * TODO result of this function is ignored above.
     * Tests for SQL database connection and then executes the passed update
     * statement.
     * @param sql The sql string to execute (not user supplied!).
     * @return True if the update succeeded, false if exception was thrown.
     */
    private boolean sqlUpdate(final String sql) {
        checkConnection();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.executeUpdate();
            return true;
        } catch (SQLException ex) {
            log.error("Unable to update SQL database. Stacktrace: ", ex);
        }
        return false;
    }

    /**
     * Attempts to reconnect to the SQL Database.
     */
    private void reconnect() {
        log.debug("Disconnected from SQL Database. Reconnecting...");
        connectToDatabase();
    }

    /**
     * Returns a PreparedStatement object from the passed String.
     * @param query The query to create into a PreparedStatement.
     * @return The PreparedStatement object.
     */
    public PreparedStatement getPreparedStatement(final String query) {
        checkConnection();
        try {
            return connection.prepareStatement(query);
        } catch (SQLException | NullPointerException ex) {
            log.error("Unable to prepare SQL statement. Stacktrace: ", ex);
            return null;
        }
    }

    /**
     * This method prepares a statement regardless of the type of data.  I thought this
     * was rather clever.  If the object is of type String, the function will set all of
     * the data passed in, into the appropriate slots of the PreparedStatement object.
     * The same will happen if the passed data happens to be of type Integer.
     * @param ps The PreparedStatement to populate with data
     * @param objects An array of String or Integer objects to put into the PreparedStatement.
     */
    public void prepareStatement(final PreparedStatement ps, final Object... objects) {
        checkConnection();
        try {
            for (int i = 0; i < objects.length; i++) {
                if (objects[i] instanceof String) {
                    ps.setString((i + 1), (String) objects[i]);
                } else {
                    ps.setInt((i + 1), Integer.parseInt(String.valueOf(objects[i])));
                }
            }
        } catch (SQLException ex) {
            log.error("Failed to set parameter in PreparedStatement. StackTrace: ", ex);
        }
    }

    /**
     * Executes a query using the passed PreparedStatement and returns the
     * resulting ResultSet object. Convenience method.
     * @param ps The PreparedStatement object to execute query on.
     * @return The resulting ResultSet object.
     */
    public ResultSet getResultSet(final PreparedStatement ps) {
        checkConnection();
        try {
            return ps.executeQuery();
        } catch (SQLException ex) {
            log.error("Failed to execute query from PreparedStatement. StackTrace: ", ex);
            return null;
        }
    }

    /**
     * Tests whether or not we are connected to the SQL database.
     * @return True if connected, false if not.
     */
    private boolean isConnected() {
        try {
            return this.connection.isValid(SQL_WAIT_TIME);
        } catch (SQLException ex) {
            log.warn("Exception checking connection validity, ", ex);
            return false;
        }
    }

    /**
     * Convenience method that checks SQL database connection and attempts to
     * reconnect if not connected.
     */
    private void checkConnection() {
        if (!isConnected()) {
            reconnect();
        }
    }

}
