package com.sinapsi.client.web;

import com.sinapsi.engine.execution.RemoteExecutionDescriptor;
import com.sinapsi.model.DeviceInterface;
import com.sinapsi.model.MacroComponent;
import com.sinapsi.model.UserInterface;
import com.sinapsi.model.impl.User;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;

/**
 * Platform independent interface, containing a collection of web methods.
 */
public interface SinapsiWebServiceFacade {

    /**
     * Platform independent version of Retrofit's Callback
     *
     * @param <T> expected response type
     */
    public interface WebServiceCallback<T> {
        void success(T t, Object response);

        void failure(Throwable error);
    }

    /**
     * Request login
     *
     * @param email email of the user
     * @param keys public key and session key recived from the server
     */
    public void requestLogin(String email,
                             WebServiceCallback<HashMap.SimpleEntry<byte[], byte[]>> keys);

    /**
     * Logs in the user
     *
     * @param email    the email
     * @param password the password
     * @param result   the User instance returned by the web service
     */
    public void login(String email,
                      String password,
                      WebServiceCallback<User> result);

    /**
     * Registers the users
     *
     * @param email    the email
     * @param password the password
     * @param result   the User instance returned by the web service
     */
    public void register(String email,
                         String password,
                         WebServiceCallback<User> result);


    /**
     * Gets all the device of the specified owner
     *
     * @param user   the user
     * @param result a List of DeviceInterface instances
     */
    public void getAllDevicesByUser(UserInterface user,
                                    WebServiceCallback<List<DeviceInterface>> result);


    /**
     * Adds a new device to the user's collection of devices
     * on the web service
     *
     * @param user                the user
     * @param deviceName          the name of the device
     * @param deviceModel         the model of the device
     * @param deviceType          the type of the device
     * @param deviceClientVersion the version of the Sinapsi Engine running on the device
     * @param result              the DeviceInterface instance returned by the web service
     */
    public void registerDevice(UserInterface user,
                               String emailUser,
                               String deviceName,
                               String deviceModel,
                               String deviceType,
                               int deviceClientVersion,
                               WebServiceCallback<DeviceInterface> result);

    /**
     * Gets the availability of actions on the specified device
     *
     * @param device the device
     * @param result a List of action instances
     */
    public void getAvailableActions(DeviceInterface device,
                                    WebServiceCallback<List<MacroComponent>> result);

    /**
     * Sets the availability of actions on the specified device
     *
     * @param device  the device
     * @param actions a List of action instances
     * @param result  a string containing infos on the operation result
     */
    public void setAvailableActions(DeviceInterface device,
                                    List<MacroComponent> actions,
                                    WebServiceCallback<String> result);

    /**
     * Gets the availability of triggers on the specified device
     *
     * @param device the device
     * @param result a List of trigger instances
     */
    public void getAvailableTriggers(DeviceInterface device,
                                     WebServiceCallback<List<MacroComponent>> result);


    /**
     * Sets the availability of triggers on the specified device
     *
     * @param device   the device
     * @param triggers a List of trigger instances
     * @param result   a string containing infos on the operation result
     */
    public void setAvailableTriggers(DeviceInterface device,
                                     List<MacroComponent> triggers,
                                     WebServiceCallback<String> result);

    /**
     * Asks the server to continued the specified macro on a remote device
     *
     * @param device the device
     * @param red    a descriptor containing infos about the execution that needs
     *               to be continued
     * @param result a string containing infos on the operation result
     */
    public void continueMacroOnDevice(DeviceInterface device,
                                      RemoteExecutionDescriptor red,
                                      WebServiceCallback<String> result);


}
