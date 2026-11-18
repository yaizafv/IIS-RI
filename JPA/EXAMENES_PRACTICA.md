# Exámenes de práctica: prueba de autoría (CarWorkshop JPA)

Seis exámenes del estilo que ha anunciado el profesor: **un caso de uso nuevo** (tocando el dominio, el
mapeo y el servicio), con **dinero, fechas y horas**, y con **todos los tests en verde** al final. No hay
nada de interfaz gráfica (UI), porque esa parte la dan ellos.

Todas las soluciones están **probadas**: las apliqué sobre una copia de tu proyecto, ejecuté un test con
datos reales para cada examen y comprobé que todos los tests originales siguen pasando (404 de dominio/JPA
y 234 de servicio).

---

## Cómo usar este documento

1. Lee el **enunciado** de un examen y ponte un tiempo (unos 90 minutos).
2. Intenta resolverlo **en una copia de tu proyecto**, sin mirar la solución. Si te atascas, abre primero
   las **pistas**.
3. Luego abre la **solución** y compárala con lo tuyo. Cada solución explica *por qué* se hace cada cosa.

Las pistas y soluciones están plegadas: pulsa en "▶ Pistas" o "▶ Solución" para verlas (en VS Code,
abre la vista previa del Markdown con `Ctrl+Shift+V`).

### Qué te suelen dar y qué tienes que hacer tú

| Te lo dan | Lo haces tú |
|---|---|
| La **interfaz del servicio** con su DTO y su Javadoc (en `cws_service_interfaces`) | **Dominio**: atributos, reglas, asociaciones |
| El método `forXxxService()` en `ServiceFactory` | **Mapeo**: `orm.xml` (atributos y `named-query`) |
| La **base de datos** con las columnas o tablas nuevas (como en la TG) | **Persistencia**: métodos de repositorio o de *queries* |
| Normalmente, **tests** | **Servicio**: comandos, `DtoAssembler`, fachada (`...ServiceImpl`) y registrarla en `JpaServicesFactoryImpl` |

En cada examen pongo también el SQL de la base de datos, por si no te la dan hecha y quieres practicar.

### La plantilla de siempre (repásala antes de cada examen)

1. **Dominio.** ¿Qué atributo o clase nueva hay? ¿Es simple, un value type o una entidad? (README, parte 3.2.)
   ¿Qué regla de negocio hay? Ponla en la clase del dominio con `ArgumentChecks` (argumentos) y
   `StateChecks` (estado).
2. **Mapeo.** ¿La columna se llama igual que el atributo? Si es así, no hace falta tocar nada. Si no, usa
   `<basic>` con `<column name=...>`. Para un enum, `<enumerated>STRING</enumerated>`. Una entidad nueva
   lleva `<entity>` completo. Respeta el **orden** dentro de `<attributes>`:
   `basic` → `many-to-one` → `one-to-many` → `embedded` → `transient`.
3. **Consultas.** Cada búsqueda nueva lleva un `<named-query>` en el `orm.xml`, un método en la interfaz del
   repositorio y su implementación JPA. `getResultList()` para listas,
   `getResultStream().findFirst()` para "uno o ninguno", y `getSingleResult()` solo para `count`/`max`.
4. **Comando.**
   * En el **constructor**, `ArgumentChecks` para todo lo que el Javadoc dice `IllegalArgumentException`.
   * En `execute()`, `BusinessChecks` para todo lo que dice `BusinessException`.
   * **Antes** de llamar al dominio, comprueba con `BusinessChecks` lo mismo que el dominio comprobaría con
     `StateChecks`. Así el usuario recibe una `BusinessException` y no una `IllegalStateException`.
5. **Fachada** con una línea por método, y **registrarla** en `JpaServicesFactoryImpl`.
6. **¿Se guarda solo?** Si modificas un objeto que has traído de un repositorio, sí: no hay `update()`. Si
   creas un objeto nuevo, tienes que hacer `repo.add(...)` (salvo que lo guarde una cascada).
7. **Ejecuta TODOS los tests** (`mvn test` en el proyecto padre), no solo los del caso de uso nuevo.

---

## Lo que ha dicho el profesor (y qué significa)

> "La prueba consistirá en implementar un nuevo caso de uso y conseguir que pasen de nuevo todos los
> tests. Es probable que tengas que manejar cálculos monetarios, fechas y horas."

* **"Un nuevo caso de uso"**: un servicio nuevo, o un método nuevo en uno que ya existe. Es lo que practican
  los exámenes de abajo. El **examen 6** es el más parecido a lo anunciado.
* **"Que pasen de nuevo todos los tests"**: seguramente te darán tests nuevos que ahora fallan, y al acabar
  tienen que pasar **los nuevos y los antiguos**. El peligro está en cambiar algo que ya existe (un
  cálculo, un constructor, una regla) y romper otro caso de uso sin darte cuenta. Al final del examen 6 hay
  un experimento real que lo demuestra.
* **"Cálculos monetarios, fechas y horas"**: repasa la chuleta de aquí abajo. Todo lo que sale en ella está
  en tu proyecto y lo he probado.

---

## Chuleta: dinero, fechas y horas

### Dinero: `Money` y `Percentage` (en `domain/types`)

**Nunca** uses `double` para dinero: `0.1 + 0.2` da `0.30000000000000004`. En el dominio, el dinero
siempre es `Money`, que por dentro es un `BigDecimal` (decimal exacto).

**Crear:**

```java
Money.of("15.00")              // desde texto: la forma más segura
Money.of(dto.price)            // desde un BigDecimal (lo que traen los DTO)
Money.ZERO
```

**Calcular:** se abre una "calculadora" con `Money.calc(...)`, se encadenan operaciones y se cierra
con `toMoney...()`:

```java
Money total = Money.calc(precioHora)      // empieza en 15.00
        .times(minutos)                   // × 90
        .div(60)                          // ÷ 60  → 22.50
        .add(otroMoney)                   // +
        .subtract(otroMoney)              // −
        .toMoneyRoundedToCents();         // cierra redondeando a 2 decimales

Money.calc().add(a).add(b)                // empieza en 0 (para ir sumando en un bucle)
```

| Para... | Usa | Ejemplo con 100 € y 21 % |
|---|---|---|
| **sumar** un porcentaje (recargo, IVA incluido) | `.with(pct)` | 121 € |
| **quitar** un porcentaje que ya estaba sumado | `.without(pct)` | 100 € ÷ 1,21 |
| calcular **solo el porcentaje** (la cuota de IVA, un descuento) | `.times(pct)` | 21 € |
| aplicar un **descuento** del 10 % | `.times("0.90")` (multiplicar por lo que queda) | 90 € |

```java
Percentage iva = Percentage.ofPercent(21);    // 21 %
Percentage recargo = Percentage.ofPercent(50);
```

⚠️ **Trampa:** `times(pct)` **no** es "aplicar" el porcentaje: `times(21 %)` da 21 €, no 121 €. Para
sumarlo es `with`.

**Cerrar la calculadora:**

* `toMoneyExact()`: sin redondear. Úsalo en **resultados intermedios** (lo que devuelve un método que luego
  se va a sumar con otros).
* `toMoneyRoundedToCents()`: redondea a céntimos con `HALF_EVEN` (si está justo en la mitad, va al par:
  0,125 → 0,12 y 0,375 → 0,38). Úsalo en el **resultado final**, el que se guarda o se devuelve en el DTO.
* **Redondea una sola vez, al final.** Si redondeas cada sumando, los céntimos no cuadran con los del test.

**Comparar:**

```java
a.equals(b)            // 10.0 es igual a 10.00 (Money compara el valor, no los decimales)
a.greaterThan(b)       // >
a.greaterOrEqual(b)    // >=
a.isNonNegative()      // >= 0
```

**Pasar al DTO y desde el DTO:**

```java
dto.total = money.amount();            // Money → BigDecimal
Money m = Money.of(dto.total);         // BigDecimal → Money
```

En el **servicio**, un `BigDecimal` se compara con `compareTo`, nunca con `>` ni con `equals` (con
`equals`, `10.0` y `10.00` son distintos):

```java
BusinessChecks.isTrue(dto.price.compareTo(BigDecimal.ZERO) >= 0, "The price cannot be negative");
```

En el **JPQL**, para comparar un `Money` hay que entrar en su campo: `v.availableAmount.amount > 0`.

### Fechas y horas (`java.time`)

| Tipo | Qué es | Ejemplo en el proyecto |
|---|---|---|
| `LocalDate` | un día | `Invoice.date`, `Vehicle.registeredDate` |
| `LocalDateTime` | un día **y** una hora | `WorkOrder.date`, `Intervention.date` |
| `YearMonth` | un mes de un año | `CreditCard.getValidThru()` |
| `DayOfWeek` | lunes...domingo | `date.getDayOfWeek() == DayOfWeek.SATURDAY` |

```java
LocalDate hoy = LocalDate.now();
LocalDate d = LocalDate.of(2026, 3, 7);
LocalDateTime t = LocalDateTime.of(2026, 3, 7, 10, 30);    // 7/3/2026 10:30

d.plusDays(10)  d.minusYears(30)  d.plusMonths(1)          // devuelven una fecha NUEVA
a.isBefore(b)   a.isAfter(b)   a.equals(b)                 // comparar (no uses < ni >)
t.toLocalDate()  t.getHour()  t.getMinute()  t.getDayOfWeek()
d.atStartOfDay()                                           // LocalDate → LocalDateTime a las 00:00
ChronoUnit.DAYS.between(a, b)    ChronoUnit.MINUTES.between(t1, t2)   // cuántos hay entre dos

YearMonth mes = YearMonth.of(2026, 3);
mes.atDay(1)            // 1/3/2026
mes.atEndOfMonth()      // 31/3/2026
mes.plusMonths(1)       // abril
mes.isAfter(YearMonth.now())
```

