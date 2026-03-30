/**
 * La anotación @JsonProperty permite mapear los nombres de los campos del JSON
 * (que pueden tener tildes, espacios, mayúsculas, etc.) a atributos Java válidos.
 * Así, aunque el JSON tenga "Código" o "Dirección", se pueden usar nombres de variables
 * como codigo o direccion en Java, y Jackson hará la conversión automáticamente.
 *
 * Ejemplo:
 *   @JsonProperty("Código")
 *   private String codigo;
 * Esto indica a Jackson que el campo "Código" del JSON se debe guardar en la variable 'codigo'.
 */
package com.emergencias.model;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CentroSalud {
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

    // Getters útiles para el filtro
    public String getMunicipio() { return municipio; }
    public String getNombre() { return nombre; }
    public String getDireccion() { return direccion; }
    public String getTelefono() { return telefono; }
    public String getLatitud() { return latitud; }
    public String getLongitud() { return longitud; }
}
