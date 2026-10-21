# CWS 2026: explicación completa desde cero

Este repositorio contiene el proyecto de gestión de un taller de reparación de
vehículos de la asignatura. El nombre habitual del proyecto es **CWS** (*Car
Workshop System*).

La aplicación permite gestionar, entre otras cosas:

- clientes;
- vehículos;
- tipos de vehículo;
- mecánicos;
- órdenes de trabajo;
- intervenciones realizadas por los mecánicos;
- facturas;
- medios de pago;
- recomendaciones entre clientes;
- generación de bonos o vales;
- cobro y liquidación de facturas.

El objetivo principal de este repositorio no es construir una aplicación
monolítica con todo mezclado, sino practicar una arquitectura por capas. Cada
parte del programa tiene una responsabilidad concreta y se comunica con la
siguiente mediante interfaces y objetos de datos.

Esta explicación está escrita desde cero. No presupone conocer JDBC, DTO,
gateways, commands, records, factories ni el patrón de capas.

---

## 1. La idea general del proyecto

Imaginemos una operación sencilla:

> Un usuario quiere buscar un cliente por su NIF.

La interfaz de usuario no debería escribir directamente una consulta SQL como:

```sql
SELECT * FROM TClients WHERE nif = ?
```

Si la interfaz hiciera eso, tendría que conocer:

- el nombre de la tabla;
- el nombre de las columnas;
- cómo abrir una conexión;
- cómo cerrar la conexión;
- cómo convertir un `ResultSet`;
- qué hacer si no hay resultados;
- cómo informar de los errores;
- cómo gestionar una transacción.

Eso haría que la interfaz estuviera fuertemente unida a la base de datos.
Además, cualquier cambio en la base de datos obligaría a modificar muchas
partes de la aplicación.

En este proyecto la operación atraviesa varias capas:

```text
Interfaz de usuario o test
              |
              v
Servicio
              |
              v
Command
              |
              v
Gateway
              |
              v
JDBC y SQL
              |
              v
Base de datos
```

Cada capa conoce solamente lo que necesita conocer:

- el cliente conoce los servicios;
- el servicio conoce las reglas de negocio y los gateways;
- el gateway conoce la persistencia;
- la clase JDBC conoce las conexiones;
- la base de datos ejecuta SQL.

La ventaja es que cada pieza puede cambiarse o probarse con menos impacto en
las demás.

---

## 2. Qué es cada cosa

### 2.1. Una entidad del negocio

Una entidad del negocio es algo que existe en el problema que estamos
resolviendo. En CWS son ejemplos de entidades:

- un cliente;
- un mecánico;
- un vehículo;
- una factura;
- una orden de trabajo;
- un medio de pago.

En una aplicación orientada a objetos clásica podríamos crear una clase
`Client` con atributos y métodos. Sin embargo, este proyecto utiliza una
separación más explícita entre los objetos usados por el servicio y los
objetos usados por la persistencia.

Por eso veremos principalmente dos tipos de objetos:

- **DTOs**, utilizados por los servicios;
- **Records**, utilizados por los gateways y JDBC.

---

### 2.2. DTO

DTO significa **Data Transfer Object**, es decir, *objeto para transferir
datos*.

Un DTO es una caja de datos. Sirve para transportar información entre el
cliente de la aplicación y la capa de servicios sin exponer detalles de la
base de datos.

Por ejemplo, la interfaz de clientes define un `ClientDto` con campos como:

```java
public static class ClientDto {
    public String id;
    public long version;

    public String nif;
    public String name;
    public String surname;
    public String addressStreet;
    public String addressCity;
    public String addressCountry;
    public String addressZipcode;
    public String phone;
    public String email;
}
```

Este objeto no representa una fila SQL completa ni contiene una consulta.
Simplemente lleva datos:

```text
id
version
nif
name
surname
dirección
teléfono
correo electrónico
```

Un DTO puede viajar desde el programa cliente hacia el servicio:

```text
Cliente/UI -> ClientDto -> Servicio
```

o desde el servicio hacia el cliente:

```text
Base de datos -> Record -> Servicio -> ClientDto -> Cliente/UI
```

#### ¿Es un DTO lo mismo que un model?

No exactamente.

Un *model* o entidad de dominio puede incluir comportamiento y reglas propias.
Por ejemplo:

```java
public class Mechanic {

    public boolean canBeDeleted() {
        // Regla de negocio
        return true;
    }
}
```

