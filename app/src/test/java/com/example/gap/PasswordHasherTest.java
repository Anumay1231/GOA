package com.example.gap;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PasswordHasherTest {

    @Test
    public void hash_verifiesCorrectPassword() {
        String stored = PasswordHasher.hash("secret123");
        assertTrue(PasswordHasher.isHashed(stored));
        assertTrue(PasswordHasher.verify("secret123", stored));
    }

    @Test
    public void hash_rejectsWrongPassword() {
        String stored = PasswordHasher.hash("secret123");
        assertFalse(PasswordHasher.verify("secret124", stored));
        assertFalse(PasswordHasher.verify("", stored));
    }

    @Test
    public void hash_isSaltedSoSamePasswordGivesDifferentHashes() {
        assertNotEquals(PasswordHasher.hash("same"), PasswordHasher.hash("same"));
    }

    @Test
    public void verify_rejectsPlainTextOrMalformedValues() {
        assertFalse(PasswordHasher.verify("password", "password"));
        assertFalse(PasswordHasher.verify("password", null));
        assertFalse(PasswordHasher.verify("password", "pbkdf2$abc"));
        assertFalse(PasswordHasher.verify("password", "pbkdf2$x$%%%$%%%"));
    }
}
