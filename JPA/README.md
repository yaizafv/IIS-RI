# CarWorkshop desde cero

Esta guía es para leer **antes** que `GUIA_PROYECTO_JPA.md`. No da nada por sabido salvo lo básico de
Java: qué es una clase, un objeto, un atributo y un método. Cada palabra técnica se explica la primera
vez que aparece, y todo se cuenta con **un único ejemplo** (los mecánicos del taller) que se va
completando poco a poco.

Léela en orden. Al final de cada parte hay un recuadro **"Lo importante"** con lo que deberías
quedarte. Si al llegar a uno no lo entiendes, vuelve atrás antes de seguir.

---

## Parte 1. ¿Qué problema resuelve este programa?

Imagina un taller de coches. Cada día pasan cosas que hay que **apuntar**:

* Llega un cliente nuevo → hay que apuntar su nombre, su NIF, su teléfono...
* Trae su coche → hay que apuntar la matrícula y de quién es.
* El coche tiene una avería → se abre una **orden de trabajo** ("cambiar los frenos").
* Un mecánico la arregla → se apunta qué mecánico, cuánto tiempo y qué piezas usó.
* Se le cobra al cliente → se hace una **factura** y se apunta cómo pagó.

Nuestro programa es la libreta del taller, pero en el ordenador. Tiene que hacer dos cosas:

1. **Guardar los datos** para que no se pierdan al apagar el ordenador.
2. **Aplicar las reglas del taller**. Por ejemplo, "no se puede facturar una avería que no está
   terminada" o "no se puede dar de alta dos clientes con el mismo NIF".

Las dos cosas las hacen piezas distintas del programa, y gran parte de esta asignatura trata de cómo
encajan esas piezas.

> **Lo importante:** el programa guarda datos del taller y vigila que se cumplan las reglas del taller.

---

## Parte 2. ¿Dónde se guardan los datos? La base de datos

Cuando un programa Java termina, todos sus objetos desaparecen de la memoria. Para que los datos
sobrevivan, se guardan en una **base de datos**: un programa especializado en guardar datos
ordenados. En este proyecto es **HSQLDB**, y sus datos están en el fichero
`cws_database/test.script`.

### 2.1 Tablas, filas y columnas

Una base de datos guarda los datos en **tablas**. Piensa en una hoja de Excel.

Así es la tabla de mecánicos, que se llama `TMECHANICS`. La T delante es una costumbre del proyecto
para saber que es una tabla:

| ID | NIF | NAME | SURNAME | VERSION |
|---|---|---|---|---|
| `a1b2...` | 11111111H | Ana | López | 1 |
| `c3d4...` | 22222222J | Luis | Pérez | 3 |

* Cada **fila** es un mecánico.
* Cada **columna** es un dato del mecánico (su NIF, su nombre...).
* La columna **ID** es un código único que identifica la fila, como el DNI de la fila. No se repite
  nunca. En este proyecto son códigos largos y aleatorios, llamados UUID, como
  `a1b2c3d4-e5f6-...`. Se le llama **clave primaria**.

### 2.2 Tablas que se apuntan entre sí: la clave ajena

Ahora la tabla de órdenes de trabajo, `TWORKORDERS` (simplificada):

| ID | DESCRIPTION | STATE | MECHANIC_ID |
|---|---|---|---|
| `w9...` | Cambiar frenos | ASSIGNED | `a1b2...` |
| `w8...` | Revisar aceite | OPEN | *(vacío)* |

Fíjate en la columna `MECHANIC_ID`. Su valor `a1b2...` es el ID de Ana en la otra tabla. Así la
base de datos apunta que **la orden "Cambiar frenos" está asignada a Ana**. Una columna que guarda el
ID de una fila de **otra** tabla se llama **clave ajena** (*foreign key*). Casi siempre termina en
`_ID`.

La segunda orden tiene `MECHANIC_ID` vacío: todavía no tiene mecánico. En bases de datos, "vacío" se
dice **NULL**.

### 2.3 SQL: el idioma de la base de datos

Para pedir o cambiar datos se usa un idioma llamado **SQL**. Por ejemplo:

```sql
SELECT NAME, SURNAME FROM TMECHANICS WHERE NIF = '11111111H';
```

En castellano: "dame el nombre y el apellido de la tabla de mecánicos, de la fila cuyo NIF es
11111111H". La respuesta sería `Ana | López`.

En este proyecto **casi nunca escribirás SQL**, por lo que veremos en la parte 4. Pero conviene saber
que la base de datos solo entiende esto.

> **Lo importante:** los datos se guardan en tablas (filas y columnas). Cada fila tiene un ID único.
> Una tabla apunta a otra guardando el ID de la otra fila en una columna `ALGO_ID`.

---

## Parte 3. ¿Cómo se representa lo mismo en Java? Las clases del dominio

En Java no pensamos en tablas, sino en **objetos**. El mecánico es una clase:

```java
public class Mechanic {
    private Nif nif;
    private FullName fullName;
    private Set<WorkOrder> assigned = new HashSet<>();   // las órdenes que tiene asignadas
    ...
}
```

