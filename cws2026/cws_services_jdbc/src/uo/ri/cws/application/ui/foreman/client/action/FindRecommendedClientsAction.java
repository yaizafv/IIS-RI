package uo.ri.cws.application.ui.foreman.client.action;

import java.util.List;

import uo.ri.conf.Factories;
import uo.ri.cws.application.service.client.ClientCrudService.ClientDto;
import uo.ri.cws.application.ui.util.Printer;
import uo.ri.util.console.Console;
import uo.ri.util.exception.BusinessException;
import uo.ri.util.exception.UserInteractionChecks;
import uo.ri.util.exception.UserInteractionException;
import uo.ri.util.menu.Action;

public class FindRecommendedClientsAction implements Action {

    @Override
    public void execute() throws BusinessException, UserInteractionException {
        String nif = Console.readString("Recommender nif");
        UserInteractionChecks.isNotEmpty(nif, "The nif cannot be empty");
        List<ClientDto> clients = Factories.service.forClientCrudService()
            .findClientsRecommendedBy(nif);
        Console.println("\nList of recommended clients\n");
        for (ClientDto client : clients) {
            Printer.printClient(client);
        }
    }
}
