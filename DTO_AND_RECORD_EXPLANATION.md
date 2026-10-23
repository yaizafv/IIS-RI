# Explicación: Campos en DTOs y Records - De dónde vienen

## El Flujo de Datos: Base de Datos → Código Java

Cuando un cliente se registra en el sistema, sus datos viajan a través de varias capas. Voy a explicar de dónde vienen **exactamente** cada uno de los campos.

---

## 1. Punto de Partida: La Base de Datos

La tabla `TCLIENTS` en la BD tiene estos campos:

```sql
CREATE TABLE TCLIENTS (
    id VARCHAR(255) PRIMARY KEY,              -- UUID único
    version BIGINT,                           -- Control de concurrencia
    createdAt TIMESTAMP,                      -- Cuándo se creó
    updatedAt TIMESTAMP,                      -- Cuándo se actualizó
    entityState VARCHAR(255),                 -- ENABLED, DISABLED, etc
    
    -- Datos del cliente
    nif VARCHAR(255) NOT NULL UNIQUE,         -- Número de identificación fiscal
    name VARCHAR(255),                        -- Nombre
    surname VARCHAR(255),                     -- Apellidos
    phone VARCHAR(255),                       -- Teléfono
    email VARCHAR(255),                       -- Correo
    
    -- Domicilio
    street VARCHAR(255),                      -- Calle
    city VARCHAR(255),                        -- Ciudad
    country VARCHAR(255),                     -- País
    postalCode VARCHAR(255)                   -- Código postal
);
```

**Conclusión:** Todos los campos del DTO vienen de la BD (la tabla TCLIENTS).

---

## 2. Capa 1: Record (Persistencia)

El **ClientRecord** es una copia exacta de lo que hay en la BD:

```java
// uo/ri/cws/application/persistence/client/ClientGateway.java

public interface ClientGateway extends Gateway<ClientGateway.ClientRecord> {
    
    class ClientRecord extends Gateway.BaseRecord {
        // ← Hereda: id, version, createdAt, updatedAt, entityState
        
        public String nif;              // ← De la BD: nif
        public String name;             // ← De la BD: name
        public String surname;          // ← De la BD: surname
        public String street;           // ← De la BD: street (nota: se llama street, no addressStreet)
        public String city;             // ← De la BD: city
        public String country;          // ← De la BD: country
        public String postalCode;       // ← De la BD: postalCode
        public String phone;            // ← De la BD: phone
        public String email;            // ← De la BD: email
    }
}
```

**¿De dónde viene BaseRecord?**

```java
// uo/ri/cws/application/persistence/Gateway.java
public interface Gateway<T extends BaseRecord> {
    
    abstract class BaseRecord {
        public String id;                   // ← UUID único
        public long version;                // ← Control de versión optimista
        public LocalDateTime createdAt;     // ← Timestamp de creación
        public LocalDateTime updatedAt;     // ← Timestamp de última actualización
        public String entityState;          // ← Estado (ENABLED/DISABLED)
    }
}
```

**Mapeo Record → BD:**

| Record | BD |
|---|---|
| `id` | `id` (UUID) |
| `version` | `version` |
| `createdAt` | `createdAt` |
| `updatedAt` | `updatedAt` |
| `entityState` | `entityState` |
| `nif` | `nif` |
| `name` | `name` |
| `surname` | `surname` |
| `street` | `street` |
| `city` | `city` |
| `country` | `country` |
| `postalCode` | `postalCode` |
| `phone` | `phone` |
| `email` | `email` |

---

## 3. Capa 2: DTO (Service)

El **ClientDto** es lo que se pasa por la UI. Está definido en la **interfaz de servicio**:

