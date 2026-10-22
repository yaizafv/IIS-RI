package uo.ri.cws.application.ui.cashier.paymentmean.action;

import java.util.Optional;

import uo.ri.conf.Factories;
import uo.ri.cws.application.service.paymentmean.PaymentMeanCrudService;
import uo.ri.cws.application.service.paymentmean.PaymentMeanCrudService.VoucherDto;
import uo.ri.cws.application.ui.util.Printer;
import uo.ri.util.console.Console;
import uo.ri.util.exception.BusinessException;
import uo.ri.util.menu.Action;

public class FindVoucherByCodeAction implements Action {

    @Override
    public void execute() throws BusinessException {
        String code = Console.readString("Voucher code");
        PaymentMeanCrudService service = Factories.service
                .forPaymentMeanCrudService();
        Optional<VoucherDto> result = service.findVoucherByCode(code);
        if (!result.isPresent()) {
            Console.println("There is no voucher with that code");
            return;
        }
        Printer.printPaymentMean(result.get());
    }
}
