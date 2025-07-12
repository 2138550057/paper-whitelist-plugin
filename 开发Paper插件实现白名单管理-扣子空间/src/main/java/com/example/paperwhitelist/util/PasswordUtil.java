package com.example.paperwhitelist.util;

import at.favre.lib.crypto.bcrypt.BCrypt;

public class PasswordUtil {
    private static final int COST_FACTOR = 10;

    public static String hashPassword(String password) {
        return BCrypt.withDefaults().hashToString(COST_FACTOR, password.toCharArray());
    }

    public static boolean verifyPassword(String password, String hashedPassword) {
        BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), hashedPassword);
        return result.verified;
    }
}