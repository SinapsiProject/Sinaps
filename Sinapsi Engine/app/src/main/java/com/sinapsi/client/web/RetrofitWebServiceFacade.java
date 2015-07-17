package com.sinapsi.client.web;

import com.bgp.codec.DecodingMethod;
import com.bgp.codec.EncodingMethod;
import com.bgp.encryption.Encrypt;
import com.bgp.generator.KeyGenerator;
import com.bgp.keymanager.PublicKeyManager;
import com.bgp.keymanager.SessionKeyManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.sinapsi.client.AppConsts;
import com.sinapsi.client.web.gson.DeviceInterfaceInstanceCreator;
import com.sinapsi.client.websocket.WSClient;
import com.sinapsi.engine.execution.RemoteExecutionDescriptor;
import com.sinapsi.model.DeviceInterface;
import com.sinapsi.model.MacroInterface;
import com.sinapsi.model.UserInterface;
import com.sinapsi.model.impl.ActionDescriptor;
import com.sinapsi.model.impl.AvailabilityMap;
import com.sinapsi.model.impl.CommunicationInfo;
import com.sinapsi.model.impl.Device;
import com.sinapsi.model.impl.FactoryModel;
import com.sinapsi.model.impl.Macro;
import com.sinapsi.model.impl.SyncOperation;
import com.sinapsi.model.impl.TriggerDescriptor;
import com.sinapsi.model.impl.User;
import com.sinapsi.utils.Pair;
import com.sinapsi.webshared.ComponentFactoryProvider;
import com.sinapsi.webshared.gson.DeviceInterfaceTypeAdapter;
import com.sinapsi.webshared.gson.MacroTypeAdapter;
import com.sinapsi.webshared.gson.UserInterfaceTypeAdapter;
import com.sinapsi.webshared.wsproto.WebSocketEventHandler;

import org.java_websocket.handshake.ServerHandshake;

import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.List;

import javax.crypto.SecretKey;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.converter.GsonConverter;
import retrofit.mime.TypedOutput;

/**
 * The current main client-side multiplatform Sinapsi web service interfacing class.
 * This facade handles Retrofit and Gson libraries initialization,
 * http requests, serialization/deserialization and encryption/decryption of model objects,
 * key management, WebSocket connection, user login status.
 * All http requests to sinapsi web server should be done by calling methods in this class.
 */
public class RetrofitWebServiceFacade implements SinapsiWebServiceFacade, BGPKeysProvider {

    private FactoryModel factoryModel = new FactoryModel();

    //local keys
    private PublicKey publicKey;
    private PrivateKey privateKey;
    private SecretKey localUncryptedSessionKey;

    // keys generated by server
    private SecretKey serverSessionKey;
    private PublicKey serverPublicKey;

    private RetrofitInterface cryptedRetrofit;
    private RetrofitInterface uncryptedRetrofit;
    private RetrofitInterface loginRetrofit;

    private EncodingMethod encodingMethod;
    private DecodingMethod decodingMethod;


    private OnlineStatusProvider onlineStatusProvider;
    private WSClient wsClient = null;
    private WebSocketEventHandler webSocketEventHandler;
    private UserLoginStatusListener userLoginStatusListener;

    private UserInterface loggedUser = null;