⚠️ **Las fechas no se modifican:** `d.plusDays(1);` sola no hace nada. Tienes que guardar el resultado:
`d = d.plusDays(1);`.

**"Dentro de un periodo"**: es más seguro un intervalo **semiabierto** `[desde, hasta)`, con
`>=` en el inicio y `<` en el fin, que usar `between` con horas:

```java
LocalDateTime desde = mes.atDay(1).atStartOfDay();               // 1/3 00:00
LocalDateTime hasta = mes.plusMonths(1).atDay(1).atStartOfDay(); // 1/4 00:00 (no entra)
```

Así no pierdes lo que pasa el 31 a las 23:59:30, y lo del 1 de abril a las 00:00 no se cuela.

⚠️ **`<` dentro del `orm.xml`**: el XML no admite `<` suelto dentro de una consulta (cree que empieza una
etiqueta y fallan **todos** los tests al cargar). Escribe `&lt;`, o mete la consulta en un bloque
`<![CDATA[ ... ]]>` como ya hacen algunas de tu `orm.xml`. El `>` sí se puede escribir tal cual.

```xml
where i.date >= ?2 and i.date &lt; ?3
```

**Un detalle del proyecto:** `WorkOrder` e `Intervention` truncan la hora a milisegundos
(`date.truncatedTo(ChronoUnit.MILLIS)`) porque la base de datos no guarda más precisión. Si comparas
fechas que vienen de la base de datos con otras creadas en Java, trúncalas igual.

---

## Examen 1. Kilometraje de los vehículos ⭐ (fácil)

### Enunciado

El taller quiere saber cuántos kilómetros tiene cada vehículo para avisar de las revisiones.

**Dominio**

* Un `Vehicle` tiene un **kilometraje** (número entero de km). Un vehículo nuevo tiene 0 km.
* El kilometraje se actualiza con `updateMileage(int km)`:
  * si `km` es negativo → `IllegalArgumentException`
  * si `km` es **menor** que el kilometraje actual → `IllegalStateException` (los km nunca bajan)

**Base de datos.** La tabla `TVEHICLES` tiene una columna nueva `KILOMETERS INTEGER`.

**Servicio (dado)** en `uo.ri.cws.application.service.vehicle`:

```java
public interface VehicleMileageService {

    /**
     * Updates the mileage (km) of the vehicle with that plate.
     * @throws IllegalArgumentException if plate is null or blank, or km is negative
     * @throws BusinessException if there is no vehicle with that plate, or
     *         km is lower than the current mileage
     */
    void updateMileage(String plate, int km) throws BusinessException;

    /**
     * @return the vehicles with mileage >= km, highest mileage first. Might be empty.
     * @throws IllegalArgumentException if km is negative
     */
    List<VehicleMileageDto> findWithMileageOver(int km) throws BusinessException;

    public static class VehicleMileageDto {
        public String plate;
        public String make;
        public String model;
        public int mileage;
        public String clientNif;
    }
}
```

<details>
<summary>▶ Pistas</summary>

* El kilometraje es un atributo **simple** (`int`), no un value type.
* La columna **no** se llama como el atributo. ¿Qué tienes que poner en el `orm.xml`?
* Ya existe `VehicleRepository.findByPlate(LicensePlate)`. Te falta una búsqueda nueva.
* Si la matrícula viene mal escrita (por ejemplo `"hola"`), `new LicensePlate("hola")` lanza una excepción.
  ¿Cómo lo evitas? (Mira cómo lo hace `FindMechanicByNif` con `Nif.isValid`.)
* "Los km nunca bajan" lo comprueba el dominio con `StateChecks`, pero el servicio debe lanzar una
  `BusinessException`.

</details>

<details>
<summary>▶ Solución</summary>

#### 1. Dominio: `Vehicle.java`

```java
// Natural attributes
private LicensePlate licensePlate;
private String make;
private String model;
private int mileage = 0;            // NUEVO
```

```java
/**
 * Updates the odometer reading. Mileage can never go down.
 */
public void updateMileage(int km) {
    ArgumentChecks.isTrue(km >= 0, "Mileage cannot be negative");
    StateChecks.isTrue(km >= mileage, "Mileage cannot decrease");
    this.mileage = km;
}

public int getMileage() {
    return mileage;
}
```

Añade también `import uo.ri.util.assertion.StateChecks;`.

* El argumento negativo es un error de quien llama al método → `ArgumentChecks` (`IllegalArgumentException`).
* Bajar los km depende del **estado** actual del objeto → `StateChecks` (`IllegalStateException`).

#### 2. Mapeo: `orm.xml`

En la entidad `Vehicle`, como **primer** elemento de `<attributes>` (los `basic` van antes que los
`many-to-one`):

```xml
<entity class="uo.ri.cws.domain.Vehicle">
    <table name="TVEHICLES" />
    <attributes>
        <basic name="mileage">
            <column name="KILOMETERS" />
        </basic>
        <many-to-one name="vehicleType" />
        ...
```

¿Por qué hace falta? Por defecto JPA buscaría una columna `MILEAGE`. Como se llama `KILOMETERS`, hay que
decírselo.

La consulta, junto a las demás `<named-query>` del principio del fichero:

```xml
<named-query name="Vehicle.findWithMileageOver">
    <query>
        select v
        from Vehicle v
        where v.mileage >= ?1
        order by v.mileage desc
    </query>
</named-query>
```

En JPQL se usa el **atributo** Java (`v.mileage`), no la columna (`KILOMETERS`).

#### 3. Base de datos (si no te la dan)

```sql
ALTER TABLE TVEHICLES ADD COLUMN KILOMETERS INTEGER DEFAULT 0;
```

El `DEFAULT 0` es importante: los vehículos que ya existen tendrían la columna a `NULL`, y JPA no puede
meter un `NULL` en un `int`.

#### 4. Repositorio

`VehicleRepository.java`:

```java
List<Vehicle> findWithMileageOver(int km);
```

`VehicleJpaRepository.java`:

```java
@Override
public List<Vehicle> findWithMileageOver(int km) {
    return Jpa.getManager()
            .createNamedQuery("Vehicle.findWithMileageOver", Vehicle.class)
            .setParameter(1, km)
            .getResultList();
}
```

#### 5. Servicio: paquete `application/service/vehicle/mileage`

`commands/UpdateMileage.java`:

```java
public class UpdateMileage implements Command<Void> {

    private String plate;
    private int km;
    private VehicleRepository repo = Factories.repository.forVehicle();

    public UpdateMileage(String plate, int km) {
        ArgumentChecks.isNotBlank(plate, "The plate cannot be null or blank");
        ArgumentChecks.isTrue(km >= 0, "The mileage cannot be negative");
        this.plate = plate;
        this.km = km;
    }

    @Override
    public Void execute() throws BusinessException {
        BusinessChecks.isTrue(LicensePlate.isValid(plate), "The vehicle does not exist");
        Optional<Vehicle> ov = repo.findByPlate(new LicensePlate(plate));
        BusinessChecks.exists(ov, "The vehicle does not exist");
        Vehicle v = ov.get();
        BusinessChecks.isTrue(km >= v.getMileage(), "The mileage cannot decrease");

        v.updateMileage(km);
        v.updatedNow();
        return null;
    }
}
```

* `LicensePlate.isValid(plate)`: una matrícula mal escrita no puede corresponder a ningún vehículo, así que
  es lo mismo que "no existe" → `BusinessException`.
* `km >= v.getMileage()` se comprueba **antes** de `v.updateMileage(km)`. Si no, el dominio lanzaría una
  `IllegalStateException` y el Javadoc pide una `BusinessException`.
* No hay `repo.update(v)`: `v` está gestionado por JPA y se guarda solo al hacer commit.

`commands/FindVehiclesWithMileageOver.java`:

```java
public class FindVehiclesWithMileageOver implements Command<List<VehicleMileageDto>> {

    private int km;
    private VehicleRepository repo = Factories.repository.forVehicle();

    public FindVehiclesWithMileageOver(int km) {
        ArgumentChecks.isTrue(km >= 0, "The mileage cannot be negative");
        this.km = km;
    }

    @Override
    public List<VehicleMileageDto> execute() throws BusinessException {
        return repo.findWithMileageOver(km).stream()
                .map(DtoAssembler::toDto)
                .toList();
    }
}
```

`DtoAssembler.java`:

```java
public class DtoAssembler {

    public static VehicleMileageDto toDto(Vehicle v) {
        VehicleMileageDto dto = new VehicleMileageDto();
        dto.plate = v.getLicensePlate().licensePlate();
        dto.make = v.getMake();
        dto.model = v.getModel();
        dto.mileage = v.getMileage();
        dto.clientNif = v.getClient() == null ? null : v.getClient().getNif().nif();
        return dto;
    }
}
```

`v.getLicensePlate().licensePlate()`: `LicensePlate` es un `record` con un campo `licensePlate`, así que se
saca el `String` llamando a `licensePlate()`. Pasa lo mismo con `getNif().nif()`.

`VehicleMileageServiceImpl.java`:

