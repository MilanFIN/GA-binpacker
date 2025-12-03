package com.binpacker.lib.ocl;

import org.jocl.*;

public class JOCLHelper {

	static {
		// Enable JOCL exceptions for easier debugging
		CL.setExceptionsEnabled(true);
	}

	/**
	 * Prints all OpenCL platforms and devices.
	 * Can be called from any existing program to verify OpenCL is working.
	 */
	public static void testOpenCL() {
		// Get number of platforms
		int[] numPlatformsArray = new int[1];
		CL.clGetPlatformIDs(0, null, numPlatformsArray);
		int numPlatforms = numPlatformsArray[0];
		System.out.println("Number of OpenCL platforms: " + numPlatforms);

		if (numPlatforms == 0) {
			System.out.println("No OpenCL platforms found!");
			return;
		}

		// Get platform IDs
		cl_platform_id[] platforms = new cl_platform_id[numPlatforms];
		CL.clGetPlatformIDs(numPlatforms, platforms, null);

		// Iterate over platforms
		for (int i = 0; i < numPlatforms; i++) {
			System.out.println("Platform " + i + ":");
			printPlatformInfo(platforms[i], CL.CL_PLATFORM_NAME);
			printPlatformInfo(platforms[i], CL.CL_PLATFORM_VENDOR);
			printPlatformInfo(platforms[i], CL.CL_PLATFORM_VERSION);

			// Get devices for this platform
			int[] numDevicesArray = new int[1];
			int result = CL.clGetDeviceIDs(platforms[i], CL.CL_DEVICE_TYPE_ALL, 0, null, numDevicesArray);
			int numDevices = (result == CL.CL_SUCCESS) ? numDevicesArray[0] : 0;

			if (numDevices == 0) {
				System.out.println("  No devices found on this platform.");
				continue;
			}

			cl_device_id[] devices = new cl_device_id[numDevices];
			CL.clGetDeviceIDs(platforms[i], CL.CL_DEVICE_TYPE_ALL, numDevices, devices, null);

			for (int d = 0; d < devices.length; d++) {
				System.out.print("  Device " + d + ": ");
				printDeviceInfo(devices[d], CL.CL_DEVICE_NAME);
			}
		}
	}

	private static void printPlatformInfo(cl_platform_id platform, int paramName) {
		long[] size = new long[1];
		CL.clGetPlatformInfo(platform, paramName, 0, null, size);

		byte[] buffer = new byte[(int) size[0]];
		CL.clGetPlatformInfo(platform, paramName, buffer.length, Pointer.to(buffer), null);

		System.out.println("    " + new String(buffer).trim());
	}

	private static void printDeviceInfo(cl_device_id device, int paramName) {
		long[] size = new long[1];
		CL.clGetDeviceInfo(device, paramName, 0, null, size);

		byte[] buffer = new byte[(int) size[0]];
		CL.clGetDeviceInfo(device, paramName, buffer.length, Pointer.to(buffer), null);

		System.out.println(new String(buffer).trim());
	}
}