    /**
     * Default ctor
     */
    public RetrofitWebServiceFacade(RestAdapter.Log retrofitLog,
                                    OnlineStatusProvider onlineStatusProvider,
                                    WebSocketEventHandler wsEventHandler,
                                    UserLoginStatusListener userLoginStatusListener,
                                    ComponentFactoryProvider componentFactoryProvider,
                                    EncodingMethod encodingMethod,
                                    DecodingMethod decodingMethod) {

        this.webSocketEventHandler = wsEventHandler;
        this.onlineStatusProvider = onlineStatusProvider;
        this.userLoginStatusListener = userLoginStatusListener;
        this.encodingMethod = encodingMethod;
        this.decodingMethod = decodingMethod;

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(
                        DeviceInterface.class,
                        new DeviceInterfaceTypeAdapter())
                .registerTypeAdapter(
                        Device.class,
                        new DeviceInterfaceTypeAdapter())
                .registerTypeAdapter(
                        UserInterface.class,
                        new UserInterfaceTypeAdapter())
                .registerTypeAdapter(
                        User.class,
                        new UserInterfaceTypeAdapter())
                .registerTypeAdapter(
                        MacroInterface.class,
                        new MacroTypeAdapter(componentFactoryProvider))
                .registerTypeAdapter(
                        Macro.class,
                        new MacroTypeAdapter(componentFactoryProvider))
                .create();


        final GsonConverter defaultGsonConverter = new GsonConverter(gson);
        final BGPGsonConverter cryptInOutGsonConverter = new BGPGsonConverter(gson, this, this.encodingMethod, this.decodingMethod);

        //This converter only decrypts data from server
        final GsonConverter loginGsonConverter = new BGPGsonConverter(gson, this, this.encodingMethod, this.decodingMethod) {
            @Override
            public TypedOutput toBody(Object object) {
                return defaultGsonConverter.toBody(object);
            }
        };

        RestAdapter cryptedRestAdapter = new RestAdapter.Builder()
                .setEndpoint(AppConsts.SINAPSI_URL)
                .setConverter(cryptInOutGsonConverter)
                .setLog(retrofitLog)
                .build();

        RestAdapter uncryptedRestAdapter = new RestAdapter.Builder()
                .setEndpoint(AppConsts.SINAPSI_URL)
                .setConverter(defaultGsonConverter)
                .setLog(retrofitLog)
                .build();

        RestAdapter loginRestAdapter = new RestAdapter.Builder()
                .setEndpoint(AppConsts.SINAPSI_URL)
                .setConverter(loginGsonConverter)
                .setLog(retrofitLog)
                .build();

        if (AppConsts.DEBUG_LOGS) {
            cryptedRestAdapter.setLogLevel(RestAdapter.LogLevel.FULL);
            uncryptedRestAdapter.setLogLevel(RestAdapter.LogLevel.FULL);
            loginRestAdapter.setLogLevel(RestAdapter.LogLevel.FULL);
        } else {
            cryptedRestAdapter.setLogLevel(RestAdapter.LogLevel.NONE);
            uncryptedRestAdapter.setLogLevel(RestAdapter.LogLevel.NONE);
            loginRestAdapter.setLogLevel(RestAdapter.LogLevel.NONE);
        }

        uncryptedRetrofit = uncryptedRestAdapter.create(RetrofitInterface.class);

        if (AppConsts.DEBUG_ENCRYPTED_RETROFIT)
            cryptedRetrofit = cryptedRestAdapter.create(RetrofitInterface.class);
        else
            cryptedRetrofit = uncryptedRetrofit;

        loginRetrofit = loginRestAdapter.create(RetrofitInterface.class);
    }

    /**
     * Ctor with default encoding/decoding methods
     */
    public RetrofitWebServiceFacade(RestAdapter.Log retrofitLog,
                                    OnlineStatusProvider onlineStatusProvider,
                                    WebSocketEventHandler wsEventHandler,
                                    UserLoginStatusListener userLoginStatusListener,
                                    ComponentFactoryProvider componentFactoryProvider) {
        this(retrofitLog, onlineStatusProvider, wsEventHandler, userLoginStatusListener, componentFactoryProvider, null, null);
        //using null as methods here is safe because will force bgp library to use
        //default apache common codec methods
    }

    @Override
    public PublicKey getPublicKey() {
        return publicKey;
    }

    @Override
    public PublicKey getServerPublicKey() {
        return serverPublicKey;
    }

    @Override
    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    @Override
    public SecretKey getServerSessionKey() {
        return serverSessionKey;
    }

    @Override
    public SecretKey getLocalUncryptedSessionKey() {
        return localUncryptedSessionKey;
    }


    /**
     * Converts a generic WebServiceCallback to a retrofit's Callback
     *
     * @param wsCallback the WebServiceCallback
     * @param <T>        the type
     * @return a retrofit's Callback
     */
    private static <T> Callback<T> convertCallback(final WebServiceCallback<T> wsCallback) {
        return new Callback<T>() {
            @Override
            public void success(T t, Response response) {
                wsCallback.success(t, response);
            }

            @Override
            public void failure(RetrofitError error) {
                wsCallback.failure(error);
            }
        };
    }

