package uo.ri.cws.application.service.mechanic.crud.commands;

import java.util.Optional;

import uo.ri.conf.Factories;
import uo.ri.cws.application.persistence.mechanic.MechanicGateway;
import uo.ri.cws.application.persistence.mechanic.MechanicGateway.MechanicRecord;
import uo.ri.cws.application.persistence.util.command.Command;
import uo.ri.cws.application.service.mechanic.MechanicCrudService.MechanicDto;
import uo.ri.cws.application.service.mechanic.crud.MechanicDtoAssembler;
import uo.ri.util.assertion.ArgumentChecks;
import uo.ri.util.assertion.BusinessChecks;
import uo.ri.util.exception.BusinessException;

public class UpdateMechanic implements Command<Void> {

    private final MechanicDto dto;
    private MechanicGateway mg = Factories.persistence.forMechanic();

    public UpdateMechanic(MechanicDto dto) {
        ArgumentChecks.isNotNull(dto, "Invalid dto");
        ArgumentChecks.isNotBlank(dto.id, "Invalid id");
        ArgumentChecks.isNotNull(dto.id, "Invalid id");
        ArgumentChecks.isNotBlank(dto.name, "Invalid name");
        ArgumentChecks.isNotNull(dto.name, "Invalid name");
        ArgumentChecks.isNotBlank(dto.surname, "Invalid surname");
        ArgumentChecks.isNotNull(dto.surname, "Invalid surname");
        ArgumentChecks.isNotBlank(dto.nif, "Invalid nif");
        this.dto = dto;
    }

    @Override
    public Void execute() throws BusinessException {
        checkMechanicExists();
        updateMechanic();
        return null;
    }

    private void checkMechanicExists() throws BusinessException {
        Optional<MechanicRecord> omr = mg.findById(dto.id);
        BusinessChecks.exists(omr, "The mechanic doesnt exists");
        if (omr.get().version != dto.version) {
            throw new BusinessException("The mechanic has been modified by another operation");
        }
    }

    private void updateMechanic() {
        MechanicRecord mr = MechanicDtoAssembler.toRecord(dto);
        mg.update(mr);
    }
}