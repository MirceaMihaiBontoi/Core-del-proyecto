# 🚨 Sistema de Gestión de Emergencias Inteligente

## 📋 Descripción del Proyecto

**Sistema de Gestión de Emergencias** es una aplicación Java que proporciona un sistema completo para detectar, procesar y registrar emergencias. El sistema guía al usuario a través de un flujo estructurado de detección de emergencias, envío de alertas a servicios de emergencia (112) y recopilación de feedback posterior.

Diseñado como un prototipo educativo, el proyecto demuestra principios sólidos de **Programación Orientada a Objetos (POO)**, incluyendo:
- ✅ **Interfaces** para abstracciones
- ✅ **Herencia** con clases abstractas
- ✅ **Polimorfismo** mediante implementaciones
- ✅ **Control de errores** robusto
- ✅ **Validaciones** exhaustivas

Autor: **Mircea Mihai Bontoi**
---

## 🎯 Características Principales

### 1. **Detección de Emergencias**
El usuario puede reportar diferentes tipos de emergencias:
- Accidente de tráfico
- Problema médico
- Incendio
- Agresión
- Otro

Cada tipo de emergencia tiene su propio nivel de prioridad y protocolo de respuesta.

### 2. **Registro y Logging**
Todas las emergencias se registran automáticamente en archivos de log:
- `emergency_history.log` - Historial de emergencias
- `emergency_alerts.log` - Alertas enviadas
- `user_feedback.log` - Feedback de usuarios

### 3. **Feedback del Usuario**
Después de reportar una emergencia, el sistema solicita evaluación:
- Puntuación de 1-5 estrellas
- Comentarios adicionales
- Registro de mejora continua

### 4. **Sistema de Alertas Flexible**
Implementación de múltiples estrategias de alerta mediante interfaces:
- **CallAlert**: Simulación de llamada telefónica al 112
- Extensible para SMS, Email, etc.

### 5. **Control de Errores Integral**
- Validación de entrada del usuario
- Manejo de excepciones en todas las operaciones
- Reintentos automáticos en campos requeridos
- Mensajes de error claros y descriptivos

---

## 🏗️ Arquitectura y Diseño POO

### Diagrama de Clases (Simplificado)

```
┌─────────────────────────────────────────────────────────┐
│                      Main                               │
│          (Punto de entrada de la aplicación)            │
└──────────────────────┬──────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────┐
│                 EmergencyManager                         │
│        (Coordinador principal del sistema)              │
├─────────────────────────────────────────────────────────┤
│ - EmergencyDetector (detecta emergencias)               │
│ - IAlert alertSender (envía alertas - POLIMORFISMO)    │
│ - EmergencyLogger (registra eventos)                    │
│ - UserData (datos del usuario)                          │
│ - Scanner (entrada compartida)                          │
└─────────────────────────────────────────────────────────┘

INTERFACES (Contratos):
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│     IAlert       │  │ ILocationService │  │     ILogger      │
├──────────────────┤  ├──────────────────┤  ├──────────────────┤
│ send()           │  │ getCoordinates() │  │ logInfo()        │
│ notifyContacts() │  │ getLocation()    │  │ logWarning()     │
│ getAlertType()   │  │ getPermission()  │  │ logError()       │
└──────────────────┘  └──────────────────┘  └──────────────────┘
       ▲                      ▲                      ▲
       │ implementa           │ implementa           │ implementa
       │                      │                      │
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│  AlertSender     │  │ GPSLocationService│ │ EmergencyLogger  │
│  CallAlert       │  │                  │  │                  │
└──────────────────┘  └──────────────────┘  └──────────────────┘

HERENCIA (Extensibilidad):
┌──────────────────────────────┐
│  EmergencyType (ABSTRACTA)   │
│  ├─ getPriority()            │
│  ├─ getDescription()         │
│  ├─ getResponseProtocol()    │ (método abstracto)
│  └─ getRequiredServices()    │ (método abstracto)
└──────────┬───────────────────┘
           │
           ▼
┌──────────────────────────────┐
│  MedicalEmergency            │
│  (Implementa métodos)        │
└──────────────────────────────┘
```

### Conceptos POO Implementados

#### 1. **Polimorfismo** 🔄
```java
private final IAlert alertSender;  // Interfaz genérica
this.alertSender = new AlertSender();  // Implementación específica

// En runtime, puede ser cualquier clase que implemente IAlert:
// - AlertSender
// - CallAlert
// - SMSAlert (futura)
// - EmailAlert (futura)
```

#### 2. **Herencia** 👨‍👧
```java
public abstract class EmergencyType {
    abstract String getResponseProtocol();
    abstract String[] getRequiredServices();
}

public class MedicalEmergency extends EmergencyType {
    // Implementación específica para emergencias médicas
}
```

