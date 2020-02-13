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

import dorkbox.systemTray.MenuItem;
import dorkbox.systemTray.SystemTray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * The ELT is intended to be run as a Windows service and is accessible via the Windows Systems Tray.
 */
public class ELTSystemTray {
   private static final Logger LOGGER = LoggerFactory.getLogger(ELTSystemTray.class);

   private final List<Runnable> exitListeners;

   /**
    * Initialize Exit listeners with exit listener, and add to system tray 
    * @param exitListener
    */
   public ELTSystemTray(Runnable exitListener) {
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
       MenuItem exitItem = new MenuItem("Exit");

       systemTray.getMenu().add(aboutItem).setCallback(new ActionListener() {
           public void actionPerformed(ActionEvent e) {
               JOptionPane.showMessageDialog(null,
                       "LeidosLabs ELT");
           }
       });

       systemTray.getMenu().add(exitItem).setCallback(new ActionListener() {
           public void actionPerformed(ActionEvent e) {
               exitListeners.stream().forEach(l->l.run());
               systemTray.shutdown();
           }
       });

   }
}
