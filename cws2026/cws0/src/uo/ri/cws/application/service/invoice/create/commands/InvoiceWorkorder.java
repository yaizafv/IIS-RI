package uo.ri.cws.application.service.invoice.create.commands;

import java.util.List;

import uo.ri.cws.application.service.invoice.InvoicingService.InvoiceDto;
import uo.ri.util.assertion.ArgumentChecks;
import uo.ri.util.exception.BusinessException;

public class InvoiceWorkorder {

    private List<String> workOrderIds;

    public InvoiceWorkorder(List<String> workOrderIds) {
        ArgumentChecks.isNotNull(workOrderIds, "La lista de órdenes de trabajo no puede ser nula");
        ArgumentChecks.isTrue(!workOrderIds.isEmpty(), "La lista de órdenes de trabajo no puede estar vacía");
        this.workOrderIds = workOrderIds;
    }

    public InvoiceDto execute() throws BusinessException {
        // 1. Validar que las órdenes de trabajo existan y estén en un estado válido (ej. APPROVED)
        // 2. Calcular el importe total y el IVA
        // 3. Generar el número/código de factura y persistirla en base de datos (tabla TInvoices)
        // 4. Actualizar las órdenes de trabajo asociadas a estado INVOICED y vincularlas a la nueva factura
        
        InvoiceDto dto = new InvoiceDto();
        // Asignar los datos correspondientes al DTO resultante (id, número, fecha, importe, iva, estado)
        
        return dto;
    }
}