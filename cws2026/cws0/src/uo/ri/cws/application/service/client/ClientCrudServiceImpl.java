package uo.ri.cws.application.service.client;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import uo.ri.cws.application.persistence.util.command.CommandExecutor;
import uo.ri.cws.application.persistence.util.jdbc.Jdbc;
import uo.ri.cws.application.persistence.util.jdbc.Queries;
import uo.ri.cws.application.service.client.ClientCrudService.ClientDto;
import uo.ri.util.assertion.ArgumentChecks;
import uo.ri.util.exception.BusinessException;
import uo.ri.util.exception.PersistenceException;

public class ClientCrudServiceImpl implements ClientCrudService {

    private final CommandExecutor executor = new CommandExecutor();

    @Override
    public ClientDto addClient(ClientDto client, String recommenderNif)
            throws BusinessException {
        validateClient(client);
        if (recommenderNif != null) {
            ArgumentChecks.isNotBlank(recommenderNif, "El NIF del recomendador no puede estar vacío");
        }

        return execute(() -> {
            Connection connection = Jdbc.getCurrentConnection();
            if (exists(connection, Queries.getSQLSentence("TCLIENTS_FIND_BY_NIF"), client.nif)) {
                throw new BusinessException("Ya existe un cliente con ese NIF");
            }
            String recommenderId = null;
            if (recommenderNif != null) {
                if (client.nif.equals(recommenderNif)) {
                    throw new BusinessException("Un cliente no puede recomendarse a sí mismo");
                }
                try (PreparedStatement pst = connection.prepareStatement(
                        Queries.getSQLSentence("TCLIENTS_FIND_BY_NIF"))) {
                    pst.setString(1, recommenderNif);
                    try (ResultSet rs = pst.executeQuery()) {
                        if (!rs.next()) {
                            throw new BusinessException("No existe el cliente recomendador");
                        }
                        recommenderId = rs.getString("id");
                    }
                }
            }

            ClientDto added = copy(client);
            added.id = UUID.randomUUID().toString();
            added.version = 1;
            Timestamp now = Timestamp.valueOf(java.time.LocalDateTime.now());
            try (PreparedStatement pst = connection.prepareStatement(
                    Queries.getSQLSentence("TCLIENTS_ADD"))) {
                bindClient(pst, added, now);
                pst.executeUpdate();
            }

            String paymentMeanId = UUID.randomUUID().toString();
            try (PreparedStatement pst = connection.prepareStatement(
                    Queries.getSQLSentence("TPAYMENTMEANS_ADD_CASH"))) {
                pst.setString(1, paymentMeanId);
                pst.setTimestamp(2, now);
                pst.setTimestamp(3, now);
                pst.setLong(4, 1);
                pst.setString(5, "ENABLED");
                pst.setString(6, added.id);
                pst.executeUpdate();
            }
            try (PreparedStatement pst = connection.prepareStatement(
                    Queries.getSQLSentence("TCASHES_ADD"))) {
                pst.setString(1, paymentMeanId);
                pst.executeUpdate();
            }

            if (recommenderId != null) {
                try (PreparedStatement pst = connection.prepareStatement(
                        Queries.getSQLSentence("TRECOMMENDATIONS_ADD"))) {
                    pst.setString(1, UUID.randomUUID().toString());
                    pst.setTimestamp(2, now);
                    pst.setTimestamp(3, now);
                    pst.setString(4, "ENABLED");
                    pst.setBoolean(5, false);
                    pst.setLong(6, 1);
                    pst.setString(7, recommenderId);
                    pst.setString(8, added.id);
                    pst.executeUpdate();
                }
            }
            return added;
        });
    }

