# TDG Puro vs TDG Pragmático

## Rama: `tdg-pure`

Esta rama implementa **TDG (Table Data Gateway) puro**, que es la interpretación más estricta del patrón según la teoría de arquitectura.

### ¿Qué es TDG Puro?

TDG Puro significa: **Un Gateway por tabla, un Record por tabla**

- Cada Gateway corresponde a una y SOLO UNA tabla en la base de datos
- Cada Gateway retorna el Record que representa exactamente los campos de esa tabla
- No se usan Records de clases padres o hijas en un Gateway que no le corresponde

### Cambios en esta rama

#### 1. PaymentMeanGateway.java (antes)
```java
public interface PaymentMeanGateway extends Gateway<PaymentMeanGateway.PaymentMeanRecord> {
    // ... métodos ...
    
    class PaymentMeanRecord extends BaseRecord {
        public String clientId;
        public BigDecimal accumulated;
        public String dtype;
    }
    
    class CashRecord extends PaymentMeanRecord {
    }
    
    class CardRecord extends PaymentMeanRecord {
        public String number;
        public YearMonth validThru;
        public String type;
    }
    
    class VoucherRecord extends PaymentMeanRecord {
        public String code;
        public String description;
        public BigDecimal available;
    }
}
```

#### PaymentMeanGateway.java (después - TDG Puro)
```java
public interface PaymentMeanGateway extends Gateway<PaymentMeanGateway.PaymentMeanRecord> {
    // ... métodos ...
    
    class PaymentMeanRecord extends BaseRecord {
        public String clientId;
        public BigDecimal accumulated;
        public String dtype;
    }
    // ✓ SIN CashRecord, CardRecord, VoucherRecord
}
```

#### 2. Los Records de subclases viven en sus propios Gateways

**VoucherGateway.java**
```java
public interface VoucherGateway extends Gateway<VoucherGateway.VoucherRecord> {
    // ... métodos específicos de Voucher ...
    
    class VoucherRecord extends BaseRecord {
        public String clientId;
        public BigDecimal accumulated;
        public String code;
        public String description;
        public BigDecimal available;
    }
}
```

**CardGateway.java**
```java
public interface CardGateway extends Gateway<CardGateway.CardRecord> {
    // ... métodos específicos de Card ...
    
    class CardRecord extends BaseRecord {
        public String number;
        public String type;
        public YearMonth validThru;
    }
}
```

**CashGateway.java**
```java
public interface CashGateway extends Gateway<CashGateway.CashRecord> {
    // ... métodos específicos de Cash ...
    
    class CashRecord extends BaseRecord {
    }
}
```

#### 3. Cambios en PaymentMeanRecordAssembler.java

**Antes:**
```java
import uo.ri.cws.application.persistence.paymentmean.PaymentMeanGateway.CardRecord;
import uo.ri.cws.application.persistence.paymentmean.PaymentMeanGateway.CashRecord;
import uo.ri.cws.application.persistence.paymentmean.PaymentMeanGateway.VoucherRecord;
```

**Después:**
```java
import uo.ri.cws.application.persistence.card.CardGateway.CardRecord;
import uo.ri.cws.application.persistence.cash.CashGateway.CashRecord;
import uo.ri.cws.application.persistence.voucher.VoucherGateway.VoucherRecord;
```

El Assembler sigue creando los Records correctos, pero ahora los importa desde sus Gateways respectivos.

## Arquitectura TDG Puro

```
┌─────────────────────────────────────────────────────────────┐
│                      Database                               │
│  ┌──────────────┬──────────────┬──────────────┐             │
│  │  TPAYMENTMEANS (dtype)                    │             │
│  │  - CASH       │              │             │             │
│  │  - CreditCard │  TCREDITCARDS│  TVOUCHERS  │             │
│  └──────────────┴──────────────┴──────────────┘             │
└─────────────────────────────────────────────────────────────┘
         ↓              ↓              ↓
┌──────────────────────────────────────────────────────────────┐
│                     Gateways                                  │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐         │
│  │PaymentMean   │ │CardGateway   │ │VoucherGateway│         │
│  │Gateway       │ │              │ │              │         │
│  │              │ │ CardRecord   │ │ VoucherRecord│         │
│  │Payment-      │ │ (completo)   │ │ (completo)   │         │
│  │MeanRecord    │ │              │ │              │         │
│  │(base)        │ └──────────────┘ └──────────────┘         │
│  └──────────────┘                                            │
└──────────────────────────────────────────────────────────────┘
         ↓              ↓              ↓
┌──────────────────────────────────────────────────────────────┐
│                   Service Layer                               │
│  ├─ ClientCrudService (usa PaymentMeanGateway para CASH)     │
│  ├─ VoucherService (puede usar VoucherGateway)              │
│  └─ CardService (puede usar CardGateway)                    │
└──────────────────────────────────────────────────────────────┘
```

## Ventajas de TDG Puro

1. **Claridad**: Un Gateway = Una tabla, sin excepciones
2. **Separación de responsabilidades**: Cada Gateway es responsable SOLO de su tabla
3. **Mantenibilidad**: Fácil de entender qué Gateway accede a qué tabla
4. **Escalabilidad**: Si necesitas agregar más campos específicos de tarjeta, solo modificas CardGateway
5. **Teoría**: Cumple 100% con la definición de TDG

## Desventajas de TDG Puro

1. **Polimorfismo más complejo**: PaymentMeanGateway retorna solo PaymentMeanRecord (base), no puede polimorfarse directamente
2. **Conversión necesaria**: Si quieres los datos completos de una tarjeta desde PaymentMeanGateway, necesitas:
   - Obtener PaymentMeanRecord de PaymentMeanGateway
   - Luego buscar CardRecord desde CardGateway usando el ID

## El Enfoque Pragmático (rama main)

La rama main usa **TDG pragmático**, que es más flexible:
- Tienes Records de subclases en PaymentMeanGateway
- Permites polimorfismo natural
- Sacrificas pureza teórica por practicidad

## Comparación en Código

### Buscar una tarjeta de crédito

**TDG Pragmático (main):**
```java
Optional<PaymentMeanRecord> pm = paymentMeanGateway.findByCardNumber("1234-5678-9012-3456");
if (pm.isPresent() && pm.get() instanceof PaymentMeanGateway.CardRecord card) {
    System.out.println("Card type: " + card.type);
}
```

**TDG Puro (tdg-pure):**
```java
Optional<PaymentMeanRecord> pm = paymentMeanGateway.findByCardNumber("1234-5678-9012-3456");
if (pm.isPresent()) {
    // Necesitas ir a CardGateway para datos completos
    Optional<CardRecord> card = cardGateway.findById(pm.get().id);
    if (card.isPresent()) {
        System.out.println("Card type: " + card.get().type);
    }
}
```

## ¿Cuál usar en tu proyecto?

- **Para la entrega (main)**: TDG pragmático funciona perfectamente y pasa todos los tests
- **Para aprender (tdg-pure)**: Esta rama te muestra la implementación teórica pura
- **Para la prueba de autoría**: Probablemente acepten ambas, pero ten las dos opciones claras

## Conclusión

No hay una respuesta "correcta" absoluta. El profesor dio la definición teórica, pero la decisión pragmática de mantener Records en PaymentMeanGateway es una extensión razonable para manejar Single Table Inheritance.

Esta rama (`tdg-pure`) demuestra que **sabes ambas opciones** y puedes elegir según el contexto.
