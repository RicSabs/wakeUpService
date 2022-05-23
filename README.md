# wakeUpService
This is an example app for Google Glass Enterprise Edition 2 to turn the screen on with a simple head gesture.

#### How it works
Tap on the TouchPad to turn off the screen (takes a couple of seconds).  
Tilt the head to the left to turn the screen on.

A partial wake lock is used to keep handling the sensor values from the accelerometer.

The logcat is saved in a external file to show if the sensor data is still being received.

#### Why using a partial wake lock
When using a foreground service or the WorkManager like described in the Android developer documentation:

- [Background Work](https://developer.android.com/guide/background#recommended-solutions)
- [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager/advanced/long-running)

the sensors stop reporting values after the screen is turned off (when disconnected from power source or the debugging via WiFi).

### Still existing problem
Screen is off + sensors are being read, not able to know if the hinge of the glass is closed.  
This will result in the unwanted behaviour of the screen turning on again when the glass is tilted and the hinge is closed.