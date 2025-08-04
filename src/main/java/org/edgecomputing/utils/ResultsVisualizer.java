package org.edgecomputing.utils;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.PieSectionLabelGenerator;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to generate visualizations from simulation results.
 * Creates various charts to help analyze simulation outcomes.
 */
public class ResultsVisualizer {
    
    private static final Logger logger = LoggerFactory.getLogger(ResultsVisualizer.class);
    
    private final String resultsDir;
    private final String outputDir;
    
    /**
     * Create a new ResultsVisualizer instance.
     * 
     * @param resultsDir Directory containing CSV result files
     * @param outputDir Directory to output chart images
     */
    public ResultsVisualizer(String resultsDir, String outputDir) {
        this.resultsDir = resultsDir;
        this.outputDir = outputDir;
        
        // Create output directory if it doesn't exist
        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
    
    /**
     * Generate all visualization charts.
     * 
     * @throws IOException if there's an error reading data or writing charts
     */
    public void generateAllCharts() throws IOException {
        logger.info("Generating visualization charts");
        
        generateTaskDistributionPieChart();
        generateResourceUtilizationLineChart();
        generateDeviceBatteryLineChart();
        generateResponseTimeLineChart();
        generateTaskCompletionBarChart();
        
        logger.info("Chart generation complete");
    }
    
    /**
     * Generate a pie chart showing task distribution (local, edge, cloud).
     * 
     * @throws IOException if there's an error reading data or writing chart
     */
    public void generateTaskDistributionPieChart() throws IOException {
        logger.info("Generating task distribution pie chart");
        
        // Read final task metrics
        Map<String, Double> finalMetrics = readFinalTaskMetrics();
        
        // Create dataset
        DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
        dataset.setValue("Local Execution", finalMetrics.getOrDefault("LocalTasks", 0.0));
        dataset.setValue("Edge Execution", finalMetrics.getOrDefault("EdgeTasks", 0.0));
        dataset.setValue("Cloud Execution", finalMetrics.getOrDefault("CloudTasks", 0.0));
        
        // Create chart
        JFreeChart chart = ChartFactory.createPieChart(
                "Task Execution Distribution",
                dataset,
                true,
                true,
                false);
        
        // Customize chart
        PiePlot<String> plot = (PiePlot<String>) chart.getPlot();
        plot.setSectionPaint("Local Execution", new Color(65, 105, 225));  // Royal Blue
        plot.setSectionPaint("Edge Execution", new Color(0, 128, 0));      // Green
        plot.setSectionPaint("Cloud Execution", new Color(220, 20, 60));   // Crimson
        plot.setLabelFont(new Font("SansSerif", Font.PLAIN, 12));
        // Create custom label generator compatible with JFreeChart version
        plot.setLegendLabelGenerator(new StandardPieSectionLabelGenerator("{0} ({2})", new DecimalFormat("0"), new DecimalFormat("0.0%")));
        
        // Save chart
        ChartUtils.saveChartAsPNG(new File(outputDir + "/task_distribution.png"), chart, 800, 600);
    }
    
    /**
     * Generate line charts showing resource utilization over time.
     * 
     * @throws IOException if there's an error reading data or writing chart
     */
    public void generateResourceUtilizationLineChart() throws IOException {
        logger.info("Generating resource utilization line charts");
        
        // Read edge node metrics
        List<Map<String, Object>> edgeMetrics = readEdgeNodeMetrics();
        
        // Create dataset for CPU utilization
        XYSeriesCollection cpuDataset = new XYSeriesCollection();
        Map<Integer, XYSeries> cpuSeries = new HashMap<>();
        
        // Create dataset for RAM utilization
        XYSeriesCollection ramDataset = new XYSeriesCollection();
        Map<Integer, XYSeries> ramSeries = new HashMap<>();
        
        // Create dataset for bandwidth utilization
        XYSeriesCollection bwDataset = new XYSeriesCollection();
        Map<Integer, XYSeries> bwSeries = new HashMap<>();
        
        // Process data
        for (Map<String, Object> record : edgeMetrics) {
            double time = (Double) record.get("Time");
            int nodeId = (Integer) record.get("NodeId");
            double cpuUtil = (Double) record.get("CpuUtilization");
            double ramUtil = (Double) record.get("RamUtilization");
            double bwUtil = (Double) record.get("BwUtilization");
            
            // CPU utilization
            XYSeries cpuNodeSeries = cpuSeries.computeIfAbsent(nodeId, 
                    k -> new XYSeries("Edge Node " + k));
            cpuNodeSeries.add(time, cpuUtil * 100.0);  // Convert to percentage
            
            // RAM utilization
            XYSeries ramNodeSeries = ramSeries.computeIfAbsent(nodeId, 
                    k -> new XYSeries("Edge Node " + k));
            ramNodeSeries.add(time, ramUtil * 100.0);  // Convert to percentage
            
            // Bandwidth utilization
            XYSeries bwNodeSeries = bwSeries.computeIfAbsent(nodeId, 
                    k -> new XYSeries("Edge Node " + k));
            bwNodeSeries.add(time, bwUtil * 100.0);  // Convert to percentage
        }
        
        // Add series to datasets
        for (XYSeries series : cpuSeries.values()) {
            cpuDataset.addSeries(series);
        }
        
        for (XYSeries series : ramSeries.values()) {
            ramDataset.addSeries(series);
        }
        
        for (XYSeries series : bwSeries.values()) {
            bwDataset.addSeries(series);
        }
        
        // Create CPU utilization chart
        JFreeChart cpuChart = createUtilizationChart(
                "Edge Node CPU Utilization Over Time",
                "Time (seconds)",
                "CPU Utilization (%)",
                cpuDataset);
        ChartUtils.saveChartAsPNG(new File(outputDir + "/cpu_utilization.png"), cpuChart, 800, 600);
        
        // Create RAM utilization chart
        JFreeChart ramChart = createUtilizationChart(
                "Edge Node RAM Utilization Over Time",
                "Time (seconds)",
                "RAM Utilization (%)",
                ramDataset);
        ChartUtils.saveChartAsPNG(new File(outputDir + "/ram_utilization.png"), ramChart, 800, 600);
        
        // Create bandwidth utilization chart
        JFreeChart bwChart = createUtilizationChart(
                "Edge Node Bandwidth Utilization Over Time",
                "Time (seconds)",
                "Bandwidth Utilization (%)",
                bwDataset);
        ChartUtils.saveChartAsPNG(new File(outputDir + "/bw_utilization.png"), bwChart, 800, 600);
    }
    
    /**
     * Helper method to create a utilization line chart.
     */
    private JFreeChart createUtilizationChart(String title, String xAxisLabel, 
                                             String yAxisLabel, XYSeriesCollection dataset) {
        JFreeChart chart = ChartFactory.createXYLineChart(
                title,
                xAxisLabel,
                yAxisLabel,
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false);
        
        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.white);
        plot.setDomainGridlinePaint(Color.lightGray);
        plot.setRangeGridlinePaint(Color.lightGray);
        
        // Customize renderer
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        for (int i = 0; i < dataset.getSeriesCount(); i++) {
            renderer.setSeriesStroke(i, new BasicStroke(2.0f));
            renderer.setSeriesShapesVisible(i, false);
        }
        plot.setRenderer(renderer);
        
        // Customize y-axis to show percentages
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setRange(0.0, 100.0);
        
        return chart;
    }
    