Un DTO normalmente no hace eso. Es preferible que sea una estructura sencilla
para transportar valores.

En este proyecto no hace falta crear una clase de dominio adicional
`Mechanic`, `Client` o `Invoice` si las interfaces ya proporcionan sus DTOs.
Las interfaces de servicio son el contrato que deben utilizar los clientes.

#### ¿Por qué hay DTOs repetidos?

Cada servicio define sus propios DTOs porque cada servicio tiene su propio
contrato.

Por ejemplo, puede existir un `PaymentMeanDto` en un servicio de medios de pago
y otro DTO con datos parecidos en el servicio de facturación. Aunque los
campos sean iguales, no deben mezclarse automáticamente: un servicio debe usar
los DTOs que declara su propia interfaz.

La repetición puede parecer innecesaria, pero mantiene la independencia entre
servicios y respeta el principio de segregación de interfaces.

---

### 2.3. Record

Un **Record** es un objeto orientado a la persistencia. Representa los datos
que el gateway lee de la base de datos o que necesita escribir en ella.

Por ejemplo, un `MechanicRecord` puede contener:

```text
id
version
createdAt
updatedAt
entityState
nif
name
surname
```

Los campos `version`, `createdAt`, `updatedAt` y `entityState` son metadatos de
persistencia. El DTO de la interfaz puede no exponerlos todos, pero el record
puede necesitarlos para trabajar con la base de datos.

La diferencia fundamental es:

| Objeto | Lo utiliza principalmente | Para qué sirve |
|---|---|---|
| DTO | Servicios y clientes | Transportar datos entre capas |
| Record | Gateways y persistencia | Representar datos de la base de datos |
| ResultSet | JDBC | Resultado técnico de una consulta SQL |

El servicio suele convertir:

```text
Record -> DTO
```

y, cuando recibe datos para guardar:

```text
DTO -> Record
```

---

### 2.4. Gateway

Un **Gateway** es la puerta de entrada a la persistencia.

Su misión es ocultar el SQL al resto de la aplicación. El servicio no debería
construir consultas SQL directamente para buscar, insertar o borrar entidades.
En lugar de eso, utiliza métodos del gateway.

Conceptualmente, un gateway de clientes podría tener operaciones como:

```java
Optional<ClientRecord> findByNif(String nif);
List<ClientRecord> findAll();
void add(ClientRecord record);
void update(ClientRecord record);
void remove(String id);
```

El servicio llama a esos métodos sin tener que saber si internamente se usa:

- JDBC;
- una consulta SQL;
- un `PreparedStatement`;
- un `ResultSet`;
- una tabla;
- varias tablas relacionadas.

#### Métodos que no se usan en una ampliación

La interfaz `Gateway` define un contrato común. Por eso una implementación
puede estar obligada a declarar métodos que una ampliación concreta no
necesita.

No es correcto dejar esos métodos vacíos ni devolver `null` sin explicación.
Eso puede ocultar errores y producir fallos más difíciles de localizar.

Cuando una operación no está implementada porque no forma parte del alcance
actual, debe indicar explícitamente que todavía no está disponible, por
ejemplo lanzando `NotYetImplementedException` de `cws_util`.

---

### 2.5. JDBC

JDBC significa **Java Database Connectivity**. Es la API estándar de Java para
conectarse a una base de datos y ejecutar SQL.

El flujo técnico típico es:

```java
Connection connection = ...;
PreparedStatement statement = ...;
ResultSet result = statement.executeQuery();
```

En este proyecto no se pretende repetir ese código en todos los servicios.
Las clases de infraestructura JDBC centralizan tareas como:

- obtener la conexión actual;
- cargar las propiedades de conexión;
- cargar las consultas de `queries.properties`;
- preparar sentencias;
- ejecutar comandos;
- gestionar resultados.

El servicio y la interfaz no deberían saber cómo se crea un `ResultSet`.

---

### 2.6. Command

Un **Command** encapsula una operación que debe ejecutarse contra la
persistencia.

En vez de poner una operación larga directamente en un servicio, se crea una
clase que representa esa acción:

```text
CreateInvoice
SettleInvoice
FindInvoiceByNumber
FindPaymentMeansByClientNif
FindWorkOrdersByClientNif
AddMechanic
UpdateMechanic
DeleteMechanic
```

El command suele hacer lo siguiente:

