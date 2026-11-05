# Cómo hacer la ampliación a partir de los tests (lo que de verdad cae)

**Lo que se sabe del examen** (por el grupo de clase): te dan **tests nuevos** y tienes que hacer una ampliación (consultas, gateways, servicios...) para que **pasen**.
- Si pasan → te corrigen el proyecto y te ponen nota.
- Si no pasan → no lo corrigen y es un 0.

Así que en el examen **los tests son el enunciado**. Este documento explica cómo leerlos, cómo ejecutarlos, cómo interpretar los fallos y cómo trabajar paso a paso hasta que salgan en verde. Léelo **antes** que los modelos de examen.

---

## 0. Buenas noticias: tu proyecto está en verde ✅

He compilado y ejecutado tus tests (sin tocar tu proyecto, en una copia temporal):

| Batería | Resultado |
|---|---|
| `RunServiceTests_1` (la oficial de tu caso, UO % 3 == 1) | **128 de 128 OK** |
| `RunServiceTests_clients` | **119 de 119 OK** |

Es decir, **partes de una base que funciona**. En el examen, todo lo que hagas debe mantener esto en verde y además poner en verde los tests nuevos.

He comprobado también que las soluciones de la guía funcionan con tests reales que ya están en tu proyecto (son de los casos de otros UO):

| Solución de la guía | Tests | Resultado |
|---|---|---|
| 8.5 Bono de 30 € por factura > 500 € | `voucher/GenerateForInvoicesOver500.feature` | **6 de 6 OK** (sin cambios: 3 fallan) |
| 8.7 Resumen de bonos `getVoucherSummary()` | `voucher/Summary.feature` | **2 de 2 OK** (sin cambios: fallan) |
| Las dos a la vez + tu batería completa | `RunServiceTests_1` | **128 de 128 OK** (no se rompe nada) |

👉 **Eso te da un simulacro de examen real** (apartado 7): tienes en tu proyecto tests que hoy fallan y que puedes poner en verde tú misma.

---

## 1. Qué te van a dar, probablemente

Viendo cómo está organizado `cws_service_tests` (y los ficheros `RunServiceTests_0/_1/_2/_clients`, que son baterías por UO), lo más probable es que te den algunas de estas cosas:

| Fichero | Qué es | Dónde va |
|---|---|---|
| `Xxx.feature` | El escenario en lenguaje casi natural (Given/When/Then) | `cws_service_tests/test/uo/ri/cws/application/service/acceptance/<tema>/` |
| `ScenarioXxx1.java`, `ScenarioXxx2.java`... | El código Java de cada escenario: prepara datos, llama a tu servicio y comprueba | `.../acceptance/<tema>/<subcarpeta>/` |
| `XxxRobustnessTests.java` | Tests JUnit que comprueban que los argumentos inválidos lanzan `IllegalArgumentException` | `.../robustness/<tema>/` |
| Cambios en `DbFixtures`, builders o records | Utilidades para meter datos de prueba | `.../acceptance/util/...` |
| Un runner (`RunServiceTests_X.java`) | Lista de qué features y paquetes se ejecutan | `.../service/` |
| Quizá la **interfaz de servicio** nueva o ampliada | El contrato que los tests llaman | `cws_service_interfaces/...` |

**Regla de oro: los tests NO se tocan.** Si tocas un test para que pase, al corregir copiarán los originales y fallará.

---

## 2. Cómo se ejecutan los tests en Eclipse

### 2.1 La batería entera de tu caso
1. En el *Package Explorer*: `cws_service_tests` → `test` → `uo.ri.cws.application.service` → `RunServiceTests_1.java`.
2. Botón derecho → **Run As → JUnit Test**.
3. Se abre la pestaña **JUnit** (abajo o a la izquierda):
   - barra **verde** = todo bien;
   - barra **roja** = algo falla. Abajo verás `Runs: 128/128  Errors: 0  Failures: 0`.
4. En la pestaña **Console** sale el detalle de Cucumber: cada escenario con sus pasos y, en los que fallan, el error.

