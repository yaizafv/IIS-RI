package uo.ri.cws.application.service.paymentmean.voucher.commands;

import java.util.ArrayList;
import java.util.List;

import uo.ri.conf.Factories;
import uo.ri.cws.application.persistence.client.ClientGateway;
import uo.ri.cws.application.persistence.util.command.Command;
import uo.ri.cws.application.persistence.voucher.VoucherGateway;
import uo.ri.cws.application.persistence.voucher.VoucherGateway.VoucherRecord;
import uo.ri.cws.application.service.paymentmean.VoucherGenerationService.VoucherDto;
import uo.ri.util.assertion.ArgumentChecks;

public class FindVouchersByClientNif implements Command<List<VoucherDto>> {

    private final String nif;
    private ClientGateway clientGateway = Factories.persistence.forClient();
    private VoucherGateway voucherGateway = Factories.persistence.forVoucher();

    public FindVouchersByClientNif(String nif) {
        ArgumentChecks.isNotBlank(nif, "El NIF del cliente no puede estar vacío");
        this.nif = nif;
    }

    @Override
    public List<VoucherDto> execute() {
        List<VoucherDto> result = new ArrayList<>();
        var client = clientGateway.findByNif(nif);
        if (client.isEmpty()) {
            return result;
        }

        for (VoucherRecord record : voucherGateway.findByClientId(client.get().id)) {
            VoucherDto dto = new VoucherDto();
            dto.id = record.id;
            dto.version = record.version;
            dto.clientId = record.clientId;
            dto.accumulatedAmount = record.accumulated;
            dto.code = record.code;
            dto.description = record.description;
            dto.availableAmount = record.available;
            result.add(dto);
        }
        return result;
    }
}
