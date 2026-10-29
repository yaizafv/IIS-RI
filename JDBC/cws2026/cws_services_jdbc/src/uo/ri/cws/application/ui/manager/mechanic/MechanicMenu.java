package uo.ri.cws.application.ui.manager.mechanic;

import uo.ri.cws.application.ui.manager.mechanic.action.AddMechanicAction;
import uo.ri.cws.application.ui.manager.mechanic.action.DeleteMechanicAction;
import uo.ri.cws.application.ui.manager.mechanic.action.ListAllMechanicsAction;
import uo.ri.cws.application.ui.manager.mechanic.action.ListMechanicAction;
import uo.ri.cws.application.ui.manager.mechanic.action.UpdateMechanicAction;
import uo.ri.util.menu.BaseMenu;

public class MechanicMenu extends BaseMenu {

    public MechanicMenu() {
        menuOptions = new Object[][] { { "Manager > Mechanics management", null },
            { "Add mechanic", AddMechanicAction.class }, { "Update mechanic", UpdateMechanicAction.class },
            { "Delete mechanic", DeleteMechanicAction.class }, { "List mechanic", ListMechanicAction.class },
            { "List mechanics", ListAllMechanicsAction.class }, };
    }

}
