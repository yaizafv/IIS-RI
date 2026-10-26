package uo.ri.cws.application.ui.manager.mechanic.action;

import java.util.List;

import uo.ri.conf.Factories;
import uo.ri.cws.application.service.mechanic.MechanicCrudService;
import uo.ri.cws.application.service.mechanic.MechanicCrudService.MechanicDto;
import uo.ri.cws.application.ui.util.Printer;
import uo.ri.util.console.Console;
import uo.ri.util.exception.BusinessException;
import uo.ri.util.menu.Action;

public class ListAllMechanicsAction implements Action {

    MechanicCrudService mechanicService = Factories.service.forMechanicCrudService();

    @Override
    public void execute() throws BusinessException {
        List<MechanicDto> mechanics = mechanicService.findAll();

        Console.println("\nList of mechanics \n");
        for (MechanicDto dto : mechanics) {
            Printer.printMechanic(dto);
        }
    }
}