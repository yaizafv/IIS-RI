package uo.ri.cws.application.ui.manager.mechanic.action;

import uo.ri.conf.Factories;
import uo.ri.cws.application.service.mechanic.MechanicCrudService;
import uo.ri.util.console.Console;
import uo.ri.util.exception.BusinessException;
import uo.ri.util.exception.UserInteractionChecks;
import uo.ri.util.exception.UserInteractionException;
import uo.ri.util.menu.Action;

public class DeleteMechanicAction implements Action {

    MechanicCrudService mechanicService = Factories.service.forMechanicCrudService();

    @Override
    public void execute() throws BusinessException, UserInteractionException {
        String idMechanic = Console.readString("Type mechanic id");
        UserInteractionChecks.isNotEmpty(idMechanic, "The id cannot be empty");

        mechanicService.delete(idMechanic);

        Console.println("Mechanic deleted");
    }

}