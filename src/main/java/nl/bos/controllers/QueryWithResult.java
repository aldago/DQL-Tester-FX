package nl.bos.controllers;

import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfAttr;
import javafx.beans.InvalidationListener;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import nl.bos.AttributeTableColumn;
import nl.bos.Repository;
import nl.bos.beans.HistoryItem;
import nl.bos.contextmenu.ContextMenuOnResultTable;
import nl.bos.utils.Calculations;
import nl.bos.utils.Controllers;
import nl.bos.utils.Resources;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static nl.bos.Constants.HISTORY_JSON;
import static nl.bos.Constants.QUERIES;

public class QueryWithResult {
    private static final Logger LOGGER = Logger.getLogger(QueryWithResult.class.getName());

    private ContextMenuOnResultTable contextMenuOnResultTable;

    private JSONObject jsonObject;

    private FXMLLoader connectionWithStatusFxmlLoader;
    private String[] parsedDescription;

    @FXML
    private ComboBox<HistoryItem> historyStatements;
    @FXML
    private ComboBox<HistoryItem> favoriteStatements;
    @FXML
    private VBox queryWithResultBox;
    @FXML
    private TextArea statement;
    @FXML
    private TableView result;
    @FXML
    private ImageView btnDeleteHistoryItem;
    @FXML
    private ImageView btnSaveHistoryItem;
    @FXML
    private ImageView btnDeleteFavoriteItem;

    private Instant start;

    ComboBox<HistoryItem> getHistoryStatements() {
        return historyStatements;
    }

    TextArea getStatement() {
        return statement;
    }

    JSONObject getJsonObject() {
        return jsonObject;
    }

    FXMLLoader getConnectionWithStatusFxmlLoader() {
        return connectionWithStatusFxmlLoader;
    }

    public Instant getStart() {
        return start;
    }

    public TableView getResult() {
        return result;
    }

    public void cleanStart() {
        this.start = null;
    }

    @FXML
    private void initialize() {
        Controllers.put(this.getClass().getSimpleName(), this);

        Tooltip.install(btnDeleteHistoryItem, new Tooltip("Delete from history"));
        Tooltip.install(btnSaveHistoryItem, new Tooltip("Save to favorites"));
        Tooltip.install(btnDeleteFavoriteItem, new Tooltip("Delete from favorites"));

        contextMenuOnResultTable = new ContextMenuOnResultTable(result);
        result.getSelectionModel().setCellSelectionEnabled(true);
        result.addEventHandler(MouseEvent.MOUSE_CLICKED, contextMenuOnResultTable::onRightMouseClick);
        result.getSortOrder().addListener((InvalidationListener) change -> {
            start = Instant.now();
            LOGGER.info("Start..." + Instant.now());
        });
        loadConnectionWithStatusFxml();

        if (historyFileReady()) {
            reloadHistory();
        }

        historyStatements.getSelectionModel().selectedIndexProperty().addListener((observableValue, oldValue, newValue) -> onStatementsSelection(newValue, historyStatements));
        favoriteStatements.getSelectionModel().selectedIndexProperty().addListener((observableValue, oldValue, newValue) -> onStatementsSelection(newValue, favoriteStatements));
    }

    private boolean historyFileReady() {
        File historyFile = new File(HISTORY_JSON);
        if (historyFile.exists()) {
            return true;
        } else {
            return isHistoryFileCreated();
        }
    }

    private void onStatementsSelection(Number newValue, ComboBox<HistoryItem> statements) {
        if (newValue.intValue() != -1) {
            String selectedItem = String.valueOf(statements.getItems().get((Integer) newValue));
            LOGGER.info(selectedItem);
            statement.setText(selectedItem);
        } else
            statement.setText("");
    }

