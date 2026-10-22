package uo.ri.cws.application.ui.foreman.client.action;

import java.util.Optional;

import uo.ri.conf.Factories;
import uo.ri.cws.application.service.client.ClientCrudService.ClientDto;
import uo.ri.cws.application.ui.util.Printer;
import uo.ri.util.console.Console;
import uo.ri.util.exception.BusinessException;
import uo.ri.util.menu.Action;

public class FindClientByNifAction implements Action {

    @Override
    public void execute() throws BusinessException {
        Optional<ClientDto> client = Factories.service.forClientCrudService()
                .findClientByNif(Console.readString("Client nif"));
        if (client.isEmpty()) {
            throw new BusinessException("There is no client with that nif");
        }
        Printer.printClient(client.get());
    }
}
