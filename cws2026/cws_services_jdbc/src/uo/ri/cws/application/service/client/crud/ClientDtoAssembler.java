package uo.ri.cws.application.service.client.crud;

import uo.ri.cws.application.persistence.client.ClientGateway.ClientRecord;
import uo.ri.cws.application.service.client.ClientCrudService.ClientDto;

public final class ClientDtoAssembler {

    private ClientDtoAssembler() {
    }

    public static ClientRecord toRecord(ClientDto dto) {
        ClientRecord record = new ClientRecord();
        record.id = dto.id == null
                ? java.util.UUID.randomUUID().toString() : dto.id;
        record.version = dto.version == 0 ? 1 : dto.version;
        record.nif = dto.nif;
        record.name = dto.name;
        record.surname = dto.surname;
        record.street = dto.addressStreet;
        record.city = dto.addressCity;
        record.country = dto.addressCountry;
        record.postalCode = dto.addressZipcode;
        record.phone = dto.phone;
        record.email = dto.email;
        record.entityState = "ENABLED";
        record.createdAt = java.time.LocalDateTime.now();
        record.updatedAt = record.createdAt;
        return record;
    }

    public static ClientDto toDto(ClientRecord record) {
        ClientDto dto = new ClientDto();
        dto.id = record.id;
        dto.version = record.version;
        dto.nif = record.nif;
        dto.name = record.name;
        dto.surname = record.surname;
        dto.addressStreet = record.street;
        dto.addressCity = record.city;
        dto.addressCountry = record.country;
        dto.addressZipcode = record.postalCode;
        dto.phone = record.phone;
        dto.email = record.email;
        return dto;
    }
}
