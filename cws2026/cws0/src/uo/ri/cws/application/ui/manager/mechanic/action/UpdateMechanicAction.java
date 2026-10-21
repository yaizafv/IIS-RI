package uo.ri.cws.application.ui.manager.mechanic.action;

import java.util.Optional;

import uo.ri.conf.Factories;
import uo.ri.cws.application.service.mechanic.MechanicCrudService;
import uo.ri.cws.application.service.mechanic.MechanicCrudService.MechanicDto;
import uo.ri.util.console.Console;
import uo.ri.util.exception.BusinessException;
import uo.ri.util.menu.Action;

public class UpdateMechanicAction implements Action {

    MechanicCrudService mechanicService = Factories.service.forMechanicCrudService();

    @Override
    public void execute() throws BusinessException {
        String id = Console.readString("Type mechanic id to update");

        // Comprobar si existe usando findById del servicio
        Optional<MechanicDto> opt = mechanicService.findById(id);
        if (!opt.isPresent()) {
            Console.println("Mechanic does not exist");
            return;
        }

        // Pedir los nuevos datos
        String name = Console.readString("Name");
        String surname = Console.readString("Surname");

        MechanicDto dto = opt.get();
        dto.name = name;
        dto.surname = surname;

        // Actualizar mediante el servicio
        mechanicService.update(dto);

        Console.println("Mechanic updated");
    }
}