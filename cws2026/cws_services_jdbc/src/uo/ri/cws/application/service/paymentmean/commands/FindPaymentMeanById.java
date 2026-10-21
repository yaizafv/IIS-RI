package uo.ri.cws.application.service.paymentmean.commands;

import java.util.Optional;

import uo.ri.conf.Factories;
import uo.ri.cws.application.persistence.paymentmean.PaymentMeanGateway;
import uo.ri.cws.application.persistence.util.command.Command;
import uo.ri.cws.application.service.paymentmean.PaymentMeanCrudService.PaymentMeanDto;
import uo.ri.util.assertion.ArgumentChecks;

public class FindPaymentMeanById implements Command<Optional<PaymentMeanDto>> {

    private final String id;

    public FindPaymentMeanById(String id) {
        ArgumentChecks.isNotBlank(id,
                "El identificador del medio de pago no puede estar vacío");
        this.id = id;
    }

    @Override
    public Optional<PaymentMeanDto> execute() {
        return PaymentMeanDtoAssembler.toDto(
                Factories.persistence.forPaymentMean().findById(id));
    }
}