```java
// uo/ri/cws/application/service/client/ClientCrudService.java
// (En el paquete cws_service_interfaces, NO en cws_services_jdbc)

public interface ClientCrudService {
    
    public static class ClientDto {
        public String id;                   // ← Del Record (o null si es nuevo)
        public long version;                // ← Del Record
        
        public String nif;                  // ← Del Record
        public String name;                 // ← Del Record
        public String surname;              // ← Del Record
        public String addressStreet;        // ← Del Record.street (¡DIFERENTE NOMBRE!)
        public String addressCity;          // ← Del Record.city   (¡DIFERENTE NOMBRE!)
        public String addressCountry;       // ← Del Record.country (¡DIFERENTE NOMBRE!)
        public String addressZipcode;       // ← Del Record.postalCode (¡DIFERENTE NOMBRE!)
        public String phone;                // ← Del Record
        public String email;                // ← Del Record
    }
}
```

**¿Por qué los nombres son diferentes?**

| DTO | Record | BD | Razón |
|---|---|---|---|
| `addressStreet` | `street` | `street` | DTO agrupa con prefijo "address" |
| `addressCity` | `city` | `city` | DTO agrupa con prefijo "address" |
| `addressCountry` | `country` | `country` | DTO agrupa con prefijo "address" |
| `addressZipcode` | `postalCode` | `postalCode` | Nombres más semánticos |

**Conclusión:** El DTO es una **versión con nombres más amigables** del Record, para la UI.

---

## 4. El Assembler: Traductor entre Record ↔ DTO

El **Assembler** convierte un lado al otro. Es el único lugar donde saben cómo mapear:

```java
// uo/ri/cws/application/service/client/crud/ClientDtoAssembler.java

public final class ClientDtoAssembler {

    // DTO → Record (cuando se guarda)
    public static ClientRecord toRecord(ClientDto dto) {
        ClientRecord record = new ClientRecord();
        
        // Copiar campos idénticos
        record.id = dto.id == null ? UUID.randomUUID().toString() : dto.id;
        record.version = dto.version == 0 ? 1 : dto.version;
        record.nif = dto.nif;
        record.name = dto.name;
        record.surname = dto.surname;
        record.phone = dto.phone;
        record.email = dto.email;
        
        // Mapear campos con nombres diferentes
        record.street = dto.addressStreet;        // ← addressStreet → street
        record.city = dto.addressCity;            // ← addressCity → city
        record.country = dto.addressCountry;      // ← addressCountry → country
        record.postalCode = dto.addressZipcode;   // ← addressZipcode → postalCode
        
        // Crear campos especiales
        record.entityState = "ENABLED";           // ← Siempre ENABLED en nuevos
        record.createdAt = LocalDateTime.now();
        record.updatedAt = record.createdAt;
        
        return record;
    }

    // Record → DTO (cuando se lee)
    public static ClientDto toDto(ClientRecord record) {
        ClientDto dto = new ClientDto();
        
        // Copiar campos idénticos
        dto.id = record.id;
        dto.version = record.version;
        dto.nif = record.nif;
        dto.name = record.name;
        dto.surname = record.surname;
        dto.phone = record.phone;
        dto.email = record.email;
        
        // Mapear campos con nombres diferentes
        dto.addressStreet = record.street;        // ← street → addressStreet
        dto.addressCity = record.city;            // ← city → addressCity
        dto.addressCountry = record.country;      // ← country → addressCountry
        dto.addressZipcode = record.postalCode;   // ← postalCode → addressZipcode
        
        // NO copiar entityState, createdAt, updatedAt (no son para UI)
        
        return dto;
    }
}
```

---

## 5. Flujo Completo: Ejemplo - Agregar un Cliente