    @Override
    public void deleteClientByNif(String nif) throws BusinessException {
        ArgumentChecks.isNotBlank(nif, "El NIF no puede estar vacío");
        execute(() -> {
            Connection connection = Jdbc.getCurrentConnection();
            String clientId = findClientId(connection, nif);
            if (clientId == null) {
                throw new BusinessException("No existe el cliente");
            }
            if (exists(connection, Queries.getSQLSentence("TVEHICLES_EXISTS_BY_CLIENT"), clientId)) {
                throw new BusinessException("No se puede eliminar un cliente con vehículos");
            }
            if (exists(connection, Queries.getSQLSentence("TCLIENTS_HAS_PAID_INVOICES"), clientId)) {
                throw new BusinessException("No se puede eliminar un cliente con facturas pagadas");
            }

            try (PreparedStatement pst = connection.prepareStatement(
                    Queries.getSQLSentence("TRECOMMENDATIONS_DELETE_FOR_CLIENT"))) {
                pst.setString(1, clientId);
                pst.setString(2, clientId);
                pst.executeUpdate();
            }
            executeSql(connection, "TCHARGES_DELETE_FOR_CLIENT", clientId);
            executeSql(connection, "TCASHES_DELETE_FOR_CLIENT", clientId);
            executeSql(connection, "TCREDITCARDS_DELETE_FOR_CLIENT", clientId);
            executeSql(connection, "TVOUCHERS_DELETE_FOR_CLIENT", clientId);
            executeSql(connection, "TPAYMENTMEANS_DELETE_FOR_CLIENT", clientId);
            try (PreparedStatement pst = connection.prepareStatement(
                    Queries.getSQLSentence("TCLIENTS_DELETE_BY_ID"))) {
                pst.setString(1, clientId);
                pst.executeUpdate();
            }
            return null;
        });
    }

    @Override
    public void updateClient(ClientDto client) throws BusinessException {
        ArgumentChecks.isNotNull(client, "Los datos del cliente no pueden ser nulos");
        ArgumentChecks.isNotBlank(client.id, "El identificador del cliente no puede estar vacío");
        ArgumentChecks.isNotBlank(client.nif, "El NIF no puede estar vacío");
        ArgumentChecks.isNotBlank(client.name, "El nombre no puede estar vacío");
        ArgumentChecks.isNotBlank(client.surname, "Los apellidos no pueden estar vacíos");
        ArgumentChecks.isNotBlank(client.addressStreet, "La calle no puede estar vacía");
        ArgumentChecks.isNotBlank(client.addressCity, "La ciudad no puede estar vacía");
        ArgumentChecks.isNotBlank(client.addressCountry, "El país no puede estar vacío");
        ArgumentChecks.isNotBlank(client.addressZipcode, "El código postal no puede estar vacío");
        ArgumentChecks.isNotBlank(client.phone, "El teléfono no puede estar vacío");
        ArgumentChecks.isNotBlank(client.email, "El correo electrónico no puede estar vacío");

        execute(() -> {
            Connection connection = Jdbc.getCurrentConnection();
            if (!exists(connection, Queries.getSQLSentence("TCLIENTS_EXISTS_BY_ID"), client.id)) {
                throw new BusinessException("No existe el cliente");
            }
            if (exists(connection, Queries.getSQLSentence("TCLIENTS_FIND_OTHER_BY_NIF"),
                    client.nif, client.id)) {
                throw new BusinessException("Ya existe otro cliente con ese NIF");
            }
            try (PreparedStatement pst = connection.prepareStatement(
                    Queries.getSQLSentence("TCLIENTS_UPDATE"))) {
                pst.setString(1, client.name);
                pst.setString(2, client.surname);
                pst.setString(3, client.nif);
                pst.setString(4, client.phone);
                pst.setString(5, client.email);
                pst.setString(6, client.addressStreet);
                pst.setString(7, client.addressCity);
                pst.setString(8, client.addressCountry);
                pst.setString(9, client.addressZipcode);
                pst.setTimestamp(10, Timestamp.valueOf(java.time.LocalDateTime.now()));
                pst.setString(11, client.id);
                pst.setLong(12, client.version);
                if (pst.executeUpdate() != 1) {
                    throw new BusinessException("El cliente ha sido modificado por otra operación");
                }
            }
            return null;
        });
    }

