package uo.ri.cws.application.ui.cashier.paymentmean.action;

import java.util.Optional;

import uo.ri.conf.Factories;
import uo.ri.cws.application.service.client.ClientCrudService;
import uo.ri.cws.application.service.client.ClientCrudService.ClientDto;
import uo.ri.util.console.Console;
import uo.ri.util.exception.BusinessException;

class PaymentMeanActionSupport {

    static Optional<ClientDto> findClientByNif() throws BusinessException {
        String nif = Console.readString("Client nif");
        ClientCrudService service = Factories.service.forClientCrudService();
        return service.findClientByNif(nif);
    }
}
