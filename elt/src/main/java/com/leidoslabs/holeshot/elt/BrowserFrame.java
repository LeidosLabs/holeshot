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

import com.leidoslabs.holeshot.elt.observations.Observation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Region;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * An HTML Browser window utilized for KML Description popups
 * 
 * @author robertsrg
 *
 */
public class BrowserFrame extends Shell {
    private static final int MIN_HEIGHT = 200;
    private static final int MIN_WIDTH = 200;

    private final Browser browser;
    private final Button closeButton;
    private int maxHeight;
    private int maxWidth;
    
    /**
     * The x,y of the currently selected feature, relative to the parent frame.
     */
    private Point location;

    /**
     * Constructor
     * @param parent
     */
    BrowserFrame(Shell parent) {
        super(parent, SWT.NO_TRIM); //SWT.RESIZE | SWT.ON_TOP | SWT.CLOSE);
        setText("View Observation");
        setImage(new Image(getDisplay(), ELTFrame.class.getClassLoader().getResourceAsStream("com/leidoslabs/holeshot/elt/images/icon.png")));

        browser = new Browser(this, SWT.NONE);

        closeButton = new Button(this, SWT.PUSH);
        closeButton.setText("X");
        closeButton.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
        closeButton.setVisible(true);
        closeButton.addListener(SWT.Selection, event -> this.close());

        setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
    }

    /**
     * Clear out all HTML associated with the widget
     */
    public void clear() {
        browser.setText("");
    }

    /**
     * Determines what side of the parent frame to anchor the browser frame based on its size and the location of the selected feature.
     * 
     * @param width - The desired width of the browser frame
     * @param height - The desired height of the browser frame
     * @return The side of the parent to anchor the browser frame to (i.e. SWT.TOP, SWT.RIGHT, SWT.LEFT, or SWT.BOTTOM)
     */
    private int determineDirection(int width, int height) {

        maxHeight = height;
        maxWidth = width;
        Point parentSize = getParent().getSize();

        int canShow = 0;
        int[] preference = {
                SWT.TOP, SWT.RIGHT, SWT.LEFT, SWT.BOTTOM
        };
        int spaceLeft = location.x;
        int spaceRight = parentSize.x - location.x;
        int spaceTop = location.y;
        int spaceBottom = parentSize.y - location.y;

        if(location.y > (int) (height * 0.5) && location.y + (int) (height * 0.5) < parentSize.y) {
            if(spaceRight > width)
                canShow |= SWT.RIGHT;
            if(spaceLeft > width)
                canShow |= SWT.LEFT;
        }

        if(location.x > (int) (width * 0.5) && location.x + (int) (width * 0.5) < parentSize.x) {
            if(spaceBottom  > height)
                canShow |= SWT.BOTTOM;
            if(spaceTop > height)
                canShow |= SWT.TOP;
        }

        // This could be improved so the dimensions don't end up too small for clicks in the corner
        // Also consider allowing the pointer to not always be centered, allowing for more space in the corners
        if(canShow == 0 && height > MIN_HEIGHT) {
            return determineDirection(width, (int) (height * 0.75));
        } else if(canShow == 0 && width > MIN_WIDTH) {
            return determineDirection((int) (width * 0.75), height);
        }

        for (int choice : preference) {
            if ((canShow & choice) == choice) {
                return choice;
            }
        }

        return SWT.BOTTOM; //default to bottom so the top of the window will never go off the screen
    }

    /**
     * Sets the size and the location of this browser frame based on the location of the selected item and the html contents of the browser page.
     */
    private void buildRegion() {

        Point absoluteLocation = new Point(location.x + getParent().getLocation().x, location.y + getParent().getLocation().y);
        int width = 400; // TODO: get initial width from browser page
        int height = 400; // TODO: get initial height from browser page

        Region region = new Region();
        int tDim = 25; //'height' of the triangle pointer thing
        int bDim = 20; //size of close button

        int direction = determineDirection(width, height);
        width = Math.min(maxWidth - tDim, width);
        height = Math.min(maxHeight - tDim, height);
        Point center = new Point((int) (width * 0.5), (int)(height * 0.5));

        switch(direction) {
            case SWT.RIGHT:
                absoluteLocation.x += tDim;
                absoluteLocation.y -= (center.y - tDim);
                region.add(new int[]{0, center.y, tDim, center.y - tDim, tDim, center.y + tDim});
                region.add(tDim, 0, width, height);
                setSize(width + tDim, height);
                closeButton.setBounds(tDim + width - bDim, 0, bDim, bDim);
                browser.setBounds(tDim, bDim, width, height - bDim);
                break;
            case SWT.BOTTOM:
                absoluteLocation.x -= (center.x - (int) (tDim * 0.5));
                absoluteLocation.y += (int) (tDim * 2);
                region.add(new int[]{center.x, 0, center.x - tDim, tDim, center.x + tDim, tDim});
                region.add(0, tDim, width, height);
                closeButton.setBounds(width - bDim, tDim, bDim, bDim);
                browser.setBounds(0, tDim + bDim, width, height - bDim);
                break;
            case SWT.LEFT:
                absoluteLocation.x -= (width + tDim);
                absoluteLocation.y -= (center.y - tDim);
                region.add(new int[]{width + tDim, center.y, width, center.y - tDim, width, center.y + tDim});
                region.add(0, 0, width, height);
                closeButton.setBounds(width - bDim, 0, bDim, bDim);
                browser.setBounds(0 , bDim, width, height - bDim);
                break;
            case SWT.TOP:
                absoluteLocation.x -= (center.x - (int) (tDim * 0.5));
                absoluteLocation.y -= (height + tDim);
                region.add(new int[]{center.x, height + tDim, center.x - tDim, height, center.x + tDim, height});
                region.add(0, 0, width, height);
                closeButton.setBounds(width - bDim,0, bDim, bDim);
                browser.setBounds(0, bDim ,width,height - bDim);
                break;
        }

        //closeButton.setVisible(true);
        setRegion(region);
        setLocation(absoluteLocation);
        region.dispose();
    }

    /**
     * Request that the description of the current observation is made visible at the given point.
     * @param obs - The observation that you want to see the description box for.
     * @param point - The location of the current observation relative to the frame.
     */
    public void viewObservation(Observation obs, Point point) {
        this.location = point;
        //setText for demo purposes
        if(obs.getProperties().get("image") != null) {
            browser.setUrl((String) obs.getProperties().get("image"));
        } else {
            StringBuilder sb = new StringBuilder();
            obs.getProperties().forEach((k, v) -> {
                sb.append("<div><p1>")
                        .append(k).append(": ").append(v)
                        .append("</p1></div>");
            });
            browser.setText(sb.toString() +
                    "<div><p1>Lng: " + obs.getGeometry().getCoordinate().x + "</p1></div>" +
                    "<div><p1>Lat: " + obs.getGeometry().getCoordinate().y + "</p1></div>");
        }
        // browser.setUrl("https://www.google.com/");
        // browser.setUrl("localhost:4200/observations/" + obs.getId());
        redraw();
        moveAbove(this.getParent());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void redraw() {
        this.checkWidget();
        buildRegion();
        super.redraw();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void checkSubclass() {
    }

}
