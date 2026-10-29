# Modelos de Examen - Prueba de Autoría (Código)

## Contexto
La prueba de autoría consiste en **ampliar el proyecto existente** implementando nuevas funcionalidades siguiendo los patrones arquitectónicos ya establecidos. No hay teoría, solo código.

---

## Modelo 1: Filtrado de Clientes por Ciudad (FÁCIL)

### Enunciado
Se requiere agregar la capacidad de **buscar clientes por ciudad**. El usuario debería poder:
1. Acceder desde el menú de clientes
2. Ingresar una ciudad
3. Ver listado de todos los clientes de esa ciudad

### Requisitos Técnicos
- Crear método `findByCity(String city)` en `ClientGateway`
- Implementar en `ClientGatewayImpl`
- Agregar query en `queries.properties`
- Crear `FindClientsByCity` command si es necesario
- Agregar opción en `ClientMenu`

### Qué Evalúan
✅ Comprensión del patrón Gateway  
✅ Uso de queries.properties  
✅ Estructura de Commands  
✅ Integración con UI  

### Pistas de Código Existente
```java
// Similar a findByNif - ver ClientGatewayImpl.java línea ~80
@Override
public Optional<ClientRecord> findByNif(String nif) {
    return findOne("TCLIENTS_FIND_BY_NIF", nif);
}
```

### Solución Esperada (Esbozo)

**queries.properties:**
```properties
TCLIENTS_FIND_BY_CITY = SELECT id, version, createdAt, updatedAt, entityState, nif, \
    name, surname, street, city, country, postalCode, phone, email FROM TClients WHERE city = ? ORDER BY name
```

**ClientGatewayImpl.java:**
```java
@Override
public List<ClientRecord> findByCity(String city) {
    try (PreparedStatement pst = Jdbc.getCurrentConnection()
        .prepareStatement(Queries.getSQLSentence("TCLIENTS_FIND_BY_CITY"))) {
        pst.setString(1, city);
        try (ResultSet rs = pst.executeQuery()) {
            return assembler.toRecordList(rs);
        }
    } catch (SQLException e) {
        throw new PersistenceException(e);
    }
}
```

**ClientMenu.java:**
```java
options.put("5", new Action("Buscar clientes por ciudad") {
    @Override
    public void execute() {
        String city = readString("Ingrese la ciudad: ");
        List<ClientDto> clients = service.findByCity(city);
        clients.forEach(c -> System.out.println(c.name + " - " + c.city));
    }
});
```

---

## Modelo 2: Mostrar Bonos Disponibles por Cliente (FÁCIL-MEDIO)

### Enunciado
Se requiere agregar una funcionalidad en el menú de bonos para **listar bonos disponibles (no consumidos) de un cliente específico**. El usuario debería:
1. Ingresar el NIF del cliente
2. Ver solo los bonos con estado AVAILABLE

### Requisitos Técnicos
- Crear método `findAvailableByClientNif(String nif)` en `VoucherGateway`
- Implementar en `VoucherGatewayImpl`
- Agregar query SQL en `queries.properties`
- Crear command `FindAvailableVouchersByClient`
- Agregar método en `VoucherGenerationService`
- Agregar opción en `VoucherMenu`

### Qué Evalúan
✅ Integración Service → Persistence  
✅ Uso de condiciones SQL (WHERE disponible > 0)  
✅ Mapeo de resultados con Assembler  
✅ Creación de nuevo Command  

### Pistas de Código Existente
```java
// VoucherGatewayImpl - findByClientNif (línea ~45)
// Usa LEFT JOIN con TPaymentMeans y TVouchers
// Necesitas agregar AND v.available > 0
```

### Solución Esperada (Esbozo)

**queries.properties:**
```properties
TVOUCHERS_FIND_AVAILABLE_BY_CLIENT_NIF = SELECT pm.id, pm.version, pm.createdAt, \
    pm.updatedAt, pm.entityState, pm.client_id, pm.accumulated, v.code, \
    v.description, v.available FROM TPaymentMeans pm \
    JOIN TVouchers v ON v.id = pm.id \
    JOIN TClients c ON c.id = pm.client_id \
    WHERE c.nif = ? AND v.available > 0 ORDER BY v.code
```

---

## Modelo 3: Validar Email Único (MEDIO)

### Enunciado
Actualmente el sistema permite que dos clientes tengan el **mismo email**. Se requiere:
1. Agregar validación para que **emails sean únicos**
2. Permitir actualizar un cliente sin cambiar su email sin error
3. Lanzar `BusinessException` si se intenta usar un email que ya existe en otro cliente

### Requisitos Técnicos
- Crear método `findOtherByEmail(String email, String clientId)` en `ClientGateway`
  - Similar a `findOtherByNif`, pero con email
  - Usar operador `<>` para excluir cliente actual
