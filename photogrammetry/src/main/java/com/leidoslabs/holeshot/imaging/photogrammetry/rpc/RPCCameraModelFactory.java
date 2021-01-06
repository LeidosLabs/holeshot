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
package com.leidoslabs.holeshot.imaging.photogrammetry.rpc;

import java.util.Map;

import org.image.common.util.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.leidoslabs.holeshot.imaging.photogrammetry.CameraModel;
import org.locationtech.jts.geom.Envelope;

/**
 * Creates RPC Camera Models from metadata RPC TREs or from a different camera
 * model
 *
 * Created by parrise on 2/22/17.
 */
public class RPCCameraModelFactory {

	private static final Logger LOGGER = LoggerFactory.getLogger(RPCCameraModelFactory.class);

	/**
	 * Conversion array for RPC00A to RPC00B cubic polynomial coefficient ordering
	 */
	private static final int RPC_A_TO_RPC_B[] = { 0, 1, 2, 3, 4, 5, 6, 8, 9, 10, 7, 11, 14, 17, 12, 15, 18, 13, 16,
			19 };

	/**
	 * Create an RPC camera model from the given metadata
	 * 
	 * @param metadata Metadata map, should contain a key representing some variant
	 *                 of an RPC TRE
	 * @return An RPC model built from the metadata
	 */
	public static RPCCameraModel buildRPCCameraFromMetadata(Map<String, Object> metadata) {
		return buildRPCCameraFromMetadata(metadata, null, null);
	}

