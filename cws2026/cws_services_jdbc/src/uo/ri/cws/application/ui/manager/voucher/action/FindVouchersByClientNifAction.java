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
        vouchers.forEach(v -> Console.printf("\t%s \t%s \t%s \t%s\n",
                v.id, v.code, v.availableAmount, v.accumulatedAmount));
        BigDecimal available = vouchers.stream().map(v -> v.availableAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal consumed = vouchers.stream().map(v -> v.accumulatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Console.printf("Vouchers: %d, total: %s, consumed: %s, available: %s\n",
                vouchers.size(), available.add(consumed), consumed, available);
    }
}
