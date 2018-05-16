# CarStoreApp
Sample Android App for demonstrating BrainTree express checkout integration

## Server side
Check [CarStoreWeb](https://github.com/liuwei108/CarStoreWeb) repository to see server side implementation

## How to run this demo app
1. download [app.apk](https://github.com/liuwei108/CarStoreApp/raw/master/app.apk)
1. make sure Android SDK is installed and adb command line work
2. open a Virtual Device or connect to Device
3. run `adb install app.apk` to install apk to device
4. run `adb shell am start -n com.wondercars.client.carstore/.MainActivity` to start app

### Sandbox test account
> buyer_1@paypalsandbox.com / 12345678