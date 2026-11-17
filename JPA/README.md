# Guía completa del proyecto CarWorkshop JPA (para la prueba de autoría)

> Objetivo: que entiendas **todo** lo que hay en tu proyecto (`cws_extended_2026`) y que seas capaz,
> en la prueba, de **modificar el modelo de dominio**, **modificar el mapeo** e **implementar un caso
> de uso desde cero en la capa de servicio**. La capa de presentación (los menús) **no** hay que tocarla.
>
> Todo el código de los ejercicios resueltos (apartado 9) se ha **compilado y probado** sobre una copia
> de tu proyecto: pasan sus tests y los 234 tests originales siguen en verde. Todas las consultas SQL y
> JPQL del apartado 6 se han ejecutado contra los datos reales de tu BD.

## Índice

1. [Mapa rápido para el día de la prueba](#1-mapa-rápido-para-el-día-de-la-prueba)
2. [Qué es el proyecto y cómo está organizado](#2-qué-es-el-proyecto-y-cómo-está-organizado)
3. [La arquitectura: el viaje de una petición](#3-la-arquitectura-el-viaje-de-una-petición)
4. [El modelo de dominio](#4-el-modelo-de-dominio)
5. [JPA explicado desde cero](#5-jpa-explicado-desde-cero)
6. [SQL sobre el modelo (y su equivalente JPQL)](#6-sql-sobre-el-modelo-y-su-equivalente-jpql)
7. [La capa de servicio (aplicación)](#7-la-capa-de-servicio-aplicación)
8. [Recetas paso a paso](#8-recetas-paso-a-paso)
9. [Ejercicios resueltos](#9-ejercicios-resueltos)
10. [Errores típicos y cómo diagnosticarlos](#10-errores-típicos-y-cómo-diagnosticarlos)
11. [Preguntas de "¿por qué...?" que te pueden hacer](#11-preguntas-de-por-qué-que-te-pueden-hacer)
12. [Chuleta final](#12-chuleta-final)

---

## 1. Mapa rápido para el día de la prueba

Lo que te pueden pedir y dónde se toca cada cosa:

| Te piden... | Tocas... |
|---|---|
| Modificar el modelo de dominio (nuevo atributo, nueva regla, nueva entidad) | `cws_services_jpa/src/uo/ri/cws/domain/...` |
| Modificar el mapeo | `cws_services_jpa/src/META-INF/orm.xml` (+ la BD si hay columnas/tablas nuevas) |
| Implementar un caso de uso | `application/service/<area>/...` (fachada + comandos + DtoAssembler), y si hace falta `application/repository`, `application/query`, `infrastructure/persistence/jpa/...` y `orm.xml` |
| No tocar | La capa de presentación (`cws_client_*`), las interfaces de servicio que te den, los tests |

Orden de trabajo recomendado para cualquier ampliación:

1. **Dominio** (la regla de negocio vive aquí).
2. **Mapeo** en `orm.xml` y **BD** (si hay columnas o tablas nuevas).
3. **Persistencia**: repositorio/consulta + named query en `orm.xml`.
4. **Servicio**: comandos + fachada + DTO assembler + registrar en la factoría.
5. **Probar** (un test JUnit rápido o los tests que te den).

---

## 2. Qué es el proyecto y cómo está organizado

**CarWorkshop** es la aplicación de un taller de coches: clientes con vehículos, órdenes de trabajo
(averías) que se asignan a mecánicos, intervenciones con repuestos, facturas que se pagan con medios
de pago (efectivo, tarjeta, bonos) y bonos de fidelización.

### Proyectos Maven (módulos)

| Módulo | Qué contiene | ¿Lo tocas? |
|---|---|---|
| `cws_util` | Utilidades: `ArgumentChecks`, `StateChecks`, `BusinessChecks`, `BusinessException`, menús base | No |
| `cws_service_interfaces` | Las **interfaces de servicio** y sus DTO (el "contrato" con la presentación) | No (salvo que el enunciado te dé una nueva) |
| `cws_services_jpa` | **Todo tu trabajo**: dominio, servicios, comandos, repositorios, consultas, `orm.xml` | Sí |
| `cws_service_tests` | Tests de aceptación (Cucumber) y robustez (JUnit) del servicio | No |
| `cws_client_*` | Aplicaciones de consola de cada actor (manager, cashier, foreman, mechanic) | No |
| `cws_database` | La BD HSQLDB (`test.script`) y scripts para arrancarla/gestionarla | Solo si cambias el esquema |

### Paquetes dentro de `cws_services_jpa/src`

```
uo.ri.conf.Factories                         ← "enchufe" central: qué implementación usar
uo.ri.cws.domain                             ← MODELO DE DOMINIO (entidades + Associations)
uo.ri.cws.domain.base.BaseEntity             ← superclase de todas las entidades
uo.ri.cws.domain.types                       ← value types (Money, Nif, Email, ...)
uo.ri.cws.application.service.<area>         ← implementaciones de servicio (fachadas)
uo.ri.cws.application.service.<area>.commands← COMANDOS (un caso de uso = un comando)
uo.ri.cws.application.repository             ← interfaces de repositorios
uo.ri.cws.application.query                  ← interfaces de consultas (informes, conteos)
uo.ri.cws.application.util.command           ← Command, CommandExecutor
uo.ri.cws.infrastructure.persistence.jpa.*   ← implementaciones JPA (repos, queries, executor, Jpa)
META-INF/persistence.xml                     ← configuración de la conexión
META-INF/orm.xml                             ← MAPEO objeto-relacional + consultas JPQL
```

### Cómo ejecutar los tests

* En Eclipse: botón derecho sobre `cws_service_tests/test/.../RunServiceTests_1.java` → *Run As → JUnit Test*
  (tu batería, UO % 3 = 1). `RunServiceTests` ejecuta todas.
* Los tests del dominio y del mapeo están en `cws_services_jpa/test` y `test_extended`.
* En el proyecto **extendido** la BD se abre en modo fichero (`jdbc:hsqldb:file:../cws_database/test;files_readonly=true`):
  **no hace falta arrancar ningún servidor** para pasar los tests.
* Con Maven (desde la carpeta del proyecto): `mvn test`.

---

## 3. La arquitectura: el viaje de una petición

La aplicación sigue una **arquitectura hexagonal** (por capas con dependencias hacia dentro):

```
 Presentación (menús, Action)              ← NO se toca en la prueba
        │  usa Factories.service.forXxx()
        ▼
 Servicio / Aplicación
   XxxServiceImpl  (FACHADA: 1 línea por método)
        │  executor.execute(new Comando(...))
        ▼
   CommandExecutor (JpaCommandExecutor)   ← abre EntityManager + transacción
        │
        ▼
   Comando.execute()                       ← valida, pide datos a repos, delega en el dominio
        │                    │
        ▼                    ▼
 Repositorios/Queries     Modelo de dominio  ← la LÓGICA DE NEGOCIO
 (interfaces en application,
  implementación JPA en infrastructure)
        │
        ▼
      JPA (EclipseLink) ── orm.xml ──► Base de datos HSQLDB
```

**Reglas de dependencia (muy preguntadas):**

* El **dominio no depende de nada** (ni de JPA, ni de repositorios, ni de comandos). Por eso en tu
  proyecto las clases del dominio **no tienen anotaciones JPA**: el mapeo está en `orm.xml`.
* La capa de **aplicación** depende del dominio y de **interfaces** de repositorio (no de JPA).
* La **infraestructura** (JPA) implementa esas interfaces.
* La **presentación** solo habla con las interfaces de servicio.

### Ejemplo real: dar de alta un mecánico

1. El menú llama a `Factories.service.forMechanicCrudService().create(dto)`.
2. `MechanicCrudServiceImpl.create` (fachada) hace solo:
   ```java
   return executor.execute(new AddMechanic(dto));
   ```
3. `JpaCommandExecutor.execute`:
   ```java
   EntityManager mapper = Jpa.createEntityManager();   // contexto de persistencia
   EntityTransaction trx = mapper.getTransaction();
   trx.begin();
   T res = cmd.execute();                              // ← tu comando
   trx.commit();                                       // aquí se escribe en la BD
   // si hay excepción: rollback; al final: close()
   ```
4. `AddMechanic` (constructor) comprueba argumentos; `execute()` comprueba reglas de negocio,
   crea el `Mechanic` y lo añade al repositorio.
5. Al hacer `commit`, JPA genera el `INSERT INTO TMECHANICS ...`.

> **Clave:** cada llamada a un servicio = **una transacción**. Dentro del comando todos los objetos
> que cargas están "gestionados" por JPA: si los modificas, **se guardan solos** al hacer commit.
> No existe `repo.update(...)`.

---

## 4. El modelo de dominio

### 4.1 Entidades y value types

* **Entidad**: objeto con identidad propia que se guarda en su tabla (`Client`, `Vehicle`, `WorkOrder`...).
  Todas extienden `BaseEntity`.
* **Value type** (`domain/types`): objeto inmutable sin identidad propia, definido por su valor. Son
  `record`s de Java: `Money`, `Nif`, `Email`, `PhoneNumber`, `FullName`, `Address`, `LicensePlate`,
  `CreditCardNumber`, `VoucherCode`, `Percentage`. Se validan en su constructor y tienen un
  `static boolean isValid(...)` para pre-validar sin lanzar excepción. Se guardan **dentro** de la
  tabla de la entidad que los contiene (en JPA: *embeddables*).

### 4.2 `BaseEntity`

```java
public abstract class BaseEntity {
    private String id = UUID.randomUUID().toString();  // identidad artificial (la genera Java, no la BD)
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = createdAt;
    private EntityState entityState = EntityState.ENABLED;
    private long version = 1;                         // control de concurrencia optimista (@Version)
    // getId(), getVersion(), updatedNow(), equals/hashCode sobre id
}
```

* `version` la incrementa JPA en cada UPDATE. Los casos de uso de "update" comparan la versión del
  DTO con la de la entidad (`BusinessChecks.hasVersion`) para detectar que otro usuario la cambió.
* `updatedNow()` la llaman los comandos de modificación para actualizar `updatedAt`.

### 4.3 Las entidades de tu proyecto

| Entidad | Tabla | Identidad (equals/hashCode) | Notas |
|---|---|---|---|
| `Client` | TCLIENTS | artificial (id de BaseEntity) | nif único; tiene vehículos, medios de pago, recomendaciones |
| `Vehicle` | TVEHICLES | natural: `licensePlate` | de un `Client`, de un `VehicleType` |
| `VehicleType` | TVEHICLETYPES | natural: `name` | precio por hora |
| `Mechanic` | TMECHANICS | artificial (id) | nif único; órdenes asignadas |
| `WorkOrder` | TWORKORDERS | natural: (`vehicle`, `date`) | estados OPEN→ASSIGNED→FINISHED→APPROVED→INVOICED |
| `Intervention` | TINTERVENTIONS | (`workOrder`, `mechanic`, `date`) | trabajo de un mecánico en una orden |
| `Substitution` | TSUBSTITUTIONS | (`intervention`, `sparePart`) | repuestos usados en una intervención |
| `SparePart` | TSPAREPARTS | natural: `code` | repuesto |
| `Invoice` | TINVOICES | natural: `number` | ISSUED→PAID; IVA 21 % (18 % antes de 1-7-2012) |
| `Charge` | TCHARGES | artificial (id) | cargo de una factura a un medio de pago |
| `PaymentMean` (abstracta) | TPAYMENTMEANS | — | herencia JOINED: `Cash`, `CreditCard`, `Voucher` |
| `Cash` | TCASHES | el cliente | un efectivo por cliente; no se puede borrar |
| `CreditCard` | TCREDITCARDS | natural: `number` | válida hasta `validThru` |
| `Voucher` | TVOUCHERS | natural: `code` | bono con saldo disponible |
| `Recommendation` | TRECOMMENDATIONS | (`recommender`, `recommended`) | un cliente recomienda a otro |

**¿Por qué `Client` y `Mechanic` tienen identidad artificial?** Porque el caso de uso de modificación
permite cambiar el NIF. Si el `equals` dependiera del NIF y lo cambiaras, el objeto "cambiaría de
identidad" (y se rompen los `HashSet` que lo contienen). El profesor lo dijo en el foro: *"natural o
artificial, una u otra; si no quieres complicarte, usa la heredada de BaseEntity"*.

### 4.4 Atributos naturales vs accidentales

* **Naturales**: datos propios (`description`, `date`, `amount`...).
* **Accidentales**: referencias a otras entidades (las asociaciones): `vehicle`, `mechanic`, `invoice`...

### 4.5 Asociaciones: `Associations`, `link`, `unlink`, `_get`, `_set`

Las asociaciones bidireccionales se gestionan **siempre** desde la clase `Associations`, para que los
dos extremos queden coherentes. Patrón (asociación `Owns`: un cliente tiene vehículos):

```java
public static class Owns {
    public static void link(Client client, Vehicle vehicle) {
        // 1) comprobaciones
        vehicle._setClient(client);            // 2) extremo "muchos" → apunta al "uno"
        client._getVehicles().add(vehicle);    // 3) extremo "uno" → añade a su colección
    }
    public static void unlink(Vehicle vehicle) {
        Client client = vehicle.getClient();
        client._getVehicles().remove(vehicle);
        vehicle._setClient(null);
    }
}
```

* `_setX(...)` y `_getX()` son **de paquete** (sin `public`): solo `Associations` los usa.
* El getter público de una colección devuelve **una copia** (`new HashSet<>(vehicles)`) para que
  nadie la modifique desde fuera.

**¿Quién llama a `link()`?** (respuesta del profesor en el foro)

* Normalmente **el comando**: `Associations.Holds.link(client, voucher)`.
* Excepción 1: si el otro extremo **forma parte de la identidad** de la entidad (clases asociativas como
  `Charge`, `Intervention`, `Substitution`, `Recommendation`), se llama **desde el constructor**.
* Excepción 2: dentro de un **agregado** (p. ej. `WorkOrder` → `Intervention` → `Substitution`) lo
  llama la entidad raíz; por eso esos `link` son de paquete.

Asociaciones de tu modelo:

| Asociación | Extremos | Dónde se enlaza |
|---|---|---|
| `Owns` | Client 1 — * Vehicle | comando |
| `Classifies` | VehicleType 1 — * Vehicle | comando |
| `Holds` | Client 1 — * PaymentMean | comando (Cash: constructor, el cliente es su identidad) |
| `Recommends` | Client — Recommendation — Client | constructor de `Recommendation` |
| `Fixes` | Vehicle 1 — * WorkOrder | constructor de `WorkOrder` |
| `Assigns` | Mechanic 1 — * WorkOrder | `WorkOrder.assignTo()` |
| `Bills` | Invoice 1 — * WorkOrder | `Invoice.addWorkOrder()` |
| `RecordsIntervenes` | WorkOrder — Intervention — Mechanic | constructor de `Intervention` |
| `PerformsSubstitutes` | Intervention — Substitution — SparePart | constructor de `Substitution` |
| `SettlesWithdraws` | Invoice — Charge — PaymentMean | constructor de `Charge` |

### 4.6 Reglas de negocio importantes (están en el dominio)

* **WorkOrder** (máquina de estados):
  `assignTo(m)` (OPEN→ASSIGNED), `unassign()`, `finish()` (ASSIGNED→FINISHED),
  `approve()` (FINISHED→APPROVED, calcula el importe), `invoice()` (APPROVED→INVOICED),
  `uninvoice()`, `unapprove()`, `reopen()`. Si el estado no es el correcto → `IllegalStateException`.
* **Invoice**: `new Invoice(numero, ordenes)` exige órdenes APPROVED, las enlaza y calcula
  subtotal/IVA/total. `settle(Map<PaymentMean, Money>)` crea los `Charge` y la pasa a PAID;
  `canBeSettledWith(...)` comprueba antes sin cambiar nada.
* **PaymentMean**: `canBeCharged(amount)` (Cash siempre; tarjeta si no ha caducado; bono si tiene saldo).
* **Bonos**: `Client.isEligibleForVoucher20()` (3 órdenes pagadas sin usar),
  `isEligibleForVoucher25()` (3 recomendaciones válidas), `Invoice.canGenerateVoucher30()` (pagada y > 500 €).

### 4.7 Excepciones en el dominio

| Clase | Lanza | Cuándo |
|---|---|---|
| `ArgumentChecks` | `IllegalArgumentException` | argumento inválido (null, vacío, negativo) |
| `StateChecks` | `IllegalStateException` | operación no permitida en el estado actual |

El dominio **nunca** lanza `BusinessException` (esa es de la capa de servicio).

---

## 5. JPA explicado desde cero

### 5.1 ¿Qué es JPA?

**JPA** (Jakarta Persistence) es la especificación estándar de Java para guardar objetos en una BD
relacional sin escribir SQL a mano: tú describes **cómo** se corresponde cada clase con una tabla
(el **mapeo**) y JPA genera los `INSERT`, `UPDATE`, `SELECT`... **EclipseLink** es la implementación
que usa el proyecto (el "mapeador").

### 5.2 Conceptos clave

| Concepto | Qué es | En tu proyecto |
|---|---|---|
| **Unidad de persistencia** | Configuración de conexión + mapeo | `persistence.xml`, nombre `carworkshop` |
| **EntityManagerFactory** | Fábrica pesada, una por aplicación | la crea `Jpa` la primera vez |
| **EntityManager** (mapper) | Puerta de entrada a JPA; mantiene el **contexto de persistencia** | `Jpa.getManager()` (uno por comando/hilo) |
| **Contexto de persistencia** | La "caché" de objetos gestionados en la transacción actual | se crea en `JpaCommandExecutor` |
| **Entidad gestionada (managed)** | Objeto cargado o añadido en el contexto | lo que devuelve `repo.findById(...)` |
| **Dirty checking** | Al hacer commit, JPA compara y hace UPDATE de lo que cambió | por eso **no hay `update()`** |
| **flush/commit** | Momento en que se envía el SQL a la BD | `trx.commit()` en el executor |
| **Bloqueo optimista** | El campo `version` evita sobrescribir cambios ajenos | `<version name="version"/>` |

Operaciones del `EntityManager` que usan los repositorios (`BaseJpaRepository`):

```java
Jpa.getManager().persist(entidad);        // add(): pasa a gestionada → INSERT en el commit
Jpa.getManager().remove(entidad);         // remove(): DELETE en el commit
Jpa.getManager().find(Clase.class, id);   // findById(): SELECT por clave primaria
Jpa.getManager().createNamedQuery(...)    // consultas JPQL definidas en orm.xml
```

### 5.3 `persistence.xml`

```xml
<persistence-unit name="carworkshop">
    <provider>org.eclipse.persistence.jpa.PersistenceProvider</provider>
    <mapping-file>META-INF/orm.xml</mapping-file>      <!-- aquí está el mapeo -->
    <exclude-unlisted-classes>false</exclude-unlisted-classes>
    <properties>
        <!-- <property name="eclipselink.ddl-generation" value="drop-and-create-tables"/> -->
        <property name="jakarta.persistence.jdbc.url"
                  value="jdbc:hsqldb:file:../cws_database/test;files_readonly=true"/>
        ...
    </properties>
</persistence-unit>
```

* **Brown field** (lo que se evalúa): la BD **ya existe** y el mapeo se tiene que ajustar a ella.
  La línea `ddl-generation` debe estar **comentada**.
* **Green field**: JPA crea las tablas a partir del mapeo (`drop-and-create-tables`). Solo se usó al
  principio de la P7. Los scripts de corrección la desactivan: si tu mapeo solo funciona en green
  field, **la entrega no arranca**.

### 5.4 `orm.xml`: anatomía del mapeo

Tu `orm.xml` tiene, en este orden obligatorio (lo exige el XSD):

1. `persistence-unit-metadata` (acceso por campos: `<access>FIELD</access>`).
2. `named-query` (todas las consultas JPQL).
3. `mapped-superclass` (`BaseEntity`).
4. `entity` (una por entidad).
5. `embeddable` (los value types).

Dentro de `<attributes>` también hay un orden: `id`, `basic`, `version`, `many-to-one`,
`one-to-many`, `one-to-one`, `many-to-many`, `element-collection`, `embedded`, `transient`.

#### Traducción anotación ↔ XML (por si te lo piden con anotaciones)

| Anotación | XML |
|---|---|
| `@Entity` | `<entity class="uo.ri.cws.domain.X">` |
| `@Table(name="TX")` | `<table name="TX"/>` |
| `@Table(uniqueConstraints=@UniqueConstraint(columnNames={"A","B"}))` | `<table name="TX"><unique-constraint><column-name>A</column-name><column-name>B</column-name></unique-constraint></table>` |
| `@MappedSuperclass` | `<mapped-superclass class="...BaseEntity">` |
| `@Id` / `@Version` | `<id name="id"/>` / `<version name="version"/>` |
| `@Column(unique=true)` | `<basic name="code"><column unique="true"/></basic>` |
| `@Column(name="MIN_STOCK")` | `<basic name="minStock"><column name="MIN_STOCK"/></basic>` |
| `@Enumerated(EnumType.STRING)` | `<basic name="state"><enumerated>STRING</enumerated></basic>` |
| `@ManyToOne` | `<many-to-one name="vehicle"/>` |
| `@OneToMany(mappedBy="vehicle")` | `<one-to-many name="workOrders" mapped-by="vehicle"/>` |
| `@OneToMany(..., cascade=ALL, orphanRemoval=true)` | `<one-to-many ... orphan-removal="true"><cascade><cascade-all/></cascade></one-to-many>` |
| `@OneToOne` / `@OneToOne(mappedBy="x")` | `<one-to-one name="..."/>` / `<one-to-one name="..." mapped-by="x"/>` |
| `@Inheritance(strategy=JOINED)` | `<inheritance strategy="JOINED"/>` |
| `@Embeddable` | `<embeddable class="uo.ri.cws.domain.types.Money"/>` |
| `@Embedded` + `@AttributeOverride` | `<embedded name="total"><attribute-override name="amount"><column name="TOTAL" precision="19" scale="2"/></attribute-override></embedded>` |
| `@Transient` | `<transient name="campo"/>` (no se guarda) |

#### Nombres por defecto (lo que JPA hace si no dices nada)

* **Columna** de un atributo: el nombre del atributo en mayúsculas (`description` → `DESCRIPTION`).
  Por eso un atributo nuevo `stock` **no necesita XML** si la columna se llama `STOCK`.
* **Clave ajena** de un `many-to-one`: `<atributo>_<PK>` → `vehicle` → `VEHICLE_ID`.
* **Tabla**: el nombre de la clase (`Vehicle`). En este proyecto siempre se indica `T...`.
* **Campos de un embeddable**: el nombre del campo del record (`FullName.name` → `NAME`).
  Si dos embeddables del mismo tipo chocan (p. ej. varios `Money`), se usa `attribute-override`.
* **Enum**: si no pones `<enumerated>STRING</enumerated>` se guarda como número (ordinal). Aquí siempre STRING.

#### ¿Cuál es el lado "dueño" de una relación?

* En una `one-to-many` bidireccional, el **dueño** es el lado `many-to-one` (tiene la columna FK).
  El lado `one-to-many` lleva `mapped-by="<nombre del atributo en la otra clase>"`.
* En `one-to-one`, el dueño es el que no lleva `mapped-by` (tiene la FK): en `Recommendation`,
  `recommended` es dueño; en `Client`, `recommendation` lleva `mapped-by="recommended"`.

#### Cascadas y huérfanos

* `cascade-all`: lo que hagas al padre (persist/remove) se propaga a los hijos. Se usa en los
  **agregados**: `WorkOrder→interventions`, `Intervention→substitutions`, `Invoice→charges`.
* `orphan-removal="true"`: si un hijo se quita de la colección, se borra de la BD.

### 5.5 JPQL: las consultas

JPQL se parece a SQL pero **trabaja con clases y atributos, no con tablas y columnas**:

```xml
<named-query name="Client.findByNif">
    <query>
        select c
        from Client c          <!-- clase, NO tabla TCLIENTS -->
        where c.nif = ?1       <!-- atributo (un Nif); ?1 = primer parámetro -->
    </query>
</named-query>
```

Trucos que aparecen en tu proyecto:

| Necesitas | Ejemplo |
|---|---|
| Navegar asociaciones (join implícito) | `where w.vehicle.client.nif = ?1` |
| Join explícito | `select v from Voucher v join v.client c ...` |
| Campo de un embeddable | `invoice.total.amount > ?1`, `c.fullName.name` |
| Comparar con un enum | `w.state = uo.ri.cws.domain.WorkOrder.WorkOrderState.APPROVED` |
| Lista de valores | `where w.id in ?1` (parámetro = `List<String>`) |
| Contar | `select count(i) from Intervention i where i.mechanic.id = ?1` |
| Subconsulta | `where ?1 <= (select count(w) from WorkOrder w where w.vehicle.client = c ...)` |
| Agregar y ordenar | `group by ... order by sum(v.availableAmount.amount) desc` |
| Usar `<`, `>`, `&` | Envolver la consulta en `<![CDATA[ ... ]]>` (si no, el XML se rompe) |

Ejecutarlas desde un repositorio:

```java
// Puede no haber resultado → Optional (getResultStream().findFirst())
return Jpa.getManager()
        .createNamedQuery("Client.findByNif", Client.class)
        .setParameter(1, nif)
        .getResultStream()
        .findFirst();

// Lista
.getResultList();

// SIEMPRE hay exactamente una fila (count, max) → getSingleResult()
return 0 < Jpa.getManager()
        .createNamedQuery("Mechanic.countInterventions", Long.class)
        .setParameter(1, mechanicId)
        .getSingleResult();
```

> ⚠️ **Nunca** uses `getSingleResult()` en una consulta que puede no devolver nada: lanza
> `NoResultException`. Penaliza en la checklist.

Errores de checklist sobre consultas: todas deben estar en `orm.xml`; nada de filtrar en Java tras un
`findAll()`; nada de consultar lo que se obtiene navegando (`mechanic.getAssigned()`); nada de
lógica de negocio en el JPQL; preferir joins de JPQL a la forma "theta" (`from A a, B b where a.x = b.y`).

### 5.6 La base de datos HSQLDB y cómo cambiar el esquema

Carpeta `cws_database`:

| Fichero | Para qué |
|---|---|
| `test.script` | **La BD** (esquema + datos). Es la que usan los tests |
| `test.script.data` | Copia con los datos "legacy" (`4.setLegacy` la copia a `test.script`) |
| `test.script.bck` | Copia vacía (`3.reset` la copia a `test.script`) |
| `1.startup.bat` | Arranca el servidor HSQLDB sobre `test` |
| `2.runManagerSwing.bat` | Abre el gestor visual conectado al servidor |

**Si añades un atributo o una entidad, la tabla/columna NO existe en la BD** (estamos en brown field).
Tienes que crearla. Procedimiento con las herramientas del proyecto:

1. Cierra todo lo que esté usando la BD (tests en ejecución, clientes).
2. Ejecuta `1.startup.bat` (deja la ventana abierta).
3. Ejecuta `2.runManagerSwing.bat`. En la ventana de conexión deja URL `jdbc:hsqldb:hsql://localhost/`,
   usuario `sa`, sin contraseña.
4. Escribe la sentencia (`ALTER TABLE ...` o `CREATE TABLE ...`) y pulsa *Execute*.
5. Ejecuta `SHUTDOWN` en el gestor: así se escribe todo en `test.script`.
6. Cierra el gestor y la ventana del servidor.

> ⚠️ `3.reset` y `4.setLegacy` **sobrescriben** `test.script`: si los ejecutas pierdes tus cambios
> de esquema y tendrás que repetirlos.
> ⚠️ Lee bien el enunciado de la prueba: puede que te den ya la BD modificada o te indiquen otro modo.

Tipos SQL habituales: `VARCHAR(255)` (String, ids), `INTEGER` (int), `BIGINT` (long),
`NUMERIC(19,2)` (Money), `DATE` (LocalDate), `TIMESTAMP` (LocalDateTime), `BOOLEAN` (boolean).

Plantilla para una tabla nueva de una entidad que extiende `BaseEntity`:

```sql
CREATE TABLE TNUEVA (
    ID VARCHAR(255) NOT NULL PRIMARY KEY,
    CREATEDAT TIMESTAMP, UPDATEDAT TIMESTAMP,
    ENTITYSTATE VARCHAR(255), VERSION BIGINT,
    -- tus atributos...
    -- tus claves ajenas: <ATRIBUTO>_ID VARCHAR(255),
    CONSTRAINT FK_TNUEVA_X_ID FOREIGN KEY (X_ID) REFERENCES TX (ID)
)
```

---

## 6. SQL sobre el modelo (y su equivalente JPQL)

Todas las consultas de este apartado se han ejecutado contra **tu** BD (`cws_extended_2026/cws_database`);
entre paréntesis verás cuántas filas devuelven con los datos que trae el proyecto. Puedes lanzarlas tú
desde el gestor de HSQLDB (`1.startup.bat` + `2.runManagerSwing.bat`, apartado 5.6).

### 6.1 SQL frente a JPQL: lo que cambia

En SQL hablas de **tablas y columnas**; en JPQL (las `named-query` del `orm.xml`) de **clases y atributos**.

| Concepto | SQL (BD) | JPQL (orm.xml) |
|---|---|---|
| Origen | `FROM TCLIENTS c` | `from Client c` |
| Columna / atributo | `c.SURNAME` | `c.fullName.surname` (atraviesa el embeddable) |
| Value type | columnas "planas": `c.NIF` | `c.nif` (el objeto `Nif`) o `c.nif.nif` (el String) |
| Relación | `JOIN TCLIENTS c ON v.CLIENT_ID = c.ID` | `join v.client c` o navegar `v.client.nif` |
| Enum | texto: `w.STATE = 'APPROVED'` | `w.state = uo.ri.cws.domain.WorkOrder.WorkOrderState.APPROVED` |
| Herencia JOINED | unir padre e hija: `TPAYMENTMEANS pm JOIN TVOUCHERS v ON v.ID = pm.ID` | basta con `from Voucher v` (JPA hace el join) |
| Tipo de subclase | `pm.DTYPE = 'Voucher'` | `type(pm) = Voucher` |
| Colección vacía | `LEFT JOIN ... WHERE x.ID IS NULL` | `where c.vehicles is empty` |
| Parámetros | `?` (en el gestor pones el valor a mano) | `?1`, `?2`... con `setParameter(1, valor)` |
| Resultado | filas | objetos (entidades) o valores |

> Regla mental: para pasar de SQL a JPQL, cambia cada tabla por su clase, cada `JOIN ... ON FK = ID`
> por la navegación del atributo, y cada columna por el camino de atributos (`c.fullName.surname`).

### 6.2 El esquema: tablas, columnas y claves ajenas

Todas las tablas tienen además `ID` (PK), `CREATEDAT`, `UPDATEDAT`, `ENTITYSTATE` y `VERSION` (de `BaseEntity`).

| Tabla | Columnas propias | Claves ajenas → tabla |
|---|---|---|
| TCLIENTS | NIF, NAME, SURNAME, EMAIL, PHONE, STREET, CITY, COUNTRY, POSTALCODE | — |
| TVEHICLES | LICENSEPLATE, MAKE, MODEL | CLIENT_ID → TCLIENTS, VEHICLETYPE_ID → TVEHICLETYPES |
| TVEHICLETYPES | NAME, PRICE_PER_HOUR | — |
| TMECHANICS | NIF, NAME, SURNAME | — |
| TWORKORDERS | DATE, DESCRIPTION, STATE, TOTAL_AMOUNT, USEDFORVOUCHER | VEHICLE_ID, MECHANIC_ID, INVOICE_ID |
| TINTERVENTIONS | DATE, MINUTES | WORKORDER_ID, MECHANIC_ID |
| TSUBSTITUTIONS | QUANTITY, PRICE | INTERVENTION_ID, SPAREPART_ID |
| TSPAREPARTS | CODE, DESCRIPTION, PRICE | — |
| TINVOICES | NUMBER, DATE, STATE, SUBTOTAL, VATRATE, VATAMOUNT, TOTAL, USEDFORVOUCHER | — |
| TCHARGES | CHARGE (importe) | INVOICE_ID, PAYMENTMEAN_ID |
| TPAYMENTMEANS | DTYPE (`Cash`/`CreditCard`/`Voucher`), ACCUMULATED | CLIENT_ID |
| TCASHES | (solo ID) | ID → TPAYMENTMEANS |
| TCREDITCARDS | NUMBER, TYPE, VALIDTHRU | ID → TPAYMENTMEANS |
| TVOUCHERS | CODE, DESCRIPTION, AVAILABLE | ID → TPAYMENTMEANS |
| TRECOMMENDATIONS | USEDFORVOUCHER | RECOMMENDER_ID, RECOMMENDED_ID → TCLIENTS |

Caminos de joins que más se usan:

```
TCLIENTS ◄─CLIENT_ID── TVEHICLES ◄─VEHICLE_ID── TWORKORDERS ──INVOICE_ID─► TINVOICES
                           │                        ▲   │                      ▲
                     VEHICLETYPE_ID          WORKORDER_ID MECHANIC_ID       INVOICE_ID
                           ▼                        │   ▼                      │
                    TVEHICLETYPES            TINTERVENTIONS ─► TMECHANICS   TCHARGES ──PAYMENTMEAN_ID─► TPAYMENTMEANS
                                                    ▲                                                    ▲ (mismo ID)
                                             INTERVENTION_ID                               TCASHES / TCREDITCARDS / TVOUCHERS
                                                    │
                                             TSUBSTITUTIONS ──SPAREPART_ID─► TSPAREPARTS
```

### 6.3 Recordatorio de la sintaxis

```sql
SELECT   columnas, funciones(...) AS alias      -- 5) qué devuelves
FROM     tabla t                                -- 1) de dónde
JOIN     otra o ON o.T_ID = t.ID                -- 1) uniones
WHERE    condición sobre filas                  -- 2) filtra filas (antes de agrupar)
GROUP BY columnas                               -- 3) agrupa
HAVING   condición sobre grupos                 -- 4) filtra grupos (después de agrupar)
ORDER BY columna [ASC|DESC]                     -- 6) ordena
LIMIT    n                                      -- 7) solo las n primeras (HSQLDB)
```

* Los números indican el **orden en que se evalúa**: por eso en `WHERE` no puedes usar `COUNT(...)` (aún no
  hay grupos) y para eso existe `HAVING`.
* Todo lo del `SELECT` que no sea función de agregado (`COUNT`, `SUM`, `AVG`, `MIN`, `MAX`) **tiene que
  estar en el `GROUP BY`**.
* `JOIN` (o `INNER JOIN`): solo filas con pareja. `LEFT JOIN`: todas las de la izquierda, con `NULL` si no
  tienen pareja.
* Comparar con nulos: `IS NULL` / `IS NOT NULL` (nunca `= NULL`).
* Texto entre comillas simples: `'APPROVED'`. `LIKE 'G%'` = empieza por G; `'%G%'` = contiene G.

### 6.4 Consultas básicas

**1. Todos los clientes ordenados por apellidos** (200 filas)

```sql
SELECT NIF, NAME, SURNAME, EMAIL
FROM TCLIENTS
ORDER BY SURNAME, NAME;
```
```text
JPQL: select c from Client c order by c.fullName.surname, c.fullName.name
```

**2. Filtrar con `LIKE` y `OR`: clientes cuyo apellido empieza por G o que viven en Oviedo** (46)

```sql
SELECT NIF, NAME, SURNAME
FROM TCLIENTS
WHERE SURNAME LIKE 'G%' OR CITY = 'Oviedo';
```
```text
JPQL: select c from Client c where c.fullName.surname like 'G%' or c.address.city = 'Oviedo'
```

**3. Rangos con `BETWEEN`: repuestos entre 10 y 50 €, del más caro al más barato** (9)

```sql
SELECT CODE, DESCRIPTION, PRICE
FROM TSPAREPARTS
WHERE PRICE BETWEEN 10 AND 50
ORDER BY PRICE DESC;
```
```text
JPQL: select sp from SparePart sp where sp.unitPrice.amount between 10 and 50 order by sp.unitPrice.amount desc
```

**4. Los 5 repuestos más caros (`LIMIT`)** (5)

```sql
SELECT CODE, DESCRIPTION, PRICE
FROM TSPAREPARTS
ORDER BY PRICE DESC
LIMIT 5;
```
(En JPQL no existe `LIMIT`: se usa `query.setMaxResults(5)` en Java.)

**5. Valores distintos (`DISTINCT`): marcas de coche que hay en el taller** (22)

```sql
SELECT DISTINCT MAKE FROM TVEHICLES ORDER BY MAKE;
```

### 6.5 Consultas con JOIN

**6. Vehículos con su dueño y su tipo (dos joins)** (266)

```sql
SELECT v.LICENSEPLATE, v.MAKE, v.MODEL, c.NAME, c.SURNAME, vt.NAME AS TIPO
FROM TVEHICLES v
JOIN TCLIENTS c       ON v.CLIENT_ID = c.ID
JOIN TVEHICLETYPES vt ON v.VEHICLETYPE_ID = vt.ID;
```
```text
JPQL: select v.licensePlate.licensePlate, c.fullName.name, v.vehicleType.name
      from Vehicle v join v.client c
```

**7. Órdenes de trabajo APROBADAS de un cliente por NIF** (es la consulta de tu
`WorkOrder.findNotInvoicedByClientNif`)

```sql
SELECT w.ID, w.DESCRIPTION, w.STATE, w.TOTAL_AMOUNT
FROM TWORKORDERS w
JOIN TVEHICLES v ON w.VEHICLE_ID = v.ID
JOIN TCLIENTS c  ON v.CLIENT_ID = c.ID
WHERE c.NIF = '71581238F'
  AND w.STATE = 'APPROVED';
```
```text
JPQL: select w from WorkOrder w
      where w.vehicle.client.nif.nif = ?1
        and w.state = uo.ri.cws.domain.WorkOrder.WorkOrderState.APPROVED
```
Fíjate en que en JPQL **no hay joins explícitos**: se navega `w.vehicle.client` y JPA genera los dos joins.

**8. Quién recomendó a quién (la misma tabla dos veces = *self join*)** (19)

```sql
SELECT r1.NIF AS RECOMIENDA, r2.NIF AS RECOMENDADO, r.USEDFORVOUCHER
FROM TRECOMMENDATIONS r
JOIN TCLIENTS r1 ON r.RECOMMENDER_ID = r1.ID
JOIN TCLIENTS r2 ON r.RECOMMENDED_ID = r2.ID;
```
```text
JPQL (los recomendados por un cliente):
      select r.recommended from Recommendation r where r.recommender.nif.nif = ?1
```

**9. `LEFT JOIN` para encontrar "los que no tienen": clientes sin vehículos** (18)

```sql
SELECT c.NIF, COUNT(v.ID) AS VEHICULOS
FROM TCLIENTS c
LEFT JOIN TVEHICLES v ON v.CLIENT_ID = c.ID
GROUP BY c.NIF
HAVING COUNT(v.ID) = 0;
```
```text
JPQL: select c from Client c where c.vehicles is empty
```
Con `JOIN` normal estos clientes desaparecerían (no tienen pareja). `COUNT(v.ID)` cuenta 0 porque no
cuenta los `NULL`; `COUNT(*)` contaría 1.

**10. Mecánicos que nunca han hecho una intervención** (0 con tus datos: todos trabajan)

```sql
SELECT m.NIF, m.NAME, m.SURNAME
FROM TMECHANICS m
LEFT JOIN TINTERVENTIONS i ON i.MECHANIC_ID = m.ID
WHERE i.ID IS NULL;
```
```text
JPQL: select m from Mechanic m
      where not exists (select i from Intervention i where i.mechanic = m)
```

### 6.6 Agregados: COUNT, SUM, AVG, GROUP BY, HAVING

**11. Cuántas órdenes de trabajo hay en cada estado** (5 grupos)

```sql
SELECT STATE, COUNT(*) AS TOTAL
FROM TWORKORDERS
GROUP BY STATE
ORDER BY TOTAL DESC;
```
```text
JPQL: select w.state, count(w) from WorkOrder w group by w.state
```

**12. Mecánicos con más de 5 intervenciones y minutos trabajados (`HAVING`)** (10)

```sql
SELECT m.NIF, m.NAME, COUNT(i.ID) AS INTERVENCIONES, SUM(i.MINUTES) AS MINUTOS
FROM TMECHANICS m
JOIN TINTERVENTIONS i ON i.MECHANIC_ID = m.ID
GROUP BY m.NIF, m.NAME
HAVING COUNT(i.ID) > 5
ORDER BY INTERVENCIONES DESC;
```
```text
JPQL: select m.nif.nif, count(i) from Intervention i join i.mechanic m
      group by m.nif.nif having count(i) > 5
```

**13. Importe medio y máximo de las órdenes facturadas por tipo de vehículo (`AVG`, `MAX`)** (6)

```sql
SELECT vt.NAME, AVG(w.TOTAL_AMOUNT) AS MEDIA, MAX(w.TOTAL_AMOUNT) AS MAXIMO
FROM TWORKORDERS w
JOIN TVEHICLES v      ON w.VEHICLE_ID = v.ID
JOIN TVEHICLETYPES vt ON v.VEHICLETYPE_ID = vt.ID
WHERE w.STATE = 'INVOICED'
GROUP BY vt.NAME;
```

**14. Facturas y facturación por año (funciones de fecha)** (2 años)

```sql
SELECT YEAR(f.DATE) AS ANYO, COUNT(*) AS FACTURAS, SUM(f.TOTAL) AS IMPORTE
FROM TINVOICES f
GROUP BY YEAR(f.DATE)
ORDER BY ANYO;
```

**15. Medios de pago por tipo (la columna `DTYPE` de la herencia)** (3)

```sql
SELECT pm.DTYPE, COUNT(*) AS TOTAL, SUM(pm.ACCUMULATED) AS ACUMULADO
FROM TPAYMENTMEANS pm
GROUP BY pm.DTYPE;
```
```text
JPQL (solo tarjetas): select pm from PaymentMean pm where type(pm) = CreditCard
```

### 6.7 ⚠️ La trampa de sumar a través de joins

Queremos "**lo facturado a cada cliente**". Una factura puede tener **varias** órdenes de trabajo, así que
si unimos cliente → vehículos → órdenes → facturas, **la misma factura aparece repetida** y `SUM(f.TOTAL)`
la cuenta varias veces.

```sql
-- ❌ MAL: la factura se repite una vez por cada orden que contiene
SELECT c.NIF, SUM(f.TOTAL)
FROM TCLIENTS c
JOIN TVEHICLES v   ON v.CLIENT_ID = c.ID
JOIN TWORKORDERS w ON w.VEHICLE_ID = v.ID
JOIN TINVOICES f   ON w.INVOICE_ID = f.ID
GROUP BY c.NIF;
```

Soluciones correctas:

```sql
-- ✅ Opción A: sumar lo que no se repite (el importe de cada orden facturada)  (148 filas)
SELECT c.NIF, SUM(w.TOTAL_AMOUNT) AS FACTURADO_SIN_IVA
FROM TCLIENTS c
JOIN TVEHICLES v   ON v.CLIENT_ID = c.ID
JOIN TWORKORDERS w ON w.VEHICLE_ID = v.ID
WHERE w.INVOICE_ID IS NOT NULL
GROUP BY c.NIF
ORDER BY FACTURADO_SIN_IVA DESC;

-- ✅ Opción B: elegir cada factura una sola vez con una subconsulta IN  (148 filas)
SELECT c.NIF, SUM(f.TOTAL) AS TOTAL_FACTURAS
FROM TCLIENTS c
JOIN TINVOICES f ON f.ID IN (SELECT w.INVOICE_ID
                             FROM TWORKORDERS w
                             JOIN TVEHICLES v ON w.VEHICLE_ID = v.ID
                             WHERE v.CLIENT_ID = c.ID)
GROUP BY c.NIF
ORDER BY TOTAL_FACTURAS DESC;
```
```text
JPQL (opción A): select c.nif.nif, sum(w.amount.amount)
                 from WorkOrder w join w.vehicle v join v.client c
                 where w.invoice is not null
                 group by c.nif.nif order by sum(w.amount.amount) desc
```
(`SUM(DISTINCT f.TOTAL)` **no** es solución: si dos facturas distintas valen lo mismo, se cuenta una sola.)

### 6.8 Subconsultas, EXISTS / NOT EXISTS

**16. Clientes con al menos 3 órdenes pagadas y no usadas para bono** (la regla del bono de 20 €) (51)

```sql
SELECT c.NIF, c.NAME
FROM TCLIENTS c
WHERE 3 <= (SELECT COUNT(*)
            FROM TWORKORDERS w
            JOIN TVEHICLES v ON w.VEHICLE_ID = v.ID
            JOIN TINVOICES f ON w.INVOICE_ID = f.ID
            WHERE v.CLIENT_ID = c.ID          -- subconsulta "correlacionada": usa la c de fuera
              AND f.STATE = 'PAID'
              AND w.USEDFORVOUCHER = FALSE);
```
Compárala con tu `Client.findWithWorkOrdersUnusedForVoucher` del `orm.xml`: es la misma idea en JPQL.

**17. Repuestos que nunca se han usado (`NOT EXISTS`)** (0 con tus datos)

```sql
SELECT sp.CODE, sp.DESCRIPTION
FROM TSPAREPARTS sp
WHERE NOT EXISTS (SELECT 1 FROM TSUBSTITUTIONS s WHERE s.SPAREPART_ID = sp.ID);
```
```text
JPQL: select sp from SparePart sp
      where not exists (select s from Substitution s where s.sparePart = sp)
```

**18. Siguiente número de factura (`COALESCE` para el caso de tabla vacía)** (1)

```sql
SELECT COALESCE(MAX(NUMBER), 0) + 1 AS SIGUIENTE FROM TINVOICES;
```
Es lo que hace tu `Invoice.getNextInvoiceNumber`: sin `COALESCE`, con la tabla vacía `MAX` devuelve
`NULL` y `NULL + 1` es `NULL`.

### 6.9 Herencia JOINED: medios de pago

Cada medio de pago tiene **una fila en `TPAYMENTMEANS`** (lo común: acumulado, cliente, tipo) **y otra en su
tabla hija con el mismo `ID`** (lo específico).

**19. Bonos con su cliente** (130)

```sql
SELECT c.NIF, v.CODE, v.DESCRIPTION, v.AVAILABLE, pm.ACCUMULATED
FROM TPAYMENTMEANS pm
JOIN TVOUCHERS v ON v.ID = pm.ID          -- padre e hija comparten ID
JOIN TCLIENTS c  ON pm.CLIENT_ID = c.ID;
```
```text
JPQL (bonos de un cliente): select v from Voucher v where v.client.nif.nif = ?1
```

**20. Resumen de bonos por cliente** (127). Es la consulta de tu `Voucher.getSummary`:

```sql
SELECT c.NIF, c.NAME, c.SURNAME,
       COUNT(*)                       AS EMITIDOS,
       SUM(v.AVAILABLE + pm.ACCUMULATED) AS TOTAL,
       SUM(v.AVAILABLE)               AS DISPONIBLE,
       SUM(pm.ACCUMULATED)            AS CONSUMIDO
FROM TPAYMENTMEANS pm
JOIN TVOUCHERS v ON v.ID = pm.ID
JOIN TCLIENTS c  ON pm.CLIENT_ID = c.ID
GROUP BY c.NIF, c.NAME, c.SURNAME
ORDER BY DISPONIBLE DESC;
```

**21. Tarjetas caducadas (comparar fechas con `CURRENT_DATE`)** (0 con tus datos)

```sql
SELECT c.NIF, cc.NUMBER, cc.TYPE, cc.VALIDTHRU
FROM TPAYMENTMEANS pm
JOIN TCREDITCARDS cc ON cc.ID = pm.ID
JOIN TCLIENTS c      ON pm.CLIENT_ID = c.ID
WHERE cc.VALIDTHRU < CURRENT_DATE;
```

### 6.10 Cálculos y expresiones

**22. Importe en repuestos de cada intervención (multiplicar columnas)** (5)

```sql
SELECT s.INTERVENTION_ID, SUM(s.QUANTITY * s.PRICE) AS REPUESTOS
FROM TSUBSTITUTIONS s
GROUP BY s.INTERVENTION_ID
ORDER BY REPUESTOS DESC
LIMIT 5;
```

**23. Mano de obra de cada intervención (minutos × precio/hora ÷ 60)** (675)

```sql
SELECT i.ID, i.MINUTES, vt.PRICE_PER_HOUR,
       i.MINUTES * vt.PRICE_PER_HOUR / 60 AS MANO_DE_OBRA
FROM TINTERVENTIONS i
JOIN TWORKORDERS w    ON i.WORKORDER_ID = w.ID
JOIN TVEHICLES v      ON w.VEHICLE_ID = v.ID
JOIN TVEHICLETYPES vt ON v.VEHICLETYPE_ID = vt.ID;
```
(Es lo que calcula `Intervention.getTotalAmount()` en Java: en este proyecto ese cálculo es **lógica de
negocio** y vive en el dominio, no en una consulta.)

**24. Facturas emitidas (sin pagar) de hace más de 30 días** (6)

```sql
SELECT f.NUMBER, f.DATE, f.TOTAL, f.STATE
FROM TINVOICES f
WHERE f.STATE = 'ISSUED'
  AND f.DATE < CURRENT_DATE - 30 DAY;
```

**25. Comprobación de integridad: facturas cuyos cargos no suman el total** (0: todo cuadra)

```sql
SELECT f.NUMBER, f.TOTAL, SUM(ch.CHARGE) AS COBRADO
FROM TINVOICES f
JOIN TCHARGES ch ON ch.INVOICE_ID = f.ID
GROUP BY f.NUMBER, f.TOTAL
HAVING SUM(ch.CHARGE) <> f.TOTAL;
```

**26. `CASE`: etiquetar filas** (3)

```sql
SELECT w.ID, w.STATE,
       CASE WHEN w.INVOICE_ID IS NULL THEN 'SIN FACTURA' ELSE 'FACTURADA' END AS SITUACION
FROM TWORKORDERS w
LIMIT 3;
```

### 6.11 Modificar datos (DML) y estructura (DDL)

Solo para el gestor de HSQLDB (en el proyecto los cambios de datos los hace JPA). Ojo: en el gestor
cada sentencia se confirma al momento.

```sql
-- Insertar (hay que dar ID y los campos de BaseEntity)
INSERT INTO TVEHICLETYPES (ID, NAME, PRICE_PER_HOUR, VERSION, ENTITYSTATE, CREATEDAT, UPDATEDAT)
VALUES ('vt-prueba', 'Caravana', 45.00, 1, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Modificar
UPDATE TSPAREPARTS SET PRICE = PRICE * 1.10 WHERE PRICE < 20;

-- Borrar (falla si otra tabla lo referencia por clave ajena)
DELETE FROM TVEHICLETYPES WHERE ID = 'vt-prueba';

-- Estructura
ALTER TABLE TSPAREPARTS ADD COLUMN STOCK INTEGER DEFAULT 0 NOT NULL;   -- añadir columna
ALTER TABLE TSPAREPARTS DROP COLUMN STOCK;                            -- quitarla
CREATE TABLE TNUEVA (...);                                            -- ver plantilla en 5.6
DROP TABLE TNUEVA;
SHUTDOWN;                                                             -- guarda todo en test.script
```

Equivalencias en JPA: `INSERT` = `repo.add(entidad)` (persist), `DELETE` = `repo.remove(entidad)`,
`UPDATE` = modificar la entidad gestionada (lo escribe el commit).

### 6.12 Practica: ejercicios con solución

Intenta escribirlos antes de mirar la solución.

1. **Número de vehículos de cada cliente, solo los que tienen más de 2.**
   ```sql
   SELECT c.NIF, COUNT(v.ID) AS VEHICULOS
   FROM TCLIENTS c JOIN TVEHICLES v ON v.CLIENT_ID = c.ID
   GROUP BY c.NIF
   HAVING COUNT(v.ID) > 2;
   ```
2. **Total de minutos trabajados por cada mecánico en órdenes de un tipo de vehículo concreto ('Van').**
   ```sql
   SELECT m.NIF, SUM(i.MINUTES) AS MINUTOS
   FROM TINTERVENTIONS i
   JOIN TMECHANICS m     ON i.MECHANIC_ID = m.ID
   JOIN TWORKORDERS w    ON i.WORKORDER_ID = w.ID
   JOIN TVEHICLES v      ON w.VEHICLE_ID = v.ID
   JOIN TVEHICLETYPES vt ON v.VEHICLETYPE_ID = vt.ID
   WHERE vt.NAME = 'Van'
   GROUP BY m.NIF;
   ```
3. **Repuesto más usado (suma de cantidades).**
   ```sql
   SELECT sp.CODE, SUM(s.QUANTITY) AS UNIDADES
   FROM TSUBSTITUTIONS s JOIN TSPAREPARTS sp ON s.SPAREPART_ID = sp.ID
   GROUP BY sp.CODE
   ORDER BY UNIDADES DESC
   LIMIT 1;
   ```
4. **Clientes que han pagado alguna factura con bono** (pista: `TCHARGES` → `TPAYMENTMEANS` con `DTYPE`).
   ```sql
   SELECT DISTINCT c.NIF
   FROM TCHARGES ch
   JOIN TPAYMENTMEANS pm ON ch.PAYMENTMEAN_ID = pm.ID
   JOIN TCLIENTS c       ON pm.CLIENT_ID = c.ID
   WHERE pm.DTYPE = 'Voucher';
   ```
5. **Pásala a JPQL**: "órdenes de trabajo de una matrícula".
   ```text
   select w from WorkOrder w where w.vehicle.licensePlate.licensePlate = ?1
   ```
   (Y recuerda: en el proyecto esto ni siquiera necesita consulta, porque se obtiene navegando
   `vehicle.getWorkOrders()`.)

---

## 7. La capa de servicio (aplicación)

### 7.1 Piezas

| Pieza | Responsabilidad | Ejemplo |
|---|---|---|
| **Interfaz de servicio** + DTO | Contrato (te la dan; Javadoc = especificación) | `ClientCrudService`, `ClientDto` |
| **Fachada** `XxxServiceImpl` | Solo reparte: **1 línea por método** | `return executor.execute(new AddClient(dto, nif));` |
| **Comando** | Un caso de uso = una clase `implements Command<T>` | `AddClient`, `DeleteClient` |
| **Repositorio** | Colección de entidades: `add`, `remove`, `findById`, `findByX` | `ClientRepository` / `ClientJpaRepository` |
| **Query** | Informes y datos derivados (conteos, sumas, DTOs) | `ClientQueries.hasPaidInvoices` |
| **DtoAssembler** | Convierte entidad ↔ DTO | `DtoAssembler.toDto(client)` |
| **Factories** | Decide qué implementación se usa | `Factories.repository.forClient()` |

### 7.2 El comando: estructura que debes memorizar

```java
public class DeleteMechanic implements Command<Void> {

    private String mechanicId;                                          // 1) datos de entrada
    private MechanicRepository repo = Factories.repository.forMechanic(); // 2) repos por factoría

    public DeleteMechanic(String mechanicId) {
        ArgumentChecks.isNotBlank(mechanicId, "The mechanic id is null or empty"); // 3) errores de programación → IAE
        this.mechanicId = mechanicId;
    }

    @Override
    public Void execute() throws BusinessException {
        Optional<Mechanic> om = repo.findById(mechanicId);             // 4) cargar
        BusinessChecks.exists(om, "The mechanic does not exist");      // 5) reglas → BusinessException
        Mechanic m = om.get();
        BusinessChecks.isTrue(m.getAssigned().isEmpty(), "The mechanic has work orders assigned");

        repo.remove(m);                                                // 6) actuar (delegar en el dominio)
        return null;                                                   // Void → return null
    }
}
```

Reglas de oro de los comandos (checklist):

* **Cortos** (< 10 líneas en `execute`): coordinan; la lógica está en el dominio.
* Repositorios **siempre** con `Factories.repository.forXxx()`.
* No imprimen nada, no abren transacciones, no capturan `RuntimeException`, no capturan `NoResultException`.
* Hacen **todos** los chequeos que dice el Javadoc de la interfaz, con la excepción indicada.

### 7.3 Qué excepción lanzar (cumplir el Javadoc)

| Situación | Excepción | Cómo |
|---|---|---|
| Argumento null/vacío ("IllegalArgumentException if...") | `IllegalArgumentException` | `ArgumentChecks` en el **constructor** del comando |
| Regla de negocio / culpa del usuario ("BusinessException if...") | `BusinessException` | `BusinessChecks` en `execute()` |
| No existe | `BusinessException` (o `Optional.empty()` si el Javadoc dice "returns empty") | `BusinessChecks.exists(optional, msg)` |
| Ya existe | `BusinessException` | `BusinessChecks.doesNotExist(optional, msg)` |
| Versión distinta | `BusinessException` | `BusinessChecks.hasVersion(entidad.getVersion(), dto.version, msg)` |

Métodos de `BusinessChecks`: `isTrue`, `isFalse`, `exists`, `doesNotExist`, `isNull`, `isNotNull`,
`isNotEmpty`, `isNotBlank`, `hasVersion`, `isEmpty(list)`.

> Truco: si una regla que lanzaría `IllegalStateException` en el dominio debe ser `BusinessException`
> en el servicio, **compruébala antes** en el comando con un método de consulta del dominio
> (p. ej. `wo.isOpen()`, `invoice.canBeSettledWith(...)`), y después llama al método que cambia el estado.

### 7.4 Repositorio vs Query

* **Repositorio**: devuelve **entidades** y sigue la metáfora de una colección (`add`, `remove`, `findBy...`).
  No hay `update`. No lanza `BusinessException`. No tiene lógica.
* **Query**: devuelve **datos derivados** (boolean, Long, DTOs de informe). P. ej. `hasCharges`,
  `getNextInvoiceNumber`, `getVouchersSummary`.

### 7.5 Mapa de tus casos de uso → clases

| Servicio | Comandos |
|---|---|
| `MechanicCrudService` | `AddMechanic`, `UpdateMechanic`, `DeleteMechanic`, `FindAllMechanics`, `FindMechanicById`, `FindMechanicByNif` |
| `InvoicingService` | `CreateInvoiceFor`, `FindNotInvoicedWorkOrdersByClientNif`, `FindWorkOrdersByClientNif`, `FindWorkOrdersByPlateNumber`, `FindInvoiceByNumber`, `FindPayMeansByClientNif`, `SettleInvoice` |
| `ClientCrudService` (tu UO) | `AddClient`, `UpdateClient`, `DeleteClient`, `FindAllClients`, `FindClientByNif`, `FindClientsRecommendedBy` |
| `VoucherGenerationService` (tu UO) | `GenerateVouchers`, `FindVouchersByClientNif`, `GetVoucherSummary` |
| `PaymentMeanCrudService` | `AddCard`, `AddVoucher`, `DeletePaymentMean`, `FindPaymentMeanById`, `FindPaymentMeansByClientNif`, `FindVoucherByCode`, `FindCreditCardByNumber` |

Lee `AddClient` y `GenerateVouchers` con calma: son tus casos de uso y te pueden preguntar por ellos.

---

## 8. Recetas paso a paso

### Receta A — Añadir un atributo a una entidad

1. **Dominio**: añade el campo `private`, inicialízalo en el **constructor completo** (con
   `ArgumentChecks`), encadena los demás constructores con `this(...)`, añade el getter y, si se puede
   modificar, un método de negocio (mejor que un setter "tonto"). Actualiza `toString()`. **No** lo
   metas en `equals/hashCode` salvo que forme parte de la identidad.
2. **Mapeo** (`orm.xml`): si la columna se llama igual que el atributo y no hay restricciones, **no hace
   falta nada**. Si tiene otro nombre, `unique`, enum o es un value type, añade el `<basic>`/`<embedded>`
   correspondiente (respetando el orden de elementos).
3. **BD**: `ALTER TABLE TX ADD COLUMN NOMBRE TIPO ...` (apartado 5.6).
4. **DTO y DtoAssembler**: si el servicio lo devuelve o lo recibe.
5. **Comandos**: los de alta y modificación lo usan; valida lo que diga el Javadoc.

### Receta B — Añadir una entidad nueva con asociaciones

1. **Clase** en `domain` que extiende `BaseEntity`: constructor sin argumentos de paquete
   (`// for JPA`), constructor completo con validaciones, getters, `_set`/`_get` de paquete,
   `equals/hashCode` sobre su identidad, `toString`.
2. **Asociación** en `Associations` (`link` / `unlink`), y en las clases del otro extremo la colección
   (`Set<X> xs = new HashSet<>()`), su `_getXs()` de paquete y su `getXs()` que devuelve copia.
3. **Mapeo**: `<entity>` con `<table>` (y `unique-constraint` si procede), `many-to-one` en la nueva y
   `one-to-many mapped-by` en el otro extremo.
4. **BD**: `CREATE TABLE ...` con `ID`, columnas de `BaseEntity`, atributos y FKs `<ATRIBUTO>_ID`.
5. **Repositorio** (interfaz en `application/repository`, implementación `XxxJpaRepository` en
   `infrastructure` que extiende `BaseJpaRepository<Xxx>`) y registrarlo en `PersistenceFactory` + `JpaRepositoryFactory`.

### Receta C — Implementar un caso de uso desde cero

1. Lee el **Javadoc** de la interfaz y apunta: entradas, salida, cada `BusinessException`, cada `IllegalArgumentException`.
2. ¿Necesitas buscar algo nuevo? → método en el **repositorio** (entidades) o en una **query** (datos
   derivados) + `<named-query>` en `orm.xml`.
3. Crea el **comando** (`implements Command<TipoRetorno>`): constructor con `ArgumentChecks`,
   `execute()` con `BusinessChecks` + llamada al dominio.
4. Crea/añade el método en la **fachada** `XxxServiceImpl` (1 línea).
5. Si el servicio es nuevo, **regístralo** en `JpaServicesFactoryImpl` (sustituye el
   `throw new NotYetImplementedException()` por `return new XxxServiceImpl();`).
6. Crea el **DtoAssembler** si hay que devolver DTOs.
7. **Prueba**: un test JUnit rápido (plantilla en el ejercicio 1) o los tests que te den.

### Receta D — Añadir una consulta

1. Método en la interfaz (`XxxRepository` o `XxxQueries`) con Javadoc corto.
2. `<named-query name="Entidad.nombreMetodo">` en `orm.xml` (con CDATA si usas `<`/`>`).
3. Implementación en `XxxJpaRepository` / `XxxJpaQueries` con `Jpa.getManager().createNamedQuery(...)`.
4. Si es una interfaz de queries nueva: añadir `forXxx()` en `QueryFactory` y `JpaQueryFactory`.

---

## 9. Ejercicios resueltos

Los tres ejercicios cubren exactamente lo que dijo el profesor. Se resolvieron y probaron sobre una
copia de tu proyecto (5 tests propios en verde + los 234 originales siguen pasando).

---

### Ejercicio 1 — Stock de repuestos: modificar dominio + mapeo + implementar `SparePartCrudService`

> **Enunciado.** La interfaz `SparePartCrudService` (ya está en tu esqueleto, sin implementar) usa un
> `SparePartDto` con `stock`, `minStock` y `maxStock`. El repuesto actual no tiene esos datos.
> Amplía el modelo, el mapeo (columnas `STOCK`, `MIN_STOCK`, `MAX_STOCK`) y la BD, e implementa los
> cuatro métodos del servicio cumpliendo su Javadoc.

**Lo que dice el Javadoc (resumen):**

* `create(dto)`: `BusinessException` si el código ya existe o precio/stock/min/max negativos;
  `IllegalArgumentException` si dto null o código/descripción null o vacíos.
* `update(dto)`: identifica por **código**; `BusinessException` si no existe, valores negativos o
  versión distinta; `IllegalArgumentException` si dto null o id/descripción vacíos.
* `delete(code)`: `BusinessException` si no existe o tiene sustituciones (las "supplies"/"orders" del
  Javadoc no existen en nuestro modelo); `IllegalArgumentException` si el código es null.
* `findByCode(code)`: `Optional.empty()` si no existe; `IllegalArgumentException` si null.

#### Paso 1 — Dominio (`SparePart.java`)

Añadimos los tres atributos, un **constructor completo** al que se encadenan los antiguos (¡no
romper los tests que usan `new SparePart(code)`!) y un método de negocio `update` que valida.

```java
public class SparePart extends BaseEntity {

	private String code;
	private String description;
	private Money unitPrice;
	private int stock;
	private int minStock;
	private int maxStock;

	SparePart() {
		// for JPA
	}

	public SparePart(String code) {
		this(code, "no-description", Money.ZERO);
	}

	public SparePart(String code, String description, Money price) {
		this(code, description, price, 0, 0, 0);          // encadenado al completo
	}

	public SparePart(String code, String description, Money price, int stock, int minStock, int maxStock) {
		ArgumentChecks.isNotBlank(code, "Code cannot be empty");
		this.code = code;
		update(description, price, stock, minStock, maxStock);  // reutiliza las validaciones
	}

	/**
	 * Changes every attribute except the code (identity)
	 */
	public void update(String description, Money price, int stock, int minStock, int maxStock) {
		ArgumentChecks.isNotBlank(description, "Description cannot be empty");
		ArgumentChecks.isNotNull(price, "Price cannot be null");
		ArgumentChecks.isTrue(price.isNonNegative(), "Price must be non-negative");
		ArgumentChecks.isTrue(stock >= 0, "Stock must be non-negative");
		ArgumentChecks.isTrue(minStock >= 0, "Min stock must be non-negative");
		ArgumentChecks.isTrue(maxStock >= 0, "Max stock must be non-negative");

		this.description = description;
		this.unitPrice = Money.calc(price).toMoneyRoundedToCents();
		this.stock = stock;
		this.minStock = minStock;
		this.maxStock = maxStock;
	}

	public int getStock() { return stock; }
	public int getMinStock() { return minStock; }
	public int getMaxStock() { return maxStock; }
	// ... getters existentes, equals/hashCode sobre code (sin cambios), toString con los nuevos campos
}
```

¿Por qué `IllegalArgumentException` aquí y `BusinessException` en el comando? El dominio protege su
integridad (programación defensiva); el comando traduce los errores **del usuario** a lo que pide el
Javadoc comprobándolos **antes** de llamar al dominio.

#### Paso 2 — Mapeo (`orm.xml`, entidad `SparePart`)

`stock` → columna `STOCK` (nombre por defecto, el `<basic>` es opcional). `minStock` y `maxStock`
tienen nombres distintos a los por defecto (`MINSTOCK`), así que hay que indicarlos:

```xml
<entity class="uo.ri.cws.domain.SparePart">
	<table name="TSPAREPARTS" />
	<attributes>
		<basic name="code">
			<column unique="true" />
		</basic>
		<basic name="stock" />
		<basic name="minStock">
			<column name="MIN_STOCK" />
		</basic>
		<basic name="maxStock">
			<column name="MAX_STOCK" />
		</basic>
		<embedded name="unitPrice">        <!-- los basic van ANTES que los embedded -->
			<attribute-override name="amount">
				<column name="PRICE" precision="19" scale="2" />
			</attribute-override>
		</embedded>
	</attributes>
</entity>
```

#### Paso 3 — Base de datos

```sql
ALTER TABLE TSPAREPARTS ADD COLUMN STOCK INTEGER DEFAULT 0 NOT NULL
ALTER TABLE TSPAREPARTS ADD COLUMN MIN_STOCK INTEGER DEFAULT 0 NOT NULL
ALTER TABLE TSPAREPARTS ADD COLUMN MAX_STOCK INTEGER DEFAULT 0 NOT NULL
SHUTDOWN
```

`DEFAULT 0` es importante: la tabla ya tiene 100 repuestos y un `int` de Java no admite `null`.

#### Paso 4 — Persistencia (repositorio + consulta)

Necesitamos buscar por código (devuelve entidad → **repositorio**) y saber si tiene sustituciones
(devuelve un boolean → **query**).

`application/repository/SparePartRepository.java`:

```java
public interface SparePartRepository extends Repository<SparePart> {

	/**
	 * @param code of the spare part
	 * @return the spare part with that code, or Optional.empty() if none
	 */
	Optional<SparePart> findByCode(String code);

}
```

`infrastructure/persistence/jpa/repository/SparePartJpaRepository.java`:

```java
public class SparePartJpaRepository
		extends BaseJpaRepository<SparePart>
		implements SparePartRepository {

	@Override
	public Optional<SparePart> findByCode(String code) {
		return Jpa.getManager()
				.createNamedQuery("SparePart.findByCode", SparePart.class)
				.setParameter(1, code)
				.getResultStream()
				.findFirst();
	}

}
```

`application/query/SparePartQueries.java` (nueva) y su implementación:

```java
public interface SparePartQueries {

	/**
	 * @return true if the spare part has been used in any substitution
	 */
	boolean hasSubstitutions(String sparePartId);

}
```

```java
public class SparePartJpaQueries implements SparePartQueries {

	@Override
	public boolean hasSubstitutions(String sparePartId) {
		return 0 < Jpa.getManager()
				.createNamedQuery("SparePart.countSubstitutions", Long.class)
				.setParameter(1, sparePartId)
				.getSingleResult();      // count siempre devuelve 1 fila → correcto
	}

}
```

Registrarla: en `QueryFactory` añade `SparePartQueries forSparePart();` y en `JpaQueryFactory`:

```java
@Override
public SparePartQueries forSparePart() {
	return new SparePartJpaQueries();
}
```

Consultas en `orm.xml` (con las demás `named-query`, antes de las entidades):

```xml
<named-query name="SparePart.findByCode">
	<query>
		select sp
		from SparePart sp
		where sp.code = ?1
	</query>
</named-query>

<named-query name="SparePart.countSubstitutions">
	<query>
		select count(s)
		from Substitution s
		where s.sparePart.id = ?1
	</query>
</named-query>
```

#### Paso 5 — Servicio: DTO assembler, comandos y fachada

Paquete nuevo `application/service/spare/crud`.

`DtoAssembler.java`:

```java
public class DtoAssembler {

	public static SparePartDto toDto(SparePart sp) {
		SparePartDto dto = new SparePartDto();
		dto.id = sp.getId();
		dto.version = sp.getVersion();
		dto.code = sp.getCode();
		dto.description = sp.getDescription();
		dto.price = sp.getPrice().amount();
		dto.stock = sp.getStock();
		dto.minStock = sp.getMinStock();
		dto.maxStock = sp.getMaxStock();
		return dto;
	}

}
```

`commands/SparePartDtoChecks.java` (comprobaciones compartidas por alta y modificación, para no repetir código):

```java
final class SparePartDtoChecks {

	private SparePartDtoChecks() {
	}

	static void checkAmounts(SparePartDto dto) throws BusinessException {
		BusinessChecks.isNotNull(dto.price, "The price is required");
		BusinessChecks.isTrue(dto.price.compareTo(BigDecimal.ZERO) >= 0, "The price cannot be negative");
		BusinessChecks.isTrue(dto.stock >= 0, "The stock cannot be negative");
		BusinessChecks.isTrue(dto.minStock >= 0, "The min stock cannot be negative");
		BusinessChecks.isTrue(dto.maxStock >= 0, "The max stock cannot be negative");
	}

}
```

`commands/AddSparePart.java`:

```java
public class AddSparePart implements Command<SparePartDto> {

	private SparePartDto dto;
	private SparePartRepository repo = Factories.repository.forSparePart();

	public AddSparePart(SparePartDto dto) {
		ArgumentChecks.isNotNull(dto, "The spare part dto cannot be null");
		ArgumentChecks.isNotBlank(dto.code, "The code cannot be null or empty");
		ArgumentChecks.isNotBlank(dto.description, "The description cannot be null or empty");
		this.dto = dto;
	}

	@Override
	public SparePartDto execute() throws BusinessException {
		SparePartDtoChecks.checkAmounts(dto);
		BusinessChecks.doesNotExist(repo.findByCode(dto.code), "There is already a spare part with that code");

		SparePart sp = new SparePart(dto.code, dto.description, Money.of(dto.price),
				dto.stock, dto.minStock, dto.maxStock);
		repo.add(sp);                       // persist → INSERT al hacer commit

		dto.id = sp.getId();                // el id lo genera Java (UUID), ya lo tenemos
		dto.version = sp.getVersion();
		return dto;
	}

}
```

`commands/UpdateSparePart.java` — fíjate: **no** hay `repo.update`; basta con modificar la entidad gestionada:

```java
public class UpdateSparePart implements Command<Void> {

	private SparePartDto dto;
	private SparePartRepository repo = Factories.repository.forSparePart();

	public UpdateSparePart(SparePartDto dto) {
		ArgumentChecks.isNotNull(dto, "The spare part dto cannot be null");
		ArgumentChecks.isNotBlank(dto.id, "The id cannot be null or empty");
		ArgumentChecks.isNotBlank(dto.code, "The code cannot be null or empty");
		ArgumentChecks.isNotBlank(dto.description, "The description cannot be null or empty");
		this.dto = dto;
	}

	@Override
	public Void execute() throws BusinessException {
		Optional<SparePart> osp = repo.findByCode(dto.code);
		BusinessChecks.exists(osp, "The spare part does not exist");
		SparePart sp = osp.get();
		BusinessChecks.hasVersion(sp.getVersion(), dto.version, "The spare part has been updated by another user");
		SparePartDtoChecks.checkAmounts(dto);

		sp.update(dto.description, Money.of(dto.price), dto.stock, dto.minStock, dto.maxStock);
		sp.updatedNow();
		return null;                        // dirty checking → UPDATE al hacer commit
	}

}
```

`commands/DeleteSparePart.java`:

```java
public class DeleteSparePart implements Command<Void> {

	private String code;
	private SparePartRepository repo = Factories.repository.forSparePart();
	private SparePartQueries queries = Factories.query.forSparePart();

	public DeleteSparePart(String code) {
		ArgumentChecks.isNotNull(code, "The code cannot be null");
		this.code = code;
	}

	@Override
	public Void execute() throws BusinessException {
		Optional<SparePart> osp = repo.findByCode(code);
		BusinessChecks.exists(osp, "The spare part does not exist");
		SparePart sp = osp.get();
		BusinessChecks.isFalse(queries.hasSubstitutions(sp.getId()), "The spare part has substitutions");

		repo.remove(sp);
		return null;
	}

}
```

`commands/FindSparePartByCode.java`:

```java
public class FindSparePartByCode implements Command<Optional<SparePartDto>> {

	private String code;
	private SparePartRepository repo = Factories.repository.forSparePart();

	public FindSparePartByCode(String code) {
		ArgumentChecks.isNotNull(code, "The code cannot be null");
		this.code = code;
	}

	@Override
	public Optional<SparePartDto> execute() throws BusinessException {
		return repo.findByCode(code).map(DtoAssembler::toDto);
	}

}
```

Fachada `SparePartCrudServiceImpl.java` (una línea por método):

```java
public class SparePartCrudServiceImpl implements SparePartCrudService {

	private CommandExecutor executor = Factories.executor.forExecutor();

	@Override
	public SparePartDto create(SparePartDto dto) throws BusinessException {
		return executor.execute(new AddSparePart(dto));
	}

	@Override
	public void delete(String code) throws BusinessException {
		executor.execute(new DeleteSparePart(code));
	}

	@Override
	public void update(SparePartDto dto) throws BusinessException {
		executor.execute(new UpdateSparePart(dto));
	}

	@Override
	public Optional<SparePartDto> findByCode(String code) throws BusinessException {
		return executor.execute(new FindSparePartByCode(code));
	}

}
```

#### Paso 6 — Registrar el servicio

En `JpaServicesFactoryImpl`:

```java
@Override
public SparePartCrudService forSparePartCrudService() {
	return new SparePartCrudServiceImpl();      // antes: throw new NotYetImplementedException();
}
```

#### Paso 7 — Probar (plantilla de test reutilizable)

En `cws_service_tests/test/...` (usa la BD de los tests y sus utilidades):

```java
class LabSparePartTests {

	private SparePartCrudService service;

	@BeforeEach
	void setUp() {
		Db.clearTables();                                   // BD vacía antes de cada test
		service = Factories.service.forSparePartCrudService();
	}

	@AfterEach
	void tearDown() {
		Factories.close();
	}

	private static SparePartDto dto(String code) {
		SparePartDto dto = new SparePartDto();
		dto.code = code;
		dto.description = "Filtro";
		dto.price = new BigDecimal("12.50");
		dto.stock = 10;
		dto.minStock = 2;
		dto.maxStock = 50;
		return dto;
	}

	@Test
	void fullLifecycle() throws BusinessException {
		service.create(dto("SP-1"));
		SparePartDto found = service.findByCode("SP-1").orElseThrow();
		assertEquals(10, found.stock);

		found.stock = 7;
		service.update(found);
		assertEquals(7, service.findByCode("SP-1").orElseThrow().stock);

		service.delete("SP-1");
		assertTrue(service.findByCode("SP-1").isEmpty());
	}

	@Test
	void businessErrors() throws BusinessException {
		service.create(dto("SP-2"));
		assertThrows(BusinessException.class, () -> service.create(dto("SP-2")));   // repetido

		SparePartDto negative = dto("SP-3");
		negative.stock = -1;
		assertThrows(BusinessException.class, () -> service.create(negative));     // negativo

		SparePartDto stale = service.findByCode("SP-2").orElseThrow();
		stale.version = stale.version - 1;
		assertThrows(BusinessException.class, () -> service.update(stale));        // versión

		assertThrows(BusinessException.class, () -> service.delete("NOPE"));       // no existe
	}
}
```

Ficheros tocados en este ejercicio: `SparePart.java`, `orm.xml`, BD, `SparePartRepository`,
`SparePartJpaRepository`, `SparePartQueries` (+Jpa), `QueryFactory`, `JpaQueryFactory`, paquete
`service/spare/crud` completo y `JpaServicesFactoryImpl`.

---

### Ejercicio 2 — Certificados: nueva entidad asociativa + mapeo + caso de uso

> **Enunciado.** Un mecánico puede estar **certificado** para reparar un tipo de vehículo, con una
> fecha. Un mecánico no puede tener dos certificados del mismo tipo. Crea la entidad `Certificate`
> (tabla `TCERTIFICATES`) e implementa un servicio con:
> `CertificateDto certify(String mechanicId, String vehicleTypeId)` (BusinessException si no existe el
> mecánico, no existe el tipo o ya está certificado) y
> `List<CertificateDto> findByVehicleTypeId(String vehicleTypeId)` (lista vacía si no existe).

Interfaz que te darían (en `cws_service_interfaces`):

```java
public interface CertificateService {

	CertificateDto certify(String mechanicId, String vehicleTypeId) throws BusinessException;

	List<CertificateDto> findByVehicleTypeId(String vehicleTypeId) throws BusinessException;

	public static class CertificateDto {
		public String id;
		public long version;
		public String mechanicId;
		public String mechanicNif;
		public String vehicleTypeId;
		public String vehicleTypeName;
		public LocalDate date;
	}
}
```

(y en `ServiceFactory`: `CertificateService forCertificateService();`)

#### Paso 1 — Dominio: la entidad

Es una **clase asociativa**: su identidad es el par (mecánico, tipo). Por eso el enlace se hace
**en el constructor** (excepción que explicó el profesor).

```java
/**
 * A mechanic is certified to repair a type of vehicle. Associative class:
 * its identity is the pair (mechanic, vehicleType).
 */
public class Certificate extends BaseEntity {

	private LocalDate date;
	private Mechanic mechanic;
	private VehicleType vehicleType;

	Certificate() {
		// for JPA
	}

	public Certificate(Mechanic mechanic, VehicleType vehicleType) {
		this(mechanic, vehicleType, LocalDate.now());
	}

	public Certificate(Mechanic mechanic, VehicleType vehicleType, LocalDate date) {
		ArgumentChecks.isNotNull(mechanic, "Mechanic cannot be null");
		ArgumentChecks.isNotNull(vehicleType, "Vehicle type cannot be null");
		ArgumentChecks.isNotNull(date, "Date cannot be null");

		this.date = date;
		Associations.Certifies.link(mechanic, this, vehicleType);
	}

	public LocalDate getDate() { return date; }
	public Mechanic getMechanic() { return mechanic; }
	public VehicleType getVehicleType() { return vehicleType; }

	void _setMechanic(Mechanic mechanic) { this.mechanic = mechanic; }
	void _setVehicleType(VehicleType vehicleType) { this.vehicleType = vehicleType; }

	@Override
	public int hashCode() {
		return Objects.hash(mechanic, vehicleType);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Certificate other = (Certificate) obj;
		return Objects.equals(mechanic, other.mechanic) && Objects.equals(vehicleType, other.vehicleType);
	}
	// toString ...
}
```

#### Paso 2 — Dominio: la asociación y los otros extremos

En `Associations`:

```java
public static class Certifies {

	/* package */ static void link(Mechanic mechanic, Certificate certificate, VehicleType vehicleType) {
		ArgumentChecks.isNotNull(mechanic);
		ArgumentChecks.isNotNull(certificate);
		ArgumentChecks.isNotNull(vehicleType);
		StateChecks.isTrue(certificate.getMechanic() == null, "Certificate already has a mechanic");
		StateChecks.isTrue(certificate.getVehicleType() == null, "Certificate already has a vehicle type");

		certificate._setMechanic(mechanic);              // primero los extremos de identidad
		certificate._setVehicleType(vehicleType);
		mechanic._getCertificates().add(certificate);    // después las colecciones (usan hashCode)
		vehicleType._getCertificates().add(certificate);
	}

	public static void unlink(Certificate certificate) {
		ArgumentChecks.isNotNull(certificate);
		Mechanic mechanic = certificate.getMechanic();
		VehicleType vehicleType = certificate.getVehicleType();
		StateChecks.isTrue(mechanic != null, "Certificate has no mechanic");

		mechanic._getCertificates().remove(certificate); // primero quitar de las colecciones
		vehicleType._getCertificates().remove(certificate);
		certificate._setMechanic(null);                  // después anular las referencias
		certificate._setVehicleType(null);
	}
}
```

> ¿Por qué ese orden? Un `HashSet` coloca el objeto según su `hashCode`, que depende de
> `mechanic` y `vehicleType`. Si lo metes en el set antes de asignarlos, o anulas las referencias antes
> de sacarlo, el set "pierde" el objeto.

En `Mechanic`:

```java
private Set<Certificate> certificates = new HashSet<>();

Set<Certificate> _getCertificates() {
	return certificates;
}

public Set<Certificate> getCertificates() {
	return new HashSet<>(certificates);
}

/**
 * @return true if the mechanic holds a certificate for that vehicle type
 */
public boolean isCertifiedFor(VehicleType vehicleType) {
	return certificates.stream()
			.anyMatch(c -> c.getVehicleType().equals(vehicleType));
}
```

En `VehicleType` lo mismo: `Set<Certificate> certificates`, `_getCertificates()` y `getCertificates()`.

#### Paso 3 — Mapeo

```xml
<!-- en Mechanic, junto a assigned -->
<one-to-many name="certificates" mapped-by="mechanic" />

<!-- en VehicleType, después del basic name y antes del embedded -->
<one-to-many name="certificates" mapped-by="vehicleType" />

<!-- entidad nueva (con las demás entidades, antes de los embeddables) -->
<entity class="uo.ri.cws.domain.Certificate">
	<table name="TCERTIFICATES">
		<unique-constraint>
			<column-name>MECHANIC_ID</column-name>
			<column-name>VEHICLETYPE_ID</column-name>
		</unique-constraint>
	</table>
	<attributes>
		<many-to-one name="mechanic" />        <!-- columna MECHANIC_ID por defecto -->
		<many-to-one name="vehicleType" />     <!-- columna VEHICLETYPE_ID por defecto -->
	</attributes>
</entity>
```

`date` no necesita mapeo (columna `DATE` por defecto, `LocalDate` → `DATE`).

#### Paso 4 — Base de datos

```sql
CREATE TABLE TCERTIFICATES (
	ID VARCHAR(255) NOT NULL PRIMARY KEY,
	CREATEDAT TIMESTAMP, DATE DATE, ENTITYSTATE VARCHAR(255),
	UPDATEDAT TIMESTAMP, VERSION BIGINT,
	MECHANIC_ID VARCHAR(255), VEHICLETYPE_ID VARCHAR(255),
	CONSTRAINT UNQ_TCERTIFICATES UNIQUE (MECHANIC_ID, VEHICLETYPE_ID),
	CONSTRAINT FK_TCERTIFICATES_MECHANIC_ID FOREIGN KEY (MECHANIC_ID) REFERENCES TMECHANICS (ID),
	CONSTRAINT FK_TCERTIFICATES_VEHICLETYPE_ID FOREIGN KEY (VEHICLETYPE_ID) REFERENCES TVEHICLETYPES (ID)
)
SHUTDOWN
```

#### Paso 5 — Repositorio

```java
public interface CertificateRepository extends Repository<Certificate> {
}

public class CertificateJpaRepository
		extends BaseJpaRepository<Certificate>
		implements CertificateRepository {
}
```

Registrar: `CertificateRepository forCertificate();` en `PersistenceFactory` y en `JpaRepositoryFactory`:

```java
@Override
public CertificateRepository forCertificate() {
	return new CertificateJpaRepository();
}
```

#### Paso 6 — Comandos, assembler y fachada (paquete `service/certificate`)

```java
public class DtoAssembler {

	public static CertificateDto toDto(Certificate c) {
		CertificateDto dto = new CertificateDto();
		dto.id = c.getId();
		dto.version = c.getVersion();
		dto.mechanicId = c.getMechanic().getId();
		dto.mechanicNif = c.getMechanic().getNif().nif();
		dto.vehicleTypeId = c.getVehicleType().getId();
		dto.vehicleTypeName = c.getVehicleType().getName();
		dto.date = c.getDate();
		return dto;
	}

	public static List<CertificateDto> toDtoList(Collection<Certificate> certificates) {
		return certificates.stream()
				.map(DtoAssembler::toDto)
				.toList();
	}

}
```

```java
public class CertifyMechanic implements Command<CertificateDto> {

	private String mechanicId;
	private String vehicleTypeId;
	private MechanicRepository mechanicRepo = Factories.repository.forMechanic();
	private VehicleTypeRepository vtRepo = Factories.repository.forVehicleType();
	private CertificateRepository certificateRepo = Factories.repository.forCertificate();

	public CertifyMechanic(String mechanicId, String vehicleTypeId) {
		ArgumentChecks.isNotBlank(mechanicId, "The mechanic id cannot be null or blank");
		ArgumentChecks.isNotBlank(vehicleTypeId, "The vehicle type id cannot be null or blank");
		this.mechanicId = mechanicId;
		this.vehicleTypeId = vehicleTypeId;
	}

	@Override
	public CertificateDto execute() throws BusinessException {
		Optional<Mechanic> om = mechanicRepo.findById(mechanicId);
		BusinessChecks.exists(om, "The mechanic does not exist");
		Optional<VehicleType> ovt = vtRepo.findById(vehicleTypeId);
		BusinessChecks.exists(ovt, "The vehicle type does not exist");
		BusinessChecks.isFalse(om.get().isCertifiedFor(ovt.get()), "The mechanic is already certified");

		Certificate certificate = new Certificate(om.get(), ovt.get());
		certificateRepo.add(certificate);
		return DtoAssembler.toDto(certificate);
	}

}
```

Para buscar **no hace falta consulta**: se navega la asociación (la checklist penaliza consultar lo
que se obtiene navegando):

```java
public class FindCertificatesByVehicleType implements Command<List<CertificateDto>> {

	private String vehicleTypeId;
	private VehicleTypeRepository repo = Factories.repository.forVehicleType();

	public FindCertificatesByVehicleType(String vehicleTypeId) {
		ArgumentChecks.isNotBlank(vehicleTypeId, "The vehicle type id cannot be null or blank");
		this.vehicleTypeId = vehicleTypeId;
	}

	@Override
	public List<CertificateDto> execute() throws BusinessException {
		return repo.findById(vehicleTypeId)
				.map(vt -> DtoAssembler.toDtoList(vt.getCertificates()))
				.orElse(List.of());
	}

}
```

Fachada:

```java
public class CertificateServiceImpl implements CertificateService {

	private CommandExecutor executor = Factories.executor.forExecutor();

	@Override
	public CertificateDto certify(String mechanicId, String vehicleTypeId) throws BusinessException {
		return executor.execute(new CertifyMechanic(mechanicId, vehicleTypeId));
	}

	@Override
	public List<CertificateDto> findByVehicleTypeId(String vehicleTypeId) throws BusinessException {
		return executor.execute(new FindCertificatesByVehicleType(vehicleTypeId));
	}

}
```

Y en `JpaServicesFactoryImpl`:

```java
@Override
public CertificateService forCertificateService() {
	return new CertificateServiceImpl();
}
```

> Consecuencia que debes mencionar si te preguntan: ahora `DeleteMechanic` debería impedir (o
> gestionar) borrar un mecánico con certificados, porque la FK de `TCERTIFICATES` lo impediría en la BD
> con una `PersistenceException`. Se añadiría
> `BusinessChecks.isTrue(m.getCertificates().isEmpty(), "The mechanic has certificates");`.

---

### Ejercicio 3 — Caso de uso desde cero sobre el modelo existente: asignar una orden de trabajo

> **Enunciado.** Implementa `void assign(String workOrderId, String mechanicId)` del servicio
> `AssignWorkOrderService`: asigna una orden **OPEN** a un mecánico **certificado** para el tipo del
> vehículo. `BusinessException` si no existe la orden, no existe el mecánico, la orden no está OPEN o el
> mecánico no está certificado. `IllegalArgumentException` si algún argumento es null o vacío.

Aquí **no hay que tocar el dominio ni el mapeo**: `WorkOrder.assignTo(mechanic)` ya existe (y enlaza
con `Associations.Assigns`), y la regla de certificación la añadimos en el ejercicio 2
(`Mechanic.isCertifiedFor`). Es el caso típico de "el comando coordina y el dominio hace el trabajo".

```java
public class AssignWorkOrder implements Command<Void> {

	private String workOrderId;
	private String mechanicId;
	private WorkOrderRepository woRepo = Factories.repository.forWorkOrder();
	private MechanicRepository mechanicRepo = Factories.repository.forMechanic();

	public AssignWorkOrder(String workOrderId, String mechanicId) {
		ArgumentChecks.isNotBlank(workOrderId, "The work order id cannot be null or blank");
		ArgumentChecks.isNotBlank(mechanicId, "The mechanic id cannot be null or blank");
		this.workOrderId = workOrderId;
		this.mechanicId = mechanicId;
	}

	@Override
	public Void execute() throws BusinessException {
		Optional<WorkOrder> owo = woRepo.findById(workOrderId);
		BusinessChecks.exists(owo, "The work order does not exist");
		Optional<Mechanic> om = mechanicRepo.findById(mechanicId);
		BusinessChecks.exists(om, "The mechanic does not exist");
		WorkOrder wo = owo.get();
		Mechanic m = om.get();

		BusinessChecks.isTrue(wo.isOpen(), "The work order is not open");         // si no, assignTo lanzaría IllegalStateException
		BusinessChecks.isTrue(m.isCertifiedFor(wo.getVehicle().getVehicleType()),
				"The mechanic is not certified for this vehicle type");

		wo.assignTo(m);                       // el dominio cambia el estado y enlaza
		return null;                          // no hay repo.update: el commit guarda los cambios
	}

}
```

Fachada y registro:

```java
public class AssignWorkOrderServiceImpl implements AssignWorkOrderService {

	private CommandExecutor executor = Factories.executor.forExecutor();

	@Override
	public void assign(String workOrderId, String mechanicId) throws BusinessException {
		executor.execute(new AssignWorkOrder(workOrderId, mechanicId));
	}

}
```

```java
// JpaServicesFactoryImpl
@Override
public AssignWorkOrderService forAssignWorkOrderService() {
	return new AssignWorkOrderServiceImpl();
}
```

Test que lo demuestra (usa los *fixtures* de los tests: `DbFixtures.aMechanic()`,
`aClient()`, `aVehicleForClient(id)`, `aWorkOrderOpenForVehicle(id)`):

```java
@Test
void assignRequiresCertificate() throws BusinessException {
	assertThrows(BusinessException.class, () -> assignment.assign(workOrder.id, mechanic.id)); // sin certificado

	certificates.certify(mechanic.id, vehicle.vehicleType_Id);
	assignment.assign(workOrder.id, mechanic.id);

	TWorkOrdersRecord reloaded = Db.findById(TWorkOrdersRecord.class, workOrder.id);
	assertEquals("ASSIGNED", reloaded.state);
	assertEquals(mechanic.id, reloaded.mechanic_Id);

	assertThrows(BusinessException.class, () -> assignment.assign(workOrder.id, mechanic.id)); // ya no está OPEN
}
```

---

### Mini-ejercicios para practicar solo (con pista)

1. **Añadir `minTrainingHours` a `VehicleType`** (el `VehicleTypeDto` del esqueleto ya lo tiene):
   atributo `int`, validar ≥ 0, `ALTER TABLE TVEHICLETYPES ADD COLUMN MINTRAININGHOURS INTEGER DEFAULT 0 NOT NULL`.
2. **`List<SparePartDto> findUnderStock()`**: repuestos con `stock < minStock`. Pista: named query
   `select sp from SparePart sp where sp.stock < sp.minStock` (¡con CDATA por el `<`!) en el repositorio.
3. **Anular el pago de una factura** `void unsettle(String invoiceId)`: comprobar que existe y que está
   PAID (`BusinessChecks.isTrue(invoice.isSettled(), ...)`) y llamar a `invoice.unSettle()`.
4. **Borrar un vehículo** `deleteVehicle(String plate)`: `BusinessException` si no existe o tiene
   órdenes de trabajo (`vehicle.getWorkOrders().isEmpty()`); `Associations.Owns.unlink(v)` y
   `Associations.Classifies.unlink(v)` antes de `repo.remove(v)`.

---

## 10. Errores típicos y cómo diagnosticarlos

| Síntoma / mensaje | Causa probable | Solución |
|---|---|---|
| `usuario no tiene privilegios suficientes o objeto no encontrado: XXX` | La columna o tabla `XXX` no existe en la BD | `ALTER TABLE`/`CREATE TABLE`, o corrige el nombre en `orm.xml` |
| `Object: ... is not a known Entity type` / `Unknown entity` | La clase no está declarada en `orm.xml` | Añade `<entity class="...">` |
| Error al arrancar compilando una named query | JPQL con un atributo que no existe, o `<`/`>` sin CDATA | Revisa nombres de atributos (no de columnas) y usa CDATA |
| `cvc-complex-type...` al leer `orm.xml` | Elementos en orden incorrecto | Respeta el orden: named-query → mapped-superclass → entity → embeddable; dentro de attributes: id, basic, version, many-to-one, one-to-many, one-to-one, embedded, transient |
| `violación de restricción de integridad: ... UNQ_...` | Valor repetido en columna única | Comprueba antes con `BusinessChecks.doesNotExist(repo.findByX(...))` |
| `violación de restricción de integridad: ... FK_...` al borrar | La entidad tiene hijos que la referencian | Comprueba que no tenga hijos o bórralos/desenlázalos antes |
| `NoResultException` | `getSingleResult()` en una consulta sin resultados | Usa `getResultStream().findFirst()` |
| `IllegalStateException` saliendo del servicio | El dominio rechaza la operación y el comando no lo comprobó antes | Añade el `BusinessChecks` previo con un método de consulta del dominio |
| El cambio "no se guarda" | Modificaste un DTO o un objeto no gestionado | Carga la entidad con el repositorio dentro del comando y modifícala |
| Un objeto nuevo no aparece en la BD | Falta `repo.add(...)` (y no hay cascada desde un padre) | Añádelo al repositorio |
| Los tests fallan con datos raros | La BD quedó sucia de una ejecución anterior | `4.setLegacy` (cuidado: borra tus `ALTER`) |
| `NotYetImplementedException` | El servicio no está registrado en `JpaServicesFactoryImpl` | Devuelve `new XxxServiceImpl()` |

---

## 11. Preguntas de "¿por qué...?" que te pueden hacer

**¿Por qué no hay método `update` en los repositorios?**
Porque siguen la metáfora de una colección: si sacas un objeto de una lista y lo cambias, no lo vuelves
a meter. JPA detecta los cambios de las entidades gestionadas (*dirty checking*) y hace el UPDATE en el commit.

**¿Por qué la fachada solo tiene una línea por método?**
Para que la lógica no se mezcle: la fachada solo oculta qué comando resuelve cada servicio y lo lanza
con el `CommandExecutor`, que es quien gestiona la transacción y las excepciones de forma centralizada.

**¿Qué hace el `CommandExecutor` si el comando lanza una excepción?**
Hace `rollback` de la transacción (no se guarda nada de lo hecho en ese comando), cierra el
`EntityManager` y relanza la excepción.

**¿Por qué el dominio no tiene anotaciones JPA?**
Porque el dominio no debe depender de la infraestructura. Llevar el mapeo al `orm.xml` es la
ampliación que hiciste; las clases quedan limpias.

**¿Por qué los value types son `record` y embeddables?**
Son inmutables y se comparan por valor; no tienen tabla propia, se guardan en columnas de la entidad
que los contiene. Validan su formato en el constructor.

**¿Diferencia entre `IllegalArgumentException`, `IllegalStateException` y `BusinessException`?**
IAE: argumento inválido (fallo del programador). ISE: operación no válida en el estado actual del
objeto (dominio). BusinessException: regla de negocio incumplida que hay que comunicar al usuario
(solo en la capa de servicio).

**¿Por qué los ids son UUID generados en Java?**
Así la entidad tiene id desde que se crea (antes de guardarse), y el comando puede devolverlo en el DTO
sin esperar a la BD.

**¿Qué es el campo `version`?**
Control de concurrencia optimista. JPA lo incrementa en cada actualización; si dos usuarios editan a la
vez, el segundo tiene una versión vieja y su cambio se rechaza.

**¿Por qué `mapped-by`?**
Indica que esa colección es el lado inverso: la FK está en la otra tabla (el `many-to-one`), así que JPA
no crea una tabla intermedia ni otra columna.

**¿Cuándo uso un repositorio y cuándo una query?**
Repositorio si devuelves entidades para trabajar con ellas; query si necesitas un dato derivado
(contar, sumar, un informe en DTO).

**¿Cuándo pongo el `link()` en el constructor?**
Solo si el otro extremo forma parte de la identidad de la entidad (clases asociativas) o si está dentro
de un agregado. En el resto de casos, desde el comando.

**¿Qué es brown field y por qué importa?**
Trabajar contra una BD que ya existe: el mapeo debe coincidir exactamente con sus tablas y columnas. Es
como se corrige; si tu mapeo solo funciona generando tablas (green field), la entrega no arranca.

---

## 12. Chuleta final

```text
NUEVO ATRIBUTO      → campo + constructor completo (encadenar los otros) + getter + método de negocio
                      → orm.xml solo si nombre/unique/enum/embeddable distinto
                      → ALTER TABLE T... ADD COLUMN ... DEFAULT ... ; SHUTDOWN
NUEVA ENTIDAD       → extends BaseEntity, X() {} de paquete, _set/_get, equals/hashCode identidad
                      → Associations.Nueva.link/unlink; colección en el otro lado (copia en getter)
                      → <entity><table/><attributes> many-to-one / one-to-many mapped-by </attributes></entity>
                      → CREATE TABLE con ID, CREATEDAT, UPDATEDAT, ENTITYSTATE, VERSION, X_ID
                      → Repository + JpaRepository + PersistenceFactory + JpaRepositoryFactory
NUEVO CASO DE USO   → leer Javadoc (BE vs IAE)
                      → Comando: ArgumentChecks en constructor, BusinessChecks en execute, delegar en dominio
                      → Fachada: return executor.execute(new Comando(...));
                      → JpaServicesFactoryImpl: return new XxxServiceImpl();
                      → DtoAssembler.toDto
NUEVA CONSULTA      → <named-query name="Entidad.metodo"> (CDATA si < >) + método repo/query
                      → Optional: getResultStream().findFirst() | lista: getResultList()
                      → count/max: getSingleResult()
NUNCA               → lógica en la fachada, System.out, catch RuntimeException, getSingleResult sin resultado,
                      filtrar en Java tras findAll, consultas fuera del orm.xml, tocar tests o interfaces
```

¡Mucha suerte en la prueba! Practica los mini-ejercicios del apartado 9 sobre una **copia** de tu
proyecto, así no estropeas la entrega.
