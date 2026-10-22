package uo.ri.cws.application.service.client.commands;

import java.time.LocalDateTime;

import uo.ri.conf.Factories;
import uo.ri.cws.application.persistence.client.ClientGateway;
import uo.ri.cws.application.persistence.client.ClientGateway.ClientRecord;
import uo.ri.cws.application.persistence.util.command.Command;
import uo.ri.cws.application.service.client.ClientCrudService.ClientDto;
import uo.ri.cws.application.service.client.crud.ClientDtoAssembler;
import uo.ri.util.assertion.ArgumentChecks;
import uo.ri.util.assertion.BusinessChecks;
import uo.ri.util.exception.BusinessException;

public class UpdateClient implements Command<Void> {

    private final ClientDto client;
    private final ClientGateway clients = Factories.persistence.forClient();

    public UpdateClient(ClientDto client) {
        ArgumentChecks.isNotNull(client, "Los datos del cliente no pueden ser nulos");
        this.client = client;
    }

    @Override
    public Void execute() throws BusinessException {
        validate(client);
        var current = clients.findById(client.id);
        BusinessChecks.exists(current, "No existe el cliente");
        BusinessChecks.doesNotExist(clients.findOtherByNif(client.nif, client.id),
            "Ya existe otro cliente con ese NIF");
        if (current.get().version != client.version) {
            throw new BusinessException("El cliente ha sido modificado por otra operación");
        }
        ClientRecord record = ClientDtoAssembler.toRecord(client);
        record.createdAt = current.get().createdAt;
        record.updatedAt = LocalDateTime.now();
        clients.update(record);
        return null;
    }

    private static void validate(ClientDto dto) {
        ArgumentChecks.isNotBlank(dto.id, "El identificador del cliente no puede estar vacío");
        ArgumentChecks.isNotBlank(dto.nif, "El NIF no puede estar vacío");
        ArgumentChecks.isNotBlank(dto.name, "El nombre no puede estar vacío");
        ArgumentChecks.isNotBlank(dto.surname, "Los apellidos no pueden estar vacíos");
        ArgumentChecks.isNotBlank(dto.addressStreet, "La calle no puede estar vacía");
        ArgumentChecks.isNotBlank(dto.addressCity, "La ciudad no puede estar vacía");
        ArgumentChecks.isNotBlank(dto.addressCountry, "El país no puede estar vacío");
        ArgumentChecks.isNotBlank(dto.addressZipcode, "El código postal no puede estar vacío");
        ArgumentChecks.isNotBlank(dto.phone, "El teléfono no puede estar vacío");
        ArgumentChecks.isNotBlank(dto.email, "El correo electrónico no puede estar vacío");
    }
}
