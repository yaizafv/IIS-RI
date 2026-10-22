package uo.ri.cws.application.service.paymentmean.commands;

import java.util.Optional;

import uo.ri.conf.Factories;
import uo.ri.cws.application.persistence.util.command.Command;
import uo.ri.cws.application.service.paymentmean.PaymentMeanCrudService.CardDto;
import uo.ri.util.assertion.ArgumentChecks;

public class FindCardByNumber implements Command<Optional<CardDto>> {

    private final String number;

    public FindCardByNumber(String number) {
        ArgumentChecks.isNotBlank(number, "El número de tarjeta no puede estar vacío");
        this.number = number;
    }

    @Override
    public Optional<CardDto> execute() {
        return PaymentMeanDtoAssembler.toCard(Factories.persistence.forPaymentMean().findByCardNumber(number));
    }
}