    private void loadConnectionWithStatusFxml() {
        try {
            connectionWithStatusFxmlLoader = new FXMLLoader(getClass().getResource("/nl/bos/views/ConnectionWithStatus.fxml"));
            BorderPane connectionWithStatus = connectionWithStatusFxmlLoader.load();
            queryWithResultBox.getChildren().add(connectionWithStatus);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    private void reloadHistory() {
        String history = convertFileToString();
        List<HistoryItem> statements = makeListFrom(history);
        setHistoryItems(statements);
        setFavoriteItems(statements);
        addToolTipToItems(historyStatements);
        addToolTipToItems(favoriteStatements);
    }

    private String convertFileToString() {
        String history = null;
        try {
            history = new String(Files.readAllBytes(Paths.get(HISTORY_JSON)), StandardCharsets.UTF_8);
            jsonObject = new JSONObject(history);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
        return history;
    }

    private List<HistoryItem> makeListFrom(String history) {
        JSONArray historyQueries = (JSONArray) new JSONObject(history).get(QUERIES);

        List<HistoryItem> statements = new ArrayList<>();
        for (int i = 0; i < historyQueries.length(); i++) {
            statements.add(histroryItemFromJsonObject(historyQueries.getJSONObject(i)));
        }

        return statements;
    }

    private void setHistoryItems(List<HistoryItem> statements) {
        ObservableList<HistoryItem> value = FXCollections.observableList(statements);
        historyStatements.setItems(value);
    }

    private void setFavoriteItems(List<HistoryItem> statements) {
        List<HistoryItem> result = statements.stream()
                .filter(HistoryItem::isFavorite)
                .collect(Collectors.toList());
        ObservableList<HistoryItem> value = FXCollections.observableList(result);
        favoriteStatements.setItems(value);
    }


    private void addToolTipToItems(ComboBox<HistoryItem> statements) {
        Callback<ListView<HistoryItem>, ListCell<HistoryItem>> factory = lv -> new ListCell<>() {
            @Override
            protected void updateItem(HistoryItem item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null) {
                    setText(item.getQuery().substring(0, Math.min(item.getQuery().length(), 100)).replaceAll("\n", " "));
                    setTooltip(new Tooltip(item.getQuery()));
                }
            }
        };
        statements.setCellFactory(factory);
    }

    private HistoryItem histroryItemFromJsonObject(JSONObject jsonObject) {
        String query = jsonObject.getString("query");
        String category = jsonObject.getString("category");
        boolean isFavorite = jsonObject.getBoolean("favorite");

        HistoryItem historyItem = new HistoryItem(query);
        historyItem.setCategory(category);
        historyItem.setFavorite(isFavorite);

        return historyItem;
    }

    private boolean isHistoryFileCreated() {
        JSONArray list = new JSONArray();
        jsonObject = new JSONObject();
        jsonObject.put(QUERIES, list);

        return Resources.writeJsonDataToJsonHistoryFile(jsonObject);
    }

    @FXML
    private void handleDeleteHistoryItem() {
        HistoryItem selectedItem = historyStatements.getSelectionModel().getSelectedItem();

        if (historyStatements.getItems().remove(selectedItem)) {
            List<HistoryItem> historyItems = makeListFrom(jsonObject.toString());
            int selectedIndex = 0;
            for (HistoryItem historyItem : historyItems) {
                if (historyItem.getQuery().equals(selectedItem.getQuery())) {
                    break;
                }
                selectedIndex++;
            }

            JSONArray queries = (JSONArray) jsonObject.get(QUERIES);
            queries.remove(selectedIndex);
            if (queries.length() > 0) {
                historyStatements.setValue(historyStatements.getItems().get(0));
            }

            if (Resources.writeJsonDataToJsonHistoryFile(jsonObject))
                reloadHistory();
        }
    }

    @FXML
    private void handleSaveHistoryItem() {
        HistoryItem selectedItem = historyStatements.getSelectionModel().getSelectedItem();

        if (selectedItem != null) {
            selectedItem.setFavorite(true);

            updateJSONData(selectedItem);
            if (Resources.writeJsonDataToJsonHistoryFile(jsonObject))
                reloadHistory();
        }
    }

    @FXML
    private void handleDeleteFavoriteItem() {
        HistoryItem selectedItem = favoriteStatements.getSelectionModel().getSelectedItem();

        if (selectedItem != null) {
            selectedItem.setFavorite(false);

            updateJSONData(selectedItem);
            if (Resources.writeJsonDataToJsonHistoryFile(jsonObject))
                reloadHistory();
        }
    }

    private void updateJSONData(HistoryItem selectedItem) {
        List<HistoryItem> historyItems = makeListFrom(jsonObject.toString());
        int selectedIndex = 0;
        for (HistoryItem historyItem : historyItems) {
            if (historyItem.getQuery().equals(selectedItem.getQuery())) {
                break;
            }
            selectedIndex++;
        }

        JSONArray queries = (JSONArray) jsonObject.get("queries");
        queries.put(selectedIndex, new JSONObject(selectedItem));
    }

    /**
     * @noinspection unchecked
     */
    int updateResultTable(IDfCollection collection) throws DfException {
        result.getItems().clear();
        result.getColumns().clear();

        List<AttributeTableColumn> columns = new ArrayList<>();
        ObservableList<ObservableList> rows = FXCollections.observableArrayList();

        int rowCount = 0;
        while (collection.next()) {
            rowCount++;
            ObservableList<String> row = FXCollections.observableArrayList();
            for (int i = 0; i < collection.getAttrCount(); i++) {
                IDfAttr attr = collection.getAttr(i);

                if (rowCount == 1) {
                    final int j = i;
                    AttributeTableColumn column = new AttributeTableColumn(attr.getName());
                    column.setAttr(attr);
                    column.setCellValueFactory((Callback<AttributeTableColumn.CellDataFeatures<ObservableList, String>, ObservableValue<String>>) param -> new SimpleStringProperty(param.getValue().get(j).toString()));
                    columns.add(column);
                }

                switch (attr.getDataType()) {
                    case IDfAttr.DM_BOOLEAN:
                        row.add(String.valueOf(collection.getBoolean(attr.getName())));
                        break;
                    case IDfAttr.DM_DOUBLE:
                        row.add(String.valueOf(collection.getDouble(attr.getName())));
                        break;
                    case IDfAttr.DM_ID:
                        row.add(String.valueOf(collection.getId(attr.getName())));
                        break;
                    case IDfAttr.DM_INTEGER:
                        row.add(String.valueOf(collection.getInt(attr.getName())));
                        break;
                    case IDfAttr.DM_STRING:
                        row.add(String.valueOf(collection.getString(attr.getName())));
                        break;
                    case IDfAttr.DM_TIME:
                        row.add(String.valueOf(collection.getTime(attr.getName())));
                        break;
                    default:
                        LOGGER.finest("Error occurred while displaying the results.");
                        row.add("N/A");
                        break;
                }
            }
            rows.add(row);
        }
        result.getColumns().addAll(columns);
        result.setItems(rows);

        return rowCount;
    }

    /**
     * @noinspection unchecked
     */
    public int updateResultTableWithStringInput(String description, List<String> columnNames) {
        contextMenuOnResultTable.getMenuItemShowPropertiesAction().setDescription(description);

        result.getItems().clear();
        result.getColumns().clear();

        List<AttributeTableColumn> columns = new ArrayList<>();
        ObservableList<ObservableList> rows = FXCollections.observableArrayList();

        int rowCount = 0;

        while (rowCount < getRowSize(description)) {
            rowCount++;
            ObservableList<String> row = FXCollections.observableArrayList();
            for (int i = 0; i < columnNames.size(); i++) {

                if (rowCount == 1) {
                    final int j = i;
                    AttributeTableColumn column = new AttributeTableColumn(columnNames.get(j));
                    column.setCellValueFactory((Callback<AttributeTableColumn.CellDataFeatures<ObservableList, String>, ObservableValue<String>>) param -> new SimpleStringProperty(param.getValue().get(j).toString()));
                    columns.add(column);
                }

                row.add(getRowValue(rowCount - 1, i));
            }
            rows.add(row);
        }
        result.getColumns().addAll(columns);
        result.setItems(rows);

        return rowCount;
    }

    private int getRowSize(String description) {
        String fromColumns;
        String type = description.substring(0, description.indexOf('\t'));

        if (type.contains("Table")) {
            fromColumns = description.substring(description.indexOf("Columns:"));
        } else {
            fromColumns = description.substring(description.indexOf("Attributes:"));
        }

        String columnsInfo = fromColumns.substring(0, fromColumns.indexOf("\r\n"));
        String value = columnsInfo.substring(columnsInfo.indexOf(':') + 1).trim();

        parseDescription(description);

        return Integer.parseInt(value);
    }

    private String getRowValue(int rowIndex, int columnIndex) {
        String rowValue = "";

        try {
            rowValue = parsedDescription[(rowIndex * 3) + columnIndex];

        } catch (ArrayIndexOutOfBoundsException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }

        return rowValue;
    }

    private void parseDescription(String descriptionInput) {
        String[] split;
        String type = descriptionInput.substring(0, descriptionInput.indexOf('\t'));

        if (type.contains("Table")) {
            split = descriptionInput.substring(descriptionInput.indexOf("\r\n", descriptionInput.indexOf("Columns:")))
                    .replace("KEYED\r\n", " KEYED ")
                    .replace("\r\n", " NOT_KEYED ")
                    .replace(" NOT_KEYED", " FALSE")
                    .replace("KEYED", " TRUE")
                    .split(" ");
        } else {
            split = descriptionInput.substring(descriptionInput.indexOf("\r\n", descriptionInput.indexOf("Attributes:"))).replace("REPEATING\r\n", " REPEATING ").replace("\r\n", " NOT_REPEATING ").replace(" NOT_REPEATING", " FALSE").replace("REPEATING", " TRUE").split(" ");
        }

        split[0] = "";
        split[1] = "";
        split[2] = "";
        split[3] = "";
        parsedDescription = Arrays.stream(split).filter(value -> !value.equals("")).toArray(String[]::new);
    }

    public void executeQuery(String query) {
        ConnectionWithStatus connectionWithStatusController = (ConnectionWithStatus) Controllers.get(ConnectionWithStatus.class.getSimpleName());

        Instant start = Instant.now();
        IDfCollection collection = Repository.getInstance().query(query);
        Instant end = Instant.now();
        connectionWithStatusController.getTimeQuery().setText(Calculations.getDurationInSeconds(start, end));

        if (collection == null) {
            return;
        }

        try {
            Instant startList = Instant.now();
            int rowCount = updateResultTable(collection);
            Instant endList = Instant.now();
            connectionWithStatusController.getTimeList().setText(Calculations.getDurationInSeconds(startList, endList));
            connectionWithStatusController.getResultCount().setText(String.valueOf(rowCount));

            collection.close();
        } catch (DfException e) {
            LOGGER.log(Level.SEVERE, String.format("Error running query: [%s]", query), e);
        }
    }
}
