package org.vaadin.presentation.views;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import org.vaadin.backend.CustomerService;
import org.vaadin.backend.domain.Customer;
import org.vaadin.backend.domain.CustomerStatus;
import org.vaadin.backend.domain.Gender;
import org.vaadin.cdiviewmenu.ViewMenuItem;
import org.vaadin.presentation.ScreenSize;
import org.vaadin.viritin.label.Header;
import org.vaadin.viritin.layouts.MHorizontalLayout;
import org.vaadin.viritin.layouts.MMarginInfo;
import org.vaadin.viritin.layouts.MVerticalLayout;
//import org.watson.visualrecognition.VisualRecognitionService;
//import org.watson.visualrecognition.response.Image;

import com.ibm.watson.developer_cloud.visual_recognition.v1.VisualRecognition;
import com.ibm.watson.developer_cloud.visual_recognition.v1.model.Label;
import com.ibm.watson.developer_cloud.visual_recognition.v1.model.LabelSet;
import com.ibm.watson.developer_cloud.visual_recognition.v1.model.RecognizedImage;
import com.ibm.watson.developer_cloud.visual_recognition.v1.model.VisualRecognitionImages;
import com.vaadin.addon.charts.Chart;
import com.vaadin.addon.charts.model.ChartType;
import com.vaadin.addon.charts.model.Configuration;
import com.vaadin.addon.charts.model.DataSeries;
import com.vaadin.addon.charts.model.DataSeriesItem;
import com.vaadin.addon.charts.model.HorizontalAlign;
import com.vaadin.addon.charts.model.Labels;
import com.vaadin.addon.charts.model.Legend;
import com.vaadin.addon.charts.model.ListSeries;
import com.vaadin.addon.charts.model.PlotOptionsColumn;
import com.vaadin.addon.charts.model.PlotOptionsFunnel;
import com.vaadin.addon.charts.model.PlotOptionsPie;
import com.vaadin.addon.charts.model.Stacking;
import com.vaadin.addon.charts.model.VerticalAlign;
import com.vaadin.addon.charts.model.XAxis;
import com.vaadin.cdi.CDIView;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Component;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.themes.ValoTheme;

import pl.exsio.plupload.Plupload;
import pl.exsio.plupload.PluploadError;
import pl.exsio.plupload.PluploadFile;
import pl.exsio.plupload.helper.filter.PluploadFilter;

/**
 * An example view that just make some simple analysis for the data and displays
 * it in various charts.
 */
//@CDIView("analyze")
@CDIView
@ViewMenuItem(icon = FontAwesome.BAR_CHART_O, order = 1)
public class AnalyzeView extends MVerticalLayout implements View {		
    @Inject
    CustomerService service;
        
    @SuppressWarnings("serial")
	@Override
    public void enter(ViewChangeListener.ViewChangeEvent viewChangeEvent) {
        removeAllComponents();

        // configure upload tachos button
        final TextArea area = new TextArea("Image recognition");
        area.setHeight("200px");
        
        final Plupload upload = new Plupload("Recognice Image");
        upload.addStyleName(ValoTheme.BUTTON_FRIENDLY);
        upload.addFilter(new PluploadFilter("Image files", "jpg,png"));
     		        
        upload.addFileUploadedListener(new Plupload.FileUploadedListener() {
        	@Override
        	public void onFileUploaded(PluploadFile file) {
        		File image = (File) file.getUploadedFile();
        		
        		VisualRecognition visualRecognitionService = new VisualRecognition();
        		visualRecognitionService.setUsernameAndPassword("miguel@thingtrack.com", "underground");
            	
            	//LabelSet labelSet = new LabelSet();
            	//labelSet.withLabelGroup("Auto Racing").withLabelGroup("Sports");
                
        		try {
        			VisualRecognitionImages recognizedImages = visualRecognitionService.recognize(image);
        			    
        			String message = null;
        			for (Label label : recognizedImages.getFirstImage().getLabels())
        				message = label.getName() + ":" + label.getScore() + "\n";        			
        			
        			area.setValue(message);
        		}
            	catch(Exception ex) {
            		Notification.show("Error :" + ex.getMessage());
            	}        	
        	}
        });
     		
    	// notify, when the upload process is completed
        upload.addUploadCompleteListener(new Plupload.UploadCompleteListener() {
 	        @Override
 	        public void onUploadComplete() {
 	    	   Notification.show("Upload Process finished");
 	    	   	    
 	        }
 		});
     		
 		// autostart the uploader after addind files
        upload.addFilesAddedListener(new Plupload.FilesAddedListener() {
		    @SuppressWarnings("unchecked")
		    @Override
	        public void onFilesAdded(PluploadFile[] files) {		    			    	
		    	upload.start();
		    }
		});
 			
 		// handle errors
        upload.addErrorListener(new Plupload.ErrorListener() {
		    @SuppressWarnings("unchecked")
			@Override
	        public void onError(PluploadError error) {
		    	 Notification.show("Error on " + error.getFile().getName());		    	
		    }
		});
		        
        setMargin(new MMarginInfo(false, true));
        add(new Header("Customer analysis").setHeaderLevel(2));

        add(upload);
        add(area);
        
        List<Customer> customerData = service.findAll();
        add(ageDistribution(customerData));
        final Component funnel = createStatusFunnel(customerData);
        final Component gender = genderDistribution(customerData);
        if (ScreenSize.getScreenSize() == ScreenSize.SMALL) {
            addComponents(funnel, gender);
        } else {
            addComponent(new MHorizontalLayout(funnel, gender).withFullWidth());
        }

    }

