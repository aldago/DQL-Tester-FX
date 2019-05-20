package nl.bos.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import nl.bos.Repository;
import nl.bos.contextmenu.menuitem.action.MenuItemExportToCsvAction;
import nl.bos.menu.menuitem.action.*;
import nl.bos.utils.Controllers;

import java.io.IOException;

public class Menu {
    @FXML
    private MenuBar menubar;
    private final Repository repository = Repository.getInstance();

    private final DescribeObjectAction describeObjectAction;
    @FXML
    private MenuItem miExportResults;

    public Menu() {
        Controllers.put(this.getClass().getSimpleName(), this);

        describeObjectAction = new DescribeObjectAction();
    }

    @FXML
    private void initialize() {
        if (repository.isConnected()) {
            menubar.setDisable(false);
        }
    }

    MenuItem getMiExportResults() {
        return miExportResults;
    }

    MenuBar getMenubar() {
        return menubar;
    }

    @FXML
    private void browseRepository() {
        new BrowseRepositoryAction();
    }

    @FXML
    private void manageJobs() {
        new ManageJobsAction();
    }

    @FXML
    private void describeObject() {
        describeObjectAction.execute();
    }

    @FXML
    private void getLastSQL() {
        new GetLastSQLAction();
    }

    @FXML
    private void enableSQLTrace() {
        new EnableSQLTraceAction();
    }

    @FXML
    private void disableSQLTrace() {
        new DisableSQLTraceAction();
    }

    @FXML
    private void showCurrentSessions() {
        new ShowCurrentSessionsAction();
    }

    @FXML
    private void executeAPIScript() {
        new ExecuteAPIScriptAction();
    }

    @FXML
    private void executeDQLScript() {
        new ExecuteDQLScriptAction();
    }

    @FXML
    private void getAttributes() throws IOException {
        Stage getAttributesStage = new Stage();
        getAttributesStage.setTitle(String.format("Attributes List - %s (%s)", "ID", repository.getRepositoryName()));
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/nl/bos/views/GetAttributes.fxml"));
        VBox getAttributesPane = fxmlLoader.load();
        Scene scene = new Scene(getAttributesPane);
        getAttributesStage.setScene(scene);
        getAttributesStage.showAndWait();
    }

    @FXML
    private void exportResults() {
        QueryWithResult queryWithResult = (QueryWithResult) Controllers.get(QueryWithResult.class.getSimpleName());
        new MenuItemExportToCsvAction(miExportResults, queryWithResult.getResult()).handle(null);
    }
}