> Si te dan un runner nuevo (p. ej. `RunServiceTests_exam.java`), haz lo mismo con ese.

### 2.2 Ejecutar SOLO los tests nuevos (para ir más rápido)
Copia `RunServiceTests_1.java` como `RunServiceTests_mio.java` (en la misma carpeta), cambia el nombre de la clase y deja solo lo nuevo:

```java
@Suite
@IncludeEngines({ "cucumber", "junit-jupiter" })
@SelectClasspathResource("uo/ri/cws/application/service/acceptance/client/FindBySurname.feature")
@SelectPackages({ "uo.ri.cws.application.service.robustness.client" })
@ConfigurationParameter(key = Constants.PLUGIN_PROPERTY_NAME, value = "pretty")
@ConfigurationParameter(key = Constants.GLUE_PROPERTY_NAME, value = "uo.ri.cws.application.service.acceptance")
public class RunServiceTests_mio {}
```
(Ruta del `.feature` con **barras `/`**; paquete de robustness con **puntos `.`**.) Al final, **bórralo** (o no lo entregues) y ejecuta la batería completa.

### 2.3 También funciona
- Botón derecho sobre un `.feature` → *Run As → Cucumber Feature* (si tienes el plugin de Cucumber en Eclipse).
- Botón derecho sobre un `XxxRobustnessTests.java` → *Run As → JUnit Test* (solo esos).
- Botón derecho sobre el `pom.xml` de `cws_service_tests` → *Run As → Maven test* (ejecuta `RunServiceTests`, que es la batería con TODO; ahí fallarán cosas de otros UO, es normal).

### 2.4 Si Eclipse no ve tus cambios
Los tests dependen de `cws_services_jdbc`. Si cambias algo y el test sigue igual: *Project → Clean...* y vuelve a ejecutar. Si hay errores rojos en `cws_service_tests`, botón derecho en el proyecto → *Maven → Update Project*.

### 2.5 Los tests vacían la BD antes de cada escenario
`CommonSteps` hace `Db.clearTables()` antes de cada escenario. Por tanto:
- **No dependas de los datos que hay en `test.script`.** Cada test mete solo sus datos (con `DbFixtures`).
- Si tu código funciona en el menú pero no en el test (o al revés), suele ser porque estás contando con datos que el test no crea.

---

## 3. Cómo leer un test (anatomía)

### 3.1 El `.feature`: el "qué"
Ejemplo real de tu proyecto (`voucher/GenerateForInvoicesOver500.feature`):
```gherkin
Feature: [V.GI] Generate vouchers for invoices over 500 euro

  Scenario: [V.GI.1] Generate one voucher for an invoice over 500 euro
    Given [V.GI.1] a client with a paid invoice over 500 euro       ← datos de partida
    When [V.GI.1] I generate vouchers                                ← la operación que se prueba
    Then [V.GI.1] one voucher of 30 euro is generated for the client ← lo que se comprueba
    And [V.GI.1] the voucher description is "Por factura de más de 500"
    And [V.GI.1] the invoice is marked as used for the voucher

  Scenario: [V.GI.2] Do not generate an invoice voucher for an invoice of 500 euro
  ...
```
- **Lee primero todos los `Scenario:`** (solo los títulos). Juntos son la lista de reglas que tienes que implementar. En este ejemplo:
  1. factura pagada > 500 → 1 bono de 30 €;
  2. factura de **500 justos** → nada (o sea, `>` estricto);
  3. factura no pagada → nada;
  4. factura ya usada → nada (hay que **marcarla**);
  5. dos facturas → dos bonos;
  6. mezcla de usadas, nuevas y no pagadas.
- El texto entre comillas (`"Por factura de más de 500"`) es **literal**: tu código debe producir exactamente esa cadena, con tilde incluida.

### 3.2 La clase `ScenarioXxx.java`: el "cómo exacto"
El `.feature` resume; **la verdad está en el Java**. Ejemplo real (`ScenarioVGI1.java`), comentado:

```java
public class ScenarioVGI1 extends AbstractVoucherScenario {

    @Given("[V.GI.1] a client with a paid invoice over 500 euro")
    public void given...() {
        client = DbFixtures.aClient();                       // 1) crea un cliente
        invoice = DbFixtures.anInvoiceForClientOf(           // 2) crea una factura...
            client.id, new BigDecimal("500.01"), PAID, NOT_USED_FOR_VOUCHER);  // de 500,01 €, pagada, sin usar
    }

    @When("[V.GI.1] I generate vouchers")
    public void when...() throws BusinessException {
        generateVouchers();               // → service.generateVouchers()  ← EL MÉTODO QUE TÚ IMPLEMENTAS
    }

    @Then("[V.GI.1] one voucher of 30 euro is generated for the client")
    public void then...() {
        assertGenerated(0, 0, 1);              // el array devuelto debe ser EXACTAMENTE {0, 0, 1}
        assertVouchers(client.id, 1, "30.00"); // el cliente tiene 1 bono, available = 30, accumulated = 0
    }

    @And("[V.GI.1] the invoice is marked as used for the voucher")
    public void and...() {
        assertInvoiceUsed(invoice);            // en BD: TInvoices.usedForVoucher = TRUE
    }
}
```

Qué sacar de cada parte:

| Parte | Qué te dice | Dónde mirar |
|---|---|---|
| **`@Given`** | Qué datos existen (qué tablas y columnas intervienen) | Abre el método de `DbFixtures` (`Ctrl+Clic`) y mira qué `Db.insert(...)` hace |
| **`@When`** | **Qué método del servicio** se llama, con qué parámetros y qué devuelve | La línea `service.xxx(...)` → esa es la firma que tienes que implementar |
| **`@Then`** | El resultado exacto esperado | Los `assertXxx(...)` |

### 3.3 Mirar `DbFixtures` te da el SQL
Ejemplo: `anInvoiceForClientOf(...)` (en `DbFixtures.java`) crea, por dentro:
- una factura en `TInvoices` (con `total`, `state`, `usedForVoucher`);
- si está pagada: un medio de pago `Cash` del cliente en `TPaymentMeans` **y un cargo** en `TCharges` que une factura ↔ medio de pago;
- un vehículo del cliente y una orden de trabajo de ese vehículo con `invoice_id` = la factura.

Con eso ya sabes que puedes llegar del cliente a la factura por dos caminos: **cargos** (`TCharges → TPaymentMeans.client_id`) o **OT → vehículo** (`TWorkOrders → TVehicles.client_id`). Y sabes qué columnas existen.

### 3.4 Los `assert` más comunes y qué exigen

| Assert | Qué exige | Consecuencia para tu código |
|---|---|---|
| `assertEquals(esperado, real)` | Igualdad exacta (`equals`) | Strings idénticos (tildes, mayúsculas, espacios). Números exactos |
| `assertEquals(0, new BigDecimal("30.00").compareTo(x))` | Mismo valor numérico (30 = 30.00) | Con `BigDecimal` no importan los decimales, sí el valor |
| `assertArrayEquals(new int[]{0,0,1}, generated)` | Mismo array posición a posición | El orden de los contadores importa (20 €, 25 €, 30 €) |
| `assertEquals(3, result.size())` | Número exacto de elementos | Sin duplicados (`DISTINCT`), sin filas de más |
| `result.get(0)`, `result.get(1)`... | **Orden** exacto | Tu `ORDER BY` (o `sort`) tiene que coincidir |
| `assertTrue(result.isEmpty())` | Lista vacía, **no** excepción | Nada de `BusinessException` |
| `assertTrue(opt.isEmpty())` | `Optional.empty()` | Devuelve `Optional`, no `null` |
| `ctx.assertBusinessExceptionWithMessage()` | Que se lance `BusinessException` **con mensaje no vacío** | `BusinessChecks.xxx(..., "mensaje")` o `throw new BusinessException("mensaje")` |
| `assertThrows(IllegalArgumentException.class, ...)` + mensaje no vacío (robustness) | Argumento inválido → `IllegalArgumentException` con mensaje | `ArgumentChecks.isNotBlank(x, "mensaje")` **con mensaje** |
| `Db.findById(TXxxRecord.class, id)` + assert | Comprueba el **estado de la BD** después | Tienes que haber hecho el `update` / `insert` / `delete` de verdad |
| `assertNull(Db.findById(...))` | La fila ya no existe | Has borrado (y en el orden correcto por las FK) |
| `Asserts.dtoMatchesRecord(record, dto)` | Todos los campos del DTO = columnas de la fila | El assembler copia **todos** los campos (también `id` y `version`) |