    /**
     * Check to ensure the keys are not null. Throws a runtime exception
     */
    private void checkKeys() {
        //noinspection ConstantConditions
        if (!AppConsts.DEBUG_BYPASS_LOGIN && (publicKey == null || privateKey == null || serverPublicKey == null || serverSessionKey == null))
            throw new RuntimeException("Missing key. Did you log in?");
    }

    /**
     * Request login
     *
     * @param email        email of the user
     * @param keysCallback public key and session key received from the server
     */
    @Override
    public void requestLogin(String email, String deviceName, String deviceModel, final WebServiceCallback<Pair<byte[], byte[]>> keysCallback) {
        if (!onlineStatusProvider.isOnline()) return;

        KeyGenerator kg = new KeyGenerator(1024, "RSA");
        final PrivateKey prk = kg.getPrivateKey();
        final PublicKey puk = kg.getPublicKey();

        try {
            uncryptedRetrofit.requestLogin(email,
                    deviceName,
                    deviceModel,
                    PublicKeyManager.convertToByte(puk),
                    new Callback<Pair<byte[], byte[]>>() {

                        @Override
                        public void success(Pair<byte[], byte[]> keys, Response response) {

                            if (keys.isErrorOccured()) {
                                keysCallback.failure(new RuntimeException(keys.getErrorDescription()));
                                return;
                            }


                            setPrivateKey(prk);
                            RetrofitWebServiceFacade.this.publicKey = puk;

                            try {
                                setServerSessionKey(SessionKeyManager.convertToKey(keys.getSecond()));
                                RetrofitWebServiceFacade.this.serverPublicKey = PublicKeyManager.convertToKey(keys.getFirst());
                            } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                                e.printStackTrace();
                            }


                            keysCallback.success(keys, response);
                        }

                        @Override
                        public void failure(RetrofitError error) {
                            keysCallback.failure(error);
                        }
                    });
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void login(final String email, String password, String deviceName, String deviceModel, final WebServiceCallback<User> result) {
        checkKeys();

        if (!onlineStatusProvider.isOnline()) return;

        try {
            Encrypt encrypt = new Encrypt(getServerPublicKey());
            encrypt.setCustomEncoding(encodingMethod);

            SecretKey sk = encrypt.getEncryptedSessionKey();
            localUncryptedSessionKey = encrypt.getSessionKey();
            loginRetrofit.login(email,
                    deviceName,
                    deviceModel,
                    new Pair<byte[], String>(SessionKeyManager.convertToByte(sk), encrypt.encrypt(password)),
                    new Callback<User>() {

                        @Override
                        public void success(User user, Response response) {

                            if (user.isErrorOccured()) {
                                result.failure(new RuntimeException(user.getErrorDescription()));
                                return;
                            }

                            try {
                                wsClient = new WSClient(email) {
                                    @Override
                                    public void onOpen(ServerHandshake handshakedata) {
                                        super.onOpen(handshakedata);
                                        webSocketEventHandler.onWebSocketOpen();
                                    }

                                    @Override
                                    public void onMessage(String message) {
                                        super.onMessage(message);
                                        webSocketEventHandler.onWebSocketMessage(message);
                                    }

                                    @Override
                                    public void onClose(int code, String reason, boolean remote) {
                                        super.onClose(code, reason, remote);
                                        webSocketEventHandler.onWebSocketClose(code, reason, remote);
                                    }

                                    @Override
                                    public void onError(Exception ex) {
                                        super.onError(ex);
                                        webSocketEventHandler.onWebSocketError(ex);
                                    }
                                };
                            } catch (URISyntaxException e) {
                                e.printStackTrace();
                            }


                            loggedUser = user;
                            userLoginStatusListener.onUserLogIn(user);
                            result.success(user, response);
                        }

                        @Override
                        public void failure(RetrofitError error) {
                            result.failure(error);
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void register(String email, String password, WebServiceCallback<User> result) {
        if (!onlineStatusProvider.isOnline()) return;
        uncryptedRetrofit.register(email, password, convertCallback(result));

    }

    @Override
    public void getAllDevicesByUser(UserInterface user, String deviceName, String deviceModel, WebServiceCallback<List<DeviceInterface>> result) {
        checkKeys();
        if (!onlineStatusProvider.isOnline()) return;
        cryptedRetrofit.getAllDevicesByUser(user.getEmail(), deviceName, deviceModel, convertCallback(result));
    }

    @Override
    public void registerDevice(UserInterface user,
                               String emailUser,
                               String deviceName,
                               String deviceModel,
                               String deviceType,
                               int deviceClientVersion,
                               final WebServiceCallback<Device> result) {
        checkKeys();
        if (!onlineStatusProvider.isOnline()) return;
        cryptedRetrofit.registerDevice(
                emailUser,
                deviceName,
                deviceModel,
                deviceType,
                deviceClientVersion,
                Integer.toString(user.getId()),
                new Callback<Device>() {
                    @Override
                    public void success(Device deviceInterface, Response response) {
                        if (deviceInterface == null) {
                            result.failure(new RuntimeException("Returned device from server is null"));
                            return;
                        }
                        if (deviceInterface.isErrorOccured()) {
                            result.failure(new RuntimeException(deviceInterface.getErrorDescription()));
                            return;
                        }
                        wsClient.setDeviceId(deviceInterface.getId());
                        result.success(deviceInterface, response);
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        result.failure(error);
                    }
                });
    }

    //Don't force call this before login or NullPointerException occurs
    @Override
    public void getAllMacros(DeviceInterface device, WebServiceCallback<Pair<Boolean, List<MacroInterface>>> result) {
        checkKeys();
        if (!onlineStatusProvider.isOnline()) return;
        cryptedRetrofit.getAllMacros(
                loggedUser.getEmail(),
                device.getName(),
                device.getModel(),
                convertCallback(result));
    }


    @Override
    public void setAvailableComponents(DeviceInterface device, List<TriggerDescriptor> triggers, List<ActionDescriptor> actions, WebServiceCallback<CommunicationInfo> result) {
        checkKeys();
        if (!onlineStatusProvider.isOnline()) return;
        cryptedRetrofit.setAvailableComponents(
                loggedUser.getEmail(),
                device.getName(),
                device.getModel(),
                new Pair<>(triggers, actions),
                convertCallback(result));
    }

    @Override
    public void getAvailableComponents(DeviceInterface device, WebServiceCallback<AvailabilityMap> result) {
        checkKeys();
        if (!onlineStatusProvider.isOnline()) return;
        cryptedRetrofit.getAvailableComponents(
                loggedUser.getEmail(),
                device.getName(),
                device.getModel(),
                convertCallback(result));
    }

    @Override
    public void continueMacroOnDevice(DeviceInterface fromDevice, DeviceInterface toDevice, RemoteExecutionDescriptor red, WebServiceCallback<CommunicationInfo> result) {
        checkKeys();
        if (!onlineStatusProvider.isOnline()) return;
        cryptedRetrofit.continueMacroOnDevice(
                fromDevice.getId(),
                toDevice.getId(),
                red,
                convertCallback(result));
    }


    public WSClient getWebSocketClient() {
        return wsClient;
    }

    @Override
    public void logout() {
        if (wsClient == null) return;
        if (wsClient.isOpen())
            wsClient.closeConnection();
        publicKey = null;
        privateKey = null;
        localUncryptedSessionKey = null;
        serverSessionKey = null;
        serverPublicKey = null;
        loggedUser = null;
        userLoginStatusListener.onUserLogOut();
    }

    @Override
    public void encryptionTest(String email, String deviceName, String deviceModel, final WebServiceCallback<Object> callback) {
        checkKeys();

        if (!onlineStatusProvider.isOnline()) return;

        // TEST 1: test crypted Retrofit
        String test = "test1";
        cryptedRetrofit.encryptionTest(
                email,
                deviceName,
                deviceModel,
                test,
                new Callback<Object>() {
                    @Override
                    public void success(Object o, Response response) {
                        System.out.println("######## success encryption test ########");
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        callback.failure(error);
                    }
                }
        );
    }

    public void pushChanges(DeviceInterface device, List<Pair<SyncOperation, MacroInterface>> changes, WebServiceCallback<List<Pair<SyncOperation, Integer>>> callback) {
        cryptedRetrofit.pushChanges(
                loggedUser.getEmail(),
                device.getName(),
                device.getModel(),
                changes,
                convertCallback(callback));
    }

    public UserInterface getLoggedUser() {
        return loggedUser;
    }

    public void setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    public void setServerSessionKey(SecretKey serverSessionKey) {
        this.serverSessionKey = serverSessionKey;
    }
}
