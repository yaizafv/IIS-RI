package uo.ri.cws.application.service.invoice.create.commands;

import java.util.Optional;

import uo.ri.conf.Factories;
import uo.ri.cws.application.persistence.invoice.InvoiceGateway;
import uo.ri.cws.application.persistence.invoice.InvoiceGateway.InvoiceRecord;
import uo.ri.cws.application.persistence.util.command.Command;
import uo.ri.cws.application.service.invoice.InvoicingService.InvoiceDto;
import uo.ri.util.assertion.ArgumentChecks;
import uo.ri.util.exception.BusinessException;

public class FindInvoiceByNumber implements Command<Optional<InvoiceDto>> {

    private final InvoiceGateway invoiceGateway = Factories.persistence.forInvoice();
    private final Long number;

    public FindInvoiceByNumber(Long number) {
        ArgumentChecks.isNotNull(number, "El número de factura no puede ser nulo");
        this.number = number;
    }

    @Override
    public Optional<InvoiceDto> execute() throws BusinessException {
        Optional<InvoiceRecord> record = invoiceGateway.findByNumber(number);
        return record.map(FindInvoiceByNumber::toDto);
    }

    private static InvoiceDto toDto(InvoiceRecord record) {
        InvoiceDto dto = new InvoiceDto();
        dto.id = record.id;
        dto.version = record.version;
        dto.number = record.number;
        dto.date = record.date;
        dto.state = record.state;
        dto.subtotal = record.subtotal;
        dto.total = record.total;
        dto.vatAmount = record.vatamount;
        dto.vatPercentage = record.vatrate;
        return dto;
    }
}
