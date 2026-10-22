package uo.ri.cws.application.ui.cashier.action;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import uo.ri.conf.Factories;
import uo.ri.cws.application.service.invoice.InvoicingService;
import uo.ri.cws.application.service.invoice.InvoicingService.InvoiceDto;
import uo.ri.cws.application.service.invoice.InvoicingService.PaymentMeanDto;
import uo.ri.cws.application.ui.util.Printer;
import uo.ri.util.console.Console;
import uo.ri.util.exception.BusinessException;
import uo.ri.util.menu.Action;

public class SettleInvoiceAction implements Action {

    @Override
    public void execute() throws BusinessException {
        InvoicingService service = Factories.service.forCreateInvoiceService();
        Long number = Console.readLong("Invoice number");
        Optional<InvoiceDto> invoice = service.findInvoiceByNumber(number);
        if (invoice.isEmpty()) {
            throw new BusinessException("There is no such invoice");
        }

        Printer.printInvoice(invoice.get());
        String nif = Console.readString("Client nif");
        List<PaymentMeanDto> means = service.findPayMeansByClientNif(nif);
        if (means.isEmpty()) {
            throw new BusinessException(
                    "The client has no payment means or the nif is wrong");
        }

        Map<String, BigDecimal> charges = new HashMap<>();
        BigDecimal total = BigDecimal.ZERO;
        while (total.compareTo(invoice.get().total) < 0) {
            Printer.printPaymentMeans(means);
            String id = Console.readString("Payment mean id");
            BigDecimal amount = Console.readBigDecimal("Amount to charge");
            charges.put(id, charges.getOrDefault(id, BigDecimal.ZERO).add(amount));
            total = BigDecimal.ZERO;
            for (BigDecimal charge : charges.values()) {
                total = total.add(charge);
            }
        }

        service.settleInvoice(invoice.get().id, charges);
        Console.println("The invoice has been settled");
    }
}