1. obtener los gateways que necesita;
2. leer o modificar records;
3. ejecutar la lógica de persistencia;
4. devolver un resultado;
5. dejar que `CommandExecutor` gestione la ejecución dentro de la conexión y
   la transacción correspondiente.

Un command no es exactamente lo mismo que un servicio.

- El **servicio** expone una operación útil para el resto de la aplicación.
- El **command** encapsula una ejecución concreta, normalmente con acceso a
  gateways.

Por ejemplo:

```text
InvoicingService.createInvoice(...)
              |
              v
        CreateInvoice
              |
              v
       InvoiceGateway
       WorkOrderGateway
       ChargeGateway
```

---

### 2.7. Service

Un **Service** es la fachada que utilizan los clientes de la aplicación.

La interfaz del servicio define qué operaciones están disponibles y qué
errores pueden producirse. La implementación contiene la lógica necesaria para
cumplir ese contrato.

Ejemplos de servicios:

- `ClientCrudService`;
- `MechanicCrudService`;
- `PaymentMeanCrudService`;
- `VoucherGenerationService`;
- `InvoicingService`.

El sufijo `Crud` viene de:

- **C**reate: crear;
- **R**ead: consultar;
- **U**pdate: modificar;
- **D**elete: borrar.

No todos los servicios tienen que ofrecer las cuatro letras exactamente, pero
el nombre indica que se ocupan de operaciones básicas sobre una entidad.

#### Los servicios no deben llamarse entre sí

Una regla importante del proyecto es que un servicio no debe utilizar otro
servicio para resolver una comprobación.

Por ejemplo, `PaymentMeanCrudService` no debería llamar a
`ClientCrudService`. Si necesita comprobar que existe un cliente, debe utilizar
el `ClientGateway` directamente.

La razón no es solamente estética. Si un servicio llama a otro:

- se mezclan responsabilidades;
- se crean dependencias circulares;
- cada servicio puede intentar abrir su propia conexión;
- la gestión de transacciones se vuelve incorrecta;
- se rompe la arquitectura por capas.

La alternativa correcta es:

```text
PaymentMeanService -> ClientGateway
```

y no:

```text
PaymentMeanService -> ClientService -> ClientGateway
```

---

### 2.8. Factory

Una **Factory** es una fábrica de objetos.

Su función es centralizar la creación de implementaciones. Los clientes no
deberían tener que escribir:

```java
new ClientCrudServiceImpl(...)
```

porque eso los acoplaría a una implementación concreta.

En su lugar, solicitan un servicio a través de las fábricas configuradas por el
proyecto:

```text
Factories
   |
   v
ServiceFactoryImpl
   |
   v
ClientCrudServiceImpl
```

La `ServiceFactoryImpl` conecta las interfaces de servicio con sus
implementaciones reales.

De forma equivalente, `PersistenceFactoryImpl` conecta las interfaces de
gateway con sus implementaciones JDBC.

Esto permite que el resto de los módulos trabaje con interfaces:

```text
ClientCrudService
```

sin conocer directamente:

```text
ClientCrudServiceImpl
```

---

## 3. Las capas del proyecto

### 3.1. Clientes de la aplicación

Los módulos `cws_client_*` contienen clientes de la capa de servicios y
menús/acciones para utilizar el sistema.

Ejemplos:

- `cws_client_manager`: operaciones del responsable o gerente;
- `cws_client_cashier`: operaciones de caja y facturación;
- `cws_client_mechanic`: operaciones del mecánico;
- `cws_client_foreman`: operaciones del encargado o jefe de taller.

Estos módulos no deberían acceder directamente a las tablas. Utilizan
interfaces de servicio.

El enunciado indica que la corrección puede ejecutarse desde estos clientes,
por lo que el proyecto de servicios debe entregar correctamente las
implementaciones que las factories devuelven.

El paquete `ui` existente en la implementación no debe borrarse por defecto.
Aunque los clientes externos también tengan menús, el UI incluido en el
proyecto forma parte de la entrega y debe seguir compilando correctamente.

---

### 3.2. Interfaces de servicio

El módulo `cws_service_interfaces` contiene los contratos públicos de los
servicios.

Este módulo dice qué se puede hacer, pero no cómo se hace.

Un método de interfaz suele documentar:

- sus argumentos;
- su resultado;
- las excepciones de negocio;
- las condiciones de los datos;
- las reglas que se deben cumplir.

Por ejemplo, `addClient` puede exigir que:

