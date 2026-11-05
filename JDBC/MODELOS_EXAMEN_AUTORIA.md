# Modelos de examen: prueba de autoría CWS2026 (JDBC)

**Caso UO % 3 == 1** (bonos por 3 órdenes de trabajo + gestión de clientes)

Este documento complementa a `GUIA_PRUEBA_AUTORIA_CWS2026.md`. Contiene:

- **Parte 0**: cómo leer un enunciado y traducir cada frase a código (léela primero).
- **6 modelos de examen**, ordenados de más fácil a más difícil. Cada uno tiene:
  1. **Enunciado** escrito como lo escribiría un profesor (largo y con letra pequeña, a propósito).
  2. **Cómo leerlo**: frase por frase, qué significa técnicamente.
  3. **Plan de ataque**: qué ficheros se tocan y en qué orden.
  4. **Solución completa** con el código adaptado a TU proyecto.
  5. **Cómo probarlo** con los datos reales de tu BD.
  6. **Trampas** del enunciado.
  7. **Preguntas cortas** (las que suelen hacer mientras te miran el código) con respuesta.

Consejo: **intenta cada modelo tú sola antes de mirar la solución**. Aunque sea solo el SQL y la lista de ficheros. Puedes cronometrarte: un modelo sencillo debería salirte en 30-40 minutos.

> Los enunciados son inventados por mí a partir del estilo de vuestros PDFs (bonos, P3, P4, Javadoc de las interfaces). No son exámenes reales.
>
> En el examen real el "enunciado" serán **tests** (`.feature` + clases `ScenarioXxx`). Lo que aquí está en prosa es lo mismo que te dirían esos tests. Cómo se traduce un test a este tipo de enunciado está en `EXAMEN_CON_TESTS.md`, sección 6. Para practicar con **tests reales**, mira la sección 7 de ese documento.

---

## Índice

