package uo.ri.cws.application.service.paymentmean.commands;

import java.util.Optional;

import uo.ri.cws.application.persistence.paymentmean.PaymentMeanGateway;
import uo.ri.cws.application.persistence.paymentmean.PaymentMeanGateway.CardRecord;
import uo.ri.cws.application.persistence.paymentmean.PaymentMeanGateway.VoucherRecord;
import uo.ri.cws.application.service.paymentmean.PaymentMeanCrudService.CardDto;
import uo.ri.cws.application.service.paymentmean.PaymentMeanCrudService.CashDto;
import uo.ri.cws.application.service.paymentmean.PaymentMeanCrudService.PaymentMeanDto;
import uo.ri.cws.application.service.paymentmean.PaymentMeanCrudService.VoucherDto;

final class PaymentMeanDtoAssembler {

    private PaymentMeanDtoAssembler() {
    }

    static Optional<PaymentMeanDto> toDto(
            Optional<PaymentMeanGateway.PaymentMeanRecord> record) {
        return record.map(PaymentMeanDtoAssembler::toDto);
    }

    static PaymentMeanDto toDto(PaymentMeanGateway.PaymentMeanRecord record) {
        PaymentMeanDto dto;
        if (record instanceof CardRecord card) {
            CardDto result = new CardDto();
            result.cardNumber = card.number;
            result.cardExpiration = card.validThru;
            result.cardType = card.type;
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
        dto.accumulatedAmount = record.accumulated;
        return dto;
    }

    static Optional<VoucherDto> toVoucher(
            Optional<PaymentMeanGateway.PaymentMeanRecord> record) {
        return record.filter(r -> "Voucher".equals(r.dtype))
                .map(PaymentMeanDtoAssembler::toDto)
                .map(VoucherDto.class::cast);
    }

    static Optional<CardDto> toCard(
            Optional<PaymentMeanGateway.PaymentMeanRecord> record) {
        return record.filter(r -> "CreditCard".equals(r.dtype))
                .map(PaymentMeanDtoAssembler::toDto)
                .map(CardDto.class::cast);
    }
}
