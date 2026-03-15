package com.binpacker.lib.solver.parallelsolvers.cuda;

import java.util.ArrayList;
import java.util.List;

import com.binpacker.lib.common.Bin;
import com.binpacker.lib.common.Box;
import com.binpacker.lib.ocl.KernelUtils;
import com.binpacker.lib.solver.common.SolverProperties;
import com.binpacker.lib.solver.common.cuda.CudaCommon;
import com.binpacker.lib.solver.parallelsolvers.ParallelSolverInterface;
import com.binpacker.lib.solver.parallelsolvers.ReferenceSolver;

import jcuda.Pointer;
import jcuda.Sizeof;
import jcuda.driver.CUdeviceptr;
import jcuda.driver.CUfunction;
import jcuda.driver.CUmodule;
import jcuda.driver.JCudaDriver;

public class CudaSolver implements ParallelSolverInterface {

    private CudaCommon cudaCommon;
    private CUmodule module;
    private CUfunction kernel;
    private String kernelSource;
    private Bin binTemplate;
    private List<Integer> rotationAxes;

    private final String kernelFileName;
    private final String kernelFunctionName;
    private final String displayName;
    private final ReferenceSolver referenceSolver;

    public CudaSolver(String kernelFileName, String kernelFunctionName, String displayName, ReferenceSolver referenceSolver) {
        this.kernelFileName = kernelFileName;
        this.kernelFunctionName = kernelFunctionName;
        this.displayName = displayName;
        this.referenceSolver = referenceSolver;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ReferenceSolver getReferenceSolver() {
        return referenceSolver;
    }

    public boolean isTemplate() {
        return kernelFileName.endsWith(".template");
    }

    public boolean isCompiled() {
        return module != null;
    }

    public void compileKernel(int maxBins, int maxSpaces) {
        if (module != null)
            return; // Already compiled

        if (cudaCommon == null) {
            try {
                cudaCommon = new CudaCommon();
            } catch (Throwable t) {
                System.err.println("CUDA not available (missing driver or nvcc): " + t.getMessage());
                return;
            }
        }

        String source = kernelSource
                .replace("{{MAX_BINS}}", String.valueOf(maxBins))
                .replace("{{MAX_SPACES_PER_BIN}}", String.valueOf(maxSpaces));

        // Initialize CUDA & compile PTX
        try {
            cudaCommon.init(0); // Defaulting to device 0 for now
            module = cudaCommon.loadModuleFromSource(source, kernelFunctionName);
            
            if (module != null) {
                kernel = new CUfunction();
                JCudaDriver.cuModuleGetFunction(kernel, module, kernelFunctionName);
            }
        } catch (Throwable t) {
            System.err.println("CUDA execution failed: " + t.getMessage());
        }
    }

    @Override
    public void init(SolverProperties properties) {
        this.binTemplate = properties.bin;
        this.rotationAxes = properties.rotationAxes;
        this.kernelSource = KernelUtils.loadKernelSource(kernelFileName);

        if (!isTemplate()) {
            if (cudaCommon == null) {
                try {
                    cudaCommon = new CudaCommon();
                } catch (Throwable t) {
                    System.err.println("CUDA not available: " + t.getMessage());
                    return;
                }
            }
            try {
                cudaCommon.init(0);
                module = cudaCommon.loadModuleFromSource(kernelSource, kernelFunctionName);
                if (module != null) {
                    kernel = new CUfunction();
                    JCudaDriver.cuModuleGetFunction(kernel, module, kernelFunctionName);
                }
            } catch (Throwable t) {
                System.err.println("CUDA initialization failed: " + t.getMessage());
            }
        }
    }

    @Override
    public List<Double> solve(List<Box> boxes, List<List<Integer>> orders) {
        int numBoxes = boxes.size();
        int numOrders = orders.size();

        if (numBoxes == 0 || numOrders == 0) {
            return new ArrayList<>();
        }

        if (module == null || kernel == null) {
            System.err.println("CUDA Solver not compiled or initialized correctly.");
            List<Double> fallback = new ArrayList<>(numOrders);
            for (int i = 0; i < numOrders; i++) fallback.add(-1.0);
            return fallback;
        }

        // 1. Prepare data
        float[] boxData = new float[numBoxes * 4];
        for (int i = 0; i < numBoxes; i++) {
            Box b = boxes.get(i);
            boxData[i * 4 + 0] = b.size.x;
            boxData[i * 4 + 1] = b.size.y;
            boxData[i * 4 + 2] = b.size.z;
            boxData[i * 4 + 3] = b.weight;
        }

        int[] orderData = new int[numOrders * numBoxes];
        for (int i = 0; i < numOrders; i++) {
            List<Integer> order = orders.get(i);
            for (int j = 0; j < numBoxes; j++) {
                orderData[i * numBoxes + j] = order.get(j);
            }
        }

        float[] scores = new float[numOrders];

        // 2. Allocate buffers
        CUdeviceptr boxesMem = new CUdeviceptr();
        JCudaDriver.cuMemAlloc(boxesMem, boxData.length * Sizeof.FLOAT);
        JCudaDriver.cuMemcpyHtoD(boxesMem, Pointer.to(boxData), boxData.length * Sizeof.FLOAT);

        CUdeviceptr ordersMem = new CUdeviceptr();
        JCudaDriver.cuMemAlloc(ordersMem, orderData.length * Sizeof.INT);
        JCudaDriver.cuMemcpyHtoD(ordersMem, Pointer.to(orderData), orderData.length * Sizeof.INT);

        CUdeviceptr scoresMem = new CUdeviceptr();
        JCudaDriver.cuMemAlloc(scoresMem, numOrders * Sizeof.FLOAT);

        // Calculate rotation mask
        int rotationMask = 0;
        if (this.rotationAxes != null) {
            if (this.rotationAxes.contains(0))
                rotationMask |= 1;
            if (this.rotationAxes.contains(1))
                rotationMask |= 2;
            if (this.rotationAxes.contains(2))
                rotationMask |= 4;
        }

        // 3. Set kernel args
        Pointer kernelParameters = Pointer.to(
            Pointer.to(boxesMem),
            Pointer.to(ordersMem),
            Pointer.to(scoresMem),
            Pointer.to(new int[]{numBoxes}),
            Pointer.to(new float[]{binTemplate.w}),
            Pointer.to(new float[]{binTemplate.h}),
            Pointer.to(new float[]{binTemplate.d}),
            Pointer.to(new float[]{binTemplate.maxWeight}),
            Pointer.to(new int[]{rotationMask})
        );

        // 4. Run kernel
        // Calculate block and grid dimensions
        int blockSizeX = 256;
        int gridSizeX = (int) Math.ceil((double) numOrders / blockSizeX);

        JCudaDriver.cuLaunchKernel(kernel,
            gridSizeX, 1, 1,      // Grid dimension
            blockSizeX, 1, 1,      // Block dimension
            0, null,               // Shared memory size and stream
            kernelParameters, null // Kernel- and extra parameters
        );
        JCudaDriver.cuCtxSynchronize();

        // 5. Read results
        JCudaDriver.cuMemcpyDtoH(Pointer.to(scores), scoresMem, numOrders * Sizeof.FLOAT);

        // 6. Convert results
        List<Double> resultList = new ArrayList<>(numOrders);
        for (float score : scores) {
            resultList.add((double) score);
        }

        // 7. Cleanup memory
        JCudaDriver.cuMemFree(boxesMem);
        JCudaDriver.cuMemFree(ordersMem);
        JCudaDriver.cuMemFree(scoresMem);

        return resultList;
    }

    @Override
    public void release() {
        if (module != null) {
            JCudaDriver.cuModuleUnload(module);
            module = null;
        }
        if (cudaCommon != null) {
            cudaCommon.release();
        }
    }
}
