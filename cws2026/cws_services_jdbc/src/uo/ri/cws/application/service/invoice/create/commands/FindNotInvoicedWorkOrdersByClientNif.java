package uo.ri.cws.application.service.invoice.create.commands;

import java.util.ArrayList;
import java.util.List;

import uo.ri.conf.Factories;
import uo.ri.cws.application.persistence.util.command.Command;
import uo.ri.cws.application.persistence.workorder.WorkOrderGateway;
import uo.ri.cws.application.persistence.workorder.WorkOrderGateway.WorkOrderRecord;
import uo.ri.cws.application.service.invoice.InvoicingService.InvoicingWorkOrderDto;
import uo.ri.cws.application.service.invoice.create.InvoiceDtoAssembler;
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
    }

    @Override
    public List<InvoicingWorkOrderDto> execute() throws BusinessException {
        List<WorkOrderRecord> records = workOrderGateway.findNotInvoicedWorkOrdersByClientNif(nif);
        List<InvoicingWorkOrderDto> workOrders = new ArrayList<>();
        for (WorkOrderRecord record : records) {
            if (!"APPROVED".equals(record.state)) {
                continue;
            }
            workOrders.add(InvoiceDtoAssembler.toInvoicingWorkOrderDto(record));
        }
        return workOrders;
    }
}