```
PASO 1: Usuario en UI ingresa datos
┌─────────────────────────────────────────┐
│ Cliente ingresa en ClientMenu:          │
│ - Nombre: "Juan García"                 │
│ - NIF: "12345678A"                      │
│ - Ciudad: "Madrid"                      │
│ - Calle: "Calle Principal, 10"          │
│ - etc.                                  │
└─────────────────────────────────────────┘
                    ↓

PASO 2: Se crea ClientDto con esos datos
┌─────────────────────────────────────────┐
│ ClientDto clientDto = new ClientDto();  │
│ clientDto.name = "Juan García";         │
│ clientDto.nif = "12345678A";            │
│ clientDto.addressCity = "Madrid";       │
│ clientDto.addressStreet = "Calle...";   │
│ clientDto.id = null;  (se genera)       │
│ clientDto.version = 0; (se calcula)     │
└─────────────────────────────────────────┘
                    ↓

PASO 3: Se llama AddClient (Command)
┌─────────────────────────────────────────┐
│ new AddClient(clientDto, null)           │
│    .execute()                           │
└─────────────────────────────────────────┘
                    ↓

PASO 4: AddClient convierte DTO → Record
┌─────────────────────────────────────────┐
│ ClientRecord record =                   │
│   ClientDtoAssembler.toRecord(clientDto)│
│                                         │
│ record.id = UUID.randomUUID() ← nuevo   │
│ record.version = 1                      │
│ record.name = "Juan García"             │
│ record.nif = "12345678A"                │
│ record.street = "Calle..." (de address  │
│                              Street)    │
│ record.city = "Madrid" (de addressCity) │
│ record.createdAt = LocalDateTime.now()  │
│ record.updatedAt = LocalDateTime.now()  │
│ record.entityState = "ENABLED"          │
└─────────────────────────────────────────┘
                    ↓

PASO 5: ClientGateway.add(record)
┌─────────────────────────────────────────┐
│ Se ejecuta SQL INSERT:                  │
│                                         │
│ INSERT INTO TCLIENTS (                  │
│   id, version, createdAt, updatedAt,    │
│   entityState, nif, name, surname,      │
│   street, city, country, postalCode,    │
│   phone, email                          │
│ ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
│                                         │
│ Valores: record.id, record.version,     │
│ record.createdAt, record.updatedAt,     │
│ record.entityState, record.nif,         │
│ record.name, record.surname,            │
│ record.street, record.city, ...         │
└─────────────────────────────────────────┘
                    ↓

PASO 6: Se lee de BD y se convierte Record → DTO
┌─────────────────────────────────────────┐
│ ClientRecord recordInserted =           │
│   clientGateway.add(record);            │
│                                         │
│ ClientDto resultDto =                   │
│   ClientDtoAssembler.toDto(             │
│      recordInserted                     │
│   );                                    │
│                                         │
│ resultDto.id = "550e8400-e29b..." ← BD  │
│ resultDto.name = "Juan García"          │
│ resultDto.addressCity = "Madrid"        │
└─────────────────────────────────────────┘
                    ↓

PASO 7: Se devuelve DTO al UI
┌─────────────────────────────────────────┐
│ UI muestra:                             │
│ "Cliente agregado correctamente"        │
│ "ID: 550e8400-e29b..."                  │
└─────────────────────────────────────────┘
```

---

## 6. Diagrama General: De dónde vienen los campos

```
┌─────────────────────────────────────────────────────────────┐
│ BASE DE DATOS (TCLIENTS)                                    │
│                                                             │
│ id | version | createdAt | updatedAt | entityState |       │
│ nif | name | surname | street | city | country |           │
│ postalCode | phone | email                                  │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ↓ (JDBC)
┌─────────────────────────────────────────────────────────────┐
│ RECORD (Capa Persistence)                                   │
│                                                             │
│ Hereda de BaseRecord:                                       │
│ - id, version, createdAt, updatedAt, entityState           │
│                                                             │
│ Propiedades propias:                                        │
│ - nif, name, surname                                        │
│ - street ← street (igual)                                   │
│ - city ← city (igual)                                       │
│ - country ← country (igual)                                 │
│ - postalCode ← postalCode (igual)                           │
│ - phone, email                                              │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ↓ (Assembler)
┌─────────────────────────────────────────────────────────────┐
│ DTO (Capa Service)                                          │
│                                                             │
│ - id (del Record)                                           │
│ - version (del Record)                                      │
│ - nif, name, surname (del Record)                           │
│ - addressStreet ← street (cambio nombre)                    │
│ - addressCity ← city (cambio nombre)                        │
│ - addressCountry ← country (cambio nombre)                  │
│ - addressZipcode ← postalCode (cambio nombre)               │
│ - phone, email (del Record)                                 │
│                                                             │
│ NO INCLUYE:                                                 │
│ - createdAt, updatedAt (internos)                           │
│ - entityState (internos)                                    │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ↓ (UI)
┌─────────────────────────────────────────────────────────────┐
│ USUARIO (Interfaz)                                          │
│                                                             │
│ Ve solo lo que necesita:                                    │
│ - Nombre: Juan García                                       │
│ - NIF: 12345678A                                            │
│ - Ciudad: Madrid                                            │
│ - etc.                                                      │
└─────────────────────────────────────────────────────────────┘
```

