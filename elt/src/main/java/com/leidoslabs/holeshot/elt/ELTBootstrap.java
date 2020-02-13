package com.leidoslabs.holeshot.elt;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;

import com.google.common.collect.ImmutableMap;

public class ELTBootstrap {
	final static Map<String, File> NATIVE_LIBS = 
			ImmutableMap.of(getSWTNativeLib(), getNativeLib("swt-native.jar"),
					getGLCanvasLib(), getNativeLib("glcanvas.jar"));

	//private static final Logger LOGGER = LoggerFactory.getLogger(ELTBootstrap.class);

	public static void main(String[] args) {
		//		ClassLoader cl = setSystemClassloader();
		try {
			if (!restartJVM()) {
				for (Entry<String, File> e: NATIVE_LIBS.entrySet()) {
					extractNative(e.getKey(), e.getValue());
				}

				ClassLoader cl = Thread.currentThread().getContextClassLoader();
				Class<?> clazz = cl.loadClass("com.leidoslabs.holeshot.elt.ELT");
				Method method = clazz.getMethod("main", String[].class);
				Object[] objargs = { args };
				method.invoke(null, objargs);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static boolean restartJVM() throws IOException {
		boolean restart = false;

		// if not a mac return false
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		System.out.println("cl = " + cl.getClass().getName());
		boolean firstThread = true;

		// get current jvm process pid
		String pid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];

		final String os = System.getProperty("os.name");
		final boolean isMac = (os.startsWith("Mac") || os.startsWith("Darwin"));
		if (isMac) {
			// get environment variable on whether XstartOnFirstThread is enabled
			String env = System.getenv("JAVA_STARTED_ON_FIRST_THREAD_" + pid);

			// if environment variable is "1" then XstartOnFirstThread is enabled
			firstThread = (env != null && env.equals("1"));
		}

		final String originalClasspath = System.getProperty("java.class.path");

		File[] missingLibraries = NATIVE_LIBS.values().stream().filter(s-> !originalClasspath.contains(s.getName())).toArray(File[]::new);


		if (!firstThread || missingLibraries.length > 0)  {
			restart = true;

			// restart jvm with -XstartOnFirstThread
			String separator = System.getProperty("file.separator");
			//String mainClass = System.getenv("JAVA_MAIN_CLASS_" + pid);
			String mainClass = ELTBootstrap.class.getName();
			String jvmPath = System.getProperty("java.home") + separator + "bin" + separator + "java";

			String classpath = originalClasspath;


			if (missingLibraries.length > 0) {
				classpath = String.join(File.pathSeparator, classpath, Arrays.stream(missingLibraries).map(s->getCanonicalPath(s)).collect(Collectors.joining(File.pathSeparator)));
			}

			System.out.println("cp == " + classpath);
			List<String> inputArguments = ManagementFactory.getRuntimeMXBean().getInputArguments();

			ArrayList<String> jvmArgs = new ArrayList<String>();

			jvmArgs.add(jvmPath);

			if (isMac) {
				jvmArgs.add("-XstartOnFirstThread");
			}
			jvmArgs.addAll(inputArguments);
			jvmArgs.add("-cp");
			jvmArgs.add(classpath);
			jvmArgs.add(mainClass);

			// if you don't need console output, just enable these two lines 
			// and delete bits after it. This JVM will then terminate.
			//ProcessBuilder processBuilder = new ProcessBuilder(jvmArgs);
			//processBuilder.start();

			try {
				ProcessBuilder processBuilder = new ProcessBuilder(jvmArgs);
				processBuilder.redirectErrorStream(true);
				Process process = processBuilder.start();

				InputStream is = process.getInputStream();
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				String line;

				while ((line = br.readLine()) != null) {
					System.out.println(line);
				}

				process.waitFor();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return restart;
	}

	private static String getCanonicalPath(File file) {
		String path = null;
		try {
			path = file.getCanonicalPath();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return path;
	}

	private static String getSWTNativeLib() {
		String result = null;
		if (SystemUtils.IS_OS_MAC) {
			result = "org.eclipse.swt.cocoa.macosx.x86_64.jar";
		} else if (SystemUtils.IS_OS_LINUX) {
			result = "org.eclipse.swt.gtk.linux.x86_64.jar";
		} else {
			result = "org.eclipse.swt.win32.win32.x86_64.jar";
		}
		return result;
	}

	private static String getGLCanvasLib() {
		String result = null;
		if (SystemUtils.IS_OS_MAC) {
			result = "com.leidoslabs.holeshot.glcanvas-macosx.jar";
		} else if (SystemUtils.IS_OS_LINUX) {
			result = "com.leidoslabs.holeshot.glcanvas-linux.jar";
		} else {
			result = "com.leidoslabs.holeshot.glcanvas-windows.jar";
		}
		return result;
	}

	private static void extractNative(String resource, File destFile) throws IOException {
		try (InputStream is = ELTBootstrap.class.getClassLoader().getResourceAsStream(resource)) {
			FileUtils.copyInputStreamToFile(is, destFile);
		}
	}

	private static File getNativeLib(String filename) {
		return new File(getNativeDirectory(), filename);
	}

	private static File getNativeDirectory() {
		String path = System.getProperty("user.home") + File.separator + ".swt";
		File swtDir = new File(path);
		swtDir.mkdirs();
		return swtDir;
	}

}
