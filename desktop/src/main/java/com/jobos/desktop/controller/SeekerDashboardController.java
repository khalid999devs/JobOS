package com.jobos.desktop.controller;

import com.jobos.desktop.core.navigation.NavigationManager;
import com.jobos.desktop.core.navigation.Route;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.property.SimpleStringProperty;

public class SeekerDashboardController {
    
    @FXML
    private Label applicationsCount;
    
    @FXML
    private Label interviewsCount;
    
    @FXML
    private Label savedJobsCount;
    
    @FXML
    private TableView<ApplicationData> applicationsTable;
    
    @FXML
    private TableColumn<ApplicationData, String> jobTitleColumn;
    
    @FXML
    private TableColumn<ApplicationData, String> companyColumn;
    
    @FXML
    private TableColumn<ApplicationData, String> statusColumn;
    
    @FXML
    private TableColumn<ApplicationData, String> dateColumn;
    
    @FXML
    private TableColumn<ApplicationData, String> salaryColumn;
    
    @FXML
    private void initialize() {
        loadDashboardData();
        setupTable();
    }
    
    private void loadDashboardData() {
        // Mock data for prototype
        applicationsCount.setText("12");
        interviewsCount.setText("5");
        savedJobsCount.setText("28");
    }
    
    private void setupTable() {
        // Setup column cell value factories
        jobTitleColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().jobTitle));
        companyColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().company));
        statusColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().status));
        dateColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().date));
        salaryColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().salary));
        
        // Add mock data
        ObservableList<ApplicationData> data = FXCollections.observableArrayList(
            new ApplicationData("Senior Java Developer", "Tech Solutions Inc", "Under Review", "Dec 20, 2025", "$120k-$150k"),
            new ApplicationData("Full Stack Engineer", "Innovation Labs", "Interview Scheduled", "Dec 18, 2025", "$100k-$130k"),
            new ApplicationData("Backend Developer", "Cloud Systems Corp", "Applied", "Dec 15, 2025", "$110k-$140k"),
            new ApplicationData("Software Architect", "Enterprise Tech", "Rejected", "Dec 12, 2025", "$150k-$180k"),
            new ApplicationData("Lead Developer", "StartUp Ventures", "Under Review", "Dec 10, 2025", "$130k-$160k")
        );
        
        applicationsTable.setItems(data);
    }
    
    @FXML
    private void handleViewAllApplications() {
        NavigationManager.getInstance().navigate(Route.SEEKER_APPLICATIONS);
    }
    
    @FXML
    private void handleBrowseJobs() {
        NavigationManager.getInstance().navigate(Route.SEEKER_JOBS);
    }
    
    @FXML
    private void handleMyCVs() {
        NavigationManager.getInstance().navigate(Route.SEEKER_CVS);
    }
    
    // Inner class for table data
    public static class ApplicationData {
        private final String jobTitle;
        private final String company;
        private final String status;
        private final String date;
        private final String salary;
        
        public ApplicationData(String jobTitle, String company, String status, String date, String salary) {
            this.jobTitle = jobTitle;
            this.company = company;
            this.status = status;
            this.date = date;
            this.salary = salary;
        }
    }
}