- el DTO no sea `null`;
- los campos obligatorios no estén vacíos;
- no exista otro cliente con el mismo NIF;
- si se indica un recomendador, ese recomendador exista;
- se cree automáticamente un medio de pago en efectivo.

La implementación del módulo JDBC debe cumplir ese contrato.

---

### 3.3. Servicios JDBC

La implementación de servicios y persistencia JDBC está en el código del
proyecto que se entrega como `cws_services_jdbc`. En esta copia de trabajo la
carpeta histórica de la implementación aparece como `cws0`.

Dentro se encuentran:

```text
src/uo/ri/cws/application/service
src/uo/ri/cws/application/persistence
src/uo/ri/cws/application/ui
src/queries.properties
src/connection.properties
```

La capa `service` implementa las interfaces.

La capa `persistence` contiene:

- gateways;
- records;
- commands;
- utilidades JDBC;
- assemblers;
- factories.

---

### 3.4. Persistencia

La persistencia es la parte que traduce entre objetos Java y tablas SQL.

Sus responsabilidades principales son:

- ejecutar consultas;
- insertar registros;
- modificar registros;
- borrar registros;
- transformar filas en records;
- transformar records en parámetros SQL;
- respetar claves ajenas;
- trabajar con las transacciones recibidas.

La persistencia no debería decidir reglas de interfaz, ni imprimir menús, ni
pedir datos al usuario.

---

### 3.5. Base de datos

La base de datos contiene tablas para las entidades del taller. Las clases
JDBC utilizan las propiedades de:

```text
connection.properties
queries.properties
```

`connection.properties` contiene la configuración de conexión.

`queries.properties` centraliza las consultas SQL utilizadas por los gateways.
Centralizar las consultas evita repartir SQL literal por toda la aplicación y
facilita revisar qué consulta corresponde a cada operación.

---

## 4. Flujo completo de una operación

Veamos una operación real de principio a fin:

> Añadir un cliente.

### Paso 1: el cliente prepara los datos

El cliente crea un `ClientDto`:

```text
NIF
nombre
apellidos
dirección
teléfono
correo electrónico
```

También puede indicar el NIF del cliente recomendador.

### Paso 2: se obtiene el servicio

El cliente pide el servicio a través de la factory:

```text
Factories -> ServiceFactory -> ClientCrudService
```

El cliente no necesita conocer la clase concreta.

### Paso 3: el servicio valida

`ClientCrudServiceImpl` comprueba, entre otras cosas:

- que el DTO no sea `null`;
- que los valores obligatorios sean válidos;
- que el NIF no esté repetido;
- que el recomendador exista si se ha indicado;
- que se puedan realizar todas las operaciones requeridas.

### Paso 4: se ejecutan commands y gateways

El servicio solicita las operaciones de persistencia necesarias:

```text
ClientGateway
PaymentMeanGateway
RecommendationGateway o gateway equivalente
```

Cuando una operación agrupa varios cambios relacionados, todos deben formar
parte de una misma transacción.

### Paso 5: se inserta el cliente

El gateway crea un `ClientRecord` y ejecuta el `INSERT` correspondiente.

### Paso 6: se crea el medio de pago por defecto

El contrato del servicio indica que un cliente nuevo recibe automáticamente un
medio de pago en efectivo. Esa operación debe quedar coordinada con el alta
del cliente.

### Paso 7: se devuelve el resultado

El servicio devuelve un `ClientDto` con el identificador generado:

```text
Base de datos -> ClientRecord -> ClientDto -> cliente
```

Si una parte falla, no se debe dejar una mitad de la operación hecha. La
transacción debe garantizar que el conjunto de cambios se confirma o se
deshace.

---

## 5. Operaciones principales de los servicios

### 5.1. Clientes

El servicio de clientes permite:

- añadir clientes;
- buscar un cliente por NIF;
- listar todos los clientes;
- actualizar los datos de un cliente;
- borrar un cliente;
- consultar los clientes recomendados por otro cliente.

Al borrar un cliente hay restricciones. Por ejemplo, no se puede borrar si
tiene vehículos registrados o facturas pagadas que dependan de él.

Al añadirlo puede existir un recomendador. En ese caso el recomendador debe
existir.

Además, el sistema crea un medio de pago en efectivo para el cliente nuevo.

---

### 5.2. Mecánicos

El servicio de mecánicos permite:

- añadir mecánicos;
- buscar por identificador;
- buscar por NIF;
- listar mecánicos;
- actualizar datos;
- borrar mecánicos cuando las reglas lo permiten.

