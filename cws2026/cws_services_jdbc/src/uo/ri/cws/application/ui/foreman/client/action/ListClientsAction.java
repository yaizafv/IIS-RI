package uo.ri.cws.application.ui.foreman.client.action;

import uo.ri.conf.Factories;
import uo.ri.cws.application.ui.util.Printer;
import uo.ri.util.console.Console;
import uo.ri.util.exception.BusinessException;
import uo.ri.util.menu.Action;

public class ListClientsAction implements Action {

    @Override
    public void execute() throws BusinessException {
        Console.println("\nList of clients\n");
        Factories.service.forClientCrudService().findAllClients()
                .forEach(Printer::printClient);
    }
}
