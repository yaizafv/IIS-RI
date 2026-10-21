package uo.ri.cws.application.service.client.commands;

import uo.ri.cws.application.persistence.util.command.Command;
import uo.ri.cws.application.service.client.ClientCrudService.ClientDto;
import uo.ri.util.exception.BusinessException;

public class UpdateClient implements Command<Void> {

    private final ClientDto client;

    public UpdateClient(ClientDto client) {
        this.client = client;
    }

    @Override
    public Void execute() throws BusinessException {
        ClientCommandSupport.update(client);
        return null;
    }
}
