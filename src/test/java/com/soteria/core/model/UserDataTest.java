package com.soteria.core.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserDataTest {

    @Test
    @DisplayName("Record constructor assigns all components correctly")
    void parameterisedConstructorAssignsFields() {
        UserData u = new UserData("Ana", "600111222", "Asma", "Luis 600333444");
        assertEquals("Ana", u.fullName());
        assertEquals("600111222", u.phoneNumber());
        assertEquals("Asma", u.medicalInfo());
        assertEquals("Luis 600333444", u.emergencyContact());
    }

    @Test
    @DisplayName("Record toString include components")
    void toStringIncludesKeyFields() {
        UserData u = new UserData("Ana", "600111222", "Asma", "Luis 600333444");
        String out = u.toString();
        assertTrue(out.contains("Ana"));
        assertTrue(out.contains("600111222"));
        assertTrue(out.contains("Asma"));
        assertTrue(out.contains("Luis 600333444"));
    }
    
    @Test
    @DisplayName("Record equals and hashCode work correctly")
    void recordsEquality() {
        UserData u1 = new UserData("Ana", "123", "None", "Mom");
        UserData u2 = new UserData("Ana", "123", "None", "Mom");
        assertEquals(u1, u2);
        assertEquals(u1.hashCode(), u2.hashCode());
    }
}
