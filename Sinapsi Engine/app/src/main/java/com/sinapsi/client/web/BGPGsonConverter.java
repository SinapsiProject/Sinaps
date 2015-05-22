package com.sinapsi.client.web;

import com.bgp.codec.DecodingMethod;
import com.bgp.codec.EncodingMethod;
import com.bgp.decryption.Decrypt;
import com.bgp.encryption.Encrypt;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.HashMap;
import javax.crypto.SecretKey;
import retrofit.converter.ConversionException;
import retrofit.converter.GsonConverter;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedOutput;

/**
 * Custom Gson Converter extension class to support
 * BGP encryption/decryption.
 */
public class BGPGsonConverter extends GsonConverter {

    //This is also here because in the base class gson is not protected but private
    protected Gson myGson;
    private BGPKeysProvider keysProvider;

    private EncodingMethod encodingMethod;
    private DecodingMethod decodingMethod;

    /**
     * Default ctor
     * @param gson the gson object
     * @param keysProvider the key provider
     */
    public BGPGsonConverter(Gson gson, BGPKeysProvider keysProvider, EncodingMethod encodingMethod, DecodingMethod decodingMethod) {
        super(gson, "UTF-8");
        this.myGson = gson;
        this.keysProvider = keysProvider;
        this.encodingMethod = encodingMethod;
        this.decodingMethod = decodingMethod;
    }

    /// Converts from body to object
    @Override
    public Object fromBody(final TypedInput body, Type type) throws ConversionException {
        String cryptedString = null;
        try {
            cryptedString = fromStream(body.in());

            //decrypts the message
            //TODO: edit the decrypter to user the server session key
            Decrypt decrypter = new Decrypt(keysProvider.getPrivateKey(), keysProvider.getServerSessionKey());
            decrypter.setCustomDecoding(decodingMethod);

            String uncryptedStr = decrypter.decrypt(cryptedString);

            //calls super to convert to object
            final InputStream is = new ByteArrayInputStream(uncryptedStr.getBytes());

            TypedInput myBody = new TypedInput() {
                @Override
                public String mimeType() {
                    return body.mimeType();
                }

                @Override
                public long length() {
                    return body.length();
                }

                @Override
                public InputStream in() throws IOException {
                    return is;
                }
            };
            return super.fromBody(myBody, type);

        } catch (Exception e) {
            throw new ConversionException(e);
        }

    }

    /// converts from object to body
    @Override
    public TypedOutput toBody(Object object) {

        String message = myGson.toJson(object);

        try {
            // use 128 bits/16bytes lenght key for session
            Encrypt encrypter = new Encrypt(keysProvider.getServerPublicKey());
            encrypter.setCustomEncoding(encodingMethod);
            String cryptedString = encrypter.encrypt(message);
            return super.toBody(cryptedString);
        } catch (Exception e) {
            e.printStackTrace();

        }
        return null;
    }

    public static String fromStream(InputStream in) throws IOException
    {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder out = new StringBuilder();
        String newLine = System.getProperty("line.separator");
        String line;
        while ((line = reader.readLine()) != null) {
            out.append(line);
            out.append(newLine);
        }
        return out.toString();
    }


}