### 3.5 Los tests de robustez (`robustness/...`)
Son JUnit normales. Cada uno llama al servicio con un argumento inválido (`null`, `""`, `"   "`, número negativo...) y exige `IllegalArgumentException` con mensaje. Ejemplo real:
```java
@ParameterizedTest
@NullSource
@ValueSource(strings = { "", "   " })    // se ejecuta 3 veces: null, vacío y espacios
void addRejectsNullOrBlankNif(String nif) { ... assertRejected(() -> service.addClient(dto, null)); }
```
→ en el constructor del comando: `ArgumentChecks.isNotBlank(nif, "El NIF no puede estar vacío");` (`isNotBlank` rechaza los tres casos).

> Ojo: los tests de robustez usan a veces datos que **no existen en la BD** (p. ej. `dto.id = "client-id"`). Si la validación está en `execute()` después de consultar la BD, puede que antes salte otra excepción. **Valida los argumentos en el constructor.**

---

## 4. El método de trabajo en el examen, paso a paso

### Paso 1. Copiar los ficheros que te den, en su sitio (5 min)
Respeta la carpeta que indica la línea `package ...;` de cada `.java`. Los `.feature` van en la carpeta equivalente (`acceptance/<tema>/`).

### Paso 2. Leer los tests sin escribir código (10 min)
1. Títulos de todos los `Scenario:` → apunta las reglas en un papel.
2. En cada `ScenarioXxx.java`:
   - `@When`: **firma exacta** del método (nombre, parámetros, tipo devuelto).
   - `@Given`: qué `DbFixtures` usa → qué tablas y datos.
   - `@Then`: qué valores, orden, mensajes y excepciones exige.
3. Rellena esta tabla (de verdad, en papel):

| Escenario | Datos de partida | Llamada | Resultado esperado | ¿Excepción? |
|---|---|---|---|---|
| 1 | cliente con factura pagada 500,01 | `generateVouchers()` | `{0,0,1}`, bono 30 € "Por factura de más de 500", factura marcada | no |
| 2 | factura pagada de 500 | ídem | `{0,0,0}` | no |
| ... | | | | |

### Paso 3. Hacer que compile (10 min)
Los errores rojos de Eclipse en los tests te dicen qué falta:

| Error de compilación en el test | Qué significa | Qué haces |
|---|---|---|
| `The method findXxx(String) is undefined for the type ClientCrudService` | La interfaz no tiene ese método | Añádelo a la interfaz con **exactamente** esa firma (si no te la dan ya) |
| `XxxDto cannot be resolved to a type` | Falta un DTO | Añádelo dentro de la interfaz (si no te lo dan) |
| `The method forXxxService() is undefined for the type ServiceFactory` | Falta un servicio completo | Método en `ServiceFactory` + `ServiceFactoryImpl` |
| `The type XxxServiceImpl must implement the inherited abstract method...` | Añadiste el método a la interfaz pero no a la fachada | Impleméntalo en `XxxServiceImpl` |

Truco: para que compile **ya**, pon en la fachada un esqueleto:
```java
@Override
public List<ClientDto> findClientsBySurname(String surnamePart) throws BusinessException {
    throw new NotYetImplementedException();
}
```
Ejecuta los tests: deben salir en **rojo pero ejecutándose** (si no se ejecutan, aún hay algún problema de compilación o de colocación de ficheros).

### Paso 4. Implementar con la receta (lo gordo)
Sigue la **receta de la sección 7 de la guía** (queries → Gateway → GatewayImpl → Comando → fachada). Empieza por el **escenario más simple** (el "camino feliz"), luego los de lista vacía y luego los de error.

