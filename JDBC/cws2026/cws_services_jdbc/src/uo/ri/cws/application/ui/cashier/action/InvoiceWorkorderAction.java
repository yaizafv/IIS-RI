package uo.ri.cws.application.ui.cashier.action;

import java.util.ArrayList;
import java.util.List;

import uo.ri.conf.Factories;
import uo.ri.cws.application.service.invoice.InvoicingService;
import uo.ri.cws.application.service.invoice.InvoicingService.InvoiceDto;
import uo.ri.cws.application.ui.util.Printer;
import uo.ri.util.console.Console;
import uo.ri.util.exception.BusinessException;
import uo.ri.util.menu.Action;

public class InvoiceWorkorderAction implements Action {

    @Override
    public void execute() throws BusinessException {
        List<String> workOrderIds = new ArrayList<>();
        do {
            workOrderIds.add(Console.readString("Workorder id"));
        } while (moreWorkOrders());

        InvoicingService service = Factories.service.forCreateInvoiceService();
        InvoiceDto invoice = service.create(workOrderIds);
        Printer.printInvoice(invoice);
    }

    private boolean moreWorkOrders() {
        return Console.readString("more work orders? (y/n) ").equalsIgnoreCase("y");
    }
}
