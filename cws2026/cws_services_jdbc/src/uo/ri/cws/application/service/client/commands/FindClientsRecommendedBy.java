package uo.ri.cws.application.service.client.commands;

import java.util.List;

import uo.ri.cws.application.persistence.util.command.Command;
import uo.ri.cws.application.service.client.ClientCrudService.ClientDto;
import uo.ri.util.exception.BusinessException;

public class FindClientsRecommendedBy
        implements Command<List<ClientDto>> {

    private final String recommenderNif;

    public FindClientsRecommendedBy(String recommenderNif) {
        this.recommenderNif = recommenderNif;
    }

    @Override
    public List<ClientDto> execute() throws BusinessException {
        return ClientCommandSupport.findRecommendedBy(recommenderNif);
    }
}