La actualización debe localizar el record por su identificador y modificar
solo los valores permitidos. La versión se utiliza para controlar la
persistencia y evitar actualizaciones incoherentes.

---

### 5.3. Vehículos y tipos de vehículo

Un vehículo pertenece a un cliente y tiene un tipo de vehículo.

Por eso, cuando se trabaja con vehículos, hay que respetar las relaciones:

```text
Cliente 1 ---- N Vehículos
Tipo de vehículo 1 ---- N Vehículos
```

Una operación de borrado debe tener en cuenta estas dependencias.

---

### 5.4. Órdenes de trabajo

Una orden de trabajo representa una reparación o servicio realizado sobre un
vehículo.

Puede incluir:

- vehículo;
- fecha;
- descripción;
- estado;
- intervenciones;
- horas de mecánicos;
- materiales o cargos;
- precio total.

Los estados son importantes. Por ejemplo, la orden puede pasar por estados
como:

```text
OPEN
APPROVED
FINISHED
```

El estado exacto permitido depende del contrato y del enunciado que acompañe a
la versión del proyecto. Para la facturación, la implementación debe utilizar
el estado que el contrato identifica como facturable; en la versión trabajada
en este proyecto es `APPROVED`, no `FINISHED`.

---

### 5.5. Intervenciones

Una intervención relaciona:

- una orden de trabajo;
- un mecánico;
- un número de horas;
- una descripción o información adicional.

No es lo mismo una intervención que un cargo. La intervención representa
trabajo realizado por un mecánico; el cargo representa un importe que debe
sumarse a la orden.

---

### 5.6. Facturas

El servicio de facturación permite:

- buscar órdenes de trabajo de un cliente;
- localizar órdenes todavía no facturadas;
- crear facturas;
- buscar facturas por número;
- añadir órdenes a una factura;
- liquidar o pagar una factura;
- consultar los medios de pago de un cliente.

Crear una factura suele implicar más de una tabla. Por eso se utiliza un
command que coordina los gateways necesarios.

Una factura puede tener:

- número;
- fecha;
- importe neto;
- IVA;
- importe total;
- estado;
- cliente;
- órdenes de trabajo asociadas.

El pago de una factura también debe comprobar que el medio de pago pertenece al
cliente adecuado y que la operación es coherente con el estado de la factura.

---

### 5.7. Medios de pago

Los medios de pago tienen una jerarquía en la base de datos:

```text
TPaymentMeans
      |
      +-- TVouchers
      +-- TCreditCards
      +-- TCashes
```

Un medio de pago común puede tener campos generales como:

- identificador;
- cliente;
- versión;
- fechas;
- estado.

Cada subtipo añade sus propios campos:

- efectivo;
- tarjeta;
- bono o vale.

#### Records de la jerarquía

Los records pueden reproducir la jerarquía:

```text
PaymentMeanRecord extends BaseRecord
VoucherRecord extends PaymentMeanRecord
CardRecord extends PaymentMeanRecord
CashRecord extends PaymentMeanRecord
```

Así, los records hijos heredan los campos comunes sin duplicarlos.

#### Lecturas y escrituras

Para leer un bono completo, el gateway del bono puede necesitar hacer un
`JOIN` con `TPaymentMeans`. Esto no rompe el patrón: el gateway sigue siendo
responsable de obtener los datos del bono y puede leer de la tabla padre para
completar el record.

Para escribir, cada gateway debe escribir en su tabla. Si la operación afecta
a la tabla padre y a la tabla hija, un command coordina el orden:

```text
Alta:
1. insertar en TPaymentMeans
2. insertar en TVouchers, TCreditCards o TCashes

Borrado:
1. borrar de la tabla hija
2. borrar de TPaymentMeans
```

El orden depende de las claves ajenas y debe respetarlas.

---

### 5.8. Recomendaciones y bonos

Un cliente puede recomendar el taller a otro cliente.

En una de las ampliaciones se generan bonos cuando se cumplen las reglas
correspondientes, por ejemplo, cuando un cliente acumula tres recomendaciones
válidas.

Una recomendación no se considera necesariamente válida solo porque exista.
La lógica debe comprobar las condiciones que indique el enunciado, como:

- que la recomendación no haya sido utilizada ya para generar un bono;
- que el cliente recomendado tenga una orden de trabajo facturada;
- que la factura esté pagada;
- que se alcance el número de recomendaciones requerido.

