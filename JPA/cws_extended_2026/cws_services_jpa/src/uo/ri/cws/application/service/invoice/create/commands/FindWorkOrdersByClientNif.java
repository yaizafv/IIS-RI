package uo.ri.cws.application.service.invoice.create.commands;

import java.util.List;

import uo.ri.conf.Factories;
import uo.ri.cws.application.repository.ClientRepository;
import uo.ri.cws.application.service.invoice.InvoicingService.InvoicingWorkOrderDto;
import uo.ri.cws.application.service.invoice.create.DtoAssembler;
import uo.ri.cws.application.util.command.Command;
import uo.ri.cws.domain.Client;
import uo.ri.cws.domain.types.Nif;
import uo.ri.util.assertion.ArgumentChecks;
import uo.ri.util.exception.BusinessException;

public class FindWorkOrdersByClientNif implements Command<List<InvoicingWorkOrderDto>> {

	private Nif nif;
	private ClientRepository repo = Factories.repository.forClient();

	public FindWorkOrdersByClientNif(String nif) {
		ArgumentChecks.isNotBlank(nif, "The client nif cannot be null or blank");
		this.nif = new Nif(nif);
	}

	@Override
	public List<InvoicingWorkOrderDto> execute() throws BusinessException {
		return repo.findByNif(nif)
				.map(Client::getWorkOrders)
				.map(DtoAssembler::toInvoicingWorkOrderDtoList)
				.orElse(List.of());
	}

}
