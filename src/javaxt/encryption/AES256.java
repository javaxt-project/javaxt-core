package javaxt.encryption;
import java.security.AlgorithmParameters;
import javax.crypto.*;
import java.security.spec.*;
import javax.crypto.spec.*;

//******************************************************************************
//**  AES-256
//******************************************************************************
/**
 *   Provides static methods used to encrypt and decrypt text using AES 256 bit
 *   encryption. Based on code posted here:
 *   http://stackoverflow.com/a/992413/
 *
 ******************************************************************************/

public class AES256 {

    private static String err = "Please install the Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy Files.";

    private AES256(){}

  //**************************************************************************
  //** encrypt
  //**************************************************************************
  /** Used to encrypt a block of text. Throws an java.security.InvalidKeyException
   *  if the JRE/JDK is missing the Java Cryptography Extension (JCE) unlimited
   *  strength jurisdiction policy files.
   */
    public static byte[] encrypt(String text, String password) throws Exception {
        try{
            java.security.SecureRandom rand = new java.security.SecureRandom();
            byte[] salt = new byte[32];
            rand.nextBytes(salt);

            SecretKey secret = getSecretKey(password, salt);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secret);
            AlgorithmParameters params = cipher.getParameters();
            byte[] iv = params.getParameterSpec(IvParameterSpec.class).getIV();
            byte[] b = cipher.doFinal(text.getBytes("UTF-8"));

            byte[] ret = new byte[salt.length + iv.length + b.length];
            System.arraycopy(salt, 0, ret, 0, salt.length);
            System.arraycopy(iv, 0, ret, salt.length, iv.length);
            System.arraycopy(b, 0, ret, salt.length+iv.length, b.length);
            return ret;
        }
        catch(java.security.InvalidKeyException e){
            if (e.getMessage().equals("Illegal key size")){
                throw new java.security.InvalidKeyException(err);
            }
            else{
                throw e;
            }
        }
    }


  //**************************************************************************
  //** decrypt
  //**************************************************************************
  /** Used to decrypt a byte array generated from the encrypt() method. Throws
   *  an java.security.InvalidKeyException if the JRE/JDK is missing the Java
   *  Cryptography Extension (JCE) unlimited strength jurisdiction policy files.
   */
    public static String decrypt(byte[] bytes, String password) throws Exception {
        try{
            byte[] salt = new byte[32];
            byte[] iv = new byte[16];
            byte[] b = new byte[bytes.length-salt.length-iv.length];

            System.arraycopy(bytes, 0, salt, 0, salt.length);
            System.arraycopy(bytes, 32, iv, 0, iv.length);
            System.arraycopy(bytes, 32+16, b, 0, b.length);

            SecretKey secret = getSecretKey(password, salt);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(iv));
            return new String(cipher.doFinal(b), "UTF-8");

        }
        catch(java.security.InvalidKeyException e){
            if (e.getMessage().equals("Illegal key size")){
                throw new java.security.InvalidKeyException(err);
            }
            else{
                throw e;
            }
        }
    }


  //**************************************************************************
  //** getSecretKey
  //**************************************************************************
    private static SecretKey getSecretKey(String password, byte[] salt) throws
        java.security.NoSuchAlgorithmException, java.security.spec.InvalidKeySpecException, java.io.UnsupportedEncodingException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 256);
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), "AES");
    }
}