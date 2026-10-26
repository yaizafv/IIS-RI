package uo.ri.cws.application.service.paymentmean.voucher.commands;

import java.util.ArrayList;
import java.util.List;

import uo.ri.conf.Factories;
import uo.ri.cws.application.persistence.client.ClientGateway;
import uo.ri.cws.application.persistence.util.command.Command;
import uo.ri.cws.application.persistence.voucher.VoucherGateway;
import uo.ri.cws.application.persistence.voucher.VoucherGateway.VoucherRecord;
import uo.ri.cws.application.service.paymentmean.VoucherGenerationService.VoucherDto;
import uo.ri.cws.application.service.paymentmean.voucher.VoucherDtoAssembler;
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
            result.add(VoucherDtoAssembler.toDto(record));
        }
        return result;
    }
}
