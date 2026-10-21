package uo.ri.cws.application.service.paymentmean.commands;

import java.util.ArrayList;
import java.util.List;

import uo.ri.conf.Factories;
import uo.ri.cws.application.persistence.util.command.Command;
import uo.ri.cws.application.service.paymentmean.PaymentMeanCrudService.PaymentMeanDto;
import uo.ri.util.assertion.ArgumentChecks;

public class FindPaymentMeansByClientNif implements Command<List<PaymentMeanDto>> {

    private final String nif;

    public FindPaymentMeansByClientNif(String nif) {
        ArgumentChecks.isNotBlank(nif,
                "El NIF del cliente no puede estar vacío");
        this.nif = nif;
    }

    @Override
    public List<PaymentMeanDto> execute() {
        List<PaymentMeanDto> result = new ArrayList<>();
        Factories.persistence.forPaymentMean().findByClientNif(nif)
                .forEach(record -> result.add(PaymentMeanDtoAssembler.toDto(record)));
        return result;
    }
}
