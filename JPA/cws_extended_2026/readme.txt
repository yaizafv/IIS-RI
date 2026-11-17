CarWorkshop JPA - Entrega de la práctica (versión extendida)
=============================================================

Alumna: Yaiza Fernández Vega
UO:       UO301762
Modelo asignado: 1

Ampliaciones implementadas:
  - Pasar todas las anotaciones Jpa al orm.xml
  - Implementar todos los casos de uso

Horas dedicadas al proyecto: 15


1. Casos de uso asignados (UO % 3 == 1)
-------------------------------------------------------------------
- Generación de bonos de 20 € por cada tres órdenes de trabajo pagadas
  (VoucherGenerationService.generateVouchers, feature GenerateForWorkOrders).
- Búsqueda de los bonos de un cliente por NIF
  (VoucherGenerationService.findVouchersByClientNif).
- CRUD completo de clientes (ClientCrudService): alta (con medio de pago Cash
  y recomendación opcional), baja (borra medios de pago y recomendaciones),
  modificación (incluido el NIF, con control de versión), listado, búsqueda
  por NIF y búsqueda de clientes recomendados por otro.
- CRUD de mecánicos, creación de facturas y búsqueda de órdenes de trabajo no facturadas de un cliente.


2. Ampliaciones realizadas
-------------------------------------------------------------------
a) Todas las anotaciones JPA pasadas al orm.xml.

b) Implementados todos los casos de uso de la versión extendida, no solo los
   del UO:
   - VoucherGenerationService completo: bonos de 20 € (órdenes de trabajo),
     25 € (recomendaciones) y 30 € (facturas de más de 500 €), y el resumen
     de bonos por cliente (getVoucherSummary).
   - PaymentMeanCrudService completo (alta de tarjetas y bonos, borrado,
     búsquedas por id, NIF, código de bono y número de tarjeta).
   - InvoicingService completo (liquidación de facturas, búsqueda de factura
     por número, medios de pago por NIF, órdenes de trabajo por NIF y por
     matrícula).