### Paso 5. Ejecutar solo los tests nuevos, leer el fallo, corregir (bucle)
Usa tu runner reducido (2.2). Cada vez que falle, mira **el primer error** (sección 5) y corrige solo eso. No cambies diez cosas a la vez.

### Paso 6. Ejecutar la batería COMPLETA de tu caso
`RunServiceTests_1` (o el runner que te den). **Todo en verde.** Si un test antiguo se ha puesto rojo, has roto algo que funcionaba. Lo típico es haber cambiado una query o un assembler compartido (por ejemplo, añadir una columna al assembler y no a todas las `SELECT` de esa tabla).

### Paso 7. Limpieza (5 min, que también puntúa)
- Vista *Problems*: **0 warnings** (imports sin usar → `Ctrl+Shift+O` en cada fichero tocado).
- Nada de `System.out.println`, ni código comentado, ni métodos sin usar.
- Borra o no entregues tu runner de pruebas.
- `queries.properties`: claves bien nombradas y en su sitio alfabético.
- Ejecuta una última vez la batería completa.

### Reparto de tiempo orientativo (examen de ~1h30)
Leer y copiar: 15 min → compilar: 10 min → implementar y depurar: 50 min → batería completa y limpieza: 15 min.

---

## 5. Diccionario de fallos: "me sale esto → significa esto"

Mira siempre **la primera línea del error** y **la primera línea que mencione tu código** (`uo.ri.cws.application.persistence...` o `...service...`) en la traza.

