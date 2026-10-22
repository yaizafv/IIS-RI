package uo.ri.cws.application.ui.manager.voucher.action;

import uo.ri.conf.Factories;
import uo.ri.util.console.Console;
import uo.ri.util.exception.BusinessException;
import uo.ri.util.menu.Action;

public class ShowVoucherSummaryAction implements Action {

    @Override
    public void execute() throws BusinessException {
        Console.println("\nVoucher summary\n");
        Factories.service.forVoucherService().getVoucherSummary().forEach(row ->
                Console.printf("\t%s %s %s \t%d \t%s \t%s \t%s\n",
                        row.nif(), row.name(), row.surname(), row.issuedQty(),
                        row.totalAmount(), row.availableAmount(),
                        row.consumedAmount()));
    }
}