```java
public class VehicleMileageServiceImpl implements VehicleMileageService {

    private CommandExecutor executor = Factories.executor.forExecutor();

    @Override
    public void updateMileage(String plate, int km) throws BusinessException {
        executor.execute(new UpdateMileage(plate, km));
    }

    @Override
    public List<VehicleMileageDto> findWithMileageOver(int km) throws BusinessException {
        return executor.execute(new FindVehiclesWithMileageOver(km));
    }
}
```

#### 6. Registro: `JpaServicesFactoryImpl.java`

```java
@Override
public VehicleMileageService forVehicleMileageService() {
    return new VehicleMileageServiceImpl();
}
```

</details>

---

## Examen 2. Órdenes de trabajo urgentes ⭐⭐ (cambia un cálculo)

### Enunciado

Algunos clientes pagan para que su reparación pase delante de las demás.

**Dominio**

* Una `WorkOrder` puede ser **urgente**. Al crearse no lo es.
* `markAsUrgent()` la marca como urgente. Solo se puede si la orden está `OPEN` o `ASSIGNED`, y solo una
  vez; si no → `IllegalStateException`.
* Cuando se **aprueba** una orden urgente, su importe (`amount`) lleva un **recargo del 20 %**.
  Ejemplo: 11 horas a 60 €/h = 660 € → urgente: 792 €.

**Base de datos.** `TWORKORDERS` tiene una columna nueva `URGENT BOOLEAN`.

**Servicio (dado)** en `uo.ri.cws.application.service.workorder`:

```java
public interface UrgentWorkOrderService {

    /**
     * Marks the work order as urgent. An urgent work order costs 20% more when approved.
     * @throws IllegalArgumentException if the id is null or blank
     * @throws BusinessException if the work order does not exist, it is
     *         neither OPEN nor ASSIGNED, or it is already urgent
     */
    void markAsUrgent(String workOrderId) throws BusinessException;

    /**
     * @return the urgent work orders still OPEN or ASSIGNED, oldest first
     */
    List<UrgentWorkOrderDto> findUrgentPending() throws BusinessException;

    public static class UrgentWorkOrderDto {
        public String id;
        public String plate;
        public String description;
        public LocalDateTime date;
        public String state;
        public String mechanicNif;   // null if not assigned
    }
}
```

<details>
<summary>▶ Pistas</summary>

* ¿Dónde se calcula el importe de una orden? Busca qué método llama `approve()`.
* Para sumar un porcentaje a un `Money` ya se usó algo en la TG: `Percentage.ofPercent(...)` y
  `calculator.with(...)`.
* La columna se llama `URGENT` y el atributo `urgent`. ¿Hace falta tocar el mapeo del atributo?
* En JPQL, para comparar con un valor de un enum hay que escribir el **nombre completo**:
  `uo.ri.cws.domain.WorkOrder.WorkOrderState.OPEN`. Ya hay un ejemplo en `WorkOrder.findNotInvoicedByClientNif`.

</details>

<details>
<summary>▶ Solución</summary>

#### 1. Dominio: `WorkOrder.java`

Una constante y un atributo:

```java
public class WorkOrder extends BaseEntity {
    private static final Percentage URGENT_SURCHARGE = Percentage.ofPercent(20);
    ...
    private boolean usedForVoucher = false;
    private boolean urgent = false;           // NUEVO
```

(con `import uo.ri.cws.domain.types.Percentage;`)

El método de negocio y su getter:

```java
public boolean isUrgent() {
    return urgent;
}

/**
 * Only open or assigned work orders can be marked as urgent, and only once.
 */
public void markAsUrgent() {
    StateChecks.isTrue(isOpen() || isAssigned(), "Work order is neither open nor assigned");
    StateChecks.isFalse(urgent, "Work order is already urgent");
    urgent = true;
}
```

Y el cálculo. `approve()` llama a `computeAmount()`, así que el recargo va ahí:

```java
private void computeAmount() {
    Money.MoneyCalculator calculator = Money.calc();
    interventions.forEach(i -> calculator.add(i.getTotalAmount()));
    if (urgent) {
        calculator.with(URGENT_SURCHARGE);    // multiplica por 1.20
    }
    amount = calculator.toMoneyRoundedToCents();
}
```

`with(porcentaje)` **suma** el porcentaje (multiplica por 1,20) y `without` lo quitaría. El recargo se
aplica **antes** de redondear a céntimos.

#### 2. Mapeo

**Nada para el atributo.** Es un `boolean`, la columna se llama `URGENT` (igual que el atributo) y el
acceso por defecto es por atributos (`FIELD`), así que JPA lo encuentra solo. Saber cuándo **no** tocar el
mapeo también cuenta.

La consulta:

```xml
<named-query name="WorkOrder.findUrgentPending">
    <query>
        select w
        from WorkOrder w
        where w.urgent = true
            and (w.state = uo.ri.cws.domain.WorkOrder.WorkOrderState.OPEN
                or w.state = uo.ri.cws.domain.WorkOrder.WorkOrderState.ASSIGNED)
        order by w.date
    </query>
</named-query>
```

Ojo con los **paréntesis** alrededor del `or`: sin ellos, el `and` se aplicaría solo al primer estado.

#### 3. Base de datos

```sql
ALTER TABLE TWORKORDERS ADD COLUMN URGENT BOOLEAN DEFAULT FALSE;
```

#### 4. Repositorio

`WorkOrderRepository.java`: `List<WorkOrder> findUrgentPending();`

`WorkOrderJpaRepository.java`:

```java
@Override
public List<WorkOrder> findUrgentPending() {
    return Jpa.getManager()
            .createNamedQuery("WorkOrder.findUrgentPending", WorkOrder.class)
            .getResultList();
}
```

#### 5. Servicio: paquete `application/service/workorder/urgent`

`commands/MarkAsUrgent.java`:

```java
public class MarkAsUrgent implements Command<Void> {

    private String workOrderId;
    private WorkOrderRepository repo = Factories.repository.forWorkOrder();

    public MarkAsUrgent(String workOrderId) {
        ArgumentChecks.isNotBlank(workOrderId, "The work order id cannot be null or blank");
        this.workOrderId = workOrderId;
    }

    @Override
    public Void execute() throws BusinessException {
        Optional<WorkOrder> ow = repo.findById(workOrderId);
        BusinessChecks.exists(ow, "The work order does not exist");
        WorkOrder w = ow.get();
        BusinessChecks.isTrue(w.isOpen() || w.isAssigned(), "The work order is neither open nor assigned");
        BusinessChecks.isFalse(w.isUrgent(), "The work order is already urgent");

        w.markAsUrgent();
        w.updatedNow();
        return null;
    }
}
```

Fíjate en que el comando repite las dos comprobaciones del dominio, pero como `BusinessChecks`. Es el
patrón de siempre: el dominio se protege con `StateChecks` y el servicio "traduce" a `BusinessException`.

`commands/FindUrgentPending.java`:

```java
public class FindUrgentPending implements Command<List<UrgentWorkOrderDto>> {

    private WorkOrderRepository repo = Factories.repository.forWorkOrder();

    @Override
    public List<UrgentWorkOrderDto> execute() throws BusinessException {
        return repo.findUrgentPending().stream()
                .map(DtoAssembler::toDto)
                .toList();
    }
}
```

(Sin parámetros: el constructor no tiene nada que comprobar.)

`DtoAssembler.java`:

```java
public static UrgentWorkOrderDto toDto(WorkOrder w) {
    UrgentWorkOrderDto dto = new UrgentWorkOrderDto();
    dto.id = w.getId();
    dto.plate = w.getVehicle().getLicensePlate().licensePlate();
    dto.description = w.getDescription();
    dto.date = w.getDate();
    dto.state = w.getState().name();       // enum -> "OPEN"
    dto.mechanicNif = w.getMechanic() == null ? null : w.getMechanic().getNif().nif();
    return dto;
}
```

Una orden `OPEN` **no tiene mecánico**: sin el `== null ? null :` saltaría un `NullPointerException`.

Fachada (`UrgentWorkOrderServiceImpl`) y registro (`forUrgentWorkOrderService()`) igual que en el examen 1.

#### Pregunta típica: "¿Por qué no aplicas el recargo en `markAsUrgent()`?"

Porque el importe de la orden **no existe todavía** cuando se marca como urgente (es 0 hasta que se
aprueba). El importe se calcula en `approve()` → `computeAmount()`, así que el recargo va ahí. Además, si
la orden se desaprueba y se vuelve a aprobar, el recargo se recalcula bien y no se aplica dos veces.

</details>

---

## Examen 3. Categorías de mecánicos ⭐⭐ (un enum)

### Enunciado

Los mecánicos tienen una **categoría profesional**: `JUNIOR`, `SENIOR` o `MASTER`.

**Dominio**

* Crea el enum `MechanicCategory` en `uo.ri.cws.domain`.
* Todo mecánico nuevo es `JUNIOR`.
* `promote()` lo sube a la siguiente categoría (JUNIOR → SENIOR → MASTER). Un `MASTER` no puede subir más →
  `IllegalStateException`.
* Para ascender, un mecánico necesita haber hecho **al menos 10 intervenciones** (esta regla la comprueba
  el servicio).

**Base de datos.** `TMECHANICS` tiene una columna nueva `CATEGORY VARCHAR(255)`, que guarda el **nombre**
de la categoría (`'JUNIOR'`...). Los mecánicos existentes son `JUNIOR`.

