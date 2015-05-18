package com.sinapsi.client.web;

import com.bgp.encryption.Encrypt;
import com.bgp.generator.KeyGenerator;
import com.bgp.keymanager.PublicKeyManager;
import com.bgp.keymanager.SessionKeyManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sinapsi.android.Lol;
import com.sinapsi.client.AppConsts;
import com.sinapsi.engine.execution.RemoteExecutionDescriptor;
import com.sinapsi.model.DeviceInterface;
import com.sinapsi.model.MacroComponent;
import com.sinapsi.model.UserInterface;
import com.sinapsi.model.impl.User;

import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.List;

import javax.crypto.SecretKey;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.converter.GsonConverter;

/**
 * WebService draft class.
 * This is now a class containing some example functions
 * to understand how RetroFit initializes a RetrofitInterface
 * variable.
 */
public class RetrofitWebServiceFacade implements SinapsiWebServiceFacade, BGPKeysProvider {

    //local keys
    private PublicKey publicKey;
    private PrivateKey privateKey;

    // keys generated by server
    private SecretKey serverSessionKey;
    private PublicKey serverPublicKey;



    private RetrofitInterface cryptedRetrofit;
    private RetrofitInterface uncryptedRetrofit;

    /**
     * Default ctor
     *
     * @param retrofitLog
     */
    public RetrofitWebServiceFacade(RestAdapter.Log retrofitLog) {

        Gson gson = new GsonBuilder().create();

        RestAdapter cryptedRestAdapter = new RestAdapter.Builder()
                .setEndpoint(AppConsts.SINAPSI_URL)
                .setConverter(new BGPGsonConverter(gson, this))
                .setLog(retrofitLog)
                .build();

        RestAdapter uncryptedRestAdapter = new RestAdapter.Builder()
                .setEndpoint(AppConsts.SINAPSI_URL)
                .setConverter(new GsonConverter(gson))
                .setLog(retrofitLog)
                .build();

        if (AppConsts.DEBUG) {
            cryptedRestAdapter.setLogLevel(RestAdapter.LogLevel.FULL);
            uncryptedRestAdapter.setLogLevel(RestAdapter.LogLevel.FULL);
        } else {
            cryptedRestAdapter.setLogLevel(RestAdapter.LogLevel.NONE);
            uncryptedRestAdapter.setLogLevel(RestAdapter.LogLevel.NONE);
        }

        cryptedRetrofit = cryptedRestAdapter.create(RetrofitInterface.class);

        uncryptedRetrofit = uncryptedRestAdapter.create(RetrofitInterface.class);
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
        if (publicKey == null || privateKey == null) //TODO: check
            throw new RuntimeException(
                    "Missing public key. Did you log in?");
    }

    /**
     * Request login
     * @param email email of the user
     * @param keys public key and session key recived from the server
     */
    @Override
    public void requestLogin(String email, final WebServiceCallback<HashMap.SimpleEntry<String, String>> keys) {

        KeyGenerator kg = new KeyGenerator();
        final PrivateKey prk = kg.getPrivateKey();
        final PublicKey puk = kg.getPublicKey();

        uncryptedRetrofit.requestLogin(email,
                puk,
                new Callback<HashMap.SimpleEntry<String, String>>() {
            @Override
            public void success(HashMap.SimpleEntry<String, String> keys, Response response) {
                try {
                    RetrofitWebServiceFacade.this.publicKey = puk;
                    RetrofitWebServiceFacade.this.privateKey = prk;
                    RetrofitWebServiceFacade.this.serverPublicKey = PublicKeyManager.convertToKey(keys.getKey());
                    RetrofitWebServiceFacade.this.serverSessionKey = SessionKeyManager.convertToKey(keys.getValue());

                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                } catch (InvalidKeySpecException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void failure(RetrofitError error) {
                keys.failure(error);
            }
        });
    }

    @Override
    public void login(String email, String password, final WebServiceCallback<User> result) {

        try {
            Encrypt encrypt = new Encrypt(getServerPublicKey());
            SecretKey sk = encrypt.getEncryptedSessionKey();

            cryptedRetrofit.login(email, new HashMap.SimpleEntry<String, SecretKey>(password,sk), new Callback<User>() {
                @Override
                public void success(User user, Response response) {
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
        uncryptedRetrofit.register(email, password, convertCallback(result));
    }

    @Override
    public void getAllDevicesByUser(UserInterface user, WebServiceCallback<List<DeviceInterface>> result) {
        checkKeys();
        cryptedRetrofit.getAllDevicesByUser(user.getEmail(), convertCallback(result));
    }

    @Override
    public void registerDevice(UserInterface user,
                               String emailUser,
                               String deviceName,
                               String deviceModel,
                               String deviceType,
                               int deviceClientVersion,
                               WebServiceCallback<DeviceInterface> result) {
        checkKeys();
        cryptedRetrofit.registerDevice(
                emailUser,
                deviceName,
                deviceModel,
                deviceType,
                deviceClientVersion,
                Integer.toString(user.getId()),
                convertCallback(result));
    }

    @Override
    public void getAvailableActions(DeviceInterface device, WebServiceCallback<List<MacroComponent>> result) {
        checkKeys();
        cryptedRetrofit.getAvailableActions(
                device.getId(),
                convertCallback(result));
    }

    @Override
    public void setAvailableActions(DeviceInterface device, List<MacroComponent> actions, WebServiceCallback<String> result) {
        checkKeys();
        cryptedRetrofit.setAvailableActions(
                device.getId(),
                actions,
                convertCallback(result));
    }

    @Override
    public void getAvailableTriggers(DeviceInterface device, WebServiceCallback<List<MacroComponent>> result) {
        checkKeys();
        cryptedRetrofit.getAvailableTriggers(
                device.getId(),
                convertCallback(result));
    }

    @Override
    public void setAvailableTriggers(DeviceInterface device, List<MacroComponent> triggers, WebServiceCallback<String> result) {
        checkKeys();
        cryptedRetrofit.setAvailableTriggers(
                device.getId(),
                triggers,
                convertCallback(result));
    }

    @Override
    public void continueMacroOnDevice(DeviceInterface device, RemoteExecutionDescriptor red, WebServiceCallback<String> result) {
        //TODO: define this method
    }
}
