package com.binpacker.lib.solver.parallelsolvers.cuda;

import com.binpacker.lib.common.Bin;
import com.binpacker.lib.common.Box;
import com.binpacker.lib.common.Point3f;
import com.binpacker.lib.solver.common.SolverProperties;
import com.binpacker.lib.solver.parallelsolvers.FirstFitReference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CudaTestRun {
    public static void main(String[] args) {
        System.out.println("Starting CudaTestRun...");
        CudaSolver solver = new CudaSolver(
            "firstfit_complete.cu.template", 
            "guillotine_first_fit", 
            "test_cuda", 
            new FirstFitReference()
        );
        Bin bin = new Bin(0, 10, 10, 10);
        bin.maxWeight = 1000;
        SolverProperties props = new SolverProperties(bin, false, "z", Arrays.asList(0, 1, 2), null, 1000);
        solver.init(props);
        solver.compileKernel(10, 100);

        List<Box> boxes = new ArrayList<>();
        boxes.add(new Box(0, new Point3f(0,0,0), new Point3f(5,5,5)));
        boxes.add(new Box(1, new Point3f(0,0,0), new Point3f(5,5,5)));
        List<List<Integer>> orders = new ArrayList<>();
        orders.add(Arrays.asList(0, 1));
        
        List<Double> scores = solver.solve(boxes, orders);
        System.out.println("CUDA Test Result scores: " + scores);
        solver.release();
    }
}
