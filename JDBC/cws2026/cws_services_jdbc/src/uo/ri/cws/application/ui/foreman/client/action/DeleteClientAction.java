package uo.ri.cws.application.ui.foreman.client.action;

import uo.ri.conf.Factories;
import uo.ri.util.console.Console;
import uo.ri.util.exception.BusinessException;
import uo.ri.util.exception.UserInteractionChecks;
import uo.ri.util.exception.UserInteractionException;
import uo.ri.util.menu.Action;

public class DeleteClientAction implements Action {

    @Override
    public void execute() throws BusinessException, UserInteractionException {
        String nif = Console.readString("Client nif");
        UserInteractionChecks.isNotEmpty(nif, "The nif cannot be empty");
        Factories.service.forClientCrudService().deleteClientByNif(nif);
        Console.println("The client has been deleted");
    }
}
