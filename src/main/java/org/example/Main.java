package org.example;

import com.google.api.Metric;
import com.google.api.MetricDescriptor;
import com.google.api.MonitoredResource;
import com.google.cloud.monitoring.v3.MetricServiceClient;
import com.google.monitoring.v3.*;
import com.google.protobuf.util.Timestamps;
import io.grpc.LoadBalancerRegistry;
import io.grpc.internal.PickFirstLoadBalancerProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
    private MetricServiceClient metricServiceClient;
    public static void main(String[] args) throws IOException, InterruptedException {
        String podName = System.getenv("POD_NAME");
        if (podName == null || podName.isEmpty()) {
            System.err.println("POD_NAME env variable is empty");
            System.exit(1);
        }

        String podNameSpace = System.getenv("POD_NS");
        if (podNameSpace== null || podNameSpace.isEmpty()) {
            System.err.println("POD_NS env variable is empty");
            System.exit(1);
        }

        String projectId = System.getenv("PROJECT_ID");
        if (projectId== null || projectId.isEmpty()) {
            System.err.println("PROJECT_ID env variable is empty");
            System.exit(1);
        }

        String clusterName = System.getenv("CLUSTER_NAME");
        if (clusterName== null || clusterName.isEmpty()) {
            System.err.println("CLUSTER_NAME env variable is empty");
            System.exit(1);
        }

        String location = System.getenv("LOCATION");
        if (location== null || location.isEmpty()) {
            System.err.println("LOCATION env variable is empty");
            System.exit(1);
        }

        LoadBalancerRegistry.getDefaultRegistry().register(new PickFirstLoadBalancerProvider());
        Main m = new Main();
        m.metricServiceClient = MetricServiceClient.create();
        while (true) {
            m.produceMetric(projectId, location, clusterName, podName, podNameSpace);
            Thread.sleep(10 * 1000);
        }
    }

    public void printMetricDescriptorsList() throws IOException {
        // Your Google Cloud Platform project ID
        // String projectId = System.getProperty("xumo-ovp-ng");
        String projectId = "xumo-ovp-ng";
        final MetricServiceClient client = MetricServiceClient.create();
        ProjectName name = ProjectName.of(projectId);

        ListMetricDescriptorsRequest request =
                ListMetricDescriptorsRequest.newBuilder().setName(name.toString()).build();
        MetricServiceClient.ListMetricDescriptorsPagedResponse response = client.listMetricDescriptors(request);

        System.out.println("Listing descriptors: ");

        for (MetricDescriptor d : response.iterateAll()) {
            System.out.println(d.getName() + " " + d.getDisplayName());
        }
    }

    public void produceMetric(String projectId, String location, String clusterName,
                              String podName, String podNameSpace) throws IOException {

        // Prepares an individual data point
        TimeInterval interval =
                TimeInterval.newBuilder()
                        .setEndTime(Timestamps.fromMillis(System.currentTimeMillis()))
                        .build();
        TypedValue value = TypedValue.newBuilder().setDoubleValue(500).build();
        Point point = Point.newBuilder().setInterval(interval).setValue(value).build();


        List<Point> pointList = new ArrayList<>();
        pointList.add(point);

        ProjectName name = ProjectName.of(projectId);

        // Prepares the metric descriptor
        Map<String, String> metricLabels = new HashMap<>();
        Metric metric =
                Metric.newBuilder()
                        .setType("custom.googleapis.com/some_latency")
                        .putAllLabels(metricLabels)
                        .build();

        // Prepares the monitored resource descriptor
        Map<String, String> resourceLabels = new HashMap<>();
        resourceLabels.put("project_id", projectId);
        resourceLabels.put("location", location);
        resourceLabels.put("cluster_name", clusterName);
        resourceLabels.put("namespace_name", podNameSpace);
        resourceLabels.put("pod_name", podName);

        MonitoredResource resource =
                MonitoredResource.newBuilder().setType("k8s_pod")
                        .putAllLabels(resourceLabels).build();

        // Prepares the time series request
        TimeSeries timeSeries =
                TimeSeries.newBuilder()
                        .setMetric(metric)
                        .setResource(resource)
                        .addAllPoints(pointList)
                        .build();


        List<TimeSeries> timeSeriesList = new ArrayList<>();
        timeSeriesList.add(timeSeries);

        CreateTimeSeriesRequest request =
                CreateTimeSeriesRequest.newBuilder()
                        .setName(name.toString())
                        .addAllTimeSeries(timeSeriesList)
                        .build();

        // Writes time series data
        metricServiceClient.createTimeSeries(request);
        System.out.println("Done writing time series value.");
    }
}