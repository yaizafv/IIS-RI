package uo.ri.cws.application.ui.manager.voucher.action;

import uo.ri.conf.Factories;
import uo.ri.util.console.Console;
import uo.ri.util.exception.BusinessException;
import uo.ri.util.menu.Action;

public class GenerateVouchersAction implements Action {

    @Override
    public void execute() throws BusinessException {
        int[] generated = Factories.service.forVoucherService().generateVouchers();
        Console.println("\nVoucher generation finished");
        Console.printf("\t20 euro vouchers for work orders: %d\n", generated[0]);
        Console.printf("\t25 euro vouchers for recommendations: %d\n", generated[1]);
        Console.printf("\t30 euro vouchers for invoices: %d\n", generated[2]);
    }
}
