package pt.ulisboa.tecnico.hdsledger.utilities;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Cipher;
import java.io.FileInputStream;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.InvalidKeyException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;

public class RSAEncryption {

    private static byte[] readFile(String path) throws FileNotFoundException, IOException {

        FileInputStream fs = new FileInputStream(path);
        byte[] data = new byte[fs.available()];
        fs.read(data);
        fs.close();

        return data;
    }

    public static PublicKey readPublicKey(String publicKeyPath) 
        throws FileNotFoundException, IOException, NoSuchAlgorithmException, InvalidKeySpecException{

        byte[] pubEncoded = readFile(publicKeyPath);
        X509EncodedKeySpec pubSpec = new X509EncodedKeySpec(pubEncoded);
        KeyFactory keyFacPub = KeyFactory.getInstance("RSA");
        PublicKey pub = keyFacPub.generatePublic(pubSpec);

        return pub;
    }

    public static PrivateKey readPrivateKey(String privateKeyPath) 
        throws FileNotFoundException, IOException, NoSuchAlgorithmException, InvalidKeySpecException{

        byte[] privEncoded = readFile(privateKeyPath);
        PKCS8EncodedKeySpec privSpec = new PKCS8EncodedKeySpec(privEncoded);
        KeyFactory keyFacPriv = KeyFactory.getInstance("RSA");
        PrivateKey priv = keyFacPriv.generatePrivate(privSpec);

        return priv;
    }

    public static byte[] encrypt(byte[] data, String privateKeyPath) 
        throws FileNotFoundException, IOException, NoSuchAlgorithmException, InvalidKeySpecException,
        NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException{

        PrivateKey privateKey = readPrivateKey(privateKeyPath);
        Cipher encryptCipher = Cipher.getInstance("RSA");
        encryptCipher.init(Cipher.ENCRYPT_MODE, privateKey);
        byte[] encryptedData = encryptCipher.doFinal(data);

        return encryptedData;
    }

    public static byte[] decrypt(byte[] data, String publicKeyPath) 
        throws FileNotFoundException, IOException, NoSuchAlgorithmException, InvalidKeySpecException,
        NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException{

        PublicKey publicKey = readPublicKey(publicKeyPath);
        Cipher decryptCipher = Cipher.getInstance("RSA");
        decryptCipher.init(Cipher.DECRYPT_MODE, publicKey);
        byte[] decryptedData = decryptCipher.doFinal(data);

        return decryptedData;
    }

    public static String digest(String data) throws NoSuchAlgorithmException{
        byte[] dataBytes = data.getBytes();
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        messageDigest.update(dataBytes);
        byte[] digestBytes = messageDigest.digest();

        return Base64.getEncoder().encodeToString(digestBytes);
    }

    public static String sign(String data, String privateKeyPath) throws FileNotFoundException,
        IOException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException,
        InvalidKeyException, IllegalBlockSizeException, BadPaddingException{

        String digest = digest(data);
        byte[] digestEncrypted = encrypt(digest.getBytes(), privateKeyPath);
        String digestBase64 = Base64.getEncoder().encodeToString(digestEncrypted);

        return digestBase64;
    }

    public static boolean verifySignature(String data, String signature, String publicKeyPath) {
        try {
            String hashedData = digest(data);
            byte[] signatureBytes = Base64.getDecoder().decode(signature);
            String decryptedHash = new String(decrypt(signatureBytes, publicKeyPath));
            return hashedData.equals(decryptedHash);

        } catch (Exception e) {
            return false;
        }
    }
}