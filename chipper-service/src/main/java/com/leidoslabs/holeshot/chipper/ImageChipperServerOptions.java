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

import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

/**
 * Options and parser for ImageChipperServer
 */
public class ImageChipperServerOptions {
  private static final XLogger logger = XLoggerFactory.getXLogger(ImageChipperServerOptions.class);

  private static Option JAVA_PROPERTY_OPTION =
      Option.builder("D")
      .argName("property=value")
      .numberOfArgs(2)
      .valueSeparator()
      .desc("use value for given property")
      .build();

  private static Option JAVAX_PROPERTY_OPTION =
        Option.builder("X")
        .argName("property=value")
        .numberOfArgs(2)
        .valueSeparator()
        .desc("use value for given property")
        .build();
  private static Option DOMAIN_OPTION = Option.builder("d")
        .required(false)
        .longOpt("domain")
        .argName("domain")
        .hasArg()
        .optionalArg(false)
        .valueSeparator()
        .desc("The FQDN to the Chipping Server")
        .build();
  

  private static final Options DAEMON_OPTIONS = new Options()
      .addOption(DOMAIN_OPTION)
      .addOption(JAVAX_PROPERTY_OPTION)
      .addOption(JAVA_PROPERTY_OPTION);


  private static final ImageChipperServerOptions instance = new ImageChipperServerOptions();
  
  private String domainName;

  public static synchronized ImageChipperServerOptions getInstance() {
    return instance;
  }

  private ImageChipperServerOptions() {
  }

  /**
   * Parses command line args, and sets system properties
   * @param args cmd line args
   * @throws ParseException
   */
  public void initialize(String[] args) throws ParseException {
    CommandLineParser parser = new DefaultParser();
    CommandLine line = parser.parse(DAEMON_OPTIONS, args);

    Properties javaProperties = line.getOptionProperties(JAVA_PROPERTY_OPTION.getOpt());
    javaProperties.entrySet().stream().forEach(p->System.setProperty((String)p.getKey(), (String)p.getValue()));
    
    setDomainName(line.getOptionValue(DOMAIN_OPTION.getOpt(), null));

  }
  
  public void setDomainName(String domainName) {
     logger.entry(domainName);
     this.domainName = domainName;
     logger.exit();
 }
  public String getDomainName() {
     return domainName;
  }

}
