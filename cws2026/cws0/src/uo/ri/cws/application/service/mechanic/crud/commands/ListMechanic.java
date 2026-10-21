package uo.ri.cws.application.service.mechanic.crud.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import uo.ri.cws.application.service.mechanic.MechanicCrudService.MechanicDto;
import uo.ri.util.assertion.ArgumentChecks;
import uo.ri.util.exception.BusinessException;

public class ListMechanic {

    private final String nif;

    public ListMechanic(String nif) {
        ArgumentChecks.isNotBlank(nif, "El NIF no puede estar vacío");
        this.nif = nif;
    }

    public List<MechanicDto> execute() throws BusinessException {
        List<MechanicDto> mechanics = new ArrayList<>();
        Optional<MechanicDto> mechanic = new FindByNif(nif).execute();
        mechanic.ifPresent(mechanics::add);
        return mechanics;
    }
}