**Servicio (dado)** en `uo.ri.cws.application.service.mechanic`:

```java
public interface MechanicCareerService {

    /**
     * Promotes the mechanic to the next category: JUNIOR -> SENIOR -> MASTER.
     * @throws IllegalArgumentException if nif is null or blank
     * @throws BusinessException if there is no mechanic with that nif, the
     *         mechanic is already MASTER, or has less than 10 interventions
     */
    void promote(String nif) throws BusinessException;

    /**
     * @param category "JUNIOR", "SENIOR" or "MASTER"
     * @return the mechanics of that category sorted by surname. Might be empty.
     * @throws IllegalArgumentException if category is null or blank
     * @throws BusinessException if category is not one of the three
     */
    List<MechanicCareerDto> findByCategory(String category) throws BusinessException;

    public static class MechanicCareerDto {
        public String id;
        public String nif;
        public String name;
        public String surname;
        public String category;
    }
}
```

<details>
<summary>▶ Pistas</summary>

* Un enum se mapea con `<enumerated>`. ¿`STRING` u `ORDINAL`? Mira qué guarda la columna.
* ¿Hay ya alguna consulta que cuente las intervenciones de un mecánico? Busca en el `orm.xml` (la usa
  `DeleteMechanic`). **Reutilízala.**
* `MechanicCategory.valueOf("BOSS")` lanza `IllegalArgumentException`. Compruébalo antes.
* En JPQL, para ordenar por apellido hay que navegar por el value type: `m.fullName.surname`.

</details>

<details>
<summary>▶ Solución</summary>

#### 1. Dominio

`MechanicCategory.java` (nuevo):

```java
package uo.ri.cws.domain;

public enum MechanicCategory {
    JUNIOR, SENIOR, MASTER;

    /**
     * @return true if the text is the name of a category (e.g. "SENIOR")
     */
    public static boolean isValid(String category) {
        for (MechanicCategory c : values()) {
            if (c.name().equals(category)) {
                return true;
            }
        }
        return false;
    }
}
```

El `isValid` imita lo que hacen los value types (`Nif.isValid`, `Email.isValid`...) y sirve para no llamar a
`valueOf` con un texto incorrecto.

`Mechanic.java`:

```java
public class Mechanic extends BaseEntity {
    public static final int INTERVENTIONS_TO_PROMOTE = 10;

    private Nif nif;
    private FullName fullName;
    private MechanicCategory category = MechanicCategory.JUNIOR;   // NUEVO
    ...

public MechanicCategory getCategory() {
    return category;
}

public boolean canBePromoted() {
    return category != MechanicCategory.MASTER;
}

/**
 * JUNIOR -> SENIOR -> MASTER
 */
public void promote() {
    StateChecks.isTrue(canBePromoted(), "A master mechanic cannot be promoted");
    if (category == MechanicCategory.JUNIOR) {
        category = MechanicCategory.SENIOR;
    } else {
        category = MechanicCategory.MASTER;
    }
}
```

(con `import uo.ri.util.assertion.StateChecks;`)

¿Por qué la regla de las 10 intervenciones **no** está en `promote()`? Para contarlas, el `Mechanic`
tendría que tener la colección de todas sus intervenciones, y no la tiene (solo tiene `assigned`, las
órdenes asignadas ahora). Contar es trabajo de una **consulta**, así que lo hace el servicio.

#### 2. Mapeo

En la entidad `Mechanic`, **antes** del `one-to-many`:

```xml
<entity class="uo.ri.cws.domain.Mechanic">
    <table name="TMECHANICS" />
    <attributes>
        <basic name="category">
            <enumerated>STRING</enumerated>
        </basic>
        <one-to-many name="assigned" mapped-by="mechanic" />
        ...
```

* `STRING` guarda `'JUNIOR'`. Sin esa línea, JPA usaría `ORDINAL` y guardaría `0`, `1` o `2`, que no encaja
  con una columna `VARCHAR`. Es lo mismo que ya se hace con `state` en `WorkOrder` e `Invoice`.
* La columna se llama `CATEGORY`, igual que el atributo, así que no hace falta `<column>`.

La consulta:

```xml
<named-query name="Mechanic.findByCategory">
    <query>
        select m
        from Mechanic m
        where m.category = ?1
        order by m.fullName.surname
    </query>
</named-query>
```

Al `?1` se le pasa el **enum** (`MechanicCategory.SENIOR`), no el texto.

#### 3. Base de datos

```sql
ALTER TABLE TMECHANICS ADD COLUMN CATEGORY VARCHAR(255) DEFAULT 'JUNIOR';
```

El `DEFAULT 'JUNIOR'` hace que los mecánicos que ya existen sean `JUNIOR`, como pide el enunciado.

#### 4. Repositorio y consultas

`MechanicRepository.java`: `List<Mechanic> findByCategory(MechanicCategory category);`

`MechanicJpaRepository.java` (añade `import java.util.List;` si no estaba):

```java
@Override
public List<Mechanic> findByCategory(MechanicCategory category) {
    return Jpa.getManager()
            .createNamedQuery("Mechanic.findByCategory", Mechanic.class)
            .setParameter(1, category)
            .getResultList();
}
```

Para contar intervenciones **ya existe** la named query `Mechanic.countInterventions` (la usa
`hasInterventions` al borrar un mecánico). Solo falta un método que devuelva el número.

`MechanicQueries.java`: `long countInterventions(String mechanicId);`

`MechanicJpaQueries.java`:

```java
@Override
public long countInterventions(String mechanicId) {
    return Jpa.getManager()
            .createNamedQuery("Mechanic.countInterventions", Long.class)
            .setParameter(1, mechanicId)
            .getSingleResult();
}
```

#### 5. Servicio: paquete `application/service/mechanic/career`

`commands/PromoteMechanic.java`:

```java
public class PromoteMechanic implements Command<Void> {

    private String nif;
    private MechanicRepository repo = Factories.repository.forMechanic();
    private MechanicQueries queries = Factories.query.forMechanic();

    public PromoteMechanic(String nif) {
        ArgumentChecks.isNotBlank(nif, "The nif cannot be null or blank");
        this.nif = nif;
    }

    @Override
    public Void execute() throws BusinessException {
        BusinessChecks.isTrue(Nif.isValid(nif), "The mechanic does not exist");
        Optional<Mechanic> om = repo.findByNif(new Nif(nif));
        BusinessChecks.exists(om, "The mechanic does not exist");
        Mechanic m = om.get();
        BusinessChecks.isTrue(m.canBePromoted(), "The mechanic is already MASTER");
        BusinessChecks.isTrue(queries.countInterventions(m.getId()) >= Mechanic.INTERVENTIONS_TO_PROMOTE,
                "The mechanic has not enough interventions");

        m.promote();
        m.updatedNow();
        return null;
    }
}
```

`commands/FindMechanicsByCategory.java`:

```java
public class FindMechanicsByCategory implements Command<List<MechanicCareerDto>> {

    private String category;
    private MechanicRepository repo = Factories.repository.forMechanic();

    public FindMechanicsByCategory(String category) {
        ArgumentChecks.isNotBlank(category, "The category cannot be null or blank");
        this.category = category;
    }

    @Override
    public List<MechanicCareerDto> execute() throws BusinessException {
        BusinessChecks.isTrue(MechanicCategory.isValid(category), "Unknown category");
        MechanicCategory c = MechanicCategory.valueOf(category);

        return repo.findByCategory(c).stream()
                .map(FindMechanicsByCategory::toDto)
                .toList();
    }

    private static MechanicCareerDto toDto(Mechanic m) {
        MechanicCareerDto dto = new MechanicCareerDto();
        dto.id = m.getId();
        dto.nif = m.getNif().nif();
        dto.name = m.getFullName().name();
        dto.surname = m.getFullName().surname();
        dto.category = m.getCategory().name();
        return dto;
    }
}
```

(Aquí el `toDto` va dentro del comando para que veas que también vale. Si el paquete ya tiene un
`DtoAssembler`, ponlo ahí.)

Fachada (`MechanicCareerServiceImpl`) y registro (`forMechanicCareerService()`) como siempre.

</details>

---

## Examen 4. Reclamaciones sobre facturas ⭐⭐⭐ (entidad nueva)

### Enunciado

Un cliente puede **reclamar** una factura que ya ha pagado.

**Dominio**

* Clase nueva `Complaint` (reclamación) con:
  * `date` (`LocalDate`): el día de la reclamación (por defecto, hoy)
  * `reason` (`String`): el motivo, que no puede estar vacío
  * `state`: `OPEN` al crearse, y `RESOLVED` cuando se resuelve
  * la **factura** reclamada
* Una factura puede tener muchas reclamaciones; cada reclamación es de una única factura.
* Solo se pueden reclamar facturas **pagadas** → si no, `IllegalStateException`.
* Una reclamación se identifica por su **factura y su día**: no puede haber dos de la misma factura el mismo
  día.
* `resolve()` la pasa a `RESOLVED`; si ya lo estaba → `IllegalStateException`.

**Base de datos.** Tabla nueva:

```sql
CREATE TABLE TCOMPLAINTS (
    ID VARCHAR(255) NOT NULL PRIMARY KEY,
    CREATEDAT TIMESTAMP, ENTITYSTATE VARCHAR(255), UPDATEDAT TIMESTAMP, VERSION BIGINT,
    DATE DATE,
    REASON VARCHAR(255),
    STATE VARCHAR(255),
    INVOICE_ID VARCHAR(255),
    CONSTRAINT UNQ_TCOMPLAINTS_0 UNIQUE (INVOICE_ID, DATE),
    CONSTRAINT FK_TCOMPLAINTS_INVOICE_ID FOREIGN KEY (INVOICE_ID) REFERENCES TINVOICES (ID)
);
```

