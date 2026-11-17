package uo.ri.cws.application.service.paymentmean.crud.commands;

import java.time.YearMonth;
import java.util.Optional;

import uo.ri.conf.Factories;
import uo.ri.cws.application.repository.ClientRepository;
import uo.ri.cws.application.repository.PaymentMeanRepository;
import uo.ri.cws.application.service.paymentmean.PaymentMeanCrudService.CardDto;
import uo.ri.cws.application.util.command.Command;
import uo.ri.cws.domain.Associations;
import uo.ri.cws.domain.Client;
import uo.ri.cws.domain.CreditCard;
import uo.ri.cws.domain.types.CreditCardNumber;
import uo.ri.util.assertion.ArgumentChecks;
import uo.ri.util.exception.BusinessChecks;
import uo.ri.util.exception.BusinessException;

public class AddCard implements Command<CardDto> {

	private CardDto dto;
	private ClientRepository clientRepo = Factories.repository.forClient();
	private PaymentMeanRepository pmRepo = Factories.repository.forPaymentMean();

	public AddCard(CardDto dto) {
		ArgumentChecks.isNotNull(dto, "The card dto cannot be null");
		ArgumentChecks.isNotBlank(dto.clientId, "The client id cannot be null or blank");
		ArgumentChecks.isNotBlank(dto.cardNumber, "The card number cannot be null or blank");
		ArgumentChecks.isNotBlank(dto.cardType, "The card type cannot be null or blank");
		ArgumentChecks.isNotNull(dto.cardExpiration, "The card expiration cannot be null");
		this.dto = dto;
	}

	@Override
	public CardDto execute() throws BusinessException {
		Optional<Client> oc = clientRepo.findById(dto.clientId);
		BusinessChecks.exists(oc, "The client does not exist");
		BusinessChecks.isTrue(CreditCardNumber.isValid(dto.cardNumber), "The card number is not valid");
		CreditCardNumber number = new CreditCardNumber(dto.cardNumber);
		BusinessChecks.doesNotExist(pmRepo.findCreditCardByNumber(number),
				"There is already a card with the same number");
		BusinessChecks.isFalse(dto.cardExpiration.isBefore(YearMonth.now()), "The card is outdated");

		CreditCard card = new CreditCard(number, dto.cardType, dto.cardExpiration);
		Associations.Holds.link(oc.get(), card);
		pmRepo.add(card);

		dto.id = card.getId();
		dto.version = card.getVersion();
		return dto;
	}

}
