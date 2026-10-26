package uo.ri.cws.application.ui.manager;

import uo.ri.cws.application.ui.manager.mechanic.MechanicMenu;
import uo.ri.cws.application.ui.manager.voucher.VouchersMenu;
import uo.ri.util.menu.BaseMenu;
import uo.ri.util.menu.NotYetImplementedAction;

public class MainMenu extends BaseMenu {

    public MainMenu() {
        menuOptions = new Object[][] { { "Manager", null },

            { "Mechanics management", MechanicMenu.class }, { "Spare parts management", NotYetImplementedAction.class },
            { "Vehicle types management", NotYetImplementedAction.class },
            { "Voucher management", VouchersMenu.class }, };
    }

    public static void main(String[] args) {
        new MainMenu().execute();
    }

}