#### 3. **Interfaces** 📋
```java
public interface IAlert {
    boolean send(EmergencyEvent event);
    void notifyContacts(UserData userData, EmergencyEvent event);
    String getAlertType();
}
```

---

## 📁 Estructura del Proyecto

```
src/main/java/com/emergencias/
│
├── Main.java                          # Punto de entrada
│
├── model/                             # Modelos de datos
│   ├── UserData.java                 # Información del usuario
│   ├── EmergencyEvent.java           # Evento de emergencia
│   ├── UserFeedback.java             # Feedback del usuario
│   ├── EmergencyType.java            # Clase abstracta para tipos
│   └── MedicalEmergency.java         # Implementación: emergencia médica
│
├── detector/                          # Detección de emergencias
│   └── EmergencyDetector.java        # Detecta y recopila datos
│
├── controller/                        # Controladores
│   └── EmergencyManager.java         # Coordinador principal
│
├── alert/                             # Sistema de alertas
│   ├── AlertSender.java              # Implementa IAlert
│   ├── CallAlert.java                # Alternativa: llamadas
│   └── EmergencyLogger.java          # Registro de eventos
│
└── services/                          # Servicios e interfaces
    ├── IAlert.java                   # Interfaz de alertas
    ├── ILocationService.java         # Interfaz de ubicación
    ├── ILogger.java                  # Interfaz de logging
    └── GPSLocationService.java       # Implementación GPS
```

---

## 🔄 Flujo de Ejecución

### 1. **Inicialización**
```
Main.java
  ├─ Crear Scanner compartido
  ├─ Crear UserData con datos de ejemplo
  ├─ Crear EmergencyManager
  └─ Llamar startSystem()
```

### 2. **Recopilación de Datos del Usuario**
```
UserData.collectUserData()
  ├─ Nombre (validado - no vacío)
  ├─ Teléfono (validado - no vacío)
  ├─ Información médica (opcional)
  └─ Contacto de emergencia (validado - no vacío)
```

### 3. **Detección de Emergencia**
```
EmergencyDetector.detectEmergency()
  ├─ ¿Hay emergencia? (S/N)
  ├─ Si SÍ:
  │   ├─ Tipo de emergencia (1-5, reintentos)
  │   ├─ Ubicación (o usar GPS)
  │   ├─ Nivel de gravedad (1-10, validado)
  │   └─ Confirmación final
  └─ Si NO: cancelar
```

### 4. **Procesamiento de Emergencia**
```
EmergencyManager.startSystem()
  ├─ LogEmergency() → genera UUID
  ├─ AlertSender.send() → envía alerta
  ├─ AlertSender.notifyContacts() → notifica contactos
  ├─ EmergencyLogger.collectAndLogFeedback() → solicita puntuación
  └─ Registrar feedback en archivo
```

### 5. **Registros Generados**
```
emergency_history.log:
[2026-01-11 14:30:45] ID: a1b2c3d4 | Tipo: Problema médico | Ubicación: Plaza Mayor, Madrid | Gravedad: 8

emergency_alerts.log:
[2026-01-11 14:30:45] ALERTA DE EMERGENCIA
Tipo: Problema médico
Ubicación: Plaza Mayor, Madrid
Nivel de gravedad: 8/10
...

user_feedback.log:
[2026-01-11 14:31:15] ID Emergencia: a1b2c3d4 | Puntuación: 5/5 | Comentarios: Excelente servicio
```

---

## 🛡️ Control de Errores

El sistema implementa manejo de errores multinivel:

### Nivel 1: Validación de Entrada
```java
// En UserData.collectUserData()
while (true) {
    String input = scanner.nextLine().trim();
    if (!input.isEmpty()) {
        this.fullName = input;
        break;
    } else {
        System.out.println("⚠️  Error: El nombre no puede estar vacío");
    }
}
```

### Nivel 2: Try-Catch en Operaciones Críticas
```java
try {
    String emergencyId = logger.logEmergency(event);
    boolean alertSent = alertSender.send(event);
    // ...
} catch (Exception e) {
    System.err.println("❌ Error al procesar la emergencia: " + e.getMessage());
}
```

### Nivel 3: Captura Global en Main
```java
try {
    emergencyManager.startSystem();
} catch (Exception e) {
    System.err.println("\n=== ERROR CRÍTICO ===");
    e.printStackTrace();
} finally {
    scanner.close();
}
```

---


## 🤝 Contribución y Mejoras Futuras

Posibles mejoras para versiones futuras:

- [ ] Integración con API de Google Maps para GPS real
- [ ] Base de datos en lugar de archivos de texto
- [ ] Interfaz gráfica (GUI) con JavaFX o Swing
- [ ] Envío real de SMS/Email
- [ ] Integración con servicios de emergencia reales
- [ ] Análisis de estadísticas de emergencias
- [ ] Sistema de autenticación de usuarios
- [ ] Aplicación móvil (Android/iOS)


