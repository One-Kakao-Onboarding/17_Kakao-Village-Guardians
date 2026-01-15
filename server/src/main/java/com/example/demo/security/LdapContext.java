package com.example.demo.security;

public class LdapContext {
    private static final ThreadLocal<String> ldapHolder = new ThreadLocal<>();

    public static void setLdap(String ldap) {
        ldapHolder.set(ldap);
    }

    public static String getLdap() {
        return ldapHolder.get();
    }

    public static void clear() {
        ldapHolder.remove();
    }
}
