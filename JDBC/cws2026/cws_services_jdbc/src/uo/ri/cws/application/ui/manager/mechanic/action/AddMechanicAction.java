package uo.ri.cws.application.ui.manager.mechanic.action;

import uo.ri.conf.Factories;
import uo.ri.cws.application.service.mechanic.MechanicCrudService;
import uo.ri.cws.application.service.mechanic.MechanicCrudService.MechanicDto;
import uo.ri.util.console.Console;
import uo.ri.util.exception.BusinessException;
import uo.ri.util.menu.Action;

public class AddMechanicAction implements Action {

    MechanicCrudService mechanicService = Factories.service.forMechanicCrudService();

    @Override
    public void execute() throws BusinessException {
        // Get info
        String nif = Console.readString("nif");
        String name = Console.readString("Name");
        String surname = Console.readString("Surname");

        MechanicDto dto = new MechanicDto();
        dto.nif = nif;
        dto.name = name;
        dto.surname = surname;

        mechanicService.create(dto);

        // Print result
        Console.println("Mechanic added");
    }

}