	/**
	 * Create an RPC camera model from the given metadata. If the metadata does not
	 * have a valid RPC TRE, the backupCameraModel and imageBounds will be used to
	 * fit coefficients to an RPC model. The given backupCameraModel will be used to
	 * create a discrete grid of tie points within the given imageBounds envelope,
	 * from which RPC values fitting this grid will be solved for
	 * 
	 *
	 * @param metadata          The image scene metadata containing the RPC TRE if
	 *                          it exists. metadata is either a JSON-like hierarchical object, or a map flattened TREs.
	 *                          We accommodate both cases
	 * @param backupCameraModel The camera model to use of no RPC TRE is found to
	 *                          create the described tie point grid
	 * @param imageBounds       The valid bounds of the backup camera model
	 * @return If valid, an RPC camera model utilizing the given metadata, else one
	 *         fitted to the backupCameraModel
	 */
	public static RPCCameraModel buildRPCCameraFromMetadata(Map<String, Object> metadata, CameraModel backupCameraModel,
			Envelope imageBounds) {
		
		// Note: At ingest, 'metadata' is a map of flattened TREs (e.g metadata may have key 'ICHIPB_FI_ROW_11' with value string)
		// When called from tile-service-sdk, 'metadata' is a JSON-like hierarchical object (e.g metadata may have key 'ICHIPB' with a map value)
		// TODO: Do some refactoring here so we don't have to do so much testing about the structure of 'metadata'
		
		String rpcSection = null;
		String rpcPrefix = "";
		
		String ichipbPrefix = "";
		Map<String, Object> rpcTREMap;
		Map<String, Object> ichipbMap = null;
		
		double scaleFactor = 1;
		double ol = 0;
		double os = 0;
		RpcSolver rpcSolver = new RpcSolver(false, false);
		
		if (metadata.containsKey("ICHIPB")) {
			ichipbMap = (Map) metadata.get("ICHIPB");
		}
		else if (metadata.containsKey("ICHIPB_FI_ROW_11")){
			ichipbMap = (Map) metadata;
			ichipbPrefix = "ICHIPB_";
		}
		if (ichipbMap != null) {
			scaleFactor = getDouble(ichipbMap, ichipbPrefix, "SCALE_FACTOR");
			ol = getDouble(ichipbMap, ichipbPrefix, "FI_ROW_11");
			os = getDouble(ichipbMap, ichipbPrefix, "FI_COL_11");
			// if we use the backup model to solve for RPCs, include ICHIPB params
			rpcSolver = new RpcSolver(false, false, ol, os, scaleFactor);
		}

		if (metadata.containsKey("RPC00A")) {
			LOGGER.debug("RPC00A TRE found.");
			rpcSection = "RPC00A";
		} else if (metadata.containsKey("RPC00B")) {
			LOGGER.debug("RPC00B TRE found.");
			rpcSection = "RPC00B";
		} else if (metadata.containsKey("RPC00B_ERR_BIAS")) {
			LOGGER.debug("RPC00B (variant 2) TRE found.");
			rpcPrefix = "RPC00B_";
		} else if (metadata.containsKey("RPC00A_ERR_BIAS")) {
			LOGGER.debug("RPC00A (variant 2) TRE found.");
			rpcPrefix = "RPC00A_";
		} else if (backupCameraModel != null && imageBounds != null && rpcPrefix.equals("")) {
			rpcSolver.solveCoefficients(imageBounds, backupCameraModel, -1, -1, false);
			return rpcSolver.getRPCCameraModel();
		} else {
			LOGGER.warn("No RPC00A or RPC00B TREs found. Can't create camera model.");
			return null;
		}

		if (rpcSection != null) {
			rpcTREMap = (Map) metadata.get(rpcSection);
		} else {
			rpcTREMap = (Map) metadata;
		}

		ErrorEstimate error = new ErrorEstimate(getDouble(rpcTREMap, rpcPrefix, "ERR_BIAS"),
				getDouble(rpcTREMap, rpcPrefix, "ERR_RAND"));

		NormalizationCoefficients normalization = new NormalizationCoefficients(
				getDouble(rpcTREMap, rpcPrefix, "SAMP_OFF"), getDouble(rpcTREMap, rpcPrefix, "LINE_OFF"),
				getDouble(rpcTREMap, rpcPrefix, "LONG_OFF"), getDouble(rpcTREMap, rpcPrefix, "LAT_OFF"),
				getDouble(rpcTREMap, rpcPrefix, "HEIGHT_OFF"), getDouble(rpcTREMap, rpcPrefix, "SAMP_SCALE"),
				getDouble(rpcTREMap, rpcPrefix, "LINE_SCALE"), getDouble(rpcTREMap, rpcPrefix, "LONG_SCALE"),
				getDouble(rpcTREMap, rpcPrefix, "LAT_SCALE"), getDouble(rpcTREMap, rpcPrefix, "HEIGHT_SCALE"));

		double[] lineNumCoeff = new double[20];
		double[] lineDenCoeff = new double[20];
		double[] sampNumCoeff = new double[20];
		double[] sampDenCoeff = new double[20];

		for (int i = 0; i < 20; i++) {
			lineNumCoeff[i] = getDouble(rpcTREMap, rpcPrefix, "LINE_NUM_COEFF_" + (i + 1));
			lineDenCoeff[i] = getDouble(rpcTREMap, rpcPrefix, "LINE_DEN_COEFF_" + (i + 1));
			sampNumCoeff[i] = getDouble(rpcTREMap, rpcPrefix, "SAMP_NUM_COEFF_" + (i + 1));
			sampDenCoeff[i] = getDouble(rpcTREMap, rpcPrefix, "SAMP_DEN_COEFF_" + (i + 1));
		}

		// If necessary swap coefficient order for RPC00A to RPC00B
		if ("RPC00A".equals(rpcSection)) {
			lineNumCoeff = convertRPC00AToRPC00B(lineNumCoeff);
			lineDenCoeff = convertRPC00AToRPC00B(lineDenCoeff);
			sampNumCoeff = convertRPC00AToRPC00B(sampNumCoeff);
			sampDenCoeff = convertRPC00AToRPC00B(sampDenCoeff);
		}

		// If the image is not a full frame then we need to correct the camera model.
		// The RPC
		// coefficients describe the entire imaging event. If this is a chip or if it is
		// a portion
		// of a multi-segment image then we need to adjust the offsets to reflect the
		// actual pixel
		// coordinates.
		if (metadata.containsKey("ICHIPB")) {
			LOGGER.debug("ICHIPB TRE found; adjusting offset and scale to image chip coordinates");
			normalization.setLineScale(normalization.getLineScale() * scaleFactor);
			normalization.setSampScale(normalization.getSampScale() * scaleFactor);
			normalization.setLineOff(scaleFactor * (normalization.getLineOff() - ol));
			normalization.setSampOff(scaleFactor * (normalization.getSampOff() - os));
		} else {
			// Possibly a multi-segment image
			int blockSizeH = -1;
			int blockSizeV = -1;
			int startRow = -1;
			int startColumn = -1;

			// TODO: FIX Multi Segment Adjustment in RPCCameraModelFactory
			// I don't believe these parameters are parsed out of the TREs using these names
			if (metadata.containsKey("IMAGE_TILE_WIDTH") && metadata.containsKey("IMAGE_TILE_HEIGHT")) {
				blockSizeH = NumberUtils.getInteger(metadata.get("IMAGE_TILE_WIDTH"));
				blockSizeV = NumberUtils.getInteger(metadata.get("IMAGE_TILE_HEIGHT"));
			}

			String stdidPrefix = null;
			if (metadata.containsKey("STDIDB")) {
				stdidPrefix = "STDIDB";
			} else if (metadata.containsKey("STDIDC")) {
				stdidPrefix = "STDIDC";
			}

			if (stdidPrefix != null) {
				LOGGER.debug("{} TRE found; adjusting RPC for multi segment image", stdidPrefix);
				Map<String, Object> stdidMap = (Map) metadata.get(stdidPrefix);
				startRow = NumberUtils.getInteger(stdidMap.get("START_ROW"));
				startColumn = NumberUtils.getInteger(stdidMap.get("START_COLUMN"));
			}

			if (blockSizeH != -1 && blockSizeV != -1 && startRow != -1 && startColumn != -1) {
				LOGGER.trace("Adjusting RPC offsets based on {} TRE:", stdidPrefix);
				LOGGER.trace("START_ROW: {} START_COLUMN: {}", startRow, startColumn);
				LOGGER.trace("BLOCK_SIZE_V: {} BLOCK_SIZE_H: {}", blockSizeV, blockSizeH);
				LOGGER.trace("Starting Offsets LINE: {} SAMPLE: {}", normalization.getLineOff(),
						normalization.getSampOff());

				normalization.setLineOff(normalization.getLineOff() - ((startRow - 1) * blockSizeV));
				normalization.setSampOff(normalization.getSampOff() - ((startColumn - 1) * blockSizeH));

				LOGGER.trace("Adjusted Offsets LINE: {} SAMPLE: {}", normalization.getLineOff(),
						normalization.getSampOff());
			}
		}

		// Create the rational polynomial camera model
		CameraPolynomial lineNumPoly = new CameraPolynomial(lineNumCoeff);
		CameraPolynomial lineDenPoly = new CameraPolynomial(lineDenCoeff);
		CameraPolynomial sampNumPoly = new CameraPolynomial(sampNumCoeff);
		CameraPolynomial sampDenPoly = new CameraPolynomial(sampDenCoeff);

		RationalCameraPolynomial lineRP = new RationalCameraPolynomial(lineNumPoly, lineDenPoly);
		RationalCameraPolynomial sampRP = new RationalCameraPolynomial(sampNumPoly, sampDenPoly);

		RPCCameraModel rpCamera = new RPCCameraModel(sampRP, lineRP, normalization, error);
		return rpCamera;
	}

	/**
	 * Reorder an RPC00A format coefficient array to RPC00B format
	 *
	 * @param oldCoeffsArray RPC00A coefficients
	 * @return RPC00B coefficients
	 */
	private static double[] convertRPC00AToRPC00B(double[] oldCoeffsArray) {
		double[] newCoeffsArray = new double[20];
		for (int i = 0; i < newCoeffsArray.length; i++) {
			newCoeffsArray[i] = oldCoeffsArray[RPC_A_TO_RPC_B[i]];
		}
		return newCoeffsArray;
	}

	/**
	 * @return A 'blank' RPC camera model
	 */
	public static RPCCameraModel buildNullCameraModel() {
		return new RPCCameraModel();
	}

	private static double getDouble(Map<String, Object> rpcTREMap, String prefix, String name) {
		Object value = rpcTREMap.get(String.join("", prefix == null ? "" : prefix, name));
		final double result = NumberUtils.getDouble(value);
		return result;
	}
}
