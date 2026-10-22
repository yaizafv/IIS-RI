package uo.ri.cws.application.service.client.commands;

import java.util.ArrayList;
import java.util.List;

import uo.ri.conf.Factories;
import uo.ri.cws.application.persistence.client.ClientGateway;
import uo.ri.cws.application.persistence.util.command.Command;
import uo.ri.cws.application.service.client.ClientCrudService.ClientDto;
import uo.ri.cws.application.service.client.crud.ClientDtoAssembler;
import uo.ri.util.assertion.ArgumentChecks;
import uo.ri.util.exception.BusinessException;

public class FindClientsRecommendedBy implements Command<List<ClientDto>> {

    private final String recommenderNif;
    private final ClientGateway clients = Factories.persistence.forClient();

    public FindClientsRecommendedBy(String recommenderNif) {
        ArgumentChecks.isNotBlank(recommenderNif, "El NIF del recomendador no puede estar vacío");
        this.recommenderNif = recommenderNif;
    }

    @Override
    public List<ClientDto> execute() throws BusinessException {
        List<ClientDto> result = new ArrayList<>();
        for (var record : clients.findRecommendedBy(recommenderNif)) {
            result.add(ClientDtoAssembler.toDto(record));
        }
        return result;
    }
}
