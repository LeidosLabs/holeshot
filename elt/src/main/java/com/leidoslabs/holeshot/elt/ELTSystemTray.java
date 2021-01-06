/*
 * Licensed to Leidos, Inc. under one or more contributor license agreements.  
 * See the NOTICE file distributed with this work for additional information regarding copyright ownership.
 * Leidos, Inc. licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.leidoslabs.holeshot.elt;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JOptionPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.leidoslabs.holeshot.elt.tileserver.TileserverImage;

import dorkbox.systemTray.MenuItem;
import dorkbox.systemTray.SystemTray;

/**
 * The ELT is intended to be run as a Windows service and is accessible via the Windows Systems Tray.
 */
public class ELTSystemTray {
	private static final Logger LOGGER = LoggerFactory.getLogger(ELTSystemTray.class);

	private final List<Runnable> exitListeners;
	private final Random random;

	/**
	 * Initialize Exit listeners with exit listener, and add to system tray 
	 * @param exitListener
	 */
	public ELTSystemTray(Runnable exitListener) {
		random = new Random();
		exitListeners = new ArrayList<Runnable>();
		exitListeners.add(exitListener);
		addApplicationToSystemTray();
	}

	/**
	 * Attempts to get the singleton instance of SystemTray.
	 * If the first call, attempts to initialize the SystemTray.
	 * Because this will also actually initialize the one instance of systemtray within the VM, we must
	 * make sure to set any properties first. ENABLE_SHUTDOWN_HOOK=false prevents a crash on Gnome,
	 * and we create our own shutdown hooks anyways.
	 * @return True if the SystemTray is supported
	 */
	public static boolean isSupported() {
		SystemTray.ENABLE_SHUTDOWN_HOOK = false;
		return SystemTray.get() != null;
	}

	private void addApplicationToSystemTray() {

		if (!isSupported()) {
			throw new RuntimeException("Unable to load SystemTray");
		}

		SystemTray systemTray = SystemTray.get();

		String imagePath = "images/smallIcon.png";
		try {
			systemTray.setImage(ELTSystemTray.class.getResource(imagePath));
		} catch(Exception e) {
			LOGGER.error("Image Resource not loaded: " + imagePath);
		}

		MenuItem aboutItem = new MenuItem("About");
		MenuItem loadTestMosaicItem = new MenuItem("Load Test Mosaic");
		MenuItem exitItem = new MenuItem("Exit");

		systemTray.getMenu().add(aboutItem).setCallback(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(null,
						"LeidosLabs ELT");
			}
		});

		systemTray.getMenu().add(loadTestMosaicItem).setCallback(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				loadTestImages();
			}
		});

		systemTray.getMenu().add(exitItem).setCallback(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				exitListeners.stream().forEach(l->l.run());
				systemTray.shutdown();
			}
		});

	}

	private void loadTestImages() {
		try {
			final String tileserver = "tileserver.leidoslabs.com";
			final String protocol = "https";
			final String app = String.format("app%d", random.nextInt());
			ELTFrame newFrame = ELT.getInstance().openFrame(app, true);
			ELTCanvas newCanvas = newFrame.getELTCanvas();

			final AtomicReference<TileserverImage> lastImage = new AtomicReference<TileserverImage>();
			Arrays.stream(new String[] {
					"XVIEWCHALLENGE-00107/20180116001820",
					"XVIEWCHALLENGE-00108/20180116002104",
					"XVIEWCHALLENGE-00109/20180116002258",
					"XVIEWCHALLENGE-00110/20180116002549",
					"XVIEWCHALLENGE-00111/20180116002652",
					"XVIEWCHALLENGE-00112/20180116002847",
					"XVIEWCHALLENGE-00118/20180116003602",
					"XVIEWCHALLENGE-00121/20180116004000",
					"XVIEWCHALLENGE-00122/20180116004138",
					"XVIEWCHALLENGE-00124/20180116004245",
					"XVIEWCHALLENGE-00125/20180116004340",
					"XVIEWCHALLENGE-00126/20180116004430",
					"XVIEWCHALLENGE-00128/20180116004603",
					"XVIEWCHALLENGE-00129/20180116004647",
					"XVIEWCHALLENGE-00130/20180116004709",
					"XVIEWCHALLENGE-00131/20180116004810",
					"XVIEWCHALLENGE-00136/20180116005103",
					"XVIEWCHALLENGE-00140/20180116005418",
					"XVIEWCHALLENGE-00142/20180116005649",
					"XVIEWCHALLENGE-00143/20180116005858",
					"XVIEWCHALLENGE-00144/20180116010109",
					"XVIEWCHALLENGE-00145/20180116010303",
					"XVIEWCHALLENGE-00147/20180116010654",
					"XVIEWCHALLENGE-00149/20180116011009",
					"XVIEWCHALLENGE-00157/20180116011529",
					"XVIEWCHALLENGE-00158/20180116011621",
					"XVIEWCHALLENGE-00159/20180116011723",
					"XVIEWCHALLENGE-00163/20180116012022"
			}).map(s->String.format("%s://%s/tileserver/%s/metadata.json", protocol, tileserver, s))
			.map(s-> {
				TileserverImage result = null;
				try {
					result = new TileserverImage(new URL(s));
					lastImage.set(result);
				} catch (IOException e) {
					e.printStackTrace();
				}
				return result;
				})
			.forEach(i->{
				newCanvas.getDisplay().asyncExec(()->{
					try {
						newCanvas.addImage(i, false);
					} catch (IOException | InterruptedException | ExecutionException e) {
						e.printStackTrace();
					}
				});
			});
			newCanvas.zoomToImage(lastImage.get());

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}