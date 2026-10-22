package uo.ri.cws.application.ui.foreman.client.action;

import uo.ri.conf.Factories;
import uo.ri.cws.application.ui.util.Printer;
import uo.ri.util.console.Console;
import uo.ri.util.exception.BusinessException;
import uo.ri.util.menu.Action;

public class FindRecommendedClientsAction implements Action {

    @Override
    public void execute() throws BusinessException {
        var clients = Factories.service.forClientCrudService()
                .findClientsRecommendedBy(Console.readString("Recommender nif"));
        Console.println("\nList of recommended clients\n");
        clients.forEach(Printer::printClient);
    }
}
