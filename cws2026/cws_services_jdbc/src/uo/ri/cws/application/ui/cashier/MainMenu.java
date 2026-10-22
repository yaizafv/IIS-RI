package uo.ri.cws.application.ui.cashier;

import uo.ri.cws.application.ui.cashier.action.FindNotInvoicedWorkOrdersByClientAction;
import uo.ri.cws.application.ui.cashier.action.InvoiceWorkorderAction;
import uo.ri.util.menu.BaseMenu;

public class MainMenu extends BaseMenu {

    public MainMenu() {

        menuOptions = new Object[][] { { "Cash", null },
                { "Find not invoiced work orders by client",
                        FindNotInvoicedWorkOrdersByClientAction.class },
                { "Invoice work orders", InvoiceWorkorderAction.class }, };

    }

    public static void main(String[] args) {
        new MainMenu().execute();
    }

}
