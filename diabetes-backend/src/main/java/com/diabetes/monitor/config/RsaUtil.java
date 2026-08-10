package com.diabetes.monitor.config;


import org.springframework.stereotype.Component;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;

/**
 * RSA加密工具类
 * 功能：生成密钥对、获取公钥、私钥解密
 */
@Component
public class RsaUtil {

    // RSA算法名称
    private static final String ALGORITHM = "RSA";
    // 密钥长度：2048位
    private static final int KEY_SIZE = 2048;
    // 私钥对象
    private PrivateKey privateKey;
    // 公钥对象
    private PublicKey publicKey;

    /**
     * 构造方法：初始化时自动生成RSA密钥对
     */
    public RsaUtil() {
        try {
            //1.创建密钥对生成器
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM);

            //2.初始化密钥长度
            keyPairGenerator.initialize(KEY_SIZE);

            //3.生成密钥对
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            //4.获取私钥和公钥
            privateKey = keyPair.getPrivate();
            publicKey = keyPair.getPublic();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("RSA密钥对生成失败", e);
        }
    }

    /**
     * 获取Base64编码的公钥字符串
     * @return Base64公钥
     */
    public String getPublicKey() {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    /**
     * 使用私钥解密数据
     * @param encrypted Base64编码的加密数据
     * @return 解密后的明文字符串
     */
    public String decrypt(String encrypted) {
        try {
            // 1. 创建Cipher实例
            Cipher cipher = Cipher.getInstance(ALGORITHM);

            //2.初始化为解密模式，传入私钥
            cipher.init(Cipher.DECRYPT_MODE,privateKey);

            //3.将Base64字符串解码为字节数组
            byte[] encryptedBytes = Base64.getDecoder().decode(encrypted);

            //执行解密
            byte[] bytes = cipher.doFinal(encryptedBytes);

           // 5. 转换为字符串
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (NoSuchPaddingException | NoSuchAlgorithmException e) {
            throw new RuntimeException("解密失败", e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException("初始化为解密模式",e);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new RuntimeException("执行解密失败",e);
        }
    }

    /**
     * 加密方法（配套使用）
     * @param plainText 明文字符串
     * @return Base64编码的加密数据
     */
    public String encrypt(String plainText) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);

            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("加密失败", e);
        }
    }

}