Y la orden de trabajo:

```java
public class WorkOrder {
    private String description;
    private WorkOrderState state;
    private Mechanic mechanic;      // ← el mecánico asignado (un OBJETO, no un ID)
    ...
}
```

Compara con la tabla. En Java la orden **no guarda el ID** del mecánico: guarda **el objeto
mecánico** entero. Si tienes una orden `wo`, para saber el nombre de su mecánico escribes
`wo.getMechanic().getFullName()`. A esto de ir de un objeto a otro siguiendo sus atributos se le
llama **navegar**.

### 3.1 ¿Qué es "el dominio"?

Al conjunto de clases que representan las cosas del taller (`Mechanic`, `Client`, `Vehicle`,
`WorkOrder`, `Invoice`...) se le llama **modelo de dominio**. "Dominio" significa "el mundo real del
que trata el programa", en este caso un taller. Están en la carpeta
`cws_services_jpa/src/uo/ri/cws/domain/`.

Estas clases no solo guardan datos: también tienen **las reglas del taller**. Por ejemplo, en
`WorkOrder`:

```java
public void assignTo(Mechanic mechanic) {
    StateChecks.isTrue(isOpen(), "Work order is not open");   // regla: solo si está ABIERTA
    Associations.Assigns.link(mechanic, this);                 // apunta: este mecánico ↔ esta orden
    state = WorkOrderState.ASSIGNED;                           // cambia su estado
}
```

En castellano: "para asignar una orden a un mecánico, la orden tiene que estar abierta; si lo está,
se relacionan los dos y la orden pasa a estado asignada". **Esa es la idea central de la asignatura:
las reglas del negocio viven dentro de estas clases.**

### 3.2 Entidades y "value types"

Hay dos tipos de clases en el dominio:

* **Entidades**: cosas que tienen identidad propia y su propia tabla. Un mecánico, un cliente, una
  factura. Aunque dos mecánicos se llamen igual, son personas distintas.
* **Value types** ("tipos valor"): datos pequeños que solo importan por su valor, como un NIF, un
  email o una cantidad de dinero. Dos NIF con las mismas letras **son el mismo NIF**. Están en
  `domain/types/` y se encargan de comprobar que el dato es válido. Por ejemplo,
  `new Nif("123")` falla porque ese NIF no es válido. No tienen tabla propia: se guardan como
  columnas dentro de la tabla de la entidad. El `Nif` de un mecánico es la columna `NIF` de
  `TMECHANICS`.

Todas las entidades **heredan** de una clase `BaseEntity`, que les da lo que tienen todas: un `id`
(el que se convierte en la columna ID), la fecha de creación y un número de `version` (lo veremos más
adelante).

> **Lo importante:** en Java el taller se representa con clases como `Mechanic` y `WorkOrder`. Un
> objeto apunta a otro guardando el objeto, no un ID. Estas clases también contienen las reglas del
> taller. A todo esto se le llama modelo de dominio.

---

## Parte 4. El traductor: JPA

Tenemos un problema: Java piensa en objetos y la base de datos piensa en tablas. Alguien tiene que
traducir de un lado a otro:

```
   Objeto Java                                   Fila en la tabla TMECHANICS
   ─────────────                                 ───────────────────────────
   Mechanic                                       ID       = "a1b2..."
     id       = "a1b2..."        ◄── JPA ──►      NIF      = "11111111H"
     nif      = Nif("11111111H")                  NAME     = "Ana"
     fullName = FullName("Ana", "López")          SURNAME  = "López"
```

Ese traductor se llama **JPA** (Jakarta Persistence API). En realidad JPA es solo un **estándar**,
es decir, unas reglas sobre cómo debe funcionar el traductor. El programa concreto que hace la
traducción en este proyecto se llama **EclipseLink**. Cuando lees "el mapeador", se refiere a esto.

Gracias a JPA, en lugar de escribir SQL como:

```sql
INSERT INTO TMECHANICS (ID, NIF, NAME, SURNAME, ...) VALUES ('a1b2...', '11111111H', 'Ana', 'López', ...);
```

en Java escribes simplemente "guarda este objeto", y JPA genera el SQL por ti.

### 4.1 El diccionario del traductor: `orm.xml`

Para traducir, JPA necesita saber, por ejemplo, que la clase `Mechanic` se guarda en la tabla
`TMECHANICS`, o que el atributo `nif` va en la columna `NIF`. Eso se le dice en un fichero llamado
**`orm.xml`**, en `cws_services_jpa/src/META-INF/orm.xml`. A ese "diccionario" se le llama **el
mapeo**. ORM significa *Object-Relational Mapping*: correspondencia entre objetos y tablas.

Esta es la parte del mecánico, comentada línea a línea:

