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

package com.leidoslabs.holeshot.chipper;

import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PoolUtils;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.AbandonedConfig;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.usefultoys.slf4j.LoggerFactory;

import com.leidoslabs.holeshot.elt.ELTDisplayExecutor;
import com.leidoslabs.holeshot.elt.ELTDisplayExecutor.ExecMode;

/**
 * Object Pool for ImageChipper. Includes a Factory class
 */
public class ChipperPool extends GenericObjectPool<ImageChipper> {
   private static final int NUMBER_OF_CHIPPERS = Integer.parseInt(System.getProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "4"));
   private static final Logger LOGGER = LoggerFactory.getLogger(ChipperPool.class);
   private static final int ABANDON_TIMEOUT_IN_SECONDS = 30;


   public ChipperPool(ELTDisplayExecutor eltDisplayExecutor) throws IllegalArgumentException, Exception {
      super(new ChipperPoolFactory(eltDisplayExecutor), getConfig(), getAbandonedConfig());
      PoolUtils.prefill(this, NUMBER_OF_CHIPPERS);
   }

   private static GenericObjectPoolConfig  getConfig() {
      GenericObjectPoolConfig config = new GenericObjectPoolConfig();
      config.setMinIdle(0);
      config.setMaxIdle(NUMBER_OF_CHIPPERS);
      config.setMaxTotal(NUMBER_OF_CHIPPERS);
      config.setBlockWhenExhausted(true);
      config.setFairness(true);
      config.setLifo(false);
      config.setTestOnBorrow(false);
      config.setTestOnCreate(false);
      config.setTestOnReturn(false);
      config.setTestWhileIdle(false);
      config.setMaxWaitMillis(-1);
      config.setTimeBetweenEvictionRunsMillis(-1);
      config.setMinEvictableIdleTimeMillis(-1);
      config.setSoftMinEvictableIdleTimeMillis(-1);
      return config;
   }

   private static AbandonedConfig getAbandonedConfig() {
      AbandonedConfig config = new AbandonedConfig();
      config.setLogAbandoned(true);
      config.setLogWriter(new PrintWriter(LoggerFactory.getWarnOutputStream(LOGGER)));
      config.setRemoveAbandonedOnBorrow(true);
      config.setRemoveAbandonedOnMaintenance(true);
      config.setRemoveAbandonedTimeout(ABANDON_TIMEOUT_IN_SECONDS);
      return config;
   }

   
   /**
    * Pooled object factory 
    * @author lobugliop
    *
    */
   public static class ChipperPoolFactory extends BasePooledObjectFactory<ImageChipper> {
      private static final Logger LOGGER = LoggerFactory.getLogger(ChipperPoolFactory.class);
      private final ELTDisplayExecutor eltDisplayExecutor;
      public ChipperPoolFactory(ELTDisplayExecutor eltDisplayExecutor) {
         super();
         this.eltDisplayExecutor = eltDisplayExecutor;
      }

      @Override
      /**
       * Create an ImageChipper instance for the object pool. Creation is a
       * synchronous task for the eltDisplayExecutor
       */
      public ImageChipper create() throws Exception {
         final AtomicReference<ImageChipper> result = new AtomicReference<ImageChipper>(null);
         final AtomicReference<Exception> exceptionRef = new AtomicReference<Exception>(null);
         eltDisplayExecutor.submit(ExecMode.SYNCHRONOUS, () -> {
            try {
               LOGGER.info("Creating Chipper");
               result.set(new ImageChipper());
            } catch (Exception e) {
               exceptionRef.set(e);
            }
         });
         if (exceptionRef.get() != null) {
            throw exceptionRef.get();
         }
         return result.get();
      }

      /**
       * Use the default PooledObject implementation.
       */
      @Override
      public PooledObject<ImageChipper> wrap(ImageChipper chipper) {
         return new DefaultPooledObject<ImageChipper>(chipper);
      }

      @Override
      /**
       * Destroy Pooled object
       */
      public void destroyObject(PooledObject<ImageChipper> p) throws Exception {
         LOGGER.info("Destroying Chipper");
         super.destroyObject(p);
         p.getObject().close();
      }
   }

   /**
    * Return instance to pool
    */
   public void returnObject(ImageChipper chipper) {
      super.returnObject(chipper);
   }

}