- [Parte 0: Cómo leer el enunciado (diccionario de frases)](#parte-0-cómo-leer-el-enunciado)
- [Modelo 1: Búsqueda de clientes por apellido (find con LIKE)](#modelo-1-búsqueda-de-clientes-por-apellido) · dificultad ★☆☆
- [Modelo 2: Clientes propietarios de una marca + refactorización (find con JOIN)](#modelo-2-clientes-propietarios-de-vehículos-de-una-marca) · ★★☆
- [Modelo 3: Bonos por fidelidad con importe variable (modificar regla)](#modelo-3-nueva-política-de-bonos-por-órdenes-de-trabajo) · ★★☆
- [Modelo 4: Ranking de recomendadores (GROUP BY / HAVING)](#modelo-4-ranking-de-recomendadores) · ★★★
- [Modelo 5: Bonos sin estrenar + nueva restricción al borrar](#modelo-5-bonos-sin-estrenar-y-nueva-restricción-de-borrado) · ★★☆
- [Modelo 6: Implementar un servicio nuevo completo: búsqueda de vehículo por matrícula](#modelo-6-servicio-de-vehículos-búsqueda-por-matrícula) · ★★★

---

## Parte 0: Cómo leer el enunciado

### 0.1 Método de lectura (5 minutos que te ahorran 30)

1. **Lee el enunciado entero una vez sin escribir nada.**
2. **Subraya los verbos del sistema**: "el sistema *pedirá*", "*mostrará*", "*comprobará*", "*registrará*", "*marcará*". Cada uno es una pieza de código en un sitio distinto (ver tabla 0.2).
3. **Rodea cada condición**: "*solo si*", "*siempre que*", "*al menos*", "*más de*", "*no podrá*". Cada una es un `WHERE` en el SQL o un `BusinessChecks` en el comando.
4. **Busca qué pasa en los casos raros**: "*si no existe...*", "*si está vacío...*". Decide si es **lista vacía**, **Optional vacío**, **BusinessException** o **IllegalArgumentException**.
5. **Identifica qué tabla devuelve filas** → en ese Gateway va el `find`.
6. **Haz la lista de ficheros** antes de escribir código (casi siempre: queries → Gateway → GatewayImpl → [interfaz servicio] → Comando → ServiceImpl → Action → Menu).
7. **Escribe y prueba el SQL primero.** Si el SQL está bien, el resto es copiar y pegar patrones que ya tienes.

### 0.2 Diccionario: frase del enunciado → qué significa → dónde va

| Frase del enunciado | Qué significa técnicamente | Dónde se programa |
|---|---|---|
| "El sistema **pedirá** el NIF..." | Leer de teclado | UI: `Console.readString("Nif")` en la `Action` |
| "El sistema **mostrará** / listará..." | Imprimir | UI: `Console.printf` / `Printer.printXxx` en la `Action` |
| "Se **añadirá una opción** al menú del jefe de taller" | Nueva línea en el menú | `ui/foreman/.../XxxMenu.java` |
| "El sistema **comprobará** que el cliente existe; en caso contrario **informará del error**" | Regla de negocio que falla → excepción de negocio | Comando: `BusinessChecks.exists(opt, "msg")` |
| "Si no existe, **se devolverá una lista vacía**" / "**no se mostrará nada**" | NO es error. No lances nada | Comando: `if (opt.isEmpty()) return new ArrayList<>();` |
| "Devuelve el cliente **o Optional.empty**" | Devolver `Optional` | Comando: `Optional.empty()` / `Optional.of(dto)` |
| "@throws BusinessException **DOES NOT**" (en el Javadoc) | Ese método NUNCA lanza BusinessException | No uses `BusinessChecks` ahí |
| "@throws IllegalArgumentException si el argumento es **null o vacío**" | Validar parámetros | Constructor del comando: `ArgumentChecks.isNotBlank(x, "msg")` |
| "...si **no cumple el formato**" | Validar formato (regex, longitud...) | Constructor: `ArgumentChecks.isTrue(x.matches("..."), "msg")` |
| "**No podrá** eliminarse si..." | Precondición de negocio | Comando: `BusinessChecks.isTrue(condicion, "msg")` antes de borrar |
| "**Al menos** N" / "**como mínimo**" | `>= N` | SQL o Java |
| "**Más de** N" / "**superior a**" | `> N` (¡estricto!) | SQL o Java |
| "**Hasta** N" / "**como máximo**" | `<= N` | SQL o Java |
| "**Ordenado** de mayor a menor por X" | `ORDER BY x DESC` | SQL (o `list.sort(...)` en Java si X se calcula en Java) |
| "...**que contenga** el texto" / "**parcial**" / "**empiece por**" | `LIKE '%texto%'` / `LIKE 'texto%'` | SQL + el `%` se añade en el GatewayImpl |
| "**Sin distinguir mayúsculas** y minúsculas" | Comparar con `UPPER(...)` | SQL: `UPPER(col) LIKE UPPER(?)` |
| "**Sin repetidos**" / "cada cliente una sola vez" | `DISTINCT` | SQL |
| "**Por cada** cliente, el número de..." | Agrupar | SQL: `GROUP BY` + `COUNT` / `SUM`, o bucle en Java |
| "Que **no tengan** ninguno..." | Anti-join | SQL: `NOT EXISTS (...)` o `LEFT JOIN ... WHERE x.id IS NULL` |
| "**No se podrá volver a utilizar**" / "se **marcará** como usado" | Poner un flag a TRUE y guardarlo | Comando: `record.usedForVoucher = true; gateway.update(record);` |
| "**De forma automática**, el sistema creará..." | Un INSERT más dentro del mismo comando (misma transacción) | Comando |
| "**Todo o nada**" / "si falla algo no debe quedar nada registrado" | Transacción | Ya lo hace `CommandExecutor` si todo está en UN comando |
| "**Respetando la arquitectura**" / "patrones vistos en clase" | Capas + TDG + Command + factorías + queries externalizadas | Todo el proceso de la receta |
| "**No modifiques** la interfaz" | No toques `cws_service_interfaces` | Solo implementación |
| "**Añade a la interfaz** XxxService el método..." | Aquí SÍ se toca la interfaz (te dan la firma) | `cws_service_interfaces/.../XxxService.java` |

### 0.3 Cuatro respuestas posibles ante un "caso raro": cómo decidir

```
¿El dato que me pasan está mal escrito (null, vacío, formato incorrecto, número negativo)?
    └─ SÍ → IllegalArgumentException   (ArgumentChecks, en el CONSTRUCTOR del comando)

¿El dato está bien escrito pero choca con lo que hay en la BD
 (no existe, ya existe, tiene vehículos, factura no pagada...)?
    ├─ y el enunciado dice "informará del error / lanzará / no se permitirá"
    │       → BusinessException   (BusinessChecks, en execute())
    ├─ y el enunciado dice "devolverá lista vacía / no mostrará nada"
    │       → return lista vacía
    └─ y el método devuelve Optional
            → return Optional.empty()
```

---

## Modelo 1: Búsqueda de clientes por apellido

**Dificultad ★☆☆ · Tiempo orientativo: 30 min**

### Enunciado

> **Ampliación: búsqueda flexible de clientes**
>
> En el mostrador del taller es frecuente que el cliente no recuerde, o no lleve encima, su documento de identificación. El jefe de taller necesita entonces localizarlo a partir de sus apellidos, pero tampoco suele conocer con exactitud cómo están escritos en el sistema (un cliente puede figurar como "Ramos Vazquez" y el jefe de taller buscar "vazq").
>
> Se desea incorporar al subsistema de gestión de clientes, cuyo actor es el jefe de taller (Foreman), una nueva operación de **búsqueda de clientes por apellido**, que respete en todo momento la arquitectura en capas y los patrones aplicados en las prácticas.
>
> El sistema pedirá al usuario un fragmento de texto y mostrará todos los clientes cuyos apellidos **contengan** dicho fragmento en cualquier posición, **sin distinguir entre mayúsculas y minúsculas**. De cada cliente se mostrarán los mismos datos que en el listado general de clientes. Los resultados se presentarán **ordenados alfabéticamente por apellidos y, a igualdad de estos, por nombre**.
>
> El fragmento de búsqueda debe tener **al menos dos caracteres** significativos (sin contar espacios iniciales o finales); en caso contrario la operación se considerará invocada con argumentos inválidos. Si ningún cliente satisface el criterio, el sistema mostrará un listado vacío y **no se considerará una situación de error**.
>
> Para ello se añadirá a la interfaz `ClientCrudService` el siguiente método, cuya implementación deberá integrarse en la fachada existente:
>
> ```java
> /**
>  * @param surnamePart fragment of the surname to look for
>  * @return the list of clients whose surname contains the fragment, ignoring case,
>  *         ordered by surname and name. It might be empty.
>  * @throws BusinessException DOES NOT
>  * @throws IllegalArgumentException if the argument is null, blank, or has less
>  *         than 2 characters after trimming
>  */
> List<ClientDto> findClientsBySurname(String surnamePart) throws BusinessException;
> ```
>
> Se añadirá además una nueva opción, "Find clients by surname", al menú de gestión de clientes del jefe de taller.

### Cómo leerlo

| Frase | Traducción |
|---|---|
| "localizarlo a partir de sus apellidos" | Devuelvo **clientes** → el find va en `ClientGateway`. Filtro por la columna `surname` de `TClients` |
| "contengan dicho fragmento **en cualquier posición**" | `LIKE '%texto%'` (el `%` delante y detrás) |
| "**sin distinguir** mayúsculas y minúsculas" | `UPPER(surname) LIKE` + el texto también en mayúsculas |
| "los mismos datos que en el listado general" | Reutiliza `Printer.printClient(...)` |
| "ordenados por apellidos y, a igualdad, por nombre" | `ORDER BY surname, name` |
| "al menos dos caracteres significativos (sin contar espacios)" | `surnamePart.trim().length() >= 2` → si no, `IllegalArgumentException` (constructor) |
| "listado vacío y no se considerará error" | Nada de `BusinessException`: devuelve la lista tal cual, aunque esté vacía |
| "se añadirá a la interfaz..." | Aquí sí se toca `ClientCrudService` |
| "integrarse en la fachada existente" | Método nuevo en `ClientCrudServiceImpl`, sin lógica: solo `executor.execute(...)` |
| "nueva opción en el menú" | Nueva `Action` + línea en `ClientMenu` |

### Plan de ataque (ficheros)

1. `queries.properties` → `TCLIENTS_FIND_BY_SURNAME_CONTAINING`
2. `persistence/client/ClientGateway.java` → `findBySurnameContaining`
3. `persistence/client/impl/ClientGatewayImpl.java`
4. `cws_service_interfaces/.../client/ClientCrudService.java` → `findClientsBySurname`
5. `service/client/crud/commands/FindClientsBySurname.java` (NUEVO)
6. `service/client/crud/ClientCrudServiceImpl.java`
7. `ui/foreman/client/action/FindClientsBySurnameAction.java` (NUEVO)
8. `ui/foreman/client/ClientMenu.java`

### Solución

**1. queries.properties** (en `# TCLIENTS`, después de `TCLIENTS_FIND_BY_NIF` por orden alfabético):
```properties
TCLIENTS_FIND_BY_SURNAME_CONTAINING = SELECT id, version, createdAt, updatedAt, entityState, \
    nif, name, surname, street, city, country, postalCode, phone, email FROM TClients \
    WHERE UPPER(surname) LIKE ? ORDER BY surname, name
```

**2. ClientGateway**:
```java
/**
 * @param text fragment to look for in the surname column (any position, case insensitive)
 * @return the clients whose surname contains the text, ordered by surname and name
 */
List<ClientRecord> findBySurnameContaining(String text) throws PersistenceException;
```

**3. ClientGatewayImpl**:
```java
@Override
public List<ClientRecord> findBySurnameContaining(String text) {
    try (PreparedStatement pst = Jdbc.getCurrentConnection()
        .prepareStatement(Queries.getSQLSentence("TCLIENTS_FIND_BY_SURNAME_CONTAINING"))) {
        pst.setString(1, "%" + text.toUpperCase() + "%");
        try (ResultSet rs = pst.executeQuery()) {
            return assembler.toRecordList(rs);
        }
    } catch (SQLException e) {
        throw new PersistenceException(e);
    }
}
```
> ¿Por qué el `%` va en Java y no en el SQL? Porque el `?` se sustituye por un **valor**, no por texto SQL. Si escribes `LIKE '%?%'`, el `?` queda dentro de una cadena y deja de ser un parámetro. También vale `LIKE '%' || ? || '%'` en el SQL (`||` concatena en SQL), pero es más lioso.

**4. ClientCrudService** (interfaz): pega el método con el Javadoc del enunciado.

**5. Comando `FindClientsBySurname`**:
```java
package uo.ri.cws.application.service.client.crud.commands;

import java.util.ArrayList;
import java.util.List;

import uo.ri.conf.Factories;
import uo.ri.cws.application.persistence.client.ClientGateway;
import uo.ri.cws.application.persistence.client.ClientGateway.ClientRecord;
import uo.ri.cws.application.persistence.util.command.Command;
import uo.ri.cws.application.service.client.ClientCrudService.ClientDto;
import uo.ri.cws.application.service.client.crud.ClientDtoAssembler;
import uo.ri.util.assertion.ArgumentChecks;

public class FindClientsBySurname implements Command<List<ClientDto>> {

    private static final int MIN_LENGTH = 2;

    private final String surnamePart;
    private final ClientGateway clients = Factories.persistence.forClient();

    public FindClientsBySurname(String surnamePart) {
        ArgumentChecks.isNotBlank(surnamePart, "El texto de búsqueda no puede estar vacío");
        ArgumentChecks.isTrue(surnamePart.trim().length() >= MIN_LENGTH,
            "El texto de búsqueda debe tener al menos dos caracteres");
        this.surnamePart = surnamePart.trim();
    }

    @Override
    public List<ClientDto> execute() {
        List<ClientDto> result = new ArrayList<>();
        for (ClientRecord record : clients.findBySurnameContaining(surnamePart)) {
            result.add(ClientDtoAssembler.toDto(record));
        }
        return result;
    }
}
```
> El orden importa: primero `isNotBlank` y después `trim().length()`. Si el argumento fuera `null`, llamar a `.trim()` lanzaría `NullPointerException` en vez de `IllegalArgumentException`.

**6. ClientCrudServiceImpl**:
```java
@Override
public List<ClientDto> findClientsBySurname(String surnamePart) throws BusinessException {
    return executor.execute(new FindClientsBySurname(surnamePart));
}
```
(+ `import ...commands.FindClientsBySurname;`)

**7. FindClientsBySurnameAction**:
```java
package uo.ri.cws.application.ui.foreman.client.action;

import java.util.List;

import uo.ri.conf.Factories;
import uo.ri.cws.application.service.client.ClientCrudService.ClientDto;
import uo.ri.cws.application.ui.util.Printer;
import uo.ri.util.console.Console;
import uo.ri.util.exception.BusinessException;
import uo.ri.util.exception.UserInteractionChecks;
import uo.ri.util.exception.UserInteractionException;
import uo.ri.util.menu.Action;

public class FindClientsBySurnameAction implements Action {

    @Override
    public void execute() throws BusinessException, UserInteractionException {
        String text = Console.readString("Surname (or part of it)");
        UserInteractionChecks.isNotEmpty(text, "The text cannot be empty");

        List<ClientDto> clients = Factories.service.forClientCrudService().findClientsBySurname(text);

        Console.println("\nClients found: " + clients.size() + "\n");
        for (ClientDto client : clients) {
            Printer.printClient(client);
        }
    }
}
```

**8. ClientMenu**: añade `{ "Find clients by surname", FindClientsBySurnameAction.class },` + su import.

### Cómo probarlo
En tu BD hay clientes como "Ramos Vazquez" y "Hernandez Ruiz". Prueba: `vazq` (debe salir), `RAMOS` (debe salir), `a` (error de argumento: menos de 2 caracteres), `zzzz` (lista vacía, sin error).

### Trampas
- Filtrar en Java con `findAll()` + `contains(...)` → penaliza ("filtrar en service lo que puede filtrar el SQL").
- Poner `BusinessException` si no hay resultados → el enunciado dice explícitamente que no es error.
- Olvidar `UPPER` en uno de los dos lados → "vazq" no encontraría "Vazquez".
- Dejar el import de algo que no usas → warning.

### Preguntas cortas
1. **¿Por qué la validación de longitud está en el constructor y no en `execute()`?** Porque es una validación de argumento (formato), no de negocio. La P3 dice que el constructor comprueba los parámetros y lanza `IllegalArgumentException`. Así falla antes de abrir conexión.
2. **¿Por qué el método va en `ClientGateway` y no en otro?** Porque devuelve filas de `TClients`.
3. **¿Qué pasaría si concatenases el texto en el SQL (`"... LIKE '%" + text + "%'"`)?** Sería vulnerable a **inyección SQL** (el usuario podría meter SQL dentro del texto) y además rompería la norma de tener el SQL externalizado.
4. **¿Dónde se abre y cierra la conexión en esta operación?** En `CommandExecutor.execute()` (`Jdbc.createThreadConnection()` y el try-with-resources). El gateway solo la recupera con `Jdbc.getCurrentConnection()`.

---

## Modelo 2: Clientes propietarios de vehículos de una marca

**Dificultad ★★☆ · Tiempo orientativo: 40 min**

### Enunciado

> **Ampliación: campañas por fabricante y revisión de consultas**
>
> El taller ha firmado un acuerdo con varios fabricantes para realizar campañas de revisión gratuitas. Cuando un fabricante anuncia una campaña, el jefe de taller necesita conocer qué clientes del taller son propietarios de, al menos, un vehículo de dicha marca, con el fin de contactar con ellos.
>
> **Parte A.** Se implementará una operación que, dada una marca, devuelva la relación de clientes que poseen **uno o más** vehículos de esa marca. Un mismo cliente **no debe aparecer repetido** en el resultado aunque posea varios vehículos de la marca indicada. La comparación de la marca **no debe distinguir mayúsculas de minúsculas** ("seat", "SEAT" y "Seat" se consideran la misma marca), aunque, a diferencia de la búsqueda por apellidos, la marca debe coincidir **completa**, no parcialmente. El listado se ordenará por NIF.
>
> Si no hay ningún cliente con vehículos de esa marca, o la marca no existe en el sistema, la operación devolverá una lista vacía. La marca es un argumento obligatorio. La firma, que se añadirá a `ClientCrudService`, será:
>
> ```java
> /**
>  * @param make the vehicle make
>  * @return clients owning at least one vehicle of that make, without duplicates,
>  *         ordered by nif; empty list if none
>  * @throws BusinessException DOES NOT
>  * @throws IllegalArgumentException if make is null or blank
>  */
> List<ClientDto> findClientsByVehicleMake(String make) throws BusinessException;
> ```
>
> Se añadirá la opción "Find clients by vehicle make" al menú de clientes del jefe de taller.
>
> **Parte B.** Durante la revisión del código entregado se ha detectado que el caso de uso que obtiene las órdenes de trabajo pendientes de facturar de un cliente **recupera de la base de datos órdenes en cualquier estado y posteriormente descarta en la capa de servicio** aquellas que no están aprobadas. Este planteamiento es contrario a los criterios de diseño de la asignatura. Refactoriza dicha operación de modo que el filtrado por estado se realice donde corresponde, sin alterar el comportamiento observable del caso de uso.

### Cómo leerlo

| Frase | Traducción |
|---|---|
| "relación de **clientes**" | Devuelvo filas de `TClients` → `ClientGateway` |
| "que poseen uno o más vehículos de esa marca" | JOIN `TClients` ↔ `TVehicles` por `client_id`, filtro `make` |
| "no debe aparecer **repetido**" | `DISTINCT` (un JOIN repite el cliente por cada vehículo) |
| "no distinguir mayúsculas" + "coincidir **completa**" | `UPPER(v.make) = UPPER(?)` → `=` y no `LIKE` |
| "ordenará por NIF" | `ORDER BY c.nif` |
| "lista vacía" / "DOES NOT" | No lances BusinessException |
| Parte B: "recupera en cualquier estado y descarta en la capa de servicio" | Se refiere a tu `FindNotInvoicedWorkOrdersByClientNif`, que tiene un `if (!"APPROVED".equals(...)) continue;` |
| "donde corresponde" | En el **SQL** (capa de persistencia) |
| "sin alterar el comportamiento observable" | El resultado debe ser idéntico: solo OTs APPROVED y sin factura |

### Plan de ataque
- **A**: queries → `ClientGateway.findByVehicleMake` → Impl → interfaz `ClientCrudService` → comando `FindClientsByVehicleMake` → fachada → Action → menú.
- **B**: queries (modificar `TWORKORDERS_FIND_NOT_INVOICED_WORK_ORDERS_BY_CLIENT_NIF`) → `WorkOrderGateway` (añadir parámetro de estado) → Impl → comando `FindNotInvoicedWorkOrdersByClientNif` (quitar el `if`).

### Solución, parte A

```properties
# TCLIENTS (orden alfabético: FIND_BY_ID < FIND_BY_NIF < FIND_BY_VEHICLE_MAKE)
TCLIENTS_FIND_BY_VEHICLE_MAKE = SELECT DISTINCT c.id, c.version, c.createdAt, c.updatedAt, \
    c.entityState, c.nif, c.name, c.surname, c.street, c.city, c.country, c.postalCode, \
    c.phone, c.email FROM TClients c JOIN TVehicles v ON v.client_id = c.id \
    WHERE UPPER(v.make) = UPPER(?) ORDER BY c.nif
```

```java
// ClientGateway
List<ClientRecord> findByVehicleMake(String make) throws PersistenceException;

// ClientGatewayImpl
@Override
public List<ClientRecord> findByVehicleMake(String make) {
    try (PreparedStatement pst = Jdbc.getCurrentConnection()
        .prepareStatement(Queries.getSQLSentence("TCLIENTS_FIND_BY_VEHICLE_MAKE"))) {
        pst.setString(1, make);
        try (ResultSet rs = pst.executeQuery()) {
            return assembler.toRecordList(rs);
        }
    } catch (SQLException e) {
        throw new PersistenceException(e);
    }
}
```

```java
// service/client/crud/commands/FindClientsByVehicleMake.java
public class FindClientsByVehicleMake implements Command<List<ClientDto>> {

    private final String make;
    private final ClientGateway clients = Factories.persistence.forClient();

    public FindClientsByVehicleMake(String make) {
        ArgumentChecks.isNotBlank(make, "La marca no puede estar vacía");
        this.make = make.trim();
    }

    @Override
    public List<ClientDto> execute() {
        List<ClientDto> result = new ArrayList<>();
        for (ClientRecord record : clients.findByVehicleMake(make)) {
            result.add(ClientDtoAssembler.toDto(record));
        }
        return result;
    }
}

// ClientCrudServiceImpl
@Override
public List<ClientDto> findClientsByVehicleMake(String make) throws BusinessException {
    return executor.execute(new FindClientsByVehicleMake(make));
}
```

La Action y el menú son iguales que en el Modelo 1 (cambia el texto que se pide y el método que se llama).

### Solución, parte B

Antes (tu código actual):
```java
for (WorkOrderRecord record : records) {
    if (!"APPROVED".equals(record.state)) {
        continue;
    }
    workOrders.add(InvoiceDtoAssembler.toInvoicingWorkOrderDto(record));
}
```

Después:

```properties
# TWORKORDERS: se cambia el nombre de la clave porque cambia el método
TWORKORDERS_FIND_NOT_INVOICED_BY_CLIENT_NIF_AND_STATE = SELECT w.id, w.version, w.entityState, \
    w.createdAt, w.updatedAt, w.vehicle_id, w.description, w.date, w.total_amount, \
    w.state, w.usedForVoucher, w.mechanic_id, w.invoice_id FROM TWorkOrders w \
    JOIN TVehicles v ON w.vehicle_id = v.id \
    JOIN TClients c ON v.client_id = c.id \
    WHERE w.invoice_id IS NULL AND c.nif = ? AND w.state = ?
```
(y **borra** la antigua `TWORKORDERS_FIND_NOT_INVOICED_WORK_ORDERS_BY_CLIENT_NIF`: nada de código muerto).

```java
// WorkOrderGateway: sustituye el método antiguo por
List<WorkOrderRecord> findNotInvoicedByClientNifAndState(String nif, String state)
    throws PersistenceException;

// WorkOrderGatewayImpl: como tiene 2 parámetros, no vale el helper findMany(key, param)
@Override
public List<WorkOrderRecord> findNotInvoicedByClientNifAndState(String nif, String state) {
    try (PreparedStatement pst = Jdbc.getCurrentConnection().prepareStatement(
        Queries.getSQLSentence("TWORKORDERS_FIND_NOT_INVOICED_BY_CLIENT_NIF_AND_STATE"))) {
        pst.setString(1, nif);
        pst.setString(2, state);
        try (ResultSet rs = pst.executeQuery()) {
            return assembler.toRecordList(rs);
        }
    } catch (SQLException e) {
        throw new PersistenceException(e);
    }
}

// FindNotInvoicedWorkOrdersByClientNif.execute()
private static final String APPROVED = "APPROVED";
...
@Override
public List<InvoicingWorkOrderDto> execute() throws BusinessException {
    List<InvoicingWorkOrderDto> workOrders = new ArrayList<>();
    for (WorkOrderRecord record : workOrderGateway.findNotInvoicedByClientNifAndState(nif, APPROVED)) {
        workOrders.add(InvoiceDtoAssembler.toInvoicingWorkOrderDto(record));
    }
    return workOrders;
}
```

> ¿Por qué pasar `"APPROVED"` como parámetro y no escribirlo en el SQL? Las dos cosas funcionan. Pasarlo como parámetro deja la **decisión de negocio** ("solo se facturan las aprobadas") en la capa de servicio y la consulta queda reutilizable para otros estados. La checklist penaliza la "lógica de negocio en las consultas".

### Cómo probarlo
En tu BD hay marcas como `BMW`, `VOLVO`, `CHEVROLET`. Prueba `bmw` y `Volvo` (deben salir clientes), una marca inventada (lista vacía) y un texto vacío (error). Para la parte B, ejecuta los tests de `FindNotInvoicedWorkOrdersByClientNif`: deben seguir pasando.

### Trampas
- Sin `DISTINCT`, un cliente con 2 BMW sale dos veces.
- Usar `LIKE` en vez de `=`: "BM" encontraría "BMW", y el enunciado pide coincidencia completa.
- En la parte B, dejar el método y la query antiguos sin usar → código muerto.
- Cambiar el comportamiento: olvidar `w.invoice_id IS NULL` o el estado.

### Preguntas cortas
1. **¿Por qué es correcto que `ClientGateway` consulte `TVehicles`?** La checklist dice que un gateway de la tabla A puede consultar otras tablas **para decidir qué filas de A devuelve**. Aquí sigo devolviendo solo columnas de `TClients`.
2. **¿Qué diferencia hay entre JOIN y LEFT JOIN aquí?** Con LEFT JOIN saldrían también clientes sin vehículos (con columnas de vehículo a NULL), pero el filtro `WHERE UPPER(v.make) = ...` los eliminaría igualmente. Lo correcto y claro es `JOIN`.
3. **¿Por qué la parte B era un error de diseño?** Traer de la BD filas que no se van a usar es ineficiente, y el filtrado de datos es responsabilidad de la consulta. La checklist: *"no implementar solo un método genérico... para luego filtrar en la capa de servicio"*.
4. **¿Has cambiado alguna interfaz de servicio en la parte B?** No. Solo el gateway (persistencia, que es mío) y la implementación del comando. El contrato `InvoicingService` sigue igual.

---

## Modelo 3: Nueva política de bonos por órdenes de trabajo

**Dificultad ★★☆ · Tiempo orientativo: 40 min**

### Enunciado

> **Ampliación: revisión de la política de fidelización**
>
> Tras varios meses de funcionamiento del programa de fidelización, la gerencia ha observado que numerosos clientes acumulan bonos a partir de órdenes de trabajo de importe muy reducido (cambios de escobillas, rellenado de líquidos, etc.), lo que desvirtúa el objetivo del programa. Al mismo tiempo, desea premiar de forma especial a los clientes cuyas reparaciones son de mayor cuantía.
>
> Se pide modificar el proceso de **generación automática de bonos por órdenes de trabajo** para que se ajuste a las siguientes reglas, que **sustituyen** a las anteriores en lo que se refiere a este tipo de bono:
>
> 1. Solo serán computables las órdenes de trabajo facturadas, con factura pagada, que no hayan sido utilizadas previamente para generar un bono y cuyo importe sea **igual o superior a 50 €**. Las órdenes que no alcancen dicho importe **no se tendrán en cuenta** a efectos del cómputo ni se marcarán como utilizadas.
> 2. Se seguirá emitiendo un bono por cada grupo de **tres** órdenes computables de un mismo cliente.
> 3. El valor del bono dependerá del importe conjunto de las tres órdenes que lo originan: si la suma de sus importes es **superior a 600 €**, el bono tendrá un valor de **40 €** y la descripción "Por tres órdenes de trabajo de alto importe"; en otro caso, el bono mantendrá su valor de **20 €** y su descripción habitual, "Por tres órdenes de trabajo".
> 4. El valor devuelto por la operación `generateVouchers()` no se modifica: la primera posición seguirá conteniendo el número **total** de bonos generados por órdenes de trabajo, con independencia de su importe.
> 5. La opción de menú correspondiente deberá informar, además, de cuántos de los bonos generados son de 40 €. Dado que la interfaz del servicio no puede modificarse, esta información **se obtendrá en la capa de presentación** a partir de las operaciones ya disponibles en el servicio de bonos... **o, si ello no fuera posible, se omitirá justificadamente**.
>
> Se valorará especialmente que los importes y umbrales de la política no queden dispersos por el código, y que ninguna de las clases auxiliares de transformación de datos asuma responsabilidades que no le corresponden.

### Cómo leerlo

| Frase | Traducción |
|---|---|
| "sustituyen a las anteriores" | Modificas `GenerateVouchers`, no creas otro comando |
| "importe igual o superior a 50 €" | `w.total_amount >= ?` en el SQL (con parámetro 50) |
| "no se tendrán en cuenta ni se marcarán" | Si las filtras en el SQL, ni siquiera llegan al bucle → no se marcan. Bien |
| "un bono por cada grupo de tres... de un mismo cliente" | Tu bucle actual de lotes no cambia |
| "suma... **superior** a 600" | `suma.compareTo(600) > 0` (estricto: 600 justos → 20 €) |
| "40 € y descripción X; en otro caso 20 € y descripción Y" | El importe y la descripción dejan de ser fijos → hay que pasarlos al assembler |
| "valor devuelto no se modifica" | Sigue siendo `{total, 0, 0}` |
| Punto 5 | **Trampa de lectura**: el servicio no tiene ninguna operación que diga "cuántos de 40 €". Se podría consultar los bonos cliente a cliente, pero la UI no sabe qué clientes han recibido bono. Lo honesto es **omitirlo justificándolo** (el enunciado lo permite). Contarlo en la UI sumando cosas sería poner lógica de negocio en la presentación |
| "importes y umbrales no dispersos" | Constantes `private static final` en un único sitio (el comando) |
| "ninguna clase auxiliar de transformación asuma responsabilidades..." | **El assembler no debe decidir el importe ni la descripción** → `VoucherDtoAssembler.toRecord(parent, description, amount)` |

### Plan de ataque
1. `queries.properties`: nueva consulta de OTs con importe mínimo (y borrar la vieja si deja de usarse).
2. `WorkOrderGateway` + Impl: método con dos parámetros.
3. `VoucherDtoAssembler.toRecord(...)`: recibe descripción e importe.
4. `GenerateVouchers`: constantes, filtro, suma del lote, importe según suma.
5. `GenerateVouchersAction`: nada (o un comentario al profesor explicando por qué no se muestran los de 40 €).

### Solución

**queries.properties** (sustituye la antigua `TWORKORDERS_FIND_NOT_USED_FOR_VOUCHER_BY_INVOICE_STATE`):
```properties
TWORKORDERS_FIND_NOT_USED_FOR_VOUCHER_BY_INVOICE_STATE_AND_MIN_AMOUNT = SELECT w.id, w.version, \
    w.entityState, w.createdAt, w.updatedAt, w.vehicle_id, w.description, w.date, w.total_amount, \
    w.state, w.usedForVoucher, w.mechanic_id, w.invoice_id FROM TWorkOrders w \
    JOIN TInvoices i ON i.id = w.invoice_id \
    JOIN TVehicles v ON v.id = w.vehicle_id \
    WHERE i.state = ? AND w.usedForVoucher = FALSE AND w.total_amount >= ? \
    ORDER BY v.client_id, w.date, w.id
```

**WorkOrderGateway** (sustituye el método antiguo):
```java
List<WorkOrderRecord> findNotUsedForVoucherByInvoiceStateAndMinAmount(String invoiceState,
    BigDecimal minAmount) throws PersistenceException;
```

**WorkOrderGatewayImpl**:
```java
@Override
public List<WorkOrderRecord> findNotUsedForVoucherByInvoiceStateAndMinAmount(String invoiceState,
    BigDecimal minAmount) {
    try (PreparedStatement pst = Jdbc.getCurrentConnection().prepareStatement(Queries.getSQLSentence(
        "TWORKORDERS_FIND_NOT_USED_FOR_VOUCHER_BY_INVOICE_STATE_AND_MIN_AMOUNT"))) {
        pst.setString(1, invoiceState);
        pst.setBigDecimal(2, minAmount);
        try (ResultSet rs = pst.executeQuery()) {
            return assembler.toRecordList(rs);
        }
    } catch (SQLException e) {
        throw new PersistenceException(e);
    }
}
```
(Necesitas `import java.math.BigDecimal;` en la interfaz y en la Impl; en la interfaz ya está.)

**VoucherDtoAssembler** (solo copia datos, no decide nada):
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

**GenerateVouchers** (versión completa):
```java
public class GenerateVouchers implements Command<int[]> {

    private static final String PAID = "PAID";
    private static final int WORK_ORDERS_PER_VOUCHER = 3;
    private static final BigDecimal MIN_WORK_ORDER_AMOUNT = new BigDecimal("50");
    private static final BigDecimal HIGH_AMOUNT_THRESHOLD = new BigDecimal("600");
    private static final BigDecimal STANDARD_VOUCHER = new BigDecimal("20.00");
    private static final BigDecimal HIGH_VOUCHER = new BigDecimal("40.00");
    private static final String STANDARD_DESCRIPTION = "Por tres órdenes de trabajo";
    private static final String HIGH_DESCRIPTION = "Por tres órdenes de trabajo de alto importe";

    private final WorkOrderGateway workOrders = Factories.persistence.forWorkOrder();
    private final VehicleGateway vehicles = Factories.persistence.forVehicle();
    private final VoucherGateway vouchers = Factories.persistence.forVoucher();
    private final PaymentMeanGateway paymentMeans = Factories.persistence.forPaymentMean();

    @Override
    public int[] execute() {
        int generated = 0;
        String currentClientId = null;
        List<WorkOrderRecord> batch = new ArrayList<>();

        for (WorkOrderRecord workOrder
            : workOrders.findNotUsedForVoucherByInvoiceStateAndMinAmount(PAID, MIN_WORK_ORDER_AMOUNT)) {
            String clientId = vehicles.findById(workOrder.vehicleId).get().clientId;
            if (!clientId.equals(currentClientId)) {
                currentClientId = clientId;
                batch.clear();
            }
            batch.add(workOrder);
            if (batch.size() == WORK_ORDERS_PER_VOUCHER) {
                markAsUsed(batch);
                createVoucher(clientId, sumOf(batch));
                generated++;
                batch.clear();
            }
        }
        return new int[] { generated, 0, 0 };
    }

    private BigDecimal sumOf(List<WorkOrderRecord> batch) {
        BigDecimal total = BigDecimal.ZERO;
        for (WorkOrderRecord workOrder : batch) {
            total = total.add(workOrder.amount);
        }
        return total;
    }

    private void markAsUsed(List<WorkOrderRecord> batch) {
        for (WorkOrderRecord workOrder : batch) {
            workOrder.usedForVoucher = true;
            workOrder.updatedAt = LocalDateTime.now();
            workOrders.update(workOrder);
        }
    }

    private void createVoucher(String clientId, BigDecimal batchAmount) {
        boolean high = batchAmount.compareTo(HIGH_AMOUNT_THRESHOLD) > 0;
        PaymentMeanRecord parent = VoucherDtoAssembler.toPaymentMeanRecord(clientId);
        paymentMeans.add(parent);
        vouchers.add(VoucherDtoAssembler.toRecord(parent,
            high ? HIGH_DESCRIPTION : STANDARD_DESCRIPTION,
            high ? HIGH_VOUCHER : STANDARD_VOUCHER));
    }
}
```
(`import java.math.BigDecimal;`.) La expresión `condicion ? a : b` es el operador ternario: "si condición, a; si no, b".

**Punto 5 (justificación que darías)**: *"La interfaz `VoucherGenerationService` solo devuelve el array de contadores {20, 25, 30} y no puedo modificarla. Para saber cuántos son de 40 € la UI tendría que consultar los bonos de todos los clientes y deducirlo, que es lógica de negocio en la presentación. Por eso lo omito, como permite el enunciado."*

### Cómo probarlo
Con la BD real es difícil saber cuántos bonos salen. Lo práctico es ejecutar `GenerateVouchers` desde el menú del Manager antes y después del cambio y comparar. Ten en cuenta que **los tests `GenerateForWorkOrders.feature` usan OTs de 100 €** (3 × 100 = 300 → bono de 20 €). Pasarán si la descripción estándar no cambia. Un test que comprobara bonos con OTs de menos de 50 € fallaría, y eso es lo esperado: has cambiado la regla.

### Trampas
- "Superior a 600" es `> 0` en `compareTo`. Con 600 justos → 20 €.
- Filtrar las OTs < 50 € en Java en vez de en el SQL. Funciona, pero en ese caso cuidado: **no deben marcarse**.
- Dejar `"20.00"` y la descripción dentro del assembler → justo lo que el enunciado dice que se valora negativamente.
- `workOrder.amount` es `BigDecimal`: nunca `+`, siempre `.add(...)`.

### Preguntas cortas
1. **Si falla el `INSERT` del tercer bono, ¿qué pasa con los dos primeros?** Nada se guarda: todo el `execute()` es una única transacción y el `CommandExecutor` hace `rollback`.
2. **¿Por qué la consulta va ordenada por `client_id`?** Porque el algoritmo agrupa OTs consecutivas del mismo cliente. Si no estuvieran ordenadas, el lote se vaciaría al cambiar de cliente y se perderían grupos.
3. **¿Por qué `compareTo` y no `>`?** Porque `BigDecimal` es un objeto: Java no permite `>` entre objetos, y `equals` además compara la escala (`20.0` no es `equals` a `20.00`).
4. **¿Qué es `usedForVoucher` y por qué se actualiza con `update` completo?** Es la marca para no reutilizar una OT en otro bono. Se usa el `update` general del gateway porque la checklist dice que cada TDG tiene un único método de modificación (nada de `markAsUsed` en el gateway).

---

## Modelo 4: Ranking de recomendadores

**Dificultad ★★★ · Tiempo orientativo: 50 min**

### Enunciado

> **Ampliación: ranking de recomendadores**
>
> La dirección del taller quiere reconocer públicamente a los clientes que más contribuyen a la captación de nuevos clientes. Para ello, el jefe de taller dispondrá de un listado de "mejores recomendadores".
>
> La operación recibirá un número entero positivo `min` y devolverá los clientes que hayan recomendado el taller **a `min` o más clientes**. De cada uno de ellos se mostrará su NIF, nombre, apellidos y **el número de clientes que ha recomendado**. El listado se presentará **ordenado de mayor a menor número de recomendaciones**; a igualdad de recomendaciones, por NIF ascendente.
>
> A efectos de este listado, **todas** las recomendaciones registradas cuentan, con independencia de que ya se hayan utilizado o no para la emisión de bonos.
>
> Se añadirá a `ClientCrudService` el tipo y la operación siguientes:
>
> ```java
> public record RecommenderDto(String nif, String name, String surname, int recommendations) {}
>
> /**
>  * @param min minimum number of recommendations
>  * @return the clients who have recommended at least min clients, with the number of
>  *         recommendations, ordered by that number descending and then by nif.
>  *         Might be empty.
>  * @throws BusinessException DOES NOT
>  * @throws IllegalArgumentException if min is less than 1
>  */
> List<RecommenderDto> findTopRecommenders(int min) throws BusinessException;
> ```
>
> Debe tenerse en cuenta que los objetos de transferencia de la capa de persistencia han de seguir siendo una representación fiel de las tablas de la base de datos.
>
> **Variante (si te sobra tiempo)**: solo deben contar las recomendaciones cuyo cliente recomendado tenga al menos una factura pagada.

### Cómo leerlo

| Frase | Traducción |
|---|---|
| "recomendado el taller a min **o más** clientes" | `HAVING COUNT(*) >= ?` |
| "el número de clientes que ha recomendado" | Un dato **calculado** que no es columna de `TClients` |
| "ordenado de mayor a menor... a igualdad, por NIF" | `ORDER BY total DESC, nif ASC` |
| "**todas** cuentan" | No filtres por `usedForVoucher` |
| `record RecommenderDto(...)` | DTO nuevo en la interfaz: se crea con `new RecommenderDto(nif, name, surname, n)` |
| "los objetos de transferencia de persistencia han de seguir siendo fieles a las tablas" | **No** añadas `recommendations` a `ClientRecord`. El recuento se obtiene de otra forma |
| "IllegalArgumentException if min < 1" | `ArgumentChecks.isTrue(min >= 1, ...)` en el constructor |

**La dificultad**: necesito un número calculado (el recuento), pero el Record debe ser fiel a la tabla. Solución en dos pasos:
1. `ClientGateway` devuelve los clientes que cumplen la condición, **ya ordenados** (el recuento se usa en el SQL pero no se devuelve).
2. Para cada uno, el comando obtiene el número con un método que ya tienes: `clients.findRecommendedBy(nif).size()`.

### Solución

**queries.properties**: JOIN con una **tabla derivada** (una subconsulta en el `FROM` que actúa como tabla):
```properties
TCLIENTS_FIND_RECOMMENDERS_WITH_AT_LEAST = SELECT c.id, c.version, c.createdAt, c.updatedAt, \
    c.entityState, c.nif, c.name, c.surname, c.street, c.city, c.country, c.postalCode, \
    c.phone, c.email FROM TClients c \
    JOIN (SELECT recommender_id, COUNT(*) AS total FROM TRecommendations \
          GROUP BY recommender_id HAVING COUNT(*) >= ?) r ON r.recommender_id = c.id \
    ORDER BY r.total DESC, c.nif
```

Lee la subconsulta sola primero:
```sql
SELECT recommender_id, COUNT(*) AS total
FROM TRecommendations
GROUP BY recommender_id          -- un grupo por recomendador
HAVING COUNT(*) >= ?             -- solo los grupos con al menos min filas
```
Da una "tabla" con dos columnas (`recommender_id`, `total`) a la que llamo `r`, y la uno con `TClients` como si fuera una tabla normal.

```java
// ClientGateway
List<ClientRecord> findRecommendersWithAtLeast(int min) throws PersistenceException;

// ClientGatewayImpl
@Override
public List<ClientRecord> findRecommendersWithAtLeast(int min) {
    try (PreparedStatement pst = Jdbc.getCurrentConnection()
        .prepareStatement(Queries.getSQLSentence("TCLIENTS_FIND_RECOMMENDERS_WITH_AT_LEAST"))) {
        pst.setInt(1, min);
        try (ResultSet rs = pst.executeQuery()) {
            return assembler.toRecordList(rs);
        }
    } catch (SQLException e) {
        throw new PersistenceException(e);
    }
}
```

```java
// service/client/crud/commands/FindTopRecommenders.java
package uo.ri.cws.application.service.client.crud.commands;

import java.util.ArrayList;
import java.util.List;

import uo.ri.conf.Factories;
import uo.ri.cws.application.persistence.client.ClientGateway;
import uo.ri.cws.application.persistence.client.ClientGateway.ClientRecord;
import uo.ri.cws.application.persistence.util.command.Command;
import uo.ri.cws.application.service.client.ClientCrudService.RecommenderDto;
import uo.ri.util.assertion.ArgumentChecks;

public class FindTopRecommenders implements Command<List<RecommenderDto>> {

    private final int min;
    private final ClientGateway clients = Factories.persistence.forClient();

    public FindTopRecommenders(int min) {
        ArgumentChecks.isTrue(min >= 1, "El mínimo de recomendaciones debe ser al menos 1");
        this.min = min;
    }

    @Override
    public List<RecommenderDto> execute() {
        List<RecommenderDto> result = new ArrayList<>();
        for (ClientRecord client : clients.findRecommendersWithAtLeast(min)) {
            int total = clients.findRecommendedBy(client.nif).size();
            result.add(new RecommenderDto(client.nif, client.name, client.surname, total));
        }
        return result;
    }
}
```
> No hace falta reordenar en Java: la consulta ya los devuelve en el orden pedido y el bucle mantiene ese orden.

```java
// ClientCrudServiceImpl
@Override
public List<RecommenderDto> findTopRecommenders(int min) throws BusinessException {
    return executor.execute(new FindTopRecommenders(min));
}
```

```java
// ui/foreman/client/action/FindTopRecommendersAction.java
public class FindTopRecommendersAction implements Action {

    @Override
    public void execute() throws BusinessException {
        int min = Console.readInt("Minimum number of recommendations");
        List<RecommenderDto> top = Factories.service.forClientCrudService().findTopRecommenders(min);
        Console.println("\nTop recommenders\n");
        for (RecommenderDto r : top) {
            Console.printf("\t%-10s %-15s %-25s %d\n", r.nif(), r.name(), r.surname(), r.recommendations());
        }
    }
}
```
Fíjate: en un `record` los datos se leen con **métodos** (`r.nif()`), no con campos (`r.nif`).

**Variante (solo recomendados con factura pagada)**: añade un `WHERE EXISTS` dentro de la subconsulta:
```sql
JOIN (SELECT rec.recommender_id, COUNT(*) AS total FROM TRecommendations rec
      WHERE EXISTS (SELECT 1 FROM TWorkOrders w
                    JOIN TVehicles v ON v.id = w.vehicle_id
                    JOIN TInvoices i ON i.id = w.invoice_id
                    WHERE v.client_id = rec.recommended_id AND i.state = 'PAID')
      GROUP BY rec.recommender_id HAVING COUNT(*) >= ?) r ON r.recommender_id = c.id
```
En la variante, el recuento del comando ya **no** puede ser `findRecommendedBy(nif).size()`, porque ese método cuenta todas las recomendaciones. Necesitarías un find específico (p. ej. `findRecommendedWithPaidInvoiceBy(nif)`) con el mismo `EXISTS`. Si no te da tiempo, explícalo de palabra: demuestra que lo entiendes.

### Cómo probarlo
Prueba con `min = 1` (deberían salir todos los que han recomendado a alguien), con un número muy alto (lista vacía) y con `0` (error de argumento). Comprueba el recuento de uno de ellos con la opción "List clients recommended by a client" (recuerda añadirla al menú, ver la guía 8.9).

### Trampas
- Añadir un campo `recommendations` a `ClientRecord` → rompe la regla "Record = vista fiel de la tabla" (el enunciado lo avisa a propósito).
- `WHERE COUNT(*) >= ?` → error de SQL: las condiciones sobre agregados van en `HAVING`.
- Contar en Java con `findAll()` de recomendaciones → ineficiente y penalizable.
- Leer el `record` con `r.nif` en vez de `r.nif()` → no compila.

### Preguntas cortas
1. **Diferencia entre WHERE y HAVING.** `WHERE` filtra filas antes de agrupar; `HAVING` filtra grupos después de agrupar (puede usar `COUNT`, `SUM`...).
2. **¿Haces N+1 consultas? ¿Es un problema?** Sí: 1 para la lista y 1 más por cliente. Con pocos clientes es aceptable y mantiene el Record fiel. Si fueran miles, convendría otra solución (por ejemplo, un find en `RecommendationGateway` que devuelva todas las recomendaciones de esos clientes de una vez y contar en Java).
3. **¿Por qué el método va en `ClientGateway` aunque cuente recomendaciones?** Porque devuelve filas de `TClients`. Mirar otras tablas para decidir qué clientes devolver está permitido.
4. **¿Qué devuelve `COUNT(*)` vs `COUNT(columna)`?** `COUNT(*)` cuenta filas; `COUNT(col)` cuenta filas donde `col` no es NULL (importante con LEFT JOIN).

---

## Modelo 5: Bonos sin estrenar y nueva restricción de borrado

**Dificultad ★★☆ · Tiempo orientativo: 40 min**

### Enunciado

> **Ampliación: control de bonos pendientes**
>
> Con el fin de evitar que los clientes pierdan su saldo en bonos, y de que el taller pueda recordarles su existencia, se plantean dos cambios relacionados.
>
> **Parte A. Listado de bonos sin estrenar (Manager).** Se añadirá al servicio de bonos una operación que, dado el NIF de un cliente, devuelva únicamente sus bonos **que no se hayan utilizado nunca**, es decir, aquellos con los que todavía no se ha realizado ningún cargo y cuyo importe acumulado es, por tanto, nulo. A diferencia del listado general de bonos de un cliente, en esta operación **la inexistencia del cliente sí se considera un error** que debe comunicarse al usuario. Si el cliente existe pero no tiene bonos sin estrenar, se devolverá una lista vacía. El listado se ordenará por código de bono.
>
> ```java
> /**
>  * @param nif of the client
>  * @return the vouchers of the client that have never been used (accumulated = 0),
>  *         ordered by code. Might be empty.
>  * @throws BusinessException if there is no client with that nif
>  * @throws IllegalArgumentException if nif is null or blank
>  */
> List<VoucherDto> findUnusedVouchersByClientNif(String nif) throws BusinessException;
> ```
>
> Se añadirá la opción correspondiente al menú de gestión de bonos del Manager, mostrando de cada bono su código, descripción e importe disponible.
>
> **Parte B. Restricción en la baja de clientes (Foreman).** A partir de ahora, además de las condiciones ya existentes, **no podrá darse de baja a un cliente que conserve saldo disponible en alguno de sus bonos**. En tal caso, la operación se cancelará y se informará al usuario del motivo. La comprobación deberá realizarse **antes** de efectuar cualquier modificación en la base de datos.

### Cómo leerlo

| Frase | Traducción |
|---|---|
| "bonos que no se hayan utilizado nunca... acumulado nulo" | `pm.accumulated = 0`. ¡Ojo! "nulo" aquí significa **cero**, no `NULL` de SQL |
| "la inexistencia del cliente **sí** se considera un error" | `BusinessChecks.exists(...)` → `BusinessException` (al revés que tu `FindVouchersByClientNif`, que devuelve lista vacía) |
| "existe pero no tiene → lista vacía" | No es error |
| "ordenará por código" | `ORDER BY v.code` |
| "código, descripción e importe disponible" | UI: `voucher.code`, `voucher.description`, `voucher.availableAmount` |
| Parte B: "no podrá darse de baja si conserva saldo" | `BusinessChecks` en `DeleteClientByNif`: ningún bono con `available > 0` |
| "**antes** de efectuar cualquier modificación" | La comprobación va junto a las otras comprobaciones, **antes** de los `remove` |

> Pregunta trampa típica: *"Si el rollback deshace todo, ¿por qué importa comprobarlo antes?"* Porque es más claro y eficiente: no se hacen borrados inútiles. Además, el enunciado lo pide expresamente.

### Solución, parte A

```properties
# TVOUCHERS (orden: FIND_BY_CLIENT_ID < FIND_UNUSED_BY_CLIENT_ID)
TVOUCHERS_FIND_UNUSED_BY_CLIENT_ID = SELECT pm.id, pm.version, pm.createdAt, pm.updatedAt, \
    pm.entityState, pm.client_id, pm.accumulated, pm.dtype, v.code, v.description, v.available \
    FROM TVouchers v JOIN TPaymentMeans pm ON pm.id = v.id \
    WHERE pm.client_id = ? AND pm.accumulated = 0 ORDER BY v.code
```

```java
// VoucherGateway
List<VoucherRecord> findUnusedByClientId(String clientId) throws PersistenceException;

// VoucherGatewayImpl: igual que findByClientId cambiando la clave
@Override
public List<VoucherRecord> findUnusedByClientId(String clientId) {
    try (PreparedStatement pst = Jdbc.getCurrentConnection()
        .prepareStatement(Queries.getSQLSentence("TVOUCHERS_FIND_UNUSED_BY_CLIENT_ID"))) {
        pst.setString(1, clientId);
        try (ResultSet rs = pst.executeQuery()) {
            return assembler.toRecordList(rs);
        }
    } catch (SQLException e) {
        throw new PersistenceException(e);
    }
}
```

```java
// service/paymentmean/voucher/commands/FindUnusedVouchersByClientNif.java
public class FindUnusedVouchersByClientNif implements Command<List<VoucherDto>> {

    private final String nif;
    private final ClientGateway clients = Factories.persistence.forClient();
    private final VoucherGateway vouchers = Factories.persistence.forVoucher();

    public FindUnusedVouchersByClientNif(String nif) {
        ArgumentChecks.isNotBlank(nif, "El NIF del cliente no puede estar vacío");
        this.nif = nif;
    }

    @Override
    public List<VoucherDto> execute() throws BusinessException {
        Optional<ClientRecord> client = clients.findByNif(nif);
        BusinessChecks.exists(client, "No existe el cliente");

        List<VoucherDto> result = new ArrayList<>();
        for (VoucherRecord record : vouchers.findUnusedByClientId(client.get().id)) {
            result.add(VoucherDtoAssembler.toDto(record));
        }
        return result;
    }
}

// VoucherGenerationServiceImpl
@Override
public List<VoucherDto> findUnusedVouchersByClientNif(String nif) throws BusinessException {
    return executor.execute(new FindUnusedVouchersByClientNif(nif));
}
```

```java
// ui/manager/voucher/action/FindUnusedVouchersAction.java
public class FindUnusedVouchersAction implements Action {

    @Override
    public void execute() throws BusinessException, UserInteractionException {
        String nif = Console.readString("Client nif");
        UserInteractionChecks.isNotEmpty(nif, "The nif cannot be empty");
        List<VoucherDto> list = Factories.service.forVoucherService().findUnusedVouchersByClientNif(nif);
        Console.println("\nUnused vouchers\n");
        for (VoucherDto v : list) {
            Console.printf("\t%s \t%s \t%s\n", v.code, v.description, v.availableAmount);
        }
    }
}
// VouchersMenu: { "Find unused vouchers by client nif", FindUnusedVouchersAction.class },
```

### Solución, parte B (`DeleteClientByNif`)

Ya tienes el campo `vouchers` en la clase. Añade la comprobación con las otras, antes de borrar nada:
```java
@Override
public Void execute() throws BusinessException {
    var found = clients.findByNif(nif);
    BusinessChecks.exists(found, "No existe el cliente");
    ClientRecord client = found.get();

    BusinessChecks.isTrue(vehicles.findByClientId(client.id).isEmpty(),
        "No se puede eliminar un cliente con vehículos");
    checkNoPaidInvoices(client);
    checkNoAvailableVoucherBalance(client);          // NUEVO

    // ... borrados como antes ...
}

private void checkNoAvailableVoucherBalance(ClientRecord client) throws BusinessException {
    for (VoucherRecord voucher : vouchers.findByClientId(client.id)) {
        BusinessChecks.isTrue(voucher.available.compareTo(BigDecimal.ZERO) == 0,
            "No se puede eliminar un cliente con saldo disponible en bonos");
    }
}
```
(Imports: `java.math.BigDecimal`, `...voucher.VoucherGateway.VoucherRecord`.)

> Ojo: esto **cambia el comportamiento** que esperan los tests originales de `Delete.feature`. Si alguno crea un cliente con bonos con saldo y espera poder borrarlo, ahora fallará. En una ampliación eso es esperable: díselo al profesor si te pregunta.

### Trampas
- Copiar `FindVouchersByClientNif` tal cual, que devuelve lista vacía si el cliente no existe. Aquí debe ser `BusinessException`.
- Interpretar "nulo" como `IS NULL`. La columna `accumulated` vale `0.00`, no `NULL`.
- `voucher.available > 0` no compila (`BigDecimal`). Usa `compareTo`.
- Poner la comprobación de la parte B después de borrar las recomendaciones.

### Preguntas cortas
1. **¿Por qué un bono está en dos tablas?** Herencia: `TPaymentMeans` tiene lo común (`accumulated`, `client_id`, `dtype`) y `TVouchers` lo específico (`code`, `description`, `available`), con el mismo `id`.
2. **¿Puede haber un bono con `accumulated = 0` y `available = 0`?** Sí, si se creó con importe inicial 0 (la regla de "Añadir bono" solo impide importes negativos). Saldría como "sin estrenar".
3. **¿Por qué la parte B usa `BusinessException` y no `IllegalArgumentException`?** El NIF es un argumento válido; lo que lo impide es el **estado de la BD** (tiene saldo). Eso es una regla de negocio.
4. **¿Qué pasaría si borrases el cliente sin borrar antes sus bonos?** Error de integridad referencial: `TPaymentMeans.CLIENT_ID` es clave ajena a `TClients`. Por eso el orden de borrado es: hijos → padre.

---

## Modelo 6: Servicio de vehículos: búsqueda por matrícula

**Dificultad ★★★ · Tiempo orientativo: 60 min**

### Enunciado

> **Ampliación: puesta en marcha del servicio de vehículos**
>
> En la versión actual, el subsistema de gestión de vehículos del jefe de taller no está operativo: la factoría de servicios lanza `NotYetImplementedException` al solicitarlo y las opciones del menú de vehículos no tienen funcionalidad.
>
> Se pide implementar la **única** operación declarada actualmente en la interfaz `VehicleCrudService`:
>
> ```java
> /**
>  * @param plate number
>  * @return an Optional with the vehicle dto specified be the plate number
>  * @throws BusinessException, DOES NOT
>  * @throws IllegalArgumentException if the plate is null, or does not match the format
>  */
> Optional<VehicleDto> findByPlate(String plate) throws BusinessException;
> ```
>
> El formato de matrícula admitido es el formato nacional vigente: **cuatro dígitos seguidos de tres letras mayúsculas** sin espacios ni guiones (por ejemplo, `5099JLG`). Por simplicidad, no se comprobará la exclusión de vocales.
>
> El objeto `VehicleDto` incluye, entre otros datos, el identificador del tipo de vehículo (`vehicleTypeId`). Ten en cuenta que, en la versión actual del proyecto, la capa de persistencia **no recupera dicha información**, por lo que será necesario adaptarla de forma coherente con las normas de la asignatura relativas a los objetos de transferencia de datos de la capa de persistencia.
>
> La implementación deberá respetar la estructura de paquetes establecida para los servicios de tipo CRUD, las normas de nomenclatura de fachadas y comandos, y el uso de las factorías. Por último, la opción "List all vehicles" del menú de vehículos del jefe de taller se sustituirá por una opción "View vehicle detail" que pida la matrícula y muestre todos los datos del vehículo, o un mensaje indicando que no existe.

### Cómo leerlo

| Frase | Traducción |
|---|---|
| "la factoría lanza NotYetImplementedException" | Hay que cambiar `ServiceFactoryImpl.forVehicleCrudService()` |
| "implementar la única operación" | Crear la **fachada** `VehicleCrudServiceImpl` completa (con un método) |
| "Optional" + "DOES NOT" | Si no existe → `Optional.empty()`, sin excepción |
| "IllegalArgumentException si null o no cumple el formato" | Constructor del comando: `isNotNull` + `isTrue(plate.matches(...))` |
| "cuatro dígitos seguidos de tres letras mayúsculas" | Expresión regular `"[0-9]{4}[A-Z]{3}"` |
| "la persistencia no recupera `vehicleTypeId`... adaptarla coherentemente con las normas de los Records" | `VehicleRecord` debe tener **todas** las columnas → añadir `vehicleTypeId` al record, al assembler y a **todos** los `SELECT` de `TVEHICLES` |
| "estructura de paquetes para servicios CRUD" | `service.vehicle.crud` (fachada + assembler) y `service.vehicle.crud.commands` (comandos) |
| "nomenclatura de fachadas y comandos" | `VehicleCrudServiceImpl`, comando `FindByPlate` (nombre del método con mayúscula) |
| "uso de las factorías" | UI → `Factories.service.forVehicleCrudService()`; comando → `Factories.persistence.forVehicle()` |
| "sustituir la opción List all vehicles" | Cambiar la línea en `VehicleMenu` y crear `ViewVehicleDetailAction` |

### Plan de ataque (ficheros)
1. `queries.properties`: `TVEHICLES_FIND_BY_PLATE` + añadir `vehicleType_id` a las dos consultas existentes de `TVEHICLES`.
2. `VehicleGateway`: campo `vehicleTypeId` en el record + método `findByPlate`.
3. `VehicleRecordAssembler`: leer `vehicleType_id`.
4. `VehicleGatewayImpl.findByPlate`.
5. `service/vehicle/crud/VehicleDtoAssembler.java` (NUEVO).
6. `service/vehicle/crud/commands/FindByPlate.java` (NUEVO).
7. `service/vehicle/crud/VehicleCrudServiceImpl.java` (NUEVO).
8. `ServiceFactoryImpl.forVehicleCrudService()`.
9. `ui/foreman/vehicle/action/ViewVehicleDetailAction.java` (NUEVO) + `VehicleMenu`.

### Solución

**1. queries.properties** (sección `# TVEHICLES` completa):
```properties
# TVEHICLES
TVEHICLES_FIND_BY_CLIENT_ID = SELECT id, version, createdAt, updatedAt, entityState, \
    client_id, licensePlate, make, model, vehicleType_id FROM TVehicles WHERE client_id = ?
TVEHICLES_FIND_BY_ID = SELECT id, version, createdAt, updatedAt, entityState, \
    client_id, licensePlate, make, model, vehicleType_id FROM TVehicles WHERE id = ?
TVEHICLES_FIND_BY_PLATE = SELECT id, version, createdAt, updatedAt, entityState, \
    client_id, licensePlate, make, model, vehicleType_id FROM TVehicles WHERE licensePlate = ?
```
> ¿Por qué hay que tocar también las dos consultas viejas? Porque el assembler es **común**: si `toRecord` lee `vehicleType_id` y una consulta no la selecciona, `rs.getString("vehicleType_id")` lanza `SQLException` y rompes `GenerateVouchers` y `DeleteClientByNif`, que usan esas consultas.

**2. VehicleGateway**:
```java
public interface VehicleGateway extends Gateway<VehicleGateway.VehicleRecord> {

    List<VehicleRecord> findByClientId(String clientId) throws PersistenceException;

    Optional<VehicleRecord> findByPlate(String plate) throws PersistenceException;

    class VehicleRecord extends Gateway.BaseRecord {
        public String clientId;
        public String licensePlate;
        public String make;
        public String model;
        public String vehicleTypeId;
    }
}
```
(+ `import java.util.Optional;`)

**3. VehicleRecordAssembler.toRecord**: añade
```java
record.vehicleTypeId = rs.getString("vehicleType_id");
```

**4. VehicleGatewayImpl**:
```java
@Override
public Optional<VehicleRecord> findByPlate(String plate) {
    try (PreparedStatement pst = Jdbc.getCurrentConnection()
        .prepareStatement(Queries.getSQLSentence("TVEHICLES_FIND_BY_PLATE"))) {
        pst.setString(1, plate);
        try (ResultSet rs = pst.executeQuery()) {
            return rs.next() ? Optional.of(assembler.toRecord(rs)) : Optional.empty();
        }
    } catch (SQLException e) {
        throw new PersistenceException(e);
    }
}
```
(Es el mismo patrón que tu `findById`: `rs.next()` una vez; si hay fila, la convierte.)

**5. VehicleDtoAssembler**:
```java
package uo.ri.cws.application.service.vehicle.crud;

import uo.ri.cws.application.persistence.vehicle.VehicleGateway.VehicleRecord;
import uo.ri.cws.application.service.vehicle.VehicleCrudService.VehicleDto;

public final class VehicleDtoAssembler {

    private VehicleDtoAssembler() {
    }

    public static VehicleDto toDto(VehicleRecord record) {
        VehicleDto dto = new VehicleDto();
        dto.id = record.id;
        dto.version = record.version;
        dto.plate = record.licensePlate;        // ¡nombres distintos en DTO y Record!
        dto.make = record.make;
        dto.model = record.model;
        dto.clientId = record.clientId;
        dto.vehicleTypeId = record.vehicleTypeId;
        return dto;
    }
}
```

**6. Comando FindByPlate**:
```java
package uo.ri.cws.application.service.vehicle.crud.commands;

import java.util.Optional;

import uo.ri.conf.Factories;
import uo.ri.cws.application.persistence.util.command.Command;
import uo.ri.cws.application.persistence.vehicle.VehicleGateway;
import uo.ri.cws.application.service.vehicle.VehicleCrudService.VehicleDto;
import uo.ri.cws.application.service.vehicle.crud.VehicleDtoAssembler;
import uo.ri.util.assertion.ArgumentChecks;

public class FindByPlate implements Command<Optional<VehicleDto>> {

    private static final String PLATE_FORMAT = "[0-9]{4}[A-Z]{3}";

    private final String plate;
    private final VehicleGateway vehicles = Factories.persistence.forVehicle();

    public FindByPlate(String plate) {
        ArgumentChecks.isNotNull(plate, "La matrícula no puede ser nula");
        ArgumentChecks.isTrue(plate.matches(PLATE_FORMAT), "Formato de matrícula no válido");
        this.plate = plate;
    }

    @Override
    public Optional<VehicleDto> execute() {
        return vehicles.findByPlate(plate).map(VehicleDtoAssembler::toDto);
    }
}
```
> `.map(...)` sobre un `Optional`: si tiene valor, lo transforma; si está vacío, sigue vacío. Es lo mismo que tu forma de `FindClientByNif` (`if isEmpty → empty; else Optional.of(toDto(get()))`). Usa la que entiendas mejor.
>
> La regex `[0-9]{4}[A-Z]{3}` significa: exactamente 4 caracteres del 0 al 9 y luego exactamente 3 letras de la A a la Z. `String.matches` exige que **toda** la cadena cumpla el patrón.

**7. VehicleCrudServiceImpl** (la fachada):
```java
package uo.ri.cws.application.service.vehicle.crud;

import java.util.Optional;

import uo.ri.cws.application.persistence.util.command.CommandExecutor;
import uo.ri.cws.application.service.vehicle.VehicleCrudService;
import uo.ri.cws.application.service.vehicle.crud.commands.FindByPlate;
import uo.ri.util.exception.BusinessException;

public class VehicleCrudServiceImpl implements VehicleCrudService {

    private final CommandExecutor executor = new CommandExecutor();

    @Override
    public Optional<VehicleDto> findByPlate(String plate) throws BusinessException {
        return executor.execute(new FindByPlate(plate));
    }
}
```

**8. ServiceFactoryImpl**:
```java
@Override
public VehicleCrudService forVehicleCrudService() {
    return new VehicleCrudServiceImpl();
}
```
(+ `import uo.ri.cws.application.service.vehicle.crud.VehicleCrudServiceImpl;`)

**9. UI**:
```java
package uo.ri.cws.application.ui.foreman.vehicle.action;

import java.util.Optional;

import uo.ri.conf.Factories;
import uo.ri.cws.application.service.vehicle.VehicleCrudService.VehicleDto;
import uo.ri.util.console.Console;
import uo.ri.util.exception.BusinessException;
import uo.ri.util.exception.UserInteractionChecks;
import uo.ri.util.exception.UserInteractionException;
import uo.ri.util.menu.Action;

public class ViewVehicleDetailAction implements Action {

    @Override
    public void execute() throws BusinessException, UserInteractionException {
        String plate = Console.readString("Plate number");
        UserInteractionChecks.isNotEmpty(plate, "The plate cannot be empty");

        Optional<VehicleDto> vehicle = Factories.service.forVehicleCrudService().findByPlate(plate);
        if (vehicle.isEmpty()) {
            Console.println("There is no vehicle with that plate");
            return;
        }
        VehicleDto v = vehicle.get();
        Console.printf("\t%s %s %s %s client: %s type: %s\n",
            v.id, v.plate, v.make, v.model, v.clientId, v.vehicleTypeId);
    }
}
```
`VehicleMenu`: sustituye `{ "List all vehicles", NotYetImplementedAction.class }` por `{ "View vehicle detail", ViewVehicleDetailAction.class }` y añade el import.

> Si el usuario escribe `5099jlg` (minúsculas), el servicio lanzará `IllegalArgumentException` y el `BaseMenu` mostrará el mensaje. Es lo que dice el Javadoc. Si te piden aceptar minúsculas, haz `plate.toUpperCase()` **en la UI** antes de llamar al servicio (normalizar la entrada del usuario es tarea de la presentación).

### Cómo probarlo
Matrículas reales de tu BD: `5099JLG` (BMW), `8114BHY` (VOLVO), `3365VFX` (CHEVROLET). Prueba también `0000BBB` (no existe → mensaje), `123ABC` (formato inválido) y vacío.

### Trampas
- Olvidar `ServiceFactoryImpl` → sigue lanzando `NotYetImplementedException`.
- Añadir `vehicleType_id` al assembler pero no a las consultas viejas → rompes otros casos de uso.
- Llamar al comando `FindVehicleByPlate`: la norma dice "igual que el método de la interfaz", o sea, `FindByPlate`.
- Poner `VehicleCrudServiceImpl` directamente en `service.vehicle` y no en `service.vehicle.crud`.
- Hacer `new VehicleCrudServiceImpl()` en la Action en vez de usar la factoría.
- Lanzar `BusinessException` si no existe: el Javadoc dice "DOES NOT".

### Preguntas cortas
1. **¿Qué patrón aplicas en `ServiceFactoryImpl` y para qué?** Simple Factory: la UI pide la fachada a la factoría y solo conoce la interfaz `VehicleCrudService`. Si cambias la implementación, la UI no se entera.
2. **¿Por qué el Record tiene que incluir `vehicleTypeId` aunque solo lo uses aquí?** La P4 dice que los Records son copia de las filas de la tabla. Además, así cualquier find de vehículos devuelve el objeto completo.
3. **¿Qué es la fachada y por qué no tiene lógica?** Es el punto único de entrada al subsistema. Solo crea el comando y lo pasa al `CommandExecutor`. La lógica está en el comando (Transaction Script).
4. **¿`findByPlate` es un método "demasiado específico" según la checklist?** No: `licensePlate` es clave natural única del vehículo, igual que `nif` en clientes. Es una búsqueda por identificador, no una comprobación puntual.

---

## Autoevaluación final

Si puedes contestar a todo esto sin mirar, estás preparada:

- [ ] Sé encontrar las columnas de cualquier tabla en `test.script` o en `queries.properties`.
- [ ] Sé escribir un `SELECT` con `JOIN`, `DISTINCT`, `LIKE`, `GROUP BY` y `HAVING`.
- [ ] Sé en qué Gateway va un find nuevo (el de la tabla cuyas filas devuelvo).
- [ ] Sé el nombre que tiene que tener la clave en `queries.properties` y dónde colocarla.
- [ ] Sé copiar el patrón de un GatewayImpl (try-with-resources, `setXxx`, `toRecordList`, `PersistenceException`).
- [ ] Sé cuándo lanzar `IllegalArgumentException`, cuándo `BusinessException`, cuándo devolver vacío.
- [ ] Sé crear un comando (constructor que valida, `execute` con la lógica, gateways por factoría).
- [ ] Sé añadir el método a la fachada y, si hace falta, a la `ServiceFactoryImpl`.
- [ ] Sé crear una `Action` y añadirla a un menú.
- [ ] Sé comparar `BigDecimal` con `compareTo` y `String` con `equals`.
- [ ] Sé explicar por qué el gateway no abre ni cierra conexiones.
- [ ] Sé explicar qué pasa si algo falla a mitad de un comando (rollback).

¡Mucha suerte! 🍀