- Implementar en `ClientGatewayImpl`
- Agregar query SQL
- Agregar validación en `UpdateClient` command
- Actualizar también `AddClient` para validar email no exista

### Qué Evalúan
✅ Comprensión de validaciones en Commands  
✅ Diferencia entre AddClient y UpdateClient  
✅ SQL con `<>` para unicidad al actualizar  
✅ BusinessException  

### Pistas de Código Existente
```java
// UpdateClient.java línea ~30
BusinessChecks.doesNotExist(clients.findOtherByNif(client.nif, client.id),
    "Ya existe otro cliente con ese NIF");

// AddClient.java línea ~50
BusinessChecks.doesNotExist(clients.findByNif(client.nif), 
    "Ya existe un cliente con ese NIF");
```

### Solución Esperada (Esbozo)

**queries.properties:**
```properties
TCLIENTS_FIND_OTHER_BY_EMAIL = SELECT id FROM TClients WHERE email = ? AND id <> ?
TCLIENTS_FIND_BY_EMAIL = SELECT id, version, createdAt, updatedAt, entityState, nif, \
    name, surname, street, city, country, postalCode, phone, email FROM TClients WHERE email = ?
```

**ClientGatewayImpl.java:**
```java
@Override
public Optional<ClientRecord> findByEmail(String email) {
    return findOne("TCLIENTS_FIND_BY_EMAIL", email);
}

@Override
public Optional<ClientRecord> findOtherByEmail(String email, String id) {
    try (PreparedStatement pst = Jdbc.getCurrentConnection()
        .prepareStatement(Queries.getSQLSentence("TCLIENTS_FIND_OTHER_BY_EMAIL"))) {
        pst.setString(1, email);
        pst.setString(2, id);
        try (ResultSet rs = pst.executeQuery()) {
            if (!rs.next()) {
                return Optional.empty();
            }
            ClientRecord record = new ClientRecord();
            record.id = rs.getString("id");
            return Optional.of(record);
        }
    } catch (SQLException e) {
        throw new PersistenceException(e);
    }
}
```

---

## Modelo 4: Auditoría de Cambios en Clientes (MEDIO)

### Enunciado
Se requiere agregar una tabla `TAUDIT_LOGS` para registrar **cada vez que se modifica un cliente**:
- Qué cliente se modificó
- Cuándo se modificó
- Qué operación (ADD, UPDATE, DELETE)
- Cambios específicos (campos anteriores y nuevos)

### Requisitos Técnicos
- Crear tabla `TAUDIT_LOGS` en la BD (ya existe para el examen)
- Crear `AuditRecord` class
- Crear `AuditLogGateway` interface
- Implementar `AuditLogGatewayImpl`
- Modificar `AddClient`, `UpdateClient`, `DeleteClientByNif` Commands para registrar auditoría
- Crear `ViewAuditLogs` command o similar

### Qué Evalúan
✅ Creación de nuevos Gateways  
✅ Modificación de Commands existentes  
✅ Transacciones (auditoría debe guardarse incluso si falla lo principal)  
✅ Patrón de extensión sin modificar interfaz  

### Pistas de Código Existente
```java
// AddClient.java línea ~50
public ClientDto execute() throws BusinessException {
    validate(client);
    // ... validaciones ...
    ClientRecord record = ClientDtoAssembler.toRecord(client);
    clients.add(record);
    // ← Aquí se debería registrar en audit
}
```

### Solución Esperada (Esbozo)

**AuditLogGateway.java:**
```java
public interface AuditLogGateway {
    void log(String clientId, String operation, String changes);
    
    class AuditLogRecord {
        public String id;
        public String clientId;
        public String operation;  // ADD, UPDATE, DELETE
        public String changes;    // JSON o texto descriptivo
        public LocalDateTime timestamp;
    }
}
```

**AddClient.java (modificado):**
```java
ClientRecord record = ClientDtoAssembler.toRecord(client);
clients.add(record);
auditGateway.log(record.id, "ADD", "Cliente agregado: " + client.name);
```

---

## Modelo 5: Implementar Búsqueda Avanzada de Clientes (MEDIO-DIFÍCIL)

### Enunciado
Se requiere una **búsqueda combinada** de clientes donde el usuario pueda filtrar por:
- Nombre (parcial, case-insensitive)
- Ciudad
- Teléfono (parcial)
- Y combinar estos filtros

Ejemplo:
```
Nombre: "Juan"
Ciudad: "Madrid"
Teléfono: "912"
→ Resultado: Clientes llamados Juan, en Madrid, con teléfono que contiene 912
```

