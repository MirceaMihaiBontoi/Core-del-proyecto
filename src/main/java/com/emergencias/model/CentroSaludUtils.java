package com.emergencias.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

/**
 * <h1>Utilidad para Cargar Centros de Salud</h1>
 *
 * <p>
 * Esta clase de utilidad proporciona un método estático para cargar una lista
 * de objetos {@link CentroSalud} desde un archivo JSON ubicado en los recursos
 * del proyecto (classpath).
 * </p>
 *
 * <p>
 * Abstrae la complejidad de la lectura de archivos y el parseo de JSON,
 * ofreciendo una forma simple y segura de obtener los datos.
 * </p>
 *
 * @author MirceaMihaiBontoi (Documentado por Davgaltol)
 * @version 1.1
 * @since 2023-10-27
 */
public class CentroSaludUtils {

    /**
     * Carga una lista de centros de salud desde un archivo JSON en el classpath.
     * <p>
     * El método utiliza {@code getResourceAsStream} para encontrar y leer el
     * archivo, lo que garantiza que funcione tanto en el entorno de desarrollo (IDE)
     * como en un archivo JAR distribuible.
     * </p>
     *
     * @param rutaArchivo La ruta al archivo JSON, relativa a la raíz del classpath.
     *                    Por ejemplo, {@code "/CentrosdeSaludMurcia.json"}.
     * @return Una lista de objetos {@link CentroSalud}. Si ocurre un error o el
     *         archivo no se encuentra, devuelve una lista vacía para evitar
     *         punteros nulos (NullPointerException).
     */
    public static List<CentroSalud> cargarCentros(String rutaArchivo) {
        try (InputStream inputStream = CentroSaludUtils.class.getResourceAsStream(rutaArchivo)) {
            // Se usa un bloque try-with-resources para asegurar que el inputStream se cierre automáticamente.

            if (inputStream == null) {
                System.err.println("❌ Error: No se pudo encontrar el archivo de recursos: " + rutaArchivo);
                return Collections.emptyList(); // Devuelve lista vacía en lugar de null.
            }

            // Se crea una instancia de ObjectMapper para realizar el mapeo.
            ObjectMapper mapper = new ObjectMapper();
            
            // Se lee el JSON y se convierte a una lista de CentroSalud.
            // TypeReference es necesario para que Jackson sepa que el JSON es una lista de objetos.
            return mapper.readValue(inputStream, new TypeReference<List<CentroSalud>>() {});
            
        } catch (Exception e) {
            System.err.println("❌ Error crítico al leer o procesar el archivo JSON de centros de salud: " + e.getMessage());
            e.printStackTrace(); // Imprime la traza para depuración.
            return Collections.emptyList(); // Devuelve lista vacía en caso de excepción.
        }
    }
}
