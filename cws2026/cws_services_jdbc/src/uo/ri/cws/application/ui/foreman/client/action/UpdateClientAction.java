package uo.ri.cws.application.ui.foreman.client.action;

import java.util.Optional;

import uo.ri.conf.Factories;
import uo.ri.cws.application.service.client.ClientCrudService;
import uo.ri.cws.application.service.client.ClientCrudService.ClientDto;
import uo.ri.cws.application.ui.util.Printer;
import uo.ri.util.console.Console;
import uo.ri.util.exception.BusinessException;
import uo.ri.util.menu.Action;

public class UpdateClientAction implements Action {

    @Override
    public void execute() throws BusinessException {
        ClientCrudService service = Factories.service.forClientCrudService();
        Optional<ClientDto> result = service.findClientByNif(Console.readString("Client nif"));
        if (result.isEmpty()) {
            throw new BusinessException("There is no client with that nif");
        }

        ClientDto client = result.get();
        Printer.printClient(client);
        client.nif = Console.readString("Nif", client.nif);
        client.name = Console.readString("Name", client.name);
        client.surname = Console.readString("Surname", client.surname);
        client.addressStreet = Console.readString("Street", client.addressStreet);
        client.addressCity = Console.readString("City", client.addressCity);
        client.addressCountry = Console.readString("Country", client.addressCountry);
        client.addressZipcode = Console.readString("Zip code", client.addressZipcode);
        client.phone = Console.readString("Phone", client.phone);
        client.email = Console.readString("Email", client.email);
        service.updateClient(client);
        Console.println("The client has been updated");
    }
}