```xml
<entity class="uo.ri.cws.domain.Mechanic">       <!-- La clase Mechanic se guarda en la BD...  -->
    <table name="TMECHANICS" />                  <!-- ...en la tabla TMECHANICS              -->
    <attributes>
        <one-to-many name="assigned" mapped-by="mechanic" />
            <!-- El atributo "assigned" (sus órdenes) NO es una columna de esta tabla.
                 Es la otra cara de la relación: para rellenarlo, JPA busca en la tabla de
                 órdenes las que tienen MECHANIC_ID = mi ID. "mapped-by" dice cómo se llama
                 el atributo en la otra clase (WorkOrder.mechanic).                           -->
        <embedded name="nif">                     <!-- nif es un value type que va "incrustado"... -->
            <attribute-override name="nif">
                <column name="NIF" unique="true" /> <!-- ...en la columna NIF, que no se puede repetir -->
            </attribute-override>
        </embedded>
        <embedded name="fullName" />              <!-- nombre y apellido → columnas NAME y SURNAME -->
    </attributes>
</entity>
```

Y la parte de la orden de trabajo que apunta al mecánico:

```xml
<many-to-one name="mechanic" />
    <!-- "muchas órdenes pueden tener un mismo mecánico".
         JPA lo guarda en la columna MECHANIC_ID (nombre del atributo + "_ID").           -->
```

Casi todo lo demás JPA lo deduce solo. Si un atributo se llama `description`, lo busca en la columna
`DESCRIPTION`. **Solo hay que escribir en `orm.xml` lo que no se puede adivinar.**

Muchos proyectos ponen esta información como **anotaciones** dentro de las clases Java (`@Entity`,
`@Table`...). En tu proyecto está toda en el XML, para que las clases del dominio no dependan de JPA.
Es una de las ampliaciones de la checklist.

### 4.2 Una consecuencia: casi no hay SQL en el proyecto

Como JPA genera el SQL, tú trabajas con objetos. Cuando necesitas **buscar** algo, usas un idioma
parecido a SQL pero que habla de clases y atributos, llamado **JPQL**. También va en el `orm.xml`:

```xml
<named-query name="Mechanic.findByNif">
    <query>
        select m from Mechanic m where m.nif = ?1
    </query>
</named-query>
```

En castellano: "dame los objetos Mechanic cuyo atributo nif sea el primer dato que te pase". `?1`
significa "el primer parámetro". Fíjate en que dice `Mechanic` (la clase), no `TMECHANICS` (la
tabla). Esa es la diferencia con SQL.

> **Lo importante:** JPA traduce entre objetos y tablas. Le explicas cómo hacerlo en `orm.xml`.
> Las búsquedas se escriben en JPQL, que es como SQL pero con nombres de clases y atributos.

---

## Parte 5. Un viaje completo: dar de alta un mecánico

Ahora juntamos las piezas. Vamos a seguir paso a paso lo que pasa cuando el jefe del taller da de
alta a un mecánico nuevo. Es el centro de todo el proyecto: si entiendes este viaje, entiendes el
proyecto.

### 5.1 La analogía del restaurante

Piensa en un restaurante:

* El **cliente** pide un plato al **camarero**.
* El camarero **no cocina**: apunta el pedido y lo pasa a cocina.
* En cocina, un **cocinero** prepara ese plato siguiendo una **receta**.
* El cocinero coge los ingredientes de la **despensa**.
* El **jefe de cocina** vigila que cada plato salga completo o no salga. No se sirve medio plato.

En el proyecto:

| Restaurante | Proyecto | Dónde está |
|---|---|---|
| Cliente que pide | La pantalla (menú) del jefe del taller | `cws_client_manager` (no la tocas) |
| Camarero | El **servicio** (fachada): `MechanicCrudServiceImpl` | `application/service/mechanic/crud/` |
| Receta + cocinero | El **comando**: `AddMechanic` | `.../mechanic/crud/commands/` |
| Jefe de cocina | El **ejecutor de comandos**: `JpaCommandExecutor` | `infrastructure/persistence/jpa/executor/` |
| Despensa | El **repositorio**: `MechanicRepository` | `application/repository/` |
| Los ingredientes | Los objetos del **dominio**: `Mechanic` | `domain/` |

### 5.2 Paso 1: la pantalla pide el servicio

En la pantalla, el jefe escribe el NIF, el nombre y el apellido. La pantalla los mete en un objeto
"caja" y llama al servicio. Esa caja es un **DTO**:

```java
MechanicDto dto = new MechanicDto();
dto.nif = "11111111H";
dto.name = "Ana";
dto.surname = "López";

Factories.service.forMechanicCrudService().create(dto);
```

* **DTO** (*Data Transfer Object*) es una clase que solo sirve para **transportar datos** entre capas.
  Solo tiene atributos públicos y ninguna regla. Así la pantalla nunca toca directamente los objetos
  del dominio.
* `Factories` es una clase que dice **qué clase concreta** usar para cada cosa. Cuando la pantalla
  pide "el servicio de mecánicos", `Factories` le da un `MechanicCrudServiceImpl`. La pantalla no
  necesita saber cómo se llama la clase que hace el trabajo.
* **Interfaz** (`MechanicCrudService`): una lista de métodos que alguien promete implementar, sin
  decir cómo. La pantalla solo conoce la interfaz. Encima de cada método hay un comentario, llamado
  **Javadoc**, que dice exactamente qué debe hacer y qué errores debe dar. **Ese comentario es el
  enunciado que tu código tiene que cumplir.**

