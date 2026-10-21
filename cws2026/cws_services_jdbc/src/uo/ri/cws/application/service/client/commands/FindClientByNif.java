package uo.ri.cws.application.service.client.commands;

import java.util.Optional;

import uo.ri.cws.application.persistence.util.command.Command;
import uo.ri.cws.application.service.client.ClientCrudService.ClientDto;
import uo.ri.util.exception.BusinessException;

public class FindClientByNif implements Command<Optional<ClientDto>> {

    private final String nif;

    public FindClientByNif(String nif) {
        this.nif = nif;
    }

    @Override
    public Optional<ClientDto> execute() throws BusinessException {
        return ClientCommandSupport.findByNif(nif);
    }
}
