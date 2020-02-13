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

package com.leidoslabs.holeshot.chipper.wms.v111;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import org.locationtech.jts.geom.Envelope;

import com.leidoslabs.holeshot.catalog.v1.CatalogClient;
import com.leidoslabs.holeshot.catalog.v1.CatalogCredentials;
import com.leidoslabs.holeshot.credentials.HoleshotCredentials;

import net.opengis.wms111.BoundingBox;
import net.opengis.wms111.Capability;
import net.opengis.wms111.ContactAddress;
import net.opengis.wms111.ContactInformation;
import net.opengis.wms111.ContactPersonPrimary;
import net.opengis.wms111.DCPType;
import net.opengis.wms111.Format;
import net.opengis.wms111.Get;
import net.opengis.wms111.GetCapabilities;
import net.opengis.wms111.GetMap;
import net.opengis.wms111.HTTP;
import net.opengis.wms111.Keyword;
import net.opengis.wms111.KeywordList;
import net.opengis.wms111.LatLonBoundingBox;
import net.opengis.wms111.Layer;
import net.opengis.wms111.OnlineResource;
import net.opengis.wms111.Post;
import net.opengis.wms111.Request;
import net.opengis.wms111.SRS;
import net.opengis.wms111.Service;
import net.opengis.wms111.WMTMSCapabilities;

class GetCapabilitiesRequestHandler111 {
   private final CatalogClient catalogClient;

   public GetCapabilitiesRequestHandler111() {
      catalogClient = new CatalogClient(CatalogCredentials.getApplicationDefaults());
   }

   public Response getResponse(HttpServletRequest servletRequest) throws IOException {
      Response response = null;

      net.opengis.wms111.ObjectFactory wmsFactory = new net.opengis.wms111.ObjectFactory();
      WMTMSCapabilities capabilities = wmsFactory.createWMTMSCapabilities();
      capabilities.setService(getService(wmsFactory, servletRequest));
      capabilities.setCapability(getCapability(wmsFactory, servletRequest));
      capabilities.setVersion("1.1.1");

      response = Response.ok(capabilities).build();

      return response;
   }

