package uo.ri.cws.application.service.client.commands;

import uo.ri.cws.application.persistence.util.command.Command;
import uo.ri.util.exception.BusinessException;

public class DeleteClientByNif implements Command<Void> {

    private final String nif;

    public DeleteClientByNif(String nif) {
        this.nif = nif;
    }

    @Override
    public Void execute() throws BusinessException {
        ClientCommandSupport.deleteByNif(nif);
        return null;
    }
}
