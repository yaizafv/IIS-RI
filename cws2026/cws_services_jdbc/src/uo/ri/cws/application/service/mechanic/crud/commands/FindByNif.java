package uo.ri.cws.application.service.mechanic.crud.commands;

import java.util.Optional;

import uo.ri.conf.Factories;
import uo.ri.cws.application.persistence.mechanic.MechanicGateway;
import uo.ri.cws.application.persistence.mechanic.MechanicGateway.MechanicRecord;
import uo.ri.cws.application.persistence.util.command.Command;
import uo.ri.cws.application.service.mechanic.MechanicCrudService.MechanicDto;
import uo.ri.cws.application.service.mechanic.crud.MechanicDtoAssembler;
import uo.ri.util.assertion.ArgumentChecks;
import uo.ri.util.exception.BusinessException;

public class FindByNif implements Command<Optional<MechanicDto>> {

    private final String nif;
    private final MechanicGateway mg = Factories.persistence.forMechanic();

    public FindByNif(String nif) {
        ArgumentChecks.isNotNull(nif, "Invalid argument, cannot be null");
        ArgumentChecks.isNotBlank(nif, "Invalid argument nif");
        this.nif = nif;
    }

    @Override
    public Optional<MechanicDto> execute() throws BusinessException {
        Optional<MechanicRecord> record = mg.findByNif(nif);
        if (record.isPresent()) {
            return MechanicDtoAssembler.toDtoOpt(record.get());
        } else {
            return Optional.empty();
        }
    }
}
