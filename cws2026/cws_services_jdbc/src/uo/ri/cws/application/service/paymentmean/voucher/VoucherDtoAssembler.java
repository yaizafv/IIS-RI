package uo.ri.cws.application.service.paymentmean.voucher;

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
}
