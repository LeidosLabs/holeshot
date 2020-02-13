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

package com.leidoslabs.holeshot.chipper.wms.v130;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import org.locationtech.jts.geom.Envelope;
import org.w3._1999.xlink.TypeType;

import com.leidoslabs.holeshot.catalog.v1.CatalogClient;
import com.leidoslabs.holeshot.catalog.v1.CatalogCredentials;
import com.leidoslabs.holeshot.credentials.HoleshotCredentials;

import net.opengis.wms.BoundingBox;
import net.opengis.wms.Capability;
import net.opengis.wms.ContactAddress;
import net.opengis.wms.ContactInformation;
import net.opengis.wms.ContactPersonPrimary;
import net.opengis.wms.DCPType;
import net.opengis.wms.EXGeographicBoundingBox;
import net.opengis.wms.Get;
import net.opengis.wms.HTTP;
import net.opengis.wms.Keyword;
import net.opengis.wms.KeywordList;
import net.opengis.wms.Layer;
import net.opengis.wms.OnlineResource;
import net.opengis.wms.OperationType;
import net.opengis.wms.Post;
import net.opengis.wms.Request;
import net.opengis.wms.Service;
import net.opengis.wms.WMSCapabilities;

/**
 * Handles HttpServletRequests for WMS capabilities info, returns WMSCapabilities 
 * instance as a response
 */
class GetCapabilitiesRequestHandler130 {
   private final CatalogClient catalogClient;

   public GetCapabilitiesRequestHandler130() {
      catalogClient = new CatalogClient(CatalogCredentials.getApplicationDefaults());
   }
   
   /**
    * Handle HttpServletRequests and return a WMSCapabilities instance as a response
    * @param servletRequest
    * @return Jaxrs response of a WMSCapabilities instance
    * @throws IOException
    */
   public Response getResponse(HttpServletRequest servletRequest) throws IOException {
      Response response = null;

      net.opengis.wms.ObjectFactory wmsFactory = new net.opengis.wms.ObjectFactory();
      WMSCapabilities capabilities = wmsFactory.createWMSCapabilities();
      capabilities.setService(getService(wmsFactory, servletRequest));
      capabilities.setCapability(getCapability(wmsFactory, servletRequest));
      capabilities.setVersion("1.3.0");

      response = Response.ok(capabilities).build();

      return response;
   }

   private Capability getCapability(net.opengis.wms.ObjectFactory wmsFactory, HttpServletRequest servletRequest) throws IOException {
      final Capability capability = wmsFactory.createCapability();
      capability.setRequest(getRequest(wmsFactory, servletRequest));
      capability.setException(getException(wmsFactory));
      capability.setLayer(getLayers(wmsFactory));
      return capability;
   }
   private Layer getLayers(net.opengis.wms.ObjectFactory wmsFactory) throws IOException {
      final Envelope worldEnvelope = new Envelope(-180.0, 180.0, -90.0, 90.0);
      Layer topLayer = createLayer(wmsFactory, false, "LeidosLabsWMS", "LeidosLabs Map Server", worldEnvelope );

      catalogClient.getCatalogEntries().stream()
      .map(e->createLayer(wmsFactory, true, e.getMetadataURL().toString(), "LeidosLabs Map Server", e.getBounds().getEnvelopeInternal()))
      .forEach(l->topLayer.getLayer().add(l));


      //      final Envelope testEnvelope = new Envelope(37.21778268804294, 37.22741293151927, 19.600140096473766, 19.60926594310458 );
      //      Layer testLayer = createLayer(wmsFactory, true, "https://rgrobert-88-tileserver.leidoslabs.com/tileserver/XVIEWCHALLENGE-00005/20180116034512/metadata.json", "LeidosLabs Map Server", testEnvelope );
      //      topLayer.getLayer().add(testLayer);
      return topLayer;
   }

   private Layer createLayer(net.opengis.wms.ObjectFactory wmsFactory,
         boolean queryable,
         String name,
         String title,
         Envelope bbox) {
      Layer layer = wmsFactory.createLayer();
      layer.setQueryable(queryable);
      layer.setName(name);
      layer.setTitle(name);
      layer.getBoundingBox();
      layer.setOpaque(false);
      layer.setNoSubsets(false);
      layer.setFixedHeight(BigInteger.ZERO);
      layer.setFixedWidth(BigInteger.ZERO);

      final List<String> crsList = layer.getCRS();
      Arrays.asList("CRS:84").stream().forEachOrdered(c->crsList.add(c));

      BoundingBox wmsBBox = wmsFactory.createBoundingBox();
      wmsBBox.setCRS("CRS:84");
      wmsBBox.setMaxx(bbox.getMaxX());
      wmsBBox.setMaxy(bbox.getMaxY());
      wmsBBox.setMinx(bbox.getMinX());
      wmsBBox.setMiny(bbox.getMinY());
      // WMS 1.3.0 flips the axis.
//      wmsBBox.setMaxx(bbox.getMaxY());
//      wmsBBox.setMaxy(bbox.getMaxX());
//      wmsBBox.setMinx(bbox.getMinY());
//      wmsBBox.setMiny(bbox.getMinX());
      layer.getBoundingBox().add(wmsBBox);

      EXGeographicBoundingBox exBBox = wmsFactory.createEXGeographicBoundingBox();
      exBBox.setEastBoundLongitude(bbox.getMaxX());
      exBBox.setNorthBoundLatitude(bbox.getMaxY());
      exBBox.setWestBoundLongitude(bbox.getMinX());
      exBBox.setSouthBoundLatitude(bbox.getMinY());
      layer.setEXGeographicBoundingBox(exBBox);

      return layer;

   }

