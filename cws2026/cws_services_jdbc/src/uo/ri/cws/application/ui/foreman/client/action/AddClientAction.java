package uo.ri.cws.application.ui.foreman.client.action;

import uo.ri.conf.Factories;
import uo.ri.cws.application.service.client.ClientCrudService.ClientDto;
import uo.ri.util.console.Console;
import uo.ri.util.exception.BusinessException;
import uo.ri.util.menu.Action;

public class AddClientAction implements Action {

    @Override
    public void execute() throws BusinessException {
        ClientDto client = new ClientDto();
        client.nif = Console.readString("Nif");
        client.name = Console.readString("Name");
        client.surname = Console.readString("Surname");
        client.addressStreet = Console.readString("Street");
        client.addressCity = Console.readString("City");
        client.addressCountry = Console.readString("Country");
        client.addressZipcode = Console.readString("Zip code");
        client.phone = Console.readString("Phone");
        client.email = Console.readString("Email");
        String recommender = Console.readString("Recommender nif (empty if none)");

        client = Factories.service.forClientCrudService().addClient(client, recommender.isBlank() ? null : recommender);
        Console.println("New client added: " + client.id);
    }
}
