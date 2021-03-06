package com.cloudycrew.cloudycar;

import com.cloudycrew.cloudycar.connectivity.IConnectivityService;
import com.cloudycrew.cloudycar.connectivity.TestConnectivityService;
import com.cloudycrew.cloudycar.elasticsearch.ElasticSearchConnectivityException;
import com.cloudycrew.cloudycar.models.Location;
import com.cloudycrew.cloudycar.models.Route;
import com.cloudycrew.cloudycar.models.User;
import com.cloudycrew.cloudycar.models.requests.CancelledRequest;
import com.cloudycrew.cloudycar.models.requests.ConfirmedRequest;
import com.cloudycrew.cloudycar.models.requests.PendingRequest;
import com.cloudycrew.cloudycar.models.requests.Request;
import com.cloudycrew.cloudycar.requeststorage.CloudRequestService;
import com.cloudycrew.cloudycar.requeststorage.CompositeRequestService;
import com.cloudycrew.cloudycar.requeststorage.IRequestService;
import com.cloudycrew.cloudycar.requeststorage.LocalRequestService;
import com.cloudycrew.cloudycar.requeststorage.PersistentRequestQueue;
import com.cloudycrew.cloudycar.scheduling.TestSchedulerProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Created by George on 2016-10-12.
 */

@RunWith(MockitoJUnitRunner.class)
public class OfflineTests {
    @Mock
    private LocalRequestService localRequestService;
    @Mock
    private CloudRequestService cloudRequestService;
    @Mock
    private CompositeRequestService compositeRequestService;
    @Mock
    private PersistentRequestQueue requestQueue;
    @Mock
    private TestConnectivityService connectivityService;
    @Mock
    private GeoDecoder geoDecoder;

    private User rider;
    private User driver;
    private PendingRequest request1;
    private PendingRequest request2;
    private PendingRequest newRequest;
    private String testDescription;

    private PendingRequest acceptedRequest1;
    private PendingRequest newAcceptedRequest;

    private CancelledRequest newCancelledRequest;
    private ConfirmedRequest newConfirmedRequest;

    private String requestDescription;

    private ArrayList<Request> cloudRequests;

    @Before
    public void set_up() {
        set_up_requests();
        this.connectivityService = new TestConnectivityService();
        compositeRequestService = new CompositeRequestService(cloudRequestService, localRequestService, connectivityService, requestQueue, new TestSchedulerProvider(), geoDecoder);
        when(localRequestService.getRequests()).thenReturn(Arrays.<Request>asList(request1, request2));
        //  when(localRequestService.getAcceptedRequests()).thenReturn(Arrays.asList(acceptedRequest1));
    }

    private void set_up_requests() {
        rider = new User("janedoedoe");
        driver = new User("driverdood");
        testDescription = "test description";
        requestDescription = "description";

        Location startingLocation = new Location(48.1472373, 11.5673969,testDescription);
        Location endingLocation = new Location(48.1258551, 11.5121003,testDescription);

        Route route = new Route(startingLocation, endingLocation);

        double price = 3.5;

        request1 = new PendingRequest(rider.getUsername(), route, price, requestDescription);
        request2 = new PendingRequest(rider.getUsername(), route, price, requestDescription);

        newRequest = new PendingRequest(rider.getUsername(), route, price, requestDescription);

        newCancelledRequest = newRequest.cancel();



        acceptedRequest1 = request1.accept(driver.getUsername());
        newAcceptedRequest = newRequest.accept(driver.getUsername());
        newConfirmedRequest = newAcceptedRequest.confirmRequest(driver.getUsername());
        cloudRequests = new ArrayList<>();
        cloudRequests.add(newAcceptedRequest);
    }

    @Test
    public void test_getRequests_ifTheDeviceIsOffline_getsLocalRequests() {
        when(localRequestService.getRequests()).thenReturn(Arrays.<Request>asList(request1, request2));
        when(cloudRequestService.getRequests()).thenThrow(new ElasticSearchConnectivityException(new Exception()));
        connectivityService.setInternetConnectivity(false);

        List<Request> expectedRequests = Arrays.<Request>asList(request1, request2);
        List<Request> actualRequests = compositeRequestService.getRequests();

        assertEquals(expectedRequests, actualRequests);
    }

