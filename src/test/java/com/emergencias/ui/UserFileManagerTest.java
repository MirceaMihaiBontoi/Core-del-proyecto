package com.emergencias.ui;

import com.emergencias.model.UserData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.*;

class UserFileManagerTest {

    private UserFileManager manager;
    private final String TEST_CACHE = "test_cache";

    @BeforeEach
    void setUp() throws Exception {
        // Asegurar que el directorio de test esté limpio
        cleanDir(new File(TEST_CACHE));
        manager = new UserFileManager(TEST_CACHE);
    }

    @AfterEach
    void tearDown() throws Exception {
        cleanDir(new File(TEST_CACHE));
    }

    private void cleanDir(File dir) throws Exception {
        if (dir.exists()) {
            Files.walk(dir.toPath())
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        }
    }

    @Test
    @DisplayName("El registro de usuario funciona y guarda en disco")
    void registerUserWorks() {
        boolean registered = manager.registerUser("paco", "1234", "Paco Jones", "111", "999", "Ninguna");
        
        assertTrue(registered);
        assertTrue(manager.userExists("paco"));
        
        // Verificar que el archivo se creó
        File f = new File(TEST_CACHE + "/users.dat");
        assertTrue(f.exists());
    }

    @Test
    @DisplayName("No se puede registrar dos veces el mismo usuario")
    void cannotRegisterDuplicate() {
        manager.registerUser("ana", "p", "Ana", "1", "2", "3");
        boolean secondTime = manager.registerUser("ana", "x", "Ana X", "0", "0", "0");
        
        assertFalse(secondTime);
    }

    @Test
    @DisplayName("El login funciona con credenciales correctas")
    void loginWorks() {
        manager.registerUser("ana", "pass", "Ana G", "123", "456", "Asma");
        
        UserData user = manager.loginUser("ana", "pass");
        assertNotNull(user);
        assertEquals("Ana G", user.getFullName());
        assertEquals("Asma", user.getMedicalInfo());
        
        // Intento fallido
        UserData fail = manager.loginUser("ana", "wrong");
        assertNull(fail);
    }
}