### 5.3 Paso 2: el camarero (servicio, también llamado "fachada")

```java
public class MechanicCrudServiceImpl implements MechanicCrudService {

    private CommandExecutor executor = Factories.executor.forExecutor();

    @Override
    public MechanicDto create(MechanicDto dto) throws BusinessException {
        return executor.execute(new AddMechanic(dto));
    }
    ...
}
```

Solo hace una cosa: crea el comando que sabe hacer ese trabajo (`new AddMechanic(dto)`) y se lo da al
ejecutor (`executor.execute(...)`). **Una línea por método, sin más lógica.** Se llama **fachada**
porque es "la cara visible" que esconde todo lo de detrás.

### 5.4 Paso 3: el jefe de cocina (el ejecutor)

```java
public <T> T execute(Command<T> cmd) throws BusinessException {
    EntityManager mapper = Jpa.createEntityManager();   // (a) abre una "sesión" con JPA
    try {
        EntityTransaction trx = mapper.getTransaction();
        trx.begin();                                    // (b) empieza la transacción
        try {
            T res = cmd.execute();                      // (c) ejecuta el comando
            trx.commit();                               // (d) todo bien: GUARDA en la BD
            return res;
        } catch (Exception ex) {
            if (trx.isActive()) {
                trx.rollback();                         // (e) algo falló: NO guardes nada
            }
            throw ex;
        }
    } finally {
        if (mapper.isOpen()) {
            mapper.close();                             // (f) cierra la sesión
        }
    }
}
```

Aquí aparecen dos palabras clave:

* **Transacción**: un grupo de cambios que se hacen **todos o ninguno**. Si un comando da de alta un
  cliente y además su forma de pago, y la segunda parte falla, la primera tampoco se guarda. Es el
  "no se sirve medio plato".
  * `commit` = "confirmo, guárdalo todo".
  * `rollback` = "deshazlo todo".
* **EntityManager**: el objeto de JPA con el que hablas durante esa transacción. Va apuntando qué
  objetos has cargado o creado y qué has cambiado en ellos. Lo que tiene apuntado se llama
  **contexto de persistencia**. En el proyecto se consigue con `Jpa.getManager()`.

Este código ya lo daba el esqueleto. Lo importante es saber **qué hace**: cada llamada a un servicio
es **una transacción**, y si el comando lanza un error, no se guarda nada.

### 5.5 Paso 4: el cocinero (el comando)

```java
public class AddMechanic implements Command<MechanicDto> {

    private MechanicDto dto;
    private MechanicRepository repo = Factories.repository.forMechanic();       // ① la despensa

    public AddMechanic(MechanicDto dto) {                                         // ② comprobaciones básicas
        ArgumentChecks.isNotNull(dto, "The mechanic dto is null");
        ArgumentChecks.isNotBlank(dto.nif, "The mechanic nif is null or empty");
        ArgumentChecks.isNotBlank(dto.name, "The mechanic name is null or empty");
        ArgumentChecks.isNotBlank(dto.surname, "The mechanic surname is null or empty");
        this.dto = dto;
    }

    @Override
    public MechanicDto execute() throws BusinessException {
        BusinessChecks.isTrue(Nif.isValid(dto.nif), "The nif is not valid");     // ③ reglas del negocio
        Nif nif = new Nif(dto.nif);
        BusinessChecks.doesNotExist(repo.findByNif(nif),
                "There is already a mechanic with the same nif");

        Mechanic m = new Mechanic(nif, new FullName(dto.name, dto.surname));     // ④ crear el objeto
        repo.add(m);                                                             // ⑤ meterlo en la despensa

        dto.id = m.getId();                                                      // ⑥ devolver el resultado
        dto.version = m.getVersion();
        return dto;
    }
}
```

1. **①** Pide el repositorio a `Factories`, igual que la pantalla pedía el servicio.
2. **②** El **constructor** comprueba que no falte nada. Si falta algo, es un fallo de quien llamó
   mal al método (un fallo de programación).
3. **③** `execute()` comprueba **las reglas del taller**: el NIF tiene que tener un formato válido y no
   puede haber ya un mecánico con ese NIF. `repo.findByNif(nif)` busca en la base de datos (usando la
   consulta JPQL `Mechanic.findByNif` de la parte 4).
4. **④** Crea el objeto `Mechanic`. Aquí todavía **solo existe en memoria**.
5. **⑤** `repo.add(m)` le dice a JPA "a partir de ahora vigila este objeto y guárdalo".
6. **⑥** Devuelve el DTO con el ID que se generó.

Cuando el comando termina, el ejecutor hace `commit` (paso 3) y **en ese momento** JPA escribe el
`INSERT` en la tabla `TMECHANICS`.

A una clase que representa **una acción concreta** ("añadir mecánico", "borrar mecánico") se le llama
**comando**. Todos implementan la interfaz `Command`, que solo tiene el método `execute()`. Un caso de
uso es un comando.

### 5.6 Paso 5: la despensa (el repositorio)