    /**
     * Generate line chart showing device battery levels over time.
     * 
     * @throws IOException if there's an error reading data or writing chart
     */
    public void generateDeviceBatteryLineChart() throws IOException {
        logger.info("Generating device battery level line chart");
        
        // Read device metrics
        List<Map<String, Object>> deviceMetrics = readDeviceMetrics();
        
        // Create dataset
        XYSeriesCollection dataset = new XYSeriesCollection();
        Map<Integer, XYSeries> series = new HashMap<>();
        
        // Process data
        for (Map<String, Object> record : deviceMetrics) {
            double time = (Double) record.get("Time");
            int deviceId = (Integer) record.get("DeviceId");
            double batteryLevel = (Double) record.get("BatteryLevel");
            
            XYSeries deviceSeries = series.computeIfAbsent(deviceId, 
                    k -> new XYSeries("Device " + k));
            deviceSeries.add(time, batteryLevel * 100.0);  // Convert to percentage
        }
        
        // Add series to dataset (limit to 10 devices to avoid cluttering)
        int count = 0;
        for (XYSeries s : series.values()) {
            dataset.addSeries(s);
            count++;
            if (count >= 10) {
                break;
            }
        }
        
        // Create chart
        JFreeChart chart = ChartFactory.createXYLineChart(
                "IoT Device Battery Levels Over Time",
                "Time (seconds)",
                "Battery Level (%)",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false);
        
        // Customize chart
        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.white);
        plot.setDomainGridlinePaint(Color.lightGray);
        plot.setRangeGridlinePaint(Color.lightGray);
        
