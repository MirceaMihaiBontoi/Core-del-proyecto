package com.emergencias.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * <h1>Entidad de Centro de Salud</h1>
 *
 * <p>
 * Esta clase (POJO) representa la información de un único centro de salud,
 * mapeando los campos de un archivo JSON a atributos de la clase.
 * </p>
 *
 * <p>
 * Utiliza anotaciones de Jackson para manejar la correspondencia entre los
 * nombres de los campos en el JSON (que pueden contener caracteres especiales,
 * espacios o mayúsculas) y los nombres de las variables en Java.
 * </p>
 *
 * <ul>
 *   <li><b>@JsonIgnoreProperties(ignoreUnknown = true):</b> Le dice a Jackson
 *       que ignore cualquier campo en el JSON que no tenga un atributo
 *       correspondiente en esta clase. Esto hace que el mapeo sea más robusto
 *       frente a cambios en el JSON de origen.</li>
 *   <li><b>@JsonProperty("NombreDelCampoEnJSON"):</b> Vincula un atributo de la
 *       clase con un campo del JSON cuyo nombre es diferente.</li>
 * </ul>
 *
 * @author MirceaMihaiBontoi (Documentado por Davgaltol)
 * @version 1.1
 * @since 2023-10-27
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CentroSalud {

    // --- ATRIBUTOS MAPEADOS DESDE JSON ---

    @JsonProperty("Código")
    private String codigo;

    @JsonProperty("Nombre")
    private String nombre;

    @JsonProperty("Dirección")
    private String direccion;

    @JsonProperty("C.P.")
    private String cp;

    @JsonProperty("Municipio")
    private String municipio;

    @JsonProperty("Pedanía")
    private String pedania;

    @JsonProperty("Teléfono")
    private String telefono;

    @JsonProperty("Fax")
    private String fax;

    @JsonProperty("Email")
    private String email;

    @JsonProperty("URL Real")
    private String urlReal;

    @JsonProperty("URL Corta")
    private String urlCorta;

    @JsonProperty("Latitud")
    private String latitud;

    @JsonProperty("Longitud")
    private String longitud;

    @JsonProperty("Foto 1")
    private String foto1;

    // --- GETTERS ---
    // Se exponen solo los getters para los campos que son útiles en la aplicación.
    // Jackson utiliza estos métodos (o los campos directamente) para la serialización.
    // No se necesitan setters si la clase solo se usa para leer datos.

    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public String getDireccion() { return direccion; }
    public String getCp() { return cp; }
    public String getMunicipio() { return municipio; }
    public String getPedania() { return pedania; }
    public String getTelefono() { return telefono; }
    public String getFax() { return fax; }
    public String getEmail() { return email; }
    public String getUrlReal() { return urlReal; }
    public String getUrlCorta() { return urlCorta; }
    public String getLatitud() { return latitud; }
    public String getLongitud() { return longitud; }
    public String getFoto1() { return foto1; }

    /**
     * Devuelve una representación simple del centro de salud, útil para listas.
     *
     * @return Una cadena con el nombre, municipio y teléfono.
     */
    @Override
    public String toString() {
        return String.format("%s (Municipio: %s, Tel: %s)", nombre, municipio, telefono);
    }
}
