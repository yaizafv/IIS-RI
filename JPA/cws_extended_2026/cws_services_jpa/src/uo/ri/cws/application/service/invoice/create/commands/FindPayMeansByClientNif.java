package uo.ri.cws.application.service.invoice.create.commands;

import java.util.List;

import uo.ri.conf.Factories;
import uo.ri.cws.application.repository.PaymentMeanRepository;
import uo.ri.cws.application.service.invoice.InvoicingService.PaymentMeanDto;
import uo.ri.cws.application.service.invoice.create.DtoAssembler;
import uo.ri.cws.application.util.command.Command;
import uo.ri.cws.domain.types.Nif;
import uo.ri.util.assertion.ArgumentChecks;
import uo.ri.util.exception.BusinessException;

public class FindPayMeansByClientNif implements Command<List<PaymentMeanDto>> {

	private Nif nif;
	private PaymentMeanRepository repo = Factories.repository.forPaymentMean();

	public FindPayMeansByClientNif(String nif) {
		ArgumentChecks.isNotBlank(nif, "The client nif cannot be null or blank");
		this.nif = new Nif(nif);
	}

	@Override
	public List<PaymentMeanDto> execute() throws BusinessException {
		return DtoAssembler.toPaymentMeanDtoList(repo.findPaymentMeansByClientNif(nif));
	}

}
