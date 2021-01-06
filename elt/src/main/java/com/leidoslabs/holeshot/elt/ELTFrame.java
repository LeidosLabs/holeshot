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

import java.awt.Toolkit;
import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.joml.Vector2ic;
import org.locationtech.jts.geom.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.leidoslabs.holeshot.elt.observations.Observation;
import com.leidoslabs.holeshot.elt.tileserver.TileserverImage;
import com.leidoslabs.holeshot.elt.tileserver.TileserverUrlBuilder;
import com.leidoslabs.holeshot.elt.ui.TabbedToolbar;
import com.leidoslabs.holeshot.elt.ui.TabbedToolbar.TabbedToolbarItem;
import com.leidoslabs.holeshot.elt.viewport.ImageWorld;
import com.leidoslabs.holeshot.elt.viewport.ImageWorldMouseListener;
import com.leidoslabs.holeshot.elt.viewport.ViewportImageListener;
import com.leidoslabs.holeshot.imaging.coord.GeointCoordinate;

@SuppressWarnings("serial")
/**
 * Representation of a frame of the ELTCanvas. Contains and managers ELTCanvas. 
 */
public class ELTFrame extends Shell {
	private static final Logger LOGGER = LoggerFactory.getLogger(ELTFrame.class);

	private static final String DEFAULT_IMAGE_BUTTON_NAME = "image";

	private final ELTCanvas glCanvas;
	private final Label locationLabel;
	private final TabbedToolbar tabbedToolbar;
	protected final BrowserFrame browserFrame;
	private static DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.0000#");
	public static final int DEFAULT_WIDTH = 512;
	public static final int DEFAULT_HEIGHT = 512;
	private final Button webMercatorButton;
	private final Button imageButton;
	private TileserverImage lastCenterImage;
	private Button[] interpolationButtons;


