package uo.ri.cws.application.ui.manager.voucher.action;

import java.math.BigDecimal;
import java.util.List;

import uo.ri.conf.Factories;
import uo.ri.cws.application.service.paymentmean.VoucherGenerationService.VoucherDto;
import uo.ri.util.console.Console;
import uo.ri.util.exception.BusinessException;
import uo.ri.util.menu.Action;

public class FindVouchersByClientNifAction implements Action {

    @Override
    public void execute() throws BusinessException {
        List<VoucherDto> vouchers = Factories.service.forVoucherService()
                .findVouchersByClientNif(Console.readString("Client nif"));
        Console.println("\nClient vouchers\n");
        BigDecimal available = BigDecimal.ZERO;
        BigDecimal consumed = BigDecimal.ZERO;
        for (VoucherDto voucher : vouchers) {
            Console.printf("\t%s \t%s \t%s \t%s\n",
                    voucher.id, voucher.code, voucher.availableAmount,
                    voucher.accumulatedAmount);
            available = available.add(voucher.availableAmount);
            consumed = consumed.add(voucher.accumulatedAmount);
        }
        Console.printf("Vouchers: %d, total: %s, consumed: %s, available: %s\n",
                vouchers.size(), available.add(consumed), consumed, available);
    }
}
