package uo.ri.cws.application.ui.util;

import uo.ri.cws.application.service.client.ClientCrudService.ClientDto;
import uo.ri.cws.application.service.invoice.InvoicingService.InvoiceDto;
import uo.ri.cws.application.service.invoice.InvoicingService.InvoicingWorkOrderDto;
import uo.ri.cws.application.service.mechanic.MechanicCrudService.MechanicDto;
import uo.ri.util.console.Console;

public class Printer {

    public static void printClient(ClientDto client) {
        Console.printf(
            "    %s %-10.10s %-15.15s %-25.25s %-20.20s %-15.15s %-15.15s %-10.10s %-15.15s %-25.25s\n",
            client.id, client.nif, client.name, client.surname, client.addressStreet,
            client.addressCity, client.addressCountry, client.addressZipcode, client.phone,
            client.email);
    }

    public static void printInvoice(InvoiceDto invoice) {
        Console.printf("Invoice #: %d\n", invoice.number);
        Console.printf("    Date: %1$td/%1$tm/%1$tY\n", invoice.date);
        Console.printf("    Total: %.2f €\n", invoice.subtotal);
        Console.printf("    Tax amount: %s €\n", invoice.vatAmount);
        Console.printf("    Total, tax inc.: %s €\n", invoice.total);
        Console.printf("    State: %s\n", invoice.state);
    }

    public static void printWorkOrder(InvoicingWorkOrderDto rep) {
        Console.printf("    %s %-40.40s %td/%<tm/%<tY %-12.12s %.2f\n", rep.id, rep.description,
            rep.date, rep.state, rep.amount);
    }

    public static void printMechanic(MechanicDto m) {
        Console.printf("    %s %-10.10s %-15.15s %-25.25s\n", m.id, m.nif, m.name, m.surname);
    }

}
