/*
 * Copyright 2019 Dominik Steinrücken
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cpro.cordova.plugin.PanasonicToughpadApi;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.widget.Toast;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import com.panasonic.toughpad.android.api.ToughpadApi;
import com.panasonic.toughpad.android.api.ToughpadApiListener;
import com.panasonic.toughpad.android.api.barcode.BarcodeData;
import com.panasonic.toughpad.android.api.barcode.BarcodeException;
import com.panasonic.toughpad.android.api.barcode.BarcodeListener;
import com.panasonic.toughpad.android.api.barcode.BarcodeReader;
import com.panasonic.toughpad.android.api.barcode.BarcodeReaderManager;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/*
* Main Cordova plugin class
 */
public class CordovaPluginPanasonicToughpadApi extends CordovaPlugin implements ToughpadApiListener {

    private static final String LOG_TAG = "cpro.ToughpadApiPlugin";
    private ToughpadApiBarcodeListener barcodeListener;


    public CordovaPluginPanasonicToughpadApi() {
    }


    @Override
    public void pluginInitialize() {
        super.pluginInitialize();
        this.barcodeListener = new ToughpadApiBarcodeListener();

        try {
            ToughpadApi.initialize(this.cordova.getActivity().getApplicationContext(), this);
        }
        catch (RuntimeException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
        }
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        Log.i(LOG_TAG, "call for action: " + action + "; args: " + args);

        if (ToughpadApiBarcodeListener.ACTION_NAME.equals(action)) {
            this.barcodeListener.setCallBack(callbackContext);
            return true;
        } else {
            return false;
        }

    }

    @Override
    public void onDestroy() {
    	try {
            super.onDestroy();
        	this.barcodeListener.destroy();
        	ToughpadApi.destroy();
    	}
        catch (RuntimeException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
        }
    }



    @Override
    public void onApiConnected(int arg0) {
        this.barcodeListener.initialize();
        Log.i(LOG_TAG, "ToughpadApi connected");
    }

    @Override
    public void onApiDisconnected() {
        Log.i(LOG_TAG, "ToughpadApi disconnected");
    }

}

/*
	Listener Class for Barcode-Events
 */
class  ToughpadApiBarcodeListener implements BarcodeListener {

    public static final String LOG_TAG = "cpro.BarcodeListener";
    public static final String ACTION_NAME = "onBarcodeRead";
    protected CallbackContext callback;


    public void initialize() {
        Log.d(LOG_TAG, "initialize");

        for (BarcodeReader barcodeReader : BarcodeReaderManager.getBarcodeReaders()) {
            Log.d(LOG_TAG, "listen to " + barcodeReader.getDeviceName());
            try {
                barcodeReader.enable(10000);
                barcodeReader.addBarcodeListener(this);
            }
            catch (BarcodeException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
            }
            catch (TimeoutException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
            }
        }
    }

    public void destroy() {
        Log.d(LOG_TAG, "destroy");

        for (BarcodeReader barcodeReader : BarcodeReaderManager.getBarcodeReaders()) {
            Log.d(LOG_TAG, "remove from " + barcodeReader.getDeviceName());
            barcodeReader.removeBarcodeListener(this);
        }

        if (this.callback != null) {
            this.callback.success();
        }
    }

    public void setCallBack(CallbackContext callback) {
        this.callback = callback;
    }
	