La comprobación puede organizarse en Java. No es obligatorio convertir toda
la regla de negocio en una única consulta SQL. Es válido obtener datos
ordenados o agrupados y después aplicar la regla en la capa adecuada.

La regla de negocio debe permanecer en el servicio o command, no en la
interfaz de usuario.

---

## 6. Transacciones

Una transacción es un conjunto de operaciones que debe comportarse como una
unidad.

Ejemplo: añadir un cliente y crear su medio de pago en efectivo.

No queremos esta situación:

```text
Cliente creado: sí
Medio de pago creado: no
```

Si la segunda operación falla, la primera debería deshacerse.

Por eso, una transacción sigue normalmente este esquema:

```text
begin
  operación 1
  operación 2
  operación 3
commit
```

Si algo falla:

```text
rollback
```

El uso de commands y `CommandExecutor` ayuda a mantener las operaciones dentro
del contexto de conexión y transacción correcto.

Una causa importante para no llamar a servicios desde otros servicios es que
cada servicio podría intentar gestionar su propia conexión. Eso dividiría una
operación que debería ser atómica en varias transacciones independientes.

---

## 7. Excepciones y validación

El proyecto distingue entre varios tipos de errores.

### `IllegalArgumentException`

Se utiliza cuando el argumento recibido es inválido desde el punto de vista
formal, por ejemplo:

- DTO `null`;
- NIF `null`;
- cadena vacía;
- cadena formada solo por espacios;
- identificador obligatorio ausente.

### `BusinessException`

Se utiliza cuando los datos tienen un formato aceptable, pero la operación no
se puede realizar por una regla del negocio.

Ejemplos:

- ya existe un cliente con el mismo NIF;
- no existe el recomendador indicado;
- se intenta borrar un cliente con vehículos;
- se intenta pagar una factura inexistente;
- se intenta insertar una relación que viola una regla del sistema.

### `NotYetImplementedException`

Se utiliza para indicar explícitamente que una operación del contrato todavía
no está implementada porque queda fuera del alcance de la ampliación actual.

No debe sustituirse por:

```java
return null;
```

ni por:

```java
return Collections.emptyList();
```

si eso hace parecer que la operación se ha ejecutado correctamente.

---

## 8. El patrón de conversión entre capas

Una operación de consulta normalmente se parece a esto:

```text
SQL
 |
 v
ResultSet
 |
 v
Record
 |
 v
DTO
 |
 v
Cliente de la aplicación
```

Por ejemplo, para buscar un cliente:

1. `ClientGatewayImpl` ejecuta la consulta SQL.
2. Un assembler lee las columnas del `ResultSet`.
3. Se crea un `ClientRecord`.
4. `ClientCrudServiceImpl` transforma el record a `ClientDto`.
5. El cliente recibe el DTO.

Para una actualización el flujo se invierte:

```text
Cliente de la aplicación
 |
 v
DTO
 |
 v
Record
 |
 v
PreparedStatement
 |
 v
SQL UPDATE
```

Separar estas conversiones evita que un `ResultSet` se escape de la
persistencia o que un DTO termine usado directamente como objeto JDBC.

---

## 9. Cómo leer el código del proyecto

Cuando no se sabe dónde buscar una operación, conviene seguir este orden:

### Paso 1: leer la interfaz

Buscar la interfaz dentro de `cws_service_interfaces`.

La interfaz explica:

- qué métodos existen;
- qué argumentos reciben;
- qué devuelven;
- qué excepciones deben lanzarse;
- qué reglas debe cumplir cada operación.

La interfaz es el contrato principal.

### Paso 2: localizar la implementación

Buscar la clase cuyo nombre termina en `Impl`.

Ejemplos:

```text
ClientCrudService       -> ClientCrudServiceImpl
MechanicCrudService     -> MechanicCrudServiceImpl
InvoicingService        -> InvoicingServiceImpl
```

### Paso 3: localizar los commands

Si la operación necesita varios pasos, buscar el paquete `commands`.

Los nombres suelen coincidir con la acción:

```text
CreateInvoice
SettleInvoice
UpdateMechanic
FindInvoiceByNumber
```

### Paso 4: localizar los gateways

Buscar el gateway de la entidad:

```text
ClientGateway
MechanicGateway
InvoiceGateway
PaymentMeanGateway
VoucherGateway
WorkOrderGateway
```

La implementación suele estar en el subpaquete `impl`.

### Paso 5: revisar `queries.properties`