```java
public interface MechanicRepository extends Repository<Mechanic> {
    Optional<Mechanic> findByNif(Nif nif);
    List<Mechanic> findAll();
}
```

Un **repositorio** es "el sitio donde están guardados los mecánicos". Se usa como una lista de Java:
`add` (meter), `remove` (sacar), `findById` / `findByNif` (buscar). Por dentro, la clase
`MechanicJpaRepository` usa JPA:

```java
public Optional<Mechanic> findByNif(Nif nif) {
    return Jpa.getManager()                                    // el EntityManager de esta transacción
            .createNamedQuery("Mechanic.findByNif", Mechanic.class)  // la consulta del orm.xml
            .setParameter(1, nif)                              // ?1 = el nif
            .getResultStream()
            .findFirst();                                      // el primero, o "nada"
}
```

* **`Optional`** es una caja que puede contener un objeto o estar vacía. Se usa para decir "puede que
  no exista" sin devolver `null`. `BusinessChecks.exists(optional, ...)` comprueba que la caja tenga
  algo.
* Ojo: **no existe un método `update`**. Si quieres cambiar un mecánico, lo buscas con el repositorio
  y lo cambias con sus métodos (`m.setFullName(...)`). JPA se da cuenta de que ha cambiado y, al hacer
  `commit`, lo guarda solo. Esto pasa porque el objeto está **gestionado**: lo cargó JPA en esta
  transacción, así que lo está vigilando.

### 5.7 El viaje completo, de un vistazo

```
Pantalla ── create(dto) ──► MechanicCrudServiceImpl (fachada: 1 línea)
                                   │ executor.execute(new AddMechanic(dto))
                                   ▼
                           JpaCommandExecutor: abre sesión JPA + empieza transacción
                                   │
                                   ▼
                           AddMechanic.execute():
                               comprueba reglas ─► repo.findByNif ─► JPA ─► SELECT en la BD
                               new Mechanic(...)     (dominio)
                               repo.add(m)           ─► JPA lo apunta
                                   │
                                   ▼
                           JpaCommandExecutor: commit ─► JPA ─► INSERT INTO TMECHANICS
                                   │
Pantalla ◄──── dto con el id ──────┘
```

### 5.8 ¿Por qué tantas piezas? (la "arquitectura")

Podría hacerse todo en un solo método, pero se separa en **capas** para que cada pieza tenga un solo
trabajo y se pueda cambiar sin romper las demás:

* La **pantalla** solo habla con el servicio. Si mañana se hace una web, el resto no cambia.
* El **dominio** (las reglas) no sabe nada de bases de datos ni de JPA. Si se cambiara de base de
  datos, las reglas seguirían igual.
* El **comando** coordina: comprueba, busca en repositorios y pide al dominio que haga el trabajo.
* La **persistencia** (repositorios, JPA, `orm.xml`) solo sabe guardar y buscar.

A esta forma de organizar el código se le llama **arquitectura hexagonal**. Su regla de oro: **el
dominio está en el centro y no depende de nadie; los demás dependen de él.**

> **Lo importante:** pantalla → servicio (1 línea) → ejecutor (transacción) → comando (comprueba,
> usa repositorios y el dominio) → JPA escribe en la base de datos al hacer commit. Si algo falla,
> no se guarda nada.

---

## Parte 6. Los tres tipos de error

En el código verás tres maneras de "quejarse". Es importante no mezclarlas:

| Error | Quién lo lanza | Qué significa | Ejemplo del día a día |
|---|---|---|---|
| `IllegalArgumentException` | `ArgumentChecks` (en constructores) | "Me has llamado mal": falta un dato o es absurdo. Es culpa de un **programador** | Pedir un plato sin decir cuál |
| `IllegalStateException` | `StateChecks` (en el dominio) | "Ahora no se puede hacer esto" | Pedir que te cobren un plato que aún no te han servido |
| `BusinessException` | `BusinessChecks` (en los comandos) | Una **regla del negocio** no se cumple y hay que explicárselo al **usuario** | "Ya existe un cliente con ese NIF" |

Reglas sencillas:

* En el **constructor del comando** → `ArgumentChecks` (datos que faltan).
* En el **`execute()` del comando** → `BusinessChecks` (reglas del negocio).
* El **dominio** nunca lanza `BusinessException`. Usa `ArgumentChecks` y `StateChecks`.
* Si el dominio fuera a quejarse con `IllegalStateException` pero el Javadoc pide `BusinessException`,
  el comando **lo comprueba antes**. Por ejemplo: `BusinessChecks.isTrue(wo.isOpen(), "...")` y solo
  después `wo.assignTo(m)`.

**¿Cómo sabes cuál usar?** Lo dice el Javadoc de la interfaz del servicio:
`@throws BusinessException if ...` y `@throws IllegalArgumentException if ...`.

> **Lo importante:** faltan datos → `ArgumentChecks` en el constructor. Reglas del negocio →
> `BusinessChecks` en `execute()`. El Javadoc te dice cuál toca.

---

## Parte 7. Relaciones entre objetos: por qué existe `Associations`