   private Capability getCapability(net.opengis.wms111.ObjectFactory wmsFactory, HttpServletRequest servletRequest) throws IOException {
      final Capability capability = wmsFactory.createCapability();
      capability.setRequest(getRequest(wmsFactory, servletRequest));
      capability.setException(getException(wmsFactory));
      capability.setLayer(getLayers(wmsFactory));
      return capability;
   }
   private Layer getLayers(net.opengis.wms111.ObjectFactory wmsFactory) throws IOException {
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

   private static String booleanToInt(boolean src) {
      return src ? "1" : "0";
   }
   private Layer createLayer(net.opengis.wms111.ObjectFactory wmsFactory,
         boolean queryable,
         String name,
         String title,
         Envelope bbox) {
      Layer layer = wmsFactory.createLayer();
      layer.setQueryable("0");
      layer.setName(name);
      layer.setTitle(name);
      layer.setOpaque("1");
      layer.setNoSubsets("0");
      layer.setFixedHeight("0");
      layer.setFixedWidth("0");

      final List<SRS> srsList = layer.getSRS();
      Arrays.asList("EPSG:4326").stream().map(s-> {
         SRS srs = wmsFactory.createSRS();
         srs.setvalue(s);
         return srs;
      }).forEach(m->srsList.add(m));

      BoundingBox wmsBBox = wmsFactory.createBoundingBox();
      wmsBBox.setSRS("EPSG:4326");
      wmsBBox.setMaxx(Double.toString(bbox.getMaxX()));
      wmsBBox.setMaxy(Double.toString(bbox.getMaxY()));
      wmsBBox.setMinx(Double.toString(bbox.getMinX()));
      wmsBBox.setMiny(Double.toString(bbox.getMinY()));
      layer.getBoundingBox().add(wmsBBox);


      final LatLonBoundingBox latLonBBox = wmsFactory.createLatLonBoundingBox();
      latLonBBox.setMaxx(wmsBBox.getMaxx());
      latLonBBox.setMaxy(wmsBBox.getMaxy());
      latLonBBox.setMinx(wmsBBox.getMinx());
      latLonBBox.setMiny(wmsBBox.getMiny());
      layer.setLatLonBoundingBox(latLonBBox);
      return layer;

   }

   private net.opengis.wms111.Exception getException(net.opengis.wms111.ObjectFactory wmsFactory) {
      net.opengis.wms111.Exception exception = wmsFactory.createException();
      final List<Format> formats = exception.getFormat();
      Arrays.asList("application/vnd.ogc.se_xml", "application/vnd.ogc.se_inimage", "application/vnd.ogc.se_blank").stream().map(s-> {
         Format format = wmsFactory.createFormat();
         format.setvalue(s);
         return format;
      }).forEach(m->formats.add(m));
      return exception;
   }

   private Request getRequest(net.opengis.wms111.ObjectFactory wmsFactory, HttpServletRequest servletRequest) {
      final Request request = wmsFactory.createRequest();

      // GetCapability
      final GetCapabilities getCapability = wmsFactory.createGetCapabilities();
      final List<Format> formats = getCapability.getFormat();
      Arrays.asList("application/vnd.ogc.wms_xml").stream().map(s-> {
         Format format = wmsFactory.createFormat();
         format.setvalue(s);
         return format;
      }).forEach(m->formats.add(m));
      final List<DCPType> dcpTypes = getCapability.getDCPType();

      DCPType getType = wmsFactory.createDCPType();
      final HTTP http = wmsFactory.createHTTP();
      final Get get = wmsFactory.createGet();
      final Post post = wmsFactory.createPost();
      final OnlineResource onlineResource = getOnlineResource(wmsFactory, servletRequest.getRequestURL().toString() + "?");
      get.setOnlineResource(onlineResource);
      post.setOnlineResource(onlineResource);
      Arrays.asList(get, post).forEach(h->http.getGetOrPost().add(h));
      getType.setHTTP(http);
      dcpTypes.add(getType);
      request.setGetCapabilities(getCapability);

      // GetMap
      final GetMap getMap = wmsFactory.createGetMap();
      final List<Format> mapFormats = getMap.getFormat();
      Arrays.asList("image/png").stream().map(s-> {
         Format format = wmsFactory.createFormat();
         format.setvalue(s);
         return format;
      }).forEach(m->mapFormats.add(m));

      final List<DCPType> mapDcpTypes = getMap.getDCPType();

      DCPType mapGetType = wmsFactory.createDCPType();
      final HTTP mapHttp = wmsFactory.createHTTP();
      final Get mapGet = wmsFactory.createGet();
      final Post mapPost = wmsFactory.createPost();
      final OnlineResource mapOnlineResource = getOnlineResource(wmsFactory, servletRequest.getRequestURL().toString() + "?");
      mapGet.setOnlineResource(mapOnlineResource);
      mapPost.setOnlineResource(mapOnlineResource);
      Arrays.asList(mapGet, mapPost).forEach(h->mapHttp.getGetOrPost().add(h));
      mapGetType.setHTTP(mapHttp);
      mapDcpTypes.add(mapGetType);
      request.setGetMap(getMap);

      return request;
   }

   private OnlineResource getOnlineResource(net.opengis.wms111.ObjectFactory wmsFactory, String url) {
      final OnlineResource onlineResource = wmsFactory.createOnlineResource();
//      onlineResource.setXlinkHref(url);
      // TODO: Don't hardcode this.  Need to pass through the dns and api key to the servlet somehow.
      onlineResource.setXlinkHref(String.format("https://chipper.leidoslabs.com/api/wms?x-api-key=%s", getAPIKey()));
      onlineResource.setXmlnsXlink("http://www.w3.org/1999/xlink");
      return onlineResource;
   }
   
   private String getAPIKey() {
	   return HoleshotCredentials.getApplicationDefaults().getSecretAccessKey();
   }

   private Service getService(net.opengis.wms111.ObjectFactory wmsFactory, HttpServletRequest servletRequest) {
      final Service service = wmsFactory.createService();
      service.setName("OGC:WMS");
      service.setTitle("LeidosLabs WMS Service");
      service.setAbstract("WMS used for serving finished tiles from the LeidosLabs ImageChipping Service.");

      final KeywordList keywordList = wmsFactory.createKeywordList();
      Arrays.asList("leidos", "leidoslabs", "geographic", "wms").stream().map(k-> {
         Keyword newKeyword = wmsFactory.createKeyword();
         newKeyword.setvalue(k);
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
      return service;
   }

}
