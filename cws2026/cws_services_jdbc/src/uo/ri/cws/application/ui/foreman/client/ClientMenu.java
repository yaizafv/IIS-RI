package uo.ri.cws.application.ui.foreman.client;

import uo.ri.cws.application.ui.foreman.client.action.AddClientAction;
import uo.ri.cws.application.ui.foreman.client.action.DeleteClientAction;
import uo.ri.cws.application.ui.foreman.client.action.FindClientByNifAction;
import uo.ri.cws.application.ui.foreman.client.action.FindRecommendedClientsAction;
import uo.ri.cws.application.ui.foreman.client.action.ListClientsAction;
import uo.ri.cws.application.ui.foreman.client.action.UpdateClientAction;
import uo.ri.util.menu.BaseMenu;

public class ClientMenu extends BaseMenu {

    public ClientMenu() {
        menuOptions = new Object[][] { { "Foreman > Client management", null },
            { "Register a client", AddClientAction.class }, { "Update a client", UpdateClientAction.class },
            { "Delete a client", DeleteClientAction.class }, { "List all clients", ListClientsAction.class },
            { "View client detail", FindClientByNifAction.class },
            { "List recommended clients", FindRecommendedClientsAction.class }, };
    }

}
