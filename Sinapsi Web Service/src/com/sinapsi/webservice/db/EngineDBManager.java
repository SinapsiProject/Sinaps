package com.sinapsi.webservice.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServlet;

import com.sinapsi.engine.ComponentFactory;
import com.sinapsi.engine.Trigger;
import com.sinapsi.model.MacroComponent;
import com.sinapsi.model.MacroInterface;
import com.sinapsi.webservice.engine.WebServiceEngine;

/**
 * Class that perform engine(triggers, actions, macros) query
 *
 */
public class EngineDBManager {
    private DatabaseController db;
    private HttpServlet http;
       
    /**
     * Default ctor
     */
    public EngineDBManager() {
        db = new DatabaseController();
    }
    
    /**
     * Secondaty ctor
     * @param db database controller
     */
    public EngineDBManager(DatabaseController db) {
        this.db = db;
    }
    
    /**
     * Secondary ctor, use the context listener to access to the db controller
     * @param http http servlet
     */
    public EngineDBManager(HttpServlet http) {
        this.http = http;
        db = (DatabaseController) http.getServletContext().getAttribute("db");
    }

    /**
     * Return the available Actions offered by a specific device
     * 
     * @param idDevice id device
     * @return list of actions
     * @throws SQLException
     */
    public List<MacroComponent> getAvailableAction(int idDevice) throws SQLException {
        Connection c = null;
        PreparedStatement s = null;
        ResultSet r = null;
        List<MacroComponent> actions = new ArrayList<MacroComponent>();

        try {
            c = db.connect();
            String query = "SELECT * FROM action, availableaction WHERE action.id = availableaction.idaction AND iddevice = ?";
            s = c.prepareStatement(query);
            s.setInt(1, idDevice);
            r = s.executeQuery();

            while (r.next()) {
                int minVersion = r.getInt("minversion");
                String name = r.getString("name");
                MacroComponent action = db.factory.newActionAbstraction(minVersion, name);
                actions.add(action);
            }

        } catch (SQLException ex) {
            db.disconnect(c, s, r);
            throw ex;
        }
        db.disconnect(c, s, r);
        return actions;
    }

    /**
     * Return the available Trigegrs offered by a specific device
     * 
     * @param idDevice id of the device
     * @return list of trigger
     * @throws SQLException
     */
    public List<MacroComponent> getAvailableTrigger(int idDevice) throws SQLException {
        Connection c = null;
        PreparedStatement s = null;
        ResultSet r = null;
        List<MacroComponent> triggers = new ArrayList<MacroComponent>();

        try {
            c = db.connect();
            String query = "SELECT * FROM trigger, availabletrigger WHERE trigger.id = availabletrigger.idtrigger AND iddevice = ?";
            s = c.prepareStatement(query);
            s.setInt(1, idDevice);
            r = s.executeQuery();

            while (r.next()) {
                int minVersion = r.getInt("minversion");
                String name = r.getString("name");
                MacroComponent trigger = db.factory.newTriggerAbstraction(minVersion, name);
                triggers.add(trigger);
            }

        } catch (SQLException ex) {
            db.disconnect(c, s, r);
            throw ex;
        }
        db.disconnect(c, s, r);
        return triggers;
    }

