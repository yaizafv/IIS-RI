package uo.ri.cws.application.ui.foreman.vehicle;

import uo.ri.util.menu.BaseMenu;
import uo.ri.util.menu.NotYetImplementedAction;

public class VehicleMenu extends BaseMenu {

    public VehicleMenu() {
        menuOptions = new Object[][] { { "Foreman > Vehicle management", null },

            { "Register new vehicle", NotYetImplementedAction.class },
            { "Update vehicle", NotYetImplementedAction.class },
            { "Disable vehicle", NotYetImplementedAction.class },
            { "List all vehicles", NotYetImplementedAction.class }, };
    }

}
