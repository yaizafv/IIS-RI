package uo.ri.cws.application.service.paymentmean.crud.commands;

import java.util.Optional;

import uo.ri.conf.Factories;
import uo.ri.cws.application.repository.PaymentMeanRepository;
import uo.ri.cws.application.service.paymentmean.PaymentMeanCrudService.CardDto;
import uo.ri.cws.application.service.paymentmean.crud.DtoAssembler;
import uo.ri.cws.application.util.command.Command;
import uo.ri.cws.domain.types.CreditCardNumber;
import uo.ri.util.assertion.ArgumentChecks;
import uo.ri.util.exception.BusinessException;

public class FindCreditCardByNumber implements Command<Optional<CardDto>> {

	private String number;
	private PaymentMeanRepository repo = Factories.repository.forPaymentMean();

	public FindCreditCardByNumber(String number) {
		ArgumentChecks.isNotBlank(number, "The card number cannot be null or blank");
		this.number = number;
	}

	/**
	 * A number with a wrong format cannot belong to any card
	 */
	@Override
	public Optional<CardDto> execute() throws BusinessException {
		if (!CreditCardNumber.isValid(number)) {
			return Optional.empty();
		}
		return repo.findCreditCardByNumber(new CreditCardNumber(number)).map(DtoAssembler::toDto);
	}

}
