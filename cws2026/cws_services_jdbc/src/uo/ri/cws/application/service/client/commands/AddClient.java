package uo.ri.cws.application.service.client.commands;

import uo.ri.cws.application.persistence.util.command.Command;
import uo.ri.conf.Factories;
import uo.ri.cws.application.persistence.client.ClientGateway;
import uo.ri.cws.application.persistence.client.ClientGateway.ClientRecord;
import uo.ri.cws.application.persistence.paymentmean.PaymentMeanGateway;
import uo.ri.cws.application.persistence.paymentmean.PaymentMeanGateway.PaymentMeanRecord;
import uo.ri.cws.application.persistence.cash.CashGateway;
import uo.ri.cws.application.service.client.ClientCrudService.ClientDto;
import uo.ri.cws.application.service.client.crud.ClientDtoAssembler;
import uo.ri.cws.application.persistence.recommendation.RecommendationGateway;
import uo.ri.util.assertion.ArgumentChecks;
import uo.ri.util.assertion.BusinessChecks;
import uo.ri.util.exception.BusinessException;

public class AddClient implements Command<ClientDto> {

    private final ClientDto client;
    private final String recommenderNif;
    private final ClientGateway clients = Factories.persistence.forClient();
    private final PaymentMeanGateway paymentMeans =
            Factories.persistence.forPaymentMean();
    private final CashGateway cash = Factories.persistence.forCash();
    private final RecommendationGateway recommendations =
            Factories.persistence.forRecommendation();

    public AddClient(ClientDto client, String recommenderNif) {
        ArgumentChecks.isNotNull(client, "Los datos del cliente no pueden ser nulos");
        this.client = client;
        this.recommenderNif = recommenderNif;
    }

    @Override
    public ClientDto execute() throws BusinessException {
        validate(client);
        if (recommenderNif != null) {
            ArgumentChecks.isNotBlank(recommenderNif,
                    "El NIF del recomendador no puede estar vacío");
        }
        BusinessChecks.doesNotExist(clients.findByNif(client.nif),
                "Ya existe un cliente con ese NIF");
        String recommenderId = null;
        if (recommenderNif != null) {
            if (client.nif.equals(recommenderNif)) {
                throw new BusinessException(
                        "Un cliente no puede recomendarse a sí mismo");
            }
            var recommender = clients.findByNif(recommenderNif);
            if (recommender.isEmpty()) {
                throw new BusinessException("No existe el cliente recomendador");
            }
            recommenderId = recommender.get().id;
        }

        ClientRecord record = ClientDtoAssembler.toRecord(client);
        clients.add(record);
        PaymentMeanRecord cashRecord = new PaymentMeanRecord();
        cashRecord.id = java.util.UUID.randomUUID().toString();
        cashRecord.dtype = "Cash";
        cashRecord.clientId = record.id;
        cashRecord.accumulated = java.math.BigDecimal.ZERO;
        cashRecord.createdAt = record.createdAt;
        cashRecord.updatedAt = record.updatedAt;
        paymentMeans.add(cashRecord);
        CashGateway.CashRecord child = new CashGateway.CashRecord();
        child.id = cashRecord.id;
        cash.add(child);
        if (recommenderId != null) {
            recommendations.add(recommenderId, record.id);
        }
        client.id = record.id;
        client.version = record.version;
        return client;
    }

    private static void validate(ClientDto dto) {
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
