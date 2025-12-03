package com.binpacker.app;

import com.binpacker.lib.ocl.JOCLHelper;

import javafx.application.Application;

public class Main {
	public static void main(String[] args) {

		JOCLHelper.testOpenCL();
		Application.launch(GuiApp.class, args);
	}
}
