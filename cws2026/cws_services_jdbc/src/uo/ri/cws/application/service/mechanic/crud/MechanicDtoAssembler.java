package uo.ri.cws.application.service.mechanic.crud;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import uo.ri.cws.application.persistence.mechanic.MechanicGateway.MechanicRecord;
import uo.ri.cws.application.service.mechanic.MechanicCrudService.MechanicDto;

public class MechanicDtoAssembler {

    public static MechanicRecord toRecord(MechanicDto dto) {
        var mr = new MechanicRecord();
        mr.nif = dto.nif;
        mr.name = dto.name;
        mr.surname = dto.surname;
        mr.id = (dto.id != null) ? dto.id : UUID.randomUUID().toString();
        mr.version = (dto.version != 0) ? dto.version : 1L;
        mr.createdAt = LocalDateTime.now();
        mr.updatedAt = LocalDateTime.now();
        mr.entityState = "ENABLED";

        return mr;
    }

    public static Optional<MechanicDto> toDtoOpt(MechanicRecord record) {
        if (record == null) {
            return Optional.empty();
        }

        MechanicDto res = new MechanicDto();
        res.id = record.id;
        res.version = record.version;
        res.nif = record.nif;
        res.name = record.name;
        res.surname = record.surname;

        return Optional.of(res);
    }

}
