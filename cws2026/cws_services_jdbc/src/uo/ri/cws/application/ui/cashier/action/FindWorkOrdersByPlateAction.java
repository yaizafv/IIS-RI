package uo.ri.cws.application.ui.cashier.action;

import uo.ri.conf.Factories;
import uo.ri.cws.application.ui.util.Printer;
import uo.ri.util.console.Console;
import uo.ri.util.exception.BusinessException;
import uo.ri.util.menu.Action;

public class FindWorkOrdersByPlateAction implements Action {

    @Override
    public void execute() throws BusinessException {
        var workOrders = Factories.service.forCreateInvoiceService()
                .findWorkOrdersByPlateNumber(Console.readString("Plate number"));
        Console.println("\nWork orders for the vehicle\n");
        workOrders.forEach(Printer::printWorkOrder);
        if (workOrders.isEmpty()) {
            Console.println("There are no work orders for the vehicle");
        }
    }
}
