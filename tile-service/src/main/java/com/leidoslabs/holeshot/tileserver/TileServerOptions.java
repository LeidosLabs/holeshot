/*
 * Licensed to The Leidos Corporation under one or more contributor license agreements.  
 * See the NOTICE file distributed with this work for additional information regarding copyright ownership.
 * The Leidos Corporation licenses this file to You under the Apache License, Version 2.0
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

package com.leidoslabs.holeshot.tileserver;

import io.netty.util.internal.StringUtil;
import org.apache.commons.cli.*;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import java.util.Properties;

public class TileServerOptions {
    private static final XLogger logger = XLoggerFactory.getXLogger(TileServerOptions.class);

    private static Option TILE_BUCKET_REGION_OPTION = Option.builder("r")
            .required()
            .longOpt("tilebucketregion")
            .argName("tilebucketregion")
            .hasArg()
            .optionalArg(false)
            .valueSeparator()
            .desc("The Region that the S3 Bucket that holds the tiles is in")
            .build();

    private static Option TILE_BUCKET_OPTION = Option.builder("t")
            .required()
            .longOpt("tilebucket")
            .argName("tilebucket")
            .hasArg()
            .optionalArg(false)
            .valueSeparator()
            .desc("The S3 Bucket that holds the tiles to be served")
            .build();

    private static Option JAVA_PROPERTY_OPTION =
            Option.builder("D")
                    .argName("property=value")
                    .numberOfArgs(2)
                    .valueSeparator()
                    .desc("use value for given property")
                    .build();

    private static final Options DAEMON_OPTIONS = new Options()
            .addOption(TILE_BUCKET_REGION_OPTION)
            .addOption(TILE_BUCKET_OPTION)
            .addOption(JAVA_PROPERTY_OPTION);


    private static TileServerOptions instance = null;
    private String tileBucket;
    private String tileBucketRegion;
    private String tileServerDomainName;

    public static synchronized TileServerOptions getInstance() {
        if (instance == null) {
            instance = new TileServerOptions();
        }
        return instance;
    }

    private TileServerOptions() {
    }
    
    /**
     * Tile server arg parser, initializes TileServerOption's instance variables.
     * @param args Command line args
     * @throws ParseException
     */
    public void initialize(String[] args) throws ParseException {
        logger.entry();
        CommandLineParser parser = new DefaultParser();
        CommandLine line = parser.parse(DAEMON_OPTIONS, args);

        Properties javaProperties = line.getOptionProperties(JAVA_PROPERTY_OPTION.getOpt());
        javaProperties.entrySet().forEach(p->System.setProperty((String)p.getKey(), (String)p.getValue()));

        setTileBucket(line.getOptionValue(TILE_BUCKET_OPTION.getOpt()));
        setTileBucketRegion(line.getOptionValue(TILE_BUCKET_REGION_OPTION.getOpt()));
        logger.exit();
    }

    public String getTileBucket() {
        logger.entry();
        logger.exit(tileBucket);
        return tileBucket;
    }

    public void setTileBucket(String tileBucket) {
        logger.entry(tileBucket);
        this.tileBucket = tileBucket;
        logger.exit();
    }

    public String getTileBucketRegion() {
        logger.entry();
        logger.exit(tileBucketRegion);
        return tileBucketRegion;
    }

    public void setTileBucketRegion(String tileBucketRegion) {
        logger.entry(tileBucketRegion);
        this.tileBucketRegion = tileBucketRegion;
        logger.exit();
    }
}
