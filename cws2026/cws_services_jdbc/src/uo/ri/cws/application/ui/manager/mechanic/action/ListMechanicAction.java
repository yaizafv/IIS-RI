package uo.ri.cws.application.ui.manager.mechanic.action;

import java.util.Optional;

import uo.ri.conf.Factories;
import uo.ri.cws.application.service.mechanic.MechanicCrudService;
import uo.ri.cws.application.service.mechanic.MechanicCrudService.MechanicDto;
import uo.ri.util.console.Console;
import uo.ri.util.exception.BusinessException;
import uo.ri.util.menu.Action;

public class ListMechanicAction implements Action {

    MechanicCrudService mechanicService = Factories.service.forMechanicCrudService();

    @Override
    public void execute() throws BusinessException {
        String nif = Console.readString("nif");

        Optional<MechanicDto> opt = mechanicService.findByNif(nif);

        if (opt.isPresent()) {
            MechanicDto dto = opt.get();
            Console.println("\nMechanic information \n");
            Console.printf("\t%s %s %s %s %d\n", dto.id, dto.name, dto.surname, dto.nif, dto.version);
        } else {
            Console.println("Mechanic not found");
        }
    }
}