	/**
	 * Construct ELTFrame. Initialize ELTCanvas,
	 * @param display
	 * @param imageMetadataURL
	 * @param appId
	 * @param isDecorated
	 * @param progressiveRender
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	public ELTFrame(Display display, String appId, boolean isDecorated, boolean progressiveRender) throws IOException, InterruptedException, ExecutionException {
		super(display, isDecorated ? SWT.SHELL_TRIM : SWT.NO_TRIM);

		final Color black = display.getSystemColor(SWT.COLOR_BLACK);
		setBackground(black);
		setForeground(black);
		if (isDecorated) {
			setLayout(new GridLayout(2,false));

			// Allows for transparent controls
			tabbedToolbar = new TabbedToolbar(this, SWT.NONE);
			tabbedToolbar.setBackground(black);
			tabbedToolbar.getToolbar().setBackground(black);
			//			tabbedToolbar.getTabFolder().setBackground(black);

			Image image = new Image(display, ELTFrame.class.getClassLoader().getResourceAsStream("com/leidoslabs/holeshot/elt/images/globeicon.png"));
			TabbedToolbarItem projection = tabbedToolbar.addItem(image, "Projections");
			Composite projectionPanel = new Composite(tabbedToolbar.getTabFolder(), SWT.NONE);
			projectionPanel.setLayout(new GridLayout(1, false));

			this.webMercatorButton = createButton(projectionPanel, ImageWorld.WEB_MERCATOR_PROJECTION.getName());
			this.imageButton = createButton(projectionPanel, DEFAULT_IMAGE_BUTTON_NAME);
			this.imageButton.setEnabled(false);
			webMercatorButton.setSelection(true);

			projection.getTabItem().setControl(projectionPanel);
			
			Image settingsImage = new Image(display, ELTFrame.class.getClassLoader().getResourceAsStream("com/leidoslabs/holeshot/elt/images/settings.png"));
			TabbedToolbarItem settings = tabbedToolbar.addItem(settingsImage, "Settings");
			Composite settingsPanel = new Composite(tabbedToolbar.getTabFolder(), SWT.NONE);
			settingsPanel.setLayout(new GridLayout(1, false));

			interpolationButtons = 
					Arrays.stream(Interpolation.values()).map(i->createInterpolationButton(settingsPanel, i)).toArray(Button[]::new);
			settings.getTabItem().setControl(settingsPanel);
			Arrays.stream(interpolationButtons).filter(b->b.getData() == Interpolation.CATMULL).findFirst().orElse(null).setSelection(true);
			
			tabbedToolbar.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, true, 1, 1));

			browserFrame = new BrowserFrame(this);
			browserFrame.open();
			browserFrame.addListener(SWT.Close, e -> {
				e.doit = false;
				browserFrame.setVisible(false);
			});
			browserFrame.setVisible(false);
		} else {
			this.tabbedToolbar = null;
			this.browserFrame = null;
			this.imageButton = null;
			this.webMercatorButton = null;
			setLayout(new GridLayout(1, false));
		}

		// set the JFrame title
		glCanvas = new ELTCanvas(this, appId, progressiveRender);
		glCanvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		GridLayout canvasLayout = new GridLayout(1, false);
		canvasLayout.marginWidth = 0;
		canvasLayout.marginHeight = 0;
		glCanvas.setLayout(canvasLayout);

		if (isDecorated) {
			locationLabel = new Label(glCanvas, SWT.NONE);
			final FontData[] fontData = locationLabel.getFont().getFontData();
			fontData[0].setHeight(12);
			final Font font = new Font(display, fontData[0]);
			locationLabel.setFont(font);
			GridData labelGridData = new GridData(SWT.RIGHT, SWT.BOTTOM, true, true, 1, 1);
			locationLabel.setLayoutData(labelGridData);
			locationLabel.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
			locationLabel.setForeground(display.getSystemColor(SWT.COLOR_GRAY));

			// Set the text to something that takes up the maximum size for this field
			locationLabel.setText(String.format("lat: %s lon: %s", DECIMAL_FORMAT.format(-179.99999), DECIMAL_FORMAT.format(-90.99999)));

			glCanvas.addELTMouseListener(new ImageWorldMouseListener() {
				@Override
				public void mouseMoved(GeointCoordinate<?> eltCoordinate) {
					Coordinate geodeticCoord = eltCoordinate.getGeodeticCoordinate();
					locationLabel.setText(String.format("lat: %s lon: %s", DECIMAL_FORMAT.format(geodeticCoord.y), DECIMAL_FORMAT.format(geodeticCoord.x)));
				}
			});
			glCanvas.addViewportImageListener(new ViewportImageListener() {
				@Override
				public void centerImageChanged(TileserverImage newCenterImage) {
					if (!imageButton.getSelection() || !newCenterImage.equals(lastCenterImage)) {
						setImageButton(newCenterImage);
						lastCenterImage = newCenterImage;
					}
				}
			});
		} else {
			locationLabel = null;
		}

		// center the JFrame on the screen
		centerWindow(this);

		setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);

		setImage(new Image(display, ELTFrame.class.getClassLoader().getResourceAsStream("com/leidoslabs/holeshot/elt/images/icon.png")));
		//      setText(String.format("ELT - %s", glCanvas.getImage().getTilePyramidDescriptor().getName()));

		glCanvas.addNameListener(n -> setText(String.format("ELT - %s", n)));
		//      this.setIconImage(ELTSystemTray.createImage("images/icon.png", "ELT"));


		this.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (((e.stateMask & (SWT.SHIFT | SWT.CONTROL)) == ((SWT.SHIFT | SWT.CONTROL))) && e.keyCode == 'T') {
					//new TileFetcher(ELTFrame.this, glCanvas.getTopTile());
				}
			}});

		this.open();
	}

	private void setImageButton(TileserverImage image) {
		String newText;
		boolean enabled;
		if (enabled = (image != null)) {
			newText = String.format("%s: %s", DEFAULT_IMAGE_BUTTON_NAME, image.getName());
		} else {
			newText = DEFAULT_IMAGE_BUTTON_NAME;
		}
		imageButton.setEnabled(enabled);
		imageButton.setText(newText);
	}

	private Button createButton(Composite projectionPanel, String name) {
		final Button button = new Button(projectionPanel, SWT.RADIO);
		button.setText(name);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (button.getSelection()) {
					changeProjection(button);
				}
			}
		});
		return button;
	}

	private Button createInterpolationButton(Composite settingsPanel, Interpolation interpolation) {
		final Button button = new Button(settingsPanel, SWT.RADIO);
		button.setData(interpolation);
		button.setText(interpolation.name());
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (button.getSelection()) {
					System.out.println("Setting interpolation to " + interpolation.toString());
					getELTCanvas().getLayerManager().getBaseLayer().setInterpolation(interpolation);
				}
			}
		});
		return button;
	}

	private void changeProjection(Button projectionButton) {
		if (projectionButton == webMercatorButton) 
		{
			glCanvas.setMultiImageMode(ImageWorld.WEB_MERCATOR_PROJECTION);
			setImageButton(lastCenterImage);
		} else if (projectionButton == imageButton) {
			glCanvas.setSingleImageMode(lastCenterImage);
		}
	}


	@Override
	protected void checkSubclass() {
	}

	@Override
	public void update() {
		browserFrame.close();
		super.update();
	}

	public String getAppId() {
		return getELTCanvas().getAppId();
	}
	public ELTCanvas getELTCanvas() {
		return glCanvas;
	}

	public void centerWindow(Shell frame) {
		java.awt.Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Point frameSize = frame.getSize();

		if (frameSize.x > screenSize.width)
			frameSize.x = screenSize.width;
		if (frameSize.y > screenSize.height)
			frameSize.y = screenSize.height;

		frame.setLocation((screenSize.width - frameSize.x) >> 1,
				(screenSize.height - frameSize.y) >> 1);
	}

	private static String getFrameName(URL imageMetadataURL) {
		TileserverUrlBuilder urlBuilder = new TileserverUrlBuilder(imageMetadataURL);

		return String.format("%s/%s", urlBuilder.getCollectionID(), urlBuilder.getTimestamp());
	}

	public void render(double interpolation) {
		glCanvas.renderLoop();
		glCanvas.swapBuffers();
	}

	public void updateAnimation() {
		glCanvas.updateAnimation();
	}

	public void showEditor(Observation obs, Vector2ic clickPoint) {
		browserFrame.setVisible(false);
		browserFrame.viewObservation(obs, new Point(clickPoint.x(), clickPoint.y()));
		browserFrame.setVisible(true);
	}

	public void hideEditor() { browserFrame.close(); }
}
