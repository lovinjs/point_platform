package com.core.coreboot.utils;

import com.core.coreboot.common.Constant;
import org.apache.tomcat.util.codec.binary.Base64;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5Utils {
    public static String getMD5(String str) throws NoSuchAlgorithmException {
         MessageDigest md5 = MessageDigest.getInstance("MD5");
         return Base64.encodeBase64String(md5.digest((str + Constant.SALT).getBytes()));
    }
}
