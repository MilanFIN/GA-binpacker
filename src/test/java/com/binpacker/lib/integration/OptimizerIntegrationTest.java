package com.binpacker.lib.integration;

import com.binpacker.lib.common.Bin;
import com.binpacker.lib.common.Box;
import com.binpacker.lib.common.Point3f;
import com.binpacker.lib.ocl.JOCLHelper;
import com.binpacker.lib.ocl.OpenCLDevice;
import com.binpacker.lib.optimizer.CPUOptimizer;
import com.binpacker.lib.optimizer.GPUOptimizer;
import com.binpacker.lib.solver.common.SolverProperties;
import com.binpacker.lib.solver.cpusolvers.BestFitEMS;
import com.binpacker.lib.solver.cpusolvers.SolverInterface;
import com.binpacker.lib.solver.parallelsolvers.BestFitEMSReference;
import com.binpacker.lib.solver.parallelsolvers.opencl.OpenCLSolver;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class OptimizerIntegrationTest {

    private List<Box> generateRandomBoxes(int count) {
        List<Box> boxes = new ArrayList<>();
        Random random = new Random(42);
        for (int i = 0; i < count; i++) {
            float width = random.nextInt(8) + 4;
            float height = random.nextInt(8) + 4;
            float depth = random.nextInt(8) + 4;
            float weight = random.nextInt(10) + 1;
            Box box = new Box(
                    new Point3f(0, 0, 0),
                    new Point3f(width, height, depth));
            box.id = i;
            box.weight = weight;
            boxes.add(box);
        }
        return boxes;
    }

    @Test
    public void testCPUOptimizerWithEMS() throws Exception {
        CPUOptimizer cpuOptimizer = new CPUOptimizer();
        List<Box> boxes = generateRandomBoxes(25);
        Bin bin = new Bin(0, 30, 30, 30);
        bin.maxWeight = 0;

        List<Integer> rotationAxes = Arrays.asList(0, 1, 2);
        boolean growingBin = false;
        String axis = "x";
        int population = 30;
        int eliteCount = 3;
        int threads = 0;

        java.util.function.Supplier<SolverInterface> factory = () -> {
            try {
                BestFitEMS s = new BestFitEMS();
                Bin freshBin = new Bin(bin.index, bin.w, bin.h, bin.d);
                freshBin.maxWeight = bin.maxWeight;
                SolverProperties freshProps = new SolverProperties(freshBin, growingBin, axis, rotationAxes, null,
                        bin.maxWeight);
                s.init(freshProps);
                return s;
            } catch (Exception ex) {
                throw new RuntimeException("Failed to create solver instance", ex);
            }
        };

        cpuOptimizer.initialize(factory, boxes, bin, growingBin, axis, rotationAxes, population, eliteCount, threads);

        List<List<Box>> result = null;
        try {
            for (int i = 0; i < 2; i++) {
                result = cpuOptimizer.executeNextGeneration();
            }

            assertNotNull(result, "Result should not be null after execution");
            assertFalse(result.isEmpty(), "There should be at least one bin used");

            int totalPlaced = result.stream().mapToInt(List::size).sum();
            assertTrue(totalPlaced > 0, "Should have placed some boxes");
        } finally {
            cpuOptimizer.release();
        }
    }

    @Test
    public void testGPUOptimizerWithEMS() throws Exception {
        List<OpenCLDevice> devices = JOCLHelper.getAvailableDevices();
        if (devices == null || devices.isEmpty()) {
            System.out.println("No OpenCL devices found. Skipping GPU integration test.");
            return;
        }
        // picking the last one
        OpenCLDevice device = devices.get(devices.size() - 1);
        // print device name
        System.out.println("Using device: " + device.toString());

        GPUOptimizer gpuOptimizer = new GPUOptimizer();
        List<Box> boxes = generateRandomBoxes(30);
        Bin bin = new Bin(0, 30, 30, 30);
        bin.maxWeight = 0;

        List<Integer> rotationAxes = Arrays.asList(0, 1, 2);
        boolean growingBin = false;
        String axis = "x";
        int population = 30;
        int eliteCount = 3;

        OpenCLSolver parallelSolver = new OpenCLSolver("bestfit_ems.cl.template", "best_fit_ems", "BestFit EMS GPU",
                new BestFitEMSReference());
        SolverProperties properties = new SolverProperties(bin, growingBin, axis, rotationAxes, device, bin.maxWeight);
        parallelSolver.init(properties);

        gpuOptimizer.initialize(parallelSolver, boxes, bin, growingBin, axis, rotationAxes, population, eliteCount,
                0);

        List<List<Box>> result = null;
        try {
            for (int i = 0; i < 2; i++) {
                result = gpuOptimizer.executeNextGeneration();
            }

            assertNotNull(result, "Result should not be null after execution");
            assertFalse(result.isEmpty(), "There should be at least one bin used");

            int totalPlaced = result.stream().mapToInt(List::size).sum();
            assertTrue(totalPlaced > 0, "Should have placed some boxes");
        } finally {
            gpuOptimizer.release();
        }
    }
}
