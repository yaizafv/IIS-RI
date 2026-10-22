package uo.ri.cws.application.service.paymentmean.commands;

import java.util.Optional;

import uo.ri.conf.Factories;
import uo.ri.cws.application.persistence.util.command.Command;
import uo.ri.cws.application.service.paymentmean.PaymentMeanCrudService.VoucherDto;
import uo.ri.util.assertion.ArgumentChecks;

public class FindVoucherByCode implements Command<Optional<VoucherDto>> {

    private final String code;

    public FindVoucherByCode(String code) {
        ArgumentChecks.isNotBlank(code, "El código del vale no puede estar vacío");
        this.code = code;
    }

    @Override
    public Optional<VoucherDto> execute() {
        return PaymentMeanDtoAssembler.toVoucher(Factories.persistence.forPaymentMean().findByVoucherCode(code));
    }
}
