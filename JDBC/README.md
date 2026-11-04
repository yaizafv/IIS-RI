# Guía para la prueba de autoría — CWS2026 (JDBC)

**Caso de implementación: UO % 3 == 1**
- Administrador: generación automática de bonos **por 3 órdenes de trabajo** (bono de 20 €).
- Jefe de taller (Foreman): **gestión de clientes** (CRUD + listar recomendados).

> Esta guía está hecha a partir de TU código (`cws2026/cws_services_jdbc`), de los enunciados (P3, P4, Generación de bonos, Enunciado de referencia), de la checklist comentada y de las diapositivas de JDBC.
> Está pensada para leerla en orden. Si vas justa de tiempo: lee las secciones **3, 4, 5, 7 y 8** como mínimo.

---

## Índice

1. [Qué es la prueba de autoría y qué te pueden pedir](#1-qué-es-la-prueba-de-autoría-y-qué-te-pueden-pedir)
2. [Conceptos básicos que necesitas (Java, SQL, JDBC)](#2-conceptos-básicos-que-necesitas)
3. [La arquitectura del proyecto: las capas y los patrones](#3-la-arquitectura-del-proyecto-capas-y-patrones)
4. [Recorrido completo de una operación (de la pantalla a la BD y vuelta)](#4-recorrido-completo-de-una-operación)
5. [Tu código explicado clase por clase](#5-tu-código-explicado-clase-por-clase)
6. [La base de datos: tablas que te importan](#6-la-base-de-datos-tablas-que-te-importan)
7. [LA RECETA: cómo añadir una consulta nueva ("un find") paso a paso](#7-la-receta-cómo-añadir-una-consulta-nueva-un-find)
8. [Ejemplos resueltos de ampliaciones probables](#8-ejemplos-resueltos-de-ampliaciones-probables)
9. [Reglas de la checklist que NO puedes romper](#9-reglas-de-la-checklist-que-no-puedes-romper)
10. [Cosas de tu código que te pueden preguntar (puntos débiles)](#10-cosas-de-tu-código-que-te-pueden-preguntar)
11. [Preguntas teóricas típicas con respuesta](#11-preguntas-teóricas-típicas-con-respuesta)
12. [Cómo ejecutar la aplicación y los tests](#12-cómo-ejecutar-la-aplicación-y-los-tests)
13. [Chuleta final (1 página)](#13-chuleta-final)

---

## 1. Qué es la prueba de autoría y qué te pueden pedir

El propio enunciado de bonos lo dice (pág. 7):

> "la demostración de autoría suele consistir en **preguntas cortas y modificaciones** sobre conceptos con los que te tuviste que enfrentar en las prácticas [...] los fallos que cometas en la demostración de autoría se contarán como si fueran fallos hechos en la práctica entregada."

Es decir: te sientas con TU proyecto delante y te piden **cambiar o añadir algo pequeño**, respetando la arquitectura. Lo que te dijeron del año pasado ("una consulta con un find") encaja perfectamente: casi seguro fue algo como

> *"Añade una operación que liste los clientes que cumplan X"* → eso implica crear un método `findXxx` en un **Gateway**, su **SQL** en `queries.properties`, un **comando** en la capa de servicio y (quizá) una **acción** en el menú.

Tipos de ampliación probables (de más a menos probable):

| Tipo | Ejemplo | Dónde se toca |
|---|---|---|
| **Nueva consulta (find)** | Listar clientes de una ciudad / clientes que han recomendado a ≥ N clientes / bonos de un cliente con saldo | queries.properties + Gateway + GatewayImpl + Command + ServiceImpl (+ UI) |
| **Cambiar una regla de negocio** | Bono por 4 OTs en vez de 3; bono de 25 € en vez de 20 €; no borrar clientes con recomendaciones | Command (`GenerateVouchers`, `DeleteClientByNif`, `AddClient`...) |
| **Implementar otro tipo de bono** | Bono de 30 € por factura > 500 € (el caso de otros UOs) | queries + InvoiceGateway + GenerateVouchers |
| **Implementar un listado agregado** | `getVoucherSummary()` (resumen de bonos por cliente) | ClientGateway + VoucherGateway + Command nuevo |
| **Añadir una opción a un menú** | Añadir "Listar recomendados" al menú del Foreman | Clase `...Menu` + clase `...Action` |

Todo esto está resuelto paso a paso en la sección 8.

---

## 2. Conceptos básicos que necesitas

### 2.1 Java mínimo

- **Clase**: una plantilla de objetos (`public class AddClient { ... }`).
- **Interfaz** (`interface`): un "contrato": dice QUÉ métodos hay, pero no CÓMO se hacen. Una clase que `implements` una interfaz está obligada a escribir esos métodos.
  - Ejemplo: `ClientGateway` (interfaz) dice que existe `findByNif(String nif)`. `ClientGatewayImpl` (clase) es quien escribe el SQL de verdad.
- **`extends`**: herencia. `ClientRecord extends BaseRecord` → `ClientRecord` tiene automáticamente `id`, `version`, `createdAt`, `updatedAt`, `entityState`.
- **Genéricos `<T>`**: `Command<List<ClientDto>>` significa "un comando cuyo `execute()` devuelve una `List<ClientDto>`". `Command<Void>` = no devuelve nada (por eso acabas con `return null;`).
- **`static`**: pertenece a la clase, no a un objeto. `Factories.persistence` es estático: lo usas sin hacer `new Factories()`.
- **`final`**: no se puede reasignar.
- **`List<T>`**: lista de elementos. `new ArrayList<>()`, `.add(x)`, `.isEmpty()`, `.size()`, `.get(0)`.
- **`Optional<T>`**: una "caja" que puede tener un valor o estar vacía. Se usa en lugar de devolver `null`.
  - `opt.isEmpty()`, `opt.isPresent()`, `opt.get()` (sacar el valor; ¡peta si está vacía!), `Optional.of(x)`, `Optional.empty()`.
- **`var`**: deja que Java deduzca el tipo (`var found = clients.findByNif(nif);` → `Optional<ClientRecord>`).
- **`record`** (Java 16+): clase de datos inmutable. `VoucherSummaryDto` es un `record`: se crea con `new VoucherSummaryDto(a, b, c...)` y sus campos se leen con métodos: `dto.nif()`, `dto.availableAmount()`.
- **`BigDecimal`**: números decimales exactos (dinero). NUNCA `double` para dinero.
  - `BigDecimal.ZERO`, `new BigDecimal("20.00")`, `a.add(b)`, `a.subtract(b)`, `a.multiply(b)`, `a.compareTo(b) > 0` (= a > b). **Nunca uses `==` ni `>` con BigDecimal.**
- **Strings**: compárarlos con `"PAID".equals(state)`, **nunca con `==`** (la checklist lo penaliza).

### 2.2 Excepciones (muy importante en la checklist)

- **Checked** (obligatorio declararlas con `throws` o capturarlas): `BusinessException`, `SQLException`.
- **Unchecked** (heredan de `RuntimeException`): `IllegalArgumentException`, `PersistenceException`, `NullPointerException`...

En el proyecto:

| Excepción | Quién la lanza | Significa | Cómo se lanza |
|---|---|---|---|
| `IllegalArgumentException` | Constructores de comandos | Te han pasado argumentos inválidos (null, vacío...) | `ArgumentChecks.isNotBlank(nif, "msg")` |
| `BusinessException` | `execute()` de comandos | Se viola una regla de negocio (NIF repetido, cliente no existe...) | `BusinessChecks.exists(opt, "msg")` o `throw new BusinessException("msg")` |
| `PersistenceException` | Gateways | Fallo de BD (envuelve a `SQLException`) | `catch (SQLException e) { throw new PersistenceException(e); }` |
| `UserInteractionException` | Acciones de UI | El usuario ha tecleado algo mal | `UserInteractionChecks.isNotEmpty(...)` |

Quién captura qué: el **`BaseMenu`** (código proporcionado) captura todas y muestra el mensaje. El **`CommandExecutor`** captura `Exception` para hacer `rollback` y relanzarla. **Tú no debes poner `catch` en ningún otro sitio** (salvo `catch (SQLException e)` en los Gateways).

### 2.3 SQL mínimo

```sql
-- Leer
SELECT col1, col2 FROM TClients WHERE nif = ?            -- filtrar
SELECT ... FROM TClients ORDER BY surname ASC, name       -- ordenar (DESC = de mayor a menor)
SELECT DISTINCT ...                                        -- sin filas repetidas

-- Unir tablas (JOIN): "dame las órdenes de trabajo con el cliente dueño del vehículo"
SELECT w.id, c.nif
FROM TWorkOrders w
JOIN TVehicles v ON w.vehicle_id = v.id      -- la OT apunta al vehículo
JOIN TClients  c ON v.client_id  = c.id      -- el vehículo apunta al cliente
WHERE c.nif = ?

-- Agregar (agrupar y contar/sumar)
SELECT recommender_id, COUNT(*) FROM TRecommendations GROUP BY recommender_id
SELECT ... GROUP BY x HAVING COUNT(*) >= 3        -- HAVING = WHERE pero sobre grupos
SUM(col), MAX(col), MIN(col), AVG(col)

-- Subconsultas
WHERE c.id IN (SELECT r.recommender_id FROM TRecommendations r)
WHERE EXISTS (SELECT 1 FROM TVehicles v WHERE v.client_id = c.id)

-- NULL se compara con IS NULL / IS NOT NULL (nunca "= NULL")
WHERE w.invoice_id IS NULL

-- Modificar
INSERT INTO TClients (id, nif, ...) VALUES (?, ?, ...)
UPDATE TClients SET name = ?, version = version + 1 WHERE id = ? AND version = ?
DELETE FROM TClients WHERE id = ?
```

El `?` es un **parámetro**: se rellena desde Java con `pst.setString(1, valor)` (los índices **empiezan en 1**, no en 0).

### 2.4 JDBC mínimo (diapositivas del tema)

JDBC = API de Java para hablar con bases de datos relacionales. Piezas:

| Pieza | Qué es |
|---|---|
| `DriverManager` | Elige el driver correcto según la URL y te da una conexión: `DriverManager.getConnection(URL, USER, PASS)` |
| `Connection` | La sesión con la BD. Las transacciones van por conexión. |
| `PreparedStatement` | Sentencia SQL precompilada con `?`. Se rellenan con `setString`, `setLong`, `setBigDecimal`, `setTimestamp`, `setBoolean`... |
| `executeQuery()` | Para `SELECT` → devuelve `ResultSet` |
| `executeUpdate()` | Para `INSERT/UPDATE/DELETE` → devuelve nº de filas afectadas (`int`) |
| `ResultSet` | El resultado. Tiene un **cursor** que empieza ANTES de la primera fila. `rs.next()` avanza y devuelve `false` cuando no hay más. Se lee con `rs.getString("nif")`, `rs.getBigDecimal("available")`, `rs.getTimestamp(...)`... |

Patrón típico (es el que usas en todos tus gateways):

```java
try (PreparedStatement pst = Jdbc.getCurrentConnection().prepareStatement(sql)) {  // 1. preparar
    pst.setString(1, nif);                                                         // 2. parámetros
    try (ResultSet rs = pst.executeQuery()) {                                      // 3. ejecutar
        while (rs.next()) { ... rs.getString("name") ... }                         // 4. recorrer
    }
} catch (SQLException e) {
    throw new PersistenceException(e);                                             // 5. traducir error
}
```

El `try ( ... )` con paréntesis es **try-with-resources**: cierra automáticamente `pst` y `rs` al acabar, aunque haya excepción. **Liberar recursos** es un punto de la checklist. Fíjate que la **conexión NO se cierra en el gateway**: la cierra el `CommandExecutor`.

**Transacción**: conjunto de operaciones que se hacen "todo o nada". Por defecto JDBC está en *autocommit* (cada sentencia se confirma sola). Para controlarlo: `con.setAutoCommit(false)` → operaciones → `con.commit()` (confirmar) o `con.rollback()` (deshacer). En tu proyecto eso lo hace el `CommandExecutor`.

**`Statement` vs `PreparedStatement`**: `PreparedStatement` es precompilado (más rápido si se repite), usa `?` y **evita inyección SQL** (los valores nunca se mezclan con el texto del SQL). Siempre `PreparedStatement`.

---

## 3. La arquitectura del proyecto (capas y patrones)

### 3.1 Las tres capas

```
┌──────────────────────────────────────────────────────────┐
│  UI (presentación)           paquete ...application.ui   │  ← Lee teclado / imprime. NADA de SQL ni reglas.
│  Menús (BaseMenu) + Actions                              │
└───────────────┬──────────────────────────────────────────┘
                │ Factories.service.forClientCrudService().addClient(dto, nif)
                ▼  (solo conoce INTERFACES de servicio y DTOs)
┌──────────────────────────────────────────────────────────┐
│  SERVICE (lógica de negocio)  ...application.service     │  ← Reglas de negocio. NADA de SQL ni Console.
│  ServiceImpl (fachada) → CommandExecutor → Command       │
└───────────────┬──────────────────────────────────────────┘
                │ Factories.persistence.forClient().findByNif(nif)
                ▼  (solo conoce INTERFACES de gateway y Records)
┌──────────────────────────────────────────────────────────┐
│  PERSISTENCE (acceso a datos) ...application.persistence │  ← Solo SQL. NADA de reglas de negocio.
│  Gateway (interfaz) → GatewayImpl (JDBC) → BD HSQLDB     │
└──────────────────────────────────────────────────────────┘
```

Regla de oro: **cada capa solo habla con la de debajo, y solo a través de interfaces + factorías**.

### 3.2 Los patrones (lo que te preguntarán por nombre)

| Patrón | Qué es | En tu código |
|---|---|---|
| **Layers (capas)** | Separar UI / negocio / persistencia | Paquetes `ui`, `service`, `persistence` |
| **Facade (fachada)** | Un punto único de entrada a un subsistema. **No tiene lógica**, solo delega. | `ClientCrudServiceImpl`, `VoucherGenerationServiceImpl`, `MechanicCrudServiceImpl` |
| **Simple Factory** | Una clase que crea objetos para que el cliente no haga `new XxxImpl()` | `ServiceFactoryImpl` (crea fachadas), `PersistenceFactoryImpl` (crea gateways), y `Factories` que las agrupa |
| **Transaction Script (TS)** | Un procedimiento por cada operación de negocio, ejecutado en una transacción | `AddClient`, `DeleteClientByNif`, `GenerateVouchers`... |
| **Command** | Encapsular cada operación en un objeto con un único método `execute()` | Los TS implementan `Command<T>`; el **Invoker** es `CommandExecutor` |
| **Table Data Gateway (TDG)** | Un objeto por TABLA que hace todo su SQL (add, remove, update, findById, findAll + finds específicos) | `ClientGateway` / `ClientGatewayImpl`, `WorkOrderGateway`... |
| **DTO (Data Transfer Object)** | Objeto "tonto" con campos públicos para pasar datos entre capas | Servicio: `ClientDto`, `VoucherDto`. Persistencia: `ClientRecord`, `WorkOrderRecord` |
| **Assembler** | Clases que convierten entre DTO ↔ Record ↔ ResultSet. **Sin lógica**. | `ClientDtoAssembler` (service), `ClientRecordAssembler` (persistence) |

### 3.3 Normas de nombres (las piden en la checklist)

- Paquete de persistencia de una tabla = nombre de tabla **en singular, sin la T, minúsculas**: `TClients` → `persistence.client`.
- Interfaz: `ClientGateway`; implementación: `persistence.client.impl.ClientGatewayImpl` (sufijo `Impl`).
- Record: `ClientRecord` (tabla en singular sin T + `Record`). Campos = columnas de la tabla en minúscula.
- Fachada: `<Interfaz>Impl` → `ClientCrudServiceImpl`.
- Comando: **igual que el método de la interfaz con mayúscula inicial**: método `findClientsRecommendedBy` → clase `FindClientsRecommendedBy`.
- Comandos en el paquete `service.<entidad>.<caso>.commands` (`service.client.crud.commands`).
- `queries.properties`:
  - Agrupado por tabla, con comentario `# TCLIENTS` encima.
  - Tablas en orden alfabético, y dentro de cada tabla las claves en orden alfabético.
  - Clave = `NOMBRETABLA_NOMBRE_DEL_METODO_DEL_GATEWAY` en mayúsculas: `findByNif` → `TCLIENTS_FIND_BY_NIF`.

---

## 4. Recorrido completo de una operación

Vamos a seguir **"Añadir cliente"** de principio a fin. Si entiendes esto, entiendes todo el proyecto.

```
Usuario elige "Register a client" en el menú del Foreman
│
▼
[UI] AddClientAction.execute()
    - Lee con Console.readString(...) nif, nombre, ... y el NIF del recomendador
    - Comprueba que no estén vacíos (UserInteractionChecks)
    - Factories.service.forClientCrudService()  → devuelve un ClientCrudServiceImpl
          .addClient(client, recommender)
│
▼
[SERVICE - fachada] ClientCrudServiceImpl.addClient(dto, nif)
    return executor.execute(new AddClient(client, recommenderNif));
    (el constructor de AddClient valida argumentos → IllegalArgumentException si mal)
│
▼
[SERVICE - invoker] CommandExecutor.execute(cmd)
    1. Connection c = Jdbc.createThreadConnection();   ← abre conexión y la guarda en el hilo (ThreadLocal)
    2. c.setAutoCommit(false);                         ← empieza transacción
    3. T res = cmd.execute();                          ← ejecuta el comando
    4. c.commit();                                     ← si todo fue bien, confirma
       (si hubo excepción → c.rollback() y relanza)
    5. try-with-resources cierra la conexión
│
▼
[SERVICE - comando] AddClient.execute()
    - validate(client)                       → campos no vacíos
    - clients.findByNif(nif) debe estar vacío → si no, BusinessException "Ya existe un cliente con ese NIF"
    - si hay recomendador: no puede ser él mismo y debe existir
    - ClientDtoAssembler.toRecord(dto)       → convierte ClientDto → ClientRecord (genera UUID)
    - clients.add(record)                    → INSERT en TClients
    - paymentMeans.add(cashRecord)           → INSERT en TPaymentMeans (dtype='Cash')
    - cash.add(cashRecord)                   → INSERT en TCashes (mismo id)
    - si recomendador: recommendations.add(...) → INSERT en TRecommendations
    - devuelve el dto con id y version
│
▼
[PERSISTENCE] ClientGatewayImpl.add(record)
    - Jdbc.getCurrentConnection()            ← recupera LA MISMA conexión del hilo (no crea otra)
    - Queries.getSQLSentence("TCLIENTS_ADD") ← lee el SQL de queries.properties
    - pst.setXxx(...) para cada ? y executeUpdate()
    - si SQLException → PersistenceException
```

**¿Por qué `ThreadLocal`?** Porque todos los gateways que usa un comando tienen que usar **la misma conexión** para que todo esté en **la misma transacción**. `createThreadConnection()` la guarda "pegada" al hilo y `getCurrentConnection()` la recupera desde cualquier gateway sin pasarla como parámetro.

**¿Por qué el gateway no hace commit ni close de la conexión?** Porque si lo hiciera, cada `INSERT` se confirmaría por separado y si el tercero fallase, los dos primeros quedarían hechos (cliente sin medio de pago = datos corruptos). La checklist lo penaliza: *"Los objetos Gateway crean y liberan conexiones y/o transacciones"*.

---

## 5. Tu código explicado clase por clase

### 5.1 Infraestructura (proporcionada; no la toques)

- **`uo.ri.conf.Factories`**: dos campos estáticos `service` y `persistence`. Es el único sitio donde se nombran las clases `...Impl` de las factorías.
- **`Gateway<T>`**: interfaz genérica con `add`, `remove`, `update`, `findById`, `findAll` + la clase `BaseRecord` (`id` UUID aleatorio, `entityState="ENABLED"`, `version=1`, `createdAt`, `updatedAt`).
- **`PersistenceFactory` / `PersistenceFactoryImpl`**: `forClient()`, `forWorkOrder()`, `forVoucher()`... cada uno hace `return new XxxGatewayImpl();`.
- **`ServiceFactoryImpl`**: `forClientCrudService()` → `new ClientCrudServiceImpl()`, `forVoucherService()` → `new VoucherGenerationServiceImpl()`. Los que no te tocaban lanzan `NotYetImplementedException`.
- **`Command<T>`**: interfaz con `T execute() throws BusinessException`.
- **`CommandExecutor`**: explicado arriba (abre conexión, autocommit false, ejecuta, commit/rollback, cierra; convierte `SQLException` en `PersistenceException`).
- **`Jdbc`**: `createThreadConnection()` y `getCurrentConnection()` con `ThreadLocal`. Lee URL/usuario/clave de `connection.properties` (parámetros de conexión externalizados ✔).
- **`BaseRecordAssembler<T>`**: `toRecord(rs)` rellena los campos comunes (`id`, `version`, `entityState`, `createdAt`, `updatedAt`) de la fila ACTUAL; `toRecordList(rs)` hace `while (rs.next())` y llama a `toRecord`. Cada assembler concreto sobrescribe `toRecord` llamando a `super.toRecord(rs)` y añadiendo sus columnas.
- **`Queries`**: `Queries.getSQLSentence("CLAVE")` lee de `queries.properties`.

### 5.2 Persistencia (gateways)

| Gateway | Métodos extra (además de los de `Gateway`) | Implementados |
|---|---|---|
| `ClientGateway` | `findByNif`, `findOtherByNif(nif, id)`, `findRecommendedBy(recommenderNif)` | add, remove, update, findById, findAll + los 3 |
| `RecommendationGateway` | `findByClientId(clientId)` (las que hizo **o** recibió) | add, remove, findByClientId |
| `PaymentMeanGateway` | `findByClientId` | add, remove, findByClientId |
| `CashGateway` | — | add, remove (solo tabla `TCashes`) |
| `CardGateway` | — | remove |
| `VoucherGateway` | `findByClientId` (JOIN con TPaymentMeans) | add, remove, findByClientId |
| `VehicleGateway` | `findByClientId` | findById, findByClientId |
| `InvoiceGateway` | `findLastNumber`, `findByPayerId` | add, findById, findByPayerId, findLastNumber |
| `WorkOrderGateway` | `findNotUsedForVoucherByInvoiceState`, `findByMechanicId`, `findNotInvoicedWorkOrdersByClientNif` | update, findById + los 3 |

Lo que no está implementado lanza `NotYetImplementedException`. **Si en la prueba necesitas uno de esos (p. ej. `InvoiceGateway.update` o `RecommendationGateway.update`), tendrás que implementarlo** (ejemplos en la sección 8).

Detalles que conviene saber explicar:

- **`update` con control de versiones (bloqueo optimista)**:
  `UPDATE TClients SET ..., version = version + 1, updatedAt = ? WHERE id = ? AND version = ?`
  Si otro proceso modificó la fila entre medias, su `version` ya no coincide → `executeUpdate()` devuelve 0 → lanzas `PersistenceException("...version has changed")`. Así no se pisan cambios.
- **`findOtherByNif(nif, id)`**: `SELECT id FROM TClients WHERE nif = ? AND id <> ?` → "¿hay OTRO cliente (distinto de mí) con ese NIF?" Se usa al actualizar un cliente para que no choque su NIF nuevo con otro.
- **`findRecommendedBy`**: JOIN de `TClients` (recomendado) con `TRecommendations` y otra vez con `TClients` (recomendador) filtrando por el NIF del recomendador. Es un TDG de `TClients` que consulta otras tablas **para decidir qué filas de TClients devolver** → permitido por la checklist.
- **`findNotUsedForVoucherByInvoiceState(state)`**: OTs cuya factura está en `state` (PAID) y `usedForVoucher = FALSE`, **ordenadas por `v.client_id, w.date, w.id`**. El orden es clave para el algoritmo del bono (ver 5.4).

### 5.3 Servicio: gestión de clientes (`service.client.crud`)

- **`ClientCrudServiceImpl`** (fachada): cada método hace `return executor.execute(new Comando(...))`. Sin lógica.
- **`ClientDtoAssembler`**: `toRecord(ClientDto)`, `toDto(ClientRecord)`, `toCashRecord(clientId)` (crea el medio de pago "Cash" con `accumulated=0`), `toRecommendationRecord(recommenderId, recommendedId)` (con `usedForVoucher=false`). Los campos de dirección se llaman distinto: `dto.addressStreet` ↔ `record.street`, `dto.addressZipcode` ↔ `record.postalCode`...

Comandos:

| Comando | Qué hace | Reglas que comprueba |
|---|---|---|
| `AddClient(dto, recommenderNif)` | Inserta cliente + TPaymentMeans + TCashes (+ recomendación) | NIF no repetido; si hay recomendador: no es él mismo y existe |
| `UpdateClient(dto)` | Actualiza todos los campos menos id | Cliente existe; NIF nuevo no lo tiene otro (`findOtherByNif`); versión coincide |
| `DeleteClientByNif(nif)` | Borra recomendaciones (hechas y recibidas), medios de pago (primero la tabla hija TCashes/TCreditCards/TVouchers y luego TPaymentMeans) y el cliente | Cliente existe; no tiene vehículos; no ha pagado facturas (`invoices.findByPayerId` → ninguna `PAID`) |
| `FindAllClients()` | `findAll` → lista de DTOs | — |
| `FindClientByNif(nif)` | `Optional<ClientDto>` | — |
| `FindClientsRecommendedBy(nif)` | Lista de recomendados | — (si no existe, lista vacía) |

**¿Por qué en `DeleteClientByNif` se borra primero `TCashes` y luego `TPaymentMeans`?** Porque `TCashes.ID` es **clave ajena** (FOREIGN KEY) a `TPaymentMeans.ID`. Si borras primero el padre, la BD da error de integridad referencial. Mismo motivo por el que se borran las recomendaciones **antes** que el cliente (TRecommendations apunta a TClients).

### 5.4 Servicio: generación de bonos (`service.paymentmean.voucher`)

`GenerateVouchers.execute()` — algoritmo del bono de 20 € por 3 OTs:

1. Pide al gateway todas las OTs con factura `PAID` y `usedForVoucher = FALSE`, **ordenadas por cliente**.
2. Recorre la lista; para cada OT busca el cliente dueño (`vehicles.findById(wo.vehicleId).get().clientId`).
3. Si cambia de cliente, vacía el "lote" (`batch.clear()`).
4. Añade la OT al lote. Cuando el lote llega a **3** (`WORK_ORDERS_PER_VOUCHER`):
   - `markAsUsed(batch)`: pone `usedForVoucher = true` en las 3 y hace `update` (así no se reutilizan),
   - `createVoucher(clientId)`: inserta en `TPaymentMeans` (dtype `Voucher`) y en `TVouchers` (mismo id, código `"V-" + UUID`, descripción `"Por tres órdenes de trabajo"`, disponible `20.00`),
   - cuenta +1 y vacía el lote.
5. Devuelve `new int[] { generated, 0, 0 }` → la interfaz exige **{nº bonos 20 €, nº bonos 25 €, nº bonos 30 €}**. Los dos últimos son 0 porque a ti no te tocaban.

Ejemplo: cliente con 7 OTs pagadas sin usar → 2 bonos (6 OTs usadas), la 7ª queda libre para la próxima vez.

`FindVouchersByClientNif`: busca el cliente por NIF; si no existe → lista vacía; si existe → `vouchers.findByClientId(id)` → DTOs.

**Herencia en BD (tabla por subclase / "joined")**: un bono está en DOS tablas: `TPaymentMeans` (datos comunes: id, accumulated, client_id, `dtype='Voucher'`) y `TVouchers` (datos propios: code, description, available) **con el mismo `id`**. Por eso para crear un bono haces 2 `INSERT` y para leerlo un `JOIN ... ON pm.id = v.id`. `dtype` dice de qué tipo es cada medio de pago: `Cash`, `CreditCard` o `Voucher`.

### 5.5 UI

- **`BaseMenu`**: los menús son una matriz `{ "Texto", ClaseAction.class }`. Al elegir una opción crea la `Action` y llama a `execute()`. Captura las excepciones y muestra el mensaje (por eso tú no capturas nada en la UI).
- **`ClientMenu`** (Foreman > Client management): Register, Update, Delete, List all, View detail.
- **`VouchersMenu`** (Manager > Voucher management): Generate vouchers, Find vouchers by client nif.
- Las `Action` usan `Console.readString(...)`, `Console.println/printf` y `Printer.printClient(...)`.

---

## 6. La base de datos: tablas que te importan

(Sacado de `cws_database/test.script`.) PK = clave primaria, FK = clave ajena.

```
TCLIENTS          (ID PK, NIF UNIQUE, NAME, SURNAME, STREET, CITY, COUNTRY, POSTALCODE, PHONE, EMAIL,
                   VERSION, CREATEDAT, UPDATEDAT, ENTITYSTATE)

TRECOMMENDATIONS  (ID PK, RECOMMENDER_ID FK→TCLIENTS, RECOMMENDED_ID FK→TCLIENTS,
                   USEDFORVOUCHER, VERSION, ...)   UNIQUE(RECOMMENDER_ID, RECOMMENDED_ID)

TVEHICLES         (ID PK, LICENSEPLATE UNIQUE, MAKE, MODEL, CLIENT_ID FK→TCLIENTS, VEHICLETYPE_ID FK, ...)

TWORKORDERS       (ID PK, VEHICLE_ID FK→TVEHICLES, MECHANIC_ID FK, INVOICE_ID FK→TINVOICES,
                   DATE, DESCRIPTION, STATE, TOTAL_AMOUNT, USEDFORVOUCHER, ...)

TINVOICES         (ID PK, NUMBER UNIQUE, DATE, STATE ('ISSUED'|'PAID'), SUBTOTAL, VATAMOUNT, VATRATE,
                   TOTAL, USEDFORVOUCHER, ...)

TCHARGES          (ID PK, INVOICE_ID FK→TINVOICES, PAYMENTMEAN_ID FK→TPAYMENTMEANS, CHARGE (importe), ...)
                   UNIQUE(INVOICE_ID, PAYMENTMEAN_ID)

TPAYMENTMEANS     (ID PK, DTYPE ('Cash'|'CreditCard'|'Voucher'), ACCUMULATED, CLIENT_ID FK→TCLIENTS, ...)
  ├─ TCASHES      (ID PK FK→TPAYMENTMEANS)
  ├─ TCREDITCARDS (ID PK FK→TPAYMENTMEANS, NUMBER UNIQUE, TYPE, VALIDTHRU)
  └─ TVOUCHERS    (ID PK FK→TPAYMENTMEANS, CODE UNIQUE, DESCRIPTION, AVAILABLE)
```

Caminos típicos para unir tablas (apréndetelos, salen en todos los finds):

- **OT → cliente dueño**: `TWorkOrders w JOIN TVehicles v ON w.vehicle_id = v.id JOIN TClients c ON v.client_id = c.id`
- **Factura → OTs**: `TWorkOrders w WHERE w.invoice_id = i.id` (una factura tiene varias OTs)
- **Factura → quién la pagó**: `TInvoices i JOIN TCharges ch ON ch.invoice_id = i.id JOIN TPaymentMeans pm ON pm.id = ch.paymentMean_id` → `pm.client_id`
- **Bono → cliente**: `TVouchers v JOIN TPaymentMeans pm ON pm.id = v.id` → `pm.client_id`
- **Cliente → recomendados**: `TRecommendations r WHERE r.recommender_id = <id cliente>` → `r.recommended_id`

---

## 7. LA RECETA: cómo añadir una consulta nueva ("un find")

Memoriza estos pasos. Valen para casi cualquier ampliación. Supón que piden *"listar los clientes que cumplan X"*.

### Paso 0 — Piensa el SQL primero
¿Qué tabla devuelve filas? (→ eso decide **qué Gateway** lleva el método). ¿Necesito JOIN/subconsulta para filtrar? ¿Qué parámetros (`?`)?

> Regla: el método va en el Gateway de la tabla **cuyas filas devuelves**. Si devuelves clientes → `ClientGateway`, aunque filtres mirando otras tablas.

### Paso 1 — `queries.properties` (`cws_services_jdbc/src/queries.properties`)
Añade la sentencia en la sección de su tabla, en orden alfabético, clave `TABLA_NOMBRE_METODO`:
```properties
TCLIENTS_FIND_BY_XXX = SELECT id, version, createdAt, updatedAt, entityState, nif, \
    name, surname, street, city, country, postalCode, phone, email FROM TClients WHERE ... = ?
```
(La `\` al final de línea permite partir la sentencia en varias líneas.)
**Importante**: un Record debe ser una copia fiel de la fila → **selecciona TODAS las columnas** que lee el assembler (`id, version, createdAt, updatedAt, entityState` + las propias). Si falta una, `rs.getString("columna")` lanza `SQLException`.

### Paso 2 — Interfaz del Gateway (`persistence/client/ClientGateway.java`)
```java
List<ClientRecord> findByXxx(String xxx) throws PersistenceException;
```

### Paso 3 — Implementación (`persistence/client/impl/ClientGatewayImpl.java`)
Copia uno parecido que ya tengas y cambia la clave y los parámetros:
```java
@Override
public List<ClientRecord> findByXxx(String xxx) {
    try (PreparedStatement pst = Jdbc.getCurrentConnection()
        .prepareStatement(Queries.getSQLSentence("TCLIENTS_FIND_BY_XXX"))) {
        pst.setString(1, xxx);
        try (ResultSet rs = pst.executeQuery()) {
            return assembler.toRecordList(rs);
        }
    } catch (SQLException e) {
        throw new PersistenceException(e);
    }
}
```

### Paso 4 — Interfaz del servicio (solo si te piden una operación nueva)
En `cws_service_interfaces/.../ClientCrudService.java` añade el método con su Javadoc:
```java
List<ClientDto> findClientsByXxx(String xxx) throws BusinessException;
```
> ⚠ En la entrega estaba prohibido modificar las interfaces. En la prueba de autoría, si te piden una operación nueva, normalmente te dirán que la añadas a la interfaz (o te darán la firma). **Pregunta al profesor** si no lo dice claramente.

### Paso 5 — Comando (`service/client/crud/commands/FindClientsByXxx.java`)
```java
public class FindClientsByXxx implements Command<List<ClientDto>> {

    private final String xxx;
    private final ClientGateway clients = Factories.persistence.forClient();

    public FindClientsByXxx(String xxx) {
        ArgumentChecks.isNotBlank(xxx, "El parámetro no puede estar vacío");   // validación EN EL CONSTRUCTOR
        this.xxx = xxx;
    }

    @Override
    public List<ClientDto> execute() throws BusinessException {
        List<ClientDto> result = new ArrayList<>();
        for (ClientRecord record : clients.findByXxx(xxx)) {
            result.add(ClientDtoAssembler.toDto(record));
        }
        return result;
    }
}
```

### Paso 6 — Fachada (`service/client/crud/ClientCrudServiceImpl.java`)
```java
@Override
public List<ClientDto> findClientsByXxx(String xxx) throws BusinessException {
    return executor.execute(new FindClientsByXxx(xxx));
}
```

### Paso 7 — UI: Action + opción de menú
`ui/foreman/client/action/FindClientsByXxxAction.java`:
```java
public class FindClientsByXxxAction implements Action {
    @Override
    public void execute() throws BusinessException, UserInteractionException {
        String xxx = Console.readString("Xxx");
        UserInteractionChecks.isNotEmpty(xxx, "Cannot be empty");
        List<ClientDto> clients = Factories.service.forClientCrudService().findClientsByXxx(xxx);
        Console.println("\nClients\n");
        for (ClientDto client : clients) {
            Printer.printClient(client);
        }
    }
}
```
Y en `ClientMenu` añade `{ "Find clients by xxx", FindClientsByXxxAction.class },`.

### Paso 8 — Comprueba
- ¿Compila sin **warnings** (imports sin usar, variables sin usar)? → `Ctrl+Shift+O` en Eclipse organiza imports.
- Ejecuta el menú y prueba: caso normal, caso vacío, parámetro vacío.
- Sin `System.out.println` de depuración, sin código comentado.

**Orden de dependencias** (si no compila algo, revisa en este orden): queries → Gateway → GatewayImpl → (PersistenceFactory si es un gateway nuevo) → Comando → Interfaz servicio → ServiceImpl → Action → Menu.

---

## 8. Ejemplos resueltos de ampliaciones probables

Todos están escritos para encajar con TU código actual (mismos nombres de clases y campos).

### 8.1 Find sencillo: "Listar los clientes de una ciudad"

**queries.properties** (dentro de `# TCLIENTS`, entre `TCLIENTS_FIND_ALL` y `TCLIENTS_FIND_BY_ID` por orden alfabético):
```properties
TCLIENTS_FIND_BY_CITY = SELECT id, version, createdAt, updatedAt, entityState, nif, \
    name, surname, street, city, country, postalCode, phone, email FROM TClients \
    WHERE city = ? ORDER BY surname, name
```

**ClientGateway**:
```java
List<ClientRecord> findByCity(String city) throws PersistenceException;
```

**ClientGatewayImpl**:
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

**ClientCrudService** (interfaz): `List<ClientDto> findClientsByCity(String city) throws BusinessException;`

**Comando `FindClientsByCity`**, **ClientCrudServiceImpl**, **Action** y **menú**: exactamente como en la receta (sección 7) cambiando `Xxx` por `City`.

### 8.2 Find con agregación: "Clientes que han recomendado a N o más clientes"

Devuelve filas de `TClients` → va en `ClientGateway`. Usamos subconsulta con `GROUP BY` + `HAVING`:

```properties
TCLIENTS_FIND_WITH_MIN_RECOMMENDATIONS = SELECT id, version, createdAt, updatedAt, entityState, \
    nif, name, surname, street, city, country, postalCode, phone, email FROM TClients \
    WHERE id IN (SELECT recommender_id FROM TRecommendations \
    GROUP BY recommender_id HAVING COUNT(*) >= ?) ORDER BY nif
```

```java
// ClientGateway
List<ClientRecord> findWithMinRecommendations(int min) throws PersistenceException;

// ClientGatewayImpl
@Override
public List<ClientRecord> findWithMinRecommendations(int min) {
    try (PreparedStatement pst = Jdbc.getCurrentConnection()
        .prepareStatement(Queries.getSQLSentence("TCLIENTS_FIND_WITH_MIN_RECOMMENDATIONS"))) {
        pst.setInt(1, min);                       // ojo: setInt porque es un número
        try (ResultSet rs = pst.executeQuery()) {
            return assembler.toRecordList(rs);
        }
    } catch (SQLException e) {
        throw new PersistenceException(e);
    }
}
```

Comando: en el constructor `ArgumentChecks.isTrue(min > 0, "El mínimo debe ser positivo");`.

> ¿Por qué no hacer `findAll()` y filtrar en Java? Porque la checklist lo penaliza: *"No implementar solo un método genérico findAll en el gateway para luego filtrar toda la información en la capa de servicio; esto provoca ineficiencia"*. Filtra en el SQL.

> ¿Y si te piden mostrar también **el número** de recomendaciones? Un `ClientRecord` no tiene ese campo y la checklist dice que un Record debe reflejar la tabla. Opción limpia: para cada cliente devuelto, contar en Java las recomendaciones con un find de `RecommendationGateway` (p. ej. `findByRecommenderId(id).size()`). Si te lo piden, coméntaselo así al profesor.

### 8.3 Find en otro gateway: "Bonos de un cliente con saldo disponible"

Devuelve filas de vouchers → `VoucherGateway`.
```properties
# en # TVOUCHERS, orden alfabético: ..._FIND_AVAILABLE_BY_CLIENT_ID va antes de ..._FIND_BY_CLIENT_ID
TVOUCHERS_FIND_AVAILABLE_BY_CLIENT_ID = SELECT pm.id, pm.version, pm.createdAt, pm.updatedAt, \
    pm.entityState, pm.client_id, pm.accumulated, pm.dtype, v.code, v.description, v.available \
    FROM TVouchers v JOIN TPaymentMeans pm ON pm.id = v.id \
    WHERE pm.client_id = ? AND v.available > 0 ORDER BY v.code
```
```java
// VoucherGateway
List<VoucherRecord> findAvailableByClientId(String clientId) throws PersistenceException;
// VoucherGatewayImpl: idéntico a findByClientId cambiando la clave de la query.
```
Comando: igual que `FindVouchersByClientNif` pero llamando a `findAvailableByClientId`.

### 8.4 Cambiar la regla del bono (muy probable y muy fácil)

En `GenerateVouchers`:
- "Que el bono sea por **4** órdenes" → `private static final int WORK_ORDERS_PER_VOUCHER = 4;`
- "Que el bono sea de **25 €**" → en `VoucherDtoAssembler.toRecord` cambia `new BigDecimal("20.00")` por `"25.00"` (y la descripción si te lo piden).
- "Solo cuentan OTs de importe ≥ 50 €": mejor en el SQL de `TWORKORDERS_FIND_NOT_USED_FOR_VOUCHER_BY_INVOICE_STATE` añadiendo `AND w.total_amount >= ?` (y un parámetro más en el método del gateway: `pst.setBigDecimal(2, min)`).
- Mejor práctica: saca el importe y la descripción a constantes en `GenerateVouchers` y pásalas al assembler (ver 8.5), porque **un assembler no debería contener reglas de negocio** (importe del bono = regla de negocio).

### 8.5 Implementar el bono de 30 € por factura > 500 € (caso de otros UOs)

Reglas: factura `PAID`, `total > 500`, `usedForVoucher = FALSE`. Un bono por factura. Se marca la factura como usada. Descripción `"Por factura de más de 500"`. El bono es para **quien la pagó** (cliente del medio de pago del cargo).

**1) queries.properties**
```properties
# TINVOICES (orden alfabético)
TINVOICES_FIND_NOT_USED_FOR_VOUCHER_BY_STATE_AND_TOTAL_OVER = SELECT id, version, entityState, \
    createdAt, updatedAt, number, date, state, usedForVoucher, subtotal, total, vatAmount, vatRate \
    FROM TInvoices WHERE state = ? AND usedForVoucher = FALSE AND total > ? ORDER BY number
TINVOICES_UPDATE = UPDATE TInvoices SET state = ?, usedForVoucher = ?, version = version + 1, \
    updatedAt = ? WHERE id = ? AND version = ?

# TPAYMENTMEANS
TPAYMENTMEANS_FIND_BY_INVOICE_ID = SELECT pm.id, pm.version, pm.createdAt, pm.updatedAt, \
    pm.entityState, pm.client_id, pm.accumulated, pm.dtype FROM TPaymentMeans pm \
    JOIN TCharges ch ON ch.paymentMean_id = pm.id WHERE ch.invoice_id = ?
```

**2) InvoiceGateway + Impl**
```java
// InvoiceGateway
List<InvoiceRecord> findNotUsedForVoucherByStateAndTotalOver(String state, BigDecimal amount)
    throws PersistenceException;

// InvoiceGatewayImpl
@Override
public List<InvoiceRecord> findNotUsedForVoucherByStateAndTotalOver(String state, BigDecimal amount) {
    try (PreparedStatement pst = Jdbc.getCurrentConnection().prepareStatement(
        Queries.getSQLSentence("TINVOICES_FIND_NOT_USED_FOR_VOUCHER_BY_STATE_AND_TOTAL_OVER"))) {
        pst.setString(1, state);
        pst.setBigDecimal(2, amount);
        try (ResultSet rs = pst.executeQuery()) {
            return assembler.toRecordList(rs);
        }
    } catch (SQLException e) {
        throw new PersistenceException(e);
    }
}

@Override
public void update(InvoiceRecord record) {          // antes lanzaba NotYetImplementedException
    try (PreparedStatement pst = Jdbc.getCurrentConnection()
        .prepareStatement(Queries.getSQLSentence("TINVOICES_UPDATE"))) {
        pst.setString(1, record.state);
        pst.setBoolean(2, record.usedForVoucher);
        pst.setTimestamp(3, Timestamp.valueOf(record.updatedAt));
        pst.setString(4, record.id);
        pst.setLong(5, record.version);
        if (pst.executeUpdate() != 1) {
            throw new PersistenceException(
                new SQLException("Invoice was not updated because its version has changed"));
        }
    } catch (SQLException e) {
        throw new PersistenceException(e);
    }
}
```
(Necesitas `import java.math.BigDecimal;` y quitar el import de `NotYetImplementedException` si ya no se usa → si no, warning.)

**3) PaymentMeanGateway + Impl**
```java
List<PaymentMeanRecord> findByInvoiceId(String invoiceId) throws PersistenceException;
// Impl: igual que findByClientId cambiando la clave a "TPAYMENTMEANS_FIND_BY_INVOICE_ID"
```

**4) VoucherDtoAssembler**: parametriza importe y descripción (que vengan del comando):
```java
public static VoucherRecord toRecord(PaymentMeanRecord parent, String description, BigDecimal amount) {
    VoucherRecord record = new VoucherRecord();
    fillCommon(record, parent.clientId, parent.id);
    record.createdAt = parent.createdAt;
    record.updatedAt = parent.updatedAt;
    record.description = description;
    record.available = amount;
    record.code = "V-" + UUID.randomUUID();
    return record;
}
```

**5) GenerateVouchers**
```java
private static final String WORK_ORDER_DESCRIPTION = "Por tres órdenes de trabajo";
private static final BigDecimal WORK_ORDER_AMOUNT = new BigDecimal("20.00");
private static final String INVOICE_DESCRIPTION = "Por factura de más de 500";
private static final BigDecimal INVOICE_AMOUNT = new BigDecimal("30.00");
private static final BigDecimal INVOICE_THRESHOLD = new BigDecimal("500");

private final InvoiceGateway invoices = Factories.persistence.forInvoice();

@Override
public int[] execute() {
    int byWorkOrders = generateForWorkOrders();   // tu bucle actual movido a este método
    int byInvoices = generateForInvoices();
    return new int[] { byWorkOrders, 0, byInvoices };
}

private int generateForInvoices() {
    int generated = 0;
    for (InvoiceRecord invoice
        : invoices.findNotUsedForVoucherByStateAndTotalOver(PAID, INVOICE_THRESHOLD)) {
        List<PaymentMeanRecord> payers = paymentMeans.findByInvoiceId(invoice.id);
        if (payers.isEmpty()) {
            continue;                              // factura sin cargos: nadie a quien premiar
        }
        invoice.usedForVoucher = true;
        invoice.updatedAt = LocalDateTime.now();
        invoices.update(invoice);
        createVoucher(payers.get(0).clientId, INVOICE_DESCRIPTION, INVOICE_AMOUNT);
        generated++;
    }
    return generated;
}

private void createVoucher(String clientId, String description, BigDecimal amount) {
    PaymentMeanRecord parent = VoucherDtoAssembler.toPaymentMeanRecord(clientId);
    paymentMeans.add(parent);
    vouchers.add(VoucherDtoAssembler.toRecord(parent, description, amount));
}
```
Y en tu bucle actual cambia `createVoucher(clientId)` por `createVoucher(clientId, WORK_ORDER_DESCRIPTION, WORK_ORDER_AMOUNT)`. En `GenerateVouchersAction` añade: `Console.printf("\t30 euro vouchers for invoices: %d\n", generated[2]);`.

> Ojo: `total > 500` (estrictamente mayor). Hay un test (`V.GI.2`) que comprueba que una factura de **exactamente** 500 € NO genera bono.

### 8.6 Bono de 25 € por 3 recomendaciones (esquema)

Reglas: el recomendador tiene al menos una OT facturada y pagada; por cada 3 recomendaciones **no usadas** cuyo recomendado tenga una OT facturada y pagada → bono de 25 €; marcar esas recomendaciones como usadas.

SQL clave (en `TRECOMMENDATIONS`): recomendaciones no usadas cuyo recomendado tiene alguna OT pagada, ordenadas por recomendador:
```properties
TRECOMMENDATIONS_FIND_NOT_USED_WITH_PAID_RECOMMENDED = SELECT r.id, r.version, r.createdAt, \
    r.updatedAt, r.entityState, r.usedForVoucher, r.recommender_id, r.recommended_id \
    FROM TRecommendations r WHERE r.usedForVoucher = FALSE AND EXISTS ( \
    SELECT 1 FROM TWorkOrders w JOIN TVehicles v ON v.id = w.vehicle_id \
    JOIN TInvoices i ON i.id = w.invoice_id \
    WHERE v.client_id = r.recommended_id AND i.state = ?) \
    ORDER BY r.recommender_id, r.createdAt
TRECOMMENDATIONS_UPDATE = UPDATE TRecommendations SET usedForVoucher = ?, version = version + 1, \
    updatedAt = ? WHERE id = ? AND version = ?
```
Algoritmo en Java: igual que tu bucle de OTs (lote por `recommenderId`, cada 3 → bono), pero antes de contar a un recomendador comprueba que él mismo tiene una OT pagada (un método específico tipo `WorkOrderGateway.findPaidByClientId(clientId)` con el mismo JOIN). Implementa `RecommendationGateway.update`.

### 8.7 Listado agregado: `getVoucherSummary()` (resumen de bonos por cliente)

Está en tu interfaz `VoucherGenerationService` pero lanza `NotYetImplementedException`. Debe devolver, por cliente con bonos: NIF, nombre, apellidos, nº bonos, total, disponible, consumido, **ordenado por disponible descendente**.

Opción respetando el patrón TDG (cada gateway devuelve filas de su tabla):

```properties
# TCLIENTS
TCLIENTS_FIND_WITH_VOUCHERS = SELECT DISTINCT c.id, c.version, c.createdAt, c.updatedAt, \
    c.entityState, c.nif, c.name, c.surname, c.street, c.city, c.country, c.postalCode, \
    c.phone, c.email FROM TClients c JOIN TPaymentMeans pm ON pm.client_id = c.id \
    JOIN TVouchers v ON v.id = pm.id
```

```java
// ClientGateway: List<ClientRecord> findWithVouchers() throws PersistenceException;

// Comando service/paymentmean/voucher/commands/GetVoucherSummary.java
public class GetVoucherSummary implements Command<List<VoucherSummaryDto>> {

    private final ClientGateway clients = Factories.persistence.forClient();
    private final VoucherGateway vouchers = Factories.persistence.forVoucher();

    @Override
    public List<VoucherSummaryDto> execute() {
        List<VoucherSummaryDto> result = new ArrayList<>();
        for (ClientRecord client : clients.findWithVouchers()) {
            List<VoucherRecord> list = vouchers.findByClientId(client.id);
            BigDecimal available = BigDecimal.ZERO;
            BigDecimal consumed = BigDecimal.ZERO;
            for (VoucherRecord v : list) {
                available = available.add(v.available);
                consumed = consumed.add(v.accumulated);
            }
            result.add(new VoucherSummaryDto(client.nif, client.name, client.surname,
                list.size(), available.add(consumed), available, consumed));
        }
        result.sort(Comparator.comparing(VoucherSummaryDto::availableAmount).reversed());
        return result;
    }
}

// VoucherGenerationServiceImpl
@Override
public List<VoucherSummaryDto> getVoucherSummary() throws BusinessException {
    return executor.execute(new GetVoucherSummary());
}
```
(Total = disponible + consumido, porque al pagar con un bono `available` baja y `accumulated` sube en la misma cantidad.)

### 8.8 Añadir/cambiar reglas de negocio en clientes

**a) "Un cliente no puede recomendar a más de 5 clientes"** (en `AddClient`, tras localizar al recomendador):
```java
// ClientGateway ya tiene findRecommendedBy(nif): reutilízalo
BusinessChecks.isTrue(clients.findRecommendedBy(recommenderNif).size() < 5,
    "El recomendador ya ha recomendado al máximo de clientes");
```

**b) "No se puede eliminar un cliente que haya hecho recomendaciones"** (en `DeleteClientByNif.execute()`, antes de borrar):
```java
BusinessChecks.isTrue(clients.findRecommendedBy(nif).isEmpty(),
    "No se puede eliminar un cliente que ha recomendado a otros");
```

**c) "No se puede eliminar un cliente que tenga bonos con saldo"**:
```java
for (VoucherRecord v : vouchers.findByClientId(client.id)) {
    BusinessChecks.isTrue(v.available.compareTo(BigDecimal.ZERO) == 0,
        "No se puede eliminar un cliente con bonos con saldo disponible");
}
```

**d) "El email debe contener @"** → es validación de **formato** de argumento → en el comando: `ArgumentChecks.isTrue(dto.email.contains("@"), "Email no válido");` (lanza `IllegalArgumentException`). Si te piden comprobarlo también en la UI: `UserInteractionChecks.isTrue(...)` en la `Action`.

Regla para decidir: **argumento mal formado → `ArgumentChecks` (IllegalArgumentException)**; **choca con el estado de la BD (existe/no existe/tiene X) → `BusinessChecks` (BusinessException)**.

### 8.9 Añadir una opción al menú (¡y arreglar algo que te falta!)

Tienes la clase `FindRecommendedClientsAction` hecha, pero **NO está en `ClientMenu`**, así que desde la aplicación no se puede usar. Arreglo (en `ui/foreman/client/ClientMenu.java`):
```java
menuOptions = new Object[][] { { "Foreman > Client management", null },
    { "Register a client", AddClientAction.class },
    { "Update a client", UpdateClientAction.class },
    { "Delete a client", DeleteClientAction.class },
    { "List all clients", ListClientsAction.class },
    { "View client detail", FindClientByNifAction.class },
    { "List clients recommended by a client", FindRecommendedClientsAction.class }, };
```
+ `import uo.ri.cws.application.ui.foreman.client.action.FindRecommendedClientsAction;`

### 8.10 Si la ampliación necesita una tabla/gateway que no tienes (p. ej. `TCharges`)

1. Crea paquete `persistence.charge` con `ChargeGateway extends Gateway<ChargeGateway.ChargeRecord>` y el record con los campos de la tabla.
2. Crea `persistence.charge.impl.ChargeGatewayImpl` y `ChargeRecordAssembler extends BaseRecordAssembler<ChargeRecord>`.
3. Añade `ChargeGateway forCharge();` en `PersistenceFactory` y su `return new ChargeGatewayImpl();` en `PersistenceFactoryImpl`.
4. Añade sección `# TCHARGES` en `queries.properties` (en su sitio alfabético: después de `TCASHES`, antes de `TCLIENTS`).
5. Los métodos que no uses: `throw new NotYetImplementedException();`.

---

## 9. Reglas de la checklist que NO puedes romper

Lo que más penaliza (y lo más fácil de romper con prisas):

1. **Nada de SQL fuera de `queries.properties`**. Nada de `"SELECT ..."` dentro de Java.
2. **Nada de SQL/JDBC fuera de los Gateways** (ni en comandos ni en UI).
3. **Nada de `Console` / `System.out` fuera de la UI**. Nada de `println` de depuración en la entrega.
4. **Nada de lógica de negocio en Gateways, Assemblers ni Fachadas**. El gateway solo hace SQL; el assembler solo copia campos; la fachada solo hace `executor.execute(new X(...))`.
5. **Los comandos obtienen gateways con `Factories.persistence.forXxx()`**, nunca `new XxxGatewayImpl()`.
6. **La UI obtiene servicios con `Factories.service.forXxx()`**, nunca `new XxxServiceImpl()`.
7. **Todo comando implementa `Command<T>` y se ejecuta con `CommandExecutor`**.
8. **Los Gateways usan `Jdbc.getCurrentConnection()`**, nunca crean, cierran ni hacen commit/rollback.
9. **Excepciones**:
   - Gateways: solo lanzan `PersistenceException` (`catch (SQLException e) { throw new PersistenceException(e); }`).
   - Servicio: lanza `BusinessException` / `IllegalArgumentException`; **no captura nada** (salvo `CommandExecutor`).
   - UI: **no hace `throw new BusinessException`** ni captura nada.
   - Nunca `catch (Exception e)` ni `catch (RuntimeException e)` en tu código.
10. **Liberar recursos**: `PreparedStatement` y `ResultSet` siempre en try-with-resources.
11. **Un Record = copia de una fila de SU tabla**. No metas en `InvoiceRecord` el `clientId` porque te venga bien.
12. **No filtrar en Java lo que puede filtrar el SQL** (nada de `findAll` + `if`). Pero tampoco métodos ridículamente específicos para todo (la checklist explica: método específico si solo necesitas un dato puntual; `findById` si la regla de negocio necesita el objeto).
13. **Sin warnings** (imports no usados, variables no usadas, genéricos sin tipo).
14. **`equals` para Strings, `compareTo` para BigDecimal**, campos `private` (salvo en DTOs/Records).
15. **No tocar** `cws_service_interfaces` ni los tests en la entrega (en la prueba, solo si te lo indican).

---

## 10. Cosas de tu código que te pueden preguntar

Revisando tu proyecto he visto estas cosas. Prepárate para explicarlas o arreglarlas si te las señalan:

1. **`FindRecommendedClientsAction` no aparece en `ClientMenu`** → la operación funciona (los tests la usan) pero no es accesible desde el menú. Arreglo en 8.9.

2. **`ClientRecordAssembler.toRecord(rs)` hace `rs.next()` dentro**, a diferencia del resto de assemblers (que leen la fila actual). Por eso existe `toCurrentRecord(rs)` y el gateway usa ese. Si te preguntan: *"`toRecord` avanza el cursor y devuelve la primera fila (o null); `toCurrentRecord` lee la fila en la que ya está el cursor; `toRecordList` recorre todas"*. Es mejorable: lo coherente sería que `toRecord` lea la fila actual como en `BaseRecordAssembler`.

3. **En `AddClient` y `UpdateClient` la validación de campos (`validate(...)`) está en `execute()`, no en el constructor.** La P3 dice que el constructor debe comprobar parámetros y lanzar `IllegalArgumentException`. Funciona igual (la excepción sube a través del `CommandExecutor`, que hace rollback), pero si te lo preguntan: lo correcto según el enunciado es moverlo al constructor (en `UpdateClient` el constructor ya comprueba que el dto no es null).

4. **`GenerateVouchers` hace `vehicles.findById(...).get()` sin comprobar si el Optional está vacío.** Es seguro en la práctica porque `TWorkOrders.VEHICLE_ID` es clave ajena (la OT siempre tiene vehículo), pero `.get()` sobre un Optional vacío lanzaría `NoSuchElementException`.

5. **`GenerateVouchers` devuelve `{generated, 0, 0}`**: porque la interfaz pide los tres contadores (20 €, 25 €, 30 €) y tú solo implementas el primero.

6. **La descripción y el importe del bono están fijos dentro de `VoucherDtoAssembler.toRecord`** ("Por tres órdenes de trabajo", 20.00). Eso es una regla de negocio dentro de un assembler (la checklist dice que el assembler no debe tener lógica). Mejora: pasarlos como parámetros desde el comando (8.5, paso 4).

7. **`FindNotInvoicedWorkOrdersByClientNif` filtra `APPROVED` en Java** (con `if ... continue`) en vez de en el SQL. Se podría añadir `AND w.state = ?` a la consulta.

8. **¿Por qué `DeleteClientByNif` mira facturas con `findByPayerId`?** Porque la regla es "no ha **pagado** facturas", y quien paga una factura se sabe por sus cargos: factura → TCharges → TPaymentMeans → client_id.

9. **¿Por qué `UpdateClient` comprueba la versión?** Control de concurrencia optimista: si el dto trae una versión distinta de la de BD, alguien lo modificó mientras tanto.

10. **Mensajes mezclados español/inglés** (p. ej. `AddMechanic` en inglés, `AddClient` en español). No es grave, pero sé consciente.

---

## 11. Preguntas teóricas típicas con respuesta

**¿Qué es JDBC?** API estándar de Java (paquetes `java.sql` y `javax.sql`) para conectarse a BD relacionales, ejecutar SQL y procesar resultados. Da independencia del SGBD (cada fabricante da un driver que implementa las interfaces) y de la plataforma (Java/JVM).

**¿Qué hace el `DriverManager`?** Mantiene la lista de drivers y, según la URL, elige el adecuado para darte una `Connection`. Desacopla tu código del driver concreto.

**Formato de URL JDBC**: `jdbc:<subprotocolo>:<host>:<puerto>/<bd>;atributos`. En tu proyecto: `jdbc:hsqldb:file:../cws_database/test;files_readonly=true` (HSQLDB en modo fichero, solo lectura: los cambios no se guardan al cerrar). Modo servidor: `jdbc:hsqldb:hsql://localhost`.

**`Statement` vs `PreparedStatement`**: PreparedStatement es precompilado, usa parámetros `?` con `setXxx`, rinde mejor si se repite y evita la inyección SQL.

**`executeQuery` vs `executeUpdate`**: el primero para SELECT (devuelve ResultSet), el segundo para INSERT/UPDATE/DELETE (devuelve nº de filas afectadas).

**¿Cómo funciona el ResultSet?** Cursor que empieza antes de la primera fila; `next()` avanza y devuelve false al final; columnas por nombre o por índice (desde 1). Por defecto es `TYPE_FORWARD_ONLY` y `CONCUR_READ_ONLY`.

**Tipos de ResultSet**: `TYPE_FORWARD_ONLY` (solo hacia delante), `TYPE_SCROLL_INSENSITIVE` (se puede mover, no ve cambios externos), `TYPE_SCROLL_SENSITIVE` (se mueve y ve cambios externos). Concurrencia: `CONCUR_READ_ONLY` / `CONCUR_UPDATABLE` (permite `updateXxx()` + `updateRow()`, `moveToInsertRow()` + `insertRow()`, `deleteRow()`). Limitaciones del actualizable: sin `SELECT *`, una sola tabla sin joins, sin columnas agregadas.

**Fetch size**: cuántas filas trae el driver de golpe del servidor (`rs.setFetchSize(25)`). Los datos no están todos en el ResultSet, se van trayendo.

**NULL en JDBC**: `getInt()` devuelve 0 si es NULL → usa `wasNull()` o tipos envoltorio. Tu `findLastNumber` usa `rs.wasNull()` precisamente para eso.

**¿Por qué cerrar recursos?** ResultSet y Statement ocupan cursores en el SGBD, que tiene límite. Con try-with-resources se cierran siempre, incluso con excepción.

**Transacción**: conjunto de operaciones que se ejecutan como una sola (todo o nada). En JDBC va ligada a la conexión. `setAutoCommit(false)`, `commit()`, `rollback()`.

**DataSource / pool de conexiones**: alternativa a DriverManager; el código no conoce URL ni driver (se obtiene por JNDI). Un pool reutiliza conexiones ya abiertas porque abrir una conexión es caro.

**¿Qué es un Transaction Script?** Un procedimiento por cada operación de negocio que se ejecuta en una sola transacción. ¿Por qué ese nombre? Porque la gestión de la transacción se hace en el propio script (en tu caso, delegada al `CommandExecutor`).

**¿Qué aporta el patrón Command?** Encapsular cada operación en un objeto con `execute()`; así un único invocador (`CommandExecutor`) hace las tareas comunes (abrir conexión, transacción, commit/rollback, cerrar) sin repetir código en cada TS.

**¿Qué es un TDG?** Un objeto por tabla que contiene todo el SQL de esa tabla; la lógica de negocio llama a sus métodos en lugar de acceder a la BD.

**¿Para qué la `PersistenceFactory`?** Para que la capa de servicio no dependa de las clases `Impl` de persistencia (bajo acoplamiento): si cambias JDBC por otra tecnología, solo cambias la factoría.

**¿Para qué `PersistenceException`?** Para que la capa de negocio no dependa de `SQLException` (detalle de JDBC). Es `RuntimeException`, así que no obliga a declararla.

**DTO vs Record**: DTO = objetos de la capa de servicio que se pasan a la UI (`ClientDto`). Record = copia exacta de una fila de BD en la capa de persistencia (`ClientRecord`, incluye `version`, `createdAt`, `updatedAt`, `entityState`). Se convierten con Assemblers.

**¿Por qué no se modifican las interfaces de servicio?** Son el contrato entre cliente (UI/tests) y proveedor. Los tests de corrección copian las originales; si las cambias, no compila.

---

## 12. Cómo ejecutar la aplicación y los tests

- **Base de datos**: `connection.properties` usa modo fichero (`jdbc:hsqldb:file:../cws_database/test;files_readonly=true`) → **no hace falta arrancar servidor**, y los cambios **no se guardan** al cerrar (bien para probar). Si quieres usar el servidor: `cws_database/1.startup.bat` y cambia `DB_URL` a `jdbc:hsqldb:hsql://localhost`.
- **Restaurar la BD** si la estropeas: `cws_database/3.reset.bat` (copia `test.script.bck` sobre `test.script`).
- **Lanzar la app** (Eclipse): botón derecho sobre `cws_services_jdbc/src/uo/ri/cws/application/ui/foreman/MainMenu.java` → *Run As → Java Application*. Para bonos: `ui/manager/MainMenu.java` → "Voucher management".
- **Tests**: `cws_service_tests/test/uo/ri/cws/application/service/RunServiceTests.java` → *Run As → JUnit Test*. Features relevantes: `acceptance/client/*.feature` y `acceptance/voucher/GenerateForWorkOrders.feature`.
- **Warnings**: vista *Problems* de Eclipse; deben ser 0.
- Atajos Eclipse útiles: `Ctrl+Shift+O` (organizar imports), `Ctrl+Shift+F` (formatear), `Ctrl+Clic` sobre un método (ir a su definición), `Ctrl+Alt+H` (quién llama a este método), `Ctrl+Shift+R` (abrir fichero por nombre).

---

## 13. Chuleta final

**Flujo**: `Action` → `Factories.service.forXxx()` → `XxxServiceImpl` → `executor.execute(new Cmd(...))` → `Cmd.execute()` → `Factories.persistence.forYyy()` → `YyyGatewayImpl` → `Queries.getSQLSentence("TYYY_METODO")` + `Jdbc.getCurrentConnection()` → `ResultSet` → `YyyRecordAssembler` → `Record` → `DtoAssembler` → `Dto` → `Action` imprime.

**Receta "nuevo find"** (en este orden):
1. SQL en `queries.properties` (clave `TTABLA_NOMBRE_METODO`, todas las columnas, orden alfabético).
2. Método en la interfaz `XxxGateway` (`throws PersistenceException`).
3. Implementación en `XxxGatewayImpl` (try-with-resources, `setXxx`, `assembler.toRecordList(rs)`, `catch SQLException → PersistenceException`).
4. (Si hace falta) método en la interfaz de servicio.
5. Comando `NombreMetodo implements Command<T>` (validar en constructor con `ArgumentChecks`, reglas en `execute` con `BusinessChecks`, gateways vía `Factories.persistence`).
6. Fachada: `return executor.execute(new NombreMetodo(...));`
7. `Action` + línea en el `Menu`.
8. Sin warnings, sin println, probar.

**Excepciones**: argumento mal → `ArgumentChecks` (IllegalArgument) · regla de negocio → `BusinessChecks` (Business) · BD → `PersistenceException` · UI → `UserInteractionChecks`.

**Joins típicos**:
- OT→cliente: `TWorkOrders w JOIN TVehicles v ON w.vehicle_id=v.id JOIN TClients c ON v.client_id=c.id`
- Factura→pagador: `TInvoices i JOIN TCharges ch ON ch.invoice_id=i.id JOIN TPaymentMeans pm ON pm.id=ch.paymentMean_id`
- Bono→cliente: `TVouchers v JOIN TPaymentMeans pm ON pm.id=v.id`
- Recomendaciones: `TRecommendations r` (`recommender_id` = quien recomienda, `recommended_id` = recomendado)

**Tu caso (UO%3==1)**: `GenerateVouchers` (lotes de 3 OTs pagadas por cliente → bono 20 €, marca `usedForVoucher`) y `service.client.crud` (Add con Cash + recomendación, Update con NIF no repetido y versión, Delete sin vehículos ni facturas pagadas borrando recomendaciones y medios de pago, FindAll, FindByNif, FindRecommendedBy).

¡Mucha suerte! 🍀