Volvamos a "Ana tiene asignada la orden de frenos". En Java eso está **apuntado en dos sitios**:

* La orden sabe quién es su mecánico: `wo.mechanic = Ana`.
* Ana sabe qué órdenes tiene: `Ana.assigned` contiene la orden.

Esto se llama relación **bidireccional**: se puede navegar en los dos sentidos. El peligro es apuntarlo
en un sitio y olvidarlo en el otro, porque entonces el programa se contradice. Es como una amistad:
si Ana dice que es amiga de Luis, Luis también tiene que decirlo.

Por eso existe la clase `Associations`, con un método `link` (enlazar) y `unlink` (desenlazar) para
cada relación, que **apunta los dos lados a la vez**:

```java
public static class Assigns {
    static void link(Mechanic mechanic, WorkOrder workOrder) {
        workOrder._setMechanic(mechanic);          // lado de la orden
        mechanic._getAssigned().add(workOrder);    // lado del mecánico
    }
    static void unlink(WorkOrder workOrder) {
        Mechanic mechanic = workOrder.getMechanic();
        mechanic._getAssigned().remove(workOrder);
        workOrder._setMechanic(null);
    }
}
```

Los métodos que empiezan por guion bajo (`_setMechanic`, `_getAssigned`) **no son públicos**: solo
`Associations` debe usarlos, para que nadie toque un lado sin el otro. Los getters públicos de las
listas (`getAssigned()`) devuelven **una copia**, para que nadie pueda cambiar la lista original desde
fuera.

**¿Quién llama a `link`?** Normalmente el **comando**. Hay dos excepciones, en las que se llama desde
el constructor o desde un método de la propia entidad:

* Cuando el otro objeto es **parte de la identidad**. Por ejemplo, un cargo (`Charge`) es "el pago de
  *esta* factura con *este* medio de pago": sin los dos, no existe.
* Cuando están dentro de un grupo de objetos que gestiona uno "jefe", llamado **agregado**. Por
  ejemplo, una orden de trabajo gestiona sus intervenciones.

> **Lo importante:** una relación se apunta en los dos objetos. Para no olvidar ninguno, siempre se
> usa `Associations.X.link(...)` / `unlink(...)`.

---

## Parte 8. Leer uno de tus casos de uso: dar de alta un cliente

Ya tienes todas las piezas para leer `AddClient`, uno de los casos de uso de tu UO:

```java
@Override
public ClientDto execute() throws BusinessException {
    ClientDtoChecks.checkFormats(dto);                        // ¿NIF, email y teléfono con formato válido?
    Nif nif = new Nif(dto.nif);
    BusinessChecks.doesNotExist(clientRepo.findByNif(nif),   // regla: no puede repetirse el NIF
            "There is already a client with the same nif");

    Client client = DtoAssembler.toClient(dto);              // DTO → objeto del dominio
    clientRepo.add(client);                                  // que JPA lo guarde
    pmRepo.add(new Cash(client));                            // regla: todo cliente nuevo tiene "efectivo"
    if (recommenderNif != null) {                            // si alguien le recomendó...
        addRecommendation(client);                           // ...se apunta la recomendación
    }

    dto.id = client.getId();
    dto.version = client.getVersion();
    return dto;
}
```

* `DtoAssembler` es una clase que **convierte** entre DTO y objeto del dominio, en los dos sentidos.
* `new Cash(client)` crea la forma de pago "efectivo" y, dentro de su constructor, la enlaza con el
  cliente. Es la primera excepción de la parte 7: el cliente forma parte de la identidad del efectivo.
* Todo esto ocurre en **una transacción**. Si la recomendación fallara, tampoco se guardaría el
  cliente.

Prueba a leer ahora `DeleteClient` y `UpdateClient` en la carpeta `application/service/client/crud/commands/`.
Tienen la misma forma.

> **Lo importante:** todos los comandos tienen la misma forma: comprobar → buscar → crear o cambiar
> objetos del dominio → (repo.add / repo.remove) → devolver.

---

## Parte 9. Tu primera ampliación, muy despacio

Supón que el enunciado dice: **"Añade a los mecánicos una especialidad (por ejemplo "motor" o
"chapa")"**. Vamos paso a paso y explicando por qué.

### Paso 1. El dominio: añadir el dato a la clase

Primero se piensa en Java, porque la regla del taller vive aquí. En `Mechanic.java`:

```java
private String specialty = "general";      // nuevo atributo, con un valor por defecto

public String getSpecialty() {
    return specialty;
}

public void changeSpecialty(String specialty) {
    ArgumentChecks.isNotBlank(specialty, "Specialty cannot be empty");   // regla: no puede estar vacía
    this.specialty = specialty;
}
```

¿Por qué `changeSpecialty` con una comprobación, y no un `setSpecialty` sin más? Porque la clase del
dominio **protege sus reglas**: nadie puede dejarle una especialidad vacía.

### Paso 2. El mapeo: ¿hay que tocar `orm.xml`?

