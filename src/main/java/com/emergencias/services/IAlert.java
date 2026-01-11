package com.emergencias.services;

import com.emergencias.model.EmergencyEvent;
import com.emergencias.model.UserData;

/**
 * Interfaz que define el contrato para enviar alertas de emergencia.
 * Implementa el patrón Strategy para permitir diferentes formas de notificación.
 * 
 * Ejemplos de implementación:
 * - CallAlert: Llamada telefónica
 * - SMSAlert: Mensaje de texto
 * - EmailAlert: Correo electrónico
 */
public interface IAlert {
    /**
     * Envía una alerta de emergencia.
     */
    boolean send(EmergencyEvent event);
    
    /**
     * Notifica a los contactos de emergencia del usuario.
     */
    void notifyContacts(UserData userData, EmergencyEvent event);
    
    /**
     * Obtiene el tipo de alerta.
     */
    String getAlertType();
}