    private Component genderDistribution(List<Customer> customerData) {
        int women = 0, men = 0;
        for (Customer c : customerData) {
            if (c.getGender() == Gender.Female) {
                women++;
            } else {
                men++;
            }
        }

        Chart chart = getBasicChart(ChartType.PIE);

        Configuration conf = chart.getConfiguration();

        PlotOptionsPie plotOptions = new PlotOptionsPie();
        Labels dataLabels = new Labels();
        dataLabels.setEnabled(true);
        dataLabels.setFormat("{point.name}: {percentage:.0f}%");
        plotOptions.setDataLabels(dataLabels);
        conf.setPlotOptions(plotOptions);

        final DataSeries series = new DataSeries();
        series.add(new DataSeriesItem("Men", men));
        series.add(new DataSeriesItem("Women", women));
        conf.setSeries(series);
        return wrapInPanel(chart, "Gender");
    }

    private static Panel wrapInPanel(Chart chart, String caption) {
        Panel panel = new Panel(caption, chart);
        panel.setHeight("300px");
        chart.setSizeFull();
        return panel;
    }

    private enum AgeGroup {

        Children(0, 15), Young(15, 30), MiddleAged(30, 60), Old(60, 100);

        private final int min;
        private final int max;

        AgeGroup(int min, int max) {
            this.min = min;
            this.max = max;
        }

        public static AgeGroup getAgeGroup(Date birthDate) {
            int age = new Date().getYear() - birthDate.getYear();
            for (AgeGroup g : AgeGroup.values()) {
                if (age <= g.max && age > g.min) {
                    return g;
                }
            }
            return Old;
        }
    }

    private Chart getBasicChart(ChartType type) {
        Chart chart = new Chart(type);
        // title from panel
        chart.getConfiguration().setTitle("");
        return chart;
    }

    private Component ageDistribution(List<Customer> customerData) {
        Integer[] menValues = new Integer[AgeGroup.values().length];
        Integer[] womenValues = new Integer[AgeGroup.values().length];
        for (int i = 0; i < AgeGroup.values().length; i++) {
            menValues[i] = 0;
            womenValues[i] = 0;
        }
        for (Customer c : customerData) {
            if (c.getBirthDate() != null) {
                AgeGroup g = AgeGroup.getAgeGroup(c.getBirthDate());
                if (c.getGender() == Gender.Female) {
                    womenValues[g.ordinal()]++;
                } else {
                    menValues[g.ordinal()]++;
                }
            }
        }

        Chart chart = getBasicChart(ChartType.COLUMN);

        Configuration conf = chart.getConfiguration();

        XAxis xAxis = new XAxis();
        String[] names = new String[AgeGroup.values().length];
        for (AgeGroup g : AgeGroup.values()) {
            names[g.ordinal()] = String.format("%s-%s", g.min,
                    g.max);
        }
        xAxis.setCategories(names);
        conf.addxAxis(xAxis);

        conf.getyAxis().setTitle("");

        Legend legend = new Legend();
        legend.setHorizontalAlign(HorizontalAlign.RIGHT);
        legend.setFloating(true);
        legend.setVerticalAlign(VerticalAlign.TOP);
        legend.setX(-5);
        legend.setY(5);
        conf.setLegend(legend);

        PlotOptionsColumn plotOptions = new PlotOptionsColumn();
        plotOptions.setStacking(Stacking.NORMAL);
        conf.setPlotOptions(plotOptions);

        conf.addSeries(new ListSeries("Men", menValues));
        conf.addSeries(new ListSeries("Women", womenValues));

        return wrapInPanel(chart, "Age distribution");

    }

    private Component createStatusFunnel(List<Customer> customerData) {
        int[] values = new int[CustomerStatus.values().length];
        for (Customer c : customerData) {
            if (c.getStatus() != null) {
                values[c.getStatus().ordinal()]++;
            }
        }
        Chart chart = getBasicChart(ChartType.FUNNEL);
        DataSeries dataSeries = new DataSeries();
        dataSeries.add(new DataSeriesItem("Imported lead",
                values[CustomerStatus.ImportedLead.ordinal()]));
        dataSeries.add(new DataSeriesItem("Not contacted",
                values[CustomerStatus.NotContacted.ordinal()]));
        dataSeries.add(new DataSeriesItem("Contacted",
                values[CustomerStatus.Contacted.ordinal()]));
        dataSeries.add(new DataSeriesItem("Customer",
                values[CustomerStatus.Customer.ordinal()]));

        Configuration conf = chart.getConfiguration();
        conf.getChart().setMarginRight(75);

        PlotOptionsFunnel options = new PlotOptionsFunnel();
        options.setNeckWidthPercentage(30);
        options.setNeckHeightPercentage(30);

        options.setWidthPercentage(70);

        dataSeries.setPlotOptions(options);
        conf.addSeries(dataSeries);

        return wrapInPanel(chart, "Sales funnel");
    }
}
