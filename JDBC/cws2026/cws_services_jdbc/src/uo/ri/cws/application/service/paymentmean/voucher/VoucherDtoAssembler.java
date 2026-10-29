package uo.ri.cws.application.service.paymentmean.voucher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import uo.ri.cws.application.persistence.paymentmean.PaymentMeanGateway.PaymentMeanRecord;
import uo.ri.cws.application.persistence.voucher.VoucherGateway.VoucherRecord;
import uo.ri.cws.application.service.paymentmean.VoucherGenerationService.VoucherDto;

public class VoucherDtoAssembler {

    public static VoucherDto toDto(VoucherRecord record) {
        VoucherDto dto = new VoucherDto();
        dto.id = record.id;
        dto.version = record.version;
        dto.clientId = record.clientId;
        dto.accumulatedAmount = record.accumulated;
        dto.code = record.code;
        dto.description = record.description;
        dto.availableAmount = record.available;
        return dto;
    }

    public static PaymentMeanRecord toPaymentMeanRecord(String clientId) {
        PaymentMeanRecord record = new PaymentMeanRecord();
        fillCommon(record, clientId, UUID.randomUUID().toString());
        return record;
    }

    public static VoucherRecord toRecord(PaymentMeanRecord parent) {
        VoucherRecord record = new VoucherRecord();
        fillCommon(record, parent.clientId, parent.id);
        record.createdAt = parent.createdAt;
        record.updatedAt = parent.updatedAt;
        record.description = "Por tres órdenes de trabajo";
        record.available = new BigDecimal("20.00");
        record.code = "V-" + UUID.randomUUID();
        return record;
    }

    private static void fillCommon(PaymentMeanRecord record, String clientId, String id) {
        record.id = id;
        record.version = 1;
        record.entityState = "ENABLED";
        record.createdAt = LocalDateTime.now();
        record.updatedAt = record.createdAt;
        record.accumulated = BigDecimal.ZERO;
        record.clientId = clientId;
        record.dtype = "Voucher";
    }
}
