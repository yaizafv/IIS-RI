package uo.ri.cws.application.ui.cashier.action;

import java.util.List;

import uo.ri.conf.Factories;
import uo.ri.cws.application.service.invoice.InvoicingService;
import uo.ri.cws.application.service.invoice.InvoicingService.InvoicingWorkOrderDto;
import uo.ri.cws.application.ui.util.Printer;
import uo.ri.util.console.Console;
import uo.ri.util.exception.BusinessException;
import uo.ri.util.exception.UserInteractionChecks;
import uo.ri.util.exception.UserInteractionException;
import uo.ri.util.menu.Action;

public class FindNotInvoicedWorkOrdersByClientAction implements Action {

    @Override
    public void execute() throws BusinessException, UserInteractionException {
        InvoicingService service = Factories.service.forCreateInvoiceService();
        String nif = Console.readString("Client nif");
        UserInteractionChecks.isNotEmpty(nif, "The nif cannot be empty");
        List<InvoicingWorkOrderDto> workOrders = service.findNotInvoicedWorkOrdersByClientNif(nif);

        Console.println("\nInvoice-pending work orders\n");
        if (workOrders.isEmpty()) {
            Console.println("There is no pending work orders");
            return;
        }

        for (InvoicingWorkOrderDto workOrder : workOrders) {
            Printer.printWorkOrder(workOrder);
        }
    }
}
