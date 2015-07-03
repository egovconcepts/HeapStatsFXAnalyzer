/*
 * Copyright (C) 2014 Yasumasa Suenaga
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package jp.co.ntt.oss.heapstats.plugin.builtin.jvmlive.mbean;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.converter.IntegerStringConverter;
import javafx.util.converter.LongStringConverter;
import jp.co.ntt.oss.heapstats.WindowController;
import jp.co.ntt.oss.heapstats.jmx.JMXHelper;
import jp.co.ntt.oss.heapstats.mbean.HeapStatsMBean;
import jp.co.ntt.oss.heapstats.utils.HeapStatsUtils;

/**
 * FXML Controller class of HeapStatsMBean.
 *
 * @author Yasumasa Suenaga
 */
public class HeapStatsMBeanController implements Initializable {
    
    @FXML
    private Label headerLabel;
    
    @FXML
    TableView<HeapStatsConfig> configTable;
    
    @FXML
    TableColumn<HeapStatsConfig, String> keyColumn;
    
    @FXML
    TableColumn<HeapStatsConfig, Object> valueColumn;
    
    private JMXHelper jmxHelper;
    
    private Stage stage;
    

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        keyColumn.setCellValueFactory(new PropertyValueFactory<>("key"));
        keyColumn.setCellFactory(p -> new TableCell<HeapStatsConfig, String>(){
                                                                                 @Override
                                                                                 protected void updateItem(String item, boolean empty){
                                                                                     if(!empty){
                                                                                         HeapStatsConfig config = configTable.getItems().get(getIndex());
                                                                                         styleProperty().bind(Bindings.createStringBinding(() -> config.changedProperty().get() ? "-fx-text-fill: orange;" : "-fx-text-fill: black;", config.changedProperty()));
                                                                                         setText(item);
                                                                                     }
                                                                                 }
                                                                              });
        valueColumn.setCellValueFactory(new PropertyValueFactory<>("value"));
        valueColumn.setCellFactory(p -> new TableCell<HeapStatsConfig, Object>() {
                                                                                    @Override
                                                                                    protected void updateItem(Object item, boolean empty) {
                                                                                      super.updateItem(item, empty);
                                                                                      
                                                                                      if(empty){
                                                                                          return;
                                                                                      }
                                                                                      
                                                                                      Property valProp = (Property)getTableColumn().getCellObservableValue(getIndex());
                                                                                      Node node;
                                                                                      
                                                                                      if(item == null){
                                                                                        node = new TextField();
                                                                                        ((TextField)node).textProperty().bindBidirectional(valProp);
                                                                                      }
                                                                                      else if(item instanceof Boolean){
                                                                                        node = new CheckBox();
                                                                                        ((CheckBox)node).selectedProperty().bindBidirectional(valProp);
                                                                                      }
                                                                                      else if(item instanceof HeapStatsMBean.LogLevel){
                                                                                        node = new ChoiceBox(FXCollections.observableArrayList(HeapStatsMBean.LogLevel.values()));
                                                                                        ((ChoiceBox<HeapStatsMBean.LogLevel>)node).valueProperty().bindBidirectional(valProp);
                                                                                      }
                                                                                      else if(item instanceof HeapStatsMBean.RankOrder){
                                                                                        node = new ChoiceBox(FXCollections.observableArrayList(HeapStatsMBean.RankOrder.values()));
                                                                                        ((ChoiceBox<HeapStatsMBean.RankOrder>)node).valueProperty().bindBidirectional(valProp);
                                                                                      }
                                                                                      else if(item instanceof Integer){
                                                                                        node = new TextField();
                                                                                        ((TextField)node).textProperty().bindBidirectional((Property<Integer>)valProp, new IntegerStringConverter());
                                                                                      }
                                                                                      else if(item instanceof Long){
                                                                                        node = new TextField();
                                                                                        ((TextField)node).textProperty().bindBidirectional((Property<Long>)valProp, new LongStringConverter());
                                                                                      }
                                                                                      else{
                                                                                        node = new TextField();
                                                                                        ((TextField)node).textProperty().bindBidirectional(valProp);
                                                                                      }
                                                                                      
                                                                                      setGraphic(node);
                                                                                    }
                                                                                 });
    }    

    public JMXHelper getJmxHelper() {
        return jmxHelper;
    }

    public void setJmxHelper(JMXHelper jmxHelper) {
        this.jmxHelper = jmxHelper;
    }
    
    public void loadAllConfigs(){
        headerLabel.setText(jmxHelper.getUrl().toString());
        configTable.setItems(jmxHelper.getMbean().getConfigurationList()
                                                 .entrySet()
                                                 .stream()
                                                 .map(e -> new HeapStatsConfig(e.getKey(), e.getValue()))
                                                 .collect(FXCollections::observableArrayList, ObservableList::add, ObservableList::addAll));
    }
    
    @FXML
    private void onCommitBtnClick(ActionEvent event){
        
        try{
            configTable.getItems().stream()
                                  .filter(c -> c.changedProperty().get())
                                  .forEach(c -> jmxHelper.getMbean().changeConfiguration(c.keyProperty().get(), c.valueProperty().getValue()));
        }
        catch(IllegalArgumentException e){
            HeapStatsUtils.showExceptionDialog(e);
        }
        
    }

    @FXML
    private void onInvokeResourceBtnClick(ActionEvent event){
        if(jmxHelper.getMbean().invokeLogCollection()){
            Alert dialog = new Alert(AlertType.INFORMATION, "Invoke Resource Log collection is succeeded.", ButtonType.OK);
            dialog.show();
        }
        else{
            Alert dialog = new Alert(AlertType.ERROR, "Invoke Resource Log collection failed.", ButtonType.OK);
            dialog.show();
        }
    }

    @FXML
    private void onInvokeAllBtnClick(ActionEvent event){
        if(jmxHelper.getMbean().invokeAllLogCollection()){
            Alert dialog = new Alert(AlertType.INFORMATION, "Invoke Resource Log collection is succeeded.", ButtonType.OK);
            dialog.show();
        }
        else{
            Alert dialog = new Alert(AlertType.ERROR, "Invoke Resource Log collection failed.", ButtonType.OK);
            dialog.show();
        }
    }

    @FXML
    private void onInvokeSnapShotBtnClick(ActionEvent event){
        jmxHelper.getMbean().invokeSnapShotCollection();
        Alert dialog = new Alert(AlertType.INFORMATION, "Invoke SnapShot is requested.", ButtonType.OK);
        dialog.show();
    }
    
    @FXML
    private void onGetResourceBtnClick(ActionEvent event){
        FileChooser dialog = new FileChooser();
        dialog.setTitle("Save resource log");
        dialog.setInitialDirectory(new File(HeapStatsUtils.getDefaultDirectory()));
        dialog.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("CSV file (*.csv)", "*.csv"),
                                            new FileChooser.ExtensionFilter("All files", "*.*"));
        File logFile = dialog.showSaveDialog(WindowController.getInstance().getOwner());
        
        if(logFile != null){
            try {
                jmxHelper.getResourceLog(logFile.toPath());
            } catch (IOException | InterruptedException | ExecutionException ex) {
                HeapStatsUtils.showExceptionDialog(ex);
            }
        }
        
    }

    @FXML
    private void onGetSnapShotBtnClick(ActionEvent event){
        FileChooser dialog = new FileChooser();
        dialog.setTitle("Save SnapShot");
        dialog.setInitialDirectory(new File(HeapStatsUtils.getDefaultDirectory()));
        dialog.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("SnapShot file (*.dat)", "*.dat"),
                                            new FileChooser.ExtensionFilter("All files", "*.*"));
        File snapshotFile = dialog.showSaveDialog(WindowController.getInstance().getOwner());
        
        if(snapshotFile != null){
            try {
                jmxHelper.getSnapShot(snapshotFile.toPath());
            } catch (IOException | InterruptedException | ExecutionException ex) {
                HeapStatsUtils.showExceptionDialog(ex);
            }
        }
        
    }

    public Stage getStage() {
        return stage;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

}