   private net.opengis.wms.Exception getException(net.opengis.wms.ObjectFactory wmsFactory) {
      net.opengis.wms.Exception exception = wmsFactory.createException();
      final List<String> formats = exception.getFormat();
      Arrays.asList("XML", "INIMAGE", "BLANK").forEach(m->formats.add(m));
      return exception;
   }

   private Request getRequest(net.opengis.wms.ObjectFactory wmsFactory, HttpServletRequest servletRequest) {
      final Request request = wmsFactory.createRequest();

      // GetCapability
      final OperationType getCapability = wmsFactory.createOperationType();
      final List<String> formats = getCapability.getFormat();
      Arrays.asList("application/vnd.ogc.wms_xml", "text/xml").forEach(m->formats.add(m));
      final List<DCPType> dcpTypes = getCapability.getDCPType();

      DCPType getType = wmsFactory.createDCPType();
      final HTTP http = wmsFactory.createHTTP();
      final Get get = wmsFactory.createGet();
      final Post post = wmsFactory.createPost();
      final OnlineResource onlineResource = getOnlineResource(wmsFactory, servletRequest.getRequestURL().toString() + "?");
      get.setOnlineResource(onlineResource);
      post.setOnlineResource(onlineResource);
      http.setGet(get);
      http.setPost(post);
      getType.setHTTP(http);
      dcpTypes.add(getType);
      request.setGetCapabilities(getCapability);

      // GetMap
      final OperationType getMap = wmsFactory.createOperationType();
      final List<String> mapFormats = getCapability.getFormat();
      Arrays.asList("image/png").forEach(m->mapFormats.add(m));
      final List<DCPType> mapDcpTypes = getCapability.getDCPType();

      DCPType mapGetType = wmsFactory.createDCPType();
      final HTTP mapHttp = wmsFactory.createHTTP();
      final Get mapGet = wmsFactory.createGet();
      final Post mapPost = wmsFactory.createPost();
      final OnlineResource mapOnlineResource = getOnlineResource(wmsFactory, servletRequest.getRequestURL().toString() + "?");
      mapGet.setOnlineResource(mapOnlineResource);
      mapPost.setOnlineResource(mapOnlineResource);
      mapHttp.setGet(mapGet);
      mapHttp.setPost(mapPost);
      mapGetType.setHTTP(mapHttp);
      mapDcpTypes.add(mapGetType);
      request.setGetMap(getMap);

      return request;
   }

   private OnlineResource getOnlineResource(net.opengis.wms.ObjectFactory wmsFactory, String url) {
      final OnlineResource onlineResource = wmsFactory.createOnlineResource();
      onlineResource.setType(TypeType.SIMPLE);
      // TODO: Don't hardcode this.  Need to pass through the dns and api key to the servlet somehow.
      onlineResource.setHref(String.format("https://chipper.leidoslabs.com/api/wms?x-api-key=%s", getAPIKey()));

//      onlineResource.setHref(url);
      return onlineResource;
   }
   
   private String getAPIKey() {
	   return HoleshotCredentials.getApplicationDefaults().getSecretAccessKey();
   }
   

   private Service getService(net.opengis.wms.ObjectFactory wmsFactory, HttpServletRequest servletRequest) {
      final Service service = wmsFactory.createService();
      service.setName("WMS");
      service.setTitle("LeidosLabs WMS Service");
      service.setAbstract("WMS used for serving finished tiles from the LeidosLabs ImageChipping Service.");

      final KeywordList keywordList = wmsFactory.createKeywordList();
      Arrays.asList("leidos", "leidoslabs", "geographic", "wms").stream().map(k-> {
         Keyword newKeyword = wmsFactory.createKeyword();
         newKeyword.setValue(k);
         return newKeyword;
      }).forEach(k->keywordList.getKeyword().add(k));
      service.setKeywordList(keywordList);

      final OnlineResource onlineResource = getOnlineResource(wmsFactory, servletRequest.getRequestURL().toString() + "?");
      service.setOnlineResource(onlineResource);

      final ContactInformation contactInfo = wmsFactory.createContactInformation();
      final ContactPersonPrimary primaryContact = wmsFactory.createContactPersonPrimary();
      primaryContact.setContactPerson("Ray Roberts");
      primaryContact.setContactOrganization("Leidos");
      contactInfo.setContactPersonPrimary(primaryContact);
      final ContactAddress contactAddress = wmsFactory.createContactAddress();
      contactAddress.setAddressType("postal");
      contactAddress.setAddress("700 North Frederick Avenue");
      contactAddress.setCity("Gaithersburg");
      contactAddress.setCountry("US");
      contactAddress.setPostCode("20879");
      contactAddress.setStateOrProvince("MD");
      contactInfo.setContactAddress(contactAddress);
      contactInfo.setContactElectronicMailAddress("ray.roberts@leidos.com");
      service.setContactInformation(contactInfo);
      service.setFees("NONE");
      service.setAccessConstraints("NONE");
      service.setMaxHeight(BigInteger.valueOf(2048));
      service.setMaxWidth(BigInteger.valueOf(2048));
      return service;
   }

}
