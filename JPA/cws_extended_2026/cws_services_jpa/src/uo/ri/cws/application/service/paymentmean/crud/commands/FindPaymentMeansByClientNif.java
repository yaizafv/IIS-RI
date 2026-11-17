package uo.ri.cws.application.service.paymentmean.crud.commands;

import java.util.List;

import uo.ri.conf.Factories;
import uo.ri.cws.application.repository.PaymentMeanRepository;
import uo.ri.cws.application.service.paymentmean.PaymentMeanCrudService.PaymentMeanDto;
import uo.ri.cws.application.service.paymentmean.crud.DtoAssembler;
import uo.ri.cws.application.util.command.Command;
import uo.ri.cws.domain.types.Nif;
import uo.ri.util.assertion.ArgumentChecks;
import uo.ri.util.exception.BusinessException;

public class FindPaymentMeansByClientNif implements Command<List<PaymentMeanDto>> {

	private String nif;
	private PaymentMeanRepository repo = Factories.repository.forPaymentMean();

	public FindPaymentMeansByClientNif(String nif) {
		ArgumentChecks.isNotBlank(nif, "The client nif cannot be null or blank");
		this.nif = nif;
	}

	/**
	 * A nif with a wrong format cannot belong to any client
	 */
	@Override
	public List<PaymentMeanDto> execute() throws BusinessException {
		if (!Nif.isValid(nif)) {
			return List.of();
		}
		return DtoAssembler.toDtoList(repo.findPaymentMeansByClientNif(new Nif(nif)));
	}

}
