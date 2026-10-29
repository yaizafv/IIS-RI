package uo.ri.cws.application.service.client.crud.commands;

import java.util.ArrayList;
import java.util.List;

import uo.ri.conf.Factories;
import uo.ri.cws.application.persistence.client.ClientGateway;
import uo.ri.cws.application.persistence.util.command.Command;
import uo.ri.cws.application.service.client.ClientCrudService.ClientDto;
import uo.ri.cws.application.service.client.crud.ClientDtoAssembler;
import uo.ri.util.exception.BusinessException;

public class FindAllClients implements Command<List<ClientDto>> {

    private final ClientGateway clients = Factories.persistence.forClient();

    @Override
    public List<ClientDto> execute() throws BusinessException {
        List<ClientDto> result = new ArrayList<>();
        for (var record : clients.findAll()) {
            result.add(ClientDtoAssembler.toDto(record));
        }
        return result;
    }
}