    @Test
    public void test_createRequest_ifTheDeviceIsOffline_thenSendsRequestWhenDeviceRegainsConnectivity() {
        when(geoDecoder.decodeLatLng(anyDouble(), anyDouble())).thenReturn("Test address");
        doThrow(new ElasticSearchConnectivityException(new IOException())).when(cloudRequestService).createRequest(any(Request.class));
        when(requestQueue.getCreateQueue()).thenReturn(Arrays.<Request>asList(newRequest));
        connectivityService.setInternetConnectivity(false);

        compositeRequestService.createRequest(newRequest);
        verify(requestQueue).enqueueNewRequest(newRequest);

        doNothing().when(cloudRequestService).createRequest(any(Request.class));
        connectivityService.setInternetConnectivity(true);

        verify(requestQueue).dequeueCreation(newRequest);
        verify(cloudRequestService, times(2)).createRequest(newRequest);
    }
    
    @Test
    public void test_acceptRequest_ifTheDeviceIsOffline_thenSendsAcceptedRequestWhenDeviceRegainsConnectivity() {
        when(geoDecoder.decodeLatLng(anyDouble(), anyDouble())).thenReturn("Test address");
        doThrow(new ElasticSearchConnectivityException(new IOException())).when(cloudRequestService).updateRequest(any(Request.class));
        when(requestQueue.getAcceptedQueue()).thenReturn(Arrays.<PendingRequest>asList(newAcceptedRequest));
        when(cloudRequestService.getRequests()).thenReturn(new ArrayList<Request>(cloudRequests));
        connectivityService.setInternetConnectivity(false);

        compositeRequestService.updateRequest(newAcceptedRequest);
        verify(requestQueue).enqueueAccept(newAcceptedRequest);

        doNothing().when(cloudRequestService).updateRequest(any(Request.class));
        connectivityService.setInternetConnectivity(true);

        verify(requestQueue).dequeueAccept(newAcceptedRequest);
        verify(cloudRequestService, times(2)).updateRequest(any(Request.class));
    }

    @Test
    public void test_cancelRequest_ifTheDeviceIsOffline_thenSendsRequestWhenDeviceRegainsConnectivity() {
        when(geoDecoder.decodeLatLng(anyDouble(), anyDouble())).thenReturn("Test address");
        doThrow(new ElasticSearchConnectivityException(new IOException())).when(cloudRequestService).updateRequest(any(Request.class));
        when(requestQueue.getCancellationQueue()).thenReturn(Arrays.<CancelledRequest>asList(newCancelledRequest));
        connectivityService.setInternetConnectivity(false);

        compositeRequestService.updateRequest(newCancelledRequest);
        verify(requestQueue).enqueueCancellation(newCancelledRequest);

        doNothing().when(cloudRequestService).updateRequest(any(Request.class));
        connectivityService.setInternetConnectivity(true);

        verify(requestQueue).dequeueCancellation(newCancelledRequest);
        verify(cloudRequestService, times(2)).updateRequest(newCancelledRequest);
    }

    @Test
    public void test_confirmRequest_ifTheDeviceIsOffline_thenSendsConfirmedRequestWhenDeviceRegainsConnectivity() {
        when(geoDecoder.decodeLatLng(anyDouble(), anyDouble())).thenReturn("Test address");
        doThrow(new ElasticSearchConnectivityException(new IOException())).when(cloudRequestService).updateRequest(any(Request.class));
        when(requestQueue.getConfirmationQueue()).thenReturn(Arrays.<ConfirmedRequest>asList(newConfirmedRequest));
        connectivityService.setInternetConnectivity(false);

        compositeRequestService.updateRequest(newConfirmedRequest);
        verify(requestQueue).enqueueConfirmation(newConfirmedRequest);

        doNothing().when(cloudRequestService).updateRequest(any(Request.class));
        connectivityService.setInternetConnectivity(true);

        verify(requestQueue).dequeueConfirmation(newConfirmedRequest);
        verify(cloudRequestService, times(2)).updateRequest(newConfirmedRequest);
    }
}
