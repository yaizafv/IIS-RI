package uo.ri.cws.application.ui.cashier.action;

import java.util.List;

import uo.ri.conf.Factories;
import uo.ri.cws.application.service.invoice.InvoicingService.InvoicingWorkOrderDto;
import uo.ri.cws.application.ui.util.Printer;
import uo.ri.util.console.Console;
import uo.ri.util.exception.BusinessException;
import uo.ri.util.menu.Action;

public class FindWorkOrdersByClientAction implements Action {

    @Override
    public void execute() throws BusinessException {
        List<InvoicingWorkOrderDto> workOrders = Factories.service.forCreateInvoiceService()
            .findWorkOrdersByClientNif(Console.readString("Client nif"));
        Console.println("\nWork orders of the client\n");
        for (InvoicingWorkOrderDto workOrder : workOrders) {
            Printer.printWorkOrder(workOrder);
        }
        if (workOrders.isEmpty()) {
            Console.println("There are no work orders");
        }
    }
}
