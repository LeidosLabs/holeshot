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

package com.leidoslabs.holeshot.elt.imageop.ogl;

/**
 * Settings for ImageChain, including some pre-sets
 */
public class ImageChainSettings {

   public static final ImageChainSettings IA_0 =
         new ImageChainSettingsFactory()
         .setSub(0.0f)
         .setMul(1.00f)
         .setTTCFamily(0)
         .setTTCMember(32)
         .setMtfeFamily(0)
         .setMtfeMember(5)
         .build();
   public static final ImageChainSettings IA_1 =
         new ImageChainSettingsFactory()
         .setpMin(0.0250f)
         .setpMax(0.99f)
         .setA(0.1f)
         .setB(0.5f)
         .setTTCFamily(0)
         .setTTCMember(10)
         .setMtfeFamily(0)
         .setMtfeMember(5)
         .build();

   public static final ImageChainSettings IA_2 =
         new ImageChainSettingsFactory()
         .setSub(0.0f)
         .setpMax(0.9995f)
         .setTTCFamily(0)
         .setTTCMember(32)
         .setMtfeFamily(0)
         .setMtfeMember(5)
         .build();

   public static enum ImageSource {
      UNSPECIFIED(IA_0),
      SAR_DEFAULT(IA_2),
      SCANNED_HARDCOPY(IA_0),
      SYERS_2(IA_0),
      GLOBAL_HAWK(IA_0),
      QUICKBIRD(IA_1),
      IKONOS(IA_1),
      GEOEYE_1(IA_1),
      CLEARVIEW(IA_1),
      NEXTVIEW(IA_1),
      WORLDVIEW_1(IA_1),
      WORLDVIEW_2(IA_1),
      ORBVIEW (IA_1);

      private ImageChainSettings settings;
      private ImageSource(ImageChainSettings settings) {
         this.settings = settings;
      }
      public ImageChainSettings getSettings() {
         return settings;
      }
   }


   private int ttcFamily;
   private int ttcMember;
   private float sub;
   private float mul;
   private float pMin;
   private float pMax;
   private float A;
   private float B;
   private int mtfeFamily;
   private int mtfeMember;
   private float gamma;

   private ImageChainSettings() {
   }

   public ImageChainSettings(ImageChainSettings settings) {
      this.ttcFamily = settings.ttcFamily;
      this.ttcMember = settings.ttcMember;
      this.sub = settings.sub;
      this.mul = settings.mul;
      this.pMin = settings.pMin;
      this.pMax = settings.pMax;
      this.A = settings.A;
      this.B = settings.B;
      this.mtfeFamily = settings.mtfeFamily;
      this.mtfeMember = settings.mtfeMember;
      this.gamma = settings.gamma;
   }

   public void setTTCFamily(int family) {
      this.ttcFamily = family;
   }


   public void setTTCMember(int member) {
      this.ttcMember = member;
   }

   public void setMTFEFamily(int family) {
      this.mtfeFamily = family;
   }


   public void setMTFEMember(int member) {
      this.mtfeMember = member;
   }


   public void setSub(float sub) {
      this.sub = sub;
   }


   public void setMul(float mul) {
      this.mul = mul;
   }

   public void setpMin(float pMin) {
      this.pMin = pMin;
   }


   public void setpMax(float pMax) {
      this.pMax = pMax;
   }


   public void setA(float a) {
      A = a;
   }


   public void setB(float b) {
      B = b;
   }
   public void setGamma(float gamma) {
      this.gamma = gamma;
   }


   public int getTTCFamily() {
      return ttcFamily;
   }


   public int getTTCMember() {
      return ttcMember;
   }

   public int getMTFEFamily() {
      return mtfeFamily;
   }


   public int getMTFEMember() {
      return mtfeMember;
   }


   public float getSub() {
      return sub;
   }


   public float getMul() {
      return mul;
   }


   public float getpMin() {
      return pMin;
   }


   public float getpMax() {
      return pMax;
   }


   public float getA() {
      return A;
   }


   public float getB() {
      return B;
   }
   public float getGamma() {
      return gamma;
   }


   public void adjustSub(float x) {
      this.sub += x;
   }
   public void adjustMul(float x) {
      this.mul += x;
   }
   public void adjustGamma(float x) {
      this.gamma += x;
   }


   public static class ImageChainSettingsFactory {
      public static final int DEFAULT_TTC_FAMILY = 0;
      public static final int DEFAULT_TTC_MEMBER = 32;
      public static final float DEFAULT_SUB = 0.0f;
      public static final float DEFAULT_MUL = 1.0f;
      public static final float DEFAULT_PMIN = 0.02f;
      public static final float DEFAULT_PMAX = 0.98f;
      public static final float DEFAULT_A = 0.2f;
      public static final float DEFAULT_B = 0.4f;
      public static final int DEFAULT_MTFE_FAMILY = 0;
      public static final int DEFAULT_MTFE_MEMBER = 5;
      public static final float DEFAULT_GAMMA = 1.953f;

      private int ttcFamily;
      private int ttcMember;
      private float sub;
      private float mul;
      private float pMin;
      private float pMax;
      private float A;
      private float B;
      private int mtfeFamily;
      private int mtfeMember;
      private float gamma;

      public ImageChainSettingsFactory() {
         ttcFamily = DEFAULT_TTC_FAMILY;
         ttcMember = DEFAULT_TTC_MEMBER;
         sub = DEFAULT_SUB;
         mul = DEFAULT_MUL;
         pMin = DEFAULT_PMIN;
         pMax = DEFAULT_PMAX;
         A = DEFAULT_A;
         B = DEFAULT_B;
         mtfeFamily = DEFAULT_MTFE_FAMILY;
         mtfeMember = DEFAULT_MTFE_MEMBER;
         gamma = DEFAULT_GAMMA;
      }
      public ImageChainSettings build() {
         final ImageChainSettings result = new ImageChainSettings();
         result.setTTCFamily(ttcFamily);
         result.setTTCMember(ttcMember);
         result.setSub(sub);
         result.setMul(mul);
         result.setpMin(pMin);
         result.setpMax(pMax);
         result.setA(A);
         result.setB(B);
         result.setMTFEFamily(mtfeFamily);
         result.setMTFEMember(mtfeMember);
         result.setGamma(gamma);
         return result;
      }

      public ImageChainSettingsFactory setTTCFamily(int family) {
         this.ttcFamily = family;
         return this;
      }
      public ImageChainSettingsFactory setTTCMember(int member) {
         this.ttcMember = member;
         return this;
      }
      public ImageChainSettingsFactory setSub(float sub) {
         this.sub = sub;
         return this;
      }
      public ImageChainSettingsFactory setMul(float mul) {
         this.mul = mul;
         return this;
      }
      public ImageChainSettingsFactory setpMin(float pMin) {
         this.pMin = pMin;
         return this;
      }
      public ImageChainSettingsFactory setpMax(float pMax) {
         this.pMax = pMax;
         return this;
      }
      public ImageChainSettingsFactory setA(float a) {
         this.A = a;
         return this;
      }
      public ImageChainSettingsFactory setB(float b) {
         this.B = b;
         return this;
      }

      public ImageChainSettingsFactory setMtfeFamily(int mtfeFamily) {
         this.mtfeFamily = mtfeFamily;
         return this;
      }
      public ImageChainSettingsFactory setMtfeMember(int mtfeMember) {
         this.mtfeMember = mtfeMember;
         return this;
      }
      public ImageChainSettingsFactory setGamma(float gamma) {
         this.gamma = gamma;
         return this;
      }

   }

}
