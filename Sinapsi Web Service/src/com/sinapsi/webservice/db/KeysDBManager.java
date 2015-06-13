package com.sinapsi.webservice.db;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.crypto.SecretKey;
import javax.servlet.http.HttpServlet;

import com.bgp.keymanager.PrivateKeyManager;
import com.bgp.keymanager.PublicKeyManager;
import com.bgp.keymanager.SessionKeyManager;


/**
 * Class that perform query about keys
 * @author Ayoub
 *
 */
public class KeysDBManager {
	private DatabaseController db;
	
	/**
	 * Default ctor
	 */
	public KeysDBManager() {
		db = new DatabaseController();
	}
	
	/**
     * Secondary ctor
     * @param db database controller
     */
	public KeysDBManager(DatabaseController db) {
	    this.db = db;
	}
	
	/**
	 * Secondary ctor, use the context listener to access to the db controller
	 * @param http http servlet obj
	 */
	public KeysDBManager(HttpServlet http) {
	    db = (DatabaseController) http.getServletContext().getAttribute("db");
	}
	
	/**
	 * Update the keys of user in the db
	 * @param email email of the user
	 * @param publicKey remote public key of the user generated by the client
	 * @throws Exception 
	 */
	public void updateUserPublicKey(String email, String publicKey) throws Exception {
		 Connection c = null;
	     PreparedStatement s = null;
	        
	     try {
	    	 c = db.connect();
	    	 String query = "UPDATE keys SET user_publickey = ? WHERE email = ?";
	         s = c.prepareStatement(query);
	         s.setString(1, publicKey);
	         s.setString(2, email);
	         s.execute();
	            
	     } catch(Exception e) {
	    	 db.disconnect(c, s);
	    	 throw e;
	     }
	     db.disconnect(c, s);
	}
	
	/**
     * Update the keys of user in the db
     * @param email email of the user
     * @param sessionKey remote public key of the user generated by the client
     * @throws Exception 
     */
    public void updateUserSessionKey(String email, String sessionKey) throws Exception {
         Connection c = null;
         PreparedStatement s = null;
            
         try {
             c = db.connect();
             String query = "UPDATE keys SET user_sessionkey = ? WHERE email = ?";
             s = c.prepareStatement(query);
             s.setString(1, sessionKey);
             s.setString(2, email);
             s.execute();
                
         } catch(Exception e) {
             db.disconnect(c, s);
             throw e;
         }
         db.disconnect(c, s);
    }
    
	/**
	 * Update the keys of server in the db
	 * @param email email of the user
	 * @param publicKey locale public key of the user generated by the client
	 * @param sessionKey local encrypted session key generated by the client
	 * @param privateKey 
	 * @throws Exception 
	 */
	public void updateServerKeys(String email, String publicKey, String privateKey, String sessionKey, String unSessionKey) throws Exception {
		 Connection c = null;
	     PreparedStatement s = null;
	        
	     try {
	    	 c = db.connect();
	    	 String query = "UPDATE keys " +
	    	                "SET server_publickey = ?, server_privatekey = ?, server_sessionkey = ?, server_uncryptedsessionkey = ? " +
	    	                "WHERE email = ?";
	         s = c.prepareStatement(query);
	         s.setString(1, publicKey);
	         s.setString(2, privateKey);
	         s.setString(3, sessionKey);
	         s.setString(4, unSessionKey);
	         s.setString(5, email);
	         s.execute();
	            
	     } catch(Exception e) {
	    	 db.disconnect(c, s);
	    	 throw e;
	     }
	     db.disconnect(c, s);
	}
	
	/**
	 * Return the client public key from db
	 * @param email email of the user associated to the publik key in the db
	 * @return public key object
	 * @throws Exception
	 */
	public PublicKey getUserPublicKey(String email) throws Exception {
		 Connection c = null;
	     PreparedStatement s = null;
	     ResultSet r = null;
	     String publicKey = null;
	     try {
	    	 c = db.connect();
	         s = c.prepareStatement("SELECT user_publickey FROM keys WHERE email = ?");
	         s.setString(1, email);
	         r = s.executeQuery();
	         if (r.next()) {
	            publicKey = r.getString("user_publickey");
	         }
	     } catch (SQLException ex) {
	         db.disconnect(c, s, r);
	         throw ex;
	     }
	     db.disconnect(c, s, r);
	     return PublicKeyManager.convertToKey(publicKey);
	}
	
