package uo.ri.cws.application.ui.manager.voucher.action;

import java.util.List;

import uo.ri.conf.Factories;
import uo.ri.cws.application.service.paymentmean.VoucherGenerationService.VoucherSummaryDto;
import uo.ri.util.console.Console;
import uo.ri.util.exception.BusinessException;
import uo.ri.util.menu.Action;

public class ShowVoucherSummaryAction implements Action {

    @Override
    public void execute() throws BusinessException {
        Console.println("\nVoucher summary\n");
        List<VoucherSummaryDto> summary = Factories.service.forVoucherService()
                .getVoucherSummary();
        for (VoucherSummaryDto row : summary) {
            Console.printf("\t%s %s %s \t%d \t%s \t%s \t%s\n",
                    row.nif(), row.name(), row.surname(), row.issuedQty(),
                    row.totalAmount(), row.availableAmount(),
                    row.consumedAmount());
        }
    }
}
