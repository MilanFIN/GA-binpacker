package com.binpacker.lib.solver.common.cuda;

import com.binpacker.lib.ocl.OpenCLDevice;
import jcuda.Pointer;
import jcuda.driver.CUcontext;
import jcuda.driver.CUdevice;
import jcuda.driver.CUmodule;
import jcuda.driver.JCudaDriver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

import static jcuda.driver.JCudaDriver.*;

public class CudaCommon {

    public CUdevice cuDevice;
    public CUcontext cuContext;

    public CudaCommon() {
        setExceptionsEnabled(true);
    }

    public void init(int deviceIndex) {
        System.out.println("Initializing JCuda...");
        cuInit(0);

        int[] numDevices = new int[1];
        cuDeviceGetCount(numDevices);
        if (numDevices[0] <= 0) {
            System.err.println("No CUDA devices found.");
            return;
        }

        int targetDevice = (deviceIndex >= 0 && deviceIndex < numDevices[0]) ? deviceIndex : 0;
        
        cuDevice = new CUdevice();
        cuDeviceGet(cuDevice, targetDevice);

        byte[] name = new byte[256];
        cuDeviceGetName(name, 256, cuDevice);
        System.out.println("Selected CUDA Device: " + new String(name).trim());

        cuContext = new CUcontext();
        cuCtxCreate(cuContext, 0, cuDevice);
    }

    public CUmodule loadModuleFromSource(String sourceCode, String kernelName) {
        try {
            // Write source code to temporary .cu file
            Path tempDir = Files.createTempDirectory("jcuda_compilation");
            File cuFile = new File(tempDir.toFile(), kernelName + ".cu");
            try (FileOutputStream fos = new FileOutputStream(cuFile)) {
                fos.write(sourceCode.getBytes());
            }

            File ptxFile = new File(tempDir.toFile(), kernelName + ".ptx");

            // Compile via nvcc system command
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "nvcc", "-ptx", cuFile.getAbsolutePath(), "-o", ptxFile.getAbsolutePath()
            );
            
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("nvcc: " + line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("nvcc compilation failed with exit code: " + exitCode);
                return null;
            }

            // Load module from compiled PTX
            CUmodule module = new CUmodule();
            cuModuleLoad(module, ptxFile.getAbsolutePath());

            // Clean up temp files
            cuFile.delete();
            ptxFile.delete();
            tempDir.toFile().delete();

            return module;

        } catch (Exception e) {
            System.err.println("Failed to compile CUDA kernel: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public void release() {
        if (cuContext != null) {
            cuCtxDestroy(cuContext);
            cuContext = null;
        }
    }
}
