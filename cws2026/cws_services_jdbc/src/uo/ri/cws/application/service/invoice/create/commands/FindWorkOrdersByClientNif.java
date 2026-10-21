package uo.ri.cws.application.service.invoice.create.commands;

import java.util.ArrayList;
import java.util.List;

import uo.ri.conf.Factories;
import uo.ri.cws.application.persistence.workorder.WorkOrderGateway;
import uo.ri.cws.application.persistence.workorder.WorkOrderGateway.WorkOrderRecord;
import uo.ri.cws.application.persistence.util.command.Command;
import uo.ri.cws.application.service.invoice.InvoicingService.InvoicingWorkOrderDto;
import uo.ri.util.exception.BusinessException;

public class FindWorkOrdersByClientNif implements Command<List<InvoicingWorkOrderDto>> {

    private final WorkOrderGateway workOrderGateway =
            Factories.persistence.forWorkOrder();
    private final String nif;

    public FindWorkOrdersByClientNif(String nif) {
        FindPaymentMeansByClientNif.validateNif(nif);
        this.nif = nif;
    }

    @Override
    public List<InvoicingWorkOrderDto> execute() throws BusinessException {
        List<InvoicingWorkOrderDto> workOrders = new ArrayList<>();
        for (WorkOrderRecord record : workOrderGateway.findByClientNif(nif)) {
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