### Requisitos Técnicos
- Crear clase `ClientSearchCriteria` que almacene criterios
- Crear método `search(ClientSearchCriteria criteria)` en `ClientGateway`
- Implementar con SQL dinámico (WHERE 1=1 y agregar condiciones)
- Crear query dinámica en `ClientGatewayImpl` (sin usar queries.properties para esta)
- Crear command `SearchClients`
- Agregar opción en `ClientMenu`

### Qué Evalúan
✅ SQL dinámico (no siempre queries.properties)  
✅ Criterios de búsqueda complejos  
✅ LIKE en SQL para búsquedas parciales  
✅ Construcción de queries con condiciones opcionales  

### Pistas de Código Existente
```java
// SQL with LIKE - no exists en proyecto actual
// Necesitas: WHERE name LIKE ? OR surname LIKE ?
// Parámetros: "%Juan%"
```

### Solución Esperada (Esbozo)

**ClientGatewayImpl.java:**
```java
public List<ClientRecord> search(ClientSearchCriteria criteria) {
    StringBuilder sql = new StringBuilder(
        "SELECT id, version, createdAt, updatedAt, entityState, nif, " +
        "name, surname, street, city, country, postalCode, phone, email " +
        "FROM TClients WHERE 1=1");
    
    List<String> params = new ArrayList<>();
    
    if (criteria.hasName()) {
        sql.append(" AND (name LIKE ? OR surname LIKE ?)");
        String pattern = "%" + criteria.getName() + "%";
        params.add(pattern);
        params.add(pattern);
    }
    
    if (criteria.hasCity()) {
        sql.append(" AND city = ?");
        params.add(criteria.getCity());
    }
    
    if (criteria.hasPhone()) {
        sql.append(" AND phone LIKE ?");
        params.add("%" + criteria.getPhone() + "%");
    }
    
    sql.append(" ORDER BY name");
    
    try (PreparedStatement pst = Jdbc.getCurrentConnection()
        .prepareStatement(sql.toString())) {
        for (int i = 0; i < params.size(); i++) {
            pst.setString(i + 1, params.get(i));
        }
        try (ResultSet rs = pst.executeQuery()) {
            return assembler.toRecordList(rs);
        }
    } catch (SQLException e) {
        throw new PersistenceException(e);
    }
}
```

---

## Modelo 6: Registrar Historial de Bonos Consumidos (DIFÍCIL)

### Enunciado
Se requiere agregar un **registro de historial** para cada bono consumido:
- Cuándo se usó el bono
- En qué factura
- Cuánto se consumió
- Saldo anterior y saldo actual

La tabla es: `TVOUCHER_USAGE_HISTORY`

### Requisitos Técnicos
- Crear `VoucherUsageRecord` y `VoucherUsageGateway`
- Implementar `VoucherUsageGatewayImpl`
- Modificar el flujo de pago (donde se consume bonos) para registrar historial
- Agregar commands para consultar historial de un bono
- Agregar opción en VoucherMenu para ver historial

### Qué Evalúan
✅ Agregar nueva entidad completa (Record, Gateway, Impl)  
✅ Integración con flujo existente (payment)  
✅ Transacciones (actualizar bono + registrar uso)  
✅ Queries para listar historial con JOINs  

### Pistas de Código Existente
```java
// VoucherGenerationServiceImpl.java - comando GenerateVouchers
// Es donde los bonos se crean
// Similar: necesitas dónde se consumen
```

### Solución Esperada (Esbozo)

**VoucherUsageGateway.java:**
```java
public interface VoucherUsageGateway {
    void recordUsage(VoucherUsageRecord record);
    List<VoucherUsageRecord> findByVoucherId(String voucherId);
    
    class VoucherUsageRecord {
        public String id;
        public String voucherId;
        public String invoiceId;
        public BigDecimal amountUsed;
        public BigDecimal balanceBefore;
        public BigDecimal balanceAfter;
        public LocalDateTime usedAt;
    }
}
```

---

## Modelo 7: Estadísticas de Bonos (DIFÍCIL)

### Enunciado
Se requiere un nuevo menú "Estadísticas de Bonos" que muestre:
1. **Total de bonos generados** (cantidad)
2. **Valor total distribuido** (suma de available + accumulated)
3. **Bonos más usados** (top 5)
4. **Cliente con más bonos** (por cantidad y por valor)
5. **Tasa de consumo** (consumido / disponible)

### Requisitos Técnicos
- Crear `VoucherStatisticsService` interface
- Crear `VoucherStatisticsServiceImpl`
- Crear varios Commands para cada estadística
- Agregar queries SQL con GROUP BY, SUM, COUNT, ORDER BY
- Crear `VoucherStatisticsMenu`
- DTOs para resultados: `VoucherStatDto`, `TopVoucherDto`, etc.

