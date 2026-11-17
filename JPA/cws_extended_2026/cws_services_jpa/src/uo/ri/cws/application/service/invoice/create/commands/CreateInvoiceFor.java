package uo.ri.cws.application.service.invoice.create.commands;

import java.util.List;
import java.util.Objects;

import uo.ri.conf.Factories;
import uo.ri.cws.application.query.InvoiceQueries;
import uo.ri.cws.application.repository.InvoiceRepository;
import uo.ri.cws.application.repository.WorkOrderRepository;
import uo.ri.cws.application.service.invoice.InvoicingService.InvoiceDto;
import uo.ri.cws.application.service.invoice.create.DtoAssembler;
import uo.ri.cws.application.util.command.Command;
import uo.ri.cws.domain.Invoice;
import uo.ri.cws.domain.WorkOrder;
import uo.ri.util.assertion.ArgumentChecks;
import uo.ri.util.exception.BusinessChecks;
import uo.ri.util.exception.BusinessException;

public class CreateInvoiceFor implements Command<InvoiceDto> {

	private List<String> workOrderIds;
	private WorkOrderRepository woRepo = Factories.repository.forWorkOrder();
	private InvoiceRepository invoiceRepo = Factories.repository.forInvoice();
	private InvoiceQueries invoiceQueries = Factories.query.forInvoice();

	public CreateInvoiceFor(List<String> workOrderIds) {
		ArgumentChecks.isNotNull(workOrderIds, "The list of work order ids cannot be null");
		ArgumentChecks.isFalse(workOrderIds.isEmpty(), "The list of work order ids cannot be empty");
		ArgumentChecks.isFalse(workOrderIds.stream().anyMatch(Objects::isNull),
				"The work order ids cannot be null");
		this.workOrderIds = workOrderIds;
	}

	@Override
	public InvoiceDto execute() throws BusinessException {
		List<WorkOrder> workOrders = woRepo.findByIds(workOrderIds);
		BusinessChecks.isTrue(workOrders.size() == workOrderIds.size(), "Some work order does not exist");
		BusinessChecks.isTrue(workOrders.stream().allMatch(WorkOrder::isApproved),
				"Some work order is not approved");

		Invoice invoice = new Invoice(invoiceQueries.getNextInvoiceNumber(), workOrders);
		invoiceRepo.add(invoice);
		return DtoAssembler.toDto(invoice);
	}

}
