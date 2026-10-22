# CWS 2026 - `cws_services_jdbc`

Implementación JDBC de **Car Workshop System (CWS)**, el sistema de gestión de
un taller de reparación y mantenimiento de vehículos desarrollado para la
asignatura de Repositorios de Información.

> **Alcance de este documento**
>
> Aunque el repositorio contiene módulos de clientes, pruebas, utilidades y
> scripts de base de datos, la pieza que se implementa y entrega es
> `cws2026/cws_services_jdbc`. Este README documenta principalmente ese módulo:
> sus contratos, servicios, comandos, gateways, records, transacciones,
> consultas SQL y reglas de negocio. Los demás módulos se mencionan únicamente
> como consumidores o soporte de la implementación JDBC.

## Índice

- [1. Dominio del problema](#1-dominio-del-problema)
- [2. Modelo de dominio](#2-modelo-de-dominio)
- [3. Actores y responsabilidades](#3-actores-y-responsabilidades)
- [4. Flujo operativo del taller](#4-flujo-operativo-del-taller)
- [5. Reglas de negocio](#5-reglas-de-negocio)
- [6. Arquitectura por capas](#6-arquitectura-por-capas)
- [7. Estructura de `cws_services_jdbc`](#7-estructura-de-cws_services_jdbc)
- [8. Patrones utilizados](#8-patrones-utilizados)
- [9. Flujo de una operación JDBC](#9-flujo-de-una-operación-jdbc)
- [10. Servicios implementados](#10-servicios-implementados)
- [11. Persistencia y SQL](#11-persistencia-y-sql)
- [12. Transacciones y concurrencia](#12-transacciones-y-concurrencia)
- [13. Excepciones y validación](#13-excepciones-y-validación)
- [14. Configuración y ejecución](#14-configuración-y-ejecución)
- [15. Pruebas](#15-pruebas)
- [16. Guía de implementación](#16-guía-de-implementación)
- [17. Convenciones y checklist](#17-convenciones-y-checklist)
- [18. Limitaciones conocidas](#18-limitaciones-conocidas)
- [19. Documentación de referencia](#19-documentación-de-referencia)

---

## 1. Dominio del problema

El sistema representa la actividad diaria de un taller mecánico. Un cliente
lleva uno o varios vehículos al taller para que se realicen reparaciones o
tareas de mantenimiento. El trabajo se organiza mediante **órdenes de
trabajo**, que son asignadas a mecánicos y contienen las intervenciones,
tiempos y repuestos utilizados.

Cuando el trabajo termina y el jefe de taller lo aprueba, una o varias órdenes
pueden convertirse en una factura. La factura se paga en caja mediante uno o
varios medios de pago: efectivo, tarjeta o bono. El sistema debe conservar el
historial suficiente para calcular importes, controlar estados, verificar
restricciones y mantener la trazabilidad completa de la reparación.

La ampliación de fidelización añade recomendaciones entre clientes y bonos:

- un cliente puede recomendar a muchos clientes;
- cada cliente recomendado solo puede tener un recomendador;
- las recomendaciones y hechos que ya hayan generado un bono no pueden
  reutilizarse;
- los bonos se consumen al pagar facturas y nunca pueden dejar saldo negativo;
- el proceso de generación se invoca manualmente desde el menú, aunque el
  enunciado lo plantea como una tarea diaria programada.

### 1.1. Modelo de dominio proporcionado

El siguiente diagrama resume las entidades principales, los agregados y sus
relaciones. Se conserva como referencia visual del modelo extendido utilizado
por la práctica:

![Modelo de dominio extendido](docs/reference/domain-model-extended-v2627.png)

La imagen permite identificar dos agregados centrales:

- **`WorkOrder`**: reúne las intervenciones, sustituciones, mecánico asignado y
  el cálculo del trabajo realizado.
- **`Invoice`**: reúne los cargos de una o varias órdenes, calcula subtotal,
  IVA y total, registra los pagos y controla su estado.

Fuera de esos agregados se encuentran entidades compartidas como `Client`,
`Vehicle`, `Mechanic`, `SparePart`, `PaymentMean`, `Voucher`, `CreditCard`,
`Cash` y `Recommendation`.

---

## 2. Modelo de dominio

### 2.1. Entidades principales

| Entidad | Responsabilidad |
| --- | --- |
| `Client` | Identidad, datos personales, vehículos y recomendaciones realizadas o recibidas. |
| `Vehicle` | Vehículo propiedad de un cliente y clasificado por un tipo. |
| `VehicleType` | Categoría del vehículo y precio por hora de mecánico. |
| `Mechanic` | Profesional que realiza intervenciones sobre órdenes de trabajo. |
| `WorkOrder` | Reparación o mantenimiento solicitado para un vehículo. |
| `Intervention` | Trabajo realizado por un mecánico, con fecha, minutos e importe. |
| `SparePart` | Pieza disponible en el catálogo del taller. |
| `Substitution` | Uso de una cantidad de repuesto dentro de una intervención. |
| `Invoice` | Documento económico generado a partir de órdenes aprobadas. |
| `Charge` | Importe pagado con un medio de pago concreto. |
| `PaymentMean` | Abstracción de cualquier forma de pago de un cliente. |
| `Cash` | Medio de pago en efectivo creado automáticamente para todo cliente. |
| `CreditCard` | Tarjeta, tipo y fecha de caducidad. |
| `Voucher` | Bono con código, descripción, importe acumulado y saldo disponible. |
| `Recommendation` | Relación entre el cliente recomendador y el recomendado. |

### 2.2. Relaciones esenciales

```text
Client 1 -------- 0..* Vehicle
VehicleType 1 --- 0..* Vehicle
Vehicle 1 ------- 0..* WorkOrder
WorkOrder 1 ----- 0..* Intervention
Mechanic 1 ------ 0..* Intervention
Intervention 1 -- 0..* Substitution
SparePart 1 ----- 0..* Substitution
Invoice 1 ------- 1..* WorkOrder
Invoice 1 ------- 0..* Charge
PaymentMean 1 --- 0..* Charge
Client 1 -------- 0..* PaymentMean
Client 1 -------- 0..* Recommendation (como recomendador)
Client 0..1 ----- 0..1 Recommendation (como recomendado)
```

Una orden puede ser trabajada sucesivamente por distintos mecánicos si se
reabre, pero solo puede tener un mecánico activo en cada instante. El historial
de intervenciones no se elimina al reasignar la orden.

### 2.3. Tipos de pago

`PaymentMean` es una jerarquía de persistencia:

```text
TPaymentMeans
├── TCashes
├── TCreditCards
└── TVouchers
```

Todos los medios comparten `id`, `version`, `clientId` y `accumulated`.
Cada especialización añade sus datos:

- efectivo: no requiere datos adicionales;
- tarjeta: tipo, número y caducidad;
- bono: código, descripción y `availableAmount`.

---

## 3. Actores y responsabilidades

La capa JDBC no dibuja menús ni interactúa directamente con el usuario; expone
servicios para que los clientes de aplicación realicen esas tareas.

| Actor | Responsabilidad funcional |
| --- | --- |
| Gerente / manager | Gestionar mecánicos, repuestos, tipos de vehículo y generar o consultar bonos. |
| Jefe de taller / foreman | Gestionar clientes y vehículos, abrir y supervisar órdenes, asignarlas y aprobarlas. |
| Mecánico | Consultar órdenes asignadas, registrar intervenciones y repuestos y cerrar trabajos. |
| Cajero / cashier | Crear facturas, consultar medios de pago y liquidar facturas. |

La separación de actores no sustituye a la separación arquitectónica: los
clientes utilizan interfaces de servicio y nunca deben construir SQL.

---

## 4. Flujo operativo del taller

### 4.1. Recepción

1. El jefe de taller registra al cliente si todavía no existe.
2. El alta crea automáticamente su medio de pago en efectivo.
3. Se registran sus vehículos y el tipo de cada vehículo.
4. Se abre una orden de trabajo para el vehículo.
5. La orden comienza sin mecánico asignado.

### 4.2. Reparación

1. El jefe de taller asigna la orden a un mecánico.
2. El mecánico registra cada intervención y el tiempo empleado.
3. Si necesita piezas, registra sustituciones contra el catálogo.
4. El mecánico marca el trabajo como terminado.
5. El jefe revisa el resultado:
   - si es correcto, aprueba la orden;
   - si no es correcto, la reabre y puede reasignarla.

### 4.3. Facturación

1. Caja consulta las órdenes no facturadas de los vehículos de un cliente.
2. Selecciona una o varias órdenes en estado `APPROVED`.
3. El servicio crea una factura, cargos y totales económicos.
4. Una orden se factura completa; no se divide entre facturas.

### 4.4. Cobro

1. Se localiza la factura por su número.
2. Se identifica al cliente que paga.
3. Se consultan sus medios de pago disponibles.
4. Se distribuye el importe entre efectivo, tarjetas y bonos.
5. La suma debe coincidir exactamente con el total de la factura.
6. Si todo es válido, se crean los cargos y la factura pasa a `PAID`.
7. Si algo falla, no se conserva ningún cargo parcial.

---

## 5. Reglas de negocio

### 5.1. Clientes

- El NIF es único.
- El cliente debe tener los datos obligatorios válidos.
- Un cliente no puede recomendarse a sí mismo.
- El recomendador indicado debe existir.
- Un cliente puede recomendar a muchos clientes.
- Un cliente solo puede ser recomendado una vez.
- Al crear un cliente se crea su medio de pago en efectivo.
- No se puede eliminar un cliente con vehículos.
- No se puede eliminar un cliente con facturas pagadas.
- Al eliminarlo deben respetarse las relaciones de pagos y recomendaciones.

### 5.2. Órdenes de trabajo

- Una orden pertenece a un vehículo.
- Una orden puede no tener mecánico al abrirse.
- Solo un mecánico puede estar asignado activamente.
- Una orden aprobada no puede reabrirse, reasignarse ni modificarse.
- Una orden solo puede facturarse cuando está en estado `APPROVED`.
- Una orden no puede formar parte de varias facturas.

### 5.3. Facturas

- Una factura agrupa una o más órdenes completas.
- El número de factura es secuencial.
- Se calculan subtotal, IVA y total.
- Una factura pagada no vuelve a pagarse.
- La factura pasa a `PAID` solo después de registrar correctamente todos sus
  cargos.

### 5.4. Tarjetas

- El número de tarjeta es único.
- La fecha de caducidad debe ser posterior al momento del alta.
- La tarjeta es utilizable hasta el último instante de su mes de caducidad.
- Una tarjeta con cargos no puede eliminarse.
- Una tarjeta caducada no puede utilizarse para liquidar una factura.

### 5.5. Bonos

- El código del bono es único.
- La descripción es obligatoria.
- El importe inicial no puede ser negativo.
- Un bono puede utilizarse varias veces mientras conserve saldo.
- `availableAmount` nunca puede ser negativo.
- Un bono con cargos no puede eliminarse.
- El acumulado representa lo consumido y el disponible lo pendiente.

### 5.6. Generación automática de bonos

El método `VoucherGenerationService.generateVouchers()` devuelve tres
contadores en este orden: bonos de 20, 25 y 30 euros.

| Bono | Condición | Descripción |
| --- | --- | --- |
| 20 € | Cada 3 órdenes facturadas y pagadas de un cliente, sin reutilizar órdenes. | `Por tres órdenes de trabajo` |
| 25 € | Cada 3 recomendaciones cuyos clientes recomendados tengan una orden facturada y pagada, sin reutilizar recomendaciones. | `Por recomendación` |
| 30 € | Cada factura pagada cuyo total sea superior a 500 €, sin reutilizar facturas. | `Por factura de más de 500` |

El proceso puede emitir varios bonos en una ejecución, incluso de tipos
distintos para el mismo cliente. La marca de uso debe quedar persistida para
evitar emitir de nuevo un bono por los mismos hechos.

---

## 6. Arquitectura por capas

El recorrido general de una petición es:

```text
Cliente / UI / test
        |
        v
Interfaz de servicio
        |
        v
ServiceImpl
        |
        v
Command
        |
        v
Gateway
        |
        v
Jdbc + Queries
        |
        v
HSQLDB
```

### 6.1. Capa de presentación o cliente

Está fuera del núcleo JDBC. Recoge datos, muestra resultados y traduce errores
para el usuario. No debe conocer tablas, `ResultSet`, `PreparedStatement` ni
consultas SQL.

### 6.2. Capa de contratos

`cws_service_interfaces` define las interfaces que implementa el módulo JDBC.
Los DTOs de estos contratos son la API pública de la aplicación.

### 6.3. Capa de aplicación

En `cws_services_jdbc/src/uo/ri/cws/application/service` viven las
implementaciones de servicios. Validan argumentos, orquestan commands y
convierten records a DTOs.

### 6.4. Capa de persistencia

En `application/persistence` viven:

- interfaces `Gateway`;
- records de persistencia;
- implementaciones JDBC;
- assemblers de `ResultSet`;
- ejecución de commands;
- conexión y carga de consultas.

### 6.5. Base de datos

HSQLDB almacena el estado. La implementación debe respetar claves primarias,
claves ajenas, restricciones de unicidad e integridad referencial.

---

## 7. Estructura de `cws_services_jdbc`

```text
cws_services_jdbc/
├── pom.xml
└── src/
    ├── connection.properties
    ├── queries.properties
    └── uo/
        ├── ri/
        │   ├── conf/
        │   │   └── Factories.java
        │   ├── cws/
        │   │   └── application/
        │   │       ├── persistence/
        │   │       │   ├── Gateway.java
        │   │       │   ├── PersistenceFactory.java
        │   │       │   ├── ...Gateway.java
        │   │       │   ├── .../impl/
        │   │       │   ├── util/assembler/
        │   │       │   ├── util/command/
        │   │       │   └── util/jdbc/
        │   │       ├── service/
        │   │       │   ├── ServiceFactoryImpl.java
        │   │       │   ├── client/
        │   │       │   ├── mechanic/
        │   │       │   ├── invoice/
        │   │       │   └── paymentmean/
        │   │       └── ui/
        │   └── util/
        │       ├── exception/
        │       └── menu/
        └── ...
```

### 7.1. Gateways disponibles

El módulo define gateways para clientes, mecánicos, vehículos, tipos de
vehículo, órdenes, intervenciones, cargos, facturas, recomendaciones, medios
de pago, tarjetas, efectivo y bonos. Cada gateway expone records propios y
oculta el detalle de las tablas.

### 7.2. Assemblers

Los `RecordAssembler` convierten filas JDBC en records. Centralizan nombres de
columnas y la construcción de objetos, evitando repetir mapeos en cada método.

### 7.3. Factories

`Factories` mantiene las factories de servicio y persistencia:

```java
Factories.service
Factories.persistence
```

Los consumidores dependen de interfaces. `ServiceFactoryImpl` y
`PersistenceFactoryImpl` deciden qué implementación concreta se construye.

---

## 8. Patrones utilizados

### 8.1. Service Layer

Cada caso de uso se expone mediante un servicio orientado a una capacidad:
clientes, mecánicos, facturación o bonos. La interfaz es el contrato y la
implementación coordina validaciones, commands y conversión de datos.

### 8.2. Data Transfer Object (DTO)

Los DTOs atraviesan la frontera de servicios. No contienen SQL ni deben
exponer metadatos internos de persistencia salvo que el contrato los requiera.

### 8.3. Data Mapper / Record Assembler

Los records representan datos persistentes y los assemblers los reconstruyen
desde `ResultSet`. El mapeo se mantiene fuera del servicio.

### 8.4. Gateway

Un gateway actúa como repositorio especializado de una entidad y oculta SQL,
conexiones y detalles de JDBC.

### 8.5. Command

Un command encapsula una operación atómica o una secuencia coordinada de
pasos. Resulta especialmente útil para crear facturas, liquidarlas o generar
bonos.

### 8.6. Factory

Las factories desacoplan consumidores de implementaciones concretas y permiten
intercambiar persistencia o servicios sin modificar los clientes.

### 8.7. Builder en pruebas

Los tests utilizan builders de records y DTOs para generar datos coherentes,
reutilizables y legibles. El patrón no forma parte de la lógica de producción,
pero es esencial para preparar escenarios.

### 8.8. Optimistic locking

Los records incluyen `version`. Las actualizaciones condicionadas por versión
evitan sobrescribir cambios realizados por otra operación y permiten informar
de conflictos de concurrencia.

---

## 9. Flujo de una operación JDBC

Ejemplo: alta de un cliente recomendado.

```text
ClientDto
  |
  v
ClientCrudServiceImpl.add(...)
  |
  v
AddClient command
  |
  +--> ClientGateway.findByNif(...)
  +--> ClientGateway.add(...)
  +--> PaymentMeanGateway.addCash(...)
  +--> RecommendationGateway.add(...)
  |
  v
CommandExecutor
  |
  v
commit / rollback
```

El flujo detallado es:

1. El cliente crea un DTO con los datos del alta.
2. La factory devuelve `ClientCrudService`.
3. El servicio valida argumentos formales.
4. El command comprueba unicidad del NIF.
5. Si hay recomendador, verifica que exista y que no sea el propio cliente.
6. Se inserta el cliente.
7. Se crea su medio de pago `Cash`.
8. Se registra la recomendación si procede.
9. Se devuelve el DTO con el identificador generado.
10. Cualquier fallo provoca rollback del conjunto.

Para una consulta, el sentido es inverso:

```text
SQL -> ResultSet -> Record -> DTO -> consumidor
```

Para una escritura:

```text
DTO -> Record -> PreparedStatement -> SQL
```

---

## 10. Servicios implementados

### 10.1. `ClientCrudService`

Gestiona altas, consultas, listados, actualizaciones, bajas y clientes
recomendados. La implementación utiliza commands como:

- `AddClient`;
- `FindClientByNif`;
- `FindAllClients`;
- `UpdateClient`;
- `DeleteClientByNif`;
- `FindClientsRecommendedBy`.

### 10.2. `MechanicCrudService`

Gestiona altas, consultas por ID o NIF, listados, actualizaciones y bajas de
mecánicos. Utiliza records con versión y controla conflictos de actualización.

### 10.3. `InvoicingService`

Expone:

- búsqueda de órdenes por cliente;
- búsqueda de órdenes no facturadas;
- búsqueda por matrícula;
- creación de facturas;
- búsqueda por número;
- consulta de medios de pago;
- liquidación de facturas.

La creación exige órdenes existentes y en estado `APPROVED`. La liquidación
recibe un `Map<String, BigDecimal>` donde la clave es el ID del medio de pago
y el valor es el importe que se carga.

### 10.4. `VoucherGenerationService`

Genera bonos según las tres políticas descritas, consulta bonos por NIF y
devuelve el resumen agregado ordenado por importe disponible descendente.

### 10.5. Servicios aún representados por contrato

El contrato contempla también operaciones de vehículos, tipos de vehículo,
órdenes, intervenciones, repuestos, historial de clientes y medios de pago.
La regla para trabajar en este repositorio es conservar la estructura completa
de paquetes y hacer explícito cualquier método fuera del alcance mediante
`NotYetImplementedException`; nunca devolver `null` silenciosamente.

---

## 11. Persistencia y SQL

### 11.1. SQL externalizado

Las consultas deben vivir en `src/queries.properties`. La clase `Queries`
resuelve las claves y evita que el SQL quede disperso por los gateways.

Ventajas:

- revisión centralizada de consultas;
- menor acoplamiento entre Java y SQL;
- facilidad para adaptar consultas al motor soportado;
- menor riesgo de concatenar valores de usuario.

Siempre se deben utilizar parámetros en `PreparedStatement`; no se debe
concatenar NIFs, códigos, matrículas o importes en SQL.

### 11.2. Propiedades de conexión

`src/connection.properties` contiene la configuración de la conexión JDBC.
Debe ser coherente con el servidor HSQLDB iniciado para las pruebas o la
ejecución manual.

### 11.3. Recursos de persistencia

Las implementaciones JDBC deben:

1. obtener la conexión asociada al hilo;
2. cargar la consulta por su clave;
3. preparar la sentencia;
4. enlazar parámetros;
5. ejecutar la operación;
6. ensamblar records si hay resultados;
7. convertir `SQLException` en la excepción de persistencia del proyecto;
8. liberar recursos mediante `try-with-resources`.

---

## 12. Transacciones y concurrencia

### 12.1. Atomicidad

Las operaciones que modifican varias tablas deben ejecutarse en una única
transacción. Ejemplos:

- alta de cliente + efectivo + recomendación;
- alta de tarjeta o bono;
- creación de factura + relación con órdenes;
- liquidación + cargos + actualización de medios + estado `PAID`;
- generación de bono + marcas de hechos consumidos.

El patrón esperado es:

```text
begin
  paso 1
  paso 2
  paso 3
commit
```

Ante una excepción:

```text
rollback
```

`CommandExecutor` centraliza este comportamiento. Los servicios no deben abrir
transacciones independientes para cada suboperación.

### 12.2. Consistencia

El orden de borrado debe respetar las claves ajenas. En una jerarquía de
medios de pago, normalmente se elimina primero la fila especializada y
después la fila común de `TPaymentMeans`, salvo que el esquema indique otro
orden.

### 12.3. Versionado

Las actualizaciones deben incluir la versión leída. Si ninguna fila se
actualiza porque la versión cambió, se informa de un conflicto en lugar de
sobrescribir datos de otra operación.

---

## 13. Excepciones y validación

### `IllegalArgumentException`

Se utiliza para argumentos formalmente inválidos:

- DTO nulo;
- identificador nulo;
- cadena vacía o en blanco;
- lista nula o vacía;
- importe nulo;
- clave nula en el mapa de cargos.

### `BusinessException`

Se utiliza cuando el argumento tiene forma correcta, pero la operación viola
una regla:

- NIF duplicado;
- recomendador inexistente;
- cliente con vehículos que se intenta borrar;
- tarjeta caducada;
- bono sin saldo suficiente;
- factura ya pagada;
- suma de cargos distinta al total;
- orden no aprobada para facturar.

### Excepción de persistencia

Los errores de JDBC se encapsulan en la excepción de persistencia del proyecto.
La capa de presentación decide cómo mostrarlos; la persistencia no imprime
trazas ni interactúa con el usuario.

### Operaciones no implementadas

Cuando un contrato incluye una operación fuera del alcance de esta entrega,
debe utilizarse `NotYetImplementedException`. No son válidos los fallos
silenciosos:

```java
return null;
return Collections.emptyList();
```

si hacen creer que la operación se completó correctamente.

---

## 14. Configuración y ejecución

### 14.1. Requisitos

- JDK 21;
- Maven;
- HSQLDB 2.7.2;
- acceso a los recursos del proyecto;
- una base de datos configurada mediante `connection.properties`.

La versión Java está fijada en el `pom.xml` raíz mediante:

```xml
<release>21</release>
```

### 14.2. Arrancar HSQLDB

Desde `cws2026/cws_database`:

```bat
1.startup.bat
```

En sistemas Unix-like:

```bash
./1.startup.sh
```

El script utiliza `hsqldb-2.7.2.jar` y arranca el servidor HSQLDB. Los scripts
adicionales permiten abrir el manager, reiniciar la base de datos y activar el
modo legacy.

### 14.3. Configurar la conexión

Revisar:

```text
cws2026/cws_services_jdbc/src/connection.properties
cws2026/cws_services_jdbc/src/queries.properties
```

No se deben introducir credenciales reales ni datos sensibles en el
repositorio.

### 14.4. Compilar

Desde `cws2026`:

```bash
mvn clean verify
```

Para compilar solo la implementación JDBC y sus dependencias:

```bash
mvn -pl cws_services_jdbc -am clean package
```

Para ejecutar los tests del módulo de servicios:

```bash
mvn -pl cws_service_tests -am test
```

El módulo de tests está configurado para ejecutar `RunServiceTests.java`.

---

## 15. Pruebas

Las pruebas del repositorio combinan:

- **Cucumber** para aceptación y comportamiento funcional;
- **JUnit Jupiter** para robustez, validaciones y casos límite.

La estructura de aceptación sigue una relación directa:

```text
feature
  -> scenario class
      -> fixtures
      -> record builders
      -> DTO builders
      -> assertions
```

Para cada nueva funcionalidad JDBC se deben cubrir como mínimo:

1. caso feliz;
2. argumento nulo o vacío;
3. entidad inexistente;
4. duplicado;
5. violación de regla de negocio;
6. rollback ante fallo intermedio;
7. restricciones de integridad referencial;
8. repetición de la operación;
9. concurrencia o conflicto de versión cuando aplique.

Las fixtures no sustituyen a la lógica de producción: sirven para construir
estados iniciales controlados y verificar resultados.

---

## 16. Guía de implementación

Esta es la secuencia recomendada para implementar un caso de uso nuevo dentro
de `cws_services_jdbc`.

### Paso 1. Leer el contrato

Comenzar por la interfaz de `cws_service_interfaces`:

- firma;
- DTOs;
- excepciones;
- precondiciones;
- postcondiciones;
- estados permitidos.

El contrato es la fuente de verdad de la API.

### Paso 2. Identificar el modelo persistente

Localizar las tablas implicadas y sus relaciones. Determinar:

- clave primaria;
- claves ajenas;
- restricciones de unicidad;
- columnas de versión;
- tablas padre e hija;
- relaciones que deben actualizarse conjuntamente.

### Paso 3. Diseñar o revisar el gateway

Añadir únicamente operaciones de persistencia, por ejemplo:

```java
Optional<ClientRecord> findByNif(String nif);
List<ClientRecord> findAll();
void add(ClientRecord record);
void update(ClientRecord record);
void remove(String id);
```

El gateway no debe aplicar reglas de presentación ni devolver DTOs.

### Paso 4. Crear o ajustar el record

El record debe contener los datos necesarios para persistir y reconstruir la
entidad, incluidos metadatos como `id` y `version` cuando correspondan.

### Paso 5. Crear el assembler

Mapear explícitamente las columnas del `ResultSet`. Si hay una jerarquía,
construir la especialización correcta (`CashRecord`, `CardRecord` o
`VoucherRecord`).

### Paso 6. Externalizar SQL

Añadir las consultas a `queries.properties` con nombres consistentes. Usar
parámetros y comprobar que la consulta funciona en HSQLDB.

### Paso 7. Implementar el command

El command debe coordinar los gateways implicados y devolver el resultado
necesario. Debe ser pequeño, expresivo y testeable.

### Paso 8. Implementar el servicio

El servicio:

1. valida argumentos formales;
2. construye el command;
3. solicita su ejecución;
4. convierte records a DTOs;
5. propaga las excepciones previstas por el contrato.

Un servicio no debe llamar a otro servicio. Si necesita una comprobación,
utiliza el gateway correspondiente dentro del mismo command y transacción.

### Paso 9. Registrar la implementación en la factory

El consumidor debe pedir una interfaz a `ServiceFactory`; nunca instanciar
directamente una clase `Impl`.

### Paso 10. Probar límites e integridad

Añadir escenarios para las restricciones y comprobar que los fallos hacen
rollback. No basta con probar únicamente el resultado del caso feliz.

---

## 17. Convenciones y checklist

Antes de considerar terminada una modificación:

- [ ] El cambio está dentro de `cws_services_jdbc`.
- [ ] Se ha leído y respetado el contrato de servicio.
- [ ] Las consultas SQL están externalizadas.
- [ ] No se concatena entrada de usuario en SQL.
- [ ] Las operaciones multi-tabla usan una única transacción.
- [ ] Los gateways no devuelven DTOs.
- [ ] Los servicios no conocen `ResultSet` ni SQL.
- [ ] Los servicios no llaman a otros servicios.
- [ ] Los DTOs no se usan como records JDBC.
- [ ] Las restricciones de integridad referencial están cubiertas.
- [ ] Se utilizan `BusinessException` e `IllegalArgumentException` en el lugar
      correcto.
- [ ] Las operaciones fuera de alcance fallan explícitamente.
- [ ] Las actualizaciones respetan la versión.
- [ ] No hay `println`, código muerto ni excepciones ignoradas sin motivo.
- [ ] El código compila con Java 21 sin warnings relevantes.
- [ ] Los tests de aceptación y robustez pasan.
- [ ] Se ha verificado el rollback de los casos compuestos.

---

## 18. Limitaciones conocidas

El repositorio contiene una estructura docente amplia y la entrega se centra
en `cws_services_jdbc`. Por ello, algunas interfaces del contrato general
pueden conservar operaciones o menús todavía no implementados para esta
ampliación. En el código actual esas operaciones deben permanecer claramente
identificadas mediante `NotYetImplementedException`.

Esta documentación describe tanto el modelo completo exigido por el dominio
como la superficie efectivamente relevante para la implementación JDBC. No
debe interpretarse que un método declarado en una interfaz está implementado
solo porque exista su firma.

---

## 19. Documentación de referencia

El modelo visual utilizado en este README está versionado en:

```text
docs/reference/domain-model-extended-v2627.png
```

También se incluye la especificación de la ampliación de bonos:

```text
docs/reference/generacion-de-bonos-2026-2027.pdf
```

La descripción textual de este README se ha contrastado con el enunciado de la
práctica, especialmente en lo relativo a:

- estados de órdenes y facturas;
- condiciones de facturación;
- pagos fraccionados;
- validez y consumo de medios de pago;
- restricciones de borrado;
- generación de bonos de 20, 25 y 30 euros;
- separación entre contratos, servicios y persistencia JDBC.
