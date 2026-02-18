package com.emergencias.services;

import com.emergencias.model.EmergencyEvent;
import com.emergencias.model.UserData;

/**
 * <h1>Contrato para Sistemas de Alerta (Interfaz)</h1>
 *
 * <p>
 * Esta interfaz define el contrato que cualquier clase de envío de alertas
 * debe cumplir. Sigue el <b>Patrón de Diseño Strategy</b>, permitiendo que
 * diferentes implementaciones (estrategias) de envío de alertas sean
 * intercambiables.
 * </p>
 *
 * <p>
 * Por ejemplo, podríamos tener diferentes clases que implementen esta interfaz:
 * <ul>
 *     <li>{@code AlertSender}: La implementación actual que simula una llamada.</li>
 *     <li>{@code SMSAlertSender}: Una futura implementación que envíe alertas por SMS.</li>
 *     <li>{@code EmailAlertSender}: Otra que envíe alertas por correo electrónico.</li>
 * </ul>
 * El {@code EmergencyManager} puede trabajar con cualquiera de ellas sin cambiar
 * su código, simplemente recibiendo una implementación diferente.
 * </p>
 *
 * @author MirceaMihaiBontoi (Documentado por Davgaltol)
 * @version 1.1
 * @since 2023-10-27
 */
public interface IAlert {

    /**
     * Envía la alerta principal a los servicios de emergencia.
     *
     * @param event El evento de emergencia que contiene todos los detalles.
     * @return {@code true} si la alerta se envió con éxito, {@code false} en caso contrario.
     */
    boolean send(EmergencyEvent event);
    
    /**
     * Notifica a los contactos personales de emergencia del usuario.
     *
     * @param userData Los datos del usuario, que contienen la información del contacto.
     * @param event    El evento de emergencia con los detalles a notificar.
     */
    void notifyContacts(UserData userData, EmergencyEvent event);
    
    /**
     * Devuelve una descripción del tipo de sistema de alerta.
     * <p>
     * Es útil para obtener información sobre qué estrategia de alerta se está
     * utilizando en un momento dado.
     * </p>
     *
     * @return Una cadena que describe el tipo de alerta (ej. "Alerta por SMS").
     */
    String getAlertType();
}
