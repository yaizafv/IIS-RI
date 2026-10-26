package uo.ri.cws.application.service.invoice.create;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import uo.ri.cws.application.persistence.invoice.InvoiceGateway.InvoiceRecord;
import uo.ri.cws.application.persistence.workorder.WorkOrderGateway.WorkOrderRecord;
import uo.ri.cws.application.service.invoice.InvoicingService.InvoiceDto;
import uo.ri.cws.application.service.invoice.InvoicingService.InvoicingWorkOrderDto;

public class DtoAssembler {

    public static InvoiceDto toDto(InvoiceRecord r) {
        InvoiceDto dto = new InvoiceDto();
        dto.id = r.id;
        dto.version = r.version;
        dto.number = r.number;
        dto.date = r.date;
        dto.state = r.state;
        dto.subtotal = r.subtotal;
        dto.vatAmount = r.vatAmount;
        dto.vatPercentage = r.vatRate;
        dto.total = r.total;
        return dto;
    }

    public static InvoicingWorkOrderDto toInvoicingWorkOrderDto(WorkOrderRecord r) {
        InvoicingWorkOrderDto dto = new InvoicingWorkOrderDto();
        dto.id = r.id;
        dto.description = r.description;
        dto.date = r.date;
        dto.state = r.state;
        dto.amount = r.amount;
        return dto;
    }

    public static InvoiceRecord newInvoiceRecord() {
        InvoiceRecord record = new InvoiceRecord();
        record.id = UUID.randomUUID().toString();
        record.version = 1;
        record.date = LocalDate.now();
        record.state = "ISSUED";
        record.usedForVoucher = false;
        record.entityState = "ENABLED";
        record.createdAt = LocalDateTime.now();
        record.updatedAt = record.createdAt;
        return record;
    }

}
