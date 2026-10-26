package uo.ri.cws.application.service.invoice.create;

import java.util.ArrayList;
import java.util.List;

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

    public static List<InvoicingWorkOrderDto> toInvoicingWorkOrderDtoList(List<WorkOrderRecord> records) {
        List<InvoicingWorkOrderDto> res = new ArrayList<>();
        for (WorkOrderRecord r : records) {
            res.add(toInvoicingWorkOrderDto(r));
        }
        return res;
    }
    
    public static InvoiceRecord toRecord(InvoiceDto dto) {
        InvoiceRecord record = new InvoiceRecord();
        record.id = dto.id;
        record.version = dto.version;
        record.number = dto.number;
        record.date = dto.date;
        record.state = dto.state;
        record.subtotal = dto.subtotal;
        record.vatAmount = dto.vatAmount;
        record.vatRate = dto.vatPercentage;
        record.total = dto.total;
        return record;
    }

}
