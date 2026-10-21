package uo.ri.cws.application.service.client.commands;

import uo.ri.cws.application.persistence.util.command.Command;
import uo.ri.cws.application.service.client.ClientCrudService.ClientDto;
import uo.ri.util.exception.BusinessException;

public class AddClient implements Command<ClientDto> {

    private final ClientDto client;
    private final String recommenderNif;

    public AddClient(ClientDto client, String recommenderNif) {
        this.client = client;
        this.recommenderNif = recommenderNif;
    }

    @Override
    public ClientDto execute() throws BusinessException {
        return ClientCommandSupport.add(client, recommenderNif);
    }
}
