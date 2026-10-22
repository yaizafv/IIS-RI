package uo.ri.cws.application.service.mechanic.crud.commands;

import java.util.ArrayList;
import java.util.List;

import uo.ri.conf.Factories;
import uo.ri.cws.application.persistence.mechanic.MechanicGateway;
import uo.ri.cws.application.persistence.util.command.Command;
import uo.ri.cws.application.service.mechanic.MechanicCrudService.MechanicDto;
import uo.ri.cws.application.service.mechanic.crud.MechanicDtoAssembler;
import uo.ri.util.exception.BusinessException;

public class ListAllMechanics implements Command<List<MechanicDto>> {

    private final MechanicGateway mg = Factories.persistence.forMechanic();

    @Override
    public List<MechanicDto> execute() throws BusinessException {
        List<MechanicDto> mechanics = new ArrayList<>();
        for (var record : mg.findAll()) {
            mechanics.add(MechanicDtoAssembler.toDtoOpt(record).orElseThrow());
        }
        return mechanics;
    }
}