        // Customize renderer
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        for (int i = 0; i < dataset.getSeriesCount(); i++) {
            renderer.setSeriesStroke(i, new BasicStroke(2.0f));
            renderer.setSeriesShapesVisible(i, false);
        }
        plot.setRenderer(renderer);
        
        // Customize y-axis to show percentages
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setRange(0.0, 100.0);
        
        // Save chart
        ChartUtils.saveChartAsPNG(new File(outputDir + "/battery_levels.png"), chart, 800, 600);
    }
    
    /**
     * Generate line chart showing average response time over time.
     * 
     * @throws IOException if there's an error reading data or writing chart
     */
    public void generateResponseTimeLineChart() throws IOException {
        logger.info("Generating response time line chart");
        
        // Read task metrics
        List<Map<String, Object>> taskMetrics = readTaskMetrics();
        
        // Create dataset
        XYSeries responseSeries = new XYSeries("Average Response Time");
        XYSeries executionSeries = new XYSeries("Average Execution Time");
        
        // Process data
        for (Map<String, Object> record : taskMetrics) {
            double time = (Double) record.get("Time");
            double responseTime = (Double) record.get("AvgResponseTime");
            double executionTime = (Double) record.get("AvgExecutionTime");
            
            responseSeries.add(time, responseTime);
            executionSeries.add(time, executionTime);
        }
        
        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(responseSeries);
        dataset.addSeries(executionSeries);
        
        // Create chart
        JFreeChart chart = ChartFactory.createXYLineChart(
                "Task Time Metrics Over Time",
                "Time (seconds)",
                "Time (seconds)",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false);
        
        // Customize chart
        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.white);
        plot.setDomainGridlinePaint(Color.lightGray);
        plot.setRangeGridlinePaint(Color.lightGray);
        
        // Customize renderer
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesStroke(0, new BasicStroke(2.0f));
        renderer.setSeriesPaint(0, Color.BLUE);
        renderer.setSeriesShapesVisible(0, false);
        renderer.setSeriesStroke(1, new BasicStroke(2.0f));
        renderer.setSeriesPaint(1, Color.RED);
        renderer.setSeriesShapesVisible(1, false);
        plot.setRenderer(renderer);
        
        // Save chart
        ChartUtils.saveChartAsPNG(new File(outputDir + "/response_times.png"), chart, 800, 600);
    }
    
    /**
     * Generate bar chart comparing task completion metrics.
     * 
     * @throws IOException if there's an error reading data or writing chart
     */
    public void generateTaskCompletionBarChart() throws IOException {
        logger.info("Generating task completion bar chart");
        
        // Read final task metrics
        Map<String, Double> finalMetrics = readFinalTaskMetrics();
        
        // Create dataset
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        
        dataset.addValue(finalMetrics.getOrDefault("LocalTasks", 0.0), 
                      "Task Count", "Local");
        dataset.addValue(finalMetrics.getOrDefault("EdgeTasks", 0.0), 
                      "Task Count", "Edge");
        dataset.addValue(finalMetrics.getOrDefault("CloudTasks", 0.0), 
                      "Task Count", "Cloud");
        
        dataset.addValue(finalMetrics.getOrDefault("DeadlineMetPercentage", 0.0), 
                      "Deadline Met %", "All Tasks");
        
        // Create chart
        JFreeChart chart = ChartFactory.createBarChart(
                "Task Completion Metrics",
                "Execution Location",
                "Count / Percentage",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false);
        
        // Customize chart
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.white);
        plot.setDomainGridlinePaint(Color.lightGray);
        plot.setRangeGridlinePaint(Color.lightGray);
        
        // Save chart
        ChartUtils.saveChartAsPNG(new File(outputDir + "/task_completion.png"), chart, 800, 600);
    }
    
    /**
     * Read device metrics from CSV file.
     * 
     * @return List of device metrics records
     * @throws IOException if there's an error reading the file
     */
    private List<Map<String, Object>> readDeviceMetrics() throws IOException {
        return readCsvFile(resultsDir + "/device_metrics.csv");
    }
    
    /**
     * Read edge node metrics from CSV file.
     * 
     * @return List of edge node metrics records
     * @throws IOException if there's an error reading the file
     */
    private List<Map<String, Object>> readEdgeNodeMetrics() throws IOException {
        return readCsvFile(resultsDir + "/edge_node_metrics.csv");
    }
    
    /**
     * Read task metrics from CSV file.
     * 
     * @return List of task metrics records
     * @throws IOException if there's an error reading the file
     */
    private List<Map<String, Object>> readTaskMetrics() throws IOException {
        return readCsvFile(resultsDir + "/task_metrics.csv");
    }
    
    /**
     * Read cloud metrics from CSV file.
     * 
     * @return List of cloud metrics records
     * @throws IOException if there's an error reading the file
     */
    private List<Map<String, Object>> readCloudMetrics() throws IOException {
        return readCsvFile(resultsDir + "/cloud_metrics.csv");
    }
    
    /**
     * Read a CSV file into a list of maps.
     * 
     * @param filePath Path to the CSV file
     * @return List of records as maps
     * @throws IOException if there's an error reading the file
     */
    private List<Map<String, Object>> readCsvFile(String filePath) throws IOException {
        List<Map<String, Object>> records = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            // Read header
            String line = reader.readLine();
            if (line == null) {
                return records;
            }
            
            String[] headers = line.split(",");
            
            // Read data rows
            while ((line = reader.readLine()) != null) {
                String[] values = line.split(",");
                Map<String, Object> record = new HashMap<>();
                
                for (int i = 0; i < headers.length && i < values.length; i++) {
                    String value = values[i].trim();
                    
                    // Convert numeric values
                    if (value.matches("-?\\d+(\\.\\d+)?")) {
                        if (value.contains(".")) {
                            record.put(headers[i], Double.parseDouble(value));
                        } else {
                            record.put(headers[i], Integer.parseInt(value));
                        }
                    } else {
                        record.put(headers[i], value);
                    }
                }
                
                records.add(record);
            }
        }
        
        return records;
    }
    
    /**
     * Read final task metrics.
     * 
     * @return Map of metric names to values
     * @throws IOException if there's an error reading the file
     */
    private Map<String, Double> readFinalTaskMetrics() throws IOException {
        List<Map<String, Object>> taskMetrics = readTaskMetrics();
        
        // Get metrics from last record
        if (!taskMetrics.isEmpty()) {
            Map<String, Object> lastRecord = taskMetrics.get(taskMetrics.size() - 1);
            Map<String, Double> finalMetrics = new HashMap<>();
            
            for (Map.Entry<String, Object> entry : lastRecord.entrySet()) {
                if (entry.getValue() instanceof Number) {
                    finalMetrics.put(entry.getKey(), ((Number) entry.getValue()).doubleValue());
                }
            }
            
            return finalMetrics;
        }
        
        return new HashMap<>();
    }
    
    /**
     * Main method to run the visualizer.
     *
     * @param args Command-line arguments
     */
    public static void main(String[] args) {
        try {
            // Default paths
            String resultsDir = "results";
            String chartsDir = "results/charts";
            
            // Use paths from arguments if provided
            if (args.length > 0) {
                resultsDir = args[0];
            }
            if (args.length > 1) {
                chartsDir = args[1];
            }
            
            // Create and run visualizer
            ResultsVisualizer visualizer = new ResultsVisualizer(resultsDir, chartsDir);
            visualizer.generateAllCharts();
            
            logger.info("Visualization completed. Charts saved to {}", chartsDir);
            
        } catch (Exception e) {
            logger.error("Error generating visualizations", e);
            e.printStackTrace();
        }
    }
}