### Qué Evalúan
✅ Queries SQL agregadas (SUM, COUNT, GROUP BY, ORDER BY, LIMIT)  
✅ Creación de nuevo Service desde cero  
✅ DTOs para reportes  
✅ Integración con ServiceFactory  

### Pistas de Código Existente
```java
// queries.properties - TVOUCHERS_SUMMARY (ya existe)
TVOUCHERS_SUMMARY = SELECT c.nif, c.name, c.surname, COUNT(v.id) AS issuedQty, \
    SUM(pm.accumulated + v.available) AS totalAmount, ...
```

### Solución Esperada (Esbozo)

**VoucherStatisticsService.java:**
```java
public interface VoucherStatisticsService {
    class VoucherStatDto {
        public int totalIssued;
        public BigDecimal totalValue;
        public BigDecimal totalConsumed;
        public BigDecimal totalAvailable;
        public double consumptionRate;
    }
    
    VoucherStatDto getStatistics();
}
```

---

## Modelo 8: Filtro de Órdenes de Trabajo por Estado (MEDIO)

### Enunciado
Se requiere poder **filtrar órdenes de trabajo por estado**:
- OPEN: órdenes abiertas
- INVOICED: órdenes facturadas
- FINISHED: órdenes completadas

El usuario debería poder:
1. Ver menú de opciones
2. Seleccionar un estado
3. Ver todas las órdenes en ese estado

### Requisitos Técnicos
- Crear método `findByState(WorkOrderState state)` en `WorkOrderGateway`
- Implementar en `WorkOrderGatewayImpl`
- Agregar query en `queries.properties`
- Crear command si es necesario
- Agregar opción en un menú de órdenes de trabajo

### Qué Evalúan
✅ Trabajo con Enums en SQL  
✅ Queries con WHERE para filtrado  
✅ Conversión String ↔ Enum  

### Solución Esperada (Esbozo)

**queries.properties:**
```properties
TWORKORDERS_FIND_BY_STATE = SELECT id, version, entityState, createdAt, updatedAt, \
    vehicle_id, description, date, total_amount, state, mechanic_id, invoice_id \
    FROM TWorkOrders WHERE state = ? ORDER BY date DESC
```

---

## Modelo 9: Recalcular Bonos Pendientes (DIFÍCIL)

### Enunciado
Existe un comando de administración que **recalcula todos los bonos pendientes** de generar. Se requiere:
1. Buscar clientes con 3 órdenes PAID no usadas para bono
2. Generar bono para cada uno
3. Marcar órdenes como usadas
4. Registrar en log del sistema

### Requisitos Técnicos
- Crear command `RecalculatePendingVouchers`
- Usar transacciones para atomicidad
- Agregar query para encontrar elegibles
- Usar `VoucherGenerationService` internamente
- Integrar en menú de administración

### Qué Evalúan
✅ Transacciones complejas  
✅ Iteración sobre conjuntos de datos  
✅ Uso de múltiples Gateways en un Command  
✅ Manejo de errores parciales  

---

## Modelo 10: Exportar Datos de Clientes (DIFÍCIL)

### Enunciado
Se requiere una funcionalidad de **exportación de datos de clientes a archivo**:
1. Usuario selecciona qué datos exportar (nombre, NIF, email, etc.)
2. Sistema genera archivo CSV
3. Usuario elige si incluir bonos de cada cliente

### Requisitos Técnicos
- Crear `ExportService` con método `exportClientsToCSV()`
- Usar `ClientGateway` y `VoucherGateway`
- Crear archivo en carpeta temporal
- Manejar excepciones de I/O
- Mostrar mensaje con ruta del archivo

### Qué Evalúan
✅ I/O en Java  
✅ Formato CSV  
✅ Integración Service + Gateways  
✅ Manejo de excepciones  

---

## Recomendación para Preparación

**Orden de dificultad para practicar:**

1. **Semana 1:** Modelos 1-2 (Fácil) - Comprender queries y Gateways
2. **Semana 2:** Modelos 3-4 (Medio) - Commands y validaciones
3. **Semana 3:** Modelos 5-6 (Difícil) - Queries complejas y nuevas entidades
4. **Semana 4:** Modelos 7-10 (Muy Difícil) - Servicios completos y reportes

**Tips para el examen:**
- Sigue EXACTAMENTE los patrones del proyecto
- Los Gateways siempre en persistence/
- Los Commands siempre en service/*/command/
- Queries SIEMPRE en queries.properties
- Valida en Commands con BusinessException
- Usa Assemblers para convertir Record ↔ DTO
- Cada entidad nueva necesita: Record, Gateway interface, GatewayImpl, Queries
