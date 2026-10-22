package uo.ri.cws.application.service.invoice.create.commands;

import java.util.ArrayList;
import java.util.List;

import uo.ri.conf.Factories;
import uo.ri.cws.application.persistence.paymentmean.PaymentMeanGateway;
import uo.ri.cws.application.persistence.paymentmean.PaymentMeanGateway.CardRecord;
import uo.ri.cws.application.persistence.paymentmean.PaymentMeanGateway.VoucherRecord;
import uo.ri.cws.application.persistence.util.command.Command;
import uo.ri.cws.application.service.invoice.InvoicingService.CardDto;
import uo.ri.cws.application.service.invoice.InvoicingService.CashDto;
import uo.ri.cws.application.service.invoice.InvoicingService.PaymentMeanDto;
import uo.ri.cws.application.service.invoice.InvoicingService.VoucherDto;
import uo.ri.util.assertion.ArgumentChecks;
import uo.ri.util.exception.BusinessException;

public class FindPaymentMeansByClientNif implements Command<List<PaymentMeanDto>> {

    private final String nif;

    public FindPaymentMeansByClientNif(String nif) {
        validateNif(nif);
        this.nif = nif;
    }

    @Override
    public List<PaymentMeanDto> execute() throws BusinessException {
        PaymentMeanGateway gateway = Factories.persistence.forPaymentMean();
        List<PaymentMeanDto> result = new ArrayList<>();
        for (PaymentMeanGateway.PaymentMeanRecord record : gateway.findByClientNif(nif)) {
            result.add(toDto(record));
        }
        return result;
    }

    private static PaymentMeanDto toDto(PaymentMeanGateway.PaymentMeanRecord record) {
        PaymentMeanDto dto;
        if (record instanceof CardRecord card) {
            CardDto result = new CardDto();
            result.cardNumber = card.number;
            result.cardType = card.type;
            result.cardExpiration = card.validThru;
            dto = result;
        } else if (record instanceof VoucherRecord voucher) {
            VoucherDto result = new VoucherDto();
            result.code = voucher.code;
            result.description = voucher.description;
            result.availableAmount = voucher.available;
            dto = result;
        } else {
            dto = new CashDto();
        }
        dto.id = record.id;
        dto.version = record.version;
        dto.clientId = record.clientId;
        dto.accumulated = record.accumulated;
        return dto;
    }

    public static void validateNif(String nif) {
        ArgumentChecks.isNotBlank(nif, "El NIF no puede estar vacío");
        ArgumentChecks.isTrue(nif.matches("[0-9]{8}[A-Z]"), "El NIF no tiene un formato válido");
        int number = Integer.parseInt(nif.substring(0, 8));
        String letters = "TRWAGMYFPDXBNJZSQVHLCKE";
        ArgumentChecks.isTrue(nif.charAt(8) == letters.charAt(number % 23), "La letra del NIF no es válida");
    }
}