	/**
	 * Return the local public key from db
	 * @param email email of the user associated to the publick key in the db
	 * @return public key object
	 * @throws Exception
	 */
	public PublicKey getServerPublicKey(String email) throws Exception {
		 Connection c = null;
	     PreparedStatement s = null;
	     ResultSet r = null;
	     String publicKey = null;
	     try {
	    	 c = db.connect();
	         s = c.prepareStatement("SELECT server_publickey FROM keys WHERE email = ?");
	         s.setString(1, email);
	         r = s.executeQuery();
	         if (r.next()) {
	            publicKey = r.getString("server_publickey");
	         }
	     } catch (SQLException ex) {
	         db.disconnect(c, s, r);
	         throw ex;
	     }
	     db.disconnect(c, s, r);
	     return PublicKeyManager.convertToKey(publicKey);
	}
	
	/**
	 * Return the private key from db
	 * @param email email of the user associated to the private key in the db
	 * @return public key object
	 * @throws Exception
	 */
	public PrivateKey getServerPrivateKey(String email) throws Exception {
		 Connection c = null;
	     PreparedStatement s = null;
	     ResultSet r = null;
	     String privateKey = null;
	     try {
	    	 c = db.connect();
	         s = c.prepareStatement("SELECT server_privatekey FROM keys WHERE email = ?");
	         s.setString(1, email);
	         r = s.executeQuery();
	         if (r.next()) {
	            privateKey = r.getString("server_privatekey");
	         }
	     } catch (SQLException ex) {
	         db.disconnect(c, s, r);
	         throw ex;
	     }
	     db.disconnect(c, s, r);
	     return PrivateKeyManager.convertToKey(privateKey);
	}
	
	/**
	 * Return the client  session key from db
	 * @param email email of the user associated to the session key in the db
	 * @return Secret key object
	 * @throws Exception
	 */
	public SecretKey getUserSessionKey(String email) throws Exception {
		 Connection c = null;
	     PreparedStatement s = null;
	     ResultSet r = null;
	     String sessionKey = null;
	     try {
	    	 c = db.connect();
	         s = c.prepareStatement("SELECT user_sessionkey FROM keys WHERE email = ?");
	         s.setString(1, email);
	         r = s.executeQuery();
	         if (r.next()) {
	            sessionKey = r.getString("user_sessionkey");
	         }
	     } catch (SQLException ex) {
	         db.disconnect(c, s, r);
	         throw ex;
	     }
	     db.disconnect(c, s, r);
	     return SessionKeyManager.convertToKey(sessionKey);
	}
	
	/**
	 * Return the local session key from db
	 * @param email email of the user associated to the session key in the db
	 * @return Secret key object
	 * @throws Exception
	 */
	public SecretKey getServerSessionKey(String email) throws Exception {
		 Connection c = null;
	     PreparedStatement s = null;
	     ResultSet r = null;
	     String sessionKey = null;
	     try {
	    	 c = db.connect();
	         s = c.prepareStatement("SELECT server_sessionkey FROM keys WHERE email = ?");
	         s.setString(1, email);
	         r = s.executeQuery();
	         if (r.next()) {
	            sessionKey = r.getString("server_sessionkey");
	         }
	     } catch (SQLException ex) {
	         db.disconnect(c, s, r);
	         throw ex;
	     }
	     db.disconnect(c, s, r);
	     return SessionKeyManager.convertToKey(sessionKey);
	}

	/**
     * Return the local uncrypted session key from db
     * @param email email of the user associated to the session key in the db
     * @return Secret key object
	 * @throws SQLException 
     * @throws Exception
     */
    public SecretKey getServerUncryptedSessionKey(String email) throws SQLException {
        Connection c = null;
        PreparedStatement s = null;
        ResultSet r = null;
        String sessionKey = null;
        try {
            c = db.connect();
            s = c.prepareStatement("SELECT server_uncryptedsessionkey FROM keys WHERE email = ?");
            s.setString(1, email);
            r = s.executeQuery();
            if (r.next()) {
               sessionKey = r.getString("server_uncryptedsessionkey");
            }
        } catch (SQLException ex) {
            db.disconnect(c, s, r);
            throw ex;
        }
        db.disconnect(c, s, r);
        return SessionKeyManager.convertToKey(sessionKey);
    }
}