El atributo se llama `specialty`, así que JPA lo buscará en una columna `SPECIALTY`. Si la columna se
llama así, **no hay que escribir nada** en el `orm.xml` (parte 4.1: solo se escribe lo que no se
puede adivinar).

Si el enunciado dijera que la columna se llama `SPECIALITY_NAME`, añadirías dentro de
`<attributes>` de `Mechanic`:

```xml
<basic name="specialty">
    <column name="SPECIALITY_NAME" />
</basic>
```

`<basic>` significa "atributo normal" (un texto, un número, una fecha), no una relación.

### Paso 3. La base de datos: crear la columna

La tabla `TMECHANICS` **no tiene** la columna `SPECIALTY`. Si ejecutas el programa sin crearla, falla
con un error del tipo *"objeto no encontrado: SPECIALTY"*. Por eso hay que añadirla con SQL:

```sql
ALTER TABLE TMECHANICS ADD COLUMN SPECIALTY VARCHAR(255) DEFAULT 'general';
```

"Modifica la tabla TMECHANICS añadiendo una columna SPECIALTY de texto, que valga 'general' en los
mecánicos que ya existían". Cómo ejecutarlo está en `GUIA_PROYECTO_JPA.md`, apartado 5.6 (con
`1.startup.bat`, `2.runManagerSwing.bat` y `SHUTDOWN` al final).

> ¿Por qué hay que hacer esto a mano? Porque se trabaja en **brown field**: la base de datos ya
> existe y el programa se tiene que adaptar a ella, no al revés.

### Paso 4. El servicio: usar el dato en un caso de uso

Supón que también te piden **"un servicio para cambiar la especialidad de un mecánico"**. Te darían
la interfaz, y tú escribirías el comando con la misma forma de siempre:

```java
public class ChangeSpecialty implements Command<Void> {

    private String mechanicId;
    private String specialty;
    private MechanicRepository repo = Factories.repository.forMechanic();

    public ChangeSpecialty(String mechanicId, String specialty) {
        ArgumentChecks.isNotBlank(mechanicId, "The mechanic id cannot be empty");   // faltan datos
        ArgumentChecks.isNotBlank(specialty, "The specialty cannot be empty");
        this.mechanicId = mechanicId;
        this.specialty = specialty;
    }

    @Override
    public Void execute() throws BusinessException {
        Optional<Mechanic> om = repo.findById(mechanicId);                  // buscar
        BusinessChecks.exists(om, "The mechanic does not exist");          // regla: tiene que existir

        om.get().changeSpecialty(specialty);                               // el dominio hace el cambio
        return null;                                                       // no hay "update": JPA lo guarda al hacer commit
    }
}
```

Y en la fachada, una línea:

```java
@Override
public void changeSpecialty(String mechanicId, String specialty) throws BusinessException {
    executor.execute(new ChangeSpecialty(mechanicId, specialty));
}
```

`Command<Void>` significa "este comando no devuelve nada"; por eso termina en `return null`.

**Eso es todo.** Los ejercicios grandes de `GUIA_PROYECTO_JPA.md` (apartado 9) son esto mismo con más
piezas: varios atributos, una entidad nueva, una consulta nueva...

> **Lo importante:** cualquier ampliación sigue el mismo orden: 1) clase del dominio, 2) `orm.xml`
> si hace falta, 3) base de datos si hay columnas o tablas nuevas, 4) comando + fachada.

---

## Parte 10. Glosario (para cuando te pierdas)