**Servicio (dado)** en `uo.ri.cws.application.service.invoice`:

```java
public interface ComplaintService {

    /**
     * Files a complaint about a paid invoice. Only one complaint per invoice and day.
     * @throws IllegalArgumentException if reason is null or blank
     * @throws BusinessException if the invoice does not exist, is not paid,
     *         or already has a complaint today
     */
    ComplaintDto file(long invoiceNumber, String reason) throws BusinessException;

    /**
     * @throws IllegalArgumentException if the id is null or blank
     * @throws BusinessException if the complaint does not exist or is already resolved
     */
    void resolve(String complaintId) throws BusinessException;

    /**
     * @return the OPEN complaints about invoices of that client. Might be empty.
     * @throws IllegalArgumentException if nif is null or blank
     */
    List<ComplaintDto> findOpenByClientNif(String nif) throws BusinessException;

    public static class ComplaintDto {
        public String id;
        public long version;
        public long invoiceNumber;
        public LocalDate date;
        public String reason;
        public String state;
    }
}
```

<details>
<summary>▶ Pistas</summary>

* Es una **entidad** nueva: clase que extiende `BaseEntity`, constructor vacío para JPA, `equals`/`hashCode`
  con su identidad (factura + día).
* La relación factura 1 — * reclamación es como la de vehículo 1 — * orden de trabajo. Copia el patrón de
  `Associations.Fixes`: clase nueva en `Associations`, `_setInvoice` en `Complaint`, y `_getComplaints` en
  `Invoice`.
* ¿Dónde se enlaza, en el constructor o en el comando? La factura forma parte de la **identidad** de la
  reclamación (regla del foro).
* Las cascadas: ¿se guardará la reclamación sola al guardar la factura? Mira si pones `cascade` en el
  mapeo. Si no, ¿qué tienes que hacer en el comando?
* La consulta del cliente tiene que ir de la reclamación al cliente: reclamación → factura → órdenes de
  trabajo → vehículo → cliente. Hará falta un `join`.
* Necesitas un repositorio nuevo **y registrarlo** en `PersistenceFactory` y `JpaRepositoryFactory`.

</details>

<details>
<summary>▶ Solución</summary>

#### 1. Dominio

`Complaint.java` (nuevo):

```java
package uo.ri.cws.domain;

public class Complaint extends BaseEntity {

    public enum ComplaintState {
        OPEN, RESOLVED
    }

    // Natural attributes
    private LocalDate date;
    private String reason;
    private ComplaintState state = ComplaintState.OPEN;

    // Accidental attributes
    private Invoice invoice;

    Complaint() {
        // for JPA
    }

    public Complaint(Invoice invoice, String reason) {
        this(invoice, LocalDate.now(), reason);
    }

    public Complaint(Invoice invoice, LocalDate date, String reason) {
        ArgumentChecks.isNotNull(invoice, "Invoice cannot be null");
        ArgumentChecks.isNotNull(date, "Date cannot be null");
        ArgumentChecks.isNotBlank(reason, "Reason cannot be empty");
        StateChecks.isTrue(invoice.isSettled(), "Only paid invoices can receive complaints");

        this.date = date;
        this.reason = reason;
        // the invoice is part of the identity: link from the constructor
        Associations.Claims.link(invoice, this);
    }

    public void resolve() {
        StateChecks.isTrue(isOpen(), "The complaint is already resolved");
        state = ComplaintState.RESOLVED;
    }

    public boolean isOpen() {
        return state == ComplaintState.OPEN;
    }

    public LocalDate getDate() { return date; }
    public String getReason() { return reason; }
    public ComplaintState getState() { return state; }
    public Invoice getInvoice() { return invoice; }

    void _setInvoice(Invoice invoice) {
        this.invoice = invoice;
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, invoice);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Complaint other = (Complaint) obj;
        return Objects.equals(date, other.date) && Objects.equals(invoice, other.invoice);
    }

    @Override
    public String toString() {
        return "Complaint [date=" + date + ", reason=" + reason + ", state=" + state + "]";
    }
}
```

Cosas en las que fijarse:

* **`Complaint()` sin argumentos y sin `public`**: JPA lo necesita para crear el objeto al leer de la base
  de datos.
* **`equals`/`hashCode` con `date` + `invoice`**, que es la identidad que dice el enunciado. Nunca con el
  `id`.
* **Se enlaza en el constructor** porque la factura forma parte de la identidad. Una reclamación sin
  factura no tiene sentido (como una `Intervention` sin su orden).
* **`toString` no incluye `invoice`**, para evitar bucles al imprimir y cargas innecesarias.

`Associations.java`, una clase nueva (copiada del patrón `Fixes`):

```java
public static class Claims {

    /* package */ static void link(Invoice invoice, Complaint complaint) {
        ArgumentChecks.isNotNull(invoice);
        ArgumentChecks.isNotNull(complaint);
        StateChecks.isTrue(complaint.getInvoice() == null, "Complaint already has an invoice");
        StateChecks.isFalse(invoice._getComplaints().contains(complaint), "Invoice already has this complaint");

        complaint._setInvoice(invoice);
        invoice._getComplaints().add(complaint);
    }

    public static void unlink(Complaint complaint) {
        ArgumentChecks.isNotNull(complaint);
        Invoice invoice = complaint.getInvoice();
        StateChecks.isTrue(invoice != null, "Complaint has no invoice");

        invoice._getComplaints().remove(complaint);
        complaint._setInvoice(null);
    }
}
```

**Orden importante en `link`:** primero `_setInvoice` y después `add`. El `hashCode` de `Complaint` usa
`invoice`, así que si se metiera en el `HashSet` con `invoice == null` y luego se cambiara, el `Set` ya no
la encontraría.

`Invoice.java`, el otro extremo:

```java
private Set<Charge> charges = new HashSet<>();
private Set<Complaint> complaints = new HashSet<>();     // NUEVO
```

```java
Set<Complaint> _getComplaints() {
    return complaints;
}

public Set<Complaint> getComplaints() {
    return new HashSet<>(complaints);
}

public boolean hasComplaintOn(LocalDate day) {
    return complaints.stream().anyMatch(c -> c.getDate().equals(day));
}
```

#### 2. Mapeo

La entidad nueva (con las demás `<entity>`, antes de los `<embeddable>`):

```xml
<entity class="uo.ri.cws.domain.Complaint">
    <table name="TCOMPLAINTS">
        <unique-constraint>
            <column-name>INVOICE_ID</column-name>
            <column-name>DATE</column-name>
        </unique-constraint>
    </table>
    <attributes>
        <basic name="state">
            <enumerated>STRING</enumerated>
        </basic>
        <many-to-one name="invoice" />
    </attributes>
</entity>
```

* `date` y `reason` no necesitan nada porque sus columnas se llaman igual.
* `<many-to-one name="invoice" />` crea la columna `INVOICE_ID` (atributo `invoice` + `_ID`).
* `<unique-constraint>` refleja la identidad natural, igual que hace `WorkOrder` con `VEHICLE_ID, DATE`.

En `Invoice`, después del `one-to-many` de `charges`:

```xml
<one-to-many name="complaints" mapped-by="invoice" />
```

`mapped-by="invoice"` significa que la clave ajena la guarda el otro lado (`Complaint.invoice`), es decir,
que **no** hay tabla intermedia.

La consulta:

```xml
<named-query name="Complaint.findOpenByClientNif">
    <query>
        select distinct c
        from Complaint c
            join c.invoice i
            join i.workOrders w
        where w.vehicle.client.nif = ?1
            and c.state = uo.ri.cws.domain.Complaint.ComplaintState.OPEN
    </query>
</named-query>
```

* `join i.workOrders w`: `workOrders` es una **colección**, así que no se puede escribir
  `c.invoice.workOrders.vehicle`. Hay que darle nombre (`w`) con un `join`. Por los atributos simples
  (`w.vehicle.client`) sí se puede navegar con puntos.
* `distinct`: una factura con 3 órdenes daría la misma reclamación 3 veces.
* `?1` es un `Nif` (value type), igual que en `Voucher.findByClientNif`.

#### 3. Base de datos

Es el `CREATE TABLE` del enunciado. Las columnas `ID`, `CREATEDAT`, `ENTITYSTATE`, `UPDATEDAT` y `VERSION`
vienen de `BaseEntity`: **todas** las tablas las tienen.

#### 4. Repositorio nuevo

`application/repository/ComplaintRepository.java`:

```java
public interface ComplaintRepository extends Repository<Complaint> {

    /**
     * @return the OPEN complaints about invoices of the client with that nif
     */
    List<Complaint> findOpenByClientNif(Nif nif);
}
```

`infrastructure/persistence/jpa/repository/ComplaintJpaRepository.java`:

```java
public class ComplaintJpaRepository
        extends BaseJpaRepository<Complaint>
        implements ComplaintRepository {

    @Override
    public List<Complaint> findOpenByClientNif(Nif nif) {
        return Jpa.getManager()
                .createNamedQuery("Complaint.findOpenByClientNif", Complaint.class)
                .setParameter(1, nif)
                .getResultList();
    }
}
```

