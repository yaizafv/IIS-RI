package uo.ri.cws.application.service.invoice.create.commands;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import uo.ri.conf.Factories;
import uo.ri.cws.application.persistence.invoice.InvoiceGateway;
import uo.ri.cws.application.persistence.invoice.InvoiceGateway.InvoiceRecord;
import uo.ri.cws.application.persistence.util.command.Command;
import uo.ri.cws.application.persistence.workorder.WorkOrderGateway;
import uo.ri.cws.application.persistence.workorder.WorkOrderGateway.WorkOrderRecord;
import uo.ri.cws.application.service.invoice.InvoicingService.InvoiceDto;
import uo.ri.cws.application.service.invoice.create.InvoiceDtoAssembler;
import uo.ri.util.assertion.ArgumentChecks;
import uo.ri.util.exception.BusinessException;

public class InvoiceWorkorder implements Command<InvoiceDto> {

    private static final BigDecimal VAT_RATE = new BigDecimal("21");

    private final List<String> workOrderIds;
    private final WorkOrderGateway workOrderGateway = Factories.persistence.forWorkOrder();
    private final InvoiceGateway invoiceGateway = Factories.persistence.forInvoice();

    public InvoiceWorkorder(List<String> workOrderIds) {
        ArgumentChecks.isNotNull(workOrderIds, "La lista de órdenes de trabajo no puede ser nula");
        ArgumentChecks.isTrue(!workOrderIds.isEmpty(), "La lista de órdenes de trabajo no puede estar vacía");
        for (String workOrderId : workOrderIds) {
            ArgumentChecks.isNotBlank(workOrderId, "El identificador de la orden no puede estar vacío");
        }
        this.workOrderIds = List.copyOf(workOrderIds);
    }

    @Override
    public InvoiceDto execute() throws BusinessException {
        List<WorkOrderRecord> workOrders = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (String workOrderId : workOrderIds) {
            Optional<WorkOrderRecord> result = workOrderGateway.findById(workOrderId);
            if (result.isEmpty()) {
                throw new BusinessException("La orden de trabajo no existe: " + workOrderId);
            }
            WorkOrderRecord workOrder = result.get();
            if (!"APPROVED".equals(workOrder.state) || workOrder.invoiceId != null) {
                throw new BusinessException("La orden de trabajo no se puede facturar en estado: " + workOrder.state);
            }
            if (workOrder.amount != null) {
                subtotal = subtotal.add(workOrder.amount);
            }
            workOrders.add(workOrder);
        }

        InvoiceRecord invoice = InvoiceDtoAssembler.newInvoiceRecord();
        invoice.number = invoiceGateway.findLastNumber().orElse(0L) + 1;
        invoice.vatRate = VAT_RATE;
        invoice.subtotal = subtotal.setScale(2, RoundingMode.HALF_EVEN);
        invoice.vatAmount = invoice.subtotal.multiply(invoice.vatRate).divide(new BigDecimal("100"), 2,
            RoundingMode.HALF_EVEN);
        invoice.total = invoice.subtotal.add(invoice.vatAmount).setScale(2, RoundingMode.HALF_EVEN);
        invoiceGateway.add(invoice);

        for (WorkOrderRecord workOrder : workOrders) {
            workOrder.state = "INVOICED";
            workOrder.invoiceId = invoice.id;
            workOrder.updatedAt = LocalDateTime.now();
            workOrderGateway.update(workOrder);
        }
        return InvoiceDtoAssembler.toDto(invoice);
    }

}