| Palabra | Qué es, en una frase |
|---|---|
| **Agregado** | Grupo de objetos que gestiona uno "jefe" (la orden de trabajo gestiona sus intervenciones). |
| **Associations** | Clase que enlaza y desenlaza objetos relacionados, apuntando los dos lados a la vez. |
| **BaseEntity** | Clase padre de todas las entidades; les da `id`, `version` y fechas. |
| **Base de datos (BD)** | Programa que guarda los datos en tablas para que no se pierdan. Aquí, HSQLDB. |
| **Brown field** | La base de datos ya existe y el programa se adapta a ella. Es como se corrige. |
| **BusinessException** | Error de una regla del negocio que hay que comunicar al usuario. |
| **Clave ajena (FK)** | Columna que guarda el ID de una fila de otra tabla (`MECHANIC_ID`). |
| **Clave primaria (PK)** | Columna que identifica cada fila sin repetirse (`ID`). |
| **Comando** | Clase que hace un caso de uso concreto (`AddMechanic`). Método `execute()`. |
| **Commit / rollback** | Confirmar y guardar todos los cambios / deshacerlos todos. |
| **Contexto de persistencia** | Lo que JPA tiene "apuntado" durante una transacción: objetos cargados y cambios. |
| **DTO** | Clase "caja" que solo transporta datos entre la pantalla y el servicio. |
| **DtoAssembler** | Clase que convierte entre DTO y objetos del dominio. |
| **EclipseLink** | El programa concreto que hace de JPA en este proyecto ("el mapeador"). |
| **Entidad** | Objeto del dominio con identidad propia y su propia tabla (Mechanic, Client...). |
| **EntityManager** | Objeto de JPA con el que se trabaja en una transacción (`Jpa.getManager()`). |
| **Factories** | Clase que decide qué implementación concreta se usa para cada cosa. |
| **Fachada / servicio** | La clase que recibe las peticiones de la pantalla y las pasa a un comando en una línea. |
| **Gestionado (managed)** | Objeto que JPA está vigilando en esta transacción: si cambia, lo guarda solo. |
| **Interfaz** | Lista de métodos prometidos, sin código. La pantalla solo conoce interfaces. |
| **Javadoc** | El comentario encima de cada método de la interfaz: es la especificación que debes cumplir. |
| **JPA** | El estándar de Java para traducir entre objetos y tablas. |
| **JPQL** | Lenguaje de consultas parecido a SQL, pero con nombres de clases y atributos. |
| **link / unlink** | Métodos de `Associations` para relacionar o separar dos objetos. |
| **Mapeo** | Las instrucciones de cómo se corresponde cada clase con su tabla (en `orm.xml`). |
| **Modelo de dominio** | Las clases que representan el taller y sus reglas (carpeta `domain`). |
| **Navegar** | Ir de un objeto a otro por sus atributos (`wo.getMechanic().getNif()`). |
| **NULL** | "Vacío" en la base de datos. |
| **Optional** | Caja que puede tener un objeto o estar vacía ("puede que no exista"). |
| **orm.xml** | Fichero donde está el mapeo y las consultas JPQL. |
| **Repositorio** | "La despensa" de un tipo de objetos: `add`, `remove`, `findBy...`. No tiene `update`. |
| **SQL** | El idioma de la base de datos. |
| **Tabla / fila / columna** | Hoja de datos / un registro (un mecánico) / un dato (su NIF). |
| **Transacción** | Grupo de cambios que se guardan todos o ninguno. Una por cada llamada a un servicio. |
| **Value type** | Dato pequeño que vale por su valor (Nif, Email, Money); se guarda como columnas de la entidad. |
| **version** | Número que sube cada vez que se modifica una fila; sirve para detectar que otro la cambió a la vez. |

---

## Parte 11. Comprueba si lo has entendido

Intenta contestar sin mirar. Las respuestas están debajo.

1. ¿En qué tabla y en qué columna está guardado a qué mecánico pertenece una orden de trabajo?
2. ¿Qué hace exactamente `MechanicCrudServiceImpl.create`?
3. ¿En qué momento se escribe de verdad el mecánico nuevo en la base de datos?
4. Si un comando lanza una `BusinessException` después de haber hecho `repo.add(...)`, ¿qué pasa con
   lo añadido?
5. ¿Por qué no hay un método `update` en los repositorios?
6. ¿Qué diferencia hay entre `select m from Mechanic m` y `SELECT * FROM TMECHANICS`?
7. Añades a una clase un atributo `color`, sin tocar `orm.xml`. ¿En qué columna lo busca JPA? ¿Qué
   más tienes que hacer para que funcione?
8. Falta el nombre en el DTO. ¿Qué error se lanza y dónde se comprueba?
9. ¿Por qué se usa `Associations.Assigns.link(...)` en vez de asignar el mecánico a la orden
   directamente?

<details>
<summary>Respuestas</summary>

1. En `TWORKORDERS`, columna `MECHANIC_ID`, que guarda el ID del mecánico (una clave ajena).
2. Nada más que `return executor.execute(new AddMechanic(dto));`: pasa el trabajo al comando a través
   del ejecutor.
3. Cuando el ejecutor hace `commit`, al terminar el comando. `repo.add` solo le dice a JPA que lo vigile.
4. El ejecutor hace `rollback` y no se guarda nada: la transacción es todo o nada.
5. Porque los objetos cargados están "gestionados": si los cambias, JPA lo detecta y lo guarda al hacer
   commit. El repositorio se usa como una lista: solo se mete, se saca y se busca.
6. La primera es JPQL: habla de la clase `Mechanic` y devuelve objetos. La segunda es SQL: habla de la
   tabla `TMECHANICS` y devuelve filas.
7. En la columna `COLOR`. Además hay que crear esa columna en la base de datos
   (`ALTER TABLE ... ADD COLUMN COLOR ...`), porque no existe.
8. `IllegalArgumentException`, comprobada con `ArgumentChecks` en el constructor del comando.
9. Porque la relación se apunta en los dos objetos (la orden sabe su mecánico y el mecánico sabe sus
   órdenes), y `link` actualiza los dos a la vez para que no se contradigan.

</details>

---

## ¿Y ahora qué?

1. Si has entendido estas partes, abre `GUIA_PROYECTO_JPA.md`. Ahora ya conoces el vocabulario, y esa
   guía sirve de **consulta**: tablas de referencia, recetas y ejercicios grandes.
2. Abre en Eclipse los ficheros que se citan aquí (`AddMechanic`, `MechanicCrudServiceImpl`, el trozo
   de `Mechanic` del `orm.xml`) y léelos con esta guía al lado.
3. Haz la ampliación de la parte 9 en una **copia** del proyecto. Hacerlo una vez con tus manos vale
   más que leerlo diez veces.