| Lo que ves | Qué pasa | Cómo se arregla |
|---|---|---|
| `NotYetImplementedException` | Se llama a un método que aún tiene el esqueleto | Implementarlo (puede ser un gateway que ya existía, como `InvoiceGateway.update`) |
| `IllegalArgumentException: Missing property key 'TXXX_YYY' in file: queries.properties` | La clave que usas en Java no está en `queries.properties` (o está mal escrita) | Copia y pega la clave del Java al fichero, o al revés |
| `PersistenceException: java.sql.SQLSyntaxErrorException: user lacks privilege or object not found: XXXX` | En el SQL hay una **tabla o columna que no existe** (`XXXX` es la que falla) | Revisa el nombre en `test.script` (sección 6.1 de la guía). Típico: `amount` en vez de `total_amount`, `client` en vez de `client_id` |
| `...SQLSyntaxErrorException: unexpected token: ...` | Error de sintaxis SQL (coma de más/menos, falta `\` al partir líneas en el properties, `WHERE` antes que `JOIN`...) | Revisa la query; pruébala en el gestor de HSQLDB |
| `PersistenceException: ... Column not found: xxx` | El assembler lee con `rs.getXxx("xxx")` una columna que tu `SELECT` no trae | Añade la columna al `SELECT` |
| `PersistenceException: ...parameter index out of range` o `...parameter not set` | Tienes más o menos `?` que `setXxx(...)`, o los índices están mal | Cuenta los `?` y numera desde 1 |
| `...integrity constraint violation: foreign key no action` | Borras un padre antes que sus hijos, o insertas un hijo apuntando a algo que no existe | Ordena: borrar hijos → padre; insertar padre → hijos |
| `...integrity constraint violation: unique constraint or index violation` | Insertas un valor repetido en una columna `UNIQUE` (nif, code, number...) o el mismo `id` dos veces | Revisa que generas ids/códigos nuevos y que compruebas antes con un find |
| `AssertionFailedError: expected: <3> but was: <2>` | Devuelves un número de elementos/valor distinto | Revisa filtros del `WHERE`, `>` vs `>=`, `DISTINCT`, estados |
| `array contents differ at index [2], expected: <1> but was: <0>` | **(ejemplo real tuyo)** El array devuelto no coincide en esa posición | En `generateVouchers` la posición 2 es el nº de bonos de 30 € y estás devolviendo 0 |
| `expected: <Por factura de más de 500> but was: <Por tres órdenes de trabajo>` | Cadena distinta | Copia la cadena **del test** (con tildes) |
| `expected: <X> but was: <null>` | No has rellenado un campo del DTO o no se guardó en BD | Revisa el assembler (todos los campos) y que haces el `update`/`insert` |
| `AssertionFailedError: An exception was expected` | El test esperaba una excepción y no lanzaste nada | Añade la comprobación con `BusinessChecks` |
| `expected: <true> but was: <false>` en `assertBusinessExceptionWithMessage` | Se lanzó una excepción, pero **no** `BusinessException` (p. ej. `NoSuchElementException` por un `.get()` sobre Optional vacío, o `NullPointerException`) | Comprueba con `BusinessChecks.exists(opt, "...")` **antes** de hacer `.get()` |
| `Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown` (robustness) | No validas ese argumento | `ArgumentChecks...` en el constructor del comando |
| `Expected IllegalArgumentException ... but BusinessException/NullPointerException was thrown` | Validas tarde (en `execute`) o el null revienta antes | Valida en el **constructor** y en orden (`isNotNull` antes de usar el objeto) |
| `NullPointerException` en un assembler o en `.equals` | Un valor viene `null` de la BD | Usa `"CONST".equals(variable)` (no al revés); comprueba nulls |
| `Undefined step` / `You can implement missing steps with the snippets below` | Cucumber no encuentra la clase `ScenarioXxx` del paso | El `.java` del escenario no está en el paquete correcto (bajo `uo.ri.cws.application.service.acceptance`) o no compila |
| `No tests found` / el runner no ejecuta nada | Ruta del `.feature` mal escrita en `@SelectClasspathResource` | Ruta con `/`, sin `test/` delante, igual que en `RunServiceTests_1` |
| `PersistenceException: ... version has changed` | El `update` no encontró la fila con ese `id` y `version` | Usa el record recién leído de BD (con su `version`), no uno construido a mano |
| `IllegalStateException: No JDBC connection is associated with this thread` | Se llamó a un gateway fuera de un comando ejecutado por `CommandExecutor` | Todas las llamadas a gateways deben estar dentro de un `execute()` lanzado con `executor.execute(...)` |

---

## 6. Ejemplo completo de "leer el test → saber qué programar"

Supón que en el examen te dan este `.feature` (inventado, pero con la forma exacta de los reales):

```gherkin
Feature: [C.FbS] Find clients by surname
  As a Foreman
  I want to find clients by part of their surname

  Scenario: [C.FbS.1] Find clients whose surname contains a text
    Given [C.FbS.1] three clients with surnames "Garcia Lopez", "Lopez Ruiz" and "Perez Diaz"
    When [C.FbS.1] I search clients by surname "lopez"
    Then [C.FbS.1] I get 2 clients
    And [C.FbS.1] they are ordered by surname

  Scenario: [C.FbS.2] No client matches
    Given [C.FbS.2] a registered client with surname "Perez Diaz"
    When [C.FbS.2] I search clients by surname "zzz"
    Then [C.FbS.2] I get an empty list
```
y en `ScenarioCFbS1.java` el `@When` hace:
```java
result = service.findClientsBySurname("lopez");
```
y el `@Then`:
```java
assertEquals(2, result.size());
assertEquals("Garcia Lopez", result.get(0).surname);
assertEquals("Lopez Ruiz", result.get(1).surname);
```

**Lo que deduces, frase a frase:**
- `service.findClientsBySurname("lopez")` sobre `ClientCrudService` → método nuevo en esa interfaz que recibe `String` y devuelve `List<ClientDto>`.
- `"lopez"` encuentra `"Garcia Lopez"` **y** `"Lopez Ruiz"` → contiene en cualquier posición (`LIKE '%..%'`) **y** sin distinguir mayúsculas (`UPPER`).
- `get(0)` = "Garcia Lopez", `get(1)` = "Lopez Ruiz" → `ORDER BY surname`.
- Escenario 2: lista vacía, sin excepción.
- Si además hay un `robustness` con `@NullSource @ValueSource(strings = {"", "  "})` → `ArgumentChecks.isNotBlank` en el constructor.

Eso es **exactamente el Modelo 1** de `MODELOS_EXAMEN_AUTORIA.md`, con la solución completa. Todos los modelos de ese documento se pueden leer así: el enunciado en prosa equivale a lo que dirían los tests.

---

## 7. Simulacros con tests REALES que ya tienes en tu proyecto 🎯

En `cws_service_tests` hay tests de los casos de los otros UO. Hoy **fallan en tu proyecto** porque no te tocaba implementarlos. Son el mejor entrenamiento posible: es justo el formato del examen.

**Antes de empezar:** haz una copia de seguridad de tu proyecto (copia la carpeta `cws2026` entera a otro sitio, p. ej. `cws2026_ENTREGADO`). Así puedes practicar sin miedo y volver atrás.

| Simulacro | Tests (`acceptance/...`) | Nº escenarios | Dificultad | Solución en la guía |
|---|---|---|---|---|
| **A** | `voucher/Summary.feature` | 2 | ★☆☆ | 8.7 (verificada ✅) |
| **B** | `voucher/GenerateForInvoicesOver500.feature` | 6 | ★★☆ | 8.5 (verificada ✅) |
| **C** | `paymentmean/FindVoucherByCode.feature` + `FindCreditCardByNumber.feature` | 2 + 2 | ★★☆ | — (servicio nuevo `PaymentMeanCrudService`, como el Modelo 6) |
| **D** | `paymentmean/FindByClientNif.feature` + `FindById.feature` | 3 + 4 | ★★☆ | — (herencia: Cash/CreditCard/Voucher, dtype) |
| **E** | `paymentmean/AddVoucher.feature`, `AddCard.feature`, `Delete.feature` | 3 + 4 + 5 | ★★★ | — (inserts en dos tablas, borrado con reglas) |
| **F** | `voucher/GenerateForRecommendations.feature` | 8 | ★★★ | 8.6 (solo esquema) |
| **G** | `invoice/FindPayMeansByClientNif.feature`, `FindInvoiceByNumber.feature`, `SettleInvoice.feature` | 5 + 2 + 12 | ★★★★ | — (liquidar factura: el más largo) |

**Cómo hacer cada simulacro:**
1. Crea `RunServiceTests_mio.java` (sección 2.2) apuntando al `.feature` del simulacro (y al paquete `robustness` correspondiente: `...robustness.voucher` o `...robustness.paymentmean`).
2. Ejecútalo → rojo. Lee los escenarios y rellena la tabla del paso 2.
3. Implementa siguiendo la receta, sin mirar la solución.
4. Verde en tu runner → ejecuta `RunServiceTests_1` → debe seguir **128 de 128**.
5. Compara con la solución de la guía (A y B).

Para los simulacros C, D y E necesitas activar el servicio en `ServiceFactoryImpl.forPaymentMeanCrudService()` (ahora lanza `NotYetImplementedException`) y crear `service.paymentmean.crud` con su `PaymentMeanCrudServiceImpl`. Es exactamente el tipo de ampliación "añadir gateways, servicios..." de la que habla tu compañero.

Mi recomendación de orden: **A → B → C → D**. Si haces A y B tú sola y te salen en verde, vas bien preparada. Si además sacas C, vas muy bien.

---

## 8. Chuleta para llevar en la cabeza

1. **Los tests mandan.** El `.feature` resume; el `ScenarioXxx.java` dice la verdad exacta (`@When` = firma, `@Then` = resultado).
2. **No toques los tests.** Ni la interfaz, salvo para añadir lo que los tests llaman.
3. **Primero que compile** (esqueletos con `NotYetImplementedException`), luego que pase.
4. **Escenario a escenario**, empezando por el fácil. Lee **el primer error**.
5. **Strings y números exactos**: copia las cadenas del test; `>` vs `>=`; orden de las listas.
6. **Lista vacía ≠ excepción.** `BusinessException` solo si el test usa `assertBusinessException...`.
7. **Validación de argumentos en el constructor**, siempre con mensaje.
8. **La BD empieza vacía en cada escenario**: no cuentes con datos de `test.script`.
9. **Al final**: batería completa en verde + 0 warnings + sin println + queries ordenadas.
