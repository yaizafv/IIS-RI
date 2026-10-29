package uo.ri.cws.application.ui.mechanic;

import uo.ri.util.menu.BaseMenu;
import uo.ri.util.menu.NotYetImplementedAction;

public class MainMenu extends BaseMenu {

    public MainMenu() {
        menuOptions = new Object[][] { { "Mechanic", null },
            { "List assigned work orders", NotYetImplementedAction.class },
            { "Add parts to work order", NotYetImplementedAction.class },
            { "Remove parts from work order", NotYetImplementedAction.class },
            { "Close a work order", NotYetImplementedAction.class }, };
    }

    public static void main(String[] args) {
        new MainMenu().execute();
    }

}