---

## 7. Resumen de Conceptos

| Concepto | Ubicación | Propósito | Origen |
|----------|-----------|----------|--------|
| **ClientRecord** | Persistence | Representar fila de BD en Java | Base de datos |
| **ClientDto** | Service (interfaz) | Pasar datos a UI | Record convertido |
| **Assembler** | Service (crud) | Convertir Record ↔ DTO | Código de mapeo manual |
| **BaseRecord** | Gateway interface | Campos comunes de auditoría | Herencia |
| **ClientGateway** | Service (interfaz) | Definir operaciones de persistencia | Patrón arquitectónico |
| **ClientGatewayImpl** | Persistence (impl) | Ejecutar JDBC y mapear ResultSet → Record | Implementación real |

---

## 8. Preguntas Frecuentes

### ¿Por qué existen Record y DTO? ¿No podrían ser lo mismo?

**No. Por varias razones:**

1. **Seguridad**: El Record tiene `entityState`, `createdAt`, `updatedAt`. No queremos que la UI los modifique.
2. **Claridad**: El DTO tiene nombres más amigables (`addressStreet` vs `street`)
3. **Evolución**: Si la BD cambia, solo cambias Record. Si la interfaz cambia, solo cambias DTO.
4. **Validación**: La UI valida DTO, la BD valida Record. Dos capas de validación.

### ¿De dónde vienen los valores null en DTO si es nuevo?

Cuando el usuario agrega un cliente:
```java
ClientDto dto = new ClientDto();
dto.name = "Juan";
// dto.id es null   ← El Gateway genera UUID
// dto.version es 0 ← El Assembler lo convierte a 1
```

### ¿Por qué algunos campos tienen nombres diferentes?

```
addressStreet  ← agrupa campos de domicilio
addressCity    ← con prefijo "address" en DTO
addressCountry    
addressZipcode

Pero en Record y BD:
street, city, country, postalCode ← nombres más simples
```

**Razón:** Mejor UX para quien usa el DTO. Viendo `addressCity` sabes que es parte del domicilio.

### ¿Quién crea el id?

```java
// En Assembler.toRecord()
record.id = dto.id == null ? UUID.randomUUID().toString() : dto.id;
//           ↑ Si es nuevo (null), generar UUID
//           ↑ Si es actualización, usar el existente
```

---

## 9. Conclusión

**Los campos vienen de aquí:**

```
┌─────────────────┐
│  BASE DE DATOS  │  ← Fuente de verdad
└────────┬────────┘
         │
         ↓
    ┌────────────┐
    │  RECORD    │  ← Copia fiel de BD
    └────┬───────┘
         │ (Assembler mapea)
         ↓
      ┌─────────┐
      │  DTO    │  ← Versión "bonita" para UI
      └─────────┘
```

**Cada capa tiene una responsabilidad clara:**
- **BD:** Persistencia
- **Record:** Representación Java de la BD
- **DTO:** Interfaz amigable entre Service y UI
- **Assembler:** Traducción entre Record y DTO