    /**
     * Get action, if doesn't exist, create it
     * 
     * @param name name of the action
     * @param versionAction min version of the action
     * @return id of the action
     * @throws SQLException
     */
    private int getAction(String name, int versionAction) throws SQLException {
        Connection c = null;
        PreparedStatement s = null;
        ResultSet r = null;
        int id = 0;

        try {
            c = db.connect();
            s = c.prepareStatement("SELECT id FROM action WHERE low(name) = low(?)");
            s.setString(1, name);
            r = s.executeQuery();

            if (r.next())
                id = r.getInt("id");

            else {
                s = c.prepareStatement("INSERT INTO action(name, minversion) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS);
                s.setString(1, name);
                s.setInt(2, versionAction);
                s.execute();
                r = s.getGeneratedKeys();
                r.next();
                id = r.getInt("id");
            }

        } catch (SQLException ex) {
            db.disconnect(c, s, r);
            throw ex;
        }
        db.disconnect(c, s, r);
        return id;
    }

    /**
     * Get trigger, if doesn't exist, create it
     * 
     * @param name name of the trigger
     * @param minVersion min version of the trigger
     * @return id of the trigger
     * @throws SQLException
     */
    public int getTrigger(String name, int minVersion) throws SQLException {
        Connection c = null;
        PreparedStatement s = null;
        ResultSet r = null;
        int id = 0;

        try {
            c = db.connect();
            s = c.prepareStatement("SELECT id FROM trigger WHERE low(name) = low(?)");
            s.setString(1, name);
            r = s.executeQuery();

            if (r.next())
                id = r.getInt("id");

            else {
                s = c.prepareStatement("INSERT INTO trigger(name, minversion) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS);
                s.setString(1, name);
                s.setInt(2, minVersion);
                s.execute();
                r = s.getGeneratedKeys();
                r.next();
                id = r.getInt("id");
            }

        } catch (SQLException ex) {
            db.disconnect(c, s, r);
            throw ex;
        }
        db.disconnect(c, s, r);
        return id;
    }

    /**
     * Return the name of the trigger from the id
     * @param id id of the trigger
     * @return
     * @throws SQLException 
     */
    public String getTrigger(int id) throws SQLException {
        Connection c = null;
        PreparedStatement s = null;
        ResultSet r = null;
        String name = null;

        try {
            c = db.connect();
            s = c.prepareStatement("SELECT name FROM trigger WHERE id = ?");
            s.setInt(1, id);
            r = s.executeQuery();

            if (r.next())
                name = r.getString("name");
            

        } catch (SQLException ex) {
            db.disconnect(c, s, r);
            throw ex;
        }
        db.disconnect(c, s, r);
        return name;
        
    }
    
    /**
     * Add list of available triggers in the db
     * 
     * @param idDevice device id
     * @param triggers list of triggers
     * @throws SQLException
     */
    public void addAvailableTriggers(int idDevice, List<MacroComponent> triggers) throws SQLException {
        Connection c = null;
        PreparedStatement s = null;
        ResultSet r = null;

        try {
            c = db.connect();

            for (int i = 0; i < triggers.size(); ++i) {
                MacroComponent trigger = triggers.get(i);
                String nameDevice = trigger.getName();
                int versionTrigger = trigger.getMinVersion();
                int idTrigger = getTrigger(nameDevice, versionTrigger);

                String query = "INSERT INTO availabletrigger(idtrigger, iddevice) VALUES(?, ?)";
                s = c.prepareStatement(query);
                s.setInt(1, idTrigger);
                s.setInt(2, idDevice);
            }

        } catch (SQLException e) {
            db.disconnect(c, s, r);
            throw e;
        }
        db.disconnect(c, s);
    }

    /**
     * Add list of available actions in the db
     * 
     * @param idDevice device id
     * @param actions  list of actions
     * @throws SQLException
     */
    public void addAvailableActions(int idDevice, List<MacroComponent> actions) throws SQLException {
        Connection c = null;
        PreparedStatement s = null;
        ResultSet r = null;

        try {
            c = db.connect();

            for (int i = 0; i < actions.size(); ++i) {
                MacroComponent action = actions.get(i);
                String nameDevice = action.getName();
                int versionTrigger = action.getMinVersion();
                int idAction = getAction(nameDevice, versionTrigger);

                String query = "INSERT INTO availableaction(idaction, iddevice) VALUES(?, ?)";
                s = c.prepareStatement(query);
                s.setInt(1, idAction);
                s.setInt(2, idDevice);
            }

        } catch (SQLException e) {
            db.disconnect(c, s, r);
            throw e;
        }
        db.disconnect(c, s);
    }

    /**
     * Return all macro of the user id
     * @param id id of the user
     * @return
     * @throws SQLException 
     */
    public List<MacroInterface> getUserMacro(int id) throws SQLException {
        Connection c = null;
        PreparedStatement s = null;
        ResultSet r = null;
        List<MacroInterface> macros = new ArrayList<MacroInterface>();
        DeviceDBManager deviceDb = new DeviceDBManager();

        try {
            c = db.connect();
            s = c.prepareStatement("SELECT * FROM macro WHERE macro.iduser = ?");
            s.setInt(1, id);
            r = s.executeQuery();

            while (r.next()) {
                // create a new macro from the information saved in the db
                MacroInterface macro = db.factory.newMacro(r.getString("name"), r.getInt("id"));
                
                // get the engine from the contex listener
                WebServiceEngine engine = (WebServiceEngine) http.getServletContext().getAttribute("engine");
                // get the component factory of the user
                ComponentFactory componentFactory = engine.getComponentFactoryForUser(id);
                // create a trigger form the information saved in the db
                Trigger trigger = componentFactory.newTrigger(getTrigger(r.getInt("idtrigger")), 
                                                              r.getString("triggerjson"), 
                                                              macro, 
                                                              deviceDb.getDevice(r.getInt("iddevice")));
                // set the trigger
                macro.setTrigger(trigger);
                macros.add(macro);
            }

        } catch (SQLException ex) {
            db.disconnect(c, s, r);
            throw ex;
        }
        db.disconnect(c, s, r);
        return macros;
    }
}