    @Override
    public List<ClientDto> findAllClients() throws BusinessException {
        return execute(() -> {
            List<ClientDto> clients = new ArrayList<>();
            try (PreparedStatement pst = Jdbc.getCurrentConnection().prepareStatement(
                    Queries.getSQLSentence("TCLIENTS_FIND_ALL"));
                 ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    clients.add(toDto(rs));
                }
            }
            return clients;
        });
    }

    @Override
    public Optional<ClientDto> findClientByNif(String nif) throws BusinessException {
        ArgumentChecks.isNotBlank(nif, "El NIF no puede estar vacío");
        return execute(() -> {
            try (PreparedStatement pst = Jdbc.getCurrentConnection().prepareStatement(
                    Queries.getSQLSentence("TCLIENTS_FIND_BY_NIF"))) {
                pst.setString(1, nif);
                try (ResultSet rs = pst.executeQuery()) {
                    return rs.next() ? Optional.of(toDto(rs)) : Optional.empty();
                }
            }
        });
    }

    @Override
    public List<ClientDto> findClientsRecommendedBy(String recommenderNif)
            throws BusinessException {
        ArgumentChecks.isNotBlank(recommenderNif, "El NIF del recomendador no puede estar vacío");
        return execute(() -> {
            List<ClientDto> clients = new ArrayList<>();
            try (PreparedStatement pst = Jdbc.getCurrentConnection().prepareStatement(
                    Queries.getSQLSentence("TCLIENTS_FIND_RECOMMENDED_BY_NIF"))) {
                pst.setString(1, recommenderNif);
                try (ResultSet rs = pst.executeQuery()) {
                    while (rs.next()) {
                        clients.add(toDto(rs));
                    }
                }
            }
            return clients;
        });
    }

    private static void validateClient(ClientDto dto) {
        ArgumentChecks.isNotNull(dto, "Los datos del cliente no pueden ser nulos");
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

    private static ClientDto copy(ClientDto source) {
        ClientDto dto = new ClientDto();
        dto.id = source.id;
        dto.version = source.version;
        dto.nif = source.nif;
        dto.name = source.name;
        dto.surname = source.surname;
        dto.addressStreet = source.addressStreet;
        dto.addressCity = source.addressCity;
        dto.addressCountry = source.addressCountry;
        dto.addressZipcode = source.addressZipcode;
        dto.phone = source.phone;
        dto.email = source.email;
        return dto;
    }

    private static void bindClient(PreparedStatement pst, ClientDto dto, Timestamp now)
            throws SQLException {
        pst.setString(1, dto.id);
        pst.setLong(2, dto.version);
        pst.setTimestamp(3, now);
        pst.setTimestamp(4, now);
        pst.setString(5, "ENABLED");
        pst.setString(6, dto.name);
        pst.setString(7, dto.surname);
        pst.setString(8, dto.nif);
        pst.setString(9, dto.phone);
        pst.setString(10, dto.email);
        pst.setString(11, dto.addressStreet);
        pst.setString(12, dto.addressCity);
        pst.setString(13, dto.addressCountry);
        pst.setString(14, dto.addressZipcode);
    }

    private static ClientDto toDto(ResultSet rs) throws SQLException {
        ClientDto dto = new ClientDto();
        dto.id = rs.getString("id");
        dto.version = rs.getLong("version");
        dto.nif = rs.getString("nif");
        dto.name = rs.getString("name");
        dto.surname = rs.getString("surname");
        dto.addressStreet = rs.getString("street");
        dto.addressCity = rs.getString("city");
        dto.addressCountry = rs.getString("country");
        dto.addressZipcode = rs.getString("postalCode");
        dto.phone = rs.getString("phone");
        dto.email = rs.getString("email");
        return dto;
    }

    private static boolean exists(Connection connection, String sql, String... values)
            throws SQLException {
        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) {
                pst.setString(i + 1, values[i]);
            }
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static String findClientId(Connection connection, String nif) throws SQLException {
        try (PreparedStatement pst = connection.prepareStatement(
                Queries.getSQLSentence("TCLIENTS_FIND_BY_NIF"))) {
            pst.setString(1, nif);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next() ? rs.getString("id") : null;
            }
        }
    }

    private static void executeSql(Connection connection, String key, String clientId)
            throws SQLException {
        try (PreparedStatement pst = connection.prepareStatement(Queries.getSQLSentence(key))) {
            pst.setString(1, clientId);
            pst.executeUpdate();
        }
    }

    private <T> T execute(SqlCommand<T> command) throws BusinessException {
        return executor.execute(() -> {
            try {
                return command.execute();
            } catch (SQLException e) {
                throw new PersistenceException(e);
            }
        });
    }

    @FunctionalInterface
    private interface SqlCommand<T> {
        T execute() throws SQLException, BusinessException;
    }
}
