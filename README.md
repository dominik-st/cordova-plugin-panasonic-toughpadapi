# cordova-plugin-panasonic-toughpadapi

Cordova android plugin for Panasonic FZ-N1

#### Prerequisite
1. Download & install Panasonic ToughPad SDK from official web site
2. Go to installation directory(for default is in C:\Program Files (x86)\Panasonic\Toughpad SDK\Library) and copy file Toughpad.jar to our source code directory in src/android/libs

#### Usage
Set Event-Handler for Scan-Event
```
PanasonicToughpadApi.onBarcodeRead('', function (res) {
    // success
}, function (error) {
    // error
});
```

#### Important
With Version <= 1.0.3 parallel usage of the scan API in different Apps was not possible,
With Versions > 2.0.0 parallel use is possible, but you have to re-set the event handler after resuming your App.


``` 
document.addEventListener("resume", this.onResume.bind(this), false);

onResume: function () {

    BarcodeScannerAPI.initAPI();

}


initAPI: function () {
	PanasonicToughpadApi.onBarcodeRead(this._onBarcodeSuccess.bind(this));
}

``` 
