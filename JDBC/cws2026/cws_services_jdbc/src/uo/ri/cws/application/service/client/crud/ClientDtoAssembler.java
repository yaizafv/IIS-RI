package uo.ri.cws.application.service.client.crud;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import uo.ri.cws.application.persistence.cash.CashGateway.CashRecord;
import uo.ri.cws.application.persistence.client.ClientGateway.ClientRecord;
import uo.ri.cws.application.persistence.recommendation.RecommendationGateway.RecommendationRecord;
import uo.ri.cws.application.service.client.ClientCrudService.ClientDto;

public final class ClientDtoAssembler {

    private ClientDtoAssembler() {
    }

    public static ClientRecord toRecord(ClientDto dto) {
        ClientRecord record = new ClientRecord();
        record.id = dto.id == null ? UUID.randomUUID().toString() : dto.id;
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
        record.createdAt = LocalDateTime.now();
        record.updatedAt = record.createdAt;
        return record;
    }

    public static CashRecord toCashRecord(String clientId) {
        CashRecord record = new CashRecord();
        record.id = UUID.randomUUID().toString();
        record.version = 1;
        record.createdAt = LocalDateTime.now();
        record.updatedAt = record.createdAt;
        record.entityState = "ENABLED";
        record.dtype = "Cash";
        record.accumulated = BigDecimal.ZERO;
        record.clientId = clientId;
        return record;
    }

    public static RecommendationRecord toRecommendationRecord(String recommenderId, String recommendedId) {
        RecommendationRecord record = new RecommendationRecord();
        record.id = UUID.randomUUID().toString();
        record.version = 1;
        record.createdAt = LocalDateTime.now();
        record.updatedAt = record.createdAt;
        record.entityState = "ENABLED";
        record.recommenderId = recommenderId;
        record.recommendedId = recommendedId;
        record.usedForVoucher = false;
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
