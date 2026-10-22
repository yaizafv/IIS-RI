package uo.ri.cws.application.service.client.commands;

import java.util.Optional;

import uo.ri.cws.application.persistence.util.command.Command;
import uo.ri.conf.Factories;
import uo.ri.cws.application.persistence.client.ClientGateway;
import uo.ri.cws.application.service.client.crud.ClientDtoAssembler;
import uo.ri.util.assertion.ArgumentChecks;
import uo.ri.cws.application.service.client.ClientCrudService.ClientDto;
import uo.ri.util.exception.BusinessException;

public class FindClientByNif implements Command<Optional<ClientDto>> {

    private final String nif;
    private final ClientGateway clients = Factories.persistence.forClient();

    public FindClientByNif(String nif) {
        ArgumentChecks.isNotBlank(nif, "El NIF no puede estar vacío");
        this.nif = nif;
    }

    @Override
    public Optional<ClientDto> execute() throws BusinessException {
        Optional<ClientGateway.ClientRecord> record = clients.findByNif(nif);
        if (record.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(ClientDtoAssembler.toDto(record.get()));
    }
}
