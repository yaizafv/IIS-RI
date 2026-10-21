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

public class FindById implements Command<Optional<MechanicDto>> {

    private String id;
    private MechanicGateway mg = Factories.persistence.forMechanic();

    public FindById(String id) {
        ArgumentChecks.isNotNull(id, "Invalid argument, cannot be null");
        ArgumentChecks.isNotBlank(id, "El identificador del mecánico no puede estar vacío");
        this.id = id;
    }

    @Override
    public Optional<MechanicDto> execute() throws BusinessException {
        Optional<MechanicRecord> record = mg.findById(id);
        if (record.isPresent()) {
            return MechanicDtoAssembler.toDtoOpt(record.get());
        } else {
            return Optional.empty();
        }
    }
}