Si el gateway utiliza SQL externo, la consulta correspondiente estará
centralizada en `queries.properties`.

### Paso 6: revisar los tests

Los tests muestran el uso real del contrato y los casos esperados:

- datos válidos;
- datos inválidos;
- entidades inexistentes;
- conflictos;
- relaciones entre tablas;
- reglas de negocio.

---

## 10. Estructura de módulos

La raíz del proyecto contiene varios módulos Maven:

```text
cws_util
cws_service_interfaces
cws_services_jdbc
cws_service_tests
cws_client_manager
cws_client_cashier
cws_client_mechanic
cws_client_foreman
cws_database
```

### `cws_util`

Contiene utilidades compartidas, excepciones y clases comunes utilizadas por
otros módulos.

### `cws_service_interfaces`

Contiene los contratos públicos de los servicios y sus DTOs.

### `cws_services_jdbc`

Contiene la implementación JDBC de los servicios, gateways, commands,
records, factories y UI propia.

En esta copia concreta, la carpeta física histórica de esta implementación es
`cws0`.

### `cws_service_tests`

Contiene los tests de aceptación y robustez de la capa de servicios.

Su README particular explica la organización interna de los tests, fixtures,
builders y escenarios.

### `cws_client_*`

Son clientes de ejemplo que consumen los servicios. Sirven para ejecutar la
aplicación y comprobar que las factories devuelven implementaciones
funcionales.

### `cws_database`

Contiene los recursos relacionados con la base de datos, como el esquema,
datos iniciales o scripts de apoyo, según la versión entregada.

---

## 11. Cómo se relacionan las clases

Un ejemplo simplificado para clientes:

```text
ClientCrudService
        ^
        |
ClientCrudServiceImpl
        |
        +--> ClientGateway
        |        ^
        |        |
        |   ClientGatewayImpl
        |        |
        |        +--> Queries
        |        +--> Jdbc
        |        +--> Base de datos
        |
        +--> ClientDtoAssembler
        |
        +--> Commands
```

Y para facturación:

```text
InvoicingService
        ^
        |
InvoicingServiceImpl
        |
        +--> CreateInvoice
        |       +--> InvoiceGateway
        |       +--> WorkOrderGateway
        |       +--> ChargeGateway
        |
        +--> FindInvoiceByNumber
        |
        +--> SettleInvoice
                +--> InvoiceGateway
                +--> PaymentMeanGateway
```

La clase exterior solo ve el servicio. El servicio coordina. Los commands
encapsulan operaciones. Los gateways persisten. JDBC ejecuta.

---

## 12. Qué significan los tests

Los tests no solo comprueban que una consulta devuelve un resultado. También
comprueban que la arquitectura y las reglas se cumplen.

Algunos runners importantes son:

- `RunServiceTests_0`;
- `RunServiceTests_1`;
- `RunServiceTests_2`;
- `RunServiceTests_clients`;
- `RunServiceTests`.

Cada variante puede corresponder a una ampliación o a un conjunto de reglas
distinto. `RunServiceTests_1` corresponde a la variante asociada a `UO % 3 ==
1` y cubre, entre otras cosas, clientes, mecánicos, generación de bonos por
órdenes de trabajo y facturación.

`RunServiceTests_clients` se centra en las operaciones de clientes y sus
regresiones asociadas.

Un test puede comprobar:

- el resultado devuelto;
- el contenido de un DTO;
- que se lanza una excepción concreta;
- que no se crea un registro inválido;
- que se conservan las relaciones;
- que una transacción no deja datos a medias.

Por eso no basta con devolver algo que “parezca correcto”. Hay que cumplir el
contrato completo.

---

## 13. Cómo saber qué operaciones hay que implementar

Las operaciones no se inventan arbitrariamente. Se obtienen de varias fuentes,
en este orden:

1. las interfaces del módulo `cws_service_interfaces`;
2. el enunciado de la práctica;
3. la checklist de la asignatura;
4. los tests;
5. los clientes de ejemplo;
6. los patrones de las implementaciones ya hechas.

La interfaz dice qué métodos deben existir. El Javadoc dice las condiciones.
Los tests muestran ejemplos ejecutables. El enunciado y la checklist determinan
qué ampliaciones forman parte de la entrega.

Por ejemplo, si aparece:

```java
ClientDto addClient(ClientDto client, String recommenderNif)
```

hay que implementar la operación porque forma parte del contrato.