`add`, `remove` y `findById` ya vienen de `BaseJpaRepository`.

Y el registro (**se olvida mucho**). En `PersistenceFactory.java`:

```java
ComplaintRepository forComplaint();
```

En `JpaRepositoryFactory.java`:

```java
@Override
public ComplaintRepository forComplaint() {
    return new ComplaintJpaRepository();
}
```

#### 5. Servicio: paquete `application/service/invoice/complaint`

`commands/FileComplaint.java`:

```java
public class FileComplaint implements Command<ComplaintDto> {

    private long invoiceNumber;
    private String reason;
    private InvoiceRepository invoiceRepo = Factories.repository.forInvoice();
    private ComplaintRepository complaintRepo = Factories.repository.forComplaint();

    public FileComplaint(long invoiceNumber, String reason) {
        ArgumentChecks.isNotBlank(reason, "The reason cannot be null or blank");
        this.invoiceNumber = invoiceNumber;
        this.reason = reason;
    }

    @Override
    public ComplaintDto execute() throws BusinessException {
        Optional<Invoice> oi = invoiceRepo.findByNumber(invoiceNumber);
        BusinessChecks.exists(oi, "The invoice does not exist");
        Invoice invoice = oi.get();
        BusinessChecks.isTrue(invoice.isSettled(), "The invoice is not paid");
        BusinessChecks.isFalse(invoice.hasComplaintOn(LocalDate.now()),
                "The invoice already has a complaint today");

        Complaint c = new Complaint(invoice, reason);
        complaintRepo.add(c);
        return DtoAssembler.toDto(c);
    }
}
```

**`complaintRepo.add(c)` es obligatorio.** En el mapeo de `complaints` no hay `cascade`, así que guardar la
factura no guarda la reclamación. Sin el `add`, la reclamación se quedaría solo en memoria y desaparecería.
(Compáralo con `Invoice.charges`, que sí tiene `cascade-all` y por eso los cargos se guardan solos.)

`commands/ResolveComplaint.java`:

```java
public class ResolveComplaint implements Command<Void> {

    private String complaintId;
    private ComplaintRepository repo = Factories.repository.forComplaint();

    public ResolveComplaint(String complaintId) {
        ArgumentChecks.isNotBlank(complaintId, "The complaint id cannot be null or blank");
        this.complaintId = complaintId;
    }

    @Override
    public Void execute() throws BusinessException {
        Optional<Complaint> oc = repo.findById(complaintId);
        BusinessChecks.exists(oc, "The complaint does not exist");
        Complaint c = oc.get();
        BusinessChecks.isTrue(c.isOpen(), "The complaint is already resolved");

        c.resolve();
        c.updatedNow();
        return null;
    }
}
```

`commands/FindOpenComplaintsByClientNif.java`:

```java
public class FindOpenComplaintsByClientNif implements Command<List<ComplaintDto>> {

    private String nif;
    private ComplaintRepository repo = Factories.repository.forComplaint();

    public FindOpenComplaintsByClientNif(String nif) {
        ArgumentChecks.isNotBlank(nif, "The nif cannot be null or blank");
        this.nif = nif;
    }

    @Override
    public List<ComplaintDto> execute() throws BusinessException {
        if (!Nif.isValid(nif)) {
            return List.of();      // a malformed nif has no complaints
        }
        return repo.findOpenByClientNif(new Nif(nif)).stream()
                .map(DtoAssembler::toDto)
                .toList();
    }
}
```

`DtoAssembler.java`:

```java
public static ComplaintDto toDto(Complaint c) {
    ComplaintDto dto = new ComplaintDto();
    dto.id = c.getId();
    dto.version = c.getVersion();
    dto.invoiceNumber = c.getInvoice().getNumber();
    dto.date = c.getDate();
    dto.reason = c.getReason();
    dto.state = c.getState().name();
    return dto;
}
```

Fachada (`ComplaintServiceImpl`, con tres métodos de una línea) y registro (`forComplaintService()`) como
siempre.

#### Resumen de lo tocado

| Capa | Ficheros |
|---|---|
| Dominio | `Complaint` (nuevo), `Associations.Claims` (nuevo), `Invoice` |
| Mapeo | `<entity Complaint>`, `one-to-many` en `Invoice`, 1 `named-query` |
| BD | tabla `TCOMPLAINTS` |
| Persistencia | `ComplaintRepository` + `ComplaintJpaRepository` (nuevos), `PersistenceFactory`, `JpaRepositoryFactory` |
| Servicio | 3 comandos, `DtoAssembler`, `ComplaintServiceImpl`, `JpaServicesFactoryImpl` |

</details>

---

## Examen 5. Caducidad de los bonos ⭐⭐⭐ (estilo TG: cambia una regla existente)

### Enunciado

A partir de ahora, los **bonos** (`Voucher`) caducan.

**Dominio**

* Un `Voucher` tiene una **fecha de caducidad**. Al crearse, caduca **dentro de un año**.
* Un bono está **caducado** si hoy es posterior a su fecha de caducidad (el mismo día de la caducidad aún
  vale).
* Un bono caducado **no se puede usar para pagar** (`canBeCharged` devuelve `false`), aunque le quede saldo.
* `extend(int months)` retrasa la caducidad esos meses:
  * `months <= 0` → `IllegalArgumentException`
  * bono ya caducado → `IllegalStateException`

**Base de datos.** `TVOUCHERS` tiene una columna nueva `EXPIRATION_DATE DATE`. Los bonos que ya existían
caducan el 31/12/2027.

**Servicio (dado)** en `uo.ri.cws.application.service.paymentmean`:

```java
public interface VoucherExpirationService {

    /**
     * @return the vouchers of the client with money left that expire between
     *         today and today + days (both included), earliest first.
     * @throws IllegalArgumentException if nif is null or blank, or days < 0
     */
    List<ExpiringVoucherDto> findExpiringSoon(String clientNif, int days) throws BusinessException;

    /**
     * Delays the expiration date of the voucher.
     * @throws IllegalArgumentException if code is null or blank, or months <= 0
     * @throws BusinessException if the voucher does not exist or it is already expired
     */
    void extend(String code, int months) throws BusinessException;

    public static class ExpiringVoucherDto {
        public String code;
        public String description;
        public BigDecimal available;
        public LocalDate expiration;
    }
}
```

<details>
<summary>▶ Pistas</summary>

* Como en la TG, el cambio importante está en una regla que **ya existe**: busca `canBeCharged` en `Voucher`.
  Al cambiarla, el pago de facturas (`settle`) la respeta automáticamente. No hay que tocar `Invoice`.
* Los constructores de `Voucher` ya existen y los usan los tests y `GenerateVouchers`. **No cambies su
  firma**: pon la caducidad por defecto dentro del constructor.
* "Caducado" = `LocalDate.now().isAfter(expiration)`. Piensa por qué no es `!isBefore`.
* En JPQL hay `between ?2 and ?3`. Para el saldo, navega por el value type: `v.availableAmount.amount`.
* Ya tienes `PaymentMeanRepository.findVoucherByCode(VoucherCode)`.

</details>

<details>
<summary>▶ Solución</summary>

#### 1. Dominio: `Voucher.java`

```java
private VoucherCode code;
private String description;
private Money availableAmount; // cents
private LocalDate expiration;          // NUEVO
```

En el constructor completo (los otros delegan en él, así que se aplica a todos):

```java
public Voucher(VoucherCode code, String description, Money available) {
    ...
    this.availableAmount = Money.calc(available).toMoneyRoundedToCents();
    this.expiration = LocalDate.now().plusYears(1);       // NUEVO
}
```

Los métodos nuevos:

```java
public LocalDate getExpiration() {
    return expiration;
}

public boolean isExpired() {
    return LocalDate.now().isAfter(expiration);
}

/**
 * Delays the expiration date the given number of months
 */
public void extend(int months) {
    ArgumentChecks.isTrue(months > 0, "Months must be positive");
    StateChecks.isFalse(isExpired(), "An expired voucher cannot be extended");
    expiration = expiration.plusMonths(months);
}
```

Y el cambio de la regla existente:

```java
@Override
public boolean canBeCharged(Money amount) {
    checkPositive(amount);
    return !isExpired() && availableAmount.greaterOrEqual(amount);   // antes: solo el saldo
}
```

(con `import java.time.LocalDate;` y `import uo.ri.util.assertion.StateChecks;`)

* `isAfter` y no `!isBefore`: el mismo día de la caducidad, `now.isAfter(expiration)` es `false`, así que el
  bono **todavía vale**, como pide el enunciado.
* ¿Por qué ya no se puede pagar con un bono caducado? `Invoice.settle()` llama a
  `paymentMean.canBeCharged(...)` para cada medio de pago, y `PaymentMean.charge()` también. Como cada
  subclase decide su `canBeCharged` (polimorfismo), basta con cambiarlo en `Voucher`.

#### 2. Mapeo

En la entidad `Voucher`, antes de los `embedded`:

```xml
<entity class="uo.ri.cws.domain.Voucher">
    <table name="TVOUCHERS" />
    <attributes>
        <basic name="expiration">
            <column name="EXPIRATION_DATE" />
        </basic>
        <embedded name="code">
        ...
```

`Voucher` hereda de `PaymentMean` (herencia `JOINED`: una tabla por clase). El atributo es de `Voucher`,
así que la columna va en `TVOUCHERS` y el mapeo va en la entidad `Voucher`, no en `PaymentMean`.

La consulta:

```xml
<named-query name="Voucher.findExpiringByClientNif">
    <query>
        select v
        from Voucher v
        where v.client.nif = ?1
            and v.expiration between ?2 and ?3
            and v.availableAmount.amount > 0
        order by v.expiration
    </query>
</named-query>
```

* `between` incluye los dos extremos, que es lo que pide el Javadoc ("both included").
* `v.availableAmount.amount`: `availableAmount` es un `Money` (embedded) y `amount` es su campo
  `BigDecimal`. No se puede comparar `v.availableAmount > 0` directamente.
* `v.client` se hereda de `PaymentMean`, y en JPQL se puede usar sin problema.

#### 3. Base de datos

```sql
ALTER TABLE TVOUCHERS ADD COLUMN EXPIRATION_DATE DATE DEFAULT DATE '2027-12-31';
```

#### 4. Repositorio

`PaymentMeanRepository.java`:

```java
List<Voucher> findExpiringVouchers(Nif nif, LocalDate from, LocalDate to);
```

`PaymentMeanJpaRepository.java`:

```java
@Override
public List<Voucher> findExpiringVouchers(Nif nif, LocalDate from, LocalDate to) {
    return Jpa.getManager()
            .createNamedQuery("Voucher.findExpiringByClientNif", Voucher.class)
            .setParameter(1, nif)
            .setParameter(2, from)
            .setParameter(3, to)
            .getResultList();
}
```

#### 5. Servicio: paquete `application/service/paymentmean/expiration`

`commands/FindExpiringVouchers.java`:

```java
public class FindExpiringVouchers implements Command<List<ExpiringVoucherDto>> {

    private String nif;
    private int days;
    private PaymentMeanRepository repo = Factories.repository.forPaymentMean();

    public FindExpiringVouchers(String nif, int days) {
        ArgumentChecks.isNotBlank(nif, "The nif cannot be null or blank");
        ArgumentChecks.isTrue(days >= 0, "Days cannot be negative");
        this.nif = nif;
        this.days = days;
    }

    @Override
    public List<ExpiringVoucherDto> execute() throws BusinessException {
        if (!Nif.isValid(nif)) {
            return List.of();
        }
        LocalDate today = LocalDate.now();
        return repo.findExpiringVouchers(new Nif(nif), today, today.plusDays(days)).stream()
                .map(FindExpiringVouchers::toDto)
                .toList();
    }

    private static ExpiringVoucherDto toDto(Voucher v) {
        ExpiringVoucherDto dto = new ExpiringVoucherDto();
        dto.code = v.getCode().code();
        dto.description = v.getDescription();
        dto.available = v.getAvailableAmount().amount();    // Money -> BigDecimal
        dto.expiration = v.getExpiration();
        return dto;
    }
}
```

Las fechas se calculan **en el comando** (`today`, `today.plusDays(days)`) y se pasan como parámetros. La
consulta JPQL no sabe qué es "dentro de N días".

`commands/ExtendVoucher.java`:

```java
public class ExtendVoucher implements Command<Void> {

    private String code;
    private int months;
    private PaymentMeanRepository repo = Factories.repository.forPaymentMean();

    public ExtendVoucher(String code, int months) {
        ArgumentChecks.isNotBlank(code, "The code cannot be null or blank");
        ArgumentChecks.isTrue(months > 0, "Months must be positive");
        this.code = code;
        this.months = months;
    }

    @Override
    public Void execute() throws BusinessException {
        BusinessChecks.isTrue(VoucherCode.isValid(code), "The voucher does not exist");
        Optional<Voucher> ov = repo.findVoucherByCode(VoucherCode.of(code));
        BusinessChecks.exists(ov, "The voucher does not exist");
        Voucher v = ov.get();
        BusinessChecks.isFalse(v.isExpired(), "The voucher is already expired");

        v.extend(months);
        v.updatedNow();
        return null;
    }
}
```

Fachada (`VoucherExpirationServiceImpl`) y registro (`forVoucherExpirationService()`) como siempre.

#### Lo que hay que comprobar al final (como en la TG)

Este cambio toca una regla que usan otros casos de uso (pagar facturas). Antes de entregar, ejecuta
**todos** los tests, no solo los nuevos. En la copia de prueba pasaron todos porque un bono nuevo caduca
dentro de un año y los tests usan bonos recién creados.

</details>

---

## Examen 6. Nómina de los mecánicos ⭐⭐⭐ (dinero + fechas + horas: el más parecido a lo anunciado)

### Enunciado

El taller paga a los mecánicos por las horas que trabajan en las intervenciones.

**Reglas**

* Se paga a **15 € la hora**, proporcional a los minutos (90 minutos = 22,50 €).
* Las horas **extra** se pagan con un **+50 %**. Es hora extra lo que se hace en **sábado o domingo**, o
  **antes de las 8:00**, o **a partir de las 20:00** (las 20:00 en punto ya es extra). Lo que cuenta es la
  fecha y hora de **inicio** de la intervención.
* La nómina de un mes suma las intervenciones del mecánico **cuya fecha cae en ese mes**. El total se
  redondea a céntimos **al final**.
* Este cambio **no** afecta a lo que se factura a los clientes.

**Base de datos.** Nada nuevo: se usan las intervenciones que ya existen.

**Servicio (dado)** en `uo.ri.cws.application.service.mechanic`:

```java
public interface MechanicPayrollService {

    /**
     * Computes what the mechanic earns in a month for his/her interventions:
     * 15 EUR/hour, +50% for extra time (Saturday, Sunday, before 8:00 or from 20:00 on).
     * The total is rounded to cents.
     * @throws IllegalArgumentException if nif is null or blank, or month is null
     * @throws BusinessException if the mechanic does not exist, or the month is
     *         after the current one
     */
    PayrollDto computePayroll(String mechanicNif, YearMonth month) throws BusinessException;

    public static class PayrollDto {
        public String mechanicNif;
        public YearMonth month;
        public int interventions;
        public int normalMinutes;
        public int extraMinutes;
        public BigDecimal total;
    }
}
```

**Ejemplo para comprobar tus cálculos.** Un mecánico con estas intervenciones:

| Fecha | Minutos | ¿Extra? | Paga |
|---|---|---|---|
| sáb 7/3/2026 10:00 | 60 | sí (sábado) | 15 × 1,5 = 22,50 |
| lun 9/3/2026 21:00 | 60 | sí (≥ 20:00) | 22,50 |
| mar 10/3/2026 10:00 | 90 | no | 15 × 1,5 h = 22,50 |
| mié 11/3/2026 07:59 | 1 | sí (< 8:00) | 0,25 × 1,5 = 0,375 |
| mié 1/4/2026 00:00 | 60 | — | no es de marzo |

Nómina de marzo de 2026: 4 intervenciones, 90 minutos normales, 121 extra, total **67,88 €**
(67,875 redondeado con `HALF_EVEN`).

<details>
<summary>▶ Pistas</summary>

* ¿Dónde pones "¿es hora extra?" y "¿cuánto se paga por esta intervención?"? Son reglas de una
  intervención y solo necesitan sus datos (fecha y minutos), así que van en `Intervention`.
* **No toques** `calculateLaborAmount()` ni `getTotalAmount()`: eso es lo que se factura. (Al final del
  examen está lo que pasa si lo tocas.)
* No hay repositorio de intervenciones: hay que crearlo y registrarlo.
* Un mes es el intervalo `[día 1 a las 00:00, día 1 del mes siguiente a las 00:00)`. Calcúlalo en Java y
  pásalo a la consulta como parámetros.
* Cuidado con el `<` en el `orm.xml` (mira la chuleta).
* ¿`toMoneyExact` o `toMoneyRoundedToCents`? Hay una intervención que vale 0,375 €.

</details>

<details>
<summary>▶ Solución</summary>

#### 1. Dominio: `Intervention.java`

Constantes al principio de la clase:

```java
public class Intervention extends BaseEntity {
    public static final Money HOURLY_WAGE = Money.of("15.00");
    private static final Percentage EXTRA_TIME_BONUS = Percentage.ofPercent(50);
    private static final int WORKDAY_START_HOUR = 8;
    private static final int WORKDAY_END_HOUR = 20;
```

Dos métodos nuevos:

```java
/**
 * Extra time: weekends, or before 8:00, or from 20:00 on
 */
public boolean isExtraTime() {
    DayOfWeek day = date.getDayOfWeek();
    int hour = date.getHour();
    return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY
            || hour < WORKDAY_START_HOUR || hour >= WORKDAY_END_HOUR;
}

/**
 * What the mechanic earns for this intervention (not rounded)
 */
public Money getMechanicPay() {
    Money.MoneyCalculator calculator = Money.calc(HOURLY_WAGE).times(minutes).div(60);
    if (isExtraTime()) {
        calculator.with(EXTRA_TIME_BONUS);
    }
    return calculator.toMoneyExact();
}
```

(con `import java.time.DayOfWeek;` y `import uo.ri.cws.domain.types.Percentage;`)

* **Los bordes**: `hour < 8` hace que las 7:59 sean extra y las 8:00 no. `hour >= 20` hace que las 20:00
  sean extra. Comprueba siempre los bordes con el enunciado: es donde fallan los tests.
* Primero `times(minutes)` y luego `div(60)`. Con `div(60)` primero también daría bien, porque `Money`
  divide con 34 cifras de precisión. Aun así, multiplicar antes de dividir es una buena costumbre.
