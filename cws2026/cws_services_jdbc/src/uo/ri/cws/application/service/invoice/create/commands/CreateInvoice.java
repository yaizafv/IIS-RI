package uo.ri.cws.application.service.invoice.create.commands;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import uo.ri.conf.Factories;
import uo.ri.cws.application.persistence.invoice.InvoiceGateway;
import uo.ri.cws.application.persistence.invoice.InvoiceGateway.InvoiceRecord;
import uo.ri.cws.application.persistence.workorder.WorkOrderGateway;
import uo.ri.cws.application.persistence.workorder.WorkOrderGateway.WorkOrderRecord;
import uo.ri.cws.application.persistence.util.command.Command;
import uo.ri.cws.application.service.invoice.InvoicingService.InvoiceDto;
import uo.ri.util.assertion.ArgumentChecks;
import uo.ri.util.exception.BusinessException;

public class CreateInvoice implements Command<InvoiceDto> {

    private final List<String> workOrderIds;
    private final WorkOrderGateway workOrderGateway =
            Factories.persistence.forWorkOrder();
    private final InvoiceGateway invoiceGateway =
            Factories.persistence.forInvoice();

    public CreateInvoice(List<String> workOrderIds) {
        ArgumentChecks.isNotNull(workOrderIds, "La lista de órdenes de trabajo no puede ser nula");
        ArgumentChecks.isTrue(!workOrderIds.isEmpty(), "La lista de órdenes de trabajo no puede estar vacía");
        for (String workOrderId : workOrderIds) {
            ArgumentChecks.isNotBlank(workOrderId, "El identificador de la orden no puede estar vacío");
        }
        this.workOrderIds = List.copyOf(workOrderIds);
    }

    @Override
    public InvoiceDto execute() throws BusinessException {
        InvoiceDto dto = new InvoiceDto();
        dto.id = UUID.randomUUID().toString();
        dto.version = 1;
        dto.date = LocalDate.now();
        dto.state = "ISSUED";
        dto.vatPercentage = new BigDecimal("21");

        BigDecimal subtotal = BigDecimal.ZERO;
        for (String workOrderId : workOrderIds) {
            WorkOrderRecord workOrder = workOrderGateway.findById(workOrderId)
                    .orElseThrow(() -> new BusinessException(
                            "La orden de trabajo no existe: " + workOrderId));
            if (!"APPROVED".equals(workOrder.state) || workOrder.invoiceId != null) {
                throw new BusinessException(
                        "La orden de trabajo no se puede facturar en estado: " + workOrder.state);
            }

            if (workOrder.amount != null) {
                subtotal = subtotal.add(workOrder.amount);
            }
        }

        dto.number = invoiceGateway.findLastNumber().orElse(0L) + 1;
        dto.subtotal = subtotal.setScale(2, RoundingMode.HALF_EVEN);
        dto.vatAmount = dto.subtotal.multiply(dto.vatPercentage)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_EVEN);
        dto.total = dto.subtotal.add(dto.vatAmount).setScale(2, RoundingMode.HALF_UP);

        InvoiceRecord invoice = new InvoiceRecord();
        invoice.id = dto.id;
        invoice.version = dto.version;
        invoice.number = dto.number;
        invoice.date = dto.date;
        invoice.state = dto.state;
        invoice.subtotal = dto.subtotal;
        invoice.total = dto.total;
        invoice.vatAmount = dto.vatAmount;
        invoice.vatRate = dto.vatPercentage;
        invoice.createdAt = LocalDateTime.now();
        invoice.updatedAt = invoice.createdAt;
        invoiceGateway.add(invoice);

        for (String workOrderId : workOrderIds) {
            workOrderGateway.setInvoiced(workOrderId, dto.id);
        }
        return dto;
    }
}
