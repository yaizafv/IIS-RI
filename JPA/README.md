# CarWorkshop desde cero

Esta guía es para leer **antes** que `GUIA_PROYECTO_JPA.md`. No da nada por sabido salvo lo básico de
Java: qué es una clase, un objeto, un atributo y un método. Cada palabra técnica se explica la primera
vez que aparece, y todo se cuenta con **un único ejemplo** (los mecánicos del taller) que se va
completando poco a poco.

Léela en orden. Al final de cada parte hay un recuadro **"Lo importante"** con lo que deberías
quedarte. Si al llegar a uno no lo entiendes, vuelve atrás antes de seguir.

## Índice

* [Parte 1. ¿Qué problema resuelve este programa?](#parte-1-qué-problema-resuelve-este-programa)
* [Parte 2. ¿Dónde se guardan los datos? La base de datos](#parte-2-dónde-se-guardan-los-datos-la-base-de-datos)
  * [2.1 Tablas, filas y columnas](#21-tablas-filas-y-columnas)
  * [2.2 Tablas que se apuntan entre sí: la clave ajena](#22-tablas-que-se-apuntan-entre-sí-la-clave-ajena)
  * [2.3 SQL: el idioma de la base de datos](#23-sql-el-idioma-de-la-base-de-datos)
* [Parte 3. ¿Cómo se representa lo mismo en Java? Las clases del dominio](#parte-3-cómo-se-representa-lo-mismo-en-java-las-clases-del-dominio)
  * [3.1 ¿Qué es "el dominio"?](#31-qué-es-el-dominio)
  * [3.2 Entidades y "value types"](#32-entidades-y-value-types)
* [Parte 4. El traductor: JPA](#parte-4-el-traductor-jpa)
  * [4.1 El diccionario del traductor: `orm.xml`](#41-el-diccionario-del-traductor-ormxml)
  * [4.2 Una consecuencia: casi no hay SQL en el proyecto](#42-una-consecuencia-casi-no-hay-sql-en-el-proyecto)
* [Parte 5. Un viaje completo: dar de alta un mecánico](#parte-5-un-viaje-completo-dar-de-alta-un-mecánico)
  * [5.1 La analogía del restaurante](#51-la-analogía-del-restaurante)
  * [5.2 Paso 1: la pantalla pide el servicio](#52-paso-1-la-pantalla-pide-el-servicio)
  * [5.3 Paso 2: el camarero (servicio, también llamado "fachada")](#53-paso-2-el-camarero-servicio-también-llamado-fachada)
  * [5.4 Paso 3: el jefe de cocina (el ejecutor)](#54-paso-3-el-jefe-de-cocina-el-ejecutor)
  * [5.5 Paso 4: el cocinero (el comando)](#55-paso-4-el-cocinero-el-comando)
  * [5.6 Paso 5: la despensa (el repositorio)](#56-paso-5-la-despensa-el-repositorio)
  * [5.7 El viaje completo, de un vistazo](#57-el-viaje-completo-de-un-vistazo)
  * [5.8 ¿Por qué tantas piezas? (la "arquitectura")](#58-por-qué-tantas-piezas-la-arquitectura)
* [Parte 6. Los tres tipos de error](#parte-6-los-tres-tipos-de-error)
* [Parte 7. Relaciones entre objetos: por qué existe `Associations`](#parte-7-relaciones-entre-objetos-por-qué-existe-associations)
* [Parte 8. Leer uno de tus casos de uso: dar de alta un cliente](#parte-8-leer-uno-de-tus-casos-de-uso-dar-de-alta-un-cliente)
* [Parte 9. Tu primera ampliación, muy despacio](#parte-9-tu-primera-ampliación-muy-despacio)
  * [Paso 1. El dominio: añadir el dato a la clase](#paso-1-el-dominio-añadir-el-dato-a-la-clase)
  * [Paso 2. El mapeo: ¿hay que tocar `orm.xml`?](#paso-2-el-mapeo-hay-que-tocar-ormxml)
  * [Paso 3. La base de datos: crear la columna](#paso-3-la-base-de-datos-crear-la-columna)
  * [Paso 4. El servicio: usar el dato en un caso de uso](#paso-4-el-servicio-usar-el-dato-en-un-caso-de-uso)
* [Parte 10. Ejercicios resueltos tipo examen, paso a paso](#parte-10-ejercicios-resueltos-tipo-examen-paso-a-paso)
  * [Ejercicio 1. Stock de repuestos](#ejercicio-1-stock-de-repuestos)
  * [Ejercicio 2. Certificados de mecánicos (una clase nueva)](#ejercicio-2-certificados-de-mecánicos-una-clase-nueva)
  * [Ejercicio 3. Asignar una orden de trabajo (caso de uso con lo que ya hay)](#ejercicio-3-asignar-una-orden-de-trabajo-caso-de-uso-con-lo-que-ya-hay)
  * [La plantilla mental para cualquier ejercicio](#la-plantilla-mental-para-cualquier-ejercicio)
* [Parte 11. El simulacro de la prueba de autoría (TG), resuelto paso a paso](#parte-11-el-simulacro-de-la-prueba-de-autoría-tg-resuelto-paso-a-paso)
  * [11.1 Qué es la prueba, en una frase](#111-qué-es-la-prueba-en-una-frase)
  * [11.2 Fase 1: antes de empezar (con internet)](#112-fase-1-antes-de-empezar-con-internet)
  * [11.3 Fase 2: preparar los materiales (sin internet)](#113-fase-2-preparar-los-materiales-sin-internet)
  * [11.4 Fase 3: entender el enunciado](#114-fase-3-entender-el-enunciado)
  * [11.5 Fase 4: resolverlo, paso a paso](#115-fase-4-resolverlo-paso-a-paso)
  * [11.6 Fase 5: comprobar y entregar](#116-fase-5-comprobar-y-entregar)
  * [11.7 Qué podrían cambiar el día de la prueba (y cómo lo adaptarías)](#117-qué-podrían-cambiar-el-día-de-la-prueba-y-cómo-lo-adaptarías)
* [Parte 12. Glosario (para cuando te pierdas)](#parte-12-glosario-para-cuando-te-pierdas)
* [Parte 13. Comprueba si lo has entendido](#parte-13-comprueba-si-lo-has-entendido)
* [¿Y ahora qué?](#y-ahora-qué)

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

**Eso es todo.** Los ejercicios de la parte 10 son esto mismo con más piezas: varios atributos, una
entidad nueva, una consulta nueva...

> **Lo importante:** cualquier ampliación sigue el mismo orden: 1) clase del dominio, 2) `orm.xml`
> si hace falta, 3) base de datos si hay columnas o tablas nuevas, 4) comando + fachada.

---

## Parte 10. Ejercicios resueltos tipo examen, paso a paso

El profesor dijo que en la prueba podrían pedir:

* **modificar el modelo de dominio**,
* **modificar el mapeo**,
* **implementar un caso de uso desde cero** (capa de servicio),
* y **no** la capa de presentación (las pantallas).

Aquí tienes tres ejercicios de ese tipo, de menos a más difícil. Cada uno empieza por **cómo pensarlo**
antes de escribir nada, y después va paso a paso explicando el porqué de cada línea nueva.

Todo el código de esta parte se ha **probado de verdad** sobre una copia de tu proyecto: funciona, y
los tests originales siguen pasando.

| Ejercicio | Dominio | Mapeo | Base de datos | Caso de uso desde cero |
|---|---|---|---|---|
| 1. Stock de repuestos | Sí (atributos nuevos) | Sí | `ALTER TABLE` | Sí (4 métodos) |
| 2. Certificados de mecánicos | Sí (clase nueva + relación) | Sí (entidad nueva) | `CREATE TABLE` | Sí (2 métodos) |
| 3. Asignar una orden de trabajo | No (se usa lo que hay) | No | No | Sí (1 método) |

---

### Ejercicio 1. Stock de repuestos

#### El enunciado

> Los repuestos (`SparePart`) deben guardar cuántas unidades hay en el almacén (`stock`) y los límites
> mínimo y máximo (`minStock`, `maxStock`). Las columnas se llamarán `STOCK`, `MIN_STOCK` y
> `MAX_STOCK`. Implementa el servicio `SparePartCrudService` (dar de alta, modificar, borrar y buscar
> por código).

Este ejercicio es muy probable. La interfaz `SparePartCrudService` **ya está en tu esqueleto sin
implementar**, y su DTO ya pide `stock`, `minStock` y `maxStock`, que el modelo no tiene.

#### Cómo pensarlo antes de escribir nada

Hazte estas preguntas, siempre en el mismo orden:

1. **¿Cambia el dominio?** Sí: `SparePart` necesita tres atributos nuevos y una forma de modificarlos.
2. **¿Cambia el mapeo?** Sí: dos columnas tienen un nombre que JPA no adivinaría (`MIN_STOCK` en vez de
   `MINSTOCK`).
3. **¿Cambia la base de datos?** Sí: la tabla `TSPAREPARTS` no tiene esas columnas.
4. **¿Qué necesito buscar?** Repuestos por su código (para comprobar que no se repite, y para modificar
   y borrar), y saber si un repuesto se ha usado alguna vez (para no borrarlo).
5. **¿Qué casos de uso hay?** Cuatro: alta, modificación, borrado y búsqueda. Serán **cuatro comandos**
   más una fachada.

Y lo más importante: **lee el Javadoc de la interfaz** y apunta qué errores pide. Resumido:

| Método | `IllegalArgumentException` si... | `BusinessException` si... |
|---|---|---|
| `create(dto)` | dto null, código o descripción vacíos | el código ya existe; precio o stocks negativos |
| `update(dto)` | dto null, id o descripción vacíos | no existe; negativos; versión distinta |
| `delete(code)` | código null | no existe; tiene sustituciones (se usó en alguna reparación) |
| `findByCode(code)` | código null | nunca (devuelve vacío si no existe) |

Recuerda la regla de la parte 6: **IllegalArgumentException → `ArgumentChecks` en el constructor del
comando; BusinessException → `BusinessChecks` en `execute()`**.

#### Paso 1. El dominio (`domain/SparePart.java`)

Así está ahora (resumido):

```java
public class SparePart extends BaseEntity {
    private String code;
    private String description;
    private Money unitPrice;

    public SparePart(String code) {
        this(code, "no-description", Money.ZERO);
    }

    public SparePart(String code, String description, Money price) {
        ... comprueba y asigna ...
    }
}
```

**1.a) Añadir los atributos.** Son números enteros:

```java
private int stock;
private int minStock;
private int maxStock;
```

**1.b) Un constructor "completo".** Necesitamos poder crear un repuesto con todos sus datos:

```java
public SparePart(String code, String description, Money price, int stock, int minStock, int maxStock) {
    ArgumentChecks.isNotBlank(code, "Code cannot be empty");
    this.code = code;
    update(description, price, stock, minStock, maxStock);
}
```

¿Y qué pasa con los constructores que ya había? **No se borran**: los usan los tests del dominio
(`new SparePart("X")`). Lo que se hace es que llamen al nuevo, poniendo 0 en los stocks. A esto se le
llama **encadenar constructores**, y la checklist lo exige para no copiar y pegar comprobaciones:

```java
public SparePart(String code) {
    this(code, "no-description", Money.ZERO);              // llama al de 3 parámetros
}

public SparePart(String code, String description, Money price) {
    this(code, description, price, 0, 0, 0);                // llama al completo
}
```

`this(...)` dentro de un constructor significa "llama a otro constructor de esta misma clase".

**1.c) Un método para modificarlo.** El servicio tendrá que cambiar la descripción, el precio y los
stocks. En lugar de cinco setters, creamos **un método de negocio** que comprueba todo junto:

```java
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
```

Fíjate en que el constructor completo **reutiliza** `update` para no repetir las comprobaciones.

¿Por qué no se puede cambiar el código? Porque el código **identifica** al repuesto: su `equals` y su
`hashCode` se basan en él. Cambiar la identidad de un objeto es como cambiarle el DNI a una persona.

**1.d) Getters** para que el servicio pueda leer los datos:

```java
public int getStock() { return stock; }
public int getMinStock() { return minStock; }
public int getMaxStock() { return maxStock; }
```

Y añade los tres campos al `toString()`.

#### Paso 2. El mapeo (`META-INF/orm.xml`)

Busca la parte de `SparePart`. Antes estaba así:

```xml
<entity class="uo.ri.cws.domain.SparePart">
    <table name="TSPAREPARTS" />
    <attributes>
        <basic name="code">
            <column unique="true" />
        </basic>
        <embedded name="unitPrice">
            ...
        </embedded>
    </attributes>
</entity>
```

Pregúntate, atributo por atributo: **¿JPA adivinaría la columna?**

* `stock` → buscaría `STOCK`. **Sí** acierta. No haría falta escribir nada, aunque ponerlo tampoco hace
  daño y deja claro que existe.
* `minStock` → buscaría `MINSTOCK`, pero la columna se llama `MIN_STOCK`. **No** acierta: hay que decírselo.
* `maxStock` → igual que el anterior.

Queda así:

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
        <embedded name="unitPrice">
            ...
        </embedded>
    </attributes>
</entity>
```

⚠️ **El orden importa.** Dentro de `<attributes>`, todos los `<basic>` van **antes** que los
`<embedded>`. Si los pones después, el programa ni arranca: el fichero XML tiene unas reglas de orden
fijas.

#### Paso 3. La base de datos

La tabla `TSPAREPARTS` no tiene esas columnas. Si no las creas, verás un error como
*"objeto no encontrado: STOCK"*. Se crean con:

```sql
ALTER TABLE TSPAREPARTS ADD COLUMN STOCK INTEGER DEFAULT 0 NOT NULL;
ALTER TABLE TSPAREPARTS ADD COLUMN MIN_STOCK INTEGER DEFAULT 0 NOT NULL;
ALTER TABLE TSPAREPARTS ADD COLUMN MAX_STOCK INTEGER DEFAULT 0 NOT NULL;
SHUTDOWN;
```

* `INTEGER` es el tipo de la columna (un `int` de Java).
* `DEFAULT 0` es imprescindible: la tabla ya tiene 100 repuestos, y algo hay que poner en las columnas
  nuevas de esas filas. Un `int` de Java no puede ser `null`, así que sin `DEFAULT` el programa fallaría
  al leerlos.
* `SHUTDOWN` hace que la base de datos escriba los cambios en el fichero.

Cómo ejecutarlo: `GUIA_PROYECTO_JPA.md`, apartado 5.6.

#### Paso 4. Buscar: el repositorio y una "query"

Necesitamos dos búsquedas, y cada una va en un sitio distinto:

* **"Dame el repuesto con este código"** devuelve un **objeto del dominio** (un `SparePart`). Eso va en
  el **repositorio**.
* **"¿Este repuesto se ha usado alguna vez?"** devuelve un **sí o no**, no un objeto. Las búsquedas que
  devuelven datos calculados (cuántos hay, sumas, sí/no) van en una clase de **consultas** (*queries*).

**4.a) El repositorio.** Primero la interfaz (`application/repository/SparePartRepository.java`), que
solo dice qué se puede hacer:

```java
public interface SparePartRepository extends Repository<SparePart> {

    /**
     * @param code of the spare part
     * @return the spare part with that code, or Optional.empty() if none
     */
    Optional<SparePart> findByCode(String code);

}
```

Después la clase que lo hace con JPA
(`infrastructure/persistence/jpa/repository/SparePartJpaRepository.java`). Ya existía, vacía; le
añadimos el método:

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

Léelo así: "con el EntityManager de esta transacción, prepara la consulta llamada
`SparePart.findByCode`, que devuelve `SparePart`; pon `code` en el `?1`; y dame el primero, o nada si
no hay".

¿Por qué `getResultStream().findFirst()` y no `getSingleResult()`? Porque puede que **no exista** un
repuesto con ese código. `getSingleResult()` lanza una excepción si no encuentra nada, y la checklist
lo penaliza.

**4.b) La consulta en el `orm.xml`.** Va con las demás `<named-query>`, al principio del fichero:

```xml
<named-query name="SparePart.findByCode">
    <query>
        select sp
        from SparePart sp
        where sp.code = ?1
    </query>
</named-query>
```

Es JPQL: habla de la clase `SparePart` y de su atributo `code`, no de la tabla.

**4.c) La query "¿se ha usado?"** Un repuesto se usa en las **sustituciones** (`Substitution`: "en esta
reparación se pusieron 2 unidades de este repuesto"). Basta con **contar** las sustituciones de ese
repuesto.

La consulta en el `orm.xml`:

```xml
<named-query name="SparePart.countSubstitutions">
    <query>
        select count(s)
        from Substitution s
        where s.sparePart.id = ?1
    </query>
</named-query>
```

`s.sparePart.id` significa "el id del repuesto de la sustitución": se **navega** del objeto
`Substitution` a su `SparePart`.

La interfaz (`application/query/SparePartQueries.java`, fichero nuevo):

```java
public interface SparePartQueries {

    /**
     * @return true if the spare part has been used in any substitution
     */
    boolean hasSubstitutions(String sparePartId);

}
```

La clase que lo hace (`infrastructure/persistence/jpa/query/SparePartJpaQueries.java`, fichero nuevo):

```java
public class SparePartJpaQueries implements SparePartQueries {

    @Override
    public boolean hasSubstitutions(String sparePartId) {
        return 0 < Jpa.getManager()
                .createNamedQuery("SparePart.countSubstitutions", Long.class)
                .setParameter(1, sparePartId)
                .getSingleResult();
    }

}
```

Aquí **sí** se usa `getSingleResult()`, porque un `count` **siempre** devuelve exactamente un número
(aunque sea 0). `0 < número` es `true` si hay alguna sustitución.

**4.d) Darla de alta en la fábrica.** ¿Recuerdas que todo se pide a `Factories`? Para que exista
`Factories.query.forSparePart()`:

En la interfaz `application/query/QueryFactory.java` se añade:

```java
SparePartQueries forSparePart();
```

En la clase `infrastructure/persistence/jpa/query/JpaQueryFactory.java` se añade:

```java
@Override
public SparePartQueries forSparePart() {
    return new SparePartJpaQueries();
}
```

(El repositorio de repuestos no hay que registrarlo: `forSparePart()` ya existía en `JpaRepositoryFactory`.)

#### Paso 5. Los comandos

Crea un paquete nuevo `application/service/spare/crud/commands`. Todos los comandos tienen la forma de
la parte 5.5: atributos → constructor con `ArgumentChecks` → `execute()` con `BusinessChecks` y el
dominio.

**5.a) Una ayuda para no repetir código.** El alta y la modificación comprueban lo mismo (precio y
stocks no negativos). En vez de copiar y pegar, se pone en una clase pequeña:

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

* `final class` y el constructor `private`: es una clase de ayuda, de la que nunca se crean objetos.
* `dto.price.compareTo(BigDecimal.ZERO) >= 0`: los precios del DTO son `BigDecimal` (números decimales
  exactos), que no se comparan con `>=`. Se usa `compareTo`, que devuelve un número negativo, 0 o
  positivo.

¿Por qué `BusinessChecks` y no `ArgumentChecks`? Porque el Javadoc dice que un número negativo es una
**BusinessException**: es un error del usuario que teclea mal, no de un programador.

**5.b) Dar de alta (`AddSparePart`):**

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
        repo.add(sp);

        dto.id = sp.getId();
        dto.version = sp.getVersion();
        return dto;
    }

}
```

Recorre el Javadoc y comprueba que está todo:

* dto null / código vacío / descripción vacía → `ArgumentChecks` en el constructor ✔
* negativos → `checkAmounts` ✔
* código repetido → `doesNotExist(repo.findByCode(...))` ✔ (`doesNotExist` lanza el error si la caja
  `Optional` **tiene** algo)
* `Money.of(dto.price)`: el dominio trabaja con el value type `Money`, no con `BigDecimal`. Se convierte
  al pasar del DTO al dominio.

**5.c) Modificar (`UpdateSparePart`):**

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
        return null;
    }

}
```

Tres cosas nuevas:

* **`osp.get()`**: saca el objeto de la caja `Optional`. Solo se puede hacer después de comprobar que
  existe (`exists`).
* **`hasVersion`**: ¿te acuerdas del número `version` de `BaseEntity`? Sube cada vez que alguien
  modifica la fila. El DTO trae la versión que vio el usuario cuando abrió la pantalla. Si no coincide
  con la actual, otro usuario lo cambió mientras tanto, y no se le pisan los cambios.
* **¡No hay `repo.update(...)`!** `sp` lo trajo JPA en esta transacción, así que está **gestionado**: al
  llamar a `sp.update(...)` JPA ve que ha cambiado y lo guarda solo al hacer `commit`. `updatedNow()`
  actualiza la fecha de modificación.

**5.d) Borrar (`DeleteSparePart`):**

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

`repo.remove(sp)` es el "sacar de la despensa": JPA hará el `DELETE` al hacer commit.

¿Por qué comprobar las sustituciones? Si un repuesto se usó en una reparación, la tabla `TSUBSTITUTIONS`
tiene filas que apuntan a él con su `SPAREPART_ID`. La base de datos no deja borrar una fila a la que
otras apuntan, y daría un error feo. Además lo pide el Javadoc.

**5.e) Buscar (`FindSparePartByCode`):**

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

`.map(DtoAssembler::toDto)` significa: "si la caja tiene un `SparePart`, conviértelo en `SparePartDto`;
si está vacía, devuelve una caja vacía". Así nunca se devuelven objetos del dominio a la pantalla.

#### Paso 6. El conversor (`DtoAssembler`) y la fachada

En el paquete `application/service/spare/crud`:

```java
public class DtoAssembler {

    public static SparePartDto toDto(SparePart sp) {
        SparePartDto dto = new SparePartDto();
        dto.id = sp.getId();
        dto.version = sp.getVersion();
        dto.code = sp.getCode();
        dto.description = sp.getDescription();
        dto.price = sp.getPrice().amount();     // Money → BigDecimal
        dto.stock = sp.getStock();
        dto.minStock = sp.getMinStock();
        dto.maxStock = sp.getMaxStock();
        return dto;
    }

}
```

La fachada, **una línea por método**:

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

#### Paso 7. Enchufar el servicio

Si ahora alguien pide `Factories.service.forSparePartCrudService()`, todavía falla, porque en
`application/service/JpaServicesFactoryImpl.java` pone:

```java
public SparePartCrudService forSparePartCrudService() {
    throw new NotYetImplementedException();       // "todavía no está hecho"
}
```

Se cambia por:

```java
public SparePartCrudService forSparePartCrudService() {
    return new SparePartCrudServiceImpl();
}
```

**Este paso se olvida muchísimo.** Si te sale `NotYetImplementedException`, es esto.

#### Paso 8. Comprobar que funciona

Si no te dan tests, puedes escribir uno pequeño en `cws_service_tests/test/` (el test completo está en
`GUIA_PROYECTO_JPA.md`, ejercicio 1, paso 7). La idea:

```java
service.create(dto("SP-1"));                                   // alta
SparePartDto found = service.findByCode("SP-1").orElseThrow(); // buscar
assertEquals(10, found.stock);                                 // ¿se guardó el stock?
found.stock = 7;
service.update(found);                                         // modificar
service.delete("SP-1");                                        // borrar
assertTrue(service.findByCode("SP-1").isEmpty());              // ya no está
```

#### Resumen de lo que has tocado

| Capa | Fichero | Qué |
|---|---|---|
| Dominio | `SparePart.java` | 3 atributos, constructor completo, `update`, getters |
| Mapeo | `orm.xml` | 3 `<basic>` + 2 `<named-query>` |
| BD | `TSPAREPARTS` | 3 columnas |
| Persistencia | `SparePartRepository` / `SparePartJpaRepository` | `findByCode` |
| Persistencia | `SparePartQueries` / `SparePartJpaQueries` (nuevos) | `hasSubstitutions` |
| Fábricas | `QueryFactory`, `JpaQueryFactory`, `JpaServicesFactoryImpl` | registrar lo nuevo |
| Servicio | paquete `spare/crud` (nuevo) | 4 comandos, ayuda, `DtoAssembler`, fachada |

---

### Ejercicio 2. Certificados de mecánicos (una clase nueva)

#### El enunciado

> Un mecánico puede estar **certificado** para reparar un tipo de vehículo (por ejemplo, "camión"), y
> se guarda la fecha del certificado. Un mecánico no puede tener dos certificados para el mismo tipo.
> Crea la clase `Certificate` (tabla `TCERTIFICATES`) e implementa un servicio con:
> * `certify(mechanicId, vehicleTypeId)`: certifica al mecánico. `BusinessException` si no existe el
>   mecánico, no existe el tipo o ya estaba certificado.
> * `findByVehicleTypeId(vehicleTypeId)`: lista los certificados de ese tipo (vacía si no existe).

#### Cómo pensarlo

1. **¿Cambia el dominio?** Sí, y mucho: hay una **clase nueva** que relaciona dos que ya existen
   (`Mechanic` y `VehicleType`).
2. **¿Cambia el mapeo?** Sí: una entidad nueva, y las "otras caras" de la relación en las dos clases.
3. **¿Cambia la base de datos?** Sí: una **tabla nueva**.
4. **¿Qué necesito buscar?** Mecánico y tipo por id (eso ya existe: `findById`). Y los certificados de
   un tipo... ¡que se consiguen **navegando** desde el tipo, sin consulta!

Dibújalo antes de escribir:

```
Mechanic  1 ──────── *  Certificate  * ──────── 1  VehicleType
(Ana)                   (fecha)                     (camión)

"Ana tiene muchos certificados; cada certificado es de un mecánico y de un tipo;
 un tipo tiene muchos certificados."
```

#### Paso 1. La clase nueva (`domain/Certificate.java`)

Un certificado **no tiene sentido sin** su mecánico y su tipo: "el certificado de Ana para camiones" se
identifica justo por esos dos. Esto se llama **clase asociativa**: existe para relacionar a otras dos, y
**su identidad son esos dos objetos**.

```java
/**
 * A mechanic is certified to repair a type of vehicle. Associative class:
 * its identity is the pair (mechanic, vehicleType).
 */
public class Certificate extends BaseEntity {

    // Natural attributes
    private LocalDate date;

    // Accidental attributes (they form the identity)
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
}
```

Explicación, trozo a trozo:

* **`extends BaseEntity`**: así tiene `id`, `version` y fechas, como todas las entidades.
* **`Certificate() { // for JPA }`**: JPA necesita un constructor **sin parámetros** para crear el objeto
  al leerlo de la base de datos (primero lo crea vacío y luego rellena los atributos). No es `public`
  porque nadie más debe usarlo. La checklist penaliza que sea público.
* **Dos constructores encadenados**: si no se dice la fecha, se usa la de hoy (`LocalDate.now()`).
* **`Associations.Certifies.link(...)` en el constructor**: normalmente los `link` se llaman desde el
  comando, pero aquí se hace en el constructor porque el mecánico y el tipo **son la identidad** del
  certificado (la excepción de la parte 7). Un certificado sin mecánico no debe existir ni un instante.
* **`_setMechanic` y `_setVehicleType`** sin `public`: solo los usa `Associations`.
* **`equals` y `hashCode` sobre (mecánico, tipo)**: dos certificados son "el mismo" si son del mismo
  mecánico y el mismo tipo.

#### Paso 2. La relación (`domain/Associations.java`)

Se añade una clase interna nueva, con la misma forma que las que ya hay:

```java
public static class Certifies {

    /* package */ static void link(Mechanic mechanic, Certificate certificate, VehicleType vehicleType) {
        ArgumentChecks.isNotNull(mechanic);
        ArgumentChecks.isNotNull(certificate);
        ArgumentChecks.isNotNull(vehicleType);
        StateChecks.isTrue(certificate.getMechanic() == null, "Certificate already has a mechanic");
        StateChecks.isTrue(certificate.getVehicleType() == null, "Certificate already has a vehicle type");

        certificate._setMechanic(mechanic);              // 1) el certificado apunta a los dos
        certificate._setVehicleType(vehicleType);
        mechanic._getCertificates().add(certificate);    // 2) los dos apuntan al certificado
        vehicleType._getCertificates().add(certificate);
    }

    public static void unlink(Certificate certificate) {
        ArgumentChecks.isNotNull(certificate);
        Mechanic mechanic = certificate.getMechanic();
        VehicleType vehicleType = certificate.getVehicleType();
        StateChecks.isTrue(mechanic != null, "Certificate has no mechanic");

        mechanic._getCertificates().remove(certificate); // 1) quitarlo de las listas
        vehicleType._getCertificates().remove(certificate);
        certificate._setMechanic(null);                  // 2) borrar sus referencias
        certificate._setVehicleType(null);
    }
}
```

**¿Por qué ese orden exacto?** Las listas son `HashSet`, que colocan cada objeto según su `hashCode`. El
`hashCode` del certificado depende del mecánico y del tipo. Por eso:

* En `link`, **primero** se asignan el mecánico y el tipo, y **después** se mete en los sets. Si lo
  metieras antes, se colocaría según un `hashCode` que luego cambia, y el set ya no lo encontraría.
* En `unlink`, **primero** se saca de los sets y **después** se borran las referencias, por el mismo
  motivo.

#### Paso 3. Las "otras caras" en `Mechanic` y `VehicleType`

Para que se pueda navegar en los dos sentidos ("los certificados de Ana", "los certificados de
camión"), cada una necesita su lista. En `Mechanic.java`:

```java
private Set<Certificate> certificates = new HashSet<>();

Set<Certificate> _getCertificates() {          // para Associations (sin public)
    return certificates;
}

public Set<Certificate> getCertificates() {    // para todos: devuelve una COPIA
    return new HashSet<>(certificates);
}
```

Y además **una regla de negocio** que nos servirá en el servicio y en el ejercicio 3:

```java
/**
 * @return true if the mechanic holds a certificate for that vehicle type
 */
public boolean isCertifiedFor(VehicleType vehicleType) {
    return certificates.stream()
            .anyMatch(c -> c.getVehicleType().equals(vehicleType));
}
```

En castellano: "¿alguno de mis certificados es de ese tipo de vehículo?". `stream().anyMatch(...)`
recorre la lista y devuelve `true` en cuanto uno cumple la condición.

En `VehicleType.java`, lo mismo: el atributo `certificates`, `_getCertificates()` y `getCertificates()`.
Recuerda añadir `import java.util.HashSet;` e `import java.util.Set;`.

#### Paso 4. El mapeo (`orm.xml`)

**4.a) La entidad nueva.** Va junto a las demás `<entity>`, antes de los `<embeddable>`:

```xml
<entity class="uo.ri.cws.domain.Certificate">
    <table name="TCERTIFICATES">
        <unique-constraint>                          <!-- no puede repetirse el par... -->
            <column-name>MECHANIC_ID</column-name>
            <column-name>VEHICLETYPE_ID</column-name>  <!-- ...(mecánico, tipo) -->
        </unique-constraint>
    </table>
    <attributes>
        <many-to-one name="mechanic" />              <!-- muchos certificados → un mecánico -->
        <many-to-one name="vehicleType" />           <!-- muchos certificados → un tipo -->
    </attributes>
</entity>
```

* `<many-to-one name="mechanic" />` se lee "muchos certificados pueden ser del mismo mecánico". JPA lo
  guarda en la columna `MECHANIC_ID` (el nombre del atributo + `_ID`), que es la clave ajena.
* `<unique-constraint>` hace que la base de datos impida dos filas con el mismo mecánico y tipo. Es la
  regla "no dos certificados iguales", protegida también en la base de datos.
* `date` no se escribe: JPA lo busca solo en la columna `DATE`.

**4.b) Las otras caras.** En la entidad `Mechanic`, junto a `assigned`:

```xml
<one-to-many name="certificates" mapped-by="mechanic" />
```

En la entidad `VehicleType`, después del `<basic name="name">` y antes del `<embedded>`:

```xml
<one-to-many name="certificates" mapped-by="vehicleType" />
```

`one-to-many` es "un mecánico tiene muchos certificados". `mapped-by="mechanic"` le dice a JPA: "esta
lista **no** tiene columna propia; para rellenarla, busca los certificados cuyo atributo `mechanic`
soy yo". Es decir, usa la columna `MECHANIC_ID` de la otra tabla.

#### Paso 5. La tabla nueva

```sql
CREATE TABLE TCERTIFICATES (
    ID VARCHAR(255) NOT NULL PRIMARY KEY,
    CREATEDAT TIMESTAMP, DATE DATE, ENTITYSTATE VARCHAR(255),
    UPDATEDAT TIMESTAMP, VERSION BIGINT,
    MECHANIC_ID VARCHAR(255), VEHICLETYPE_ID VARCHAR(255),
    CONSTRAINT UNQ_TCERTIFICATES UNIQUE (MECHANIC_ID, VEHICLETYPE_ID),
    CONSTRAINT FK_TCERTIFICATES_MECHANIC_ID FOREIGN KEY (MECHANIC_ID) REFERENCES TMECHANICS (ID),
    CONSTRAINT FK_TCERTIFICATES_VEHICLETYPE_ID FOREIGN KEY (VEHICLETYPE_ID) REFERENCES TVEHICLETYPES (ID)
);
SHUTDOWN;
```

Línea a línea:

* `ID ... PRIMARY KEY`: la clave primaria, como en todas las tablas.
* `CREATEDAT`, `UPDATEDAT`, `ENTITYSTATE`, `VERSION`: los atributos que hereda de `BaseEntity`. **Todas**
  las tablas de entidades los tienen.
* `DATE DATE`: la columna `DATE`, de tipo fecha (`LocalDate`).
* `MECHANIC_ID`, `VEHICLETYPE_ID`: las claves ajenas.
* `UNIQUE (...)`: la regla de no repetir el par.
* `FOREIGN KEY ... REFERENCES ...`: le dice a la base de datos que esos IDs deben existir en las otras
  tablas.

#### Paso 6. El repositorio

Para poder guardar certificados nuevos hace falta su "despensa". Como no necesita búsquedas especiales,
es muy corto:

```java
// application/repository/CertificateRepository.java
public interface CertificateRepository extends Repository<Certificate> {
}

// infrastructure/persistence/jpa/repository/CertificateJpaRepository.java
public class CertificateJpaRepository
        extends BaseJpaRepository<Certificate>
        implements CertificateRepository {
}
```

`BaseJpaRepository` ya trae hechos `add`, `remove`, `findById` y `findAll`.

Se registra en las fábricas. En `application/repository/PersistenceFactory.java`:

```java
CertificateRepository forCertificate();
```

y en `infrastructure/persistence/jpa/repository/JpaRepositoryFactory.java`:

```java
@Override
public CertificateRepository forCertificate() {
    return new CertificateJpaRepository();
}
```

#### Paso 7. Los comandos

**7.a) Certificar (`CertifyMechanic`):**

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

Compáralo con el enunciado: no existe el mecánico ✔, no existe el tipo ✔, ya certificado ✔ (usando la
regla del dominio `isCertifiedFor`). Después crea el certificado (el constructor ya lo enlaza) y lo
guarda.

**7.b) Listar los certificados de un tipo (`FindCertificatesByVehicleType`):**

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

Léelo así: "busca el tipo; si existe, coge sus certificados y conviértelos en DTOs; si no existe,
devuelve una lista vacía (`List.of()`)".

**¡No hay consulta JPQL!** Como `VehicleType` tiene la lista `certificates` (paso 3), basta con
**navegar** (`vt.getCertificates()`). La checklist penaliza hacer una consulta para algo que se
consigue navegando.

#### Paso 8. Conversor, fachada y registro

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

`getNif().nif()`: `getNif()` devuelve el value type `Nif`, y `.nif()` saca el texto que lleva dentro.

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

(La interfaz `CertificateService` con su DTO, y la línea `CertificateService forCertificateService();`
en `ServiceFactory`, te las darían en el enunciado. Están completas en `GUIA_PROYECTO_JPA.md`,
ejercicio 2.)

#### Una pregunta que te pueden hacer

**"¿Y ahora qué pasa si borras un mecánico que tiene certificados?"** La base de datos no lo dejaría,
porque hay filas en `TCERTIFICATES` que apuntan a él, y saltaría un error feo. Habría que añadir en
`DeleteMechanic` una comprobación:

```java
BusinessChecks.isTrue(m.getCertificates().isEmpty(), "The mechanic has certificates");
```

Pensar en estas consecuencias es lo que distingue a quien entiende el modelo.

---

### Ejercicio 3. Asignar una orden de trabajo (caso de uso con lo que ya hay)

#### El enunciado

> Implementa `assign(workOrderId, mechanicId)`: asigna una orden de trabajo **abierta** a un mecánico
> **certificado** para el tipo del vehículo de la orden. `BusinessException` si no existe la orden, no
> existe el mecánico, la orden no está abierta o el mecánico no está certificado.
> `IllegalArgumentException` si algún parámetro está vacío.

#### Cómo pensarlo

1. **¿Cambia el dominio?** No. `WorkOrder` ya tiene `assignTo(mechanic)` (lo viste en la parte 3.1), y
   `Mechanic.isCertifiedFor(...)` lo hicimos en el ejercicio 2.
2. **¿Cambia el mapeo o la base de datos?** No.
3. **¿Qué necesito buscar?** La orden y el mecánico por id: `findById`, que ya existe.

Así que **solo** hay que escribir el comando, la fachada y registrarla. Es el ejercicio más típico de
"caso de uso desde cero": **el comando coordina y el dominio hace el trabajo**.

#### El punto delicado: el error correcto

`WorkOrder.assignTo` ya comprueba que la orden esté abierta, pero lanza `IllegalStateException`
(es del dominio, parte 6). El enunciado pide `BusinessException`. La solución es **comprobarlo antes
en el comando**, con un método del dominio que solo pregunta (`wo.isOpen()`), y **después** llamar al
que cambia el estado. Así el dominio nunca llega a quejarse.

#### El comando

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
        Optional<WorkOrder> owo = woRepo.findById(workOrderId);           // ¿existe la orden?
        BusinessChecks.exists(owo, "The work order does not exist");
        Optional<Mechanic> om = mechanicRepo.findById(mechanicId);        // ¿existe el mecánico?
        BusinessChecks.exists(om, "The mechanic does not exist");
        WorkOrder wo = owo.get();
        Mechanic m = om.get();

        BusinessChecks.isTrue(wo.isOpen(), "The work order is not open"); // ¿está abierta?
        BusinessChecks.isTrue(m.isCertifiedFor(wo.getVehicle().getVehicleType()),
                "The mechanic is not certified for this vehicle type");     // ¿está certificado?

        wo.assignTo(m);                                                    // el dominio hace el cambio
        return null;
    }

}
```

Fíjate en `wo.getVehicle().getVehicleType()`: se **navega** de la orden a su vehículo y del vehículo a su
tipo. Sin consultas.

Y otra vez **no hay `update`**: `wo` y `m` están gestionados. `assignTo` cambia el estado de la orden y,
con `Associations.Assigns.link`, apunta la relación en los dos objetos. Al hacer commit, JPA escribe
`STATE = 'ASSIGNED'` y `MECHANIC_ID = <id de Ana>` en la fila de la orden.

#### Fachada y registro

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
// en JpaServicesFactoryImpl
@Override
public AssignWorkOrderService forAssignWorkOrderService() {
    return new AssignWorkOrderServiceImpl();
}
```

---

### La plantilla mental para cualquier ejercicio

Cuando leas el enunciado en la prueba, sigue siempre estas preguntas:

```
1. ¿Qué dice el Javadoc?            → apunta cada IllegalArgumentException y cada BusinessException
2. ¿Cambia el dominio?              → atributos, constructor completo (encadenar), métodos de negocio,
                                       clase nueva + Associations + listas en las otras clases
3. ¿Cambia el mapeo?                → solo lo que JPA no adivina; entidad nueva; many-to-one / one-to-many
                                       ¡orden de los elementos en el XML!
4. ¿Cambia la base de datos?        → ALTER TABLE (columna) / CREATE TABLE (tabla) + SHUTDOWN
5. ¿Qué tengo que buscar?           → ¿se consigue navegando? entonces sin consulta.
                                       ¿devuelve objetos? repositorio. ¿devuelve datos/números? query.
                                       consulta nueva = <named-query> en orm.xml
6. Comandos                         → constructor: ArgumentChecks / execute: BusinessChecks + dominio
                                       sin repo.update: los objetos gestionados se guardan solos
7. Fachada                          → una línea por método
8. Registrar                        → JpaServicesFactoryImpl (y fábricas de repos/queries si hay nuevos)
9. Probar                           → un test pequeño o los que te den
```

> **Lo importante:** los tres ejercicios usan siempre las mismas piezas en el mismo orden. Si sabes
> hacer el ejercicio 1 con calma, sabes hacer cualquiera.

---

## Parte 11. El simulacro de la prueba de autoría (TG), resuelto paso a paso

En la carpeta tienes los materiales de un **simulacro** de la prueba:

* `TG.ES. autoria.demo.example.pdf`: las instrucciones y el enunciado.
* `tg_authoring_demo.zip`: lo que te darían ese día (una base de datos nueva y tests nuevos).

Esta parte explica qué hay que hacer, en orden, y **los problemas reales que aparecen** al seguir las
instrucciones al pie de la letra. Todo se ha hecho de verdad sobre una copia de tu proyecto: al
terminar pasan los 404 tests de dominio y mapeo y los 52 de servicio, incluido el nuevo.

### 11.1 Qué es la prueba, en una frase

Te dan una **modificación pequeña** del programa y unos **tests nuevos**. Tienes que cambiar tu
`cws_services_jpa` hasta que todos los tests pasen, sin internet, y entregar solo esa carpeta
comprimida. Sirve para demostrar que el código de tu entrega lo entiendes tú.

### 11.2 Fase 1: antes de empezar (con internet)

El PDF dice:

1. **Descarga tu entrega** del campus virtual.
2. **Descarga el esqueleto de la extensión** (el de la práctica 8 extendida).
3. **Sustituye `cws_services_jpa`** del esqueleto por el tuyo: borra la carpeta del esqueleto **entera**
   y copia la tuya. No mezcles carpetas: si quedan ficheros del esqueleto, pueden aparecer clases
   repetidas o vacías.
4. **Comprueba que todos los tests pasan en verde** (`RunServiceTests_1` y los de `cws_services_jpa`).

> Si algo falla aquí, es un problema de tu entrega o de cómo has copiado las carpetas, no de la prueba.
> Resuélvelo **antes** de seguir, porque después no tendrás internet.

### 11.3 Fase 2: preparar los materiales (sin internet)

El PDF dice: desconectar la red, ejecutar `4.setLegacy.bat`, copiar los archivos del ZIP y cambiar el
`persistence.xml`. Aquí es donde aparecen las trampas. Hazlo así:

**Paso 1. Ejecuta `4.setLegacy.bat`** (en `cws_database`). Deja la base de datos con los datos de
partida: copia `test.script.data` sobre `test.script`.

**Paso 2. La base de datos del ZIP.** El ZIP trae una carpeta `cws_database` con la columna nueva ya
creada. **No tienes que hacer ningún `ALTER TABLE`.** Lo puedes comprobar abriendo
`cws_database/test.script` con un editor de texto y buscando `REGISTRATION_DATE`.

⚠️ **Trampa 1: el nombre de la carpeta.** El PDF dice "copia la carpeta `cws_database`", pero la URL
que te manda poner en el `persistence.xml` apunta a `../cws_database_demo/test`. Si la URL apunta a una
carpeta que **no existe**, HSQLDB no da error: **crea una base de datos vacía** en ese sitio. Entonces
fallan todos los tests con mensajes como *"objeto no encontrado: TCLIENTS"*. Tienes dos opciones; elige
una y sé coherente:

* **Opción A (la que se probó):** copia la carpeta del ZIP a la raíz del proyecto **con el nombre
  `cws_database_demo`** (junto a tu `cws_database`) y usa la URL tal cual la da el PDF.
* **Opción B:** copia la carpeta del ZIP **encima** de tu `cws_database` y en la URL pon
  `../cws_database/test`.

**Paso 3. Los tests del ZIP.**

⚠️ **Trampa 2: no copies los tests "encima".** El ZIP trae el proyecto `cws_service_tests` **completo**
(con su `pom.xml` y sus utilidades). Sus utilidades (`Asserts`, `DbFixtures`, los *records*...) son una
versión **más antigua**, la de la práctica 8 base, sin nada de bonos, clientes ni medios de pago. Si
copias el contenido del ZIP sobre tu `cws_service_tests`, se mezclan versiones y los tests de tu
ampliación **dejan de compilar**. Se comprobó: salen decenas de errores `cannot find symbol` en
`voucher/...`.

Lo correcto es **sustituir el proyecto entero**:

1. Borra la carpeta `cws_service_tests` de tu proyecto.
2. Copia en su lugar la carpeta `cws_service_tests` del ZIP.
3. En Eclipse: botón derecho sobre el proyecto → *Refresh*. Si hace falta, *Maven → Update Project*.

(El PDF lo llama `cws_acceptance_tests`, pero el proyecto se llama `cws_service_tests`. Es otra pequeña
errata.)

Esto es seguro porque lo que **entregas** es solo `cws_services_jpa`: los tests se los ponen ellos.

**Paso 4. El `persistence.xml`** (`cws_services_jpa/src/META-INF/persistence.xml`). Debe quedar así:

```xml
<properties>

    <!--
        <property name="eclipselink.ddl-generation" value="drop-and-create-tables"/>
     -->

    ...

    <property name="jakarta.persistence.jdbc.url"
              value="jdbc:hsqldb:file:../cws_database_demo/test; files_readonly=true"/>
    <!--
        ... el resto de URLs, todas DENTRO del comentario ...
    -->
```

* La línea `ddl-generation` tiene que estar **comentada** (trabajamos en brown field).
* Solo puede haber **una** línea `jdbc.url` fuera de comentarios. En tu fichero hay varias URLs dentro
  de un bloque `<!-- ... -->`: esas no cuentan. Cambia **solo la que está activa**.
* El espacio después de `;` que aparece en el PDF (`test; files_readonly`) no da problemas: se probó.

**Paso 5. Ejecuta los tests antes de tocar nada.** Lo esperado es:

* `cws_services_jpa`: todo verde (404).
* `cws_service_tests` (`RunServiceTests`): **un solo fallo**, el escenario nuevo
  `[demo.I.C.1] Create one invoice for an existing workorder`.

Si falla algo más, revisa los pasos 2 a 4 antes de seguir. Si falla solo ese, ya sabes exactamente qué
hay que hacer pasar.

### 11.4 Fase 3: entender el enunciado

> Ahora los vehículos también registran la **fecha de matriculación** (`registeredDate` de tipo
> `LocalDate`) y el cálculo del importe de las facturas cambia: si el vehículo es un **clásico** (tiene
> **más de 30 años**), el importe de sus órdenes de trabajo se incrementa un **50 %** al incluirlas en la
> factura.
>
> * La clase `WorkOrder` **no debe ser modificada**.
> * El cálculo de la factura totalizará el importe de la orden con el incremento necesario.
> * La columna se llama `REGISTRATION_DATE`, pero el atributo debe llamarse `registeredDate`.

Pasa el enunciado por la plantilla mental de la parte 10:

| Pregunta | Respuesta |
|---|---|
| ¿Cambia el dominio? | **Sí.** `Vehicle` necesita el atributo `registeredDate` y saber si es clásico. `Invoice` tiene que aplicar el +50 % al sumar las órdenes. |
| ¿Dónde va el +50 %? | En `Invoice`, al calcular el importe. **No** en `WorkOrder`: lo prohíbe el enunciado, y además la orden vale lo mismo; solo **se factura** más cara. |
| ¿Cambia el mapeo? | **Sí.** El atributo (`registeredDate`) y la columna (`REGISTRATION_DATE`) se llaman distinto, así que JPA no lo adivina. |
| ¿Cambia la base de datos? | **No hay que hacer nada:** la columna ya viene creada en la BD del ZIP. |
| ¿Hay caso de uso nuevo? | **No.** El comando `CreateInvoiceFor` ya hace `new Invoice(número, órdenes)`, y es la factura la que calcula su importe. Si el cálculo cambia en el dominio, el servicio funciona solo. |

Fíjate en la última fila: **la lógica está en el dominio**, así que un cambio de regla de negocio no
toca el servicio. Es justo lo que la asignatura quiere que entiendas, y esta prueba lo comprueba.

¿Cómo calcula el test lo que espera? Mirar el test ayuda a no adivinar. En
`DemoInvoiceHelper.computeFor` (en los tests del ZIP):

```text
subtotal = importe de la orden × 1,5          (redondeado a céntimos)
IVA      = subtotal × 21 %                     (redondeado a céntimos)
total    = subtotal + IVA
```

Y el vehículo del test se matriculó el `1965-05-15`, así que es clásico.

### 11.5 Fase 4: resolverlo, paso a paso

#### Paso 1. `Vehicle`: el nuevo atributo y la regla "es clásico"

En `domain/Vehicle.java`:

**1.a) Una constante y el atributo.** El "30" es una regla del negocio, así que se le pone nombre en
vez de dejar un número suelto en el código:

```java
public class Vehicle extends BaseEntity {
    private static final int CLASSIC_AGE_YEARS = 30;

    // Natural attributes
    private LicensePlate licensePlate;
    private String make;
    private String model;
    private LocalDate registeredDate;          // ← nuevo
    ...
```

Añade arriba `import java.time.LocalDate;`.

**1.b) Constructores.** Se añade uno completo con la fecha, y el que ya había lo llama poniendo la
fecha de hoy (**encadenar constructores**, parte 10). Así no se rompen los tests de dominio, que crean
vehículos con `new Vehicle(matrícula)`:

```java
public Vehicle(LicensePlate plateNumber, String make, String model) {
    this(plateNumber, make, model, LocalDate.now());
}

public Vehicle(LicensePlate plateNumber, String make, String model, LocalDate registeredDate) {
    ArgumentChecks.isNotNull(plateNumber, "License plate cannot be null");
    ArgumentChecks.isNotBlank(make, "Make cannot be empty");
    ArgumentChecks.isNotBlank(model, "Model cannot be empty");
    ArgumentChecks.isNotNull(registeredDate, "Registered date cannot be null");

    this.licensePlate = plateNumber;
    this.make = make;
    this.model = model;
    this.registeredDate = registeredDate;
}
```

**1.c) La regla de negocio y el getter:**

```java
/**
 * A vehicle is a classic if it was registered more than 30 years ago
 */
public boolean isClassic() {
    return registeredDate != null
            && registeredDate.isBefore(LocalDate.now().minusYears(CLASSIC_AGE_YEARS));
}

public LocalDate getRegisteredDate() {
    return registeredDate;
}
```

* `LocalDate.now().minusYears(30)` es "la fecha de hoy hace 30 años". Si se matriculó **antes** de esa
  fecha (`isBefore`), tiene más de 30 años.
* `registeredDate != null`: un vehículo guardado en la BD **sin fecha** (`NULL` en la columna) no se
  considera clásico, en vez de hacer fallar el programa.

¿Por qué `isClassic` va en `Vehicle` y no en `Invoice`? Porque "ser clásico" es una propiedad **del
vehículo**. Cada clase sabe lo suyo: el vehículo sabe si es clásico, y la factura sabe cuánto cobrar.

#### Paso 2. `Invoice`: aplicar el +50 % al sumar las órdenes

En `domain/Invoice.java` ya existe el método privado `computeAmount()`, que suma las órdenes y calcula
el IVA. Hasta ahora sumaba `wo.getAmount()`:

```java
private void computeAmount() {
    Money.MoneyCalculator calculator = Money.calc();
    workOrders.forEach(wo -> calculator.add(wo.getAmount()));      // ← antes
    ...
```

Se cambia esa línea para sumar **lo que se factura** de cada orden:

```java
    workOrders.forEach(wo -> calculator.add(amountToInvoice(wo))); // ← ahora
```

Y se añade el método que decide ese importe, y una constante para el 50 %:

```java
private static final Percentage CLASSIC_SURCHARGE = Percentage.ofPercent(50);

/**
 * The work orders of classic vehicles are invoiced with a 50% surcharge.
 * WorkOrder is not changed: the surcharge only applies when invoicing.
 */
private Money amountToInvoice(WorkOrder workOrder) {
    Money amount = workOrder.getAmount();
    Vehicle vehicle = workOrder.getVehicle();
    if (vehicle != null && vehicle.isClassic()) {
        return Money.calc(amount).with(CLASSIC_SURCHARGE).toMoneyExact();
    }
    return amount;
}
```

* `Money.calc(amount).with(CLASSIC_SURCHARGE)` es "el importe **con** un 50 % más", es decir, × 1,5. El
  value type `Money` ya trae esta operación: `with(porcentaje)` suma el porcentaje y `without(...)` lo
  quita.
* `toMoneyExact()` no redondea todavía, porque el redondeo a céntimos ya lo hace `computeAmount` al
  final (`toMoneyRoundedToCents()`). Así se redondea **una sola vez**, como hace el test.
* El resto de `computeAmount` (IVA y total) no cambia.

⚠️ **Trampa 3: `vehicle != null`.** La primera versión de la solución no tenía esa comprobación, y al
ejecutar los tests **fallaron varios tests de dominio de `Invoice`** que antes pasaban, con:

```text
NullPointerException: Cannot invoke "Vehicle.isClassic()" because the return value of
"WorkOrder.getVehicle()" is null
```

¿Por qué? Esos tests no usan órdenes de trabajo de verdad, sino **mocks**: objetos *falsos* creados con
la librería Mockito (`mock(WorkOrder.class)`) que solo responden lo que el test les ha enseñado. En este
caso, el importe y el estado. A todo lo demás responden `null`, también a `getVehicle()`. Como los tests
**no se pueden modificar**, el código tiene que aguantarlo: una orden sin vehículo no es de un clásico.

> Lección general: **ejecuta también los tests de `cws_services_jpa`** (dominio y mapeo), no solo el
> test nuevo. Un cambio en el dominio puede romper tests que antes pasaban.

#### Paso 3. El mapeo de la nueva columna

En `orm.xml`, dentro de la entidad `Vehicle`:

```xml
<entity class="uo.ri.cws.domain.Vehicle">
    <table name="TVEHICLES" />
    <attributes>
        <basic name="registeredDate">                <!-- ← nuevo -->
            <column name="REGISTRATION_DATE" />
        </basic>
        <many-to-one name="vehicleType" />
        <many-to-one name="client" />
        <one-to-many name="workOrders" mapped-by="vehicle" />
        <embedded name="licensePlate">
            ...
        </embedded>
    </attributes>
</entity>
```

* El atributo se llama `registeredDate`, y por defecto JPA buscaría la columna `REGISTEREDDATE`, que no
  existe. Por eso se le dice el nombre real con `<column name="REGISTRATION_DATE" />`.
* `LocalDate` se guarda en una columna `DATE` sin más configuración.
* ⚠️ El `<basic>` va **el primero** dentro de `<attributes>`, antes de los `many-to-one`: es el orden
  obligatorio del XML (parte 10, ejercicio 1, paso 2).

#### Paso 4. El servicio: nada

`CreateInvoiceFor` no se toca. Cuando hace `new Invoice(número, órdenes)`, el constructor llama a
`addWorkOrder`, que llama a `computeAmount`, que ahora aplica el recargo. **Comprueba que no hay nada más
que tocar antes de ponerte a escribir.**

### 11.6 Fase 5: comprobar y entregar

1. Ejecuta los tests de `cws_services_jpa`: 404 en verde. Con esto compruebas que no has roto el
   dominio ni el mapeo.
2. Ejecuta `RunServiceTests` en `cws_service_tests`: 52 en verde, incluido `[demo.I.C.1]`.
3. **Entrega:** comprime **solo** la carpeta `cws_services_jpa` en un ZIP y súbela al campus.

Resumen de lo que cambió, tres ficheros:

| Fichero | Cambio |
|---|---|
| `domain/Vehicle.java` | atributo `registeredDate`, constructor completo encadenado, `isClassic()`, getter |
| `domain/Invoice.java` | constante `CLASSIC_SURCHARGE`, método `amountToInvoice`, una línea de `computeAmount` |
| `META-INF/orm.xml` | `<basic name="registeredDate">` con la columna `REGISTRATION_DATE` |
| (`persistence.xml`) | la URL de la base de datos de la prueba |

### 11.7 Qué podrían cambiar el día de la prueba (y cómo lo adaptarías)

El esquema suele ser el mismo: **un dato nuevo en una entidad + una regla que lo usa**. Variantes
posibles:

| Si te piden... | Dónde va |
|---|---|
| "Los vehículos de tipo *X* tienen un descuento del 10 %" | `VehicleType` o `Vehicle` sabe si aplica; `Invoice.amountToInvoice` usa `without(...)` o `times(...)` |
| "Los clientes VIP pagan menos" | atributo en `Client` (+ mapeo + columna); la factura llega al cliente navegando `wo.getVehicle().getClient()` |
| "Una intervención de más de N minutos cuesta más" | `Intervention.getTotalAmount()` |
| "No se puede facturar una orden de un vehículo sin ITV" | atributo en `Vehicle`, y en `CreateInvoiceFor` un `BusinessChecks` previo con un método del dominio |
| "Un dato nuevo que se pide al dar de alta algo" | dominio + mapeo + el comando de alta y su DTO (el DTO te lo darían en la interfaz) |

Para cualquiera: 1) lee el test nuevo y su *helper*, que te dicen exactamente qué se espera;
2) aplica la plantilla de la parte 10; 3) ejecuta **todos** los tests al terminar.

> **Lo importante:** prepara bien los materiales (BD en la carpeta a la que apunta la URL; proyecto de
> tests **sustituido**, no mezclado) y ejecuta los tests antes de tocar nada. La modificación va en el
> **dominio** y en el **mapeo**. Cuidado con los `null` que dan los mocks de los tests de dominio.

---

## Parte 12. Glosario (para cuando te pierdas)

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
| **Mock** | Objeto falso que usan algunos tests (librería Mockito); solo responde lo que el test le enseña, y a lo demás devuelve `null`. |
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

## Parte 13. Comprueba si lo has entendido

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
3. Haz la ampliación de la parte 9 y después el ejercicio 1 de la parte 10 en una **copia** del
   proyecto, sin mirar la solución, y compárala al terminar. Hacerlo una vez con tus manos vale más que
   leerlo diez veces.
4. **Haz el simulacro completo (parte 11) por tu cuenta**, sobre una copia de `cws_extended_2026`: prepara
   los materiales del ZIP, comprueba que solo falla `[demo.I.C.1]`, resuélvelo y comprueba que todo
   queda en verde. Es lo más parecido al día de la prueba.
5. Para practicar más, `GUIA_PROYECTO_JPA.md` tiene mini-ejercicios propuestos al final del apartado 9
   y ejercicios de SQL en el apartado 6.