Si el gateway hereda un método que no se usa en la ampliación, el método sigue
teniendo que existir por el contrato del gateway, pero puede indicar claramente
que todavía no está implementado mediante `NotYetImplementedException`.

---

## 14. Errores de diseño que se deben evitar

### SQL directamente en los servicios

Incorrecto:

```text
Service -> SQL -> Base de datos
```

Correcto:

```text
Service -> Command -> Gateway -> SQL
```

### Llamar a un servicio desde otro servicio

Incorrecto:

```text
PaymentMeanService -> ClientService
```

Correcto:

```text
PaymentMeanService -> ClientGateway
```

### Devolver `null` para ocultar una operación incompleta

Incorrecto:

```java
return null;
```

Correcto si no forma parte del alcance:

```java
throw new NotYetImplementedException();
```

### Mezclar DTOs y Records

Un DTO no debe utilizarse como si fuera un record JDBC. Un record no debe
escaparse como si fuera el contrato público del servicio.

### Ignorar las transacciones

Si una operación modifica varias tablas relacionadas, no debe confirmarse una
parte y dejar otra sin confirmar.

### Borrar una parte de una jerarquía sin tener en cuenta las claves ajenas

En medios de pago hay que coordinar tabla padre y tabla hija respetando el
orden exigido por la base de datos.

### Poner la lógica de negocio en la UI

La UI puede recoger datos y mostrar resultados, pero no debe decidir si una
factura se puede pagar o si una recomendación genera un bono.

---

## 15. Resumen de una operación completa

Cuando el usuario pulsa una opción del menú:

```text
1. La UI recoge datos.
2. La UI obtiene una interfaz de servicio mediante una factory.
3. La UI construye un DTO.
4. El servicio valida argumentos.
5. El servicio aplica reglas de negocio.
6. El servicio ejecuta un command si necesita coordinar varios pasos.
7. El command utiliza uno o varios gateways.
8. El gateway obtiene una conexión JDBC.
9. El gateway carga una consulta de queries.properties.
10. JDBC ejecuta SQL.
11. El resultado se convierte en un Record.
12. El servicio convierte el Record a DTO.
13. La operación confirma la transacción.
14. La UI muestra el resultado.
```

Si ocurre un error:

```text
1. Se lanza la excepción adecuada.
2. Se hace rollback si había una transacción.
3. No se devuelve un resultado falso.
4. La UI o el test recibe información del fallo.
```

---

## 16. Guía rápida para empezar a estudiar el proyecto

Una ruta recomendable para entenderlo es:

1. Leer una interfaz sencilla, por ejemplo `ClientCrudService`.
2. Identificar su `ClientDto`.
3. Leer `ClientCrudServiceImpl`.
4. Seguir una operación como `findClientByNif`.
5. Localizar el gateway que usa.
6. Leer la consulta correspondiente.
7. Leer cómo se construye el record.
8. Ver cómo se transforma el record en DTO.
9. Leer el test que comprueba esa operación.
10. Repetir el recorrido con una operación que use una transacción, como
    crear una factura o añadir un cliente con su medio de pago.

Una vez entendido ese recorrido, el resto de servicios sigue prácticamente la
misma idea:

```text
Contrato -> Servicio -> Command -> Gateway -> JDBC -> Base de datos
```

---

## 17. Idea final

La idea más importante del proyecto es que cada capa tiene una función:

| Capa | Pregunta que responde |
|---|---|
| Cliente/UI | ¿Qué quiere hacer el usuario? |
| Servicio | ¿Está permitida la operación y qué reglas debe cumplir? |
| Command | ¿Cómo se coordinan los pasos de una operación compleja? |
| Gateway | ¿Cómo se leen y escriben los datos persistentes? |
| JDBC | ¿Cómo se ejecuta técnicamente el SQL? |
| Base de datos | ¿Cómo se almacenan y relacionan los datos? |

Los DTOs transportan información entre el exterior y los servicios. Los
Records representan datos de persistencia. Los gateways esconden la base de
datos. Los commands encapsulan operaciones. Las factories conectan interfaces
con implementaciones. Las transacciones garantizan que las operaciones
relacionadas no dejen datos a medias.

Cuando se entiende esa separación, una clase deja de parecer aislada y pasa a
formar parte de un recorrido completo:

```text
usuario
  -> cliente
  -> servicio
  -> command
  -> gateway
  -> JDBC
  -> base de datos
```

Ese recorrido es el funcionamiento fundamental de CWS.
