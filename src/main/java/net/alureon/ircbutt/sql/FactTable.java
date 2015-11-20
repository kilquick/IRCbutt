package net.alureon.ircbutt.sql;

import net.alureon.ircbutt.IRCbutt;
import net.alureon.ircbutt.util.SqlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class FactTable {


    private IRCbutt butt;
    final static Logger log = LoggerFactory.getLogger(FactTable.class);

    public FactTable(IRCbutt butt) {
        this.butt = butt;
    }

    public void insertKnowledge(String item, String data, String grabber) {
        String update = "INSERT INTO `" + butt.getYamlConfigurationFile().getSqlTablePrefix() + "_knowledge` (item,data,added_by) VALUES(?,?,?)";
        try (PreparedStatement ps = butt.getSqlManager().getPreparedStatement(update)) {
            ps.setString(1, item);
            ps.setString(2, data);
            ps.setString(3, grabber);
            ps.executeUpdate();
        } catch (SQLException ex) {
            log.error("Unable to insert knowledge into SQL database.  Stacktrace: ", ex);
        }
    }

    public String queryKnowledge(String item) {
        String query = "SELECT * FROM `" + butt.getYamlConfigurationFile().getSqlTablePrefix() + "_knowledge` WHERE item=?";
        try(PreparedStatement ps = butt.getSqlManager().getPreparedStatement(query)) {
        Object[] objects = { item };
        butt.getSqlManager().prepareStatement(ps, objects);
        ResultSet rs = butt.getSqlManager().getResultSet(ps);
                if (rs.next()) {
                    return rs.getString("data");
                }
            } catch (SQLException ex) {
                log.error("Failed to query knowledge database. StackTrace:", ex);
            } finally {
                SqlUtils.close(ps, rs);
            }
        return null;
    }

    public boolean deleteKnowledge(String item) {
        log.debug(item);
        String update = "DELETE FROM `" + butt.getYamlConfigurationFile().getSqlTablePrefix() + "_knowledge` WHERE item=?";
        try (PreparedStatement ps = butt.getSqlManager().getPreparedStatement(update)) {
            ps.setString(1, item);
            int rows = ps.executeUpdate();
            return (rows > 0); // if no rows have been updated then we haven't actually deleted anything
        } catch (SQLException ex) {
            log.error("Failed to delete knowledge from database. StackTrace:", ex);
        }
        return false;
    }

    public String getRandomData() {
        String query = "SELECT * FROM `" + butt.getYamlConfigurationFile().getSqlTablePrefix() + "_knowledge` ORDER BY RAND() LIMIT 1";
        try (PreparedStatement ps = butt.getSqlManager().getPreparedStatement(query);
             ResultSet rs = butt.getSqlManager().getResultSet(ps)) {
            if (rs.next()) {
                return rs.getString("data");
            }
        } catch (SQLException ex) {
            log.error("SQL Exception has occurred. StackTrace:", ex);
        }
        return null;
    }

    public String getFactInfo(String name) {
        String query = "SELECT * FROM `" + butt.getYamlConfigurationFile().getSqlTablePrefix() + "_knowledge` WHERE item=?";
        try (PreparedStatement ps = butt.getSqlManager().getPreparedStatement(query)) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int id = rs.getInt("id");
                String user = rs.getString("added_by");
                String time = rs.getString("timestamp");
                return "(" + id + ") " + name + ": added by " + user + " on " + time;
            }
        } catch (SQLException ex) {
            log.error("SQL Exception.  StackTrace:", ex);
        }
        return null;
    }

    public String findFact(String search) {
        String firstResult = null;
        String query = "SELECT * FROM `" + butt.getYamlConfigurationFile().getSqlTablePrefix() + "_knowledge` WHERE data LIKE ?";
        try(PreparedStatement ps = butt.getSqlManager().getPreparedStatement(query)) {
            ps.setString(1, "%" + search + "%");
            ResultSet rs = ps.executeQuery();
            int moreUses = 1;
            if (rs.next()) {
                firstResult = getFormattedFact(rs);
                while (rs.next()) {
                    switch (moreUses) {
                        case 1:
                            butt.getMoreHandler().setMore(getFormattedFact(rs));
                            break;
                        case 2:
                            butt.getMoreHandler().setMore2(getFormattedFact(rs));
                            break;
                        case 3:
                            butt.getMoreHandler().setMore3(getFormattedFact(rs));
                            break;
                        default:
                            continue;
                    }
                    moreUses++;
                }
            } else {
                //todo try to find a fact with similar name?
            }
        } catch (SQLException ex) {
            log.error("SQL Exception, ", ex);
        }
        return firstResult;
    }

    public String getFormattedFact(ResultSet rs) throws SQLException {
        return "(" + rs.getInt("id") + ") " + rs.getString("item") + ": " + rs.getString("data");
    }

    public String findFactById(int fid) {
        String query = "SELECT * FROM `" + butt.getYamlConfigurationFile().getSqlTablePrefix() + "_knowledge` WHERE id = ?";
        try (PreparedStatement ps = butt.getSqlManager().getPreparedStatement(query)) {
            ps.setInt(1, fid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int id = rs.getInt("id");
                String item = rs.getString("item");
                String data = rs.getString("data");
                return "(" + id + ") " + item + ": " + data;
            }
        } catch (SQLException ex) {
            log.error("SQL Exception, ", ex);
        }
        return null;
    }

    public void appendKnowledge(String item, String data, String grabber) {
        String update = "UPDATE `" + butt.getYamlConfigurationFile().getSqlTablePrefix() + "_knowledge` SET data = CONCAT(data, ' ', ?) WHERE item = ?";
        try (PreparedStatement ps = butt.getSqlManager().getPreparedStatement(update)) {
            ps.setString(1, data);
            ps.setString(2, item);
            ps.executeUpdate();
        } catch (SQLException ex) {
            log.error("Unable to insert knowledge into SQL database.  Stacktrace: ", ex);
        }
    }

}
