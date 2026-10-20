package uo.ri.cws.application.ui.manager.mechanic.action;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.UUID;

import uo.ri.util.console.Console;
import uo.ri.util.exception.BusinessException;
import uo.ri.util.jdbc.Jdbc;
import uo.ri.util.menu.Action;

public class AddMechanicAction implements Action {
    private static final String TMECHANICS_ADD = "insert into TMechanics"
            + "(id, nif, name, surname, version, "
            + "createdAt, updatedAt, entityState) "
            + "values (?, ?, ?, ?, ?, ?, ?, ?)";

    @Override
    public void execute() throws BusinessException {

        // Get info
        String nif = Console.readString("nif");
        String name = Console.readString("Name");
        String surname = Console.readString("Surname");
        String id = UUID.randomUUID().toString();
        long version = 1;

        // Process
        try (Connection c = Jdbc.createThreadConnection();) {
            try (PreparedStatement pst = c.prepareStatement(TMECHANICS_ADD)) {
                pst.setString(1, id);
                pst.setString(2, nif);
                pst.setString(3, name);
                pst.setString(4, surname);
                pst.setLong(5, version);
                pst.setTimestamp(6, new Timestamp(System.currentTimeMillis()));
                pst.setTimestamp(7, new Timestamp(System.currentTimeMillis()));
                pst.setString(8, "ENABLED");               

                pst.executeUpdate();

            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        // Print result
        Console.println("Mechanic added");
    }

}
