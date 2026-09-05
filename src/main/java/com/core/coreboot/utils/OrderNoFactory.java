package com.core.coreboot.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

public class OrderNoFactory {
    private static String getDateTime() {
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        return dateFormat.format(new Date());
    }

    private static int getRandomNum() {
        Random random = new Random();
        return (int) (random.nextDouble() * 90000) + 10000;
    }

    public static String getOrderNo() {
        return "NO" + getDateTime() + getRandomNum();
    }
}
