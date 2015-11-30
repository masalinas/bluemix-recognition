package org.vaadin.presentation.views;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.vaadin.addon.leaflet.LMap;
import org.vaadin.addon.leaflet.LMarker;
import org.vaadin.addon.leaflet.LOpenStreetMapLayer;
import org.vaadin.addon.leaflet.LeafletClickEvent;
import org.vaadin.addon.leaflet.LeafletClickListener;
import org.vaadin.addon.leaflet.control.LZoom;
import org.vaadin.addon.leaflet.shared.ControlPosition;
import org.vaadin.backend.CustomerService;
import org.vaadin.backend.domain.Customer;
import org.vaadin.cdiviewmenu.ViewMenuItem;
import org.vaadin.viritin.label.Header;
import org.vaadin.viritin.layouts.MVerticalLayout;

import com.vaadin.cdi.CDIView;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.AbstractSelect.ItemCaptionMode;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Notification;

@CDIView("map")
@ViewMenuItem(icon = FontAwesome.GLOBE, order = 1)
public class MapView extends MVerticalLayout implements View {

    @Inject
    CustomerService service;

    BeanItemContainer<Customer> customerContainer = new BeanItemContainer<Customer>(Customer.class);
    
    ComboBox customersField = new ComboBox("Customers");
    LMap worldMap = new LMap();

    @PostConstruct
    void init() {
    	customerContainer.removeAllItems();
		customerContainer.addAll( service.findAll());
		
    	// bind combox and show the firstname of the customer
    	customersField.setContainerDataSource(customerContainer);
		customersField.setItemCaptionPropertyId("firstName");
		customersField.setItemCaptionMode(ItemCaptionMode.PROPERTY);
		customersField.setImmediate(true);
		customersField.setNullSelectionAllowed(true);
    			
		customersField.addValueChangeListener(new ValueChangeListener() {			
			@Override
			public void valueChange(ValueChangeEvent event) {
				List<Customer> customers = new ArrayList<Customer>();
				
				if (customersField.getValue() == null)
					customers.addAll(service.findAll());
				else
					customers.add((Customer) customersField.getValue());
				
				paintMap(customers);								
			}
		});
		
        add(new Header("Customers on map").setHeaderLevel(2));
        add(customersField);
        expand(worldMap);
        setMargin(new MarginInfo(false, true, true, true));

        LZoom zoom = new LZoom();
        zoom.setPosition(ControlPosition.topright);
        worldMap.addControl(zoom);
    }

    @Override
    public void enter(ViewChangeListener.ViewChangeEvent viewChangeEvent) {
    	paintMap(service.findAll());
    	
        /*worldMap.removeAllComponents();
        LOpenStreetMapLayer osm = new LOpenStreetMapLayer();
        osm.setDetectRetina(true);
        worldMap.addComponent(osm);
        for (final Customer customer : service.findAll()) {
            if(customer.getLocation() != null) {
                LMarker marker = new LMarker(customer.getLocation());
                marker.addClickListener(new LeafletClickListener() {
                    @Override
                    public void onClick(LeafletClickEvent event) {
                        Notification.show(
                                "Customer: " + customer.getFirstName() + " " + customer.
                                getLastName());
                    }
                });
                worldMap.addComponent(marker);
           }
        }
        worldMap.zoomToContent();*/
    }
    
    private void paintMap(List<Customer> customers) {
    	worldMap.removeAllComponents();
        LOpenStreetMapLayer osm = new LOpenStreetMapLayer();
        osm.setDetectRetina(true);
        worldMap.addComponent(osm);
        for (final Customer customer : customers) {
            if(customer.getLocation() != null) {
                LMarker marker = new LMarker(customer.getLocation());
                marker.addClickListener(new LeafletClickListener() {
                    @Override
                    public void onClick(LeafletClickEvent event) {
                        Notification.show(
                                "Customer: " + customer.getFirstName() + " " + customer.
                                getLastName());
                    }
                });
                worldMap.addComponent(marker);
           }
        }
        worldMap.zoomToContent();
    }
}