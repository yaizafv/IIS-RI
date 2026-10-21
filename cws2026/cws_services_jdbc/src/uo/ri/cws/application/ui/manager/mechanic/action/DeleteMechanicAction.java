package uo.ri.cws.application.ui.manager.mechanic.action;

import uo.ri.conf.Factories;
import uo.ri.cws.application.service.mechanic.MechanicCrudService;
import uo.ri.util.console.Console;
import uo.ri.util.exception.BusinessException;
import uo.ri.util.menu.Action;

public class DeleteMechanicAction implements Action {

    MechanicCrudService mechanicService = Factories.service.forMechanicCrudService();

    @Override
    public void execute() throws BusinessException {
        // Pedir el ID del mecánico por consola
        String idMechanic = Console.readString("Type mechanic id");

        // Llamar al servicio para borrar
        mechanicService.delete(idMechanic);

        // Mensaje de éxito
        Console.println("Mechanic deleted");
    }

}