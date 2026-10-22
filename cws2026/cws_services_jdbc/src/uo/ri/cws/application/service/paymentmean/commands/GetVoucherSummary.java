package uo.ri.cws.application.service.paymentmean.commands;

import java.util.ArrayList;
import java.util.List;

import uo.ri.conf.Factories;
import uo.ri.cws.application.persistence.util.command.Command;
import uo.ri.cws.application.persistence.voucher.VoucherGateway.SummaryRecord;
import uo.ri.cws.application.service.paymentmean.VoucherGenerationService.VoucherSummaryDto;

public class GetVoucherSummary implements Command<List<VoucherSummaryDto>> {

    @Override
    public List<VoucherSummaryDto> execute() {
        List<VoucherSummaryDto> result = new ArrayList<>();
        for (SummaryRecord summary : Factories.persistence.forVoucher().findSummary()) {
            result.add(new VoucherSummaryDto(summary.nif(), summary.name(), summary.surname(), summary.issuedQty(),
                summary.totalAmount(), summary.availableAmount(), summary.consumedAmount()));
        }
        return result;
    }
}
