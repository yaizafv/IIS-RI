package uo.ri.cws.application.service.paymentmean.commands;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

import uo.ri.conf.Factories;
import uo.ri.cws.application.persistence.card.CardGateway.CardRecord;
import uo.ri.cws.application.persistence.paymentmean.PaymentMeanGateway;
import uo.ri.cws.application.persistence.util.command.Command;
import uo.ri.cws.application.service.paymentmean.PaymentMeanCrudService.CardDto;
import uo.ri.util.assertion.ArgumentChecks;
import uo.ri.util.exception.BusinessException;

public class AddCard implements Command<CardDto> {

    private final CardDto card;

    public AddCard(CardDto card) {
        ArgumentChecks.isNotNull(card, "La tarjeta no puede ser nula");
        ArgumentChecks.isNotBlank(card.cardNumber, "El número de tarjeta no puede estar vacío");
        ArgumentChecks.isNotNull(card.cardExpiration, "La fecha de caducidad no puede ser nula");
        ArgumentChecks.isNotBlank(card.cardType, "El tipo de tarjeta no puede estar vacío");
        ArgumentChecks.isNotBlank(card.clientId, "El identificador del cliente no puede estar vacío");
        this.card = card;
    }

    @Override
    public CardDto execute() throws BusinessException {
        PaymentMeanGateway paymentMeans = Factories.persistence.forPaymentMean();
        if (!paymentMeans.existsClient(card.clientId)) {
            throw new BusinessException("No existe el cliente");
        }
        if (paymentMeans.existsCardNumber(card.cardNumber)) {
            throw new BusinessException("Ya existe una tarjeta con ese número");
        }
        if (card.cardExpiration.isBefore(YearMonth.from(LocalDate.now()))) {
            throw new BusinessException("La tarjeta está caducada");
        }
        CardRecord record = new CardRecord();
        record.id = UUID.randomUUID().toString();
        record.number = card.cardNumber;
        record.validThru = card.cardExpiration;
        record.type = card.cardType;
        paymentMeans.add(toPaymentMean(record));
        Factories.persistence.forCard().add(record);
        card.id = record.id;
        card.version = record.version;
        card.accumulatedAmount = BigDecimal.ZERO;
        return card;
    }

    private PaymentMeanGateway.PaymentMeanRecord toPaymentMean(CardRecord record) {
        PaymentMeanGateway.PaymentMeanRecord parent = new PaymentMeanGateway.PaymentMeanRecord();
        parent.id = record.id;
        parent.dtype = "CreditCard";
        parent.clientId = card.clientId;
        parent.accumulated = BigDecimal.ZERO;
        return parent;
    }
}