* **`toMoneyExact()`**: este importe se va a sumar con otros. Si lo redondearas aquí, 0,375 pasaría a
  0,38, y en otros casos los céntimos acumulados no cuadrarían con el total esperado.
* Sobre `calculator.with(...)` sin guardar el resultado: la calculadora **sí** se modifica a sí misma
  (devuelve `this`), así que vale. Con las fechas no pasa lo mismo.

#### 2. Mapeo: solo la consulta

```xml
<named-query name="Intervention.findByMechanicBetween">
    <query>
        select i
        from Intervention i
        where i.mechanic.id = ?1
            and i.date >= ?2
            and i.date &lt; ?3
        order by i.date
    </query>
</named-query>
```

`&lt;` es `<`. Si escribes `<` tal cual, el `orm.xml` deja de ser un XML válido y **fallan todos los
tests** con `SAXParseException ... must consist of well-formed character data`. Me pasó al probar esta
solución, así que es un error muy fácil de cometer.

#### 3. Repositorio nuevo

`application/repository/InterventionRepository.java`:

```java
public interface InterventionRepository extends Repository<Intervention> {

    /**
     * @return the interventions of the mechanic with date in [from, to)
     */
    List<Intervention> findByMechanicBetween(String mechanicId, LocalDateTime from, LocalDateTime to);
}
```

`infrastructure/persistence/jpa/repository/InterventionJpaRepository.java`:

```java
public class InterventionJpaRepository
        extends BaseJpaRepository<Intervention>
        implements InterventionRepository {

    @Override
    public List<Intervention> findByMechanicBetween(String mechanicId, LocalDateTime from, LocalDateTime to) {
        return Jpa.getManager()
                .createNamedQuery("Intervention.findByMechanicBetween", Intervention.class)
                .setParameter(1, mechanicId)
                .setParameter(2, from)
                .setParameter(3, to)
                .getResultList();
    }
}
```

Regístralo: `InterventionRepository forIntervention();` en `PersistenceFactory`, y en `JpaRepositoryFactory`:

```java
@Override
public InterventionRepository forIntervention() {
    return new InterventionJpaRepository();
}
```

#### 4. Servicio: paquete `application/service/mechanic/payroll`

`commands/ComputePayroll.java`:

```java
public class ComputePayroll implements Command<PayrollDto> {

    private String nif;
    private YearMonth month;
    private MechanicRepository mechanicRepo = Factories.repository.forMechanic();
    private InterventionRepository interventionRepo = Factories.repository.forIntervention();

    public ComputePayroll(String nif, YearMonth month) {
        ArgumentChecks.isNotBlank(nif, "The nif cannot be null or blank");
        ArgumentChecks.isNotNull(month, "The month cannot be null");
        this.nif = nif;
        this.month = month;
    }

    @Override
    public PayrollDto execute() throws BusinessException {
        BusinessChecks.isFalse(month.isAfter(YearMonth.now()), "The month has not arrived yet");
        BusinessChecks.isTrue(Nif.isValid(nif), "The mechanic does not exist");
        Optional<Mechanic> om = mechanicRepo.findByNif(new Nif(nif));
        BusinessChecks.exists(om, "The mechanic does not exist");
        Mechanic m = om.get();

        // [first instant of the month, first instant of the next month)
        LocalDateTime from = month.atDay(1).atStartOfDay();
        LocalDateTime to = month.plusMonths(1).atDay(1).atStartOfDay();
        List<Intervention> interventions = interventionRepo.findByMechanicBetween(m.getId(), from, to);

        PayrollDto dto = new PayrollDto();
        dto.mechanicNif = nif;
        dto.month = month;
        dto.interventions = interventions.size();
        Money.MoneyCalculator total = Money.calc();
        for (Intervention i : interventions) {
            if (i.isExtraTime()) {
                dto.extraMinutes += i.getMinutes();
            } else {
                dto.normalMinutes += i.getMinutes();
            }
            total.add(i.getMechanicPay());
        }
        dto.total = total.toMoneyRoundedToCents().amount();
        return dto;
    }
}
```

* **`YearMonth.now()`**: compara mes con mes. Si compararas con `LocalDate.now()` tendrías que pensar en el
  día; así no hace falta.
* **Las fechas se calculan en Java** y se pasan como parámetros. JPQL no sabe qué es "el mes siguiente".
* **Redondeo una sola vez**, al final: `toMoneyRoundedToCents()` y luego `.amount()` para pasarlo a
  `BigDecimal` en el DTO.
* Un mecánico que no ha trabajado ese mes no es un error: la nómina sale con 0 intervenciones y total 0.

`MechanicPayrollServiceImpl.java`:

```java
public class MechanicPayrollServiceImpl implements MechanicPayrollService {

    private CommandExecutor executor = Factories.executor.forExecutor();

    @Override
    public PayrollDto computePayroll(String mechanicNif, YearMonth month) throws BusinessException {
        return executor.execute(new ComputePayroll(mechanicNif, month));
    }
}
```

Y regístralo con `forMechanicPayrollService()` en `JpaServicesFactoryImpl`.

**Resultado en la copia de prueba:** con los datos del ejemplo sale 67,88 € y todos los tests originales
pasan (404 de dominio/JPA y 234 de servicio).

#### 5. El experimento: "conseguir que pasen de nuevo todos los tests"

Probé a hacer lo que el enunciado dice que **no** hay que hacer: aplicar también el +50 % a lo que se
**factura**, cambiando `calculateLaborAmount()` en `Intervention`. Al ejecutar todos los tests, fallaron
**3 tests de dominio que antes pasaban**:

```
InterventionConstructorTests.testAmountOneHour
InterventionConstructorTests.testSpareAndTimeAmount
InvoiceCanGenerateVoucher30Tests.testPaidInvoiceOfExactly500CannotGenerateVoucher
```

¿Por qué? Esos tests crean intervenciones con `LocalDateTime.now()`, y yo los ejecuté a las **23:40**, que
es hora extra. Los importes subieron un 50 % y no coincidían con lo esperado. **A las 10:00 de un martes
habrían pasado**: el mismo código falla o no según la hora a la que ejecutes los tests.

Lo que tienes que aprender de esto:

1. **Si cambias una regla que ya existe, ejecuta todos los tests**, no solo los nuevos.
2. Si un test que antes pasaba falla, **lee qué espera** (el `assertEquals`) antes de tocar nada. Casi
   siempre te dice qué regla has roto.
3. **No modifiques los tests** para que pasen: lo que hay que arreglar es tu código.
4. Si un test falla "a veces", sospecha de `now()` y de las reglas que dependen de la hora o del día.

</details>

---

## Tabla resumen: qué practica cada examen

| | Atributo simple | Enum | Entidad nueva | Asociación | Cambia una regla existente | `<basic>`/`<column>` | Named query | `join` en JPQL | Repo nuevo |
|---|---|---|---|---|---|---|---|---|---|
| 1. Kilometraje | ✔ | | | | | ✔ | ✔ | | |
| 2. Urgentes | ✔ | (usa uno) | | | ✔ (`computeAmount`) | no hace falta | ✔ | | |
| 3. Categorías | | ✔ | | | | `<enumerated>` | ✔ | | (`countInterventions`) |
| 4. Reclamaciones | ✔ | ✔ | ✔ | ✔ 1-N | | `<entity>` | ✔ | ✔ | ✔ |
| 5. Caducidad | ✔ | | | | ✔ (`canBeCharged`) | ✔ | ✔ | | |
| 6. Nómina | | (`DayOfWeek`) | | | no (¡y por qué!) | no hace falta | ✔ (con `&lt;`) | | ✔ |

El 6 es el que más practica **dinero + fechas + horas**. Si solo te da tiempo a hacer uno, haz ese.

## Errores que más se repiten (repásalos antes de entregar)

1. **No registrar el servicio** en `JpaServicesFactoryImpl`, o el repositorio nuevo en
   `PersistenceFactory`/`JpaRepositoryFactory`. Te salta `NotYetImplementedException` o un error de compilación.
2. **Desordenar `<attributes>`** en el `orm.xml`: `basic` → `many-to-one` → `one-to-many` → `embedded`.
   Si no, el XML no es válido y fallan **todos** los tests.
3. **Dejar escapar una `IllegalStateException`** del dominio cuando el Javadoc pide una `BusinessException`.
   Comprueba con `BusinessChecks` antes de llamar al dominio.
4. **`new Nif(texto)` / `new LicensePlate(texto)` sin `isValid`**: con texto mal escrito, explota con una
   `IllegalArgumentException`.
5. **Olvidar `repo.add(...)`** para un objeto nuevo sin cascada.
6. **Columna nueva sin `DEFAULT`** cuando el atributo es primitivo (`int`, `boolean`): las filas antiguas
   tienen `NULL` y JPA falla al leerlas.
7. **`getSingleResult()` en una búsqueda que puede no encontrar nada**: usa
   `getResultStream().findFirst()`.
8. **Navegar por una colección con puntos en JPQL** (`i.workOrders.vehicle`): usa `join i.workOrders w`.
9. **Escribir `<` en una consulta del `orm.xml`**: usa `&lt;` o `<![CDATA[ ... ]]>`.
10. **Dinero**: `times(pct)` en lugar de `with(pct)`, redondear cada sumando en lugar de redondear al final,
    o usar `double`.
11. **Fechas**: escribir `d.plusDays(1);` sin guardar el resultado, o equivocarte en un borde (`<` frente a
    `<=`, o las 20:00 en punto).
