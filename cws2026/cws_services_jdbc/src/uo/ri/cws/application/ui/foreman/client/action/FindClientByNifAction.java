package uo.ri.cws.application.ui.foreman.client.action;

import java.util.Optional;

import uo.ri.conf.Factories;
import uo.ri.cws.application.service.client.ClientCrudService.ClientDto;
import uo.ri.cws.application.ui.util.Printer;
import uo.ri.util.console.Console;
import uo.ri.util.exception.BusinessException;
import uo.ri.util.exception.UserInteractionChecks;
import uo.ri.util.exception.UserInteractionException;
import uo.ri.util.menu.Action;

public class FindClientByNifAction implements Action {

    @Override
    public void execute() throws BusinessException, UserInteractionException {
        String nif = Console.readString("Client nif");
        UserInteractionChecks.isNotEmpty(nif, "The nif cannot be empty");
        Optional<ClientDto> client = Factories.service.forClientCrudService()
            .findClientByNif(nif);
        if (client.isEmpty()) {
            Console.println("There is no client with that nif");
            return;
        }
        Printer.printClient(client.get());
    }
}