	/* Event fired when Barcode is scanned
	
	*/
    @Override
    public void onRead(BarcodeReader barcodeReader, BarcodeData barcodeData) {
        Log.d(LOG_TAG, "onRead: " + barcodeReader.getDeviceName() + " - " + barcodeData.getSymbology());

        JSONObject jsonOnRead = new JSONObject();

        try {
            jsonOnRead.put("barcodeReader", BarcodeReaderSuccess.toJson(barcodeReader));
            jsonOnRead.put("barcodeData", BarcodeDataSuccess.toJson(barcodeData));

            this.sendPluginResult(jsonOnRead);
        }
        catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            this.sendPluginResult(e);
        }
        catch (BarcodeException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            this.sendPluginResult(e);
        }
    }


    public static void sendPluginResult(CallbackContext callback, JSONException je) {
        if (callback != null) {
            PluginResult result = new PluginResult(PluginResult.Status.JSON_EXCEPTION, je.getMessage());
            result.setKeepCallback(true);
            callback.sendPluginResult(result);
        }
    }

    public static void sendPluginResult(CallbackContext callback, Exception e) {
        if (callback != null) {
            PluginResult result = new PluginResult(PluginResult.Status.ERROR, e.getMessage());
            result.setKeepCallback(true);
            callback.sendPluginResult(result);
        }
    }

    public static void sendPluginResult(CallbackContext callback, JSONObject message) {
        if (callback != null) {
            PluginResult result = new PluginResult(PluginResult.Status.OK, message);
            result.setKeepCallback(true);
            callback.sendPluginResult(result);
        }
    }

    protected void sendPluginResult(Exception e) {
        sendPluginResult(this.callback, e);
    }

    protected void sendPluginResult(JSONException je) {
        sendPluginResult(this.callback, je);
    }

    protected void sendPluginResult(JSONObject message) {
        sendPluginResult(this.callback, message);
    }

}

/*
	Object to return Device-Data
*/

class BarcodeReaderSuccess {
    public static final String LOG_TAG = "cpro.BCSuccess";

    public static JSONObject toJson(com.panasonic.toughpad.android.api.barcode.BarcodeReader barcodeReader)
            throws JSONException, BarcodeException {
        JSONObject jsonBarcodeReader = new JSONObject();

        jsonBarcodeReader.put("deviceName", barcodeReader.getDeviceName());
        jsonBarcodeReader.put("isEnabled", barcodeReader.isEnabled());
        jsonBarcodeReader.put("isExternal", barcodeReader.isExternal());
        jsonBarcodeReader.put("isHardwareTriggerAvailable", barcodeReader.isHardwareTriggerAvailable());

        if (barcodeReader.isEnabled()) {
            try {
                int barcodeType = barcodeReader.getBarcodeType();
                String barcodeTypeName;

                switch (barcodeType) {
                    case com.panasonic.toughpad.android.api.barcode.BarcodeReader.BARCODE_TYPE_CAMERA:
                        barcodeTypeName = "BARCODE_TYPE_CAMERA";
                        break;
                    case com.panasonic.toughpad.android.api.barcode.BarcodeReader.BARCODE_TYPE_ONE_DIMENSIONAL:
                        barcodeTypeName = "BARCODE_TYPE_ONE_DIMENSIONAL";
                        break;
                    case com.panasonic.toughpad.android.api.barcode.BarcodeReader.BARCODE_TYPE_TWO_DIMENSIONAL:
                        barcodeTypeName = "BARCODE_TYPE_TWO_DIMENSIONAL";
                        break;
                    default:
                        barcodeTypeName = "";
                        break;
                }

                jsonBarcodeReader.put("barcodeType", barcodeType);
                jsonBarcodeReader.put("barcodeTypeName", barcodeTypeName);

                jsonBarcodeReader.put("batteryCharge", barcodeReader.getBatteryCharge());
                jsonBarcodeReader.put("deviceFirmwareVersion", barcodeReader.getDeviceFirmwareVersion());
                jsonBarcodeReader.put("deviceSerialNumber", barcodeReader.getDeviceSerialNumber());
                jsonBarcodeReader.put("isBatteryCharging", barcodeReader.isBatteryCharging());
                jsonBarcodeReader.put("isHardwareTriggerEnabled", barcodeReader.isHardwareTriggerEnabled());
            }
            catch (IllegalStateException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
            }
        }

        return jsonBarcodeReader;
    }
}


/*
	Object to return Barcode-Data
*/
class BarcodeDataSuccess {
    public static JSONObject toJson(com.panasonic.toughpad.android.api.barcode.BarcodeData barcodedata) throws JSONException {
        JSONObject jsonBarcodeData = new JSONObject();

        jsonBarcodeData.put("binaryData", barcodedata.getBinaryData());
        jsonBarcodeData.put("encoding", barcodedata.getEncoding());
        jsonBarcodeData.put("symbology", barcodedata.getSymbology());
        jsonBarcodeData.put("textData", barcodedata.getTextData());

        return jsonBarcodeData;
    }

}