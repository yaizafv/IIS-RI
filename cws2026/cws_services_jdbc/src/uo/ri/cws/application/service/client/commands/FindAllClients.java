package uo.ri.cws.application.service.client.commands;

import java.util.List;

import uo.ri.cws.application.persistence.util.command.Command;
import uo.ri.conf.Factories;
import uo.ri.cws.application.persistence.client.ClientGateway;
import uo.ri.cws.application.service.client.crud.ClientDtoAssembler;
import uo.ri.cws.application.service.client.ClientCrudService.ClientDto;
import uo.ri.util.exception.BusinessException;

public class FindAllClients implements Command<List<ClientDto>> {

    private final ClientGateway clients = Factories.persistence.forClient();

    @Override
    public List<ClientDto> execute() throws BusinessException {
        return clients.findAll().stream()
                .map(ClientDtoAssembler::toDto)
                .toList();
    }
}
