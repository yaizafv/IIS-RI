package uo.ri.cws.application.service.invoice;

import java.util.ArrayList;
import java.util.List;

import uo.ri.conf.Factories;
import uo.ri.cws.application.persistence.util.command.Command;
import uo.ri.cws.application.persistence.workorder.WorkOrderGateway;
import uo.ri.cws.application.persistence.workorder.WorkOrderGateway.WorkOrderRecord;
import uo.ri.cws.application.service.invoice.InvoicingService.InvoicingWorkOrderDto;
import uo.ri.util.assertion.ArgumentChecks;
import uo.ri.util.exception.BusinessException;

public class FindNotInvoicedWorkOrdersByClientNif implements Command<List<InvoicingWorkOrderDto>> {

    private final WorkOrderGateway workOrderGateway = Factories.persistence.forWorkOrder();
    private final String nif;

    public FindNotInvoicedWorkOrdersByClientNif(String nif) {
        validateNif(nif);
        this.nif = nif;
    }

    private static void validateNif(String nif) {
        ArgumentChecks.isNotBlank(nif, "El NIF no puede estar vacío");
        ArgumentChecks.isTrue(nif.matches("[0-9]{8}[A-Z]"),
                "El NIF no tiene un formato válido");
        int number = Integer.parseInt(nif.substring(0, 8));
        String letters = "TRWAGMYFPDXBNJZSQVHLCKE";
        ArgumentChecks.isTrue(nif.charAt(8) == letters.charAt(number % 23),
                "La letra del NIF no es válida");
    }

    @Override
    public List<InvoicingWorkOrderDto> execute() throws BusinessException {
        List<WorkOrderRecord> records = workOrderGateway.findNotInvoicedWorkOrdersByClientNif(nif);
        List<InvoicingWorkOrderDto> workOrders = new ArrayList<>();
        for (WorkOrderRecord record : records) {
            if (!"APPROVED".equals(record.state)) {
                continue;
            }
            InvoicingWorkOrderDto dto = new InvoicingWorkOrderDto();
            dto.id = record.id;
            dto.date = record.date;
            dto.description = record.description;
            dto.state = record.state;
            dto.amount = record.amount;
            workOrders.add(dto);
        }
        return workOrders;
    